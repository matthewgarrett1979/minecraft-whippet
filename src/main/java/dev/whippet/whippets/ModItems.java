package dev.whippet.whippets;

import dev.whippet.whippets.item.WhippetBallItem;
import dev.whippet.whippets.item.WhippetLureItem;
import dev.whippet.whippets.item.WhippetWhistleItem;
import java.util.function.Function;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

public final class ModItems {
	public static final Item WHIPPET_SPAWN_EGG = register(
		"whippet_spawn_egg", SpawnEggItem::new, new Item.Settings().spawnEgg(ModEntities.WHIPPET)
	);
	public static final Item SQUIRREL_SPAWN_EGG = register(
		"squirrel_spawn_egg", SpawnEggItem::new, new Item.Settings().spawnEgg(ModEntities.SQUIRREL)
	);
	public static final Item WHIPPET_WHISTLE = register("whippet_whistle", WhippetWhistleItem::new, new Item.Settings().maxCount(1));
	public static final Item WHIPPET_LURE = register("whippet_lure", WhippetLureItem::new, new Item.Settings().maxCount(1));
	public static final Item WHIPPET_BALL = register("whippet_ball", WhippetBallItem::new, new Item.Settings().maxCount(1));
	/** What the Guv'nor hands over for beating his dog. There is no recipe for one. */
	public static final Item RACING_TROPHY = register("racing_trophy", Item::new, new Item.Settings().maxCount(1).rarity(net.minecraft.util.Rarity.RARE));

	private ModItems() {
	}

	private static Item register(String name, Function<Item.Settings, Item> factory, Item.Settings settings) {
		RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Whippets.id(name));
		return Registry.register(Registries.ITEM, key, factory.apply(settings.registryKey(key)));
	}

	public static void initialize() {
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(entries -> {
			entries.add(WHIPPET_SPAWN_EGG);
			entries.add(SQUIRREL_SPAWN_EGG);
		});
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
			entries.add(WHIPPET_WHISTLE);
			entries.add(WHIPPET_LURE);
			entries.add(WHIPPET_BALL);
			entries.add(RACING_TROPHY);
		});
	}
}
