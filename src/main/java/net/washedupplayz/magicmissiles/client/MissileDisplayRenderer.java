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

/**
 * Draws the missile mesh on its stand, in one of two modes, so the model can be
 * told apart from the way the ghost renders it. See {@link MissileDisplayBlock}.
 *
 * <p>Both modes go through the same {@link MeshRenderer} and the same baked model
 * as the missile in flight, so nothing here is a separate render path that could
 * hide a problem.
 */
public class MissileDisplayRenderer implements BlockEntityRenderer<MissileDisplayBlockEntity> {
    /** Matches GhostRenderer, whose roll came from the old GeckoLib fly animation. */
    private static final float ROLL_PERIOD_TICKS = 40.0f;
    /** Lifted clear of the plinth so the body is not buried in it. */
    private static final double DISPLAY_HEIGHT = 0.75;

    public MissileDisplayRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(MissileDisplayBlockEntity display, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int light, int overlay) {
        boolean flight = display.getBlockState().getValue(MissileDisplayBlock.FLIGHT);
        VertexConsumer consumer = buffers.getBuffer(RenderType.cutout());

        poseStack.pushPose();
        poseStack.translate(0.5, DISPLAY_HEIGHT, 0.5);
        // point the nose along the block's facing; the mesh is +Z forward
        poseStack.mulPose(Axis.YP.rotationDegrees(-display.getBlockState()
                .getValue(MissileDisplayBlock.FACING).toYRot()));

        if (flight) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(rollDegrees(display, partialTick)));
        }

        // flight mode reproduces the ghost's full-bright lighting; static mode uses the
        // real light level, which is what makes the mesh's shading readable
        MeshRenderer.render(MeshModels.get(MeshModels.MISSILE), poseStack, consumer,
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

    /** The missile sticks far outside its block, so widen the render box or it vanishes. */
    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public boolean shouldRenderOffScreen(MissileDisplayBlockEntity display) {
        return true;
    }
}
