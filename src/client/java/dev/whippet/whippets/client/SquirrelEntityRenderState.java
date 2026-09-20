package dev.whippet.whippets.client;

import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.util.Identifier;

public class SquirrelEntityRenderState extends LivingEntityRenderState {
	public Identifier texture = net.minecraft.util.Identifier.ofVanilla("textures/entity/fox/fox.png");
	public boolean sittingUp;
	public boolean taunting;
	public boolean climbing;
	public boolean carrying;
	public float tailAngle;
}
