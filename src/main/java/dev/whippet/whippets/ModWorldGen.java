package dev.whippet.whippets;

import dev.whippet.whippets.world.WhippetTrackFeature;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.PlacedFeature;

public final class ModWorldGen {
	/** The track itself: built block by block rather than stamped from a file. */
	public static final Feature<DefaultFeatureConfig> WHIPPET_TRACK = Registry.register(
		Registries.FEATURE, Whippets.id("whippet_track"), new WhippetTrackFeature(DefaultFeatureConfig.CODEC)
	);
	private static final RegistryKey<PlacedFeature> WHIPPET_TRACK_PLACED = RegistryKey.of(RegistryKeys.PLACED_FEATURE, Whippets.id("whippet_track"));

	private ModWorldGen() {
	}

	public static void initialize() {
		// Somebody built a track out here. It is in open country, which is the
		// only country a whippet track can be in, and there are dogs on it.
		BiomeModifications.addFeature(
			BiomeSelectors.includeByKey(
				BiomeKeys.PLAINS, BiomeKeys.SUNFLOWER_PLAINS, BiomeKeys.MEADOW, BiomeKeys.SAVANNA, BiomeKeys.SAVANNA_PLATEAU
			),
			GenerationStep.Feature.SURFACE_STRUCTURES,
			WHIPPET_TRACK_PLACED
		);

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
