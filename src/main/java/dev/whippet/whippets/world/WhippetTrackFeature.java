package dev.whippet.whippets.world;

import com.mojang.serialization.Codec;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * A whippet track, out in open country: forty-eight blocks of run between two
 * rails, four traps at one end, a lure on a post at the other, a stand down one
 * side for people to lean on, and a hut with the club's gear in it.
 *
 * <p>There are dogs living on it. Nobody knows whose they are.
 *
 * <p>It is built block by block rather than stamped out of a saved file, which
 * means it levels its own ground, comes out slightly different every time, and
 * does not need a single binary asset — the same rule the rest of this mod is
 * built on.
 */
public class WhippetTrackFeature extends Feature<DefaultFeatureConfig> {
	/**
	 * The run itself. Thirty-six blocks: long enough for the better dog to tell,
	 * and short enough that the whole site fits inside the three chunks a
	 * feature is allowed to write to, so it is never found half built.
	 */
	private static final int TRACK_LENGTH = 36;
	/** Half the width of the running surface, so the track is seven wide. */
	private static final int TRACK_HALF = 3;
	/** How much ground is levelled either side of the rails. */
	private static final int APRON = 5;
	/** Steeper than this and there is no track to be had here. */
	private static final int TOO_HILLY = 10;
	/** How far down it will build up to level ground on a slope. */
	private static final int FOUNDATION = 8;
	/** How far above the surface the site is cleared. */
	private static final int HEADROOM = 6;

	public WhippetTrackFeature(Codec<DefaultFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		Random random = context.getRandom();
		BlockPos origin = context.getOrigin();
		// The track runs along one of the two axes, so it never looks stamped.
		boolean alongX = random.nextBoolean();
		int level = this.levelOf(world, origin, alongX);

		if (level == Integer.MIN_VALUE) {
			return false;
		}

		BlockPos start = new BlockPos(origin.getX(), level, origin.getZ());
		this.clearAndLevel(world, start, alongX);
		this.layTrack(world, start, alongX, random);
		this.buildRails(world, start, alongX);
		this.buildTraps(world, start, alongX);
		this.buildFinish(world, start, alongX);
		this.buildStand(world, start, alongX);
		this.buildHut(world, start, alongX, random);
		this.putTheDogsOut(world, start, alongX, random);
		return true;
	}

	/**
	 * The height the whole thing is built at, or nothing if the ground here is
	 * no use: a track has to be flat, and flattening a hillside would leave a
	 * cliff down one side of it.
	 */
	private int levelOf(StructureWorldAccess world, BlockPos origin, boolean alongX) {
		int lowest = Integer.MAX_VALUE;
		int highest = Integer.MIN_VALUE;
		int total = 0;
		int samples = 0;

		for (int along = 0; along <= TRACK_LENGTH; along += 6) {
			for (int across = -APRON; across <= APRON; across += 6) {
				BlockPos spot = this.at(origin, alongX, along, 0, across);
				int height = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, spot.getX(), spot.getZ());
				lowest = Math.min(lowest, height);
				highest = Math.max(highest, height);
				total += height;
				samples++;
			}
		}

		if (samples == 0 || highest - lowest > TOO_HILLY) {
			return Integer.MIN_VALUE;
		}

