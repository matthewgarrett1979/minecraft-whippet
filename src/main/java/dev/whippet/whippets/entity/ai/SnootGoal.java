package dev.whippet.whippets.entity.ai;

import dev.whippet.whippets.entity.WhippetEntity;
import java.util.EnumSet;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

/**
 * A hungry whippet does not bark at you about it. It walks up behind you, where
 * you cannot see it coming, and puts its nose against the back of your leg — a
 * cold, deliberate, unmistakable tap — and then stands there. If that does not
 * work it does it again a few seconds later, and it will keep doing it until
 * you feed it, which is the intended outcome and always works.
 */
public class SnootGoal extends Goal {
	private static final double RANGE = 16.0;
	private static final double SPEED = 1.1;
	/** Close enough for a nose to reach a leg. */
	private static final double REACH = 1.6;
	/** How far behind the owner it aims for. */
	private static final double BEHIND = 0.9;
	/** Inside this, walking straight at them will not work any more. */
	private static final double ROUND_THE_OUTSIDE = 2.8;
	/** How far round the outside it swings on each step, in radians. */
	private static final double ARC = 0.9;
	/** After this long it gives up on doing it properly and taps anyway. */
	private static final int STOPS_BEING_FUSSY = 160;
	/** Ticks between attempts, so it nags rather than pesters. */
	private static final int BETWEEN_TAPS = 160;
	private static final int GIVE_UP = 300;

	private final WhippetEntity whippet;
	private @Nullable PlayerEntity owner;
	private int cooldown;
	private int ticks;
	private boolean tapped;

	public SnootGoal(WhippetEntity whippet) {
		this.whippet = whippet;
		this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		if (this.cooldown > 0) {
			this.cooldown--;
			return false;
		}

		if (!this.whippet.isHungry() || this.whippet.isRacing() || this.whippet.isBurrowed() || this.whippet.isInSittingPose()) {
			return false;
		}

		LivingEntity owner = this.whippet.getOwner();

		if (!(owner instanceof PlayerEntity player) || !player.isAlive() || player.isSpectator()) {
			return false;
		}

		if (this.whippet.squaredDistanceTo(player) > RANGE * RANGE) {
			return false;
		}

		this.owner = player;
		return true;
	}

	@Override
	public boolean shouldContinue() {
		return this.owner != null && !this.tapped && this.ticks < GIVE_UP && this.whippet.isHungry();
	}

	@Override
	public void start() {
		this.ticks = 0;
		this.tapped = false;
	}

	@Override
	public void stop() {
		this.cooldown = BETWEEN_TAPS + this.whippet.getRandom().nextInt(120);
		this.owner = null;
		this.whippet.getNavigation().stop();
	}

	@Override
	public void tick() {
		if (this.owner == null) {
			return;
		}

		this.ticks++;
		this.whippet.getLookControl().lookAt(this.owner, 30.0F, 30.0F);

		Vec3d facing = this.facing(this.owner);
		Vec3d toDog = this.whippet.getEntityPos().subtract(this.owner.getEntityPos());
		boolean behind = toDog.horizontalLengthSquared() > 1.0E-4 && toDog.normalize().dotProduct(facing) < -0.1;

		// Nose against the back of the leg. This is the entire behaviour — and
		// if it has spent long enough failing to get round the back, it settles
		// for whichever leg is going.
		if ((behind || this.ticks > STOPS_BEING_FUSSY) && this.whippet.squaredDistanceTo(this.owner) <= REACH * REACH) {
			this.whippet.getNavigation().stop();
			this.whippet.snoot();
			this.tapped = true;
			return;
		}

		if (!this.whippet.getNavigation().isIdle()) {
			return;
		}

		Vec3d target = this.approach(facing, toDog);
		this.whippet.getNavigation().startMovingTo(target.x, target.y, target.z, SPEED);
	}

	/**
	 * Where to head next. From a distance, straight for the spot behind them;
	 * once it is close but on the wrong side, one step round the outside, which
	 * it repeats until it is behind. Walking at somebody's front only ends up
	 * pressed against their shins.
	 */
	private Vec3d approach(Vec3d facing, Vec3d toDog) {
		Vec3d ownerPos = this.owner.getEntityPos();

		if (toDog.horizontalLengthSquared() > ROUND_THE_OUTSIDE * ROUND_THE_OUTSIDE) {
			return ownerPos.add(facing.multiply(-BEHIND));
		}

		Vec3d radial = toDog.horizontalLengthSquared() > 1.0E-4 ? toDog.normalize() : facing;
		// Keep circling the same way round rather than dithering across the front.
		double cross = facing.x * radial.z - facing.z * radial.x;
		double turn = cross >= 0.0 ? ARC : -ARC;
		double cos = Math.cos(turn);
		double sin = Math.sin(turn);
		Vec3d stepped = new Vec3d(radial.x * cos - radial.z * sin, 0.0, radial.x * sin + radial.z * cos);
		return ownerPos.add(stepped.multiply(1.25));
	}

	/** Which way the owner is looking, flat, so "behind" means something. */
	private Vec3d facing(PlayerEntity player) {
		float yaw = player.getYaw() * (float)(Math.PI / 180.0);
		return new Vec3d(-MathHelper.sin(yaw), 0.0, MathHelper.cos(yaw));
	}
}
