package dev.whippet.whippets;

import dev.whippet.whippets.entity.WhippetEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnLocationTypes;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.Heightmap;

public final class ModEntities {
	public static final RegistryKey<EntityType<?>> WHIPPET_KEY = RegistryKey.of(RegistryKeys.ENTITY_TYPE, Whippets.id("whippet"));

	public static final EntityType<WhippetEntity> WHIPPET = Registry.register(
		Registries.ENTITY_TYPE,
		WHIPPET_KEY,
		FabricEntityType.Builder.createMob(
				WhippetEntity::new,
				SpawnGroup.CREATURE,
				mob -> mob.spawnRestriction(
						SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, AnimalEntity::isValidNaturalSpawn
					)
					.defaultAttributes(WhippetEntity::createWhippetAttributes)
			)
			.dimensions(0.6F, 1.0F)
			.eyeHeight(0.92F)
			.passengerAttachments(1.1F)
			.maxTrackingRange(10)
			.build(WHIPPET_KEY)
	);

	private ModEntities() {
	}

	public static void initialize() {
	}
}
