package dev.whippet.whippets.world;

import dev.whippet.whippets.ModEntities;
import dev.whippet.whippets.entity.GuvnorEntity;
import dev.whippet.whippets.entity.WhippetEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.entity.SpawnReason;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

/**
 * The stadium.
 *
 * <p>Modelled on the Emirates and built to something like its size: a shallow
 * oval bowl about two hundred blocks by a hundred and sixty, a hundred and
 * twenty by eighty-four of track inside it, two tiers of red seating raked one
 * in two all the way round, a concourse between them, a quartz facade with
 * arched openings, a roof ring over the back of the stand and four floodlight
 * masts. On the home straight there are six traps and a finish line under a
 * gantry.
 *
 * <p>It is built block by block from the geometry below rather than stamped out
 * of a saved file, like everything else in this mod. The one thing that made it
 * possible at this size is that it is a structure rather than a feature:
 * structures are handed one chunk at a time and everything outside that chunk
 * is clipped, so a build this big goes down over a hundred and forty chunks
 * without any of it being lost at the seams.
 */
public class StadiumPiece extends StructurePiece {
	/** The whole site, in blocks. */
	public static final int WIDTH = 206;
	public static final int DEPTH = 172;
	public static final int HEIGHT = 52;
	private static final int CX = WIDTH / 2;
	private static final int CZ = DEPTH / 2;
	/** How far up the piece the ground sits, leaving room for footings below it. */
	public static final int FLOOR = 10;

	/** The track's outer ellipse: a hundred and twenty blocks by eighty-four. */
	private static final double A = 60.0;
	private static final double B = 42.0;
	/** The running surface, in blocks, measured in from that ellipse. */
	private static final double TRACK_WIDTH = 9.0;
	/** Where the stand starts, measured out from the track's outer edge. */
	private static final double FRONT_ROW = 3.0;
	private static final double TIER_ONE = 14.0;
	private static final double CONCOURSE = 18.0;
	private static final double TIER_TWO = 34.0;
	private static final double FACADE = 39.0;
	/** The rake: one block up for every two out, which is what a stand looks like. */
	private static final double RAKE = 0.5;
	private static final int TIER_TWO_LIFT = 3;
	private static final int FACADE_TOP = 27;
	private static final int ROOF = 30;
	private static final int MAST_TOP = 44;
	/** The four ways in through the stand. */
	private static final int TUNNEL_HEIGHT = 4;
	private static final double TUNNEL_WIDTH = 5.0;
	/** The home straight: how far off the middle the traps and the line sit. */
	private static final int STRAIGHT_Z = 37;
	private static final int TRAP_X = -30;
	private static final int FINISH_X = 28;

	private static final BlockState SEATS = Blocks.RED_CONCRETE.getDefaultState();
	private static final BlockState STEPS = Blocks.LIGHT_GRAY_CONCRETE.getDefaultState();
	private static final BlockState FRAME = Blocks.SMOOTH_QUARTZ.getDefaultState();
	private static final BlockState PILLAR = Blocks.QUARTZ_PILLAR.getDefaultState();
	private static final BlockState ROOFING = Blocks.LIGHT_GRAY_CONCRETE.getDefaultState();
	private static final BlockState TRACK = Blocks.DIRT_PATH.getDefaultState();
	private static final BlockState WORN = Blocks.COARSE_DIRT.getDefaultState();
	/**
	 * The pitch. Concrete rather than turf, and not for the look of it: the
	 * world plants its trees and long grass after the structures go down, so a
	 * grass infield comes up with an oak wood in the middle of it. Nothing
	 * seeds in concrete, so the pitch stays mown.
	 */
	private static final BlockState INFIELD = Blocks.GREEN_CONCRETE.getDefaultState();
	private static final BlockState PITCH_LINE = Blocks.LIME_CONCRETE.getDefaultState();
	private static final BlockState RAIL = Blocks.DIORITE_WALL.getDefaultState();
	private static final BlockState LINE = Blocks.WHITE_CONCRETE.getDefaultState();
	private static final BlockState FOOTING = Blocks.STONE_BRICKS.getDefaultState();
	private static final BlockState APRON = Blocks.POLISHED_ANDESITE.getDefaultState();
	/** The dog to beat. */
	private static final String CHAMPION_NAME = "Bobby Brazil";

