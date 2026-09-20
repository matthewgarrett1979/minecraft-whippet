package dev.whippet.whippets.entity;

import dev.whippet.whippets.ModEntities;
import dev.whippet.whippets.ModTags;
import dev.whippet.whippets.Whippets;
import dev.whippet.whippets.entity.ai.RaceGoal;
import dev.whippet.whippets.entity.ai.ZoomiesGoal;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.AnimalMateGoal;
import net.minecraft.entity.ai.goal.AttackWithOwnerGoal;
import net.minecraft.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.PounceAtTargetGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SitGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.ai.goal.TrackOwnerAttackerGoal;
import net.minecraft.entity.ai.goal.UntamedActiveTargetGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.WolfSoundVariant;
import net.minecraft.entity.passive.WolfSoundVariants;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.text.Text;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * A whippet. Faster than anything else on four legs in the overworld, quiet
 * about it, and asleep for twenty-three hours of the day.
 */
public class WhippetEntity extends TameableEntity {
	private static final TrackedData<Integer> COAT = DataTracker.registerData(WhippetEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Integer> COLLAR_COLOR = DataTracker.registerData(WhippetEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Boolean> ZOOMING = DataTracker.registerData(WhippetEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

	public static final Identifier ZOOMIES_SPEED_MODIFIER_ID = Whippets.id("zoomies");
	private static final EntityAttributeModifier ZOOMIES_SPEED_MODIFIER = new EntityAttributeModifier(
		ZOOMIES_SPEED_MODIFIER_ID, 0.65, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
	);

	private static final float WILD_MAX_HEALTH = 14.0F;
	private static final float TAMED_MAX_HEALTH = 24.0F;
	private static final DyeColor DEFAULT_COLLAR_COLOR = DyeColor.LIGHT_BLUE;
	/** Whippets are quiet dogs that sigh a lot, so they borrow the sad wolf's voice. */
	private static final WolfSoundVariant SOUNDS = SoundEvents.WOLF_SOUNDS.get(WolfSoundVariants.Type.SAD);
	/** Sighthounds run by sight: rabbits and chickens are the whole job description. */
	public static final TargetPredicate.EntityPredicate PREY_PREDICATE = (entity, world) -> {
		EntityType<?> type = entity.getType();
		return type == EntityType.RABBIT || type == EntityType.CHICKEN;
	};

	private float tuckProgress;
	private float lastTuckProgress;
	/** Where the lure is while this dog is racing, and whether it is still in the traps. */
	private @Nullable Vec3d lurePos;
	private boolean inTraps;
	private int reactionTicks;
	private boolean zoomiesRequested;
	/** This dog's form: a lasting edge or handicap over a racing distance. */
	private float pace = 1.0F;

	public WhippetEntity(EntityType<? extends WhippetEntity> entityType, World world) {
		super(entityType, world);
		this.setTamed(false, false);
		// Thin-skinned and thin-coated: powder snow is a whippet's idea of hell.
		this.setPathfindingPenalty(PathNodeType.POWDER_SNOW, -1.0F);
		this.setPathfindingPenalty(PathNodeType.DANGER_POWDER_SNOW, -1.0F);
		this.setPathfindingPenalty(PathNodeType.WATER, 4.0F);
	}

	public static DefaultAttributeContainer.Builder createWhippetAttributes() {
		return AnimalEntity.createAnimalAttributes()
			.add(EntityAttributes.MOVEMENT_SPEED, 0.38)
			.add(EntityAttributes.MAX_HEALTH, WILD_MAX_HEALTH)
			.add(EntityAttributes.ATTACK_DAMAGE, 3.0)
			.add(EntityAttributes.FOLLOW_RANGE, 24.0)
			.add(EntityAttributes.JUMP_STRENGTH, 0.55)
			.add(EntityAttributes.SAFE_FALL_DISTANCE, 4.0);
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(0, new RaceGoal(this));
		this.goalSelector.add(1, new SwimGoal(this));
		this.goalSelector.add(1, new TameableEntity.TameableEscapeDangerGoal(1.6, DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES));
		this.goalSelector.add(2, new SitGoal(this));
		this.goalSelector.add(3, new ZoomiesGoal(this));
		this.goalSelector.add(4, new PounceAtTargetGoal(this, 0.45F));
		this.goalSelector.add(5, new MeleeAttackGoal(this, 1.3, true));
		this.goalSelector.add(6, new FollowOwnerGoal(this, 1.35, 10.0F, 2.0F));
		this.goalSelector.add(7, new AnimalMateGoal(this, 1.0));
		this.goalSelector.add(8, new TemptGoal(this, 1.15, stack -> stack.isIn(ModTags.WHIPPET_FOOD), false));
		this.goalSelector.add(9, new WanderAroundFarGoal(this, 1.0));
		this.goalSelector.add(10, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
		this.goalSelector.add(10, new LookAroundGoal(this));
		this.targetSelector.add(1, new TrackOwnerAttackerGoal(this));
		this.targetSelector.add(2, new AttackWithOwnerGoal(this));
		this.targetSelector.add(3, new RevengeGoal(this).setGroupRevenge());
		this.targetSelector.add(4, new UntamedActiveTargetGoal<>(this, AnimalEntity.class, false, PREY_PREDICATE));
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(COAT, WhippetCoat.FAWN.getId());
		builder.add(COLLAR_COLOR, DEFAULT_COLLAR_COLOR.getIndex());
		builder.add(ZOOMING, false);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putString("Coat", this.getCoat().getName());
		view.putFloat("Pace", this.pace);
		view.put("CollarColor", DyeColor.INDEX_CODEC, this.getCollarColor());
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		String coatName = view.getString("Coat", WhippetCoat.FAWN.getName());

		for (WhippetCoat coat : WhippetCoat.values()) {
			if (coat.getName().equals(coatName)) {
				this.setCoat(coat);
				break;
			}
		}

		this.setCollarColor(view.read("CollarColor", DyeColor.INDEX_CODEC).orElse(DEFAULT_COLLAR_COLOR));
		this.pace = view.getFloat("Pace", 1.0F);
	}

	@Override
	public @Nullable EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData) {
		this.setCoat(WhippetCoat.random(world.getRandom()));
		this.pace = rollPace(world.getRandom());
		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	public WhippetCoat getCoat() {
		return WhippetCoat.byId(this.dataTracker.get(COAT));
	}

	public void setCoat(WhippetCoat coat) {
		this.dataTracker.set(COAT, coat.getId());
	}

	public DyeColor getCollarColor() {
		return DyeColor.byIndex(this.dataTracker.get(COLLAR_COLOR));
	}

	public void setCollarColor(DyeColor color) {
		this.dataTracker.set(COLLAR_COLOR, color.getIndex());
	}

	/** Form is mostly luck of the draw, clustered around average. */
	private static float rollPace(net.minecraft.util.math.random.Random random) {
		return 0.90F + (random.nextFloat() + random.nextFloat()) * 0.10F;
	}

	public float getPace() {
		return this.pace;
	}

	/** Ticks this dog is still blinking at the bell before it gets going. */
	public int getReactionTicks() {
		return this.reactionTicks;
	}

	public void tickReaction() {
		if (this.reactionTicks > 0) {
			this.reactionTicks--;
		}
	}

	/** Puts the dog on the start line: held facing the lure until the bell. */
	public void enterTraps(Vec3d lure, Vec3d lane, float yaw) {
		this.lurePos = lure;
		this.inTraps = true;
		this.setSitting(false);
		this.setInSittingPose(false);
		this.setTarget(null);
		this.setZooming(false);
		this.navigation.stop();

		if (this.getEntityWorld() instanceof ServerWorld world) {
			double y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, MathHelper.floor(lane.x), MathHelper.floor(lane.z));
			this.teleport(world, lane.x, Math.min(y, lane.y + 4.0), lane.z, java.util.Set.of(), yaw, 0.0F, true);
		}

		this.setYaw(yaw);
		this.setBodyYaw(yaw);
		this.setHeadYaw(yaw);
	}

	public void leaveTraps() {
		this.inTraps = false;
		// Some dogs miss the break completely; it is half of whippet racing.
		this.reactionTicks = this.random.nextInt(11);
	}

	public void stopRacing() {
		this.lurePos = null;
		this.inTraps = false;
		this.reactionTicks = 0;
	}

	public boolean isRacing() {
		return this.lurePos != null;
	}

	public boolean isInTraps() {
		return this.inTraps;
	}

	public @Nullable Vec3d getLurePos() {
		return this.lurePos;
	}

	/** Asks for the zoomies now rather than whenever they were next due. */
	public void requestZoomies() {
		this.zoomiesRequested = true;
	}

	public boolean consumeZoomiesRequest() {
		boolean requested = this.zoomiesRequested;
		this.zoomiesRequested = false;
		return requested;
	}

	public boolean isZooming() {
		return this.dataTracker.get(ZOOMING);
	}

	public void setZooming(boolean zooming) {
		this.dataTracker.set(ZOOMING, zooming);
		EntityAttributeInstance speed = this.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);

		if (speed != null) {
			speed.removeModifier(ZOOMIES_SPEED_MODIFIER_ID);

			if (zooming) {
				speed.addTemporaryModifier(ZOOMIES_SPEED_MODIFIER);
			}
		}
	}

	@Override
	protected void updateAttributesForTamed() {
		if (this.isTamed()) {
			this.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(TAMED_MAX_HEALTH);
			this.setHealth(TAMED_MAX_HEALTH);
		} else {
			this.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(WILD_MAX_HEALTH);
		}

		this.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE).setBaseValue(3.0);
	}

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		ItemStack stack = player.getStackInHand(hand);
		Item item = stack.getItem();

		if (this.isTamed()) {
			if (this.isBreedingItem(stack) && this.getHealth() < this.getMaxHealth()) {
				this.eat(player, hand, stack);
				FoodComponent food = stack.get(DataComponentTypes.FOOD);
				float nutrition = food != null ? food.nutrition() : 1.0F;
				this.heal(2.0F * nutrition);
				return ActionResult.SUCCESS;
			}

			if (item instanceof DyeItem dye && this.isOwner(player)) {
				DyeColor color = dye.getColor();

				if (color != this.getCollarColor()) {
					this.setCollarColor(color);
					stack.decrementUnlessCreative(1, player);
					return ActionResult.SUCCESS;
				}
			}

			ActionResult result = super.interactMob(player, hand);

			if (!result.isAccepted() && this.isOwner(player)) {
				this.setSitting(!this.isSitting());
				this.jumping = false;
				this.navigation.stop();
				this.setTarget(null);
				return ActionResult.SUCCESS.noIncrementStat();
			}

			return result;
		} else if (!this.getEntityWorld().isClient() && stack.isIn(ModTags.WHIPPET_FOOD)) {
			stack.decrementUnlessCreative(1, player);
			this.tryTame(player);
			return ActionResult.SUCCESS_SERVER;
		}

		return super.interactMob(player, hand);
	}

