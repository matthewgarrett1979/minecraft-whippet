package dev.whippet.whippets.world;

import dev.whippet.whippets.ModEntities;
import dev.whippet.whippets.ModItems;
import dev.whippet.whippets.entity.WhippetEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

/**
 * The little track, out in open country: thirty-six blocks of run between two
 * rails, four traps at one end, a lure on a post at the other, a stand down one
 * side for people to lean on, and a hut with the club's gear in it.
 *
 * <p>There are dogs living on it. Nobody knows whose they are.
 *
 * <p>It is built block by block rather than stamped out of a saved file, which
 * means it levels its own ground, comes out slightly different every time, and
 * does not need a single binary asset — the same rule the rest of this mod is
 * built on.
 *
 * <p>It is a structure rather than a feature, which matters more than it sounds.
 * A feature may only write to the chunk it is called for and the ring of chunks
 * around it; anything further off is thrown away with an error in the log. A
 * track is forty-three blocks long, so laid from a random spot inside a chunk it
 * ran off the end of what it was allowed to write and came up missing its
 * finish. A structure is called once per chunk with everything outside that
 * chunk clipped, so the whole thing goes down however it falls across the grid.
 */
public class TrackPiece extends StructurePiece {
	/** The run itself: long enough for the better dog to tell. */
	public static final int TRACK_LENGTH = 36;
	/** Half the width of the running surface, so the track is seven wide. */
	private static final int TRACK_HALF = 3;
	/** How much ground is levelled either side of the rails. */
	private static final int APRON = 5;
	/** How far down it will build up to level ground on a slope. */
	private static final int FOUNDATION = 8;
	/** How far above the surface the site is cleared. */
	private static final int HEADROOM = 6;

	/** The site, measured from the traps end and the middle of the track. */
	private static final int FROM_ALONG = -3;
	private static final int TO_ALONG = TRACK_LENGTH + 3;
	private static final int FROM_ACROSS = -TRACK_HALF - 4;
	private static final int TO_ACROSS = TRACK_HALF + 7;
	/** How far up the piece the running surface sits, leaving room for footings. */
	public static final int FLOOR = FOUNDATION + 1;
	public static final int HEIGHT = FLOOR + HEADROOM;

	/** Which way round this one was laid, so it never looks stamped. */
	private final boolean alongX;

	public TrackPiece(int x, int level, int z, boolean alongX) {
		super(ModStructures.TRACK_PIECE, 0, boxFor(x, level, z, alongX));
		this.alongX = alongX;
		this.setOrientation(Direction.SOUTH);
	}

	public TrackPiece(NbtCompound nbt) {
		super(ModStructures.TRACK_PIECE, nbt);
		this.alongX = nbt.getBoolean("AlongX", true);
	}

	@Override
	protected void writeNbt(StructureContext context, NbtCompound nbt) {
		nbt.putBoolean("AlongX", this.alongX);
	}

	private static BlockBox boxFor(int x, int level, int z, boolean alongX) {
		int minX = x + (alongX ? FROM_ALONG : FROM_ACROSS);
		int maxX = x + (alongX ? TO_ALONG : TO_ACROSS);
		int minZ = z + (alongX ? FROM_ACROSS : FROM_ALONG);
		int maxZ = z + (alongX ? TO_ACROSS : TO_ALONG);
		return new BlockBox(minX, level - FLOOR, minZ, maxX, level + HEADROOM - 1, maxZ);
	}

	/** The traps end of the track, in world coordinates: everything hangs off it. */
	public BlockPos start() {
		return new BlockPos(
			this.boundingBox.getMinX() - (this.alongX ? FROM_ALONG : FROM_ACROSS),
			this.boundingBox.getMinY() + FLOOR,
			this.boundingBox.getMinZ() - (this.alongX ? FROM_ACROSS : FROM_ALONG)
		);
	}

	@Override
	public void generate(
		StructureWorldAccess world,
		StructureAccessor structureAccessor,
		ChunkGenerator chunkGenerator,
		Random random,
		BlockBox chunkBox,
		ChunkPos chunkPos,
		BlockPos pivot
	) {
		BlockPos start = this.start();
		// Every chunk this site covers is built from the same seed, drawn in the
		// same order, so the patchy ground and the dogs come out the same
		// wherever the chunk seams happen to fall.
		Random site = Random.create(this.boundingBox.getMinX() * 341873128712L + this.boundingBox.getMinZ() * 132897987541L);

		this.clearAndLevel(world, chunkBox, start);
		this.layTrack(world, chunkBox, start, site);
		this.buildRails(world, chunkBox, start);
		this.buildTraps(world, chunkBox, start);
		this.buildFinish(world, chunkBox, start);
		this.buildStand(world, chunkBox, start);
		this.buildHut(world, chunkBox, start, site);
		this.putTheDogsOut(world, chunkBox, start, site);
	}

