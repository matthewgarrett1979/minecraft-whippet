package dev.whippet.whippets.client;

import dev.whippet.whippets.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.TexturedModelData;

public class WhippetsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityModelLayerRegistry.registerModelLayer(
			WhippetModelLayers.WHIPPET, () -> TexturedModelData.of(WhippetEntityModel.getModelData(Dilation.NONE), 64, 64)
		);
		EntityModelLayerRegistry.registerModelLayer(
			WhippetModelLayers.WHIPPET_BABY,
			() -> TexturedModelData.of(WhippetEntityModel.getModelData(Dilation.NONE), 64, 64).transform(WhippetEntityModel.BABY_TRANSFORMER)
		);
		EntityRendererRegistry.register(ModEntities.WHIPPET, WhippetEntityRenderer::new);
	}
}