	private void tryTame(PlayerEntity player) {
		if (this.random.nextInt(3) == 0) {
			this.setTamedBy(player);
			this.navigation.stop();
			this.setTarget(null);
			this.setSitting(true);
			this.getEntityWorld().sendEntityStatus(this, EntityStatuses.ADD_POSITIVE_PLAYER_REACTION_PARTICLES);
		} else {
			this.getEntityWorld().sendEntityStatus(this, EntityStatuses.ADD_NEGATIVE_PLAYER_REACTION_PARTICLES);
		}
	}

	@Override
	public boolean isBreedingItem(ItemStack stack) {
		return stack.isIn(ModTags.WHIPPET_FOOD);
	}

	@Override
	public @Nullable WhippetEntity createChild(ServerWorld world, PassiveEntity other) {
		WhippetEntity puppy = ModEntities.WHIPPET.create(world, SpawnReason.BREEDING);

		if (puppy != null && other instanceof WhippetEntity mate) {
			// Pups take after their parents, give or take.
			puppy.pace = MathHelper.clamp((this.pace + mate.pace) / 2.0F + (this.random.nextFloat() - 0.5F) * 0.06F, 0.85F, 1.15F);

			// Coat comes from one parent or the other, with the odd throwback.
			if (this.random.nextInt(10) == 0) {
				puppy.setCoat(WhippetCoat.random(this.random));
			} else {
				puppy.setCoat(this.random.nextBoolean() ? this.getCoat() : mate.getCoat());
			}

			if (this.isTamed()) {
				puppy.setOwner(this.getOwnerReference());
				puppy.setTamed(true, true);
				puppy.setCollarColor(DyeColor.mixColors(world, this.getCollarColor(), mate.getCollarColor()));
			}
		}

		return puppy;
	}

