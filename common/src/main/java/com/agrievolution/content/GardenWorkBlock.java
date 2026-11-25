package com.agrievolution.content;

import com.agrievolution.platform.Game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class GardenWorkBlock extends Block {
    private final boolean preserving;

    public GardenWorkBlock(Properties properties, boolean preserving) {
        super(properties);
        this.preserving = preserving;
        registerDefaultState(defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(HorizontalDirectionalBlock.FACING, rotation.rotate(state.getValue(HorizontalDirectionalBlock.FACING)));
    }

    @Override protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(HorizontalDirectionalBlock.FACING)));
    }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (preserving) return box(1, 0, 1, 15, 16, 15);
        return state.getValue(HorizontalDirectionalBlock.FACING).getAxis() == Direction.Axis.Z
                ? box(1, 0, 5, 15, 16, 11) : box(5, 0, 1, 11, 16, 15);
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer) {
            var orders = preserving ? java.util.List.<GardenWork.Job>of() : GardenOrderReload.current();
            Game.hooks().openMenu(serverPlayer, new SimpleMenuProvider(
                    (id, inventory, owner) -> new GardenWorkMenu(id, inventory, ContainerLevelAccess.create(level, pos), preserving, orders),
                    Component.translatable(getDescriptionId())), buffer -> GardenOrderReload.write(buffer, orders));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
