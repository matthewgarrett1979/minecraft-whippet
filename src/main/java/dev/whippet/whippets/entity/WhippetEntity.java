package dev.whippet.whippets.entity;

import dev.whippet.whippets.ModEntities;
import dev.whippet.whippets.ModSounds;
import dev.whippet.whippets.ModTags;
import dev.whippet.whippets.Whippets;
import dev.whippet.whippets.entity.ai.BarkUpTheTreeGoal;
import dev.whippet.whippets.entity.ai.BegGoal;
import dev.whippet.whippets.entity.ai.BurrowGoal;
import dev.whippet.whippets.entity.ai.CuddleGoal;
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
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
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
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
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
	private static final TrackedData<Boolean> CURLED = DataTracker.registerData(WhippetEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean> BURROWED = DataTracker.registerData(WhippetEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean> BEGGING = DataTracker.registerData(WhippetEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

	public static final Identifier ZOOMIES_SPEED_MODIFIER_ID = Whippets.id("zoomies");
	private static final EntityAttributeModifier ZOOMIES_SPEED_MODIFIER = new EntityAttributeModifier(
		ZOOMIES_SPEED_MODIFIER_ID, 0.65, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
	);

	private static final float WILD_MAX_HEALTH = 14.0F;
	private static final float TAMED_MAX_HEALTH = 24.0F;
	private static final DyeColor DEFAULT_COLLAR_COLOR = DyeColor.LIGHT_BLUE;
	/** Ticks under the covers before a whippet counts as warm again. */
	private static final int WARMED_THROUGH = 2400;
	/**
	 * A whippet's voice: the small dog's panting and muttering, and the sad dog's
	 * whine, which is the noise they actually make most of the time.
	 */
	private static final WolfSoundVariant VOICE = SoundEvents.WOLF_SOUNDS.get(WolfSoundVariants.Type.CUTE);
	private static final WolfSoundVariant WHINGE = SoundEvents.WOLF_SOUNDS.get(WolfSoundVariants.Type.SAD);
	/** Ticks between whines, so they nag rather than drone. */
	private static final int WHINE_COOLDOWN = 60;
	/** Ticks between honks. Longer: a honk is not a background noise. */
	private static final int HONK_COOLDOWN = 90;
	/**
	 * How long a whippet will ask nicely before it starts honking at you. Two
	 * and a half seconds, which is about right.
	 */
	private static final int PATIENCE = 50;
	/** Sighthounds run by sight: rabbits and chickens are the whole job description. */
	public static final TargetPredicate.EntityPredicate PREY_PREDICATE = (entity, world) -> {
		EntityType<?> type = entity.getType();
		return type == EntityType.RABBIT || type == EntityType.CHICKEN;
	};
	/** What a whippet gets back for catching one, which is most of the appeal. */
	private static final float SQUIRREL_IS_WORTH = 4.0F;

	private float tuckProgress;
	private float lastTuckProgress;
	/** Where the lure is while this dog is racing, and whether it is still in the traps. */
	private @Nullable Vec3d lurePos;
	private boolean inTraps;
	private int reactionTicks;
	private boolean zoomiesRequested;
	/** How thoroughly tucked up this dog is; it will not come out until it is warm. */
	private int warmth;
	private int whineCooldown;
	private int honkCooldown;
	private int nagCooldown;
	/** How long this dog has been shut out of something it wanted. */
	private int shutOutTicks;
	private float begProgress;
	private float lastBegProgress;
	private boolean settledSigh;
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
		this.goalSelector.add(4, new BurrowGoal(this));
		this.goalSelector.add(5, new CuddleGoal(this));
		this.goalSelector.add(5, new BegGoal(this));
		this.goalSelector.add(6, new PounceAtTargetGoal(this, 0.45F));
		this.goalSelector.add(6, new BarkUpTheTreeGoal(this));
		this.goalSelector.add(7, new MeleeAttackGoal(this, 1.3, true));
		this.goalSelector.add(8, new FollowOwnerGoal(this, 1.35, 10.0F, 2.0F));
		this.goalSelector.add(9, new AnimalMateGoal(this, 1.0));
		this.goalSelector.add(10, new TemptGoal(this, 1.15, stack -> stack.isIn(ModTags.WHIPPET_FOOD), false));
		this.goalSelector.add(11, new WanderAroundFarGoal(this, 1.0));
		this.goalSelector.add(12, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
		this.goalSelector.add(12, new LookAroundGoal(this));
		this.targetSelector.add(1, new TrackOwnerAttackerGoal(this));
		this.targetSelector.add(2, new AttackWithOwnerGoal(this));
		this.targetSelector.add(3, new RevengeGoal(this).setGroupRevenge());
		this.targetSelector.add(4, new UntamedActiveTargetGoal<>(this, AnimalEntity.class, false, PREY_PREDICATE));
		// Squirrels are the exception to every rule about a well-behaved dog:
		// tame or wild, sighthounds go after them. Only while the squirrel is
		// still on the ground, though — once it is up the trunk the chase is
		// over and the dog knows it, whatever it says about it afterwards.
		this.targetSelector.add(5, new ActiveTargetGoal<>(this, SquirrelEntity.class, 10, true, false, (entity, world) -> {
			return !this.isRacing() && !this.isInSittingPose() && entity instanceof SquirrelEntity squirrel && squirrel.isReachable(this.getY());
		}));
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(COAT, WhippetCoat.FAWN.getId());
		builder.add(COLLAR_COLOR, DEFAULT_COLLAR_COLOR.getIndex());
		builder.add(ZOOMING, false);
		builder.add(CURLED, false);
		builder.add(BURROWED, false);
		builder.add(BEGGING, false);
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

	/**
	 * A caught squirrel is eaten on the spot, and the dog is pleased with itself
	 * for a while afterwards. Rabbits and chickens go the same way.
	 */
	@Override
	public boolean onKilledOther(ServerWorld world, LivingEntity other, DamageSource damageSource) {
		if (other instanceof SquirrelEntity) {
			this.heal(SQUIRREL_IS_WORTH);
			this.playSound(SoundEvents.ENTITY_FOX_EAT, 0.7F, this.getSoundPitch());
			world.spawnParticles(
				new net.minecraft.particle.ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(Items.RABBIT)),
				this.getX(),
				this.getY() + 0.35,
				this.getZ(),
				12,
				0.2,
				0.1,
				0.2,
				0.03
			);
		}

		return super.onKilledOther(world, other, damageSource);
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

	public boolean isBegging() {
		return this.dataTracker.get(BEGGING);
	}

	public void setBegging(boolean begging) {
		this.dataTracker.set(BEGGING, begging);
	}

	public float getBegProgress(float tickProgress) {
		return MathHelper.lerp(tickProgress, this.lastBegProgress, this.begProgress);
	}

	/** The noise. Rate-limited, because even a whippet has to breathe. */
	public void whine() {
		if (this.whineCooldown > 0 || this.isSilent()) {
			return;
		}

		this.whineCooldown = WHINE_COOLDOWN + this.random.nextInt(40);
		this.playSound(WHINGE.whineSound().value(), this.getSoundVolume() * 1.1F, this.getSoundPitch());
	}

	/**
	 * The honk: a flat, nasal, carrying shout, and the reason whippet owners all
	 * say the same thing about geese. It is not a distress noise — it is a
	 * demand, and it is aimed at you.
	 */
	public void honk() {
		if (this.honkCooldown > 0 || this.isSilent()) {
			return;
		}

		this.honkCooldown = HONK_COOLDOWN + this.random.nextInt(70);
		// The whine shares the cooldown, so a honking dog does not also whinge
		// over the top of itself.
		this.whineCooldown = Math.max(this.whineCooldown, WHINE_COOLDOWN);
		this.playSound(ModSounds.WHIPPET_HONK, this.getSoundVolume() * 1.25F, this.getHonkPitch());
	}

	/**
	 * Asking for something. A whippet starts by whining about it and works up to
	 * honking at you, and the longer it has been asking the more likely the honk
	 * — which is exactly how it goes in a kitchen at teatime.
	 *
	 * @param persistence ticks this dog has wanted whatever it is
	 */
	public void demand(int persistence) {
		if (persistence > PATIENCE || this.random.nextInt(5) == 0) {
			this.honk();
			return;
		}

		this.whine();
	}

	/** Puppies honk higher, and no two dogs honk on quite the same note. */
	private float getHonkPitch() {
		float pitch = this.isBaby() ? 1.35F : 1.0F;
		// A dog's own voice: derived from its pace so each dog sounds like itself.
		return pitch * (0.94F + (this.pace - 0.9F) * 0.6F) + (this.random.nextFloat() - 0.5F) * 0.06F;
	}

	/** The long-suffering sigh of a dog that has just got comfortable. */
	private void sigh() {
		this.playSound(WHINGE.whineSound().value(), this.getSoundVolume() * 0.7F, 0.75F + this.random.nextFloat() * 0.1F);
	}

	public boolean isCurled() {
		return this.dataTracker.get(CURLED);
	}

	public void setCurled(boolean curled) {
		this.dataTracker.set(CURLED, curled);
	}

	public boolean isBurrowed() {
		return this.dataTracker.get(BURROWED);
	}

	public void setBurrowed(boolean burrowed) {
		this.dataTracker.set(BURROWED, burrowed);
	}

	/** Climbs into the bedding and vanishes under it. */
	public void burrowInto(BlockPos bedding) {
		this.navigation.stop();
		// Sits on the bedding; the renderer drops it the rest of the way under.
		this.refreshPositionAndAngles(bedding.getX() + 0.5, bedding.getY() + 0.5625, bedding.getZ() + 0.5, this.getYaw(), 0.0F);
		this.setVelocity(Vec3d.ZERO);
		this.setBurrowed(true);
		this.setCurled(true);
	}

	/** Gets a whippet out from under the covers and off your lap. */
	public void clearComfort() {
		this.setBurrowed(false);
		this.setCurled(false);
	}

	/**
	 * A whippet is only interested in the duvet when it is cold, or when the
	 * world is: rain, snow, a cold biome or simply night.
	 */
	public boolean wantsToBurrow() {
		if (this.isBurrowed()) {
			// Stays put until it has warmed through, and stays in all night regardless.
			return this.warmth < WARMED_THROUGH || this.isWeatherCold() || this.getEntityWorld().isNight();
		}

		return this.isWeatherCold() || this.getEntityWorld().isNight();
	}

	/**
	 * Being tucked up warms the dog through, and anything it is curled against
	 * gets the benefit too: a whippet on your lap thaws you out.
	 */
	public void warmUp(@Nullable LivingEntity sharing) {
		this.warmth = Math.min(WARMED_THROUGH + 200, this.warmth + 1);

		if (this.warmth % 40 == 0 && this.getHealth() < this.getMaxHealth()) {
			this.heal(1.0F);
		}

		if (sharing != null && sharing.getFrozenTicks() > 0) {
			sharing.setFrozenTicks(Math.max(0, sharing.getFrozenTicks() - 4));
		}
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

			this.lastBegProgress = this.begProgress;
			float begTarget = this.isBegging() ? 1.0F : 0.0F;
			this.begProgress = this.begProgress + (begTarget - this.begProgress) * 0.25F;
		}
	}

	@Override
	public void tickMovement() {
		super.tickMovement();

		if (this.honkCooldown > 0) {
			this.honkCooldown--;
		}

		if (this.whineCooldown > 0) {
			this.whineCooldown--;
		}

		if (!this.getEntityWorld().isClient()) {
			this.tickWhinging();
		}

		if (!this.isSheltered() && this.warmth > 0) {
			this.warmth--;
		}

		if (this.isBurrowed()) {
			// Under a duvet nothing much happens, which is the point.
			this.setVelocity(Vec3d.ZERO);
			this.navigation.stop();
		}

		if (this.getEntityWorld() instanceof ServerWorld world && this.isZooming() && this.isOnGround() && this.age % 3 == 0) {
			world.spawnParticles(
				ParticleTypes.CLOUD, this.getX(), this.getY() + 0.05, this.getZ(), 1, 0.1, 0.0, 0.1, 0.01
			);
		}
	}

	/**
	 * The running commentary. A whippet whines when it is shut out of something
	 * it wants: left behind, told to stay, or cold and unable to find a duvet.
	 * It also sighs once, heavily, the moment it finally settles against you.
	 */
	private void tickWhinging() {
		if (this.isCurled()) {
			if (!this.settledSigh) {
				this.settledSigh = true;
				this.sigh();
			}

			return;
		}

		this.settledSigh = false;

		if (this.nagCooldown > 0) {
			this.nagCooldown--;
			return;
		}

		if (!this.isTamed() || this.isBurrowed() || this.isRacing()) {
			return;
		}

		this.nagCooldown = 100 + this.random.nextInt(140);
		LivingEntity owner = this.getOwner();

		// A whippet that can simply follow you does; the noise starts when it
		// cannot — told to stay, or on a lead — and you walk off anyway.
		boolean stuck = this.isSitting() || this.isLeashed();

		if (stuck && owner != null && owner.isAlive() && owner.getEntityWorld() == this.getEntityWorld()) {
			double distance = this.squaredDistanceTo(owner);

			if (distance > 6.0 * 6.0 && distance < 48.0 * 48.0) {
				// Left behind and it knows it. The first call is a whine; stay
				// away and it stops asking nicely.
				this.demand(this.shutOutTicks);
				this.shutOutTicks += 120;
				return;
			}
		}

		this.shutOutTicks = 0;

		// Cold, wet, and nowhere to get under.
		if (this.isCold() && this.random.nextInt(3) == 0) {
			this.whine();
		}
	}

	/** Thin coat, no body fat: whippets shiver in snow and rain long before a wolf would. */
	public boolean isCold() {
		return !this.isSheltered() && this.isWeatherCold();
	}

	private boolean isWeatherCold() {
		return this.isTouchingWaterOrRain()
			|| this.getEntityWorld().getBiome(this.getBlockPos()).value().isCold(this.getBlockPos(), this.getEntityWorld().getSeaLevel());
	}

	/** Under the covers or pressed against somebody: either way, warm. */
	public boolean isSheltered() {
		return this.isBurrowed() || this.isCurled();
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
			return VOICE.pantSound().value();
		} else if (this.isBegging() || this.isTamed() && this.getHealth() < this.getMaxHealth() * 0.5F) {
			return WHINGE.whineSound().value();
		}

		return this.random.nextInt(3) == 0 ? VOICE.pantSound().value() : VOICE.ambientSound().value();
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return WHINGE.hurtSound().value();
	}

	@Override
	protected SoundEvent getDeathSound() {
		return WHINGE.deathSound().value();
	}

	@Override
	protected float getSoundVolume() {
		return 0.3F;
	}

	/** Small, narrow dog: everything it says comes out higher than a wolf. */
	@Override
	public float getSoundPitch() {
		float pitch = this.isBaby() ? 1.6F : 1.25F;
		return pitch + (this.random.nextFloat() - this.random.nextFloat()) * 0.15F;
	}

	@Override
	public int getMinAmbientSoundDelay() {
		// They are not quiet dogs so much as constantly muttering ones.
		return 100;
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