	/** Flattens the site: ground up to the level, everything above it cleared. */
	private void clearAndLevel(StructureWorldAccess world, BlockBox chunkBox, BlockPos start) {
		for (int along = FROM_ALONG; along <= TO_ALONG; along++) {
			for (int across = -APRON - 2; across <= APRON + 2; across++) {
				BlockPos surface = this.at(start, along, -1, across);

				// Whole columns outside this chunk are skipped rather than
				// clipped block by block, so nothing is ever read from a chunk
				// that has not been generated yet either.
				if (!this.inColumn(chunkBox, surface)) {
					continue;
				}

				this.put(world, chunkBox, surface, Blocks.GRASS_BLOCK.getDefaultState());

				// Build down to whatever is under it, so the site never ends up
				// standing on air where the ground fell away.
				for (int down = 1; down <= FOUNDATION; down++) {
					BlockPos below = surface.down(down);

					if (!world.getBlockState(below).isAir() && !world.getBlockState(below).isLiquid() && down > 2) {
						break;
					}

					this.put(world, chunkBox, below, Blocks.DIRT.getDefaultState());
				}

				for (int up = 0; up < HEADROOM; up++) {
					this.put(world, chunkBox, this.at(start, along, up, across), Blocks.AIR.getDefaultState());
				}
			}
		}
	}

	/**
	 * The running surface: bare trodden ground, with the odd rougher patch. It
	 * is laid as path rather than as dirt on purpose — nothing will seed in a
	 * path, so the grass that comes up everywhere else afterwards stops at the
	 * rails, and the track stays a track.
	 */
	private void layTrack(StructureWorldAccess world, BlockBox chunkBox, BlockPos start, Random site) {
		for (int along = 0; along <= TRACK_LENGTH; along++) {
			for (int across = -TRACK_HALF; across <= TRACK_HALF; across++) {
				// Drawn for every square whether this chunk wants it or not, so
				// the rough patches line up across the seams.
				BlockState surface = site.nextInt(7) == 0 ? Blocks.COARSE_DIRT.getDefaultState() : Blocks.DIRT_PATH.getDefaultState();
				this.put(world, chunkBox, this.at(start, along, -1, across), surface);
			}
		}

		// The line itself, across the track at the finish.
		for (int across = -TRACK_HALF; across <= TRACK_HALF; across++) {
			this.put(world, chunkBox, this.at(start, TRACK_LENGTH - 1, -1, across), Blocks.WHITE_CONCRETE.getDefaultState());
		}
	}

	/** Rails down both sides, with a lantern on a post every so often. */
	private void buildRails(StructureWorldAccess world, BlockBox chunkBox, BlockPos start) {
		for (int along = -1; along <= TRACK_LENGTH + 1; along++) {
			for (int side = -1; side <= 1; side += 2) {
				int across = side * (TRACK_HALF + 1);
				boolean post = Math.floorMod(along, 8) == 0;
				this.put(world, chunkBox, this.at(start, along, 0, across), Blocks.OAK_FENCE.getDefaultState());

				if (post) {
					this.put(world, chunkBox, this.at(start, along, 1, across), Blocks.OAK_FENCE.getDefaultState());
					this.put(world, chunkBox, this.at(start, along, 2, across), Blocks.LANTERN.getDefaultState());
				}
			}
		}
	}

	/** Four traps, side by side, facing down the track. */
	private void buildTraps(StructureWorldAccess world, BlockBox chunkBox, BlockPos start) {
		for (int trap = 0; trap < 4; trap++) {
			int across = trap * 2 - 3;

			for (int depth = -3; depth <= -1; depth++) {
				this.put(world, chunkBox, this.at(start, depth, 0, across - 1), Blocks.STONE_BRICKS.getDefaultState());
				this.put(world, chunkBox, this.at(start, depth, 1, across - 1), Blocks.STONE_BRICKS.getDefaultState());
			}

			// The back of the box, and a lid over it.
			this.put(world, chunkBox, this.at(start, -3, 0, across), Blocks.STONE_BRICKS.getDefaultState());
			this.put(world, chunkBox, this.at(start, -3, 1, across), Blocks.STONE_BRICKS.getDefaultState());

			for (int depth = -3; depth <= -1; depth++) {
				this.put(world, chunkBox, this.at(start, depth, 2, across), Blocks.STONE_BRICK_SLAB.getDefaultState());
				this.put(world, chunkBox, this.at(start, depth, 2, across - 1), Blocks.STONE_BRICK_SLAB.getDefaultState());
			}

			// The gate they come out of.
			this.put(world, chunkBox, this.at(start, -1, 0, across), Blocks.IRON_BARS.getDefaultState());
			this.put(world, chunkBox, this.at(start, -1, 1, across), Blocks.IRON_BARS.getDefaultState());
		}

		// The last wall on the end of the row.
		for (int depth = -3; depth <= -1; depth++) {
			this.put(world, chunkBox, this.at(start, depth, 0, 4), Blocks.STONE_BRICKS.getDefaultState());
			this.put(world, chunkBox, this.at(start, depth, 1, 4), Blocks.STONE_BRICKS.getDefaultState());
		}
	}

