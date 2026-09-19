package dev.whippet.whippets.client;

import dev.whippet.whippets.Whippets;
import net.minecraft.client.render.entity.model.EntityModelLayer;

public final class WhippetModelLayers {
	public static final EntityModelLayer WHIPPET = new EntityModelLayer(Whippets.id("whippet"), "main");
	public static final EntityModelLayer WHIPPET_BABY = new EntityModelLayer(Whippets.id("whippet_baby"), "main");

	private WhippetModelLayers() {
	}
}
