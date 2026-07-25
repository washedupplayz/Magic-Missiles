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
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Periodically scans a radius for living entities and keeps a list of tracked
 * targets. This is the data source the guidance and interception systems will
 * query.
 */
public class RadarBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final double SCAN_RANGE = 32.0;
    private static final int SCAN_INTERVAL_TICKS = 20;

    private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("animation.radar.spin");

    private final List<UUID> trackedTargets = new ArrayList<>();
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int scanCooldown;

    public RadarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADAR.get(), pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "spin", state -> state.setAndContinue(SPIN)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
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

    /** Targets currently within radar range, refreshed once per second. */
    public List<UUID> getTrackedTargets() {
        return Collections.unmodifiableList(trackedTargets);
    }

    /**
     * Best current target to hand to a launching missile: the tracked, still-alive
     * entity nearest to {@code origin}, or {@code null} if the radar has no lock.
     */
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