		return total / samples;
	}

	/** Flattens the site: ground up to the level, everything above it cleared. */
	private void clearAndLevel(StructureWorldAccess world, BlockPos start, boolean alongX) {
		for (int along = -3; along <= TRACK_LENGTH + 3; along++) {
			for (int across = -APRON - 2; across <= APRON + 2; across++) {
				BlockPos surface = this.at(start, alongX, along, -1, across);
				world.setBlockState(surface, Blocks.GRASS_BLOCK.getDefaultState(), Block.NOTIFY_LISTENERS);

				// Build down to whatever is under it, so the site never ends up
				// standing on air where the ground fell away.
				for (int down = 1; down <= FOUNDATION; down++) {
					BlockPos below = surface.down(down);

					if (!world.getBlockState(below).isAir() && !world.getBlockState(below).isLiquid() && down > 2) {
						break;
					}

					world.setBlockState(below, Blocks.DIRT.getDefaultState(), Block.NOTIFY_LISTENERS);
				}

				for (int up = 0; up < HEADROOM; up++) {
					world.setBlockState(this.at(start, alongX, along, up, across), Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
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
	private void layTrack(StructureWorldAccess world, BlockPos start, boolean alongX, Random random) {
		for (int along = 0; along <= TRACK_LENGTH; along++) {
			for (int across = -TRACK_HALF; across <= TRACK_HALF; across++) {
				BlockState surface = random.nextInt(7) == 0 ? Blocks.COARSE_DIRT.getDefaultState() : Blocks.DIRT_PATH.getDefaultState();
				world.setBlockState(this.at(start, alongX, along, -1, across), surface, Block.NOTIFY_LISTENERS);
			}
		}

		// The line itself, across the track at the finish.
		for (int across = -TRACK_HALF; across <= TRACK_HALF; across++) {
			world.setBlockState(this.at(start, alongX, TRACK_LENGTH - 1, -1, across), Blocks.WHITE_CONCRETE.getDefaultState(), Block.NOTIFY_LISTENERS);
		}
	}

	/** Rails down both sides, with a lantern on a post every so often. */
	private void buildRails(StructureWorldAccess world, BlockPos start, boolean alongX) {
		for (int along = -1; along <= TRACK_LENGTH + 1; along++) {
			for (int side = -1; side <= 1; side += 2) {
				int across = side * (TRACK_HALF + 1);
				boolean post = Math.floorMod(along, 8) == 0;
				world.setBlockState(this.at(start, alongX, along, 0, across), Blocks.OAK_FENCE.getDefaultState(), Block.NOTIFY_LISTENERS);

				if (post) {
					world.setBlockState(this.at(start, alongX, along, 1, across), Blocks.OAK_FENCE.getDefaultState(), Block.NOTIFY_LISTENERS);
					world.setBlockState(this.at(start, alongX, along, 2, across), Blocks.LANTERN.getDefaultState(), Block.NOTIFY_LISTENERS);
				}
			}
		}
	}

	/** Four traps, side by side, facing down the track. */
	private void buildTraps(StructureWorldAccess world, BlockPos start, boolean alongX) {
		for (int trap = 0; trap < 4; trap++) {
			int across = trap * 2 - 3;

			for (int depth = -3; depth <= -1; depth++) {
				world.setBlockState(this.at(start, alongX, depth, 0, across - 1), Blocks.STONE_BRICKS.getDefaultState(), Block.NOTIFY_LISTENERS);
				world.setBlockState(this.at(start, alongX, depth, 1, across - 1), Blocks.STONE_BRICKS.getDefaultState(), Block.NOTIFY_LISTENERS);
			}

			// The back of the box, and a lid over it.
			world.setBlockState(this.at(start, alongX, -3, 0, across), Blocks.STONE_BRICKS.getDefaultState(), Block.NOTIFY_LISTENERS);
			world.setBlockState(this.at(start, alongX, -3, 1, across), Blocks.STONE_BRICKS.getDefaultState(), Block.NOTIFY_LISTENERS);

			for (int depth = -3; depth <= -1; depth++) {
				world.setBlockState(this.at(start, alongX, depth, 2, across), Blocks.STONE_BRICK_SLAB.getDefaultState(), Block.NOTIFY_LISTENERS);
				world.setBlockState(this.at(start, alongX, depth, 2, across - 1), Blocks.STONE_BRICK_SLAB.getDefaultState(), Block.NOTIFY_LISTENERS);
			}

			// The gate they come out of.
			world.setBlockState(this.at(start, alongX, -1, 0, across), Blocks.IRON_BARS.getDefaultState(), Block.NOTIFY_LISTENERS);
			world.setBlockState(this.at(start, alongX, -1, 1, across), Blocks.IRON_BARS.getDefaultState(), Block.NOTIFY_LISTENERS);
		}

		// The last wall on the end of the row.
		for (int depth = -3; depth <= -1; depth++) {
			world.setBlockState(this.at(start, alongX, depth, 0, 4), Blocks.STONE_BRICKS.getDefaultState(), Block.NOTIFY_LISTENERS);
			world.setBlockState(this.at(start, alongX, depth, 1, 4), Blocks.STONE_BRICKS.getDefaultState(), Block.NOTIFY_LISTENERS);
		}
	}

	/** The lure on its post, over the line, where the dogs are all looking. */
	private void buildFinish(StructureWorldAccess world, BlockPos start, boolean alongX) {
		int along = TRACK_LENGTH + 1;

		for (int side = -1; side <= 1; side += 2) {
			int across = side * (TRACK_HALF + 1);

			for (int up = 0; up < 4; up++) {
				world.setBlockState(this.at(start, alongX, along, up, across), Blocks.OAK_FENCE.getDefaultState(), Block.NOTIFY_LISTENERS);
			}
		}

		for (int across = -TRACK_HALF - 1; across <= TRACK_HALF + 1; across++) {
			world.setBlockState(this.at(start, alongX, along, 4, across), Blocks.OAK_FENCE.getDefaultState(), Block.NOTIFY_LISTENERS);
		}

		// The rag itself, hanging over the middle of the track.
		world.setBlockState(this.at(start, alongX, along, 3, 0), Blocks.WHITE_WOOL.getDefaultState(), Block.NOTIFY_LISTENERS);
		world.setBlockState(this.at(start, alongX, along, 2, 0), Blocks.RED_WOOL.getDefaultState(), Block.NOTIFY_LISTENERS);
	}

	/** Two steps of stand down one side, for leaning on and shouting from. */
	private void buildStand(StructureWorldAccess world, BlockPos start, boolean alongX) {
		int from = TRACK_LENGTH / 2 - 9;
		int to = TRACK_LENGTH / 2 + 9;
		Direction faces = this.across(alongX, -1);

		for (int along = from; along <= to; along++) {
			world.setBlockState(
				this.at(start, alongX, along, 0, -TRACK_HALF - 2),
				Blocks.STONE_BRICK_STAIRS.getDefaultState().with(StairsBlock.FACING, faces),
				Block.NOTIFY_LISTENERS
			);
			world.setBlockState(this.at(start, alongX, along, 0, -TRACK_HALF - 3), Blocks.STONE_BRICKS.getDefaultState(), Block.NOTIFY_LISTENERS);
			world.setBlockState(
				this.at(start, alongX, along, 1, -TRACK_HALF - 3),
				Blocks.STONE_BRICK_STAIRS.getDefaultState().with(StairsBlock.FACING, faces),
				Block.NOTIFY_LISTENERS
			);
			world.setBlockState(this.at(start, alongX, along, 1, -TRACK_HALF - 4), Blocks.STONE_BRICKS.getDefaultState(), Block.NOTIFY_LISTENERS);
		}
	}

	/** The hut: the club's gear lives in here, and so does the kettle. */
	private void buildHut(StructureWorldAccess world, BlockPos start, boolean alongX, Random random) {
		int alongFrom = 2;
		int acrossFrom = TRACK_HALF + 3;

		for (int along = alongFrom; along <= alongFrom + 5; along++) {
			for (int across = acrossFrom; across <= acrossFrom + 4; across++) {
				boolean wall = along == alongFrom || along == alongFrom + 5 || across == acrossFrom || across == acrossFrom + 4;

				for (int up = 0; up <= 2; up++) {
					BlockState state = wall ? Blocks.OAK_PLANKS.getDefaultState() : Blocks.AIR.getDefaultState();
					world.setBlockState(this.at(start, alongX, along, up, across), state, Block.NOTIFY_LISTENERS);
				}

				world.setBlockState(this.at(start, alongX, along, 3, across), Blocks.OAK_SLAB.getDefaultState(), Block.NOTIFY_LISTENERS);

				if (!wall) {
					world.setBlockState(this.at(start, alongX, along, -1, across), Blocks.OAK_PLANKS.getDefaultState(), Block.NOTIFY_LISTENERS);
				}
			}
		}

		// A window on the track side, so you can see the racing from the kettle.
		world.setBlockState(this.at(start, alongX, alongFrom + 2, 1, acrossFrom), Blocks.GLASS_PANE.getDefaultState(), Block.NOTIFY_LISTENERS);
		world.setBlockState(this.at(start, alongX, alongFrom + 3, 1, acrossFrom), Blocks.GLASS_PANE.getDefaultState(), Block.NOTIFY_LISTENERS);

		// The door, at the far end from the traps.
		BlockPos door = this.at(start, alongX, alongFrom + 5, 0, acrossFrom + 2);
		Direction out = this.along(alongX, 1);
		world.setBlockState(door, Blocks.OAK_DOOR.getDefaultState().with(DoorBlock.FACING, out).with(DoorBlock.HALF, DoubleBlockHalf.LOWER), Block.NOTIFY_LISTENERS);
		world.setBlockState(
			door.up(), Blocks.OAK_DOOR.getDefaultState().with(DoorBlock.FACING, out).with(DoorBlock.HALF, DoubleBlockHalf.UPPER), Block.NOTIFY_LISTENERS
		);

		BlockPos lamp = this.at(start, alongX, alongFrom + 5, 2, acrossFrom + 4);
		world.setBlockState(lamp, Blocks.LANTERN.getDefaultState(), Block.NOTIFY_LISTENERS);

		// Outside: somewhere to stand about with a cup of tea while the dogs
		// are walked, which is most of what happens at a track.
		world.setBlockState(this.at(start, alongX, alongFrom + 7, 0, acrossFrom + 1), Blocks.CAMPFIRE.getDefaultState(), Block.NOTIFY_LISTENERS);
		world.setBlockState(this.at(start, alongX, alongFrom + 7, 0, acrossFrom + 3), Blocks.HAY_BLOCK.getDefaultState(), Block.NOTIFY_LISTENERS);
		world.setBlockState(this.at(start, alongX, alongFrom + 8, 0, acrossFrom + 3), Blocks.HAY_BLOCK.getDefaultState(), Block.NOTIFY_LISTENERS);
		world.setBlockState(this.at(start, alongX, alongFrom + 8, 1, acrossFrom + 3), Blocks.HAY_BLOCK.getDefaultState(), Block.NOTIFY_LISTENERS);
		world.setBlockState(this.at(start, alongX, alongFrom + 6, 0, acrossFrom + 4), Blocks.BARREL.getDefaultState(), Block.NOTIFY_LISTENERS);

		// A water trough for the dogs, because somebody thought of it.
		world.setBlockState(this.at(start, alongX, alongFrom - 1, -1, acrossFrom + 2), Blocks.CAULDRON.getDefaultState(), Block.NOTIFY_LISTENERS);

		// The gear.
		BlockPos chest = this.at(start, alongX, alongFrom + 1, 0, acrossFrom + 1);
		world.setBlockState(chest, Blocks.CHEST.getDefaultState().with(HorizontalFacingBlock.FACING, this.across(alongX, 1)), Block.NOTIFY_LISTENERS);

		if (world.getBlockEntity(chest) instanceof ChestBlockEntity kit) {
			kit.setStack(0, new ItemStack(ModItems.WHIPPET_LURE));
			kit.setStack(1, new ItemStack(ModItems.WHIPPET_WHISTLE));
			kit.setStack(2, new ItemStack(ModItems.WHIPPET_BALL));
			kit.setStack(4, new ItemStack(Items.RABBIT, 2 + random.nextInt(4)));
			kit.setStack(5, new ItemStack(Items.BONE, 3 + random.nextInt(6)));
			kit.setStack(7, new ItemStack(Items.LEAD));
		}
	}

	/** The dogs that live here. Nobody has ever seen who feeds them. */
	private void putTheDogsOut(StructureWorldAccess world, BlockPos start, boolean alongX, Random random) {
		int dogs = 2 + random.nextInt(2);

		for (int i = 0; i < dogs; i++) {
			WhippetEntity whippet = ModEntities.WHIPPET.create(world.toServerWorld(), SpawnReason.STRUCTURE);

			if (whippet == null) {
				continue;
			}

			BlockPos spot = this.at(start, alongX, 6 + random.nextInt(TRACK_LENGTH - 12), 0, random.nextInt(TRACK_HALF * 2 + 1) - TRACK_HALF);
			whippet.refreshPositionAndAngles(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
			whippet.initialize(world, world.getLocalDifficulty(spot), SpawnReason.STRUCTURE, null);
			whippet.setPersistent();
			world.spawnEntity(whippet);
		}
	}

	/** Track coordinates to world ones, whichever way round this track was laid. */
	private BlockPos at(BlockPos start, boolean alongX, int along, int up, int across) {
		return alongX
			? start.add(along, up, across)
			: start.add(across, up, along);
	}

	private Direction along(boolean alongX, int sign) {
		return alongX ? (sign > 0 ? Direction.EAST : Direction.WEST) : (sign > 0 ? Direction.SOUTH : Direction.NORTH);
	}

	private Direction across(boolean alongX, int sign) {
		return alongX ? (sign > 0 ? Direction.SOUTH : Direction.NORTH) : (sign > 0 ? Direction.EAST : Direction.WEST);
	}
}