	public StadiumPiece(int x, int groundLevel, int z) {
		super(ModStructures.STADIUM_PIECE, 0, new BlockBox(x, groundLevel - FLOOR, z, x + WIDTH - 1, groundLevel - FLOOR + HEIGHT - 1, z + DEPTH - 1));
		this.setOrientation(Direction.SOUTH);
	}

	public StadiumPiece(NbtCompound nbt) {
		super(ModStructures.STADIUM_PIECE, nbt);
	}

	@Override
	protected void writeNbt(StructureContext context, NbtCompound nbt) {
	}

	/** Where the middle of the pitch is in the world, which other things hang off. */
	public BlockPos centre() {
		return new BlockPos(this.boundingBox.getMinX() + CX, this.boundingBox.getMinY() + FLOOR, this.boundingBox.getMinZ() + CZ);
	}

	/** The finish line, in world coordinates: where the lure goes and the crowd looks. */
	public BlockPos finishLine() {
		return this.centre().add(FINISH_X, 0, STRAIGHT_Z);
	}

	/** The traps, in world coordinates. */
	public BlockPos traps() {
		return this.centre().add(TRAP_X, 0, STRAIGHT_Z);
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
		int originX = this.boundingBox.getMinX();
		int originY = this.boundingBox.getMinY();
		int originZ = this.boundingBox.getMinZ();
		// Only the part of the site inside this chunk is worth walking over.
		int fromX = Math.max(0, chunkBox.getMinX() - originX);
		int toX = Math.min(WIDTH - 1, chunkBox.getMaxX() - originX);
		int fromZ = Math.max(0, chunkBox.getMinZ() - originZ);
		int toZ = Math.min(DEPTH - 1, chunkBox.getMaxZ() - originZ);

		for (int x = fromX; x <= toX; x++) {
			for (int z = fromZ; z <= toZ; z++) {
				this.column(world, chunkBox, random, originX + x, originY, originZ + z, x - CX, z - CZ);
			}
		}

		this.trapsAndLine(world, chunkBox, originX, originY, originZ);
		this.floodlights(world, chunkBox, originX, originY, originZ);
		this.staff(world, chunkBox);
	}

	/**
	 * The people who come with the place: the Guv'nor on his box by the line,
	 * his own dog on the pitch, and a couple of others knocking about. Spawned
	 * once, with the chunk that holds the finish, or there would be a Guv'nor
	 * for every chunk the stadium covers.
	 */
	private void staff(StructureWorldAccess world, BlockBox chunkBox) {
		BlockPos finish = this.finishLine();

		if (!chunkBox.contains(finish)) {
			return;
		}

		BlockPos box = finish.add(2, 4, 8);
		GuvnorEntity guvnor = ModEntities.GUVNOR.create(world.toServerWorld(), SpawnReason.STRUCTURE);

		if (guvnor != null) {
			guvnor.refreshPositionAndAngles(box.getX() + 0.5, box.getY(), box.getZ() + 0.5, 180.0F, 0.0F);
			guvnor.takeCharge(finish, this.traps());
			guvnor.setCustomName(Text.translatable("entity.whippets.guvnor"));
			world.spawnEntity(guvnor);
		}

		// Bobby Brazil, on the pitch, waiting for somebody to bring him a race.
		BlockPos paddock = finish.add(-4, 0, -12);
		WhippetEntity bobby = ModEntities.WHIPPET.create(world.toServerWorld(), SpawnReason.STRUCTURE);

		if (bobby != null) {
			bobby.refreshPositionAndAngles(paddock.getX() + 0.5, paddock.getY(), paddock.getZ() + 0.5, 0.0F, 0.0F);
			bobby.initialize(world, world.getLocalDifficulty(paddock), SpawnReason.STRUCTURE, null);
			bobby.makeChampion(this.centre());
			bobby.setCustomName(Text.literal(CHAMPION_NAME));
			bobby.setCustomNameVisible(true);
			world.spawnEntity(bobby);
		}

		// And a couple of the stadium's own, loose on the pitch.
		for (int i = 0; i < 2; i++) {
			WhippetEntity dog = ModEntities.WHIPPET.create(world.toServerWorld(), SpawnReason.STRUCTURE);

			if (dog == null) {
				continue;
			}

			BlockPos spot = paddock.add(i * 5 - 8, 0, -6);
			dog.refreshPositionAndAngles(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 0.0F, 0.0F);
			dog.initialize(world, world.getLocalDifficulty(spot), SpawnReason.STRUCTURE, null);
			dog.setPersistent();
			world.spawnEntity(dog);
		}
	}

