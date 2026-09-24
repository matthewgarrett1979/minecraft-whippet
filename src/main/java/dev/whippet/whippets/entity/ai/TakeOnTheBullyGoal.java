package dev.whippet.whippets.entity.ai;

import dev.whippet.whippets.entity.BullyEntity;
import dev.whippet.whippets.entity.WhippetEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.util.math.Box;

/**
 * The bully.
 *
 * <p>A whippet is not a fighting dog and knows it. One on its own will not go
 * near an XL Bully: it will stand off, shout, and stay exactly where it is,
 * which is the right answer and is why lone whippets do not die of this.
 *
 * <p>What changes it is numbers. Get three of them together and the arithmetic
 * they are doing stops being about the bully's size and starts being about how
 * many of them there are, and then they all go in at once. Five and a lurcher
 * is about what it takes to finish it, and it still costs somebody.
 */
public class TakeOnTheBullyGoal extends ActiveTargetGoal<BullyEntity> {
	/** How far a dog counts its friends before deciding this is on. */
	private static final double SHOULDER_TO_SHOULDER = 14.0;
	/** And how many of them there have to be, counting itself. */
	private static final int ENOUGH_OF_US = 3;
	/** How far the word goes once one of them starts it. */
	private static final double RALLY = 24.0;
	private static final int RECIPROCAL_CHANCE = 10;

	private final WhippetEntity whippet;

	public TakeOnTheBullyGoal(WhippetEntity whippet) {
		super(whippet, BullyEntity.class, RECIPROCAL_CHANCE, true, false, (entity, world) -> true);
		this.whippet = whippet;
	}

	@Override
	public boolean canStart() {
		if (this.whippet.isRacing() || this.whippet.isInSittingPose() || this.whippet.isBurrowed() || this.whippet.isBaby()) {
			return false;
		}

		return this.enoughOfUs() && super.canStart();
	}

	@Override
	public boolean shouldContinue() {
		// Once it is on it stays on: a dog that counted three of them and then
		// watched two die does not stand there recounting.
		return super.shouldContinue();
	}

	/** Counts the dogs stood near enough to be in on it, this one included. */
	private boolean enoughOfUs() {
		if (this.whippet.isChampion()) {
			// Bobby does not count anybody. He has done this before and he was
			// on his own that time as well.
			return true;
		}

		Box shoulder = this.whippet.getBoundingBox().expand(SHOULDER_TO_SHOULDER);
		int dogs = 1;

		for (WhippetEntity other : this.whippet.getEntityWorld().getEntitiesByClass(WhippetEntity.class, shoulder, dog -> !dog.isBaby())) {
			if (other != this.whippet && !other.isInSittingPose() && !other.isBurrowed()) {
				dogs++;
			}
		}

		return dogs >= ENOUGH_OF_US;
	}

	@Override
	public void start() {
		super.start();
		BullyEntity bully = this.whippet.getTarget() instanceof BullyEntity it ? it : null;

		if (bully == null) {
			return;
		}

		this.whippet.honk();
		Box earshot = this.whippet.getBoundingBox().expand(RALLY);

		for (WhippetEntity other : this.whippet.getEntityWorld().getEntitiesByClass(WhippetEntity.class, earshot, dog -> dog.getTarget() == null)) {
			if (other == this.whippet || other.isRacing() || other.isInSittingPose() || other.isBurrowed() || other.isBaby()) {
				continue;
			}

			other.setTarget(bully);
		}
	}
}
