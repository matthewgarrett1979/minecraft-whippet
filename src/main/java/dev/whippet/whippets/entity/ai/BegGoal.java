package dev.whippet.whippets.entity.ai;

import dev.whippet.whippets.ModTags;
import dev.whippet.whippets.entity.WhippetEntity;
import java.util.EnumSet;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import org.jspecify.annotations.Nullable;

/**
 * Hold food anywhere near a whippet and it will stand in front of you, tilt its
 * head over and whine at you about it. This is most of what they do.
 */
public class BegGoal extends Goal {
	private static final float BEG_DISTANCE = 9.0F;
	private static final double CLOSE_ENOUGH = 3.0;
	private static final double SPEED = 1.0;

	private final WhippetEntity whippet;
	private final ServerWorld world;
	private final TargetPredicate predicate;
	private @Nullable PlayerEntity begFrom;
	private int timer;
	private int whineIn;

	public BegGoal(WhippetEntity whippet) {
		this.whippet = whippet;
		this.world = getServerWorld(whippet);
		this.predicate = TargetPredicate.createNonAttackable().setBaseMaxDistance(BEG_DISTANCE);
		this.setControls(EnumSet.of(Goal.Control.LOOK, Goal.Control.MOVE));
	}

	@Override
	public boolean canStart() {
		if (this.whippet.isRacing() || this.whippet.isBurrowed()) {
			return false;
		}

		this.begFrom = this.world.getClosestPlayer(this.predicate, this.whippet);
		return this.begFrom != null && isHoldingDinner(this.begFrom);
	}

	@Override
	public boolean shouldContinue() {
		PlayerEntity player = this.begFrom;

		if (player == null || !player.isAlive() || this.whippet.squaredDistanceTo(player) > BEG_DISTANCE * BEG_DISTANCE) {
			return false;
		}

		return this.timer > 0 && isHoldingDinner(player);
	}

	@Override
	public void start() {
		this.whippet.setBegging(true);
		this.whippet.setCurled(false);
		this.timer = this.getTickCount(80 + this.whippet.getRandom().nextInt(80));
		this.whineIn = 10;
	}

	@Override
	public void stop() {
		this.whippet.setBegging(false);
		this.whippet.getNavigation().stop();
		this.begFrom = null;
	}

	@Override
	public void tick() {
		PlayerEntity player = this.begFrom;

		if (player == null) {
			return;
		}

		this.timer--;
		this.whippet.getLookControl().lookAt(player.getX(), player.getEyeY(), player.getZ(), 10.0F, this.whippet.getMaxLookPitchChange());

		// Close the gap, then stand there being pitiful.
		if (this.whippet.squaredDistanceTo(player) > CLOSE_ENOUGH * CLOSE_ENOUGH) {
			if (this.whippet.getNavigation().isIdle()) {
				this.whippet.getNavigation().startMovingTo(player, SPEED);
			}
		} else {
			this.whippet.getNavigation().stop();
		}

		if (this.whineIn-- <= 0) {
			this.whineIn = 30 + this.whippet.getRandom().nextInt(40);
			this.whippet.whine();
		}
	}

	private boolean isHoldingDinner(PlayerEntity player) {
		for (Hand hand : Hand.values()) {
			ItemStack stack = player.getStackInHand(hand);

			if (stack.isIn(ModTags.WHIPPET_FOOD)) {
				return true;
			}
		}

		return false;
	}
}
