package dev.whippet.whippets;

import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;

public final class ModTags {
	/** What a whippet will follow you across a biome for, and what tames one. */
	public static final TagKey<Item> WHIPPET_FOOD = TagKey.of(RegistryKeys.ITEM, Whippets.id("whippet_food"));

	/** Seeds, nuts and fruit: what a squirrel will steal, bury or be bribed with. */
	public static final TagKey<Item> SQUIRREL_FOOD = TagKey.of(RegistryKeys.ITEM, Whippets.id("squirrel_food"));

	private ModTags() {
	}
}
