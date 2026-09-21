package dev.whippet.whippets.entity.ai;

import dev.whippet.whippets.entity.WhippetEntity;
import java.util.EnumSet;
import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * The duvet instinct. A cold whippet goes looking for bedding — a bed, or
 * failing that anything woollen — climbs in and disappears under it.
 *
 * <p>Three house rules, all of them taken from life. One dog to a bed: they do
 * not share, and a whippet that finds another one already in there goes and
 * finds its own. If you are asleep in it, that is your problem — it gets in
 * anyway and you are woken up, which is how it works in a house with a whippet
 * in it. And it never stays long: a minute at the outside, and then it is
 * suddenly and urgently somewhere else.
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
	/**
	 * The longest a whippet stays under the covers before it is overcome by the
	 * conviction that something is happening elsewhere. One minute.
	 */
	private static final int MAX_KIP = 20 * 60;
	/** Longer before it tries again after a full kip, so it is not in and out. */
	private static final int SETTLED_COOLDOWN = 20 * 40;
	/** How wide to look for a dog already in the bed, covering both halves of it. */
	private static final double BED_IS_TAKEN = 2.2;
	/** How far off another whippet can be and still have called that bed. */
	private static final double CLAIMS_CARRY = 24.0;

	private final WhippetEntity whippet;
	private @Nullable BlockPos bedding;
	private int cooldown;
	private int approachTicks;
	private int repathIn;
	/** Ticks spent under the covers this time. */
	private int kipTicks;

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
		this.whippet.claimBedding(this.bedding);
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

		// Stays in until it has had enough, which is never more than a minute.
		return this.whippet.isBurrowed()
			? this.kipTicks < MAX_KIP && this.whippet.wantsToBurrow()
			: this.approachTicks < APPROACH_LIMIT;
	}

	@Override
	public void start() {
		this.approachTicks = 0;
		this.repathIn = 0;
		this.kipTicks = 0;
		this.whippet.setInSittingPose(false);
	}

	@Override
	public void stop() {
		boolean slept = this.whippet.isBurrowed();
		this.whippet.setBurrowed(false);
		this.whippet.setCurled(false);
		this.whippet.getNavigation().stop();

		if (slept) {
			// Out in one movement, which is the only way a whippet leaves a bed.
			this.whippet.jumpOut();
		}

		this.bedding = null;
		this.whippet.claimBedding(null);
		this.cooldown = slept
			? SETTLED_COOLDOWN + this.whippet.getRandom().nextInt(SETTLED_COOLDOWN)
			: this.whippet.getRandom().nextBetween(MIN_COOLDOWN, MAX_COOLDOWN);
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
			this.kipTicks++;
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
			// Somebody in it is not an obstacle, it is a detail.
			this.whippet.turnOutSleeper(bed);
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
		Optional<BlockPos> bed = BlockPos.findClosest(
			origin, SEARCH_RADIUS, SEARCH_HEIGHT, pos -> world.getBlockState(pos).isIn(BlockTags.BEDS) && !this.alreadyTaken(pos)
		);

		if (bed.isPresent()) {
			return bed.get().toImmutable();
		}

		return BlockPos.findClosest(origin, SEARCH_RADIUS, SEARCH_HEIGHT, pos -> isSoft(world.getBlockState(pos)) && !this.alreadyTaken(pos))
			.map(BlockPos::toImmutable)
			.orElse(null);
	}

	/**
	 * One dog to a bed. They will not share and it is no use suggesting it — and
	 * a bed another whippet is merely walking towards is taken as well, or two
	 * of them set off for the same one and arrive in it together.
	 */
	private boolean alreadyTaken(BlockPos bedding) {
		Box around = new Box(bedding).expand(CLAIMS_CARRY);

		for (WhippetEntity other : this.whippet.getEntityWorld().getEntitiesByClass(WhippetEntity.class, around, dog -> dog != this.whippet)) {
			BlockPos claim = other.getBeddingClaim();

			if (claim != null && claim.isWithinDistance(bedding, BED_IS_TAKEN)) {
				return true;
			}

			if (other.isBurrowed() && other.getBlockPos().isWithinDistance(bedding, BED_IS_TAKEN)) {
				return true;
			}
		}

		return false;
	}

	private static boolean isBedding(BlockState state) {
		return state.isIn(BlockTags.BEDS) || isSoft(state);
	}

	private static boolean isSoft(BlockState state) {
		return state.isIn(BlockTags.WOOL) || state.isIn(BlockTags.WOOL_CARPETS);
	}
}
