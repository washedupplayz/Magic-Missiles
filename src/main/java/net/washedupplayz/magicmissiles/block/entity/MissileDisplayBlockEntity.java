package net.washedupplayz.magicmissiles.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.washedupplayz.magicmissiles.registry.ModBlockEntities;

public class MissileDisplayBlockEntity extends BlockEntity {
    public MissileDisplayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MISSILE_DISPLAY.get(), pos, state);
    }
}
