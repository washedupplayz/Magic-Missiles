package net.washedupplayz.magicmissiles.block.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.washedupplayz.magicmissiles.MagicMissiles;
import net.washedupplayz.magicmissiles.registry.ModBlockEntities;

public class RadarBlockEntity extends BlockEntity {
    private static final double SCAN_RANGE = 32.0;
    private static final int SCAN_INTERVAL_TICKS = 20;

    private final List<UUID> trackedTargets = new ArrayList<>();
    private int scanCooldown;

    public RadarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADAR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RadarBlockEntity radar) {
        if (--radar.scanCooldown > 0) {
            return;
        }
        radar.scanCooldown = SCAN_INTERVAL_TICKS;
        radar.scan(level, pos);
    }

    private void scan(Level level, BlockPos pos) {
        AABB area = new AABB(pos).inflate(SCAN_RANGE);
        List<LivingEntity> found = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity.isAlive() && !(entity instanceof Player));

        trackedTargets.clear();
        for (LivingEntity entity : found) {
            trackedTargets.add(entity.getUUID());
        }

        if (!found.isEmpty()) {
            MagicMissiles.LOGGER.debug("Radar at {} is tracking {} target(s)", pos, found.size());
        }
    }

    public List<UUID> getTrackedTargets() {
        return Collections.unmodifiableList(trackedTargets);
    }

    @Nullable
    public LivingEntity designateTarget(Vec3 origin) {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return null;
        }
        LivingEntity best = null;
        double bestDistSqr = Double.MAX_VALUE;
        for (UUID id : trackedTargets) {
            Entity entity = serverLevel.getEntity(id);
            if (entity instanceof LivingEntity target && target.isAlive()) {
                double distSqr = target.distanceToSqr(origin);
                if (distSqr < bestDistSqr) {
                    bestDistSqr = distSqr;
                    best = target;
                }
            }
        }
        return best;
    }
}
