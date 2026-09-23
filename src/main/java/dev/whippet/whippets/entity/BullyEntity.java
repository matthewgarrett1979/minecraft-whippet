package dev.whippet.whippets.entity;

import dev.whippet.whippets.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.entity.passive.WolfSoundVariant;
import net.minecraft.entity.passive.WolfSoundVariants;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.Box;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * The XL Bully.
 *
 * <p>Half a ton of dog built like a filing cabinet: a metre and a half at the
 * shoulder, a head the width of its own chest, and no interest whatsoever in
 * being reasoned with. There is only ever one of them about — they do not run
 * in packs and they do not tolerate each other — and it does not belong to
 * anybody, whatever the man who lost it says.
 *
 * <p>It is not fast. That is the whole of the answer to it: nothing on four
 * short legs catches a whippet on grass. What it is, is heavy enough that
 * getting hold of it hurts, and hard enough to knock down that one dog trying
 * is one dog dead. It takes the pack — five or six of them, in and out, taking
 * turns at it while it is busy with somebody else — and even then it costs you.
 *
 * <p>A whippet on its own knows better and will not start. Three of them
 * together will, which is roughly the point at which they stop counting.
 */
public class BullyEntity extends PathAwareEntity {
	/** It is slow, and it never gets any faster. A whippet does 13 b/s; this does 5. */
	private static final double PACE = 0.26;
	private static final double HEALTH = 120.0;
	private static final double BITE = 6.0;
	private static final double ARMOUR = 2.0;
	/** It throws what it bites, which is most of why a pack cannot simply stand on it. */
	private static final double THROW = 1.1;
	/** Knocking it back is nearly impossible, so a whippet's charge does not move it. */
	private static final double PLANTED = 0.75;
	private static final double SEES_YOU = 32.0;
	/** How long between bites. It is a heavy dog and it commits to each one. */
	private static final int BITE_EVERY = 30;
	/** It does not care how close another bully is, because there is never one. */
	public static final double ALONE = 160.0;
	/** How near you have to be before it decides you are the problem. */
	private static final double YOUR_PROBLEM_NOW = 12.0;
	private static final TrackedData<Integer> LUNGE = DataTracker.registerData(BullyEntity.class, TrackedDataHandlerRegistry.INTEGER);

	/** The big-dog end of the wolf's own voice, for the one noise it only makes once. */
	private static final WolfSoundVariant BIG = SoundEvents.WOLF_SOUNDS.get(WolfSoundVariants.Type.ANGRY);

	private int biteCooldown;

	public BullyEntity(EntityType<? extends BullyEntity> type, World world) {
		super(type, world);
		this.setPersistent();
		this.experiencePoints = 40;
	}

	public static DefaultAttributeContainer.Builder createBullyAttributes() {
		return MobEntity.createMobAttributes()
			.add(EntityAttributes.MOVEMENT_SPEED, PACE)
			.add(EntityAttributes.MAX_HEALTH, HEALTH)
			.add(EntityAttributes.ATTACK_DAMAGE, BITE)
			.add(EntityAttributes.ATTACK_KNOCKBACK, THROW)
			.add(EntityAttributes.ARMOR, ARMOUR)
			.add(EntityAttributes.KNOCKBACK_RESISTANCE, PLANTED)
			.add(EntityAttributes.FOLLOW_RANGE, SEES_YOU)
			.add(EntityAttributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(LUNGE, 0);
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(1, new SwimGoal(this));
		this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0, true));
		this.goalSelector.add(6, new WanderAroundFarGoal(this, 0.8));
		this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 12.0F));
		this.goalSelector.add(8, new LookAroundGoal(this));
		this.targetSelector.add(1, new RevengeGoal(this));
		// Whippets, because they are there and because they started it.
		this.targetSelector.add(2, new ActiveTargetGoal<>(this, WhippetEntity.class, 10, true, false, (dog, world) -> !dog.isBaby()));
		// And you, if you come close enough to be worth the walk.
		this.targetSelector.add(3, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false, (player, world) -> {
			return this.squaredDistanceTo(player) < YOUR_PROBLEM_NOW * YOUR_PROBLEM_NOW;
		}));
	}

	/** True while it is mid-bite, which is what the model leans on. */
	public float lungeProgress(float tickProgress) {
		int lunge = this.dataTracker.get(LUNGE);
		return lunge <= 0 ? 0.0F : Math.min(1.0F, lunge / 6.0F);
	}

	@Override
	public void tick() {
		super.tick();

		if (!this.getEntityWorld().isClient()) {
			int lunge = this.dataTracker.get(LUNGE);

			if (lunge > 0) {
				this.dataTracker.set(LUNGE, lunge - 1);
			}

			if (this.biteCooldown > 0) {
				this.biteCooldown--;
			}
		}
	}

	@Override
	public boolean tryAttack(ServerWorld world, net.minecraft.entity.Entity target) {
		if (this.biteCooldown > 0) {
			return false;
		}

		this.biteCooldown = BITE_EVERY;
		this.dataTracker.set(LUNGE, 8);
		boolean bitten = super.tryAttack(world, target);

		if (bitten) {
			this.playSound(ModSounds.BULLY_BARK, 1.0F, 0.9F + this.random.nextFloat() * 0.2F);
		}

		return bitten;
	}

	/**
	 * There is only ever one. Anything else — two of them meeting in a field,
	 * six of them off one spawn — is a different animal to the one this is.
	 */
	public static boolean isAlone(ServerWorldAccess world, double x, double y, double z) {
		Box about = new Box(x - ALONE, y - 48.0, z - ALONE, x + ALONE, y + 48.0, z + ALONE);
		return world.getEntitiesByClass(BullyEntity.class, about, bully -> true).isEmpty();
	}

	@Override
	@Nullable
	public EntityData initialize(
		ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData
	) {
		this.setPersistent();
		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ModSounds.BULLY_GROWL;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.BULLY_BARK;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return BIG.deathSound().value();
	}

	@Override
	protected float getSoundVolume() {
		return 1.4F;
	}

	@Override
	public float getSoundPitch() {
		// A big chest on a short throat: everything it says comes out low.
		return 0.7F;
	}

	@Override
	public int getMinAmbientSoundDelay() {
		return 160;
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putInt("BiteCooldown", this.biteCooldown);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		this.biteCooldown = view.getInt("BiteCooldown", 0);
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return false;
	}

	@Override
	public boolean cannotDespawn() {
		return true;
	}

	/** Whatever is about to happen to it, it is not being carried off by a whippet. */
	@Override
	public boolean canTarget(LivingEntity target) {
		return !(target instanceof BullyEntity) && super.canTarget(target);
	}
}
