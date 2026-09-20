package dev.whippet.whippets.client;

import dev.whippet.whippets.entity.SquirrelEntity;
import net.minecraft.client.render.entity.AgeableMobEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;

public class SquirrelEntityRenderer extends AgeableMobEntityRenderer<SquirrelEntity, SquirrelEntityRenderState, SquirrelEntityModel> {
	public SquirrelEntityRenderer(EntityRendererFactory.Context context) {
		super(
			context,
			new SquirrelEntityModel(context.getPart(WhippetModelLayers.SQUIRREL)),
			new SquirrelEntityModel(context.getPart(WhippetModelLayers.SQUIRREL_BABY)),
			0.25F
		);
	}

	@Override
	public Identifier getTexture(SquirrelEntityRenderState state) {
		return state.texture;
	}

	@Override
	public SquirrelEntityRenderState createRenderState() {
		return new SquirrelEntityRenderState();
	}

	@Override
	public void updateRenderState(SquirrelEntity squirrel, SquirrelEntityRenderState state, float tickProgress) {
		super.updateRenderState(squirrel, state, tickProgress);
		state.texture = squirrel.getVariant().getTexture();
		state.sittingUp = squirrel.isSittingUp();
		state.taunting = squirrel.isTaunting();
		state.climbing = squirrel.isScrambling();
		state.carrying = squirrel.isCarrying();
		state.tailAngle = squirrel.getTailAngle();
	}
}
