package dev.whippet.whippets.world;

import dev.whippet.whippets.Whippets;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.world.gen.structure.StructureType;

/** The stadium and the little track, as the world generator needs to be told about them. */
public final class ModStructures {
	public static final StructurePieceType STADIUM_PIECE = register("stadium", (StructurePieceType.Simple)StadiumPiece::new);
	public static final StructureType<StadiumStructure> STADIUM = Registry.register(
		Registries.STRUCTURE_TYPE, Whippets.id("stadium"), () -> StadiumStructure.CODEC
	);
	public static final StructurePieceType TRACK_PIECE = register("whippet_track", (StructurePieceType.Simple)TrackPiece::new);
	public static final StructureType<TrackStructure> TRACK = Registry.register(
		Registries.STRUCTURE_TYPE, Whippets.id("whippet_track"), () -> TrackStructure.CODEC
	);

	private ModStructures() {
	}

	private static StructurePieceType register(String name, StructurePieceType type) {
		return Registry.register(Registries.STRUCTURE_PIECE, Whippets.id(name), type);
	}

	public static void initialize() {
	}
}
