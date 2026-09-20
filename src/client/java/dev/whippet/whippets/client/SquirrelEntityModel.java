package dev.whippet.whippets.client;

import java.util.Set;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.entity.model.BabyModelTransformer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.render.entity.model.ModelTransformer;
import net.minecraft.util.math.MathHelper;

/**
 * A squirrel: a small hunched body, a head slightly too big for it, ears with
 * tufts on, and a tail worth more than the rest of the animal put together.
 */
public class SquirrelEntityModel extends EntityModel<SquirrelEntityRenderState> {
	// Half scale, three-quarter-scale head: the offsets put that head back on
	// the front of the shrunken body.
	public static final ModelTransformer BABY_TRANSFORMER = new BabyModelTransformer(
		true, 10.1F, 0.9F, 2.0F, 2.0F, 24.0F, Set.of(EntityModelPartNames.HEAD)
	);

	private static final String MUZZLE = "muzzle";
	private static final String RIGHT_EAR = "right_ear";
	private static final String LEFT_EAR = "left_ear";
	private static final String NUT = "nut";
	private static final String TAIL_TIP = "tail_tip";

	/** Where each part sits on a squirrel standing on all fours. */
	private static final float HEAD_Y = 17.6F;
	private static final float HEAD_Z = -2.6F;
	private static final float BODY_Y = 21.0F;
	private static final float BODY_Z = 0.0F;
	private static final float LEG_Y = 21.0F;
	private static final float FRONT_LEG_Z = -2.0F;
	private static final float HIND_LEG_Z = 1.8F;
	private static final float TAIL_Y = 19.5F;
	private static final float TAIL_Z = 3.0F;

	private final ModelPart head;
	private final ModelPart nut;
	private final ModelPart body;
	private final ModelPart rightFrontLeg;
	private final ModelPart leftFrontLeg;
	private final ModelPart rightHindLeg;
	private final ModelPart leftHindLeg;
	private final ModelPart tail;

	public SquirrelEntityModel(ModelPart root) {
		super(root);
		this.head = root.getChild(EntityModelPartNames.HEAD);
		this.nut = this.head.getChild(NUT);
		this.body = root.getChild(EntityModelPartNames.BODY);
		this.rightFrontLeg = root.getChild(EntityModelPartNames.RIGHT_FRONT_LEG);
		this.leftFrontLeg = root.getChild(EntityModelPartNames.LEFT_FRONT_LEG);
		this.rightHindLeg = root.getChild(EntityModelPartNames.RIGHT_HIND_LEG);
		this.leftHindLeg = root.getChild(EntityModelPartNames.LEFT_HIND_LEG);
		this.tail = root.getChild(EntityModelPartNames.TAIL);
	}

