package dev.whippet.whippets.entity.ai;

import dev.whippet.whippets.entity.WhippetEntity;
import java.util.EnumSet;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.Vec3d;

/**
 * Holds a whippet in the traps and then runs it at the lure. It outranks every
 * other goal: a racing dog does not stop to look at a player, and it certainly
 * does not sit down.
 */
public class RaceGoal extends Goal {
	/**
	 * The pace they settle into out of the traps. It is below what a whippet can
	 * actually do, because the whole shape of a race is the run-in: a dog that
	 * was already flat out at the bell has nothing to find at the business end,
	 * and there is nothing to watch.
	 */
	private static final double RACE_SPEED = 1.35;
	/**
	 * Re-issue the path this often. The dog steers itself at the lure while it
	 * has a clear run; this is the fallback for when it has not, so it has to
	 * keep up with a dog that is moving.
	 */
	private static final int REPATH_INTERVAL = 8;
	/** The earliest and latest a dog will make its run, as a share of the trip. */
	private static final double EARLIEST_KICK = 0.25;
	private static final double LATEST_KICK = 0.75;

	private final WhippetEntity whippet;
	private int repathIn;
	/** How far from the lure this dog decided to go flat out, in blocks. */
	private double kickAt;

	public RaceGoal(WhippetEntity whippet) {
		this.whippet = whippet;
		this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK, Goal.Control.JUMP, Goal.Control.TARGET));
	}

	@Override
	public boolean canStart() {
		return this.whippet.isRacing();
	}

	@Override
	public boolean shouldContinue() {
		return this.whippet.isRacing();
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void start() {
		this.repathIn = 0;
		this.whippet.setInSittingPose(false);
		this.whippet.setTarget(null);

		Vec3d lure = this.whippet.getLurePos();

		if (lure == null) {
			this.kickAt = 0.0;
			return;
		}

		// Every dog decides for itself where to make its run, and there is only
		// six seconds of flat out in any of them. Going early wins a short race
		// and loses a long one, because a dog that empties the tank before the
		// line comes home blown — which is the whole of whippet racing.
		double trip = Math.sqrt(this.whippet.squaredDistanceTo(lure.x, lure.y, lure.z));
		this.kickAt = trip * (EARLIEST_KICK + this.whippet.getRandom().nextDouble() * (LATEST_KICK - EARLIEST_KICK));
	}

	@Override
	public void stop() {
		this.whippet.getNavigation().stop();
		this.repathIn = 0;
	}

	@Override
	public void tick() {
		Vec3d lure = this.whippet.getLurePos();

		if (lure == null) {
			return;
		}

		this.whippet.getLookControl().lookAt(lure.x, lure.y + 0.5, lure.z);

		if (!this.whippet.isInTraps() && this.whippet.getReactionTicks() > 0) {
			// Broke slowly: still on the line while the others are gone.
			this.whippet.tickReaction();
			this.whippet.getNavigation().stop();
			this.whippet.setVelocity(Vec3d.ZERO);
			return;
		}

		if (this.whippet.isInTraps()) {
			// Held on the line: no creeping forward before the bell.
			this.whippet.getNavigation().stop();
			this.whippet.setVelocity(Vec3d.ZERO);
			this.whippet.setSidewaysSpeed(0.0F);
			this.whippet.setForwardSpeed(0.0F);
			return;
		}

		// The run-in. Inside its own mark the dog goes flat out and stays there
		// for as long as it has the breath, which may or may not be far enough.
		if (this.whippet.squaredDistanceTo(lure.x, lure.y, lure.z) <= this.kickAt * this.kickAt) {
			this.whippet.slip();
		}

		if (this.repathIn-- <= 0) {
			this.repathIn = REPATH_INTERVAL;
			this.whippet.getNavigation().startMovingTo(lure.x, lure.y, lure.z, RACE_SPEED * this.whippet.getPace());
		}
	}
}
