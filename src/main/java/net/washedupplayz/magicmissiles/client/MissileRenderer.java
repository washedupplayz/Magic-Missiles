package net.washedupplayz.magicmissiles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.washedupplayz.magicmissiles.MagicMissiles;
import net.washedupplayz.magicmissiles.entity.MissileEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renders the missile with its GeckoLib model/animation. The default model
 * resolves to {@code geo/entity/missile.geo.json}, {@code textures/entity/missile.png}
 * and {@code animations/entity/missile.animation.json}.
 */
public class MissileRenderer extends GeoEntityRenderer<MissileEntity> {
    public MissileRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(
                ResourceLocation.fromNamespaceAndPath(MagicMissiles.MOD_ID, "missile")));
    }

    /**
     * Point the missile along its flight direction.
     *
     * <p>{@link GeoEntityRenderer}'s default only applies yaw (and with a convention
     * that doesn't match this model), so a climbing or diving missile stays lying flat.
     * We replace it with the full orientation for this {@code +Z}-forward model:
     * {@code YP(yaw + 180)} to face the travel azimuth and {@code XP(pitch + 180)} to
     * tilt the nose up/down. The {@code +180} offsets fall out of GeckoLib's model flip
     * combined with how the model is built; they were confirmed in-game.
     */
    @Override
    protected void applyRotations(MissileEntity missile, PoseStack poseStack, float ageInTicks,
                                  float rotationYaw, float partialTick, float nativeScale) {
        float yaw = Mth.rotLerp(partialTick, missile.yRotO, missile.getYRot());
        float pitch = Mth.lerp(partialTick, missile.xRotO, missile.getXRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw+180));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch+180));
    }
}
