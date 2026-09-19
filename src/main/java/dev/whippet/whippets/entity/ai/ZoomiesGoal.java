package dev.whippet.whippets.entity.ai;

import dev.whippet.whippets.entity.WhippetEntity;
import java.util.EnumSet;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.passive.WolfSoundVariants;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

/**
 * The zoomies. Every so often a whippet detonates: three or four laps at full
 * stretch, wide arcs around its owner if it has one, and then it flops down
 * exactly where it stopped and refuses to move for a while.
 */
public class ZoomiesGoal extends Goal {
	private static final int MIN_COOLDOWN = 1200;
	private static final int MAX_COOLDOWN = 3600;
	private static final int MIN_RUN_TICKS = 100;
	private static final int MAX_RUN_TICKS = 200;
	private static final int MIN_FLOP_TICKS = 60;
	private static final int MAX_FLOP_TICKS = 140;
	private static final double SPRINT_SPEED = 1.6;
	/** How far out the arcs swing, in blocks. */
	private static final double ARC_RADIUS = 9.0;

	private final WhippetEntity whippet;
	private int cooldown;
	private int runTicks;
	private int flopTicks;
	private int legTurn;
	private double targetX;
	private double targetY;
	private double targetZ;

	public ZoomiesGoal(WhippetEntity whippet) {
		this.whippet = whippet;
		this.cooldown = whippet.getRandom().nextBetween(MIN_COOLDOWN / 2, MAX_COOLDOWN);
		this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK, Goal.Control.JUMP));
	}

	@Override
	public boolean canStart() {
		if (this.cooldown > 0) {
			this.cooldown--;
			return false;
		}

		if (this.whippet.isSitting() || this.whippet.isBaby() && this.whippet.getRandom().nextInt(2) == 0) {
			return false;
		}

		if (!this.whippet.isOnGround() || this.whippet.getTarget() != null || this.whippet.isLeashed() || this.whippet.hasVehicle()) {
			return false;
		}

		// Whippets don't do this in the rain, and they don't do it in deep water.
		if (this.whippet.isTouchingWaterOrRain()) {
			return false;
		}

		return this.pickTarget();
	}

	@Override
	public boolean shouldContinue() {
		return this.runTicks > 0 || this.flopTicks > 0;
	}

	@Override
	public void start() {
		this.runTicks = this.whippet.getRandom().nextBetween(MIN_RUN_TICKS, MAX_RUN_TICKS);
		this.flopTicks = 0;
		this.legTurn = 0;
		this.whippet.setZooming(true);
		this.whippet.setInSittingPose(false);
		this.whippet.getNavigation().startMovingTo(this.targetX, this.targetY, this.targetZ, SPRINT_SPEED);
	}

	@Override
	public void stop() {
		this.whippet.setZooming(false);
		this.whippet.setInSittingPose(false);
		this.whippet.getNavigation().stop();
		this.runTicks = 0;
		this.flopTicks = 0;
		this.cooldown = this.whippet.getRandom().nextBetween(MIN_COOLDOWN, MAX_COOLDOWN);
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (this.runTicks > 0) {
			this.runTicks--;

			if (this.whippet.getNavigation().isIdle() || this.whippet.squaredDistanceTo(this.targetX, this.targetY, this.targetZ) < 4.0) {
				this.legTurn++;

				if (this.pickTarget()) {
					this.whippet.getNavigation().startMovingTo(this.targetX, this.targetY, this.targetZ, SPRINT_SPEED);
				} else {
					this.runTicks = 0;
				}
			}

			this.whippet.getLookControl().lookAt(this.targetX, this.targetY + this.whippet.getStandingEyeHeight(), this.targetZ);

			if (this.runTicks == 0) {
				// Out of puff: flop where you stand.
				this.flopTicks = this.whippet.getRandom().nextBetween(MIN_FLOP_TICKS, MAX_FLOP_TICKS);
				this.whippet.setZooming(false);
				this.whippet.getNavigation().stop();
				this.whippet.setInSittingPose(true);
				this.whippet.playSound(SoundEvents.WOLF_SOUNDS.get(WolfSoundVariants.Type.SAD).whineSound().value(), 0.25F, 1.5F);
			}
		} else if (this.flopTicks > 0) {
			this.flopTicks--;
			this.whippet.getNavigation().stop();
		}
	}

	/**
	 * Picks the next corner of the lap: a point on a wide arc around the owner if
	 * the whippet has one nearby, otherwise anywhere it can reach at speed.
	 */
	private boolean pickTarget() {
		LivingEntity owner = this.whippet.getOwner();
		Vec3d centre = owner != null && owner.isAlive() && this.whippet.squaredDistanceTo(owner) < 32.0 * 32.0
			? owner.getEntityPos()
			: this.whippet.getEntityPos();
		double angle = this.whippet.getRandom().nextDouble() * Math.PI * 2.0 + this.legTurn * 2.1;
		double radius = ARC_RADIUS * (0.5 + this.whippet.getRandom().nextDouble() * 0.5);
		double x = centre.getX() + MathHelper.sin((float)angle) * radius;
		double z = centre.getZ() + MathHelper.cos((float)angle) * radius;
		@Nullable Path path = this.whippet.getNavigation().findPathTo(x, centre.getY(), z, 1);

		if (path == null || !path.reachesTarget()) {
			return false;
		}

		this.targetX = x;
		this.targetY = centre.getY();
		this.targetZ = z;
		return true;
	}
}
