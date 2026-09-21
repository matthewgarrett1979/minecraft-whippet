package dev.whippet.whippets.entity.ai;

import dev.whippet.whippets.entity.WhippetEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Box;

/**
 * Cats.
 *
 * <p>There is no negotiating with a sighthound about a cat. It is not dislike
 * exactly — it is that a cat is the right size, the right shape and the wrong
 * speed, and a whippet's opinion on the matter is settled before it has thought
 * about it. What makes it worse is that they do it together: the first dog to
 * see one brings every whippet within sixteen blocks in on it, and a cat with
 * three of them coming has nowhere to be.
 */
public class HuntCatsGoal extends ActiveTargetGoal<MobEntity> {
	/** How far the word goes when one of them spots a cat. */
	private static final double RALLY = 16.0;
	/** Checked this often, so the dogs do not all notice on the same tick. */
	private static final int RECIPROCAL_CHANCE = 10;

	private final WhippetEntity whippet;

	public HuntCatsGoal(WhippetEntity whippet) {
		super(whippet, MobEntity.class, RECIPROCAL_CHANCE, true, false, (entity, world) -> isCat(entity));
		this.whippet = whippet;
	}

	public static boolean isCat(LivingEntity entity) {
		return entity.getType() == EntityType.CAT || entity.getType() == EntityType.OCELOT;
	}

	@Override
	public boolean canStart() {
		return !this.whippet.isRacing() && !this.whippet.isInSittingPose() && !this.whippet.isBurrowed() && super.canStart();
	}

	@Override
	public void start() {
		super.start();
		LivingEntity cat = this.whippet.getTarget();

		if (cat == null) {
			return;
		}

		// The noise that starts it, and then everybody else comes.
		this.whippet.honk();
		this.rally(cat);
	}

	/** Brings the rest of the pack in on it. */
	private void rally(LivingEntity cat) {
		Box earshot = this.whippet.getBoundingBox().expand(RALLY);

		for (WhippetEntity other : this.whippet.getEntityWorld().getEntitiesByClass(WhippetEntity.class, earshot, dog -> dog.getTarget() == null)) {
			if (other == this.whippet || other.isRacing() || other.isInSittingPose() || other.isBurrowed() || other.isBaby()) {
				continue;
			}

			other.setTarget(cat);
		}
	}
}
