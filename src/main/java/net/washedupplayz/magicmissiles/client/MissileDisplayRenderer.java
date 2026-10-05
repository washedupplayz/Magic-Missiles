package net.washedupplayz.magicmissiles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.washedupplayz.magicmissiles.block.MissileDisplayBlock;
import net.washedupplayz.magicmissiles.block.entity.MissileDisplayBlockEntity;
import net.washedupplayz.magicmissiles.client.mesh.MeshModels;
import net.washedupplayz.magicmissiles.client.mesh.MeshRenderer;

public class MissileDisplayRenderer implements BlockEntityRenderer<MissileDisplayBlockEntity> {
    private static final float ROLL_PERIOD_TICKS = 40.0f;
    private static final double DISPLAY_HEIGHT = 0.75;

    public MissileDisplayRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(MissileDisplayBlockEntity display, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int light, int overlay) {
        boolean flight = display.getBlockState().getValue(MissileDisplayBlock.FLIGHT);
        VertexConsumer consumer = buffers.getBuffer(RenderType.cutout());

        poseStack.pushPose();
        poseStack.translate(0.5, DISPLAY_HEIGHT, 0.5);
        // mesh is +z forward
        poseStack.mulPose(Axis.YP.rotationDegrees(-display.getBlockState()
                .getValue(MissileDisplayBlock.FACING).toYRot()));

        if (flight) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(rollDegrees(display, partialTick)));
        }

        // flight mode matches the ghost, static mode uses real light
        MeshRenderer.render(MeshModels.get(MeshModels.MM1), poseStack, consumer,
                flight ? LightTexture.FULL_BRIGHT : light, overlay);

        poseStack.popPose();
    }

    private static float rollDegrees(MissileDisplayBlockEntity display, float partialTick) {
        if (display.getLevel() == null) {
            return 0.0f;
        }
        float ticks = display.getLevel().getGameTime() % (long) ROLL_PERIOD_TICKS + partialTick;
        return ticks * (360.0f / ROLL_PERIOD_TICKS);
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    // mesh sticks far outside the block
    @Override
    public boolean shouldRenderOffScreen(MissileDisplayBlockEntity display) {
        return true;
    }
}
