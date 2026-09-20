package dev.whippet.whippets.client;

import dev.whippet.whippets.Whippets;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

/** The collar, dyed whatever colour the owner last shoved in the dog's face. */
public class WhippetCollarFeatureRenderer extends FeatureRenderer<WhippetEntityRenderState, WhippetEntityModel> {
	private static final Identifier COLLAR_TEXTURE = Whippets.id("textures/entity/whippet/whippet_collar.png");

	public WhippetCollarFeatureRenderer(FeatureRendererContext<WhippetEntityRenderState, WhippetEntityModel> context) {
		super(context);
	}

	@Override
	public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, WhippetEntityRenderState state, float limbAngle, float limbDistance) {
		DyeColor collarColor = state.collarColor;

		if (collarColor != null && !state.invisible) {
			queue.getBatchingQueue(1)
				.submitModel(
					this.getContextModel(),
					state,
					matrices,
					RenderLayers.entityCutoutNoCull(COLLAR_TEXTURE),
					light,
					OverlayTexture.DEFAULT_UV,
					collarColor.getEntityColor(),
					null,
					state.outlineColor,
					null
				);
		}
	}
}
