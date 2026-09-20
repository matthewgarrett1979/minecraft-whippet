package dev.whippet.whippets.entity;

import dev.whippet.whippets.Whippets;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

/**
 * What a squirrel turns up in. Reds and greys share the wood; the black one is
 * a melanistic grey, which does happen and is worth stopping to look at.
 */
public enum SquirrelVariant {
	RED("red", 5),
	GREY("grey", 5),
	BLACK("black", 1);

	private static final SquirrelVariant[] VALUES = values();
	private static final int TOTAL_WEIGHT;

	private final String name;
	private final int weight;
	private final Identifier texture;

	SquirrelVariant(String name, int weight) {
		this.name = name;
		this.weight = weight;
		this.texture = Whippets.id("textures/entity/squirrel/squirrel_" + name + ".png");
	}

	public String getName() {
		return this.name;
	}

	public Identifier getTexture() {
		return this.texture;
	}

	public int getId() {
		return this.ordinal();
	}

	public static SquirrelVariant byId(int id) {
		return id >= 0 && id < VALUES.length ? VALUES[id] : RED;
	}

	public static SquirrelVariant byName(String name) {
		for (SquirrelVariant variant : VALUES) {
			if (variant.name.equals(name)) {
				return variant;
			}
		}

		return RED;
	}

	public static SquirrelVariant random(Random random) {
		int roll = random.nextInt(TOTAL_WEIGHT);

		for (SquirrelVariant variant : VALUES) {
			roll -= variant.weight;

			if (roll < 0) {
				return variant;
			}
		}

		return RED;
	}

	static {
		int total = 0;

		for (SquirrelVariant variant : VALUES) {
			total += variant.weight;
		}

		TOTAL_WEIGHT = total;
	}
}