	/** The lure on its post, over the line, where the dogs are all looking. */
	private void buildFinish(StructureWorldAccess world, BlockBox chunkBox, BlockPos start) {
		int along = TRACK_LENGTH + 1;

		for (int side = -1; side <= 1; side += 2) {
			int across = side * (TRACK_HALF + 1);

			for (int up = 0; up < 4; up++) {
				this.put(world, chunkBox, this.at(start, along, up, across), Blocks.OAK_FENCE.getDefaultState());
			}
		}

		for (int across = -TRACK_HALF - 1; across <= TRACK_HALF + 1; across++) {
			this.put(world, chunkBox, this.at(start, along, 4, across), Blocks.OAK_FENCE.getDefaultState());
		}

		// The rag itself, hanging over the middle of the track.
		this.put(world, chunkBox, this.at(start, along, 3, 0), Blocks.WHITE_WOOL.getDefaultState());
		this.put(world, chunkBox, this.at(start, along, 2, 0), Blocks.RED_WOOL.getDefaultState());
	}

	/** Two steps of stand down one side, for leaning on and shouting from. */
	private void buildStand(StructureWorldAccess world, BlockBox chunkBox, BlockPos start) {
		int from = TRACK_LENGTH / 2 - 9;
		int to = TRACK_LENGTH / 2 + 9;
		Direction faces = this.across(-1);

		for (int along = from; along <= to; along++) {
			this.put(
				world, chunkBox, this.at(start, along, 0, -TRACK_HALF - 2), Blocks.STONE_BRICK_STAIRS.getDefaultState().with(StairsBlock.FACING, faces)
			);
			this.put(world, chunkBox, this.at(start, along, 0, -TRACK_HALF - 3), Blocks.STONE_BRICKS.getDefaultState());
			this.put(
				world, chunkBox, this.at(start, along, 1, -TRACK_HALF - 3), Blocks.STONE_BRICK_STAIRS.getDefaultState().with(StairsBlock.FACING, faces)
			);
			this.put(world, chunkBox, this.at(start, along, 1, -TRACK_HALF - 4), Blocks.STONE_BRICKS.getDefaultState());
		}
	}