	/**
	 * Everything that stands on one square of ground: which ring of the bowl it
	 * is in decides whether it is track, infield, seating, concourse, facade or
	 * the apron outside.
	 */
	private void column(StructureWorldAccess world, BlockBox chunkBox, Random random, int wx, int originY, int wz, int dx, int dz) {
		double r = Math.sqrt(sq(dx / A) + sq(dz / B));
		// How many blocks one unit of that ellipse is worth here, so that
		// everything measured outwards comes out the same width all the way
		// round instead of pinching on the bends.
		double scale = r < 1.0E-4 ? A : Math.sqrt((double)dx * dx + (double)dz * dz) / r;
		double out = (r - 1.0) * scale;
		int ground = originY + FLOOR;

		if (out > FACADE + 6.0) {
			return;
		}

		this.clearAbove(world, chunkBox, wx, ground, wz);

		if (out < -TRACK_WIDTH) {
			// Mown in stripes, the way a groundsman would.
			this.surface(world, chunkBox, wx, ground, wz, Math.floorMod(dx, 12) < 6 ? INFIELD : PITCH_LINE);
			return;
		}

		if (out < 0.0) {
			// The running surface, worn where the dogs take the bend tightest.
			this.surface(world, chunkBox, wx, ground, wz, random.nextInt(9) == 0 ? WORN : TRACK);
			return;
		}

		if (out < FRONT_ROW) {
			// The rail, and the strip of concrete behind it.
			this.surface(world, chunkBox, wx, ground, wz, APRON);

			if (out < 1.0) {
				this.put(world, chunkBox, wx, ground + 1, wz, RAIL);
			}

			return;
		}

		double seating = out - FRONT_ROW;

		boolean tunnel = this.inTunnel(dx, dz);

		if (seating < TIER_ONE) {
			int rise = (int)(seating * RAKE);

			if (tunnel && rise < TUNNEL_HEIGHT + 1) {
				// The mouth of the tunnel, opening onto the track.
				this.surface(world, chunkBox, wx, ground, wz, APRON);
				return;
			}

			this.stand(world, chunkBox, wx, ground, wz, rise, (int)seating % 2 == 0 ? SEATS : STEPS, tunnel);
			return;
		}

		if (seating < CONCOURSE) {
			this.stand(world, chunkBox, wx, ground, wz, (int)(TIER_ONE * RAKE), FRAME, tunnel);
			return;
		}

		if (seating < TIER_TWO) {
			int rise = (int)(CONCOURSE * RAKE) + TIER_TWO_LIFT + (int)((seating - CONCOURSE) * RAKE);
			this.stand(world, chunkBox, wx, ground, wz, rise, (int)seating % 2 == 0 ? SEATS : STEPS, tunnel);
			this.roof(world, chunkBox, wx, ground, wz, seating);
			return;
		}

		if (seating < FACADE) {
			if (tunnel) {
				// The way in from outside.
				this.surface(world, chunkBox, wx, ground, wz, APRON);

				for (int air = ground + 1; air < ground + TUNNEL_HEIGHT; air++) {
					this.put(world, chunkBox, wx, air, wz, Blocks.AIR.getDefaultState());
				}

				this.put(world, chunkBox, wx, ground + TUNNEL_HEIGHT, wz, FRAME);
				this.roof(world, chunkBox, wx, ground, wz, seating);
				return;
			}

			// The wall the whole thing is wrapped in, with its arches.
			this.facade(world, chunkBox, wx, ground, wz, dx, dz);
			this.roof(world, chunkBox, wx, ground, wz, seating);
			return;
		}

		this.surface(world, chunkBox, wx, ground, wz, APRON);
	}

	/** Air from the ground up, so the bowl is not full of hillside. */
	private void clearAbove(StructureWorldAccess world, BlockBox chunkBox, int wx, int ground, int wz) {
		for (int y = ground; y < ground + HEIGHT - FLOOR; y++) {
			this.put(world, chunkBox, wx, y, wz, Blocks.AIR.getDefaultState());
		}
	}

