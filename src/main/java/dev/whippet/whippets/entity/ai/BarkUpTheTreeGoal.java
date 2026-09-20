package dev.whippet.whippets.entity.ai;

import dev.whippet.whippets.entity.SquirrelEntity;
import dev.whippet.whippets.entity.WhippetEntity;
import java.util.EnumSet;
import net.minecraft.entity.ai.goal.Goal;
import org.jspecify.annotations.Nullable;

/**
 * The squirrel is four blocks up the trunk and the whippet is not going to
 * accept that. It plants itself at the bottom, stares straight up and whines
 * about it until it can be persuaded the squirrel has won.
 */
public class BarkUpTheTreeGoal extends Goal {
	private static final double RANGE = 8.0;
	private static final double OVERHEAD = 2.0;
	/** Twenty seconds of hoping, which is longer than most dogs manage. */
	private static final int PATIENCE = 400;
	private static final int GIVE_UP_FOR = 600;

	private final WhippetEntity whippet;
	private @Nullable SquirrelEntity squirrel;
	private int ticks;
	private int cooldown;

	public BarkUpTheTreeGoal(WhippetEntity whippet) {
		this.whippet = whippet;
		this.setControls(EnumSet.of(Goal.Control.LOOK, Goal.Control.MOVE));
	}

	@Override
	public boolean canStart() {
		if (this.cooldown > 0) {
			this.cooldown--;
			return false;
		}

		if (this.whippet.isRacing() || this.whippet.isInSittingPose() || this.whippet.isBurrowed()) {
			return false;
		}

		this.squirrel = this.nearestTreedSquirrel();
		return this.squirrel != null;
	}

	@Override
	public boolean shouldContinue() {
		return this.squirrel != null && this.squirrel.isAlive() && this.isTreed(this.squirrel) && this.ticks < PATIENCE;
	}

	@Override
	public void start() {
		this.ticks = 0;
	}

	@Override
	public void stop() {
		this.squirrel = null;
		this.cooldown = GIVE_UP_FOR;
		this.whippet.getNavigation().stop();
	}

	@Override
	public void tick() {
		if (this.squirrel == null) {
			return;
		}

		this.ticks++;
		this.whippet.getLookControl().lookAt(this.squirrel.getX(), this.squirrel.getY() + 0.2, this.squirrel.getZ(), 30.0F, 60.0F);

		// Close enough to stand under it, then stay there and complain.
		if (this.whippet.squaredDistanceTo(this.squirrel) > 4.0 && this.whippet.getNavigation().isIdle()) {
			this.whippet.getNavigation().startMovingTo(this.squirrel.getX(), this.whippet.getY(), this.squirrel.getZ(), 1.2);
		}

		if (this.ticks % 50 == 0) {
			this.whippet.whine();
		}
	}

	private boolean isTreed(SquirrelEntity squirrel) {
		return squirrel.getY() - this.whippet.getY() >= OVERHEAD && this.whippet.squaredDistanceTo(squirrel) < RANGE * RANGE;
	}

	private @Nullable SquirrelEntity nearestTreedSquirrel() {
		return Nearby.nearest(SquirrelEntity.class, this.whippet, RANGE, this::isTreed);
	}
}
