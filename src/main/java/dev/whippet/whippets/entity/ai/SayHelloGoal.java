package dev.whippet.whippets.entity.ai;

import dev.whippet.whippets.entity.BullyEntity;
import dev.whippet.whippets.entity.SquirrelEntity;
import dev.whippet.whippets.entity.WhippetEntity;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.Monster;
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
 * minute of being remembered. He does not say hello to anything he is fighting,
 * anything he hunts, or anything that would eat him — which leaves nearly
 * everything, because he is not fussy about who his friends are.
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
	private static final int GIVE_UP = 160;
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
		return this.friend != null && this.friend.isAlive() && this.ticks < GIVE_UP && !this.busy();
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

	/** Anything he would not interrupt to say hello — which is a short list. */
	private boolean busy() {
		return this.whippet.isRacing()
			|| this.whippet.isBurrowed()
			|| this.whippet.isCurled()
			|| this.whippet.isInSittingPose()
			|| this.whippet.getTarget() != null;
	}

	/**
	 * Who counts. Everything alive except what he is hunting, what would hunt
	 * him, and the one animal on the ground he has no manners for.
	 */
	private boolean worthSayingHelloTo(LivingEntity candidate) {
		if (candidate == this.whippet || !candidate.isAlive() || this.saidHelloTo.containsKey(candidate.getUuid())) {
			return false;
		}

		if (candidate instanceof Monster || candidate instanceof BullyEntity || candidate instanceof SquirrelEntity) {
			return false;
		}

		return !HuntCatsGoal.isCat(candidate);
	}

	/** Lets the names go again, oldest first if he has met a crowd. */
	private void forget() {
		this.saidHelloTo.entrySet().removeIf(met -> met.setValue(met.getValue() - 1) <= 0);

		while (this.saidHelloTo.size() > MEMORY) {
			this.saidHelloTo.remove(this.saidHelloTo.keySet().iterator().next());
		}
	}
}
