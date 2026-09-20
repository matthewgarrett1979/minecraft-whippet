package dev.whippet.whippets.item;

import dev.whippet.whippets.entity.WhippetEntity;
import java.util.List;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * Blow it and every whippet you own within earshot stands up, stops whatever it
 * was doing and comes back to you — which is more recall than a real one offers.
 * Crouch and blow it and you do the opposite: the whole pack is slipped, and
 * goes flat out until it runs out of breath.
 */
public class WhippetWhistleItem extends Item {
	private static final double RANGE = 48.0;
	private static final int COOLDOWN_TICKS = 100;

	public WhippetWhistleItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		// Two notes: a high one to bring them in, a short low one to let them go.
		boolean slipping = user.isSneaking();
		world.playSound(
			null,
			user.getX(),
			user.getY(),
			user.getZ(),
			SoundEvents.BLOCK_NOTE_BLOCK_FLUTE.value(),
			SoundCategory.PLAYERS,
			0.9F,
			slipping ? 1.2F : 1.8F
		);
		user.getItemCooldownManager().set(stack, COOLDOWN_TICKS);

		if (world instanceof ServerWorld serverWorld) {
			List<WhippetEntity> pack = serverWorld.getEntitiesByClass(
				WhippetEntity.class, user.getBoundingBox().expand(RANGE), whippet -> whippet.isTamed() && whippet.isOwner(user)
			);

			if (slipping) {
				int slipped = 0;

				for (WhippetEntity whippet : pack) {
					if (whippet.slip()) {
						slipped++;
					}
				}

				user.sendMessage(
					slipped == 0
						? Text.translatable("item.whippets.whippet_whistle.nothing_left")
						: Text.translatable("item.whippets.whippet_whistle.slipped", slipped),
					true
				);
				return ActionResult.SUCCESS;
			}

			int recalled = 0;

			for (WhippetEntity whippet : pack) {
				whippet.setSitting(false);
				whippet.setInSittingPose(false);
				whippet.clearComfort();
				whippet.setZooming(false);
				whippet.setTarget(null);
				whippet.getNavigation().stop();

				if (whippet.squaredDistanceTo(user) > 12.0 * 12.0) {
					whippet.tryTeleportToOwner();
				}

				recalled++;
			}

			user.sendMessage(
				recalled == 0
					? Text.translatable("item.whippets.whippet_whistle.nobody")
					: Text.translatable("item.whippets.whippet_whistle.recalled", recalled),
				true
			);
		}

		return ActionResult.SUCCESS;
	}
}
