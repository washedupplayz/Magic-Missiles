package net.washedupplayz.magicmissiles.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.washedupplayz.magicmissiles.block.entity.MissileDisplayBlockEntity;

/**
 * An inspection stand that holds a missile mesh still so it can be looked at.
 *
 * <p>Exists because the missile is normally only visible mid-flight, moving fast,
 * lit at full brightness and rolling — which makes it impossible to tell a bad model
 * from a bad render path. {@link #FLIGHT} switches between the two:
 *
 * <ul>
 * <li>{@code false} — neutral pose, real world lighting, no roll. This is the mesh
 *     itself, as close to how Blockbench shows it as the game gets.
 * <li>{@code true} — exactly what {@code GhostRenderer} does in flight: full
 *     brightness and the continuous roll. Any difference between the two modes is
 *     the render path, not the model.
 * </ul>
 *
 * <p>Right-click toggles. Placeable anywhere and independent of its surroundings:
 * it is a plain block entity with no neighbour or support requirements, so it will
 * not pop when the block it was placed against is removed.
 */
public class MissileDisplayBlock extends BaseEntityBlock {
    public static final MapCodec<MissileDisplayBlock> CODEC = simpleCodec(MissileDisplayBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** Render as the ghost does in flight, rather than as a static model. */
    public static final BooleanProperty FLIGHT = BooleanProperty.create("flight");

    /** Low plinth: the missile renders well outside this, which is fine for a renderer. */
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 3.0, 14.0);

    public MissileDisplayBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(FLIGHT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FLIGHT);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            level.setBlock(pos, state.cycle(FLIGHT), Block.UPDATE_ALL);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MissileDisplayBlockEntity(pos, state);
    }
}
