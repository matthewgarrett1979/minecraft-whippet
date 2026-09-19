package dev.whippet.whippets;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.world.biome.BiomeKeys;

public final class ModWorldGen {
	private ModWorldGen() {
	}

	public static void initialize() {
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
