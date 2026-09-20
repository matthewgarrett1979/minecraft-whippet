package dev.whippet.whippets.entity.ai;

import dev.whippet.whippets.entity.WhippetEntity;
import java.util.EnumSet;
import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * The duvet instinct. A cold whippet goes looking for bedding — a bed, or
 * failing that anything woollen — climbs in and disappears under it until it is
 * warm again or somebody calls it.
 */
public class BurrowGoal extends Goal {
	private static final int SEARCH_RADIUS = 10;
	private static final int SEARCH_HEIGHT = 3;
	private static final double SPEED = 1.0;
	/** Pathfinding stops at the edge of the bedding, so arrival is measured flat. */
	private static final double ARRIVED = 2.0;
	private static final int MIN_COOLDOWN = 200;
	private static final int MAX_COOLDOWN = 600;
	/** Give up if the bed cannot be reached in this long. */
	private static final int APPROACH_LIMIT = 20 * 20;

	private final WhippetEntity whippet;
	private @Nullable BlockPos bedding;
	private int cooldown;
	private int approachTicks;
	private int repathIn;

	public BurrowGoal(WhippetEntity whippet) {
		this.whippet = whippet;
		this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK, Goal.Control.JUMP));
	}

	@Override
	public boolean canStart() {
		if (this.cooldown > 0) {
			this.cooldown--;
			return false;
		}

		if (!this.whippet.isTamed() || this.whippet.isSitting() || this.whippet.isRacing() || this.whippet.isBaby() && this.whippet.getRandom().nextInt(2) == 0) {
			return false;
		}

		if (!this.whippet.wantsToBurrow()) {
			return false;
		}

		this.bedding = this.findBedding();
		return this.bedding != null;
	}

	@Override
	public boolean shouldContinue() {
		if (this.bedding == null || this.whippet.isSitting() || this.whippet.isRacing()) {
			return false;
		}

		if (!isBedding(this.whippet.getEntityWorld().getBlockState(this.bedding))) {
			return false;
		}

		// Stays in until it has warmed through, or until it is hauled out.
		return this.whippet.isBurrowed() ? this.whippet.wantsToBurrow() : this.approachTicks < APPROACH_LIMIT;
	}

	@Override
	public void start() {
		this.approachTicks = 0;
		this.repathIn = 0;
		this.whippet.setInSittingPose(false);
	}

	@Override
	public void stop() {
		this.whippet.setBurrowed(false);
		this.whippet.setCurled(false);
		this.whippet.getNavigation().stop();
		this.bedding = null;
		this.cooldown = this.whippet.getRandom().nextBetween(MIN_COOLDOWN, MAX_COOLDOWN);
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		BlockPos bed = this.bedding;

		if (bed == null) {
			return;
		}

		if (this.whippet.isBurrowed()) {
			this.whippet.getNavigation().stop();
			this.whippet.warmUp(null);
			return;
		}

		this.approachTicks++;
		this.whippet.getLookControl().lookAt(bed.getX() + 0.5, bed.getY() + 1.0, bed.getZ() + 0.5);

		double dx = this.whippet.getX() - (bed.getX() + 0.5);
		double dz = this.whippet.getZ() - (bed.getZ() + 0.5);
		double flat = dx * dx + dz * dz;

		if (flat <= ARRIVED * ARRIVED || this.whippet.getNavigation().isIdle() && flat <= (ARRIVED + 1.0) * (ARRIVED + 1.0)) {
			this.whippet.burrowInto(bed);
			return;
		}

		if (this.repathIn-- <= 0) {
			this.repathIn = 20;
			this.whippet.getNavigation().startMovingTo(bed.getX() + 0.5, bed.getY(), bed.getZ() + 0.5, SPEED);
		}
	}

	/** Beds first, then anything woolly — whippets are not fussy about whose. */
	private @Nullable BlockPos findBedding() {
		World world = this.whippet.getEntityWorld();
		BlockPos origin = this.whippet.getBlockPos();
		Optional<BlockPos> bed = BlockPos.findClosest(origin, SEARCH_RADIUS, SEARCH_HEIGHT, pos -> world.getBlockState(pos).isIn(BlockTags.BEDS));

		if (bed.isPresent()) {
			return bed.get().toImmutable();
		}

		return BlockPos.findClosest(origin, SEARCH_RADIUS, SEARCH_HEIGHT, pos -> isSoft(world.getBlockState(pos)))
			.map(BlockPos::toImmutable)
			.orElse(null);
	}

	private static boolean isBedding(BlockState state) {
		return state.isIn(BlockTags.BEDS) || isSoft(state);
	}

	private static boolean isSoft(BlockState state) {
		return state.isIn(BlockTags.WOOL) || state.isIn(BlockTags.WOOL_CARPETS);
	}
}
