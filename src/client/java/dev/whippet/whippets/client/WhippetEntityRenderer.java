package dev.whippet.whippets.client;

import dev.whippet.whippets.entity.WhippetEntity;
import net.minecraft.client.render.entity.AgeableMobEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;

public class WhippetEntityRenderer extends AgeableMobEntityRenderer<WhippetEntity, WhippetEntityRenderState, WhippetEntityModel> {
	public WhippetEntityRenderer(EntityRendererFactory.Context context) {
		super(
			context,
			new WhippetEntityModel(context.getPart(WhippetModelLayers.WHIPPET)),
			new WhippetEntityModel(context.getPart(WhippetModelLayers.WHIPPET_BABY)),
			0.4F
		);
		this.addFeature(new WhippetCollarFeatureRenderer(this));
	}

	@Override
	public Identifier getTexture(WhippetEntityRenderState state) {
		return state.texture;
	}

	@Override
	public WhippetEntityRenderState createRenderState() {
		return new WhippetEntityRenderState();
	}

	@Override
	public void updateRenderState(WhippetEntity whippet, WhippetEntityRenderState state, float tickProgress) {
		super.updateRenderState(whippet, state, tickProgress);
		state.texture = whippet.getTextureId();
		state.inSittingPose = whippet.isInSittingPose();
		state.zooming = whippet.isZooming();
		state.tailAngle = whippet.getTailAngle();
		state.tuckProgress = whippet.getTuckProgress(tickProgress);
		state.collarColor = whippet.isTamed() ? whippet.getCollarColor() : null;
	}
}
