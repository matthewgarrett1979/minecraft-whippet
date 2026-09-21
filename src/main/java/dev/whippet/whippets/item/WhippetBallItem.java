package dev.whippet.whippets.item;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * A ball. Right-click to throw it and every whippet that sees it go stops
 * whatever it was doing — this is not a decision any of them make.
 *
 * <p>It is thrown as an ordinary dropped item, so it bounces, rolls, floats and
 * can be picked back up by hand; what makes it a ball rather than litter is
 * that the dogs know what it is.
 */
public class WhippetBallItem extends Item {
	/** How hard it goes. A whippet will catch it either way. */
	private static final double THROW_POWER = 1.35;
	private static final double LIFT = 0.18;
	/** Ticks before the thrower can pick it up again, so it is not caught on release. */
	private static final int PICKUP_DELAY = 30;

	public WhippetBallItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);

		world.playSound(
			null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.PLAYERS, 0.6F, 1.6F
		);
		user.getItemCooldownManager().set(stack, 10);

		if (world instanceof ServerWorld serverWorld) {
			Vec3d aim = user.getRotationVector().normalize();
			ItemEntity ball = new ItemEntity(
				serverWorld,
				user.getX(),
				user.getEyeY() - 0.2,
				user.getZ(),
				stack.copyWithCount(1),
				aim.x * THROW_POWER,
				aim.y * THROW_POWER + LIFT,
				aim.z * THROW_POWER
			);
			ball.setPickupDelay(PICKUP_DELAY);
			ball.setThrower(user);
			serverWorld.spawnEntity(ball);
			stack.decrementUnlessCreative(1, user);
		}

		return ActionResult.SUCCESS;
	}
}
