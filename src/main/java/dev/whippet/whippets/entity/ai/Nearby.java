package dev.whippet.whippets.entity.ai;

import java.util.function.Predicate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

/** The nearest thing of a given sort, which is all these goals ever ask for. */
final class Nearby {
	private Nearby() {
	}

	static <T extends LivingEntity> @Nullable T nearest(Class<T> type, Entity from, double range, Predicate<T> filter) {
		T closest = null;
		double best = range * range;

		for (T candidate : from.getEntityWorld().getEntitiesByClass(type, from.getBoundingBox().expand(range), filter)) {
			if (!candidate.isAlive() || candidate == from) {
				continue;
			}

			double distance = from.squaredDistanceTo(candidate);

			if (distance < best) {
				best = distance;
				closest = candidate;
			}
		}

		return closest;
	}
}
