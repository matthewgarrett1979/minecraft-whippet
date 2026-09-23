package dev.whippet.whippets.race;

import dev.whippet.whippets.entity.GuvnorEntity;
import dev.whippet.whippets.entity.WhippetEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

/**
 * Keeps track of where each player has pegged their lure and runs whatever
 * races are in progress. Lures are remembered for the session only — peg it
 * again after a restart.
 */
public final class RaceManager {
	/** How far from the player a whippet can be and still be called to the traps. */
	public static final double CALL_RANGE = 24.0;
	/** The longest track the lure will accept. */
	public static final double MAX_TRACK = 128.0;

	private static final Map<UUID, Lure> LURES = new HashMap<>();
	private static final List<WhippetRace> RACES = new ArrayList<>();

	private RaceManager() {
	}

	public static void initialize() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			Iterator<WhippetRace> races = RACES.iterator();

			while (races.hasNext()) {
				if (races.next().tick()) {
					races.remove();
				}
			}
		});
	}

	public static void setLure(ServerPlayerEntity player, Vec3d pos) {
		LURES.put(player.getUuid(), new Lure(player.getEntityWorld().getRegistryKey().getValue().toString(), pos));
	}

	public static @Nullable Vec3d getLure(ServerPlayerEntity player) {
		Lure lure = LURES.get(player.getUuid());

		if (lure == null || !lure.dimension.equals(player.getEntityWorld().getRegistryKey().getValue().toString())) {
			return null;
		}

		return lure.pos;
	}

	public static boolean isRacing(ServerPlayerEntity player) {
		return RACES.stream().anyMatch(race -> race.isOwnedBy(player));
	}

	public static void start(ServerPlayerEntity player, ServerWorld world, Vec3d start, Vec3d finish, List<WhippetEntity> pack) {
		start(player, world, start, finish, pack, null);
	}

	/** The same, but run by somebody who settles up afterwards. */
	public static void start(
		ServerPlayerEntity player,
		ServerWorld world,
		Vec3d start,
		Vec3d finish,
		List<WhippetEntity> pack,
		@Nullable GuvnorEntity promoter
	) {
		RACES.add(new WhippetRace(player, world, start, finish, pack, promoter));
	}

	private record Lure(String dimension, Vec3d pos) {
	}
}
