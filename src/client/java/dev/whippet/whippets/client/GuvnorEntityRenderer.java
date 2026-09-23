package dev.whippet.whippets.client;

import dev.whippet.whippets.Whippets;
import dev.whippet.whippets.entity.GuvnorEntity;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.util.Identifier;

/** The Guv'nor, in his cap, exactly where he always is. */
public class GuvnorEntityRenderer extends BipedEntityRenderer<GuvnorEntity, BipedEntityRenderState, BipedEntityModel<BipedEntityRenderState>> {
	private static final Identifier TEXTURE = Whippets.id("textures/entity/guvnor.png");

	public GuvnorEntityRenderer(EntityRendererFactory.Context context) {
		super(context, new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER)), 0.5F);
	}

	@Override
	public Identifier getTexture(BipedEntityRenderState state) {
		return TEXTURE;
	}

	@Override
	public BipedEntityRenderState createRenderState() {
		return new BipedEntityRenderState();
	}
}
