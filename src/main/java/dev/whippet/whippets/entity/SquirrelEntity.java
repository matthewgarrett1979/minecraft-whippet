package dev.whippet.whippets.entity;

import dev.whippet.whippets.ModEntities;
import dev.whippet.whippets.ModTags;
import dev.whippet.whippets.entity.ai.SquirrelFleeWhippetGoal;
import dev.whippet.whippets.entity.ai.SquirrelStashGoal;
import dev.whippet.whippets.entity.ai.SquirrelTauntGoal;
import net.minecraft.block.BlockState;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.AnimalMateGoal;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * A squirrel: small, quick, and entirely aware that it is faster up a tree than
 * anything chasing it is. Spends its day burying things it will not find again,
 * sitting up to eat with both hands, and telling dogs exactly what it thinks of
 * them from a branch six feet out of reach.
 */
public class SquirrelEntity extends AnimalEntity {
	private static final TrackedData<Integer> VARIANT = DataTracker.registerData(SquirrelEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Boolean> CARRYING = DataTracker.registerData(SquirrelEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean> TAUNTING = DataTracker.registerData(SquirrelEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean> SITTING_UP = DataTracker.registerData(SquirrelEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean> CLIMBING = DataTracker.registerData(SquirrelEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

	/** How far off a squirrel notices a whippet and stops pretending to be busy. */
	public static final double ALARM_RANGE = 12.0;
	/** Ticks of standing still before it sits up on its haunches to eat. */
	private static final int SETTLED = 60;
	/** How long it will hang on a trunk before deciding the coast is clear. */
	private static final int HOLDS_ON_FOR = 600;

	public final AnimationState tailFlickState = new AnimationState();

	private int stillTicks;
	private int chatterCooldown;
	private int clingTicks;

	public SquirrelEntity(EntityType<? extends SquirrelEntity> entityType, World world) {
		super(entityType, world);
	}

	public static DefaultAttributeContainer.Builder createSquirrelAttributes() {
		return AnimalEntity.createAnimalAttributes()
			.add(EntityAttributes.MOVEMENT_SPEED, 0.32)
			.add(EntityAttributes.MAX_HEALTH, 4.0)
			.add(EntityAttributes.FOLLOW_RANGE, 16.0)
			.add(EntityAttributes.JUMP_STRENGTH, 0.62)
			// Squirrels fall out of trees all the time and get up unbothered.
			.add(EntityAttributes.SAFE_FALL_DISTANCE, 12.0);
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(0, new SwimGoal(this));
		this.goalSelector.add(1, new EscapeDangerGoal(this, 2.0));
		this.goalSelector.add(1, new SquirrelFleeWhippetGoal(this));
		this.goalSelector.add(2, new SquirrelTauntGoal(this));
		// Cheek beats caution: it will come and take a seed out of your hand and
		// only then remember it is supposed to be frightened of you.
		this.goalSelector.add(3, new TemptGoal(this, 1.2, stack -> stack.isIn(ModTags.SQUIRREL_FOOD), false, 8.0));
		this.goalSelector.add(4, new AnimalMateGoal(this, 1.0));
		this.goalSelector.add(5, new SquirrelStashGoal(this));
		this.goalSelector.add(6, new FleeEntityGoal<>(this, PlayerEntity.class, 3.0F, 1.5, 2.0));
		this.goalSelector.add(7, new WanderAroundFarGoal(this, 1.0));
		this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
		this.goalSelector.add(8, new LookAroundGoal(this));
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(VARIANT, SquirrelVariant.RED.getId());
		builder.add(CARRYING, false);
		builder.add(TAUNTING, false);
		builder.add(SITTING_UP, false);
		builder.add(CLIMBING, false);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putString("Variant", this.getVariant().getName());
		view.putBoolean("Carrying", this.isCarrying());
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		this.setVariant(SquirrelVariant.byName(view.getString("Variant", SquirrelVariant.RED.getName())));
		this.setCarrying(view.getBoolean("Carrying", false));
	}

	@Override
	public @Nullable EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData) {
		this.setVariant(SquirrelVariant.random(world.getRandom()));
		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	public SquirrelVariant getVariant() {
		return SquirrelVariant.byId(this.dataTracker.get(VARIANT));
	}

	public void setVariant(SquirrelVariant variant) {
		this.dataTracker.set(VARIANT, variant.getId());
	}

	/** True while it has something in its cheeks that it means to bury. */
	public boolean isCarrying() {
		return this.dataTracker.get(CARRYING);
	}

	public void setCarrying(boolean carrying) {
		this.dataTracker.set(CARRYING, carrying);
	}

	/** Telling a dog off from somewhere the dog cannot get to. */
	public boolean isTaunting() {
		return this.dataTracker.get(TAUNTING);
	}

	public void setTaunting(boolean taunting) {
		this.dataTracker.set(TAUNTING, taunting);
	}

	/** Up on its haunches with both front paws at its mouth. */
	public boolean isSittingUp() {
		return this.dataTracker.get(SITTING_UP);
	}

	public void setSittingUp(boolean sittingUp) {
		this.dataTracker.set(SITTING_UP, sittingUp);
	}

	/** Hanging on the side of a tree, which is a squirrel's natural habitat. */
	public boolean isScrambling() {
		return this.dataTracker.get(CLIMBING);
	}

	/**
	 * Takes hold of, or lets go of, a trunk. A squirrel on bark does not slide
	 * back down while it gets its breath back, so gravity is switched off while
	 * it is holding on — it lets go on its own once nothing is chasing it.
	 */
	public void setScrambling(boolean clinging) {
		this.dataTracker.set(CLIMBING, clinging);
		this.setNoGravity(clinging);

		if (!clinging) {
			this.clingTicks = 0;
		}
	}

	/**
	 * Squirrels go up trees, not up walls. The game's ladder movement does the
	 * work; this only decides that bark counts and cobblestone does not.
	 */
	@Override
	public boolean isClimbing() {
		return this.isScrambling();
	}

	/** Whether there is something tree-shaped against the squirrel to go up. */
	public boolean isAgainstTrunk() {
		BlockPos pos = this.getBlockPos();

		for (Direction direction : Direction.Type.HORIZONTAL) {
			BlockState state = this.getEntityWorld().getBlockState(pos.offset(direction));

			if (state.isIn(BlockTags.LOGS) || state.isIn(BlockTags.LEAVES)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void tick() {
		super.tick();

		if (!this.getEntityWorld().isClient()) {
			if (this.isScrambling()) {
				this.clingTicks++;

				// Let go once the bark runs out, or once it has been up there
				// long enough to have made its point.
				if (!this.isAgainstTrunk() || this.clingTicks > HOLDS_ON_FOR) {
					this.setScrambling(false);
				}
			}

			if (this.getVelocity().horizontalLengthSquared() < 1.0E-5 && this.getNavigation().isIdle()) {
				this.stillTicks++;
			} else {
				this.stillTicks = 0;
			}

			// Sitting up to eat is what a settled squirrel does, and it is the
			// single most recognisable thing about them.
			this.setSittingUp(this.stillTicks > SETTLED && !this.isTaunting() && !this.isScrambling() && !this.isTouchingWater());

			if (this.chatterCooldown > 0) {
				this.chatterCooldown--;
			}
		} else if (this.isTaunting()) {
			this.tailFlickState.startIfNotRunning(this.age);
		} else {
			this.tailFlickState.stop();
		}
	}

	/** The scold: a hard rattling chatter, aimed at whatever is below. */
	public void chatter() {
		if (this.chatterCooldown > 0) {
			return;
		}

		this.chatterCooldown = 30;
		this.playSound(SoundEvents.ENTITY_FOX_AGGRO, 0.7F, 1.7F + this.random.nextFloat() * 0.2F);
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_FOX_SNIFF;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_FOX_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_FOX_DEATH;
	}

	@Override
	public float getSoundPitch() {
		// Small animal, small voice.
		return (this.isBaby() ? 2.1F : 1.8F) + (this.random.nextFloat() - 0.5F) * 0.1F;
	}

	@Override
	protected float getSoundVolume() {
		return 0.35F;
	}

	@Override
	public int getMinAmbientSoundDelay() {
		return 200;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		// Too light to make a noise worth hearing.
	}

	@Override
	public boolean isBreedingItem(ItemStack stack) {
		return stack.isIn(ModTags.SQUIRREL_FOOD);
	}

	@Override
	public @Nullable PassiveEntity createChild(ServerWorld world, PassiveEntity other) {
		SquirrelEntity kit = ModEntities.SQUIRREL.create(world, SpawnReason.BREEDING);

		if (kit != null) {
			SquirrelVariant mine = this.getVariant();
			SquirrelVariant theirs = other instanceof SquirrelEntity squirrel ? squirrel.getVariant() : mine;
			kit.setVariant(this.random.nextBoolean() ? mine : theirs);
		}

		return kit;
	}

	@Override
	protected int computeFallDamage(double fallDistance, float damagePerDistance) {
		// A squirrel's whole design is falling out of trees and carrying on.
		return 0;
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	/** Where the whippet has to get to, in a hurry, before this goes up a tree. */
	public boolean isReachable(double byY) {
		return this.getY() - byY < 2.0;
	}

	public float getTailAngle() {
		// Carried high and curled forward over the back; higher still when it is
		// showing off, which is most of the time.
		float base = this.isSittingUp() || this.isTaunting() ? 3.15F : 3.0F;
		return base + MathHelper.sin(this.age * 0.08F) * 0.06F;
	}
}
