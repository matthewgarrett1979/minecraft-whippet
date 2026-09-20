package dev.whippet.whippets.client;

import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

public class WhippetEntityRenderState extends LivingEntityRenderState {
	public Identifier texture = net.minecraft.util.Identifier.ofVanilla("textures/entity/wolf/wolf.png");
	public boolean inSittingPose;
	public boolean zooming;
	public boolean curled;
	public float begProgress;
	public boolean burrowed;
	public float tailAngle;
	public float tuckProgress;
	public @Nullable DyeColor collarColor;
}
