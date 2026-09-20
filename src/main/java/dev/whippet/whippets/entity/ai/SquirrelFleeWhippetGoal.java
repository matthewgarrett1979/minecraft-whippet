package dev.whippet.whippets.entity.ai;

import dev.whippet.whippets.entity.SquirrelEntity;
import dev.whippet.whippets.entity.WhippetEntity;
import java.util.EnumSet;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

/**
 * The whole point of a squirrel. A whippet comes into view, the squirrel drops
 * whatever it was doing, runs for the nearest trunk and goes up it — and if
 * there is no tree, it simply outruns the dog in a straight line, which is the
 * one thing a whippet does not expect.
 */
public class SquirrelFleeWhippetGoal extends Goal {
	private static final double SPRINT = 2.0;
	/** How far up the trunk counts as out of reach and worth stopping at. */
	private static final double HIGH_ENOUGH = 5.0;
	private static final int TRUNK_SEARCH = 10;

	private final SquirrelEntity squirrel;
	private @Nullable WhippetEntity dog;
	private @Nullable BlockPos trunk;
	private double groundY;

	public SquirrelFleeWhippetGoal(SquirrelEntity squirrel) {
		this.squirrel = squirrel;
		this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		this.dog = this.nearestWhippet();

		if (this.dog == null) {
			return false;
		}

		this.groundY = this.squirrel.getY();
		this.trunk = this.findTrunk();
		return true;
	}

	@Override
	public boolean shouldContinue() {
		if (this.dog == null || !this.dog.isAlive()) {
			return false;
		}

		// Once it is up out of reach it stops running and starts gloating; the
		// taunt goal takes over from there.
		if (this.squirrel.getY() - this.groundY >= HIGH_ENOUGH) {
			return false;
		}

		return this.squirrel.squaredDistanceTo(this.dog) < SquirrelEntity.ALARM_RANGE * SquirrelEntity.ALARM_RANGE * 1.5;
	}

	@Override
	public void start() {
		this.run();
	}

	@Override
	public void stop() {
		this.dog = null;
		this.trunk = null;
		this.squirrel.getNavigation().stop();
	}

	@Override
	public void tick() {
		if (this.dog == null) {
			return;
		}

		this.squirrel.getLookControl().lookAt(this.dog, 30.0F, 30.0F);

		// Bark within reach: take hold and go straight up it. Nothing a whippet
		// can do about this, which is the entire point of being a squirrel.
		if (this.squirrel.isAgainstTrunk()) {
			this.squirrel.setScrambling(true);
			this.squirrel.getNavigation().stop();

			if (this.squirrel.getY() - this.groundY < HIGH_ENOUGH) {
				this.squirrel.setVelocity(0.0, 0.32, 0.0);
				this.squirrel.velocityDirty = true;
			} else {
				this.squirrel.setVelocity(Vec3d.ZERO);
			}

			return;
		}

		if (this.squirrel.getNavigation().isIdle()) {
			this.run();
		}
	}

	/** Head for the trunk if there is one, otherwise straight away from the dog. */
	private void run() {
		if (this.dog == null) {
			return;
		}

		if (this.trunk != null) {
			this.squirrel.getNavigation().startMovingTo(this.trunk.getX() + 0.5, this.trunk.getY(), this.trunk.getZ() + 0.5, SPRINT);
			return;
		}

		Vec3d away = this.squirrel.getEntityPos().subtract(this.dog.getEntityPos()).normalize().multiply(8.0);
		Vec3d to = this.squirrel.getEntityPos().add(away.x, 0.0, away.z);
		this.squirrel.getNavigation().startMovingTo(to.x, to.y, to.z, SPRINT);
	}

	private @Nullable WhippetEntity nearestWhippet() {
		return Nearby.nearest(WhippetEntity.class, this.squirrel, SquirrelEntity.ALARM_RANGE, dog -> !dog.isBurrowed());
	}

	/**
	 * The nearest trunk that is not on the dog's side of the squirrel — running
	 * past the thing chasing you is how squirrels get caught.
	 */
	private @Nullable BlockPos findTrunk() {
		if (this.dog == null) {
			return null;
		}

		BlockPos origin = this.squirrel.getBlockPos();
		Vec3d fromDog = this.squirrel.getEntityPos().subtract(this.dog.getEntityPos());
		BlockPos best = null;
		double bestScore = Double.MAX_VALUE;

		for (BlockPos pos : BlockPos.iterate(origin.add(-TRUNK_SEARCH, -3, -TRUNK_SEARCH), origin.add(TRUNK_SEARCH, 4, TRUNK_SEARCH))) {
			BlockState state = this.squirrel.getEntityWorld().getBlockState(pos);

			if (!state.isIn(BlockTags.LOGS)) {
				continue;
			}

			Vec3d toTrunk = Vec3d.ofCenter(pos).subtract(this.squirrel.getEntityPos());

			if (toTrunk.horizontalLengthSquared() > 1.0 && toTrunk.normalize().dotProduct(fromDog.normalize()) < -0.2) {
				continue;
			}

			double score = toTrunk.lengthSquared();

			if (score < bestScore) {
				bestScore = score;
				best = pos.toImmutable();
			}
		}

		return best;
	}
}
