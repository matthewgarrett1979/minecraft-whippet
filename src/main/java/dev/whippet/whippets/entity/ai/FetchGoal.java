package dev.whippet.whippets.entity.ai;

import dev.whippet.whippets.ModItems;
import dev.whippet.whippets.entity.WhippetEntity;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

/**
 * The ball.
 *
 * <p>A thrown ball is the one thing a whippet will interrupt anything for. It
 * goes after it flat out, picks it up, and then — and this is the part every
 * whippet owner will recognise — it mostly brings it back. Mostly. One throw in
 * four it takes the ball somewhere else entirely and does a lap with it first,
 * on the grounds that it is now its ball.
 *
 * <p>A ball lying at your feet is not interesting. It has to have been thrown.
 */
public class FetchGoal extends Goal {
	/** How far off a ball is still worth going for. */
	private static final double SEE_IT_GO = 24.0;
	/** A ball nearer to you than this is one you are holding out, not one in play. */
	private static final double IN_PLAY = 4.0;
	/** Far enough that it is worth going flat out rather than trotting over. */
	private static final double WORTH_A_SPRINT = 8.0;
	private static final double SPEED = 1.3;
	/** Close enough to get a mouth round it. */
	private static final double MOUTHFUL = 1.3;
	/** Close enough to give it back, roughly. */
	private static final double DELIVERED = 2.2;
	/** How long it will chase one ball before losing interest. */
	private static final int GIVE_UP = 20 * 30;
	/** A dog with nobody to give it to carries it about for this long. */
	private static final int CARRIES_IT_ABOUT = 20 * 10;
	/** How long a dog that has decided the ball is now its own keeps it away from you. */
	private static final int LAP_OF_THE_FIELD = 20 * 6;
	/** How far off it takes it to do that. */
	private static final double AWAY_FROM_YOU = 10.0;
	private static final int BETWEEN_FETCHES = 40;

	private final WhippetEntity whippet;
	private @Nullable ItemEntity ball;
	private int ticks;
	private int cooldown;
	/** Ticks left of taking the ball somewhere else entirely, which is its right. */
	private int showingOff;

	public FetchGoal(WhippetEntity whippet) {
		this.whippet = whippet;
		this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		if (this.cooldown > 0) {
			this.cooldown--;
			return false;
		}

		if (this.busy()) {
			return false;
		}

		if (this.whippet.isCarryingBall()) {
			return true;
		}

		this.ball = this.nearestBallInPlay();

		return this.ball != null;
	}

	@Override
	public boolean shouldContinue() {
		if (this.busy() || this.ticks > GIVE_UP) {
			return false;
		}

		return this.whippet.isCarryingBall() || this.ball != null && this.ball.isAlive();
	}

	@Override
	public void start() {
		this.ticks = 0;
		this.showingOff = 0;

		// A ball thrown any distance is chased properly, which is the point of
		// throwing it for a whippet rather than for any other dog.
		if (this.ball != null && this.whippet.squaredDistanceTo(this.ball) > WORTH_A_SPRINT * WORTH_A_SPRINT) {
			this.whippet.slip();
		}
	}

	@Override
	public void stop() {
		this.ball = null;
		this.cooldown = BETWEEN_FETCHES;
		this.whippet.getNavigation().stop();
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		this.ticks++;

		if (this.whippet.isCarryingBall()) {
			this.bringItBack();
			return;
		}

		ItemEntity ball = this.ball;

		if (ball == null || !ball.isAlive()) {
			return;
		}

		this.whippet.getLookControl().lookAt(ball);

		if (this.whippet.squaredDistanceTo(ball) <= MOUTHFUL * MOUTHFUL) {
			this.whippet.pickUpBall(ball);
			this.ticks = 0;
			// One dog in four decides at this point that it is now its ball.
			this.showingOff = this.whippet.getRandom().nextInt(4) == 0 ? LAP_OF_THE_FIELD : 0;
			return;
		}

		if (this.whippet.getNavigation().isIdle()) {
			this.whippet.getNavigation().startMovingTo(ball, SPEED);
		}
	}

	/** Back to whoever threw it, at a trot, with its head up. */
	private void bringItBack() {
		LivingEntity owner = this.whippet.getOwner();

		// Not yet. It is a very good ball and it is going over there with it.
		if (this.showingOff > 0 && owner != null) {
			this.showingOff--;
			this.whippet.getLookControl().lookAt(owner);

			if (this.whippet.squaredDistanceTo(owner) < AWAY_FROM_YOU * AWAY_FROM_YOU && this.whippet.getNavigation().isIdle()) {
				Vec3d away = this.whippet.getEntityPos().subtract(owner.getEntityPos());
				Vec3d off = this.whippet.getEntityPos().add(away.horizontalLength() < 1.0E-4 ? new Vec3d(AWAY_FROM_YOU, 0.0, 0.0) : away.normalize().multiply(AWAY_FROM_YOU));
				this.whippet.getNavigation().startMovingTo(off.x, off.y, off.z, SPEED);
			}

			return;
		}

		if (owner == null || !owner.isAlive() || owner.getEntityWorld() != this.whippet.getEntityWorld()) {
			// Nobody to give it to. It keeps it, then gets bored of keeping it.
			if (this.ticks > CARRIES_IT_ABOUT) {
				this.whippet.dropBall();
			}

			return;
		}

		this.whippet.getLookControl().lookAt(owner);

		if (this.whippet.squaredDistanceTo(owner) <= DELIVERED * DELIVERED) {
			this.whippet.dropBall();
			this.whippet.getNavigation().stop();
			return;
		}

		if (this.whippet.getNavigation().isIdle()) {
			this.whippet.getNavigation().startMovingTo(owner, SPEED);
		}
	}

	/** Anything a whippet would not drop for a ball. There is not much on the list. */
	private boolean busy() {
		return this.whippet.isRacing() || this.whippet.isInSittingPose() || this.whippet.isBurrowed() || this.whippet.isLeashed();
	}

	/**
	 * The nearest thrown ball. One sitting at its owner's feet does not count —
	 * otherwise the dog spends the rest of the day handing you the same ball.
	 */
	private @Nullable ItemEntity nearestBallInPlay() {
		Box range = this.whippet.getBoundingBox().expand(SEE_IT_GO);
		List<ItemEntity> balls = this.whippet.getEntityWorld().getEntitiesByClass(
			ItemEntity.class, range, item -> item.isAlive() && item.getStack().isOf(ModItems.WHIPPET_BALL)
		);
		LivingEntity owner = this.whippet.getOwner();
		ItemEntity nearest = null;
		double best = Double.MAX_VALUE;

		for (ItemEntity ball : balls) {
			if (owner != null && ball.squaredDistanceTo(owner) < IN_PLAY * IN_PLAY) {
				continue;
			}

			double distance = this.whippet.squaredDistanceTo(ball);

			if (distance < best) {
				best = distance;
				nearest = ball;
			}
		}

		return nearest;
	}
}
