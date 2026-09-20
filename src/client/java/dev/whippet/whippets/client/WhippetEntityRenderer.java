package dev.whippet.whippets.client;

import dev.whippet.whippets.entity.WhippetCoat;
import dev.whippet.whippets.entity.WhippetEntity;
import net.minecraft.client.render.entity.AgeableMobEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;

public class WhippetEntityRenderer extends AgeableMobEntityRenderer<WhippetEntity, WhippetEntityRenderState, WhippetEntityModel> {
	/** Name a whippet after the blue brindle this mod was drawn from and it looks the part. */
	private static final String BONNIE = "Bonnie";

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
	public void render(
		WhippetEntityRenderState state,
		net.minecraft.client.util.math.MatrixStack matrices,
		net.minecraft.client.render.command.OrderedRenderCommandQueue queue,
		net.minecraft.client.render.state.CameraRenderState camera
	) {
		if (state.burrowed) {
			// Down into the bedding, so only a nose and a lump are showing.
			matrices.push();
			matrices.translate(0.0F, -0.32F, 0.0F);
			super.render(state, matrices, queue, camera);
			matrices.pop();
			return;
		}

		super.render(state, matrices, queue, camera);
	}

	@Override
	public Identifier getTexture(WhippetEntityRenderState state) {
		return state.texture;
	}

	@Override
	public WhippetEntityRenderState createRenderState() {
		return new WhippetEntityRenderState();
	}

	private static boolean isNamedBonnie(WhippetEntity whippet) {
		return whippet.getCustomName() != null && BONNIE.equals(whippet.getCustomName().getString());
	}

	@Override
	public void updateRenderState(WhippetEntity whippet, WhippetEntityRenderState state, float tickProgress) {
		super.updateRenderState(whippet, state, tickProgress);
		state.texture = isNamedBonnie(whippet) ? WhippetCoat.BONNIE.getTexture() : whippet.getTextureId();
		state.inSittingPose = whippet.isInSittingPose();
		state.zooming = whippet.isZooming();
		state.curled = whippet.isCurled();
		state.begProgress = whippet.getBegProgress(tickProgress);
		state.burrowed = whippet.isBurrowed();
		state.tailAngle = whippet.getTailAngle();
		state.tuckProgress = whippet.getTuckProgress(tickProgress);
		state.collarColor = whippet.isTamed() ? whippet.getCollarColor() : null;
	}
}
