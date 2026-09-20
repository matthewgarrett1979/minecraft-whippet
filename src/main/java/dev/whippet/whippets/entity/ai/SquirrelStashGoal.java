package dev.whippet.whippets.entity.ai;

import dev.whippet.whippets.ModTags;
import dev.whippet.whippets.entity.SquirrelEntity;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

/**
 * Thieving and burying, which between them account for most of a squirrel's
 * waking life. Anything edible lying on the ground gets picked up, carried off
 * a sensible distance and buried — and no, it will not remember where.
 */
public class SquirrelStashGoal extends Goal {
	private static final double SEARCH = 8.0;
	private static final double TROT = 1.25;
	/** How far off it carries the prize before digging. */
	private static final double CARRY_DISTANCE = 6.0;
	private static final int DIG_TICKS = 40;

	private final SquirrelEntity squirrel;
	private @Nullable ItemEntity loot;
	private @Nullable Vec3d stash;
	private int digging;

	public SquirrelStashGoal(SquirrelEntity squirrel) {
		this.squirrel = squirrel;
		this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		if (this.squirrel.isBaby()) {
			return false;
		}

		if (this.squirrel.isCarrying()) {
			this.pickStash();
			return this.stash != null;
		}

		this.loot = this.nearestLoot();
		return this.loot != null;
	}

	@Override
	public boolean shouldContinue() {
		if (this.squirrel.isCarrying()) {
			return this.stash != null;
		}

		return this.loot != null && this.loot.isAlive();
	}

	@Override
	public void stop() {
		this.loot = null;
		this.stash = null;
		this.digging = 0;
		this.squirrel.getNavigation().stop();
	}

	@Override
	public void tick() {
		if (!this.squirrel.isCarrying()) {
			this.fetch();
			return;
		}

		this.bury();
	}

	/** Trot over to the dropped food and take it. */
	private void fetch() {
		if (this.loot == null) {
			return;
		}

		this.squirrel.getLookControl().lookAt(this.loot);

		// Re-pathing every tick makes it dither on the spot a foot short of the
		// prize, which is maddening to watch.
		if (this.squirrel.getNavigation().isIdle() || this.squirrel.age % 10 == 0) {
			this.squirrel.getNavigation().startMovingTo(this.loot, TROT);
		}

		if (this.squirrel.squaredDistanceTo(this.loot) > 2.25) {
			return;
		}

		this.loot.getStack().decrement(1);

		if (this.loot.getStack().isEmpty()) {
			this.loot.discard();
		}

		this.squirrel.setCarrying(true);
		this.squirrel.playSound(SoundEvents.ENTITY_FOX_EAT, 0.4F, 1.8F);
		this.loot = null;
		this.pickStash();
	}

	/** Carry it off, dig, drop it in and pat the ground down over it. */
	private void bury() {
		if (this.stash == null) {
			return;
		}

		if (this.squirrel.getEntityPos().squaredDistanceTo(this.stash) > 1.5 && this.digging == 0) {
			this.squirrel.getNavigation().startMovingTo(this.stash.x, this.stash.y, this.stash.z, TROT);

			if (this.squirrel.getNavigation().isIdle()) {
				// Cannot get there; here will do. It always does.
				this.stash = this.squirrel.getEntityPos();
			}

			return;
		}

		this.squirrel.getNavigation().stop();
		this.digging++;

		if (this.squirrel.getEntityWorld() instanceof ServerWorld world && this.digging % 4 == 0) {
			BlockPos below = this.squirrel.getBlockPos().down();
			world.spawnParticles(
				new BlockStateParticleEffect(ParticleTypes.BLOCK, world.getBlockState(below)),
				this.squirrel.getX(),
				this.squirrel.getY() + 0.05,
				this.squirrel.getZ(),
				3,
				0.15,
				0.0,
				0.15,
				0.02
			);
		}

		if (this.digging >= DIG_TICKS) {
			this.squirrel.setCarrying(false);
			this.squirrel.playSound(SoundEvents.ENTITY_FOX_SNIFF, 0.4F, 1.9F);
			this.digging = 0;
			this.stash = null;
		}
	}

	/** Somewhere over there. Squirrels are not systematic about this. */
	private void pickStash() {
		Vec3d here = this.squirrel.getEntityPos();
		double angle = this.squirrel.getRandom().nextDouble() * Math.PI * 2.0;
		this.stash = here.add(Math.cos(angle) * CARRY_DISTANCE, 0.0, Math.sin(angle) * CARRY_DISTANCE);
	}

	private @Nullable ItemEntity nearestLoot() {
		List<ItemEntity> items = this.squirrel
			.getEntityWorld()
			.getEntitiesByClass(
				ItemEntity.class,
				this.squirrel.getBoundingBox().expand(SEARCH),
				item -> item.isAlive() && !item.cannotPickup() && item.getStack().isIn(ModTags.SQUIRREL_FOOD)
			);

		ItemEntity closest = null;
		double best = Double.MAX_VALUE;

		for (ItemEntity item : items) {
			double distance = this.squirrel.squaredDistanceTo(item);

			if (distance < best) {
				best = distance;
				closest = item;
			}
		}

		return closest;
	}
}
