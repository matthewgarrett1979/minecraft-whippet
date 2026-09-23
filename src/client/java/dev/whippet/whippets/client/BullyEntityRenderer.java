package dev.whippet.whippets.client;

import dev.whippet.whippets.Whippets;
import dev.whippet.whippets.entity.BullyEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class BullyEntityRenderer extends MobEntityRenderer<BullyEntity, BullyEntityRenderState, BullyEntityModel> {
	private static final Identifier TEXTURE = Whippets.id("textures/entity/bully/bully.png");

	public BullyEntityRenderer(EntityRendererFactory.Context context) {
		super(context, new BullyEntityModel(context.getPart(WhippetModelLayers.BULLY)), 0.9F);
	}

	/**
	 * The model is drawn at the size a very large dog is drawn at, and then
	 * everything is multiplied by a third again on top of that, because an XL
	 * Bully is not a very large dog. At this scale it stands over a player and
	 * the whippets come up to its elbow.
	 */
	private static final float XL = 1.35F;

	@Override
	protected void scale(BullyEntityRenderState state, MatrixStack matrices) {
		super.scale(state, matrices);
		matrices.scale(XL, XL, XL);
	}

	@Override
	public Identifier getTexture(BullyEntityRenderState state) {
		return TEXTURE;
	}

	@Override
	public BullyEntityRenderState createRenderState() {
		return new BullyEntityRenderState();
	}

	@Override
	public void updateRenderState(BullyEntity bully, BullyEntityRenderState state, float tickProgress) {
		super.updateRenderState(bully, state, tickProgress);
		state.lungeProgress = bully.lungeProgress(tickProgress);
	}
}
