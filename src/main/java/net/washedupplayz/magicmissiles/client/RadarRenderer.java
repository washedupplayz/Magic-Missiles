package net.washedupplayz.magicmissiles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.washedupplayz.magicmissiles.block.entity.RadarBlockEntity;
import net.washedupplayz.magicmissiles.client.mesh.MeshModels;
import net.washedupplayz.magicmissiles.client.mesh.MeshRenderer;

/**
 * Draws the radar as two OBJ meshes: a static base and a dish spun about the
 * column axis. Replaces the GeckoLib renderer — the old
 * {@code animation.radar.spin} was a single constant rotation of one bone, which
 * is cheaper to do here directly than to carry an animation system for.
 */
public class RadarRenderer implements BlockEntityRenderer<RadarBlockEntity> {
    /** Matches the 4 s loop of the animation this replaced. */
    private static final float SPIN_PERIOD_TICKS = 80.0f;

    public RadarRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(RadarBlockEntity radar, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int light, int overlay) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.cutout());

        MeshRenderer.render(MeshModels.get(MeshModels.RADAR_BASE), poseStack, consumer, light, overlay);

        poseStack.pushPose();
        // spin about the column, which stands at the centre of the block footprint
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(spinDegrees(radar, partialTick)));
        poseStack.translate(-0.5, 0.0, -0.5);
        MeshRenderer.render(MeshModels.get(MeshModels.RADAR_DISH), poseStack, consumer, light, overlay);
        poseStack.popPose();
    }

    private static float spinDegrees(RadarBlockEntity radar, float partialTick) {
        if (radar.getLevel() == null) {
            return 0.0f;
        }
        float ticks = radar.getLevel().getGameTime() % (long) SPIN_PERIOD_TICKS + partialTick;
        return ticks * (360.0f / SPIN_PERIOD_TICKS);
    }
}
