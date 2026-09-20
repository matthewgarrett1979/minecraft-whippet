package dev.whippet.whippets.entity;

import dev.whippet.whippets.Whippets;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

/**
 * The coats a whippet can turn up in. Weights are roughly how common the colour
 * is in the breed, so fawn and brindle dominate and pure white is a treat. A
 * coat with no weight is never handed out by chance.
 */
public enum WhippetCoat {
	FAWN("fawn", 5),
	BRINDLE("brindle", 4),
	BLUE("blue", 3),
	BLACK("black", 3),
	WHITE("white", 1),
	/**
	 * Bonnie's own: blue brindle, silver face, white blaze, four white feet.
	 * Weight zero, so she is never rolled at random and never turns up wild — a
	 * whippet has to be named for her before she wears it.
	 */
	BONNIE("bonnie", 0);

	private static final WhippetCoat[] VALUES = values();
	private static final int TOTAL_WEIGHT;

	private final String name;
	private final int weight;
	private final Identifier texture;

	WhippetCoat(String name, int weight) {
		this.name = name;
		this.weight = weight;
		this.texture = Whippets.id("textures/entity/whippet/whippet_" + name + ".png");
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

	public static WhippetCoat byId(int id) {
		return id >= 0 && id < VALUES.length ? VALUES[id] : FAWN;
	}

	/** Picks a coat by weight. Coats weighted zero are never picked. */
	public static WhippetCoat random(Random random) {
		int roll = random.nextInt(TOTAL_WEIGHT);

		for (WhippetCoat coat : VALUES) {
			if (coat.weight == 0) {
				continue;
			}

			roll -= coat.weight;

			if (roll < 0) {
				return coat;
			}
		}

		return FAWN;
	}

	static {
		int total = 0;

		for (WhippetCoat coat : VALUES) {
			total += coat.weight;
		}

		TOTAL_WEIGHT = total;
	}
}
