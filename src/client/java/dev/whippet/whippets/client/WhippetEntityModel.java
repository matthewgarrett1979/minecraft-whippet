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
	// A puppy is the adult at half scale with a three-quarter-scale head. The
	// offsets are what put that head back on the end of the shrunken neck.
	public static final ModelTransformer BABY_TRANSFORMER = new BabyModelTransformer(
		true, 14.3F, 2.4F, 2.0F, 2.0F, 24.0F, Set.of(EntityModelPartNames.HEAD)
	);
	// Where each part sits on a standing adult. The poses below work in these
	// units, and puppies are the same model at half scale, so every move has to
	// be measured from here rather than written out as an absolute.
	private static final float HEAD_Y = 5.1F;
	private static final float HEAD_Z = -7.2F;
	private static final float NECK_Y = 9.8F;
	private static final float NECK_Z = -3.5F;
	private static final float BODY_Y = 11.5F;
	private static final float BODY_Z = 0.0F;
	private static final float LEG_Y = 15.0F;
	private static final float FRONT_LEG_Z = -4.5F;
	private static final float HIND_LEG_Z = 7.0F;
	private static final float TAIL_Y = 10.0F;
	private static final float TAIL_Z = 8.5F;
	private static final String REAL_HEAD = "real_head";
	private static final String RIGHT_EAR = "right_ear";
	private static final String LEFT_EAR = "left_ear";
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

		ModelPartData head = root.addChild(EntityModelPartNames.HEAD, ModelPartBuilder.create(), ModelTransform.origin(0.0F, HEAD_Y, HEAD_Z));
		// A long, narrow wedge: the skull is three wide and five deep, and the
		// muzzle carries on from it with barely any stop, as it does on the dog.
		ModelPartData realHead = head.addChild(
			REAL_HEAD,
			ModelPartBuilder.create()
				.uv(0, 0)
				.cuboid(-1.5F, -1.5F, -4.0F, 3.0F, 3.0F, 5.0F, dilation)
				.uv(20, 0)
				.cuboid(-1.0F, -1.0F, -7.0F, 2.0F, 2.0F, 3.0F, dilation),
			ModelTransform.NONE
		);

		// Rose ears: small flaps folded back along the skull with the tips turned
		// out, not the upright triangles a wolf wears.
		ModelPartBuilder ear = ModelPartBuilder.create().uv(36, 0).cuboid(-1.0F, -1.0F, 0.0F, 1.0F, 2.0F, 3.0F, dilation);
		ModelPartBuilder mirroredEar = ModelPartBuilder.create().mirrored().uv(36, 0).cuboid(0.0F, -1.0F, 0.0F, 1.0F, 2.0F, 3.0F, dilation);
		realHead.addChild(RIGHT_EAR, ear, ModelTransform.of(-1.4F, -0.3F, -0.5F, -0.15F, -0.4F, -0.3F));
		realHead.addChild(LEFT_EAR, mirroredEar, ModelTransform.of(1.4F, -0.3F, -0.5F, -0.15F, 0.4F, 0.3F));

		root.addChild(
			EntityModelPartNames.NECK,
			ModelPartBuilder.create().uv(32, 28).cuboid(-1.5F, -1.5F, -6.0F, 3.0F, 3.0F, 7.0F, dilation),
			ModelTransform.of(0.0F, NECK_Y, NECK_Z, -0.9F, 0.0F, 0.0F)
		);

		root.addChild(
			EntityModelPartNames.BODY,
			ModelPartBuilder.create()
				.uv(0, 12)
				.cuboid(-2.5F, -3.5F, -6.0F, 5.0F, 7.0F, 7.0F, dilation)
				.uv(26, 12)
				.cuboid(-2.0F, -3.5F, 1.0F, 4.0F, 4.0F, 8.0F, dilation),
			ModelTransform.origin(0.0F, BODY_Y, BODY_Z)
		);

		ModelPartBuilder leg = ModelPartBuilder.create().uv(0, 28).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 9.0F, 2.0F, dilation);
		ModelPartBuilder mirroredLeg = ModelPartBuilder.create().mirrored().uv(0, 28).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 9.0F, 2.0F, dilation);
		ModelPartBuilder haunch = ModelPartBuilder.create().uv(10, 28).cuboid(-1.5F, -3.0F, -1.5F, 3.0F, 4.0F, 3.0F, dilation);
		ModelPartBuilder mirroredHaunch = ModelPartBuilder.create().mirrored().uv(10, 28).cuboid(-1.5F, -3.0F, -1.5F, 3.0F, 4.0F, 3.0F, dilation);

		root.addChild(EntityModelPartNames.RIGHT_FRONT_LEG, mirroredLeg, ModelTransform.origin(-1.6F, LEG_Y, FRONT_LEG_Z));
		root.addChild(EntityModelPartNames.LEFT_FRONT_LEG, leg, ModelTransform.origin(1.6F, LEG_Y, FRONT_LEG_Z));
		root.addChild(EntityModelPartNames.RIGHT_HIND_LEG, mirroredLeg, ModelTransform.origin(-1.7F, LEG_Y, HIND_LEG_Z))
			.addChild("right_haunch", mirroredHaunch, ModelTransform.NONE);
		root.addChild(EntityModelPartNames.LEFT_HIND_LEG, leg, ModelTransform.origin(1.7F, LEG_Y, HIND_LEG_Z))
			.addChild("left_haunch", haunch, ModelTransform.NONE);

		ModelPartData tail = root.addChild(
			EntityModelPartNames.TAIL, ModelPartBuilder.create(), ModelTransform.of(0.0F, TAIL_Y, TAIL_Z, 1.0F, 0.0F, 0.0F)
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
			float hurry = 0.0F;

			if (state.zooming || state.racing) {
				this.gallop(limbSwing);
			} else {
				hurry = this.trot(limbSwing, limbAmplitude, state.pace, state.groundSpeed);
			}

			this.neck.pitch = -0.9F + state.tuckProgress * 0.6F;
			// Low tail carriage, and it curls under the belly when the dog is cold.
			// It comes up off the hocks and streams out as the dog quickens.
			this.tail.pitch = state.tailAngle - state.tuckProgress * 0.9F - hurry * 0.3F;
			// The whip tail swings across the body rather than wagging up and down,
			// and the swing shortens and quickens with the stride.
			this.tail.yaw = MathHelper.cos(limbSwing * (0.55F + hurry * 0.5F)) * (1.1F - hurry * 0.35F) * limbAmplitude;
			this.realTail.roll = MathHelper.sin(limbSwing * 0.3F) * 0.15F;
		}

		if (!state.curled) {
			this.head.pitch = this.head.pitch + state.pitch * (float)(Math.PI / 180.0) + state.tuckProgress * 0.25F;
			this.head.yaw = state.relativeHeadYaw * (float)(Math.PI / 180.0);
		}

		// Begging: head over on one side, which is the whole trick.
		if (state.begProgress > 0.001F) {
			this.realHead.roll = state.begProgress * 0.55F;
			this.head.pitch -= state.begProgress * 0.18F;
			this.neck.pitch -= state.begProgress * 0.25F;
		}
	}

	/**
	 * Puts a part where an adult whippet would carry it. A puppy is the same
	 * model at half scale, so the distance from the part's home has to shrink
	 * with it or the pose comes apart in the middle.
	 */
	private static void moveTo(ModelPart part, float homeY, float homeZ, float y, float z) {
		ModelTransform home = part.getDefaultTransform();
		part.originY = home.y() + (y - homeY) * part.yScale;
		part.originZ = home.z() + (z - homeZ) * part.zScale;
	}

	/**
	 * The trot, at whatever pace the dog is going. Dawdling, it is the easy
	 * diagonal walk it always was: long strides, almost no movement above the
	 * elbow. Pushed on, it tightens into the quick trot — a much higher
	 * cadence, shorter strides, the feet snapping through and holding at the
	 * ends rather than sweeping, and the whole body rocking from side to side
	 * and bouncing on each diagonal. It is the armadillo's busy scuttle done on
	 * a sighthound's legs, and it is how a whippet actually covers ground when
	 * it has not yet decided to gallop.
	 */
	private float trot(float limbSwing, float limbAmplitude, float pace, float groundSpeed) {
		// How much of the quick trot is showing, measured on ground actually
		// covered: nothing at a dawdle, half of it at a wander, all of it once
		// the dog is going somewhere.
		float hurry = MathHelper.clamp((groundSpeed - 0.17F) / 0.14F, 0.0F, 1.0F);
		float cadence = MathHelper.lerp(hurry, 0.7F, 1.5F) * pace;
		float reach = MathHelper.lerp(hurry, 1.25F, 0.85F) * limbAmplitude;
		float phase = limbSwing * cadence;
		float swing = snap(MathHelper.cos(phase), hurry) * reach;
		float offSwing = snap(MathHelper.cos(phase + (float)Math.PI), hurry) * reach;
		this.rightFrontLeg.pitch = swing;
		this.leftFrontLeg.pitch = offSwing;
		this.rightHindLeg.pitch = offSwing;
		this.leftHindLeg.pitch = swing;
		this.body.pitch = 0.0F;
		// Rock and bounce: the torso rolls with the diagonals and lifts on each
		// beat, while the neck and head ride above it and stay level.
		this.body.roll = MathHelper.sin(phase) * 0.09F * hurry;
		moveTo(this.body, BODY_Y, BODY_Z, BODY_Y - Math.abs(MathHelper.cos(phase)) * 0.55F * hurry, BODY_Z);
		float lift = Math.abs(MathHelper.cos(phase)) * 0.35F * hurry;
		moveTo(this.head, HEAD_Y, HEAD_Z, HEAD_Y - lift, HEAD_Z);
		moveTo(this.neck, NECK_Y, NECK_Z, NECK_Y - lift, NECK_Z);
		return hurry;
	}

	/**
	 * Flattens the ends of a wave and steepens the crossing, so a leg hangs at
	 * the end of its stride and then snaps through instead of sweeping evenly.
	 * At zero this is an ordinary sine; at one it is a quick, busy step.
	 */
	private static float snap(float wave, float amount) {
		float sharpened = Math.signum(wave) * (float)Math.pow(Math.abs(wave), 0.55);
		return MathHelper.lerp(amount, wave, sharpened);
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
		moveTo(this.body, BODY_Y, BODY_Z, BODY_Y + MathHelper.cos(phase * 2.0F) * 0.6F, BODY_Z);
	}

	/**
	 * Curled: flat on the floor, legs folded under, head round on its own flank
	 * and the tail over the nose. This is the shape a whippet holds for hours.
	 */
	private void curl() {
		// Flat out, brisket on the floor.
		this.body.pitch = 0.0F;
		moveTo(this.body, BODY_Y, BODY_Z, 20.8F, 0.5F);
		// Neck down off the front of the chest so the chin lands on the ground.
		this.neck.pitch = 0.9F;
		this.neck.yaw = 0.25F;
		moveTo(this.neck, NECK_Y, NECK_Z, 19.6F, -4.3F);
		moveTo(this.head, HEAD_Y, HEAD_Z, 22.4F, -8.0F);
		this.head.pitch = 0.25F;
		this.head.yaw = 0.5F;
		this.head.roll = 0.0F;
		// Legs folded away under the body, out of sight where they belong.
		this.rightFrontLeg.pitch = 1.6F;
		this.leftFrontLeg.pitch = 1.6F;
		moveTo(this.rightFrontLeg, LEG_Y, FRONT_LEG_Z, 22.6F, -3.5F);
		moveTo(this.leftFrontLeg, LEG_Y, FRONT_LEG_Z, 22.6F, -3.5F);
		this.rightHindLeg.pitch = (float)(Math.PI * 1.5);
		this.leftHindLeg.pitch = (float)(Math.PI * 1.5);
		moveTo(this.rightHindLeg, LEG_Y, HIND_LEG_Z, 22.6F, 6.0F);
		moveTo(this.leftHindLeg, LEG_Y, HIND_LEG_Z, 22.6F, 6.0F);
		// Tail round the outside, the way it always ends up.
		this.tail.pitch = 1.6F;
		this.tail.yaw = 1.3F;
		moveTo(this.tail, TAIL_Y, TAIL_Z, 22.6F, 6.5F);
		this.realTail.roll = 0.0F;
	}

	/**
	 * Sitting: chest up, hocks folded forward along the ground, tail laid out
	 * behind. A sitting whippet is mostly elbows.
	 */
	private void sit() {
		// The rump goes down on the ground, the back comes up at forty degrees,
		// and the front legs stay straight underneath. Mostly elbows.
		this.body.pitch = -0.7F;
		moveTo(this.body, BODY_Y, BODY_Z, 17.8F, 2.0F);
		this.neck.pitch = -1.2F;
		moveTo(this.neck, NECK_Y, NECK_Z, 11.6F, -1.5F);
		moveTo(this.head, HEAD_Y, HEAD_Z, 6.0F, -3.7F);
		this.rightHindLeg.pitch = (float)(Math.PI * 1.5);
		this.leftHindLeg.pitch = (float)(Math.PI * 1.5);
		moveTo(this.rightHindLeg, LEG_Y, HIND_LEG_Z, 22.3F, 8.0F);
		moveTo(this.leftHindLeg, LEG_Y, HIND_LEG_Z, 22.3F, 8.0F);
		this.rightFrontLeg.pitch = 0.0F;
		this.leftFrontLeg.pitch = 0.0F;
		this.tail.pitch = 1.75F;
		this.tail.yaw = 0.3F;
		moveTo(this.tail, TAIL_Y, TAIL_Z, 22.0F, 8.5F);
		this.realTail.roll = 0.0F;
	}
}
