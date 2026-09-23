package dev.whippet.whippets.world;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

/**
 * Where a little track is allowed to be: forty-odd blocks of ground flat enough
 * to run a dog down, which is most of a plain and none of a mountain.
 */
public class TrackStructure extends Structure {
	public static final MapCodec<TrackStructure> CODEC = createCodec(TrackStructure::new);
	/** Steeper than this and there is no track to be had here. */
	private static final int TOO_HILLY = 10;

	public TrackStructure(Structure.Config config) {
		super(config);
	}

	@Override
	protected Optional<Structure.StructurePosition> getStructurePosition(Structure.Context context) {
		ChunkPos chunk = context.chunkPos();
		// Which way round it is laid is settled here, once, and written into the
		// piece: every chunk that builds part of it has to agree.
		boolean alongX = context.random().nextBoolean();
		int startX = chunk.getStartX() - (alongX ? TrackPiece.TRACK_LENGTH / 2 : 1);
		int startZ = chunk.getStartZ() - (alongX ? 1 : TrackPiece.TRACK_LENGTH / 2);
		int lowest = Integer.MAX_VALUE;
		int highest = Integer.MIN_VALUE;
		int total = 0;
		int samples = 0;

		for (int along = 0; along <= TrackPiece.TRACK_LENGTH; along += 6) {
			for (int across = -5; across <= 5; across += 5) {
				int x = startX + (alongX ? along : across);
				int z = startZ + (alongX ? across : along);
				int height = context.chunkGenerator()
					.getHeightOnGround(x, z, Heightmap.Type.WORLD_SURFACE_WG, context.world(), context.noiseConfig());
				lowest = Math.min(lowest, height);
				highest = Math.max(highest, height);
				total += height;
				samples++;
			}
		}

		if (samples == 0 || highest - lowest > TOO_HILLY) {
			return Optional.empty();
		}

		int level = total / samples;

		if (level <= context.chunkGenerator().getSeaLevel() + 1) {
			// Not on a beach, and certainly not in the sea.
			return Optional.empty();
		}

		BlockPos middle = new BlockPos(chunk.getStartX(), level, chunk.getStartZ());
		return Optional.of(
			new Structure.StructurePosition(middle, collector -> collector.addPiece(new TrackPiece(startX, level, startZ, alongX)))
		);
	}

	@Override
	public StructureType<?> getType() {
		return ModStructures.TRACK;
	}
}
