package net.washedupplayz.magicmissiles.client.mesh;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;

public final class MeshRenderer {
    private MeshRenderer() {}

    public static void render(BakedModel model, PoseStack poseStack, VertexConsumer consumer, int light) {
        render(model, poseStack, consumer, light, OverlayTexture.NO_OVERLAY);
    }

    public static void render(BakedModel model, PoseStack poseStack, VertexConsumer consumer,
                              int light, int overlay) {
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                // null state, obj quads are state independent
                poseStack.last(), consumer, null, model, 1.0f, 1.0f, 1.0f, light, overlay);
    }
}
