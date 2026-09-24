package dev.whippet.whippets.entity;

import dev.whippet.whippets.ModEntities;
import dev.whippet.whippets.ModItems;
import dev.whippet.whippets.ModSounds;
import dev.whippet.whippets.ModTags;
import dev.whippet.whippets.Whippets;
import dev.whippet.whippets.entity.ai.BarkUpTheTreeGoal;
import dev.whippet.whippets.entity.ai.BegGoal;
import dev.whippet.whippets.entity.ai.BurrowGoal;
import dev.whippet.whippets.entity.ai.CuddleGoal;
import dev.whippet.whippets.entity.ai.FetchGoal;
import dev.whippet.whippets.entity.ai.GreetGoal;
import dev.whippet.whippets.entity.ai.HuntCatsGoal;
import dev.whippet.whippets.entity.ai.RaceGoal;
import dev.whippet.whippets.entity.ai.SnootGoal;
import dev.whippet.whippets.entity.ai.TakeOnTheBullyGoal;
import dev.whippet.whippets.entity.ai.ZoomiesGoal;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
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
import net.minecraft.entity.ai.pathing.Path;
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
import net.minecraft.particle.BlockStateParticleEffect;
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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
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
	private static final TrackedData<Boolean> SNOOTING = DataTracker.registerData(WhippetEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean> TURBO = DataTracker.registerData(WhippetEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean> CARRYING_BALL = DataTracker.registerData(WhippetEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean> LURCHER = DataTracker.registerData(WhippetEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

	public static final Identifier ZOOMIES_SPEED_MODIFIER_ID = Whippets.id("zoomies");
	private static final EntityAttributeModifier ZOOMIES_SPEED_MODIFIER = new EntityAttributeModifier(
		ZOOMIES_SPEED_MODIFIER_ID, 0.65, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
	);

	/**
	 * Turbo. The modifier is rebuilt in steps as the dog winds up, because a
	 * whippet does not arrive at top speed, it accelerates into it.
	 */
	/**
	 * A lurcher is a whippet's bigger cousin — a sighthound crossed with
	 * something with a bit more bone — so it stands a head taller, carries more
	 * weight and has more pace than any whippet on the field.
	 */
	public static final Identifier LURCHER_SIZE_MODIFIER_ID = Whippets.id("lurcher_size");
	public static final Identifier LURCHER_HEALTH_MODIFIER_ID = Whippets.id("lurcher_health");
	private static final double LURCHER_SCALE = 0.18;
	private static final double LURCHER_HEALTH = 8.0;
	public static final Identifier CHAMPION_HEALTH_MODIFIER_ID = Whippets.id("champion_health");
	public static final Identifier CHAMPION_DAMAGE_MODIFIER_ID = Whippets.id("champion_damage");
	public static final Identifier CHAMPION_ARMOUR_MODIFIER_ID = Whippets.id("champion_armour");
	public static final Identifier CHAMPION_FOOTING_MODIFIER_ID = Whippets.id("champion_footing");
	/**
	 * What a champion is carrying that the dog he was yesterday is not. A
	 * lurcher off the stadium is a racing dog with a bit of size on it; the one
	 * at the top of the card has been doing this for years, against things that
	 * bite back, and it shows in every one of these numbers.
	 */
	private static final double CHAMPION_HEALTH = 48.0;
	private static final double CHAMPION_DAMAGE = 19.0;
	private static final double CHAMPION_ARMOUR = 10.0;
	/** He is not thrown about, which is most of what beats an ordinary dog here. */
	private static final double CHAMPION_FOOTING = 0.7;
	/**
	 * A lurcher's pace. Well clear of anything a whippet is born with — the
	 * best of them roll about 1.10 — because the whole point of the dog at the
	 * top of the card is that beating him takes a good one and a good run.
	 */
	private static final float LURCHER_PACE = 1.28F;
	public static final Identifier TURBO_SPEED_MODIFIER_ID = Whippets.id("turbo");
	public static final Identifier BLOWN_SPEED_MODIFIER_ID = Whippets.id("blown");
	/** What a blown dog is reduced to until it gets its breath back. */
	private static final EntityAttributeModifier BLOWN_SPEED_MODIFIER = new EntityAttributeModifier(
		BLOWN_SPEED_MODIFIER_ID, -0.3, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
	);
	/** Flat out, on top of everything else: what gets the dog up to speed. */
	private static final double TURBO_BOOST = 0.45;
	/**
	 * Top speed, in blocks a tick, and the reason there is a number here at all:
	 * left to the attribute and the game's own friction a dog at full stretch
	 * settles at nearly thirty blocks a second, which is not a whippet, it is a
	 * bullet. Thirteen blocks a second is a bit over twice a sprinting player,
	 * which is the ratio between a whippet and a person in life, and a shade
	 * over the fastest horse — so a whippet is the quickest thing on four legs
	 * in the overworld, for six seconds at a time and no longer.
	 */
	private static final double TOP_SPEED = 0.65;
	/** What it is doing before the turbo comes in, so the wind-up has somewhere to start. */
	private static final double CRUISING_SPEED = 0.3;
	/** What a dog settles into out of the traps, before it makes its run. */
	private static final double RACE_CRUISE = 0.45;
	/**
	 * What the ground takes back off a running animal each tick, measured rather
	 * than looked up: a dog accelerating at a per tick settles at a/DRAG.
	 */
	private static final double GROUND_DRAG = 0.454;
	/** How many steps the wind-up is applied in. */
	private static final int TURBO_STEPS = 8;
	/**
	 * Ticks of flat-out running in a whippet. Six seconds, which sounds mean
	 * until you remember that a real one covers a hundred and fifty metres in
	 * that and then wants to lie down.
	 */
	private static final int LUNGS = 120;
	/** Puppies have a fraction of the tank and absolutely no judgement about it. */
	private static final int PUPPY_LUNGS = 50;
	/**
	 * Breath a blown dog has to get back before it will go again: three quarters
	 * of the tank, not a mouthful. A dog that goes again on the first breath it
	 * gets back just blows again three seconds later, which is neither a dog nor
	 * a feature.
	 */
	private static final int RECOVERED = 90;
	/** One tick of breath back per this many spent not running flat out. */
	private static final int RECOVERY_RATE = 3;
	/** A slip is a licence to run: this long, or until the tank is empty. */
	private static final int SLIP_TICKS = 100;
	/** Beyond this the dog has to run something down rather than trot after it. */
	private static final double WORTH_RUNNING_FOR = 6.0;
	/**
	 * How often a dog at full stretch is given a fresh line to run on. The goals
	 * repath on their own schedule, which is built for animals that walk: a
	 * turbo whippet arrives where the path ended and stands there waiting for
	 * the next one, so it gets its own.
	 */
	private static final int TURBO_REPATH_INTERVAL = 4;
	private static final double TURBO_NAV_SPEED = 1.45;
	/** How wound up a dog has to be before it stops reading the path and just goes. */
	private static final float RUNS_BY_SIGHT_AT = 0.45F;
	/** Inside this it is close enough to stop steering and start biting. */
	private static final double CLOSE_ENOUGH = 1.0;
	/** How far ahead it looks for something it would rather not hit. */
	private static final double LOOKS_AHEAD = 1.6;
	/** Where to look for room on the start line, in blocks from the lane itself. */
	private static final double[] TRAP_SHIFTS = {0.0, -1.0, 1.0, -2.0, 2.0};
	/** How far off you can be, sprinting, and still have the dog come with you. */
	private static final double KEEPS_UP_WITHIN = 24.0;

	private static final float WILD_MAX_HEALTH = 14.0F;
	private static final float TAMED_MAX_HEALTH = 24.0F;
	private static final DyeColor DEFAULT_COLLAR_COLOR = DyeColor.LIGHT_BLUE;
	/**
	 * Ticks between meals before a whippet starts reminding you. Rather less
	 * than half a Minecraft day, so it comes round two or three times a day —
	 * which, if you have met one, is generous.
	 */
	private static final int HUNGRY_AFTER = 9000;
	/** How long the nose stays against you, in ticks. Long enough to notice. */
	private static final int SNOOT_TICKS = 10;
	/** Ticks under the covers before a whippet counts as warm again. */
	private static final int WARMED_THROUGH = 2400;
	/** How far round a bed to look for somebody already asleep in it. */
	private static final double BED_REACH = 2.2;
	/** The hop out of the bed. */
	private static final double LEAP_OUT = 0.42;
	/**
	 * What is left of the borrowed voice. The whine and the honk are Bonnie
	 * herself now; this is the panting and muttering underneath them, and the
	 * yelp when something hurts — which nobody has recorded her doing, and
	 * nobody is going to.
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
	/** What a whippet gets back for catching something, which is most of the appeal. */
	private static final float A_GOOD_CATCH = 4.0F;

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
	private float snootProgress;
	private float lastSnootProgress;
	/** Ticks since this dog was last fed. */
	private int hungerTicks;
	/** The bedding this dog is on its way to, so no two head for the same one. */
	private @Nullable BlockPos beddingClaim;
	/** The stadium's champion: untameable, and he stays on the ground. */
	private boolean champion;
	private int snootTicks;
	private boolean settledSigh;
	/** This dog's form: a lasting edge or handicap over a racing distance. */
	private float pace = 1.0F;
	/** Ticks of flat-out running left in this dog. */
	private int breath = LUNGS;
	/** Ticks of licence left on a slip: told to go, and going. */
	private int slipTicks;
	/** Run the tank dry and the dog is blown: no turbo, and slower than usual. */
	private boolean blown;
	/** How wound up it is, 0 to 1, which is both the speed and the shape of it. */
	private float turboProgress;
	private float lastTurboProgress;

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
		// A thrown ball outranks the zoomies, the duvet and everything under
		// them, which is the correct order of things.
		this.goalSelector.add(3, new FetchGoal(this));
		this.goalSelector.add(4, new ZoomiesGoal(this));
		this.goalSelector.add(5, new BurrowGoal(this));
		this.goalSelector.add(6, new CuddleGoal(this));
		this.goalSelector.add(6, new BegGoal(this));
		// Hungry: go and put your nose against the back of their leg.
		this.goalSelector.add(6, new SnootGoal(this));
		this.goalSelector.add(7, new GreetGoal(this));
		this.goalSelector.add(7, new PounceAtTargetGoal(this, 0.45F));
		this.goalSelector.add(7, new BarkUpTheTreeGoal(this));
		this.goalSelector.add(8, new MeleeAttackGoal(this, 1.3, true));
		this.goalSelector.add(9, new FollowOwnerGoal(this, 1.35, 10.0F, 2.0F));
		this.goalSelector.add(10, new AnimalMateGoal(this, 1.0));
		this.goalSelector.add(11, new TemptGoal(this, 1.15, stack -> stack.isIn(ModTags.WHIPPET_FOOD), false));
		this.goalSelector.add(12, new WanderAroundFarGoal(this, 1.0));
		this.goalSelector.add(13, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
		this.goalSelector.add(13, new LookAroundGoal(this));
		this.targetSelector.add(1, new TrackOwnerAttackerGoal(this));
		this.targetSelector.add(2, new AttackWithOwnerGoal(this));
		this.targetSelector.add(3, new RevengeGoal(this).setGroupRevenge());
		this.targetSelector.add(4, new UntamedActiveTargetGoal<>(this, AnimalEntity.class, false, PREY_PREDICATE));
		// Squirrels are the exception to every rule about a well-behaved dog:
		// tame or wild, sighthounds go after them. Only while the squirrel is
		// still on the ground, though — once it is up the trunk the chase is
		// over and the dog knows it, whatever it says about it afterwards.
		// Cats. Every whippet within earshot comes in on it; see HuntCatsGoal.
		this.targetSelector.add(4, new HuntCatsGoal(this));
		this.targetSelector.add(4, new TakeOnTheBullyGoal(this));
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
		builder.add(SNOOTING, false);
		builder.add(TURBO, false);
		builder.add(CARRYING_BALL, false);
		builder.add(LURCHER, false);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putString("Coat", this.getCoat().getName());
		view.putFloat("Pace", this.pace);
		view.putInt("Hunger", this.hungerTicks);
		view.putInt("Breath", this.breath);
		view.putBoolean("Ball", this.isCarryingBall());
		view.putBoolean("Lurcher", this.isLurcher());
		view.putBoolean("Champion", this.champion);
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
		this.hungerTicks = view.getInt("Hunger", 0);
		this.breath = view.getInt("Breath", LUNGS);
		this.dataTracker.set(CARRYING_BALL, view.getBoolean("Ball", false));
		this.champion = view.getBoolean("Champion", false);

		if (view.getBoolean("Lurcher", false)) {
			this.setLurcher(true);
		}

		if (this.champion) {
			// The modifiers are temporary ones, so they have to go back on
			// every time he is read off the disk, or the stadium's champion
			// comes back after a restart as an ordinary big dog.
			float health = view.getFloat("Health", 0.0F);
			this.championsBuild();

			if (health > 0.0F) {
				this.setHealth(Math.min(health, this.getMaxHealth()));
			}
		}
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
	 * for a while afterwards. Rabbits, chickens and — to the horror of everyone
	 * except the dog — cats go the same way.
	 */
	@Override
	public boolean onKilledOther(ServerWorld world, LivingEntity other, DamageSource damageSource) {
		if (other instanceof SquirrelEntity || HuntCatsGoal.isCat(other)) {
			this.heal(A_GOOD_CATCH);
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

	/**
	 * Hungry. A whippet that is hungry does not bark about it: it comes and puts
	 * its nose against the back of your leg, which is worse.
	 */
	public boolean isHungry() {
		return this.isTamed() && this.hungerTicks > HUNGRY_AFTER;
	}

	/** Fed. Resets the clock, whatever the food actually did for it. */
	public void feed() {
		this.hungerTicks = 0;
	}

	public boolean isSnooting() {
		return this.dataTracker.get(SNOOTING);
	}

	/**
	 * The snoot: a cold nose put deliberately against you, or against another
	 * whippet by way of hello. It is a whole conversation in this breed.
	 */
	public void snoot() {
		this.snootTicks = SNOOT_TICKS;
		this.dataTracker.set(SNOOTING, true);
		this.playSound(SoundEvents.ENTITY_FOX_SNIFF, this.getSoundVolume() * 0.7F, 1.2F + this.random.nextFloat() * 0.15F);
	}

	public float getSnootProgress(float tickProgress) {
		return MathHelper.lerp(tickProgress, this.lastSnootProgress, this.snootProgress);
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
	public void enterTraps(Vec3d lure, Vec3d lane, Vec3d startLine, float yaw) {
		this.lurePos = lure;
		this.inTraps = true;
		this.setSitting(false);
		this.setInSittingPose(false);
		this.setTarget(null);
		this.setZooming(false);
		this.navigation.stop();

		if (this.getEntityWorld() instanceof ServerWorld world) {
			Vec3d trap = this.findTrap(world, lane, startLine);
			this.teleport(world, trap.x, trap.y, trap.z, java.util.Set.of(), yaw, 0.0F, true);
		}

		this.setYaw(yaw);
		this.setBodyYaw(yaw);
		this.setHeadYaw(yaw);
	}

	/**
	 * Somewhere to stand on the line. Drawn across open grass a start line is
	 * fine; drawn across a bank, a ditch or a spruce it is not, and a dog put
	 * inside a tree trunk is not going to race anybody. Failing all of it, the
	 * dog goes where the starter is standing, which is somewhere a body fits.
	 */
	private Vec3d findTrap(ServerWorld world, Vec3d lane, Vec3d startLine) {
		// Its own lane first, then a stride either side of it, because a lane
		// that lands in a bank or a pond is no use to the dog standing in it.
		for (double shift : TRAP_SHIFTS) {
			for (double swing : TRAP_SHIFTS) {
				Vec3d spot = lane.add(shift, 0.0, swing);

				for (int step = 3; step >= -4; step--) {
					double y = lane.y + step;

					if (this.roomToStand(world, BlockPos.ofFloored(spot.x, y, spot.z))) {
						return new Vec3d(spot.x, y, spot.z);
					}
				}
			}
		}

		return startLine;
	}

	/** Ground under it and two blocks of nothing above that. */
	private boolean roomToStand(ServerWorld world, BlockPos feet) {
		BlockPos below = feet.down();
		BlockPos head = feet.up();
		return !world.getBlockState(below).getCollisionShape(world, below).isEmpty()
			&& world.getBlockState(feet).getCollisionShape(world, feet).isEmpty()
			&& world.getBlockState(head).getCollisionShape(world, head).isEmpty();
	}

	public void leaveTraps() {
		this.inTraps = false;
		// Some dogs miss the break completely; it is half of whippet racing.
		// Not the champion, though. He has done this before.
		this.reactionTicks = this.champion ? this.random.nextInt(3) : this.random.nextInt(11);
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

	/**
	 * Turbo. A whippet has one trick nothing else in the overworld can answer:
	 * it drops into a double-suspension gallop and goes twice as fast as a
	 * sprinting human. What it has not got is any depth to it — about six
	 * seconds of that, and then it is done, and a dog that runs the tank all the
	 * way out is blown and no use to anybody for a while. So it is not a second
	 * speed setting. It is something a whippet spends.
	 */
	public boolean isTurbo() {
		return this.dataTracker.get(TURBO);
	}

	/** How wound up it is, 0 to 1. A whippet needs three strides to get going. */
	public float getTurboProgress(float tickProgress) {
		return MathHelper.lerp(tickProgress, this.lastTurboProgress, this.turboProgress);
	}

	/** Run right out of breath: reduced to a trot until it has some back. */
	public boolean isBlown() {
		return this.blown;
	}

	/** Ticks of flat-out running left, out of {@link #getLungs()}. */
	public int getBreath() {
		return this.breath;
	}

	public int getLungs() {
		return this.isBaby() ? PUPPY_LUNGS : LUNGS;
	}

	/**
	 * Slipped, which is the word the racing people use for letting one go. The
	 * dog runs flat out at whatever it was already doing — and if it was not
	 * doing anything, it invents something, because a whippet handed permission
	 * to run does not stand there holding it.
	 *
	 * @return whether there was anything in the tank to slip
	 */
	public boolean slip() {
		if (!this.hasSomethingInTheTank() || this.isTiedUp()) {
			return false;
		}

		// A dog being let go gets up first. Being asked is one of the few things
		// that will get a whippet off a bed.
		this.setSitting(false);
		this.setInSittingPose(false);
		this.clearComfort();
		this.slipTicks = SLIP_TICKS;

		if (this.getTarget() == null && !this.isRacing()) {
			this.requestZoomies();
		}

		return true;
	}

	/**
	 * Whether this one is a lurcher: bigger, heavier and faster than the dogs it
	 * lines up against, and not something that turns up wild.
	 */
	public boolean isLurcher() {
		return this.dataTracker.get(LURCHER);
	}

	public void setLurcher(boolean lurcher) {
		this.dataTracker.set(LURCHER, lurcher);
		this.applyBuild(EntityAttributes.SCALE, LURCHER_SIZE_MODIFIER_ID, LURCHER_SCALE, lurcher);
		this.applyBuild(EntityAttributes.MAX_HEALTH, LURCHER_HEALTH_MODIFIER_ID, LURCHER_HEALTH, lurcher);

		if (lurcher) {
			this.setCoat(WhippetCoat.LURCHER);
			this.pace = LURCHER_PACE;
			this.setHealth(this.getMaxHealth());
		}
	}

	/** Puts one of the lurcher's modifiers on or takes it off again. */
	private void applyBuild(net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.attribute.EntityAttribute> attribute, Identifier id, double amount, boolean on) {
		EntityAttributeInstance instance = this.getAttributeInstance(attribute);

		if (instance == null) {
			return;
		}

		instance.removeModifier(id);

		if (on) {
			instance.addTemporaryModifier(new EntityAttributeModifier(id, amount, EntityAttributeModifier.Operation.ADD_VALUE));
		}
	}

	/**
	 * The stadium's own dog. He is not for sale, he is not coming home with you,
	 * and he does not leave the ground.
	 */
	public boolean isChampion() {
		return this.champion;
	}

	public void makeChampion(BlockPos home) {
		this.champion = true;
		this.setLurcher(true);
		this.championsBuild();
		this.setPersistent();
		this.setPositionTarget(home, 70);
	}

	/**
	 * The champion's build. Health, bite, hide and footing, in that order, and
	 * all four are needed: an XL Bully is beaten by hitting it far harder than
	 * a whippet can, for longer than a whippet lasts, while it is unable to put
	 * you on your back. Take any one of them away and the dog loses.
	 */
	private void championsBuild() {
		this.applyBuild(EntityAttributes.MAX_HEALTH, CHAMPION_HEALTH_MODIFIER_ID, CHAMPION_HEALTH, true);
		this.applyBuild(EntityAttributes.ATTACK_DAMAGE, CHAMPION_DAMAGE_MODIFIER_ID, CHAMPION_DAMAGE, true);
		this.applyBuild(EntityAttributes.ARMOR, CHAMPION_ARMOUR_MODIFIER_ID, CHAMPION_ARMOUR, true);
		this.applyBuild(EntityAttributes.KNOCKBACK_RESISTANCE, CHAMPION_FOOTING_MODIFIER_ID, CHAMPION_FOOTING, true);
		this.setHealth(this.getMaxHealth());
	}

	/** Whether it has a ball in its mouth, which changes a whippet's whole day. */
	public boolean isCarryingBall() {
		return this.dataTracker.get(CARRYING_BALL);
	}

	/**
	 * Takes the ball. One dog in four decides at this point that it is now its
	 * ball and goes off round the field with it before any question of giving it
	 * back arises, which is the correct and traditional behaviour.
	 */
	public void pickUpBall(ItemEntity ball) {
		ball.discard();
		this.dataTracker.set(CARRYING_BALL, true);
		this.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 0.4F, 1.6F);
	}

	/** Gives it back, or at least puts it down, which is the same thing eventually. */
	public void dropBall() {
		if (!this.isCarryingBall()) {
			return;
		}

		this.dataTracker.set(CARRYING_BALL, false);

		if (this.getEntityWorld() instanceof ServerWorld world) {
			ItemEntity ball = new ItemEntity(world, this.getX(), this.getY() + 0.2, this.getZ(), new ItemStack(ModItems.WHIPPET_BALL));
			ball.setPickupDelay(10);
			ball.setVelocity(this.getRotationVector().multiply(0.08).add(0.0, 0.08, 0.0));
			world.spawnEntity(ball);
		}

		this.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 0.3F, 1.2F);
	}

	/** Whether there is anything left to spend. */
	public boolean hasSomethingInTheTank() {
		return !this.blown && this.breath > 0;
	}

	/** On a lead, on a boat, or in the water: not going anywhere fast either way. */
	public boolean isTiedUp() {
		return this.isLeashed() || this.hasVehicle() || this.isTouchingWater();
	}

	/** Nothing with this dog's build declines to run. These are the things that stop it. */
	public boolean canTurbo() {
		if (!this.hasSomethingInTheTank() || this.isTiedUp()) {
			return false;
		}

		// Held on the line, or still blinking at the bell: a dog that goes
		// because somebody sprinted past the traps has jumped the gun.
		if (this.isInTraps() || this.getReactionTicks() > 0) {
			return false;
		}

		return !this.isInSittingPose() && !this.isBurrowed() && !this.isCurled();
	}

	/**
	 * Decides whether the dog is flat out this tick, spends or returns breath
	 * accordingly, and keeps the speed attribute in step with the wind-up.
	 */
	private void tickTurbo() {
		boolean turbo = (this.slipTicks > 0 || this.hasSomethingWorthRunningAt()) && this.canTurbo();

		if (turbo != this.isTurbo()) {
			this.dataTracker.set(TURBO, turbo);
		}

		if (turbo) {
			if (this.slipTicks > 0) {
				this.slipTicks--;
			}

			if (--this.breath <= 0) {
				this.breath = 0;
				this.blow();
			}

			if (this.age % TURBO_REPATH_INTERVAL == 0) {
				this.keepTheLine();
			}
		} else {
			this.slipTicks = 0;

			if (this.breath < this.getLungs() && this.age % RECOVERY_RATE == 0) {
				this.breath++;
			}

			if (this.blown && this.breath >= Math.min(RECOVERED, this.getLungs())) {
				this.blown = false;
			}
		}

		this.applyTurboSpeed();
		this.holdTopSpeed();
	}

	@Override
	protected void mobTick(ServerWorld world) {
		super.mobTick(world);

		// Sighthounds run by sight, and this is where that stops being a figure
		// of speech. This runs after the navigation has had its say and before
		// the movement is applied, so at full stretch it overrides the path: a
		// path is a chain of blocks with a corner at every one of them, and a
		// dog going this fast overruns all of them and arrives having zig-zagged
		// the whole way. Given a clear run it goes straight at the thing instead.
		// A racing dog runs by sight from the bell, not only when it goes flat
		// out: the lure is a point it can see, and a block path to it through
		// trees and banks is how a race turns into four dogs milling about.
		if (this.turboProgress > RUNS_BY_SIGHT_AT || this.isRunningTheLine()) {
			this.runBySight();
		}
	}

	/** Straight at it, while there is nothing in the way. */
	private void runBySight() {
		Vec3d aim = this.aimPoint();

		if (aim == null) {
			return;
		}

		Vec3d flat = new Vec3d(aim.x - this.getX(), 0.0, aim.z - this.getZ());
		double distance = flat.horizontalLength();

		if (distance < CLOSE_ENOUGH) {
			return;
		}

		Vec3d line = flat.multiply(1.0 / distance);

		// Something in the way: hand the steering back to the pathfinder, which
		// is slower and knows about corners. Unless the pathfinder has nothing
		// either — at the foot of a bank it returns a path one node long that
		// goes nowhere — in which case the dog pushes at it regardless, scrabbles
		// and jumps, and looks like a dog rather than a statue.
		if (!this.clearRun(line) && !this.navigation.isIdle()) {
			return;
		}

		this.getMoveControl().moveTo(aim.x, this.getY(), aim.z, this.controlSpeedFor(this.topSpeedNow()));
		this.getLookControl().lookAt(aim.x, aim.y, aim.z);
	}

	/** What it is running at, in the order a whippet would care about. */
	private @Nullable Vec3d aimPoint() {
		if (this.lurePos != null) {
			return this.lurePos;
		}

		LivingEntity quarry = this.getTarget();

		if (quarry != null && quarry.isAlive()) {
			return quarry.getEntityPos();
		}

		if (this.getOwner() instanceof PlayerEntity owner && owner.isSprinting() && owner.getEntityWorld() == this.getEntityWorld()) {
			return owner.getEntityPos();
		}

		// Nothing in particular: the far end of whatever it was already doing,
		// which for a slipped dog is the next corner of its lap.
		Path path = this.navigation.getCurrentPath();
		return path == null ? null : Vec3d.ofBottomCenter(path.getTarget());
	}

	/**
	 * Whether the next stride and a half is worth taking at speed: nothing to
	 * run into at knee or chest height, something to put a foot on, and no water
	 * to go through.
	 */
	private boolean clearRun(Vec3d line) {
		World world = this.getEntityWorld();
		BlockPos ahead = BlockPos.ofFloored(this.getX() + line.x * LOOKS_AHEAD, this.getY() + 0.1, this.getZ() + line.z * LOOKS_AHEAD);
		BlockPos chest = ahead.up();
		BlockPos footing = ahead.down();

		if (!world.getBlockState(ahead).getCollisionShape(world, ahead).isEmpty()
			|| !world.getBlockState(chest).getCollisionShape(world, chest).isEmpty()) {
			return false;
		}

		if (world.getBlockState(footing).getCollisionShape(world, footing).isEmpty()) {
			return false;
		}

		return world.getFluidState(ahead).isEmpty() && world.getFluidState(footing).isEmpty();
	}

	/**
	 * Keeps a fresh line under a dog at full stretch. Racing is left alone —
	 * {@link dev.whippet.whippets.entity.ai.RaceGoal} runs its own line to the
	 * lure — and so are the zoomies, which have nowhere in particular to be.
	 */
	private void keepTheLine() {
		if (this.isRacing()) {
			return;
		}

		LivingEntity quarry = this.getTarget();

		if (quarry != null && quarry.isAlive()) {
			this.navigation.startMovingTo(quarry, TURBO_NAV_SPEED);
			return;
		}

		if (!this.isZooming() && this.getOwner() instanceof PlayerEntity owner && owner.isSprinting()) {
			this.navigation.startMovingTo(owner, TURBO_NAV_SPEED);
		}
	}

	/**
	 * Whether there is anything about that a whippet cannot let go past: quarry
	 * far enough off that it has to be run down, or an owner who has just broken
	 * into a run. The lure is not here — {@link dev.whippet.whippets.entity.ai.RaceGoal}
	 * slips the dog itself, at the point in the race that dog has decided on.
	 */
	private boolean hasSomethingWorthRunningAt() {
		LivingEntity target = this.getTarget();

		if (target != null && target.isAlive() && this.squaredDistanceTo(target) > WORTH_RUNNING_FOR * WORTH_RUNNING_FOR) {
			return true;
		}

		if (!this.isTamed() || this.isSitting()) {
			return false;
		}

		// You started running. A whippet is physically unable to ignore this.
		if (!(this.getOwner() instanceof PlayerEntity owner) || !owner.isSprinting() || owner.getEntityWorld() != this.getEntityWorld()) {
			return false;
		}

		double distance = this.squaredDistanceTo(owner);
		return distance > 4.0 * 4.0 && distance < KEEPS_UP_WITHIN * KEEPS_UP_WITHIN;
	}

	/** How fast this dog should be going right now, in blocks a tick. */
	private double topSpeedNow() {
		double settled = this.isRunningTheLine() ? RACE_CRUISE : CRUISING_SPEED;
		return MathHelper.lerp(this.turboProgress, settled, TOP_SPEED) * this.pace;
	}

	/** Racing, out of the traps and off the line: running, in other words. */
	public boolean isRunningTheLine() {
		return this.isRacing() && !this.inTraps && this.reactionTicks <= 0;
	}

	/**
	 * What to hand the move control to end up at a given speed. The control
	 * turns its figure into an acceleration of (speed × the movement attribute)
	 * squared, which the ground's drag then settles into a terminal speed — so
	 * this is derived from the speed wanted rather than guessed at, and the
	 * attribute is left to do what it is for, which is getting there quickly.
	 */
	private double controlSpeedFor(double topSpeed) {
		double attribute = this.getAttributeValue(EntityAttributes.MOVEMENT_SPEED);
		return attribute <= 0.0 ? 1.0 : Math.sqrt(topSpeed * GROUND_DRAG) / attribute;
	}

	/**
	 * The ceiling, held hard. The derivation above gets a dog to the right speed
	 * on grass; ice, soul sand and mud all have their own drag, and without this
	 * a whippet on a frozen lake would still be accelerating when it reached the
	 * far shore.
	 */
	private void holdTopSpeed() {
		if (this.turboProgress < 0.001F) {
			return;
		}

		Vec3d velocity = this.getVelocity();
		double flat = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
		double ceiling = this.topSpeedNow();

		if (flat > ceiling) {
			double scale = ceiling / flat;
			this.setVelocity(velocity.x * scale, velocity.y, velocity.z * scale);
		}
	}

	/** Out of puff. It knows, you know, and everybody hears about it. */
	private void blow() {
		if (this.blown) {
			return;
		}

		this.blown = true;
		this.playSound(VOICE.pantSound().value(), this.getSoundVolume() * 1.5F, 0.8F);
		this.whine();
	}

	/**
	 * The speed itself. A modifier's value cannot be changed once it is on, so
	 * the wind-up goes on in a handful of steps. What is wanted is compared
	 * against what is actually on the dog rather than against a remembered
	 * value, because anything that reloads its attributes — a chunk reload, a
	 * command, another mod — quietly drops the modifier, and a whippet that
	 * thinks it is flat out while running at walking pace is worse than no
	 * turbo at all. A better dog turbos harder, on the same pace that decides
	 * races.
	 */
	private void applyTurboSpeed() {
		EntityAttributeInstance speed = this.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);

		if (speed == null) {
			return;
		}

		int step = Math.round(this.turboProgress * TURBO_STEPS);
		double wanted = TURBO_BOOST * this.pace * step / TURBO_STEPS;
		EntityAttributeModifier applied = speed.getModifier(TURBO_SPEED_MODIFIER_ID);

		if (step == 0) {
			if (applied != null) {
				speed.removeModifier(TURBO_SPEED_MODIFIER_ID);
			}
		} else if (applied == null || applied.value() != wanted) {
			speed.removeModifier(TURBO_SPEED_MODIFIER_ID);
			speed.addTemporaryModifier(new EntityAttributeModifier(
				TURBO_SPEED_MODIFIER_ID, wanted, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
			));
		}

		if (this.blown != speed.hasModifier(BLOWN_SPEED_MODIFIER_ID)) {
			speed.removeModifier(BLOWN_SPEED_MODIFIER_ID);

			if (this.blown) {
				speed.addTemporaryModifier(BLOWN_SPEED_MODIFIER);
			}
		}
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
		this.playSound(ModSounds.WHIPPET_WHINE, this.getSoundVolume() * 1.1F, this.getWhinePitch());
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

	/**
	 * The long-suffering sigh of a dog that has just got comfortable. It is her
	 * whine again, dropped and quietened: the same noise a settling whippet
	 * makes, let out slowly instead of aimed at anybody.
	 */
	private void sigh() {
		this.playSound(ModSounds.WHIPPET_WHINE, this.getSoundVolume() * 0.6F, 0.7F + this.random.nextFloat() * 0.06F);
	}

	/** Her call, pitched to this dog: puppies higher, and no two quite alike. */
	private float getWhinePitch() {
		float pitch = this.isBaby() ? 1.3F : 1.0F;
		return pitch * (0.95F + (this.pace - 0.9F) * 0.5F) + (this.random.nextFloat() - 0.5F) * 0.08F;
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

	/**
	 * Says which bedding this dog is on its way to. Two whippets setting off for
	 * the same bed and arriving together is how you end up with two whippets in
	 * one bed, which is not a thing either of them would put up with.
	 */
	public void claimBedding(@Nullable BlockPos bedding) {
		this.beddingClaim = bedding;
	}

	public @Nullable BlockPos getBeddingClaim() {
		return this.beddingClaim;
	}

	/**
	 * Turns whoever is in the bed out of it — you, or a villager who thought
	 * they had found somewhere quiet. A whippet does not consider a sleeping
	 * body an obstacle; it considers it warm, and in the way.
	 */
	public void turnOutSleeper(BlockPos bedding) {
		Box bed = new Box(bedding).expand(BED_REACH);

		for (LivingEntity sleeper : this.getEntityWorld().getEntitiesByClass(LivingEntity.class, bed, LivingEntity::isSleeping)) {
			sleeper.wakeUp();
			this.playSound(SoundEvents.ENTITY_FOX_SNIFF, this.getSoundVolume(), 1.1F);

			if (sleeper instanceof PlayerEntity player) {
				player.sendMessage(Text.translatable("entity.whippets.whippet.took_the_bed", this.getRaceName()), true);
			}
		}
	}

	/**
	 * How a whippet leaves a bed: not a climb down, a departure. Straight up and
	 * out, usually because it has heard something, and never for any reason you
	 * are able to establish.
	 */
	public void jumpOut() {
		this.setVelocity(
			(this.random.nextDouble() - 0.5) * 0.25,
			LEAP_OUT,
			(this.random.nextDouble() - 0.5) * 0.25
		);
		this.velocityDirty = true;
		this.playSound(SoundEvents.ENTITY_WOLF_SHAKE, this.getSoundVolume() * 0.8F, this.getSoundPitch());
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
				this.feed();
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

			// Crouch and hold out an empty hand at your own dog and you slip it:
			// permission to run, which it takes entirely literally.
			if (this.isOwner(player) && player.isSneaking() && stack.isEmpty()) {
				if (this.getEntityWorld().isClient()) {
					return ActionResult.SUCCESS;
				}

				if (!this.slip()) {
					player.sendMessage(
						Text.translatable(
							this.isTiedUp() ? "entity.whippets.whippet.tied_up" : "entity.whippets.whippet.blown",
							this.getRaceName()
						),
						true
					);
				}

				return ActionResult.SUCCESS_SERVER;
			}

			ActionResult result = super.interactMob(player, hand);

			// Bred, grown or simply eaten: any food that goes in stops the nose
			// coming back for a while.
			if (result.isAccepted() && this.isBreedingItem(stack)) {
				this.feed();
			}

			if (!result.isAccepted() && this.isOwner(player)) {
				this.setSitting(!this.isSitting());
				this.jumping = false;
				this.navigation.stop();
				this.setTarget(null);
				return ActionResult.SUCCESS.noIncrementStat();
			}

			return result;
		} else if (this.champion) {
			// Flattered, but he runs for the stadium.
			if (!this.getEntityWorld().isClient()) {
				player.sendMessage(Text.translatable("entity.whippets.whippet.not_for_sale", this.getRaceName()), true);
			}

			return ActionResult.SUCCESS;
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
	public void onDeath(DamageSource source) {
		// Whatever else is happening, the ball goes back into the world.
		this.dropBall();
		super.onDeath(source);
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

			// The nose goes out fast and comes back slower, the way a boop does.
			this.lastSnootProgress = this.snootProgress;
			float snootTarget = this.isSnooting() ? 1.0F : 0.0F;
			this.snootProgress = this.snootProgress + (snootTarget - this.snootProgress) * (this.isSnooting() ? 0.55F : 0.2F);

			// Winding up into the gallop and coming back off it. Three strides
			// to full stretch, and rather quicker than that to stop.
			this.lastTurboProgress = this.turboProgress;
			this.turboProgress = MathHelper.clamp(this.turboProgress + (this.isTurbo() ? 0.09F : -0.15F), 0.0F, 1.0F);
		}
	}

	@Override
	public void tickMovement() {
		super.tickMovement();

		if (this.honkCooldown > 0) {
			this.honkCooldown--;
		}

		if (this.snootTicks > 0 && --this.snootTicks == 0) {
			this.dataTracker.set(SNOOTING, false);
		}

		// Hunger only runs for a dog that has somebody to complain to.
		if (this.isTamed() && this.hungerTicks < HUNGRY_AFTER * 3) {
			this.hungerTicks++;
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

		if (!this.getEntityWorld().isClient()) {
			this.tickTurbo();

		}

		if (this.getEntityWorld() instanceof ServerWorld world && this.isZooming() && this.isOnGround() && this.age % 3 == 0) {
			world.spawnParticles(
				ParticleTypes.CLOUD, this.getX(), this.getY() + 0.05, this.getZ(), 1, 0.1, 0.0, 0.1, 0.01
			);
		}

		if (this.getEntityWorld() instanceof ServerWorld world && this.turboProgress > 0.4F && this.isOnGround() && this.age % 2 == 0) {
			this.kickUpTurf(world);
		}
	}

	/**
	 * What a whippet at full stretch leaves behind it: whatever it is running
	 * over, thrown backwards out of the ground.
	 */
	private void kickUpTurf(ServerWorld world) {
		BlockState ground = this.getSteppingBlockState();

		if (ground.isAir()) {
			return;
		}

		Vec3d back = this.getRotationVector().multiply(-0.5);
		world.spawnParticles(
			new BlockStateParticleEffect(ParticleTypes.BLOCK, ground),
			this.getX() + back.x,
			this.getY() + 0.1,
			this.getZ() + back.z,
			2,
			0.12,
			0.02,
			0.12,
			0.08
		);
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
		if (this.blown) {
			// Nothing left: it comes down and stays down.
			return 0.4F;
		} else if (this.isTurbo()) {
			// Straight out behind, in line with the back, where it does some good.
			return 1.55F;
		} else if (this.isZooming()) {
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
		if (this.blown) {
			return VOICE.pantSound().value();
		} else if (this.isZooming()) {
			return VOICE.pantSound().value();
		} else if (this.isBegging() || this.isTamed() && this.getHealth() < this.getMaxHealth() * 0.5F) {
			return ModSounds.WHIPPET_WHINE;
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
