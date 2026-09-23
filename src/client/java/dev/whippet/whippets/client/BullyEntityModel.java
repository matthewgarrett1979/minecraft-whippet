package dev.whippet.whippets.client;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.util.math.MathHelper;

/**
 * An XL Bully, which is very nearly a cube with a dog at each corner.
 *
 * <p>Everything about it is the opposite of the whippet in the next file. The
 * whippet is drawn out of lines — a long wedge of a head, a neck you could put
 * your hand round, legs like knitting needles. This is drawn out of blocks: the
 * head is as wide as the chest, the neck is shorter than the head is long and
 * thicker than either, and the legs are stumps set at the corners. At two
 * blocks to the shoulder and two and a half nose to tail it stands as tall as
 * you do and weighs what the four whippets it is looking at weigh together.
 */
public class BullyEntityModel extends EntityModel<BullyEntityRenderState> {
	private static final float HEAD_Y = 0.5F;
	private static final float HEAD_Z = -15.0F;
	private static final float NECK_Y = 1.0F;
	private static final float NECK_Z = -13.0F;
	private static final float BODY_Y = 6.5F;
	/** Short legs under a deep body: the daylight under an XL Bully is a slot. */
	private static final float LEG_Y = 14.0F;
	private static final float FRONT_LEG_Z = -9.5F;
	private static final float HIND_LEG_Z = 6.5F;
	private static final float TAIL_Y = 2.0F;
	private static final float TAIL_Z = 8.0F;
	private static final String MUZZLE = "muzzle";
	private static final String RIGHT_EAR = "right_ear";
	private static final String LEFT_EAR = "left_ear";

	private final ModelPart head;
	private final ModelPart neck;
	private final ModelPart body;
	private final ModelPart rightFrontLeg;
	private final ModelPart leftFrontLeg;
	private final ModelPart rightHindLeg;
	private final ModelPart leftHindLeg;
	private final ModelPart tail;

	public BullyEntityModel(ModelPart root) {
		super(root);
		this.head = root.getChild(EntityModelPartNames.HEAD);
		this.neck = root.getChild(EntityModelPartNames.NECK);
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

		// The head: a brick, eleven wide, with a short blunt muzzle on the front
		// of it and no stop worth drawing.
		ModelPartData head = root.addChild(
			EntityModelPartNames.HEAD,
			ModelPartBuilder.create().uv(0, 30).cuboid(-6.0F, -6.0F, -11.0F, 12.0F, 12.0F, 11.0F, dilation),
			ModelTransform.origin(0.0F, HEAD_Y, HEAD_Z)
		);
		head.addChild(MUZZLE, ModelPartBuilder.create().uv(80, 30).cuboid(-4.0F, -1.0F, -14.0F, 8.0F, 6.0F, 3.0F, dilation), ModelTransform.NONE);

		// Small ears set wide and folded forward, because there is nowhere on a
		// head that shape for a big ear to go.
		ModelPartBuilder ear = ModelPartBuilder.create().uv(94, 55).cuboid(-1.5F, -4.0F, -1.0F, 3.0F, 4.0F, 2.0F, dilation);
		head.addChild(RIGHT_EAR, ear, ModelTransform.of(-4.5F, -5.0F, -6.0F, -0.3F, -0.25F, -0.2F));
		head.addChild(LEFT_EAR, ear, ModelTransform.of(4.5F, -5.0F, -6.0F, -0.3F, 0.25F, 0.2F));

		// The neck: shorter than the head and thicker than the head, which is
		// the single thing that makes this dog read as what it is.
		root.addChild(
			EntityModelPartNames.NECK,
			ModelPartBuilder.create().uv(48, 30).cuboid(-5.0F, -5.0F, -5.0F, 10.0F, 10.0F, 5.0F, dilation),
			ModelTransform.of(0.0F, NECK_Y, NECK_Z, -0.2F, 0.0F, 0.0F)
		);

		// Chest first and deep, then a shorter, narrower loin: it is front-heavy
		// on purpose, the way the dog is.
		root.addChild(
			EntityModelPartNames.BODY,
			ModelPartBuilder.create()
				.uv(0, 0)
				.cuboid(-7.0F, -7.5F, -14.0F, 14.0F, 15.0F, 13.0F, dilation)
				.uv(56, 0)
				.cuboid(-6.0F, -6.5F, -1.0F, 12.0F, 13.0F, 11.0F, dilation),
			ModelTransform.origin(0.0F, BODY_Y, 0.0F)
		);

		ModelPartBuilder foreleg = ModelPartBuilder.create().uv(38, 55).cuboid(-3.0F, 0.0F, -3.0F, 6.0F, 10.0F, 6.0F, dilation);
		ModelPartBuilder hindleg = ModelPartBuilder.create().uv(64, 55).cuboid(-3.5F, 0.0F, -3.5F, 7.0F, 10.0F, 7.0F, dilation);
		ModelPartBuilder haunch = ModelPartBuilder.create().uv(0, 55).cuboid(-4.5F, -7.0F, -4.5F, 9.0F, 9.0F, 9.0F, dilation);

		root.addChild(EntityModelPartNames.RIGHT_FRONT_LEG, foreleg, ModelTransform.origin(-4.5F, LEG_Y, FRONT_LEG_Z));
		root.addChild(EntityModelPartNames.LEFT_FRONT_LEG, foreleg, ModelTransform.origin(4.5F, LEG_Y, FRONT_LEG_Z));
		root.addChild(EntityModelPartNames.RIGHT_HIND_LEG, hindleg, ModelTransform.origin(-4.5F, LEG_Y, HIND_LEG_Z))
			.addChild("right_haunch", haunch, ModelTransform.NONE);
		root.addChild(EntityModelPartNames.LEFT_HIND_LEG, hindleg, ModelTransform.origin(4.5F, LEG_Y, HIND_LEG_Z))
			.addChild("left_haunch", haunch, ModelTransform.NONE);

		// A short thick tail carried low. There is no whip in it at all.
		root.addChild(
			EntityModelPartNames.TAIL,
			ModelPartBuilder.create().uv(94, 63).cuboid(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 9.0F, dilation),
			ModelTransform.of(0.0F, TAIL_Y, TAIL_Z, 0.85F, 0.0F, 0.0F)
		);

		return modelData;
	}

