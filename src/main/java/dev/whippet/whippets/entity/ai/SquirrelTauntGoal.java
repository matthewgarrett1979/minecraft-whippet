package dev.whippet.whippets.entity.ai;

import dev.whippet.whippets.entity.SquirrelEntity;
import dev.whippet.whippets.entity.WhippetEntity;
import java.util.EnumSet;
import net.minecraft.entity.ai.goal.Goal;
import org.jspecify.annotations.Nullable;

/**
 * From a branch, with the dog on the ground: face it, flick the tail and
 * chatter at it. A squirrel that is safe does not leave — it stays exactly far
 * enough away to be insulting.
 */
public class SquirrelTauntGoal extends Goal {
	/** How far above the dog counts as safely out of reach. */
	private static final double SAFE_HEIGHT = 2.5;
	private static final double RANGE = 10.0;

	private final SquirrelEntity squirrel;
	private @Nullable WhippetEntity dog;
	private int ticks;

	public SquirrelTauntGoal(SquirrelEntity squirrel) {
		this.squirrel = squirrel;
		this.setControls(EnumSet.of(Goal.Control.LOOK, Goal.Control.MOVE));
	}

	@Override
	public boolean canStart() {
		this.dog = this.nearestWhippet();
		return this.dog != null && this.isSafeFrom(this.dog);
	}

	@Override
	public boolean shouldContinue() {
		return this.dog != null && this.dog.isAlive() && this.isSafeFrom(this.dog) && this.ticks < 400;
	}

	@Override
	public void start() {
		this.ticks = 0;
		this.squirrel.getNavigation().stop();
		this.squirrel.setTaunting(true);
	}

	@Override
	public void stop() {
		this.squirrel.setTaunting(false);

		// The dog has gone, or given up. Down we come.
		if (this.squirrel.isScrambling()) {
			this.squirrel.setScrambling(false);
		}

		this.dog = null;
	}

	@Override
	public void tick() {
		if (this.dog == null) {
			return;
		}

		this.ticks++;
		this.squirrel.getLookControl().lookAt(this.dog, 40.0F, 40.0F);

		// Keep hold of the trunk while the shouting goes on.
		if (this.squirrel.isAgainstTrunk()) {
			this.squirrel.setScrambling(true);
			this.squirrel.setVelocity(net.minecraft.util.math.Vec3d.ZERO);
		}

		if (this.ticks % 25 == 0) {
			this.squirrel.chatter();
		}
	}

	private boolean isSafeFrom(WhippetEntity dog) {
		return this.squirrel.getY() - dog.getY() >= SAFE_HEIGHT && this.squirrel.squaredDistanceTo(dog) < RANGE * RANGE;
	}

	private @Nullable WhippetEntity nearestWhippet() {
		return Nearby.nearest(WhippetEntity.class, this.squirrel, RANGE, dog -> true);
	}
}
