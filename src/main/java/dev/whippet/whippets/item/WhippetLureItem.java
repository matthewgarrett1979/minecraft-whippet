package dev.whippet.whippets.item;

import dev.whippet.whippets.entity.WhippetEntity;
import dev.whippet.whippets.race.RaceManager;
import java.util.List;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * The lure. Peg it somewhere with a right-click on a block, then right-click in
 * the air to call your whippets to the traps: they line up abreast, wait out a
 * three-count, and run at it the moment the bell goes.
 */
public class WhippetLureItem extends Item {
	private static final int COOLDOWN_TICKS = 40;
	/** How far a pegged lure will fall looking for ground to sit on. */
	private static final int MAX_DROP = 16;

	public WhippetLureItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		PlayerEntity player = context.getPlayer();

		if (player == null) {
			return ActionResult.PASS;
		}

		if (context.getWorld() instanceof ServerWorld world && player instanceof ServerPlayerEntity serverPlayer) {
			BlockPos pos = groundUnder(world, context.getBlockPos().offset(context.getSide()));
			Vec3d lure = Vec3d.ofBottomCenter(pos);
			RaceManager.setLure(serverPlayer, lure);
			world.spawnParticles(ParticleTypes.END_ROD, lure.x, lure.y + 0.3, lure.z, 20, 0.2, 0.3, 0.2, 0.01);
			world.playSound(null, pos, SoundEvents.BLOCK_WOOL_PLACE, SoundCategory.PLAYERS, 0.8F, 1.4F);
			serverPlayer.sendMessage(Text.translatable("item.whippets.whippet_lure.pegged").formatted(Formatting.YELLOW), true);
		}

		return ActionResult.SUCCESS;
	}

	/**
	 * Drops the lure to the ground under wherever it was pegged. Pegged on the
	 * side of a ledge or a wall it would otherwise hang in the air, and a lure
	 * the dogs cannot stand on is a race nobody can win: they arrive underneath
	 * it, mill about and never finish. Standing on the ground it is on the same
	 * level as the dogs running at it.
	 */
	private static BlockPos groundUnder(ServerWorld world, BlockPos pegged) {
		BlockPos pos = pegged;

		for (int drop = 0; drop < MAX_DROP; drop++) {
			BlockPos below = pos.down();

			if (!world.getBlockState(below).getCollisionShape(world, below).isEmpty()) {
				return pos;
			}

			pos = below;
		}

		// Pegged over a ravine or the void: leave it where they put it.
		return pegged;
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		if (!(world instanceof ServerWorld serverWorld) || !(user instanceof ServerPlayerEntity player)) {
			return ActionResult.SUCCESS;
		}

		user.getItemCooldownManager().set(user.getStackInHand(hand), COOLDOWN_TICKS);
		Vec3d lure = RaceManager.getLure(player);

		if (lure == null) {
			player.sendMessage(Text.translatable("item.whippets.whippet_lure.no_lure").formatted(Formatting.RED), true);
			return ActionResult.SUCCESS;
		}

		if (RaceManager.isRacing(player)) {
			player.sendMessage(Text.translatable("item.whippets.whippet_lure.already_racing").formatted(Formatting.RED), true);
			return ActionResult.SUCCESS;
		}

		Vec3d start = user.getEntityPos();

		if (start.squaredDistanceTo(lure) > RaceManager.MAX_TRACK * RaceManager.MAX_TRACK) {
			player.sendMessage(Text.translatable("item.whippets.whippet_lure.too_far").formatted(Formatting.RED), true);
			return ActionResult.SUCCESS;
		}

		List<WhippetEntity> pack = serverWorld.getEntitiesByClass(
			WhippetEntity.class,
			user.getBoundingBox().expand(RaceManager.CALL_RANGE),
			whippet -> whippet.isTamed() && whippet.isOwner(user) && whippet.isAlive() && !whippet.isBaby() && !whippet.isLeashed()
		);

		if (pack.isEmpty()) {
			player.sendMessage(Text.translatable("item.whippets.whippet_lure.no_dogs").formatted(Formatting.RED), true);
			return ActionResult.SUCCESS;
		}

		RaceManager.start(player, serverWorld, start, lure, pack);
		player.sendMessage(Text.translatable("item.whippets.whippet_lure.lining_up", pack.size()).formatted(Formatting.YELLOW), false);
		return ActionResult.SUCCESS;
	}
}
