package dev.whippet.whippets.entity.ai;

import dev.whippet.whippets.entity.WhippetEntity;
import java.util.EnumSet;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

/**
 * Whippets are heat-seeking. Sit still anywhere near one and it will come and
 * press itself against you, curl up, and stay there until you move.
 */
public class CuddleGoal extends Goal {
	private static final double SEEK_RANGE = 16.0;
	private static final double CUDDLE_RANGE = 2.2;
	private static final double SPEED = 1.0;
	/** How long the owner has to keep still before it counts as an invitation. */
	private static final int SETTLED_TICKS = 40;
	private static final int MIN_COOLDOWN = 100;
	private static final int MAX_COOLDOWN = 400;

	private final WhippetEntity whippet;
	private @org.jspecify.annotations.Nullable PlayerEntity owner;
	private int settled;
	private int cooldown;
	private int repathIn;

	public CuddleGoal(WhippetEntity whippet) {
		this.whippet = whippet;
		this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		if (this.cooldown > 0) {
			this.cooldown--;
			return false;
		}

		if (!this.whippet.isTamed() || this.whippet.isSitting() || this.whippet.isRacing() || this.whippet.isZooming()) {
			return false;
		}

		LivingEntity owner = this.whippet.getOwner();

		if (!(owner instanceof PlayerEntity player) || !player.isAlive() || player.isSpectator()) {
			this.settled = 0;
			return false;
		}

		if (this.whippet.squaredDistanceTo(player) > SEEK_RANGE * SEEK_RANGE) {
			this.settled = 0;
			return false;
		}

		// Sitting still, sneaking or asleep all read as an invitation.
		boolean inviting = player.isSleeping() || player.isSneaking() || player.getVelocity().horizontalLengthSquared() < 0.002;
		this.settled = inviting ? this.settled + 1 : 0;

		if (this.settled < SETTLED_TICKS) {
			return false;
		}

		this.owner = player;
		return true;
	}

	@Override
	public boolean shouldContinue() {
		PlayerEntity player = this.owner;

		if (player == null || !player.isAlive() || this.whippet.isSitting() || this.whippet.isRacing()) {
			return false;
		}

		if (this.whippet.squaredDistanceTo(player) > SEEK_RANGE * SEEK_RANGE) {
			return false;
		}

		// Gets up the moment its cushion walks off.
		return player.isSleeping() || player.isSneaking() || player.getVelocity().horizontalLengthSquared() < 0.01;
	}

	@Override
	public void start() {
		this.repathIn = 0;
	}

	@Override
	public void stop() {
		this.whippet.setCurled(false);
		this.whippet.getNavigation().stop();
		this.owner = null;
		this.settled = 0;
		this.cooldown = this.whippet.getRandom().nextBetween(MIN_COOLDOWN, MAX_COOLDOWN);
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		PlayerEntity player = this.owner;

		if (player == null) {
			return;
		}

		this.whippet.getLookControl().lookAt(player, 10.0F, this.whippet.getMaxLookPitchChange());

		if (this.whippet.squaredDistanceTo(player) > CUDDLE_RANGE * CUDDLE_RANGE) {
			this.whippet.setCurled(false);

			if (this.repathIn-- <= 0) {
				this.repathIn = 10;
				this.whippet.getNavigation().startMovingTo(player, SPEED);
			}

			return;
		}

		// Close enough to lean on: curl up against them and stay put.
		this.whippet.getNavigation().stop();
		this.whippet.setCurled(true);
		this.whippet.warmUp(player);

		if (this.whippet.getEntityWorld() instanceof ServerWorld world && this.whippet.age % 60 == 0) {
			world.spawnParticles(
				ParticleTypes.HEART, this.whippet.getX(), this.whippet.getY() + 0.7, this.whippet.getZ(), 1, 0.3, 0.2, 0.3, 0.0
			);
		}
	}
}
