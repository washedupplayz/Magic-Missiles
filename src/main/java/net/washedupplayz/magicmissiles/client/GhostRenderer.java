package net.washedupplayz.magicmissiles.client;

import java.util.Collection;
import java.util.List;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import software.bernie.geckolib.renderer.GeoObjectRenderer;

/**
 * Draws every {@link MissileGhost} directly into the world during
 * {@link RenderLevelStageEvent}. Because this bypasses the entity renderer, missiles
 * are visible at any distance the client renders terrain — including the extended
 * LOD range of Distant Horizons / Voxy — and independent of entity tracking range.
 *
 * <p>The body is GeckoLib's {@link GeoObjectRenderer}, which applies the same
 * scale/flip as a {@code GeoEntityRenderer}, so the model matches the geo assets;
 * orientation is {@code YP(yaw + 180)} then {@code XP(pitch + 180)} for this
 * {@code +Z}-forward model.
 *
 * <p>The smoke contrail is drawn here too, as a self-rendered camera-facing ribbon
 * through the missile's recent positions. Unlike vanilla particles it persists and is
 * visible at any range — the whole point of a missile you can watch from miles away.
 */
public final class GhostRenderer {
    private static final GeoObjectRenderer<MissileGhost> RENDERER =
            new GeoObjectRenderer<>(new MissileGhostModel());

    /** Translucent, un-textured, un-culled ribbon; depth-tested so terrain occludes it. */
    private static final RenderType TRAIL_TYPE = RenderType.create(
            "magicmissiles:missile_trail",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.TRIANGLES,
            1536,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false));

    // Ribbon shape: narrow, hot exhaust at the nozzle widening into cool grey smoke.
    private static final double HEAD_WIDTH = 0.12;
    private static final double TAIL_WIDTH = 1.10;
    private static final float HEAD_ALPHA = 0.65f;
    private static final float[] HEAD_COLOR = {1.00f, 0.75f, 0.40f};
    private static final float[] TAIL_COLOR = {0.50f, 0.50f, 0.52f};

    private GhostRenderer() {}

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Collection<MissileGhost> ghosts = ClientMissileManager.ghosts();
        if (ghosts.isEmpty()) {
            return;
        }

        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 cam = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();

        renderModels(ghosts, poseStack, buffers, cam, partial);
        buffers.endBatch();                       // flush the (opaque) missile bodies first

        renderTrails(ghosts, poseStack, buffers, cam);
        buffers.endBatch();                       // then the translucent contrails over them
    }

    private static void renderModels(Collection<MissileGhost> ghosts, PoseStack poseStack,
                                     MultiBufferSource.BufferSource buffers, Vec3 cam, float partial) {
        int packedLight = LightTexture.FULL_BRIGHT;
        for (MissileGhost ghost : ghosts) {
            double rx = Mth.lerp(partial, ghost.prevX, ghost.x) - cam.x;
            double ry = Mth.lerp(partial, ghost.prevY, ghost.y) - cam.y;
            double rz = Mth.lerp(partial, ghost.prevZ, ghost.z) - cam.z;
            float yaw = Mth.rotLerp(partial, ghost.prevYaw, ghost.yaw);
            float pitch = Mth.lerp(partial, ghost.prevPitch, ghost.pitch);

            poseStack.pushPose();
            poseStack.translate(rx, ry, rz);
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw + 180.0f));
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch + 180.0f));
            // GeoObjectRenderer is built for block/item models and always adds a fixed
            // translate(0.5, 0.51, 0.5) to centre them in a 1x1x1 cell. Our missile is
            // positioned freely, so cancel that shift — otherwise the body sits ~half a
            // block off its true position and the exhaust trail appears to float above it.
            poseStack.translate(-0.5, -0.51, -0.5);

            ResourceLocation texture = RENDERER.getTextureLocation(ghost);
            RenderType renderType = RENDERER.getRenderType(ghost, texture, buffers, partial);
            RENDERER.render(poseStack, ghost, buffers, renderType, buffers.getBuffer(renderType),
                    packedLight, partial);

            poseStack.popPose();
        }
    }

    private static void renderTrails(Collection<MissileGhost> ghosts, PoseStack poseStack,
                                     MultiBufferSource.BufferSource buffers, Vec3 cam) {
        VertexConsumer consumer = buffers.getBuffer(TRAIL_TYPE);
        Matrix4f matrix = poseStack.last().pose();
        for (MissileGhost ghost : ghosts) {
            List<Vec3> points = ghost.trailSnapshot();          // newest first
            int n = points.size();
            if (n < 2) {
                continue;
            }
            for (int i = 0; i < n - 1; i++) {
                Vec3 a = points.get(i);
                Vec3 b = points.get(i + 1);
                double fracA = (double) i / (n - 1);
                double fracB = (double) (i + 1) / (n - 1);
                Vec3 side = ribbonSide(a, b, cam);
                double wA = Mth.lerp(fracA, HEAD_WIDTH, TAIL_WIDTH);
                double wB = Mth.lerp(fracB, HEAD_WIDTH, TAIL_WIDTH);

                Vec3 a0 = a.subtract(side.scale(wA));
                Vec3 a1 = a.add(side.scale(wA));
                Vec3 b0 = b.subtract(side.scale(wB));
                Vec3 b1 = b.add(side.scale(wB));

                // Two triangles forming the quad between slice A and slice B.
                vertex(consumer, matrix, cam, a0, fracA);
                vertex(consumer, matrix, cam, a1, fracA);
                vertex(consumer, matrix, cam, b1, fracB);

                vertex(consumer, matrix, cam, a0, fracA);
                vertex(consumer, matrix, cam, b1, fracB);
                vertex(consumer, matrix, cam, b0, fracB);
            }
        }
    }

    /** A ribbon half-width direction at point {@code a}, perpendicular to the segment and facing the camera. */
    private static Vec3 ribbonSide(Vec3 a, Vec3 b, Vec3 cam) {
        Vec3 dir = b.subtract(a);
        if (dir.lengthSqr() < 1.0e-8) {
            dir = new Vec3(0, 0, 1);
        }
        Vec3 side = dir.cross(cam.subtract(a));
        if (side.lengthSqr() < 1.0e-8) {
            side = dir.cross(new Vec3(0, 1, 0));           // segment points at the camera; pick any perpendicular
        }
        if (side.lengthSqr() < 1.0e-8) {
            side = new Vec3(1, 0, 0);
        }
        return side.normalize();
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Vec3 cam, Vec3 pos, double frac) {
        float r = (float) Mth.lerp(frac, HEAD_COLOR[0], TAIL_COLOR[0]);
        float g = (float) Mth.lerp(frac, HEAD_COLOR[1], TAIL_COLOR[1]);
        float b = (float) Mth.lerp(frac, HEAD_COLOR[2], TAIL_COLOR[2]);
        float alpha = (float) (HEAD_ALPHA * (1.0 - frac));   // fade out toward the tail
        consumer.addVertex(matrix, (float) (pos.x - cam.x), (float) (pos.y - cam.y), (float) (pos.z - cam.z))
                .setColor(r, g, b, alpha);
    }
}
