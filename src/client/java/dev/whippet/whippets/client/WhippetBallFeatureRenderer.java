package dev.whippet.whippets.client;

import dev.whippet.whippets.Whippets;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/** The ball, in the mouth, where it stays until the dog decides otherwise. */
public class WhippetBallFeatureRenderer extends FeatureRenderer<WhippetEntityRenderState, WhippetEntityModel> {
	private static final Identifier BALL_TEXTURE = Whippets.id("textures/entity/whippet/ball.png");

	private final ModelPart ball;

	public WhippetBallFeatureRenderer(FeatureRendererContext<WhippetEntityRenderState, WhippetEntityModel> context, EntityRendererFactory.Context ctx) {
		super(context);
		this.ball = ctx.getPart(WhippetModelLayers.WHIPPET_BALL);
	}

	/** A three-unit ball, which is about a tennis ball against a whippet's head. */
	public static TexturedModelData getModelData() {
		ModelData data = new ModelData();
		data.getRoot().addChild("ball", ModelPartBuilder.create().uv(0, 0).cuboid(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F, Dilation.NONE), net.minecraft.client.model.ModelTransform.NONE);
		return TexturedModelData.of(data, 16, 16);
	}

	@Override
	public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, WhippetEntityRenderState state, float limbAngle, float limbDistance) {
		if (!state.carryingBall || state.invisible) {
			return;
		}

		matrices.push();
		this.getContextModel().alignToMuzzle(matrices);
		queue.submitModelPart(
			this.ball,
			matrices,
			RenderLayers.entityCutoutNoCull(BALL_TEXTURE),
			light,
			OverlayTexture.DEFAULT_UV,
			null,
			false,
			false,
			-1,
			null,
			state.outlineColor
		);
		matrices.pop();
	}
}