	/** One square of ground, with footings taken down to whatever is under it. */
	private void surface(StructureWorldAccess world, BlockBox chunkBox, int wx, int ground, int wz, BlockState top) {
		this.put(world, chunkBox, wx, ground, wz, top);

		for (int y = ground - 1; y >= ground - FLOOR; y--) {
			BlockPos pos = new BlockPos(wx, y, wz);

			if (!chunkBox.contains(pos)) {
				continue;
			}

			if (!world.getBlockState(pos).isAir() && y < ground - 2) {
				break;
			}

			world.setBlockState(pos, y > ground - 3 ? Blocks.DIRT.getDefaultState() : FOOTING, Block.NOTIFY_LISTENERS);
		}
	}

	/**
	 * A step of the stand at the height its rake has reached, sitting on the
	 * quartz underneath it — except where a tunnel goes through, which is how
	 * anybody gets in. Four of them, one at each quarter, and each one comes
	 * out at the front of the stand beside the track.
	 */
	private void stand(StructureWorldAccess world, BlockBox chunkBox, int wx, int ground, int wz, int rise, BlockState top, boolean tunnel) {
		int y = ground + rise;
		this.put(world, chunkBox, wx, y, wz, top);
		int floor = tunnel && rise >= TUNNEL_HEIGHT + 1 ? ground + TUNNEL_HEIGHT : ground - 2;

		for (int fill = y - 1; fill >= floor; fill--) {
			this.put(world, chunkBox, wx, fill, wz, fill % 6 == 0 ? FRAME : Blocks.SMOOTH_QUARTZ.getDefaultState());
		}

		if (tunnel && rise >= TUNNEL_HEIGHT + 1) {
			this.put(world, chunkBox, wx, ground, wz, APRON);

			for (int air = ground + 1; air < ground + TUNNEL_HEIGHT; air++) {
				this.put(world, chunkBox, wx, air, wz, Blocks.AIR.getDefaultState());
			}
		}
	}

	/**
	 * Whether this column is in one of the four ways in. Measured on the angle
	 * round the bowl, so the tunnel stays the same width the whole way through
	 * instead of fanning out.
	 */
	private boolean inTunnel(int dx, int dz) {
		double distance = Math.sqrt((double)dx * dx + (double)dz * dz);

		if (distance < 1.0) {
			return false;
		}

		double angle = Math.atan2(dz, dx);

		for (int quarter = 0; quarter < 4; quarter++) {
			double centre = quarter * Math.PI / 2.0 + Math.PI / 4.0;
			double off = Math.abs(Math.atan2(Math.sin(angle - centre), Math.cos(angle - centre))) * distance;

			if (off < TUNNEL_WIDTH / 2.0) {
				return true;
			}
		}

		return false;
	}

	/** The outer wall: quartz, with an arched opening every eight blocks. */
	private void facade(StructureWorldAccess world, BlockBox chunkBox, int wx, int ground, int wz, int dx, int dz) {
		int top = ground + FACADE_TOP;
		// Which way round the bowl this column is, so the arches are evenly spaced.
		double angle = Math.atan2(dz, dx);
		double station = angle * 30.0;
		boolean opening = Math.floorMod((int)Math.round(station), 8) < 4;

		for (int y = ground; y <= top; y++) {
			boolean arch = opening && y > ground + 1 && y < ground + 10;
			BlockState state = arch ? Blocks.AIR.getDefaultState() : (y % 9 == 0 ? PILLAR : FRAME);
			this.put(world, chunkBox, wx, y, wz, state);
		}

		this.surface(world, chunkBox, wx, ground, wz, APRON);
		this.put(world, chunkBox, wx, ground + 11, wz, Blocks.SEA_LANTERN.getDefaultState());
	}

	/** The roof ring, out over the back of the stand where the rain comes in. */
	private void roof(StructureWorldAccess world, BlockBox chunkBox, int wx, int ground, int wz, double seating) {
		int y = ground + ROOF;
		this.put(world, chunkBox, wx, y, wz, ROOFING);

		if (seating < CONCOURSE + 2.0) {
			this.put(world, chunkBox, wx, y, wz, Blocks.GLASS.getDefaultState());
		}
	}