	@Override
	public boolean canBreedWith(AnimalEntity other) {
		if (other == this || !this.isTamed() || !(other instanceof WhippetEntity mate) || !mate.isTamed()) {
			return false;
		}

		return !mate.isInSittingPose() && this.isInLove() && mate.isInLove();
	}

	@Override
	public void tick() {
		super.tick();

		if (this.isAlive()) {
			// Whippets stand with a tucked-up loin; they tuck harder when cold or wet.
			this.lastTuckProgress = this.tuckProgress;
			float target = this.isCold() ? 1.0F : 0.0F;
			this.tuckProgress = this.tuckProgress + (target - this.tuckProgress) * 0.1F;
		}
	}

	@Override
	public void tickMovement() {
		super.tickMovement();

		if (this.getEntityWorld() instanceof ServerWorld world && this.isZooming() && this.isOnGround() && this.age % 3 == 0) {
			world.spawnParticles(
				ParticleTypes.CLOUD, this.getX(), this.getY() + 0.05, this.getZ(), 1, 0.1, 0.0, 0.1, 0.01
			);
		}
	}

	/** Thin coat, no body fat: whippets shiver in snow and rain long before a wolf would. */
	public boolean isCold() {
		return this.isTouchingWaterOrRain() || this.getEntityWorld().getBiome(this.getBlockPos()).value().isCold(this.getBlockPos(), this.getEntityWorld().getSeaLevel());
	}

