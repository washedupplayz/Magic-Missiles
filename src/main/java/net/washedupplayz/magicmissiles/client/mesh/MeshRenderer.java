package net.washedupplayz.magicmissiles.client.mesh;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;

/**
 * Draws a baked mesh at the current pose.
 *
 * <p>Vanilla's {@code ModelBlockRenderer} already walks every face direction plus
 * the unculled list with the correct random seeding, so we hand off to it rather
 * than iterating quads ourselves. The block state is null: OBJ meshes are state
 * independent and return the same quads for any state.
 *
 * <p>Meshes are drawn in their own authored coordinates. Nothing is recentred
 * here, so a model's origin is exactly where it lands in the pose.
 */
public final class MeshRenderer {
    private MeshRenderer() {}

    public static void render(BakedModel model, PoseStack poseStack, VertexConsumer consumer, int light) {
        render(model, poseStack, consumer, light, OverlayTexture.NO_OVERLAY);
    }

    public static void render(BakedModel model, PoseStack poseStack, VertexConsumer consumer,
                              int light, int overlay) {
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                poseStack.last(), consumer, null, model, 1.0f, 1.0f, 1.0f, light, overlay);
    }
}