	/** The traps at one end of the home straight and the line at the other. */
	private void trapsAndLine(StructureWorldAccess world, BlockBox chunkBox, int originX, int originY, int originZ) {
		int ground = originY + FLOOR;
		int cx = originX + CX;
		int cz = originZ + CZ;

		// Six traps, side by side, facing down the straight.
		for (int trap = 0; trap < 6; trap++) {
			int z = cz + STRAIGHT_Z - 4 + trap;

			for (int depth = 0; depth < 3; depth++) {
				int x = cx + TRAP_X - 3 + depth;
				this.put(world, chunkBox, x, ground + 1, z, FOOTING);
				this.put(world, chunkBox, x, ground + 2, z, FOOTING);
			}

			this.put(world, chunkBox, cx + TRAP_X, ground + 1, z, Blocks.IRON_BARS.getDefaultState());
			this.put(world, chunkBox, cx + TRAP_X, ground + 2, z, Blocks.IRON_BARS.getDefaultState());
			this.put(world, chunkBox, cx + TRAP_X - 4, ground + 1, z, FOOTING);
			this.put(world, chunkBox, cx + TRAP_X - 4, ground + 2, z, FOOTING);
		}

		// The line itself, and the gantry over it.
		for (int z = cz + STRAIGHT_Z - 5; z <= cz + STRAIGHT_Z + 5; z++) {
			this.put(world, chunkBox, cx + FINISH_X, ground, z, LINE);
		}

		for (int y = ground + 1; y <= ground + 5; y++) {
			this.put(world, chunkBox, cx + FINISH_X, y, cz + STRAIGHT_Z - 6, PILLAR);
			this.put(world, chunkBox, cx + FINISH_X, y, cz + STRAIGHT_Z + 6, PILLAR);
		}

		for (int z = cz + STRAIGHT_Z - 6; z <= cz + STRAIGHT_Z + 6; z++) {
			this.put(world, chunkBox, cx + FINISH_X, ground + 6, z, FRAME);
		}

		this.put(world, chunkBox, cx + FINISH_X, ground + 5, cz + STRAIGHT_Z, Blocks.WHITE_WOOL.getDefaultState());
		this.put(world, chunkBox, cx + FINISH_X, ground + 4, cz + STRAIGHT_Z, Blocks.RED_WOOL.getDefaultState());

		// The box the stadium is run from, beside the line.
		int boxX = cx + FINISH_X + 2;
		int boxZ = cz + STRAIGHT_Z + 8;

		for (int x = boxX - 2; x <= boxX + 2; x++) {
			for (int z = boxZ - 1; z <= boxZ + 1; z++) {
				this.put(world, chunkBox, x, ground + 3, z, FRAME);
				this.put(world, chunkBox, x, ground + 4, z, Blocks.AIR.getDefaultState());
			}
		}

		for (int x = boxX - 2; x <= boxX + 2; x++) {
			this.put(world, chunkBox, x, ground + 4, boxZ + 1, Blocks.QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH).with(StairsBlock.HALF, BlockHalf.TOP));
		}
	}

	/** Four masts, at the corners, so racing does not stop at dusk. */
	private void floodlights(StructureWorldAccess world, BlockBox chunkBox, int originX, int originY, int originZ) {
		int ground = originY + FLOOR;

		for (int corner = 0; corner < 4; corner++) {
			int sx = (corner & 1) == 0 ? -1 : 1;
			int sz = (corner & 2) == 0 ? -1 : 1;
			int x = originX + CX + (int)(sx * (A + FACADE - 4) * 0.72);
			int z = originZ + CZ + (int)(sz * (B + FACADE - 4) * 0.80);

			for (int y = ground; y <= ground + MAST_TOP; y++) {
				this.put(world, chunkBox, x, y, z, PILLAR);
			}

			for (int lx = -2; lx <= 2; lx++) {
				for (int lz = -2; lz <= 2; lz++) {
					if (Math.abs(lx) + Math.abs(lz) <= 3) {
						this.put(world, chunkBox, x + lx, ground + MAST_TOP + 1, z + lz, Blocks.SEA_LANTERN.getDefaultState());
					}
				}
			}
		}
	}

	private void put(StructureWorldAccess world, BlockBox chunkBox, int x, int y, int z, BlockState state) {
		BlockPos pos = new BlockPos(x, y, z);

		if (chunkBox.contains(pos)) {
			world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
		}
	}

	private static double sq(double v) {
		return v * v;
	}

	/** Used by the structure to decide whether this ground will take a stadium. */
	public static Heightmap.Type groundType() {
		return Heightmap.Type.WORLD_SURFACE_WG;
	}
}
