package dev.whippet.whippets.client;

import dev.whippet.whippets.Whippets;
import net.minecraft.client.render.entity.model.EntityModelLayer;

/** Every model this mod puts in the world. */
public final class WhippetModelLayers {
	public static final EntityModelLayer WHIPPET = new EntityModelLayer(Whippets.id("whippet"), "main");
	public static final EntityModelLayer WHIPPET_BABY = new EntityModelLayer(Whippets.id("whippet_baby"), "main");

	public static final EntityModelLayer SQUIRREL = new EntityModelLayer(Whippets.id("squirrel"), "main");
	public static final EntityModelLayer SQUIRREL_BABY = new EntityModelLayer(Whippets.id("squirrel_baby"), "main");

	private WhippetModelLayers() {
	}
}