	/** The hut: the club's gear lives in here, and so does the kettle. */
	private void buildHut(StructureWorldAccess world, BlockBox chunkBox, BlockPos start, Random site) {
		int alongFrom = 2;
		int acrossFrom = TRACK_HALF + 3;

		for (int along = alongFrom; along <= alongFrom + 5; along++) {
			for (int across = acrossFrom; across <= acrossFrom + 4; across++) {
				boolean wall = along == alongFrom || along == alongFrom + 5 || across == acrossFrom || across == acrossFrom + 4;

				for (int up = 0; up <= 2; up++) {
					BlockState state = wall ? Blocks.OAK_PLANKS.getDefaultState() : Blocks.AIR.getDefaultState();
					this.put(world, chunkBox, this.at(start, along, up, across), state);
				}

				this.put(world, chunkBox, this.at(start, along, 3, across), Blocks.OAK_SLAB.getDefaultState());

				if (!wall) {
					this.put(world, chunkBox, this.at(start, along, -1, across), Blocks.OAK_PLANKS.getDefaultState());
				}
			}
		}

		// A window on the track side, so you can see the racing from the kettle.
		this.put(world, chunkBox, this.at(start, alongFrom + 2, 1, acrossFrom), Blocks.GLASS_PANE.getDefaultState());
		this.put(world, chunkBox, this.at(start, alongFrom + 3, 1, acrossFrom), Blocks.GLASS_PANE.getDefaultState());

		// The door, at the far end from the traps.
		BlockPos door = this.at(start, alongFrom + 5, 0, acrossFrom + 2);
		Direction out = this.along(1);
		this.put(world, chunkBox, door, Blocks.OAK_DOOR.getDefaultState().with(DoorBlock.FACING, out).with(DoorBlock.HALF, DoubleBlockHalf.LOWER));
		this.put(world, chunkBox, door.up(), Blocks.OAK_DOOR.getDefaultState().with(DoorBlock.FACING, out).with(DoorBlock.HALF, DoubleBlockHalf.UPPER));

		this.put(world, chunkBox, this.at(start, alongFrom + 5, 2, acrossFrom + 4), Blocks.LANTERN.getDefaultState());

		// Outside: somewhere to stand about with a cup of tea while the dogs
		// are walked, which is most of what happens at a track.
		this.put(world, chunkBox, this.at(start, alongFrom + 7, 0, acrossFrom + 1), Blocks.CAMPFIRE.getDefaultState());
		this.put(world, chunkBox, this.at(start, alongFrom + 7, 0, acrossFrom + 3), Blocks.HAY_BLOCK.getDefaultState());
		this.put(world, chunkBox, this.at(start, alongFrom + 8, 0, acrossFrom + 3), Blocks.HAY_BLOCK.getDefaultState());
		this.put(world, chunkBox, this.at(start, alongFrom + 8, 1, acrossFrom + 3), Blocks.HAY_BLOCK.getDefaultState());
		this.put(world, chunkBox, this.at(start, alongFrom + 6, 0, acrossFrom + 4), Blocks.BARREL.getDefaultState());

		// A water trough for the dogs, because somebody thought of it.
		this.put(world, chunkBox, this.at(start, alongFrom - 1, -1, acrossFrom + 2), Blocks.CAULDRON.getDefaultState());

		// The gear. Drawn from the site's own seed either way, so the chunk the
		// chest happens to fall in gets the same kit every time.
		int rabbits = 2 + site.nextInt(4);
		int bones = 3 + site.nextInt(6);
		BlockPos chest = this.at(start, alongFrom + 1, 0, acrossFrom + 1);
		this.put(world, chunkBox, chest, Blocks.CHEST.getDefaultState().with(HorizontalFacingBlock.FACING, this.across(1)));

		if (chunkBox.contains(chest) && world.getBlockEntity(chest) instanceof ChestBlockEntity kit) {
			kit.setStack(0, new ItemStack(ModItems.WHIPPET_LURE));
			kit.setStack(1, new ItemStack(ModItems.WHIPPET_WHISTLE));
			kit.setStack(2, new ItemStack(ModItems.WHIPPET_BALL));
			kit.setStack(4, new ItemStack(Items.RABBIT, rabbits));
			kit.setStack(5, new ItemStack(Items.BONE, bones));
			kit.setStack(7, new ItemStack(Items.LEAD));
		}
	}

	/** The dogs that live here. Nobody has ever seen who feeds them. */
	private void putTheDogsOut(StructureWorldAccess world, BlockBox chunkBox, BlockPos start, Random site) {
		int dogs = 2 + site.nextInt(2);

		for (int i = 0; i < dogs; i++) {
			// Worked out for all of them, so each dog is put out by whichever
			// chunk it stands in and by that one only.
			BlockPos spot = this.at(start, 6 + site.nextInt(TRACK_LENGTH - 12), 0, site.nextInt(TRACK_HALF * 2 + 1) - TRACK_HALF);
			float facing = site.nextFloat() * 360.0F;

			if (!chunkBox.contains(spot)) {
				continue;
			}

			WhippetEntity whippet = ModEntities.WHIPPET.create(world.toServerWorld(), SpawnReason.STRUCTURE);

			if (whippet == null) {
				continue;
			}

			whippet.refreshPositionAndAngles(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, facing, 0.0F);
			whippet.initialize(world, world.getLocalDifficulty(spot), SpawnReason.STRUCTURE, null);
			whippet.setPersistent();
			world.spawnEntity(whippet);
		}
	}

	/** Track coordinates to world ones, whichever way round this track was laid. */
	private BlockPos at(BlockPos start, int along, int up, int across) {
		return this.alongX
			? start.add(along, up, across)
			: start.add(across, up, along);
	}

	private Direction along(int sign) {
		return this.alongX ? (sign > 0 ? Direction.EAST : Direction.WEST) : (sign > 0 ? Direction.SOUTH : Direction.NORTH);
	}

	private Direction across(int sign) {
		return this.alongX ? (sign > 0 ? Direction.SOUTH : Direction.NORTH) : (sign > 0 ? Direction.EAST : Direction.WEST);
	}

	/** Is this column inside the chunk being built? */
	private boolean inColumn(BlockBox chunkBox, BlockPos pos) {
		return pos.getX() >= chunkBox.getMinX()
			&& pos.getX() <= chunkBox.getMaxX()
			&& pos.getZ() >= chunkBox.getMinZ()
			&& pos.getZ() <= chunkBox.getMaxZ();
	}

	/**
	 * Lays one block, if it is in the chunk being built. Everything outside it
	 * belongs to another call, which will lay its own.
	 */
	private void put(StructureWorldAccess world, BlockBox chunkBox, BlockPos pos, BlockState state) {
		if (chunkBox.contains(pos)) {
			world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
		}
	}
}
