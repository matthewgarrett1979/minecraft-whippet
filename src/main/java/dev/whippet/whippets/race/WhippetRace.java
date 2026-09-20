package dev.whippet.whippets.race;

import dev.whippet.whippets.entity.WhippetEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * One race, from traps to finish.
 *
 * <p>The dogs are lined up abreast on the start line, held there for a
 * three-count, then released at the lure. The first one to reach it wins and
 * takes a lap of honour; everyone else gets a time as they cross.
 */
public class WhippetRace {
	/** Ticks between the countdown beeps. */
	private static final int COUNTDOWN_STEP = 20;
	private static final int COUNTDOWN_TICKS = COUNTDOWN_STEP * 3;
	/** A race is abandoned if nobody has finished by now. */
	private static final int TIME_LIMIT_TICKS = 20 * 60;
	/** How close a dog has to get to the lure to have caught it. */
	private static final double FINISH_RADIUS = 2.0;
	private static final double LANE_WIDTH = 1.6;

	private final ServerPlayerEntity owner;
	private final ServerWorld world;
	private final Vec3d finish;
	private final List<Racer> racers = new ArrayList<>();

	private int ticks;
	private int placed;

	public WhippetRace(ServerPlayerEntity owner, ServerWorld world, Vec3d start, Vec3d finish, List<WhippetEntity> pack) {
		this.owner = owner;
		this.world = world;
		this.finish = finish;

		Vec3d down = finish.subtract(start);
		float yaw = (float)(MathHelper.atan2(down.z, down.x) * 180.0 / Math.PI) - 90.0F;
		// Lanes run across the track, so the field lines up abreast of the lure.
		Vec3d across = new Vec3d(-down.z, 0.0, down.x).normalize();
		double offset = -(pack.size() - 1) * LANE_WIDTH / 2.0;

		for (WhippetEntity whippet : pack) {
			Vec3d lane = start.add(across.multiply(offset));
			whippet.enterTraps(finish, lane, yaw);
			this.racers.add(new Racer(whippet));
			offset += LANE_WIDTH;
		}
	}

	/** @return true once the race is over and the manager can drop it. */
	public boolean tick() {
		this.ticks++;

		if (this.ticks <= COUNTDOWN_TICKS) {
			this.countdown();
			return false;
		}

		if (this.ticks == COUNTDOWN_TICKS + 1) {
			this.release();
		}

		boolean anyRunning = false;

		for (Racer racer : this.racers) {
			WhippetEntity whippet = racer.whippet;

			if (racer.finishedAt > 0) {
				continue;
			}

			if (!whippet.isAlive() || whippet.isRemoved() || !whippet.isRacing()) {
				racer.finishedAt = -1;
				continue;
			}

			anyRunning = true;

			if (whippet.squaredDistanceTo(this.finish) <= FINISH_RADIUS * FINISH_RADIUS) {
				this.finish(racer);
			}
		}

		if (!anyRunning || this.ticks > COUNTDOWN_TICKS + TIME_LIMIT_TICKS) {
			this.end();
			return true;
		}

		return false;
	}

	private void countdown() {
		int remaining = (COUNTDOWN_TICKS - this.ticks) / COUNTDOWN_STEP + 1;

		if (this.ticks % COUNTDOWN_STEP == 0) {
			float pitch = 0.8F + (3 - remaining) * 0.1F;
			this.world.playSound(null, this.owner.getX(), this.owner.getY(), this.owner.getZ(), SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), SoundCategory.NEUTRAL, 1.0F, pitch);
			this.owner.sendMessage(Text.translatable("race.whippets.countdown", remaining), true);
		}
	}

	private void release() {
		this.world.playSound(
			null, this.owner.getX(), this.owner.getY(), this.owner.getZ(), SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.NEUTRAL, 1.0F, 1.6F
		);
		this.owner.sendMessage(Text.translatable("race.whippets.go").formatted(Formatting.GOLD), true);

		for (Racer racer : this.racers) {
			racer.whippet.leaveTraps();
		}
	}

	private void finish(Racer racer) {
		racer.finishedAt = this.ticks;
		this.placed++;
		int place = this.placed;
		String time = String.format("%.2f", (this.ticks - COUNTDOWN_TICKS) / 20.0);
		racer.whippet.stopRacing();

		if (place == 1) {
			// The winner is owed a lap of honour.
			racer.whippet.requestZoomies();
			this.world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, this.finish.x, this.finish.y + 0.8, this.finish.z, 12, 0.6, 0.4, 0.6, 0.0);
			this.announce(Text.translatable("race.whippets.winner", racer.whippet.getRaceName(), time).formatted(Formatting.GOLD));
		} else {
			this.announce(Text.translatable("race.whippets.finished", place, racer.whippet.getRaceName(), time));
		}
	}

	private void end() {
		for (Racer racer : this.racers) {
			racer.whippet.stopRacing();
		}

		if (this.placed == 0) {
			this.announce(Text.translatable("race.whippets.nobody").formatted(Formatting.GRAY));
		}
	}

	/** Race results go to everyone who can see the finish, not just the owner. */
	private void announce(Text message) {
		for (PlayerEntity player : this.world.getPlayers()) {
			if (player.squaredDistanceTo(this.finish) < 64.0 * 64.0 || player == this.owner) {
				player.sendMessage(message, false);
			}
		}
	}

	public boolean isOwnedBy(PlayerEntity player) {
		return this.owner == player;
	}

	private static class Racer {
		private final WhippetEntity whippet;
		/** Tick the dog crossed the line; -1 if it dropped out. */
		private int finishedAt;

		private Racer(WhippetEntity whippet) {
			this.whippet = whippet;
		}
	}
}
