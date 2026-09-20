package dev.whippet.whippets.entity.ai;

import dev.whippet.whippets.entity.WhippetEntity;
import java.util.EnumSet;
import net.minecraft.entity.ai.goal.Goal;
import org.jspecify.annotations.Nullable;

/**
 * Two whippets who meet go nose to nose. They do not circle, sniff and posture
 * the way other dogs do — they walk straight up to each other, touch noses once,
 * and then get on with whatever they were doing. It lasts a second and it is the
 * whole greeting.
 */
public class GreetGoal extends Goal {
	private static final double RANGE = 9.0;
	private static final double SPEED = 1.1;
	/** Nose to nose. */
	private static final double REACH = 1.7;
	private static final int GIVE_UP = 200;
	/** A long while before the same dog bothers saying hello again. */
	private static final int BETWEEN_GREETINGS = 900;

	private final WhippetEntity whippet;
	private @Nullable WhippetEntity other;
	private int cooldown;
	private int ticks;
	private boolean greeted;

	public GreetGoal(WhippetEntity whippet) {
		this.whippet = whippet;
		this.cooldown = whippet.getRandom().nextInt(200);
		this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		if (this.cooldown > 0) {
			this.cooldown--;
			return false;
		}

		if (this.busy(this.whippet)) {
			return false;
		}

		this.other = Nearby.nearest(WhippetEntity.class, this.whippet, RANGE, dog -> !this.busy(dog));
		return this.other != null;
	}

	@Override
	public boolean shouldContinue() {
		return this.other != null && this.other.isAlive() && !this.greeted && this.ticks < GIVE_UP && !this.busy(this.whippet);
	}

	@Override
	public void start() {
		this.ticks = 0;
		this.greeted = false;
	}

	@Override
	public void stop() {
		this.cooldown = BETWEEN_GREETINGS + this.whippet.getRandom().nextInt(600);
		this.other = null;
		this.whippet.getNavigation().stop();
	}

	@Override
	public void tick() {
		if (this.other == null) {
			return;
		}

		this.ticks++;
		this.whippet.getLookControl().lookAt(this.other, 30.0F, 30.0F);

		if (this.whippet.squaredDistanceTo(this.other) > REACH * REACH) {
			if (this.whippet.getNavigation().isIdle()) {
				this.whippet.getNavigation().startMovingTo(this.other, SPEED);
			}

			return;
		}

		this.whippet.getNavigation().stop();
		this.whippet.snoot();
		this.greeted = true;
	}

	/** Anything a whippet would not interrupt to say hello. */
	private boolean busy(WhippetEntity dog) {
		return dog.isRacing() || dog.isBurrowed() || dog.isCurled() || dog.isInSittingPose() || dog.isZooming() || dog.getTarget() != null;
	}
}
