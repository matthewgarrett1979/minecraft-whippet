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
	private static final double RACE_SPEED = 1.8;
	/** Re-issue the path this often; navigation gives up on long straights. */
	private static final int REPATH_INTERVAL = 20;

	private final WhippetEntity whippet;
	private int repathIn;

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

		if (this.repathIn-- <= 0) {
			this.repathIn = REPATH_INTERVAL;
			this.whippet.getNavigation().startMovingTo(lure.x, lure.y, lure.z, RACE_SPEED * this.whippet.getPace());
		}
	}
}
