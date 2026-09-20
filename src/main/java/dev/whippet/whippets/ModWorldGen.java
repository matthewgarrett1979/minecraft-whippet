package dev.whippet.whippets;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.world.biome.BiomeKeys;

public final class ModWorldGen {
	private ModWorldGen() {
	}

	public static void initialize() {
		// Squirrels want trees, and plenty of them.
		BiomeModifications.addSpawn(
			BiomeSelectors.includeByKey(
				BiomeKeys.FOREST,
				BiomeKeys.FLOWER_FOREST,
				BiomeKeys.BIRCH_FOREST,
				BiomeKeys.OLD_GROWTH_BIRCH_FOREST,
				BiomeKeys.DARK_FOREST,
				BiomeKeys.TAIGA,
				BiomeKeys.OLD_GROWTH_PINE_TAIGA,
				BiomeKeys.OLD_GROWTH_SPRUCE_TAIGA,
				BiomeKeys.WOODED_BADLANDS,
				BiomeKeys.GROVE
			),
			SpawnGroup.CREATURE,
			ModEntities.SQUIRREL,
			10,
			2,
			4
		);

		// Open, grassy country: room to run, and rabbits to run after.
		BiomeModifications.addSpawn(
			BiomeSelectors.includeByKey(
				BiomeKeys.PLAINS, BiomeKeys.SUNFLOWER_PLAINS, BiomeKeys.MEADOW, BiomeKeys.SAVANNA, BiomeKeys.SAVANNA_PLATEAU
			),
			SpawnGroup.CREATURE,
			ModEntities.WHIPPET,
			8,
			1,
			3
		);
	}
}
