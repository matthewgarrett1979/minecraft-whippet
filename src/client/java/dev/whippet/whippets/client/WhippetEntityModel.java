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
 * A whippet: deep chest, tucked loin, long thin legs, whip tail, and a head
 * narrow enough to look like it was drawn with a ruler.
 */
public class WhippetEntityModel extends EntityModel<WhippetEntityRenderState> {
	public static final ModelTransformer BABY_TRANSFORMER = new BabyModelTransformer(
		true, 6.0F, 2.5F, 2.0F, 2.0F, 22.0F, Set.of(EntityModelPartNames.HEAD)
	);
	private static final String REAL_HEAD = "real_head";
	private static final String REAL_TAIL = "real_tail";

	private final ModelPart head;
	private final ModelPart realHead;
	private final ModelPart neck;
	private final ModelPart body;
	private final ModelPart rightFrontLeg;
	private final ModelPart leftFrontLeg;
	private final ModelPart rightHindLeg;
	private final ModelPart leftHindLeg;
	private final ModelPart tail;
	private final ModelPart realTail;

	public WhippetEntityModel(ModelPart root) {
		super(root);
		this.head = root.getChild(EntityModelPartNames.HEAD);
		this.realHead = this.head.getChild(REAL_HEAD);
		this.neck = root.getChild(EntityModelPartNames.NECK);
		this.body = root.getChild(EntityModelPartNames.BODY);
		this.rightFrontLeg = root.getChild(EntityModelPartNames.RIGHT_FRONT_LEG);
		this.leftFrontLeg = root.getChild(EntityModelPartNames.LEFT_FRONT_LEG);
		this.rightHindLeg = root.getChild(EntityModelPartNames.RIGHT_HIND_LEG);
		this.leftHindLeg = root.getChild(EntityModelPartNames.LEFT_HIND_LEG);
		this.tail = root.getChild(EntityModelPartNames.TAIL);
		this.realTail = this.tail.getChild(REAL_TAIL);
	}

