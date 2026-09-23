package dev.whippet.whippets;

import dev.whippet.whippets.entity.GuvnorEntity;
import dev.whippet.whippets.entity.SquirrelEntity;
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

	public static final RegistryKey<EntityType<?>> SQUIRREL_KEY = RegistryKey.of(RegistryKeys.ENTITY_TYPE, Whippets.id("squirrel"));

	public static final EntityType<SquirrelEntity> SQUIRREL = Registry.register(
		Registries.ENTITY_TYPE,
		SQUIRREL_KEY,
		FabricEntityType.Builder.createMob(
				SquirrelEntity::new,
				SpawnGroup.CREATURE,
				mob -> mob.spawnRestriction(
						SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, AnimalEntity::isValidNaturalSpawn
					)
					.defaultAttributes(SquirrelEntity::createSquirrelAttributes)
			)
			.dimensions(0.5F, 0.6F)
			.eyeHeight(0.45F)
			.maxTrackingRange(8)
			.build(SQUIRREL_KEY)
	);

	public static final RegistryKey<EntityType<?>> GUVNOR_KEY = RegistryKey.of(RegistryKeys.ENTITY_TYPE, Whippets.id("guvnor"));

	/** The man who runs the stadium. One to a stadium, and he does not spawn wild. */
	public static final EntityType<GuvnorEntity> GUVNOR = Registry.register(
		Registries.ENTITY_TYPE,
		GUVNOR_KEY,
		FabricEntityType.Builder.createMob(GuvnorEntity::new, SpawnGroup.MISC, mob -> mob.defaultAttributes(GuvnorEntity::createGuvnorAttributes))
			.dimensions(0.6F, 1.95F)
			.eyeHeight(1.74F)
			.maxTrackingRange(10)
			.build(GUVNOR_KEY)
	);

	private ModEntities() {
	}

	public static void initialize() {
	}
}