	@Override
	public void setAngles(BullyEntityRenderState state) {
		super.setAngles(state);
		float limbSwing = state.limbSwingAnimationProgress;
		float limbAmplitude = Math.min(state.limbSwingAmplitude, 1.0F);

		// A short, heavy, flat-footed stride: half the reach of the whippet's,
		// and the whole dog rolls from side to side on it because it is wider
		// than it is tall.
		float reach = 0.7F * limbAmplitude;
		this.rightFrontLeg.pitch = MathHelper.cos(limbSwing * 0.5F) * reach;
		this.leftFrontLeg.pitch = MathHelper.cos(limbSwing * 0.5F + (float)Math.PI) * reach;
		this.rightHindLeg.pitch = MathHelper.cos(limbSwing * 0.5F + (float)Math.PI) * reach;
		this.leftHindLeg.pitch = MathHelper.cos(limbSwing * 0.5F) * reach;

		float roll = MathHelper.sin(limbSwing * 0.25F) * 0.09F * limbAmplitude;
		this.body.roll = roll;
		this.neck.roll = roll * 0.6F;
		this.body.pitch = MathHelper.cos(limbSwing * 0.5F) * 0.03F * limbAmplitude;

		this.tail.pitch = 0.9F - limbAmplitude * 0.25F;
		this.tail.yaw = MathHelper.cos(limbSwing * 0.5F) * 0.3F * limbAmplitude;

		this.neck.pitch = -0.25F + limbAmplitude * 0.12F;
		this.head.pitch = state.pitch * (float)(Math.PI / 180.0);
		this.head.yaw = state.relativeHeadYaw * (float)(Math.PI / 180.0);

		// The bite. The head goes forward and down and the shoulders drop in
		// behind it: this dog does not snap, it leans on you.
		if (state.lungeProgress > 0.001F) {
			float lunge = state.lungeProgress;
			this.neck.pitch += lunge * 0.55F;
			this.head.pitch += lunge * 0.35F;
			this.head.originY = HEAD_Y + lunge * 2.5F;
			this.head.originZ = HEAD_Z - lunge * 3.0F;
			this.body.pitch += lunge * 0.12F;
		} else {
			this.head.originY = HEAD_Y;
			this.head.originZ = HEAD_Z;
		}
	}
}