	public static ModelData getModelData(Dilation dilation) {
		ModelData modelData = new ModelData();
		ModelPartData root = modelData.getRoot();

		ModelPartData head = root.addChild(EntityModelPartNames.HEAD, ModelPartBuilder.create(), ModelTransform.origin(0.0F, 7.4F, -6.8F));
		head.addChild(
			REAL_HEAD,
			ModelPartBuilder.create()
				.uv(0, 0)
				.cuboid(-2.0F, -2.0F, -3.0F, 4.0F, 4.0F, 4.0F, dilation)
				.uv(20, 0)
				.cuboid(-1.5F, -0.5F, -6.0F, 3.0F, 2.0F, 3.0F, dilation)
				.uv(36, 0)
				.cuboid(-2.0F, -3.5F, -1.5F, 2.0F, 2.0F, 1.0F, dilation)
				.uv(36, 0)
				.mirrored()
				.cuboid(0.0F, -3.5F, -1.5F, 2.0F, 2.0F, 1.0F, dilation),
			ModelTransform.NONE
		);

		root.addChild(
			EntityModelPartNames.NECK,
			ModelPartBuilder.create().uv(32, 28).cuboid(-1.5F, -1.5F, -4.0F, 3.0F, 3.0F, 5.0F, dilation),
			ModelTransform.of(0.0F, 9.8F, -3.5F, -0.6F, 0.0F, 0.0F)
		);

		root.addChild(
			EntityModelPartNames.BODY,
			ModelPartBuilder.create()
				.uv(0, 12)
				.cuboid(-2.5F, -3.0F, -6.0F, 5.0F, 6.0F, 7.0F, dilation)
				.uv(26, 12)
				.cuboid(-2.0F, -3.5F, 1.0F, 4.0F, 4.0F, 6.0F, dilation),
			ModelTransform.origin(0.0F, 11.5F, 0.0F)
		);

		ModelPartBuilder leg = ModelPartBuilder.create().uv(0, 28).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 9.0F, 2.0F, dilation);
		ModelPartBuilder mirroredLeg = ModelPartBuilder.create().mirrored().uv(0, 28).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 9.0F, 2.0F, dilation);
		ModelPartBuilder haunch = ModelPartBuilder.create().uv(10, 28).cuboid(-1.5F, -0.5F, -1.5F, 3.0F, 4.0F, 3.0F, dilation);
		ModelPartBuilder mirroredHaunch = ModelPartBuilder.create().mirrored().uv(10, 28).cuboid(-1.5F, -0.5F, -1.5F, 3.0F, 4.0F, 3.0F, dilation);

		root.addChild(EntityModelPartNames.RIGHT_FRONT_LEG, mirroredLeg, ModelTransform.origin(-1.6F, 15.0F, -4.5F));
		root.addChild(EntityModelPartNames.LEFT_FRONT_LEG, leg, ModelTransform.origin(1.6F, 15.0F, -4.5F));
		root.addChild(EntityModelPartNames.RIGHT_HIND_LEG, mirroredLeg, ModelTransform.origin(-1.7F, 15.0F, 5.0F))
			.addChild("right_haunch", mirroredHaunch, ModelTransform.NONE);
		root.addChild(EntityModelPartNames.LEFT_HIND_LEG, leg, ModelTransform.origin(1.7F, 15.0F, 5.0F))
			.addChild("left_haunch", haunch, ModelTransform.NONE);

		ModelPartData tail = root.addChild(
			EntityModelPartNames.TAIL, ModelPartBuilder.create(), ModelTransform.of(0.0F, 10.0F, 6.5F, 1.0F, 0.0F, 0.0F)
		);
		tail.addChild(REAL_TAIL, ModelPartBuilder.create().uv(26, 28).cuboid(-0.5F, 0.0F, -0.5F, 1.0F, 9.0F, 1.0F, dilation), ModelTransform.NONE);

		return modelData;
	}

	@Override
	public void setAngles(WhippetEntityRenderState state) {
		super.setAngles(state);
		float limbSwing = state.limbSwingAnimationProgress;
		float limbAmplitude = state.limbSwingAmplitude;

		if (state.curled) {
			this.curl();
		} else if (state.inSittingPose) {
			this.sit();
		} else {
			if (state.zooming) {
				this.gallop(limbSwing);
			} else {
				this.trot(limbSwing, limbAmplitude);
			}

			this.neck.pitch = -0.6F + state.tuckProgress * 0.5F;
			// Low tail carriage, and it curls under the belly when the dog is cold.
			this.tail.pitch = state.tailAngle - state.tuckProgress * 0.9F;
			// The whip tail swings across the body rather than wagging up and down.
			this.tail.yaw = MathHelper.cos(limbSwing * 0.55F) * 1.1F * limbAmplitude;
			this.realTail.roll = MathHelper.sin(limbSwing * 0.3F) * 0.15F;
		}

		if (!state.curled) {
			this.head.pitch = this.head.pitch + state.pitch * (float)(Math.PI / 180.0) + state.tuckProgress * 0.25F;
			this.head.yaw = state.relativeHeadYaw * (float)(Math.PI / 180.0);
		}
	}

	/** An easy trot: diagonal pairs, low amplitude, almost no body movement. */
	private void trot(float limbSwing, float limbAmplitude) {
		float swing = MathHelper.cos(limbSwing * 0.7F) * 1.25F * limbAmplitude;
		float offSwing = MathHelper.cos(limbSwing * 0.7F + (float)Math.PI) * 1.25F * limbAmplitude;
		this.rightFrontLeg.pitch = swing;
		this.leftFrontLeg.pitch = offSwing;
		this.rightHindLeg.pitch = offSwing;
		this.leftHindLeg.pitch = swing;
		this.body.pitch = 0.0F;
		this.body.originY = 11.5F;
	}

	/**
	 * A double-suspension gallop: front pair and hind pair swing together and the
	 * whole dog folds and extends. This is the gait whippets are famous for.
	 */
	private void gallop(float limbSwing) {
		float phase = limbSwing * 0.55F;
		float front = MathHelper.cos(phase) * 1.9F;
		float hind = MathHelper.cos(phase + 2.2F) * 1.9F;
		this.rightFrontLeg.pitch = front;
		this.leftFrontLeg.pitch = front - 0.25F;
		this.rightHindLeg.pitch = hind;
		this.leftHindLeg.pitch = hind - 0.25F;
		this.body.pitch = MathHelper.sin(phase) * 0.12F;
		this.body.originY = 11.5F + MathHelper.cos(phase * 2.0F) * 0.6F;
	}

	/**
	 * Curled: flat on the floor, legs folded under, head round on its own flank
	 * and the tail over the nose. This is the shape a whippet holds for hours.
	 */
	private void curl() {
		// Flat out, brisket on the floor.
		this.body.pitch = 0.0F;
		this.body.originY = 20.8F;
		this.body.originZ = 0.5F;
		// Neck down off the front of the chest so the chin lands on the ground.
		this.neck.pitch = 0.9F;
		this.neck.yaw = 0.25F;
		this.neck.originY = 19.6F;
		this.neck.originZ = -4.3F;
		this.head.originY = 22.4F;
		this.head.originZ = -7.2F;
		this.head.pitch = 0.25F;
		this.head.yaw = 0.5F;
		this.head.roll = 0.0F;
		// Legs folded away under the body, out of sight where they belong.
		this.rightFrontLeg.pitch = 1.6F;
		this.leftFrontLeg.pitch = 1.6F;
		this.rightFrontLeg.originY = 22.6F;
		this.leftFrontLeg.originY = 22.6F;
		this.rightFrontLeg.originZ = -3.5F;
		this.leftFrontLeg.originZ = -3.5F;
		this.rightHindLeg.pitch = (float)(Math.PI * 1.5);
		this.leftHindLeg.pitch = (float)(Math.PI * 1.5);
		this.rightHindLeg.originY = 22.6F;
		this.leftHindLeg.originY = 22.6F;
		this.rightHindLeg.originZ = 4.0F;
		this.leftHindLeg.originZ = 4.0F;
		// Tail round the outside, the way it always ends up.
		this.tail.pitch = 1.6F;
		this.tail.yaw = 1.3F;
		this.tail.originY = 22.6F;
		this.tail.originZ = 4.5F;
		this.realTail.roll = 0.0F;
	}

	/**
	 * Sitting: chest up, hocks folded forward along the ground, tail laid out
	 * behind. A sitting whippet is mostly elbows.
	 */
	private void sit() {
		this.body.pitch = -0.55F;
		this.body.originY = 16.5F;
		this.body.originZ = 2.0F;
		this.neck.pitch = -0.9F;
		this.neck.originY = 10.5F;
		this.neck.originZ = -4.0F;
		this.head.originY = 6.8F;
		this.head.originZ = -7.0F;
		this.rightHindLeg.pitch = (float)(Math.PI * 1.5);
		this.leftHindLeg.pitch = (float)(Math.PI * 1.5);
		this.rightHindLeg.originY = 22.0F;
		this.leftHindLeg.originY = 22.0F;
		this.rightHindLeg.originZ = 4.0F;
		this.leftHindLeg.originZ = 4.0F;
		this.rightFrontLeg.pitch = 0.0F;
		this.leftFrontLeg.pitch = 0.0F;
		this.tail.pitch = 1.75F;
		this.tail.yaw = 0.3F;
		this.tail.originY = 21.0F;
		this.tail.originZ = 5.5F;
		this.realTail.roll = 0.0F;
	}
}