	public static ModelData getModelData(Dilation dilation) {
		ModelData modelData = new ModelData();
		ModelPartData root = modelData.getRoot();

		ModelPartData head = root.addChild(
			EntityModelPartNames.HEAD,
			ModelPartBuilder.create().uv(20, 0).cuboid(-2.0F, -3.0F, -3.0F, 4.0F, 4.0F, 4.0F, dilation),
			ModelTransform.origin(0.0F, HEAD_Y, HEAD_Z)
		);
		head.addChild(
			MUZZLE,
			ModelPartBuilder.create().uv(36, 0).cuboid(-1.0F, -1.0F, -4.0F, 2.0F, 2.0F, 1.0F, dilation),
			ModelTransform.NONE
		);
		// Ears up on top with a tuft, which is most of the cuteness.
		head.addChild(
			RIGHT_EAR,
			ModelPartBuilder.create().uv(44, 0).cuboid(-1.0F, -2.0F, -0.5F, 2.0F, 2.0F, 1.0F, dilation),
			ModelTransform.of(-1.2F, -3.0F, -1.0F, 0.0F, 0.0F, -0.25F)
		);
		head.addChild(
			LEFT_EAR,
			ModelPartBuilder.create().mirrored().uv(44, 0).cuboid(-1.0F, -2.0F, -0.5F, 2.0F, 2.0F, 1.0F, dilation),
			ModelTransform.of(1.2F, -3.0F, -1.0F, 0.0F, 0.0F, 0.25F)
		);
		// Whatever it is carrying, held in both hands at its mouth.
		head.addChild(
			NUT,
			ModelPartBuilder.create().uv(44, 12).cuboid(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, dilation),
			ModelTransform.origin(0.0F, 0.6F, -4.6F)
		);

		root.addChild(
			EntityModelPartNames.BODY,
			ModelPartBuilder.create().uv(0, 0).cuboid(-2.0F, -4.0F, -3.0F, 4.0F, 4.0F, 6.0F, dilation),
			ModelTransform.origin(0.0F, BODY_Y, BODY_Z)
		);

		ModelPartBuilder leg = ModelPartBuilder.create().uv(0, 12).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F, dilation);
		ModelPartBuilder mirroredLeg = ModelPartBuilder.create().mirrored().uv(0, 12).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F, dilation);
		root.addChild(EntityModelPartNames.RIGHT_FRONT_LEG, mirroredLeg, ModelTransform.origin(-1.2F, LEG_Y, FRONT_LEG_Z));
		root.addChild(EntityModelPartNames.LEFT_FRONT_LEG, leg, ModelTransform.origin(1.2F, LEG_Y, FRONT_LEG_Z));
		root.addChild(EntityModelPartNames.RIGHT_HIND_LEG, mirroredLeg, ModelTransform.origin(-1.3F, LEG_Y, HIND_LEG_Z));
		root.addChild(EntityModelPartNames.LEFT_HIND_LEG, leg, ModelTransform.origin(1.3F, LEG_Y, HIND_LEG_Z));

		// The tail: up off the rump and curled forward over the back in two
		// lengths, because one straight box is a rat.
		ModelPartData tail = root.addChild(
			EntityModelPartNames.TAIL,
			ModelPartBuilder.create().uv(10, 12).cuboid(-2.0F, 0.0F, -1.5F, 4.0F, 8.0F, 3.0F, dilation),
			ModelTransform.of(0.0F, TAIL_Y, TAIL_Z, 3.0F, 0.0F, 0.0F)
		);
		tail.addChild(
			TAIL_TIP,
			ModelPartBuilder.create().uv(26, 12).cuboid(-2.5F, 0.0F, -1.5F, 5.0F, 5.0F, 3.0F, dilation),
			ModelTransform.of(0.0F, 7.5F, 0.0F, 1.2F, 0.0F, 0.0F)
		);

		return modelData;
	}

	@Override
	public void setAngles(SquirrelEntityRenderState state) {
		super.setAngles(state);
		this.nut.visible = state.carrying;
		this.tail.pitch = state.tailAngle;

		if (state.climbing) {
			this.climb(state);
			return;
		}

		if (state.sittingUp || state.taunting) {
			this.sitUp(state);
		} else {
			this.bound(state.limbSwingAnimationProgress, state.limbSwingAmplitude);
		}

		this.head.pitch = state.pitch * (float)(Math.PI / 180.0);
		this.head.yaw = state.relativeHeadYaw * (float)(Math.PI / 180.0);
	}

	/**
	 * Squirrels bound: both front feet, then both back feet, with the spine
	 * arching over each hop. Nothing about it is a trot.
	 */
	private void bound(float limbSwing, float limbAmplitude) {
		float hop = MathHelper.cos(limbSwing * 0.9F) * 1.4F * limbAmplitude;
		float push = MathHelper.cos(limbSwing * 0.9F + 1.6F) * 1.4F * limbAmplitude;
		this.rightFrontLeg.pitch = hop;
		this.leftFrontLeg.pitch = hop;
		this.rightHindLeg.pitch = push;
		this.leftHindLeg.pitch = push;
		this.body.pitch = MathHelper.sin(limbSwing * 0.9F) * 0.18F * limbAmplitude;
		this.body.originY = BODY_Y - Math.abs(MathHelper.cos(limbSwing * 0.9F)) * 0.8F * limbAmplitude;
		this.head.originY = HEAD_Y - Math.abs(MathHelper.cos(limbSwing * 0.9F)) * 0.8F * limbAmplitude;
		this.tail.yaw = 0.0F;
	}

	/**
	 * Up on the haunches with both paws at the mouth — eating, or shouting at a
	 * dog, which from a squirrel's point of view are much the same activity.
	 */
	private void sitUp(SquirrelEntityRenderState state) {
		// The body swings upright on the rump, so its length becomes its height.
		this.body.pitch = -1.57F;
		moveTo(this.body, BODY_Y, BODY_Z, BODY_Y, -2.0F);
		moveTo(this.head, HEAD_Y, HEAD_Z, 16.0F, 1.0F);
		// Hocks folded flat along the ground, which is what it is sitting on.
		this.rightHindLeg.pitch = -1.57F;
		this.leftHindLeg.pitch = -1.57F;
		moveTo(this.rightHindLeg, LEG_Y, HIND_LEG_Z, 23.0F, 0.5F);
		moveTo(this.leftHindLeg, LEG_Y, HIND_LEG_Z, 23.0F, 0.5F);
		// Front paws up under the chin, holding whatever it is eating.
		this.rightFrontLeg.pitch = -2.2F;
		this.leftFrontLeg.pitch = -2.2F;
		moveTo(this.rightFrontLeg, LEG_Y, FRONT_LEG_Z, 19.6F, -1.0F);
		moveTo(this.leftFrontLeg, LEG_Y, FRONT_LEG_Z, 19.6F, -1.0F);

		if (state.taunting) {
			// The tail does the swearing.
			this.tail.yaw = MathHelper.cos(state.age * 0.9F) * 0.55F;
			this.head.pitch = -0.15F + MathHelper.cos(state.age * 0.9F) * 0.12F;
		} else {
			this.tail.yaw = MathHelper.cos(state.age * 0.1F) * 0.08F;
			// Turning the nut over in both hands.
			this.head.pitch = 0.2F + MathHelper.cos(state.age * 0.35F) * 0.08F;
		}

		this.head.yaw = state.relativeHeadYaw * (float)(Math.PI / 360.0);
	}

	/** Flat against the bark, all four feet splayed, going up. */
	private void climb(SquirrelEntityRenderState state) {
		float age = state.age;
		this.body.pitch = -1.45F;
		moveTo(this.body, BODY_Y, BODY_Z, 20.5F, -1.5F);
		moveTo(this.head, HEAD_Y, HEAD_Z, 16.2F, 0.5F);
		this.head.pitch = 0.45F;
		this.head.yaw = 0.0F;
		float scramble = MathHelper.cos(age * 0.8F) * 0.8F;
		this.rightFrontLeg.pitch = -1.4F + scramble;
		this.leftFrontLeg.pitch = -1.4F - scramble;
		this.rightHindLeg.pitch = -0.6F - scramble;
		this.leftHindLeg.pitch = -0.6F + scramble;
		moveTo(this.rightFrontLeg, LEG_Y, FRONT_LEG_Z, 19.0F, -1.5F);
		moveTo(this.leftFrontLeg, LEG_Y, FRONT_LEG_Z, 19.0F, -1.5F);
		moveTo(this.rightHindLeg, LEG_Y, HIND_LEG_Z, 22.0F, 0.5F);
		moveTo(this.leftHindLeg, LEG_Y, HIND_LEG_Z, 22.0F, 0.5F);
		this.rightFrontLeg.roll = -0.35F;
		this.leftFrontLeg.roll = 0.35F;
		// Straight down the trunk behind it, out of the way of its own feet —
		// and thrashing side to side if it is shouting at something below.
		this.tail.pitch = 0.2F;
		this.tail.yaw = MathHelper.cos(age * (state.taunting ? 0.9F : 0.4F)) * (state.taunting ? 0.6F : 0.15F);

		if (state.taunting) {
			this.head.pitch = 0.45F + MathHelper.cos(age * 0.9F) * 0.15F;
		}
	}

	/**
	 * Puts a part where an adult squirrel would carry it. Kits are the same model
	 * at half scale, so every offset has to shrink with them.
	 */
	private static void moveTo(ModelPart part, float homeY, float homeZ, float y, float z) {
		ModelTransform home = part.getDefaultTransform();
		part.originY = home.y() + (y - homeY) * part.yScale;
		part.originZ = home.z() + (z - homeZ) * part.zScale;
	}
}
