package dev.whippet.whippets;

import dev.whippet.whippets.race.RaceManager;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;

/**
 * Whippets: a tameable sighthound with a top speed, a taste for rabbits and a
 * daily appointment with the zoomies.
 */
public class Whippets implements ModInitializer {
	public static final String MOD_ID = "whippets";

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModEntities.initialize();
		ModItems.initialize();
		ModWorldGen.initialize();
		RaceManager.initialize();
	}
}
