package net.washedupplayz.magicmissiles.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.washedupplayz.magicmissiles.block.entity.RadarBlockEntity;
import net.washedupplayz.magicmissiles.registry.ModBlockEntities;

/**
 * Radar station. Its block entity periodically scans the surrounding area and
 * publishes a list of tracked targets that other systems (silos, interceptors)
 * will consume.
 */
public class RadarBlock extends BaseEntityBlock {
    public static final MapCodec<RadarBlock> CODEC = simpleCodec(RadarBlock::new);

    public RadarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadarBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                 BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.RADAR.get(), RadarBlockEntity::serverTick);
    }
}
