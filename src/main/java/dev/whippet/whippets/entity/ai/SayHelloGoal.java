package dev.whippet.whippets.entity.ai;

import dev.whippet.whippets.entity.WhippetEntity;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import org.jspecify.annotations.Nullable;

/**
 * Bobby says hello.
 *
 * <p>The dog that takes an XL Bully apart in nine seconds is, the rest of the
 * time, the friendliest animal on the ground. He stops whatever he is doing for
 * anybody who comes near — you, the Guv'nor, the other dogs, a villager who has
 * wandered in, a sheep that has no idea what it is being greeted by — walks
 * over, stands still, puts his nose on them once and wags his tail off. Then he
 * goes back to what he was doing, and does it again for the next one.
 *
 * <p>He does not say hello to the same one over and over: each gets about a
 * minute of being remembered. Beyond that there is no list of exceptions. A
 * zombie is somebody he has not met. A creeper is somebody he has not met. The
 * XL Bully is somebody he has not met, and he will go over and put his nose on
 * it, and what happens next is the bully's business. Nor does he take being hit
 * as an answer: once his nose is on you the greeting is finished properly, and
 * the only thing that keeps him from starting one is having already decided to
 * chase something else.
 */
public class SayHelloGoal extends Goal {
	/** How far off he will notice somebody worth greeting. */
	private static final double NOTICES_YOU = 14.0;
	private static final double SPEED = 1.1;
	/** Near enough to touch noses. */
	private static final double REACH = 2.2;
	/** How long the greeting itself lasts once he gets there. */
	private static final int HELLO_TICKS = 50;
	/** And how long he will keep trying to reach somebody before giving up. */
	private static final int GIVE_UP = 240;
	/** How long he remembers having said hello to a particular one. */
	private static final int REMEMBERS = 1200;
	/** A breath between greetings, so he is delighted rather than frantic. */
	private static final int BETWEEN = 30;
	/** Nobody needs more names than this remembered at once. */
	private static final int MEMORY = 24;

	private final WhippetEntity whippet;
	private final Map<UUID, Integer> saidHelloTo = new HashMap<>();
	private @Nullable LivingEntity friend;
	private int ticks;
	private int greeting;
	private int cooldown;

	public SayHelloGoal(WhippetEntity whippet) {
		this.whippet = whippet;
		this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		// The champion only. An ordinary whippet says hello to other whippets
		// and is reserved about the rest of the world, which is the breed; this
		// one is not the breed about it at all.
		if (!this.whippet.isChampion()) {
			return false;
		}

		this.forget();

		if (this.cooldown > 0) {
			this.cooldown--;
			return false;
		}

		if (this.busy()) {
			return false;
		}

		this.friend = Nearby.nearest(LivingEntity.class, this.whippet, NOTICES_YOU, this::worthSayingHelloTo);
		return this.friend != null;
	}

	@Override
	public boolean shouldContinue() {
		if (this.friend == null || !this.friend.isAlive() || this.ticks >= GIVE_UP) {
			return false;
		}

		// Once his nose is actually on somebody, he finishes. Being bitten
		// halfway through a hello is not a reason to stop saying hello — he
		// takes the view that they will come round.
		return this.greeting > 0 || !this.busy();
	}

	@Override
	public void start() {
		this.ticks = 0;
		this.greeting = 0;
	}

	@Override
	public void stop() {
		if (this.friend != null) {
			this.saidHelloTo.put(this.friend.getUuid(), REMEMBERS);
		}

		this.friend = null;
		this.cooldown = BETWEEN;
		this.whippet.stopSayingHello();
		this.whippet.getNavigation().stop();
	}

	@Override
	public void tick() {
		if (this.friend == null) {
			return;
		}

		this.ticks++;
		this.whippet.getLookControl().lookAt(this.friend, 30.0F, 30.0F);

		if (this.greeting == 0 && this.whippet.squaredDistanceTo(this.friend) > REACH * REACH) {
			if (this.whippet.getNavigation().isIdle()) {
				this.whippet.getNavigation().startMovingTo(this.friend, SPEED);
			}

			return;
		}

		// Arrived. Everything stops: this is the important part of his day.
		this.whippet.getNavigation().stop();

		if (this.greeting == 0) {
			this.whippet.snoot();
			this.whippet.sayHello(HELLO_TICKS);
		}

		this.greeting++;

		if (this.greeting == HELLO_TICKS / 2) {
			// A word as well, about halfway through, because he cannot help it.
			this.whippet.whine();
		}

		if (this.greeting >= HELLO_TICKS) {
			this.saidHelloTo.put(this.friend.getUuid(), REMEMBERS);
			this.friend = null;
		}
	}

	/**
	 * Anything he would not interrupt to say hello, which is a short list and
	 * gets no longer. Having decided to chase something is not on it: he says
	 * hello to it first and chases it afterwards, and that goes for the rabbit
	 * he is about to eat and the XL Bully he is about to take apart alike. The
	 * greeting holds the controls for two and a half seconds and then hands
	 * them back, and he does not greet the same one twice inside a minute, so
	 * nothing he was going to do stops happening — it happens second.
	 */
	private boolean busy() {
		return this.whippet.isRacing() || this.whippet.isBurrowed() || this.whippet.isCurled() || this.whippet.isInSittingPose();
	}

	/**
	 * Who counts. Everything alive, and that is the whole rule: a zombie coming
	 * at him across a field is a new friend who has not been introduced yet, and
	 * so is the XL Bully. Whether the other party wants any of this does not
	 * come into it and never has.
	 *
	 * <p>There is nothing else to it. He is not checking whether they are
	 * friendly, whether they are coming at him, or whether he is about to have
	 * to fight them.
	 */
	private boolean worthSayingHelloTo(LivingEntity candidate) {
		return candidate != this.whippet && candidate.isAlive() && !this.saidHelloTo.containsKey(candidate.getUuid());
	}

	/** Lets the names go again, oldest first if he has met a crowd. */
	private void forget() {
		this.saidHelloTo.entrySet().removeIf(met -> met.setValue(met.getValue() - 1) <= 0);

		while (this.saidHelloTo.size() > MEMORY) {
			this.saidHelloTo.remove(this.saidHelloTo.keySet().iterator().next());
		}
	}
}