	public float getTuckProgress(float tickProgress) {
		return MathHelper.lerp(tickProgress, this.lastTuckProgress, this.tuckProgress);
	}

	/**
	 * Tail carriage. A whippet's tail hangs low with a curve in it; it streams out
	 * behind at speed and creeps further under the dog the more miserable it is.
	 */
	public float getTailAngle() {
		if (this.isZooming()) {
			return 1.25F;
		} else if (this.isTamed()) {
			float health = this.getMaxHealth();
			float missing = (health - this.getHealth()) / health;
			return 0.65F - missing * 0.35F;
		} else {
			return 0.45F;
		}
	}

	/** What the race results call this dog: its name, or its colour. */
	public Text getRaceName() {
		return this.hasCustomName() ? this.getDisplayName() : Text.translatable("race.whippets.unnamed", Text.translatable("coat.whippets." + this.getCoat().getName()));
	}

	public Identifier getTextureId() {
		return this.getCoat().getTexture();
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		this.playSound(SoundEvents.ENTITY_WOLF_STEP, 0.08F, 1.4F);
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		if (this.isZooming()) {
			return SOUNDS.pantSound().value();
		} else if (this.isTamed() && this.getHealth() < this.getMaxHealth() * 0.5F) {
			return SOUNDS.whineSound().value();
		}

		return this.random.nextInt(3) == 0 ? SOUNDS.pantSound().value() : SOUNDS.ambientSound().value();
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SOUNDS.hurtSound().value();
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SOUNDS.deathSound().value();
	}

	@Override
	protected float getSoundVolume() {
		return 0.3F;
	}

	@Override
	public int getLimitPerChunk() {
		return 4;
	}

	@Override
	public boolean canBeLeashed() {
		return true;
	}
}
