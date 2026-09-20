package dev.whippet.whippets;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class ModSounds {
	/**
	 * The honk. Whippets have a flat, nasal, two-tone shout they use when they
	 * want something and have decided that whining is not getting through, and
	 * it sounds uncannily like a goose. Four takes, so a nagging dog does not
	 * loop.
	 */
	public static final SoundEvent WHIPPET_HONK = register("entity.whippet.honk");

	private ModSounds() {
	}

	private static SoundEvent register(String path) {
		Identifier id = Whippets.id(path);
		RegistryKey<SoundEvent> key = RegistryKey.of(RegistryKeys.SOUND_EVENT, id);
		return Registry.register(Registries.SOUND_EVENT, key, SoundEvent.of(id));
	}

	public static void initialize() {
	}
}
