package dev.whippet.whippets.entity;

import dev.whippet.whippets.ModItems;
import dev.whippet.whippets.race.RaceManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * The Guv'nor: the man who runs the stadium.
 *
 * <p>He stands by the line in a flat cap, he does not race and he does not
 * fight. What he does is put on a meeting. Bring your dogs, ask him, and he
 * pegs the lure at the finish, puts his own dog in against yours and starts the
 * card. If one of yours wins he pays out; if you beat Bobby Brazil he hands
 * over the trophy, which does not happen often.
 */
public class GuvnorEntity extends PathAwareEntity {
	/** How far he will look for your dogs when you ask him for a race. */
	private static final double FIELD_RANGE = 40.0;
	/** How far he will look for his own dog to put in against them. */
	private static final double CHAMPION_RANGE = 90.0;
	/**
	 * How far from the line he will stray. Small on purpose: the first one of
	 * these walked up into the stand, fell off the front of the terrace and
	 * killed himself, which left a stadium with nobody to run it.
	 */
	private static final int HIS_PATCH = 8;
	/** The purse for winning an ordinary race here. */
	private static final int PURSE = 3;
	/** And for beating the champion, which is another thing entirely. */
	private static final int CHAMPION_PURSE = 12;

	/** His ground: the line the dogs run to and the traps they come out of. */
	private @Nullable BlockPos finish;
	private @Nullable BlockPos traps;

	public GuvnorEntity(EntityType<? extends GuvnorEntity> entityType, World world) {
		super(entityType, world);
		this.setPersistent();
	}

	public static DefaultAttributeContainer.Builder createGuvnorAttributes() {
		return MobEntity.createMobAttributes()
			.add(EntityAttributes.MAX_HEALTH, 40.0)
			.add(EntityAttributes.MOVEMENT_SPEED, 0.28)
			.add(EntityAttributes.KNOCKBACK_RESISTANCE, 0.8)
			.add(EntityAttributes.FOLLOW_RANGE, 32.0);
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(0, new SwimGoal(this));
		// He does not go far. He has been stood in that spot for years.
		this.goalSelector.add(1, new WanderAroundGoal(this, 0.45, 80));
		this.goalSelector.add(2, new LookAtEntityGoal(this, PlayerEntity.class, 10.0F));
		this.goalSelector.add(3, new LookAroundGoal(this));
	}

	/** Told where his stadium is when the structure puts him in it. */
	public void takeCharge(BlockPos finish, BlockPos traps) {
		this.finish = finish;
		this.traps = traps;
		this.setPositionTarget(finish, HIS_PATCH);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);

		if (this.finish != null) {
			view.put("Finish", BlockPos.CODEC, this.finish);
		}

		if (this.traps != null) {
			view.put("Traps", BlockPos.CODEC, this.traps);
		}
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		this.finish = view.read("Finish", BlockPos.CODEC).orElse(null);
		this.traps = view.read("Traps", BlockPos.CODEC).orElse(null);

		if (this.finish != null) {
			this.setPositionTarget(this.finish, HIS_PATCH);
		}
	}

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		if (!(this.getEntityWorld() instanceof ServerWorld world) || !(player instanceof ServerPlayerEntity racer)) {
			return ActionResult.SUCCESS;
		}

		if (this.finish == null || this.traps == null) {
			this.say(racer, "no_ground");
			return ActionResult.SUCCESS_SERVER;
		}

		if (RaceManager.isRacing(racer)) {
			this.say(racer, "already_racing");
			return ActionResult.SUCCESS_SERVER;
		}

		List<WhippetEntity> field = this.callTheField(world, racer);

		if (field.isEmpty()) {
			this.say(racer, "no_dogs");
			return ActionResult.SUCCESS_SERVER;
		}

		int entered = field.size();
		WhippetEntity champion = this.findChampion(world);

		if (champion != null) {
			field.add(champion);
		}

		Vec3d lure = Vec3d.ofBottomCenter(this.finish);
		RaceManager.setLure(racer, lure);
		RaceManager.start(racer, world, Vec3d.ofBottomCenter(this.traps), lure, field, this);
		racer.sendMessage(
			Text.translatable("entity.whippets.guvnor.card", entered, champion == null ? 0 : 1).formatted(Formatting.GOLD), false
		);
		this.playSound(SoundEvents.ENTITY_VILLAGER_YES, 1.0F, 0.9F);
		return ActionResult.SUCCESS_SERVER;
	}

	/** Every tamed dog of theirs that is fit to run. */
	private List<WhippetEntity> callTheField(ServerWorld world, ServerPlayerEntity racer) {
		Box around = this.getBoundingBox().expand(FIELD_RANGE);
		List<WhippetEntity> field = new ArrayList<>();

		for (WhippetEntity dog : world.getEntitiesByClass(WhippetEntity.class, around, dog -> dog.isTamed() && dog.isOwner(racer))) {
			if (dog.isAlive() && !dog.isBaby() && !dog.isLeashed()) {
				field.add(dog);
			}
		}

		return field;
	}

	/** His own dog, if he is anywhere about. */
	private @Nullable WhippetEntity findChampion(ServerWorld world) {
		for (WhippetEntity dog : world.getEntitiesByClass(WhippetEntity.class, this.getBoundingBox().expand(CHAMPION_RANGE), WhippetEntity::isChampion)) {
			if (dog.isAlive()) {
				return dog;
			}
		}

		return null;
	}

	/**
	 * Settling up. An ordinary win is worth a few emeralds; beating the
	 * champion is worth the trophy, and he does not mind saying so.
	 */
	public void payOut(ServerPlayerEntity racer, WhippetEntity winner, boolean championBeaten) {
		if (winner.isChampion()) {
			racer.sendMessage(Text.translatable("entity.whippets.guvnor.champion_wins", winner.getRaceName()).formatted(Formatting.GRAY), false);
			this.playSound(SoundEvents.ENTITY_VILLAGER_AMBIENT, 1.0F, 0.8F);
			return;
		}

		int purse = championBeaten ? CHAMPION_PURSE : PURSE;
		this.give(racer, new ItemStack(Items.EMERALD, purse));

		if (championBeaten) {
			this.give(racer, new ItemStack(ModItems.RACING_TROPHY));
			racer.sendMessage(
				Text.translatable("entity.whippets.guvnor.trophy", winner.getRaceName()).formatted(Formatting.GOLD), false
			);
		} else {
			racer.sendMessage(Text.translatable("entity.whippets.guvnor.purse", winner.getRaceName(), purse).formatted(Formatting.YELLOW), false);
		}

		this.playSound(SoundEvents.ENTITY_VILLAGER_CELEBRATE, 1.0F, 1.0F);
	}

	private void give(ServerPlayerEntity racer, ItemStack stack) {
		if (!racer.giveItemStack(stack)) {
			racer.dropItem(stack, false);
		}
	}

	private void say(ServerPlayerEntity racer, String what) {
		racer.sendMessage(Text.translatable("entity.whippets.guvnor." + what).formatted(Formatting.GRAY), true);
		this.playSound(SoundEvents.ENTITY_VILLAGER_NO, 0.8F, 0.9F);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_VILLAGER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_VILLAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_VILLAGER_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, net.minecraft.block.BlockState state) {
		this.playSound(SoundEvents.ENTITY_PLAYER_BIG_FALL, 0.05F, 1.6F);
	}

	/** And he does not come to any harm stepping off his own box. */
	@Override
	public boolean handleFallDamage(double fallDistance, float damagePerDistance, DamageSource damageSource) {
		return false;
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}
}
