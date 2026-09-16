package net.washedupplayz.magicmissiles.block;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.washedupplayz.magicmissiles.block.entity.RadarBlockEntity;
import net.washedupplayz.magicmissiles.missile.MissileManager;
import net.washedupplayz.magicmissiles.missile.MissileSpecs;

/**
 * A stationary launcher. Right-clicking fires a missile skyward. If a radar
 * station is within {@link #RADAR_LINK_RANGE} blocks, the silo hands the missile
 * an immediate long-range target lock; otherwise the missile relies on its own
 * onboard seeker. (Ammo storage and automatic fire come in a later phase.)
 */
public class MissileSiloBlock extends Block {
    private static final int RADAR_LINK_RANGE = 8;

    public MissileSiloBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel serverLevel) {
            Vec3 launchPoint = new Vec3(pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5);
            LivingEntity designated = findRadarTarget(level, pos, launchPoint);
            MissileManager.get(serverLevel).launch(
                    launchPoint, new Vec3(0.0, 1.0, 0.0), MissileSpecs.STANDARD, player, designated);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Ask the nearest radar within link range for a target to designate. */
    @Nullable
    private static LivingEntity findRadarTarget(Level level, BlockPos siloPos, Vec3 origin) {
        RadarBlockEntity nearestRadar = null;
        double nearestDistSqr = Double.MAX_VALUE;

        for (BlockPos candidate : BlockPos.betweenClosed(
                siloPos.offset(-RADAR_LINK_RANGE, -RADAR_LINK_RANGE, -RADAR_LINK_RANGE),
                siloPos.offset(RADAR_LINK_RANGE, RADAR_LINK_RANGE, RADAR_LINK_RANGE))) {
            BlockEntity blockEntity = level.getBlockEntity(candidate);
            if (blockEntity instanceof RadarBlockEntity radar) {
                double distSqr = candidate.distSqr(siloPos);
                if (distSqr < nearestDistSqr) {
                    nearestDistSqr = distSqr;
                    nearestRadar = radar;
                }
            }
        }

        return nearestRadar == null ? null : nearestRadar.designateTarget(origin);
    }
}
