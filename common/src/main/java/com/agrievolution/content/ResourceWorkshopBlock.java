package com.agrievolution.content;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class ResourceWorkshopBlock extends BaseEntityBlock {
    public static final IntegerProperty TIER = IntegerProperty.create("tier", 1, 3);
    private static final MapCodec<ResourceWorkshopBlock> CODEC = simpleCodec(ResourceWorkshopBlock::new);

    public ResourceWorkshopBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH).setValue(TIER, 1));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING, TIER);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(BlockStateProperties.HORIZONTAL_FACING, rotation.rotate(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
    }

    @Override protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ResourceWorkshopBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, AEBlockEntities.RESOURCE_WORKSHOP.get(),
                (world, pos, current, workshop) -> workshop.process());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof ResourceWorkshopBlockEntity workshop
                && WorkstationAccess.mayConfigure(workshop, player)) server.openMenu(workshop);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand == InteractionHand.MAIN_HAND && stack.isEmpty() && upgradeTier(player.getOffhandItem()) != 0) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        int upgrade = upgradeTier(stack);
        if (upgrade == 0) {
            if (!stack.isEmpty()) useWithoutItem(state, level, pos, player, hit);
            return stack.isEmpty() ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                    : ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        var before = stack.copy();
        if (!(level.getBlockEntity(pos) instanceof ResourceWorkshopBlockEntity workshop)
                || !WorkstationAccess.mayConfigure(workshop, player)
                || !WorkstationAccess.mayUse(level, pos, player, hit.getDirection(), stack)
                || !WorkstationAccess.mayCompleteItemUse(level, player, hand, stack, before)
                || !WorkstationAccess.stillValid(workshop, player) || level.getBlockState(pos) != state) {
            return ItemInteractionResult.FAIL;
        }
        if (state.getValue(TIER) + 1 != upgrade) {
            player.displayClientMessage(Component.translatable("message.agri_evolution.resource_workshop.upgrade_order"), true);
        } else if (level.setBlock(pos, state.setValue(TIER, upgrade), Block.UPDATE_ALL) && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return ItemInteractionResult.CONSUME;
    }

    private static int upgradeTier(ItemStack stack) {
        if (stack.is(AEItems.WORKSHOP_REFINEMENT_UPGRADE.get())) return 2;
        return stack.is(AEItems.WORKSHOP_MASTERY_UPGRADE.get()) ? 3 : 0;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!replacement.is(this) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof ResourceWorkshopBlockEntity workshop) {
            Containers.dropContents(level, pos, workshop);
            if (state.getValue(TIER) >= 2) popResource(level, pos, new ItemStack(AEItems.WORKSHOP_REFINEMENT_UPGRADE.get()));
            if (state.getValue(TIER) >= 3) popResource(level, pos, new ItemStack(AEItems.WORKSHOP_MASTERY_UPGRADE.get()));
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
