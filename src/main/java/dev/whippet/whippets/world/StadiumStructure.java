package dev.whippet.whippets.world;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

/**
 * Where a whippet stadium is allowed to be. It wants two hundred blocks by a
 * hundred and sixty of ground that is nearly level — which rules out most of
 * the world, and is why you do not find one behind every hill.
 */
public class StadiumStructure extends Structure {
	public static final MapCodec<StadiumStructure> CODEC = createCodec(StadiumStructure::new);
	/** The most the ground may rise and fall across the site before it is no good. */
	private static final int TOO_HILLY = 18;
	/** How many points across the site are sampled to decide that. */
	private static final int SAMPLES = 7;

	public StadiumStructure(Structure.Config config) {
		super(config);
	}

	@Override
	protected Optional<Structure.StructurePosition> getStructurePosition(Structure.Context context) {
		ChunkPos chunk = context.chunkPos();
		int originX = chunk.getStartX() - StadiumPiece.WIDTH / 2;
		int originZ = chunk.getStartZ() - StadiumPiece.DEPTH / 2;
		int lowest = Integer.MAX_VALUE;
		int highest = Integer.MIN_VALUE;
		int total = 0;

		for (int i = 0; i < SAMPLES; i++) {
			for (int j = 0; j < SAMPLES; j++) {
				int x = originX + i * (StadiumPiece.WIDTH - 1) / (SAMPLES - 1);
				int z = originZ + j * (StadiumPiece.DEPTH - 1) / (SAMPLES - 1);
				int height = context.chunkGenerator()
					.getHeightOnGround(x, z, Heightmap.Type.WORLD_SURFACE_WG, context.world(), context.noiseConfig());
				lowest = Math.min(lowest, height);
				highest = Math.max(highest, height);
				total += height;
			}
		}

		if (highest - lowest > TOO_HILLY) {
			return Optional.empty();
		}

		int level = total / (SAMPLES * SAMPLES);

		if (level <= context.chunkGenerator().getSeaLevel() + 1) {
			// Not on a beach, and certainly not in the sea.
			return Optional.empty();
		}

		BlockPos middle = new BlockPos(chunk.getStartX(), level, chunk.getStartZ());
		return Optional.of(new Structure.StructurePosition(middle, collector -> collector.addPiece(new StadiumPiece(originX, level, originZ))));
	}

	@Override
	public StructureType<?> getType() {
		return ModStructures.STADIUM;
	}
}
