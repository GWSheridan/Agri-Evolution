package com.agrievolution.content;

import com.agrievolution.platform.Game;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class VerticalGreenhouseBlock extends BaseEntityBlock {
    public static final IntegerProperty SECTION = IntegerProperty.create("section", 0, 2);
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final MapCodec<VerticalGreenhouseBlock> CODEC = simpleCodec(VerticalGreenhouseBlock::new);

    public VerticalGreenhouseBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(SECTION, 0).setValue(FACING, Direction.NORTH));
    }

    @Override protected MapCodec<VerticalGreenhouseBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(SECTION, FACING); }

    @Override protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos base = context.getClickedPos();
        Level level = context.getLevel();
        if (level.isOutsideBuildHeight(base.above(2)) || !level.isEmptyBlock(base.above()) || !level.isEmptyBlock(base.above(2))) return null;
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level.isClientSide || state.getValue(SECTION) != 0) return;
        for (int section = 1; section <= 2; section++) {
            var part = pos.above(section);
            level.setBlock(part, state.setValue(SECTION, section), IrrigationNetwork.loadedUpdateFlags(level, part, UPDATE_ALL));
        }
    }

    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        if (!level.isClientSide && !old.is(this) && state.getValue(SECTION) == 0) {
            IrrigationNetwork.syncLoadedFittings(level, pos, state, this);
        }
    }

    static boolean matches(BlockState state, Block block, Direction facing, int section) {
        return state.is(block) && state.getValue(SECTION) == section && state.getValue(FACING) == facing;
    }

    public static boolean port(BlockState state, Direction side) { return state.getValue(SECTION) == 0 && side != Direction.UP; }

    static VerticalGreenhouseBlockEntity controller(Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof VerticalGreenhouseBlock) || !level.hasChunkAt(pos)
                || level.getBlockState(pos) != state) return null;
        BlockPos base = pos.below(state.getValue(SECTION));
        return level.hasChunkAt(base) && matches(level.getBlockState(base), state.getBlock(), state.getValue(FACING), 0)
                && level.getBlockEntity(base) instanceof VerticalGreenhouseBlockEntity greenhouse
                && greenhouse.getLevel() == level && WorkstationAccess.isCurrent(greenhouse) ? greenhouse : null;
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(SECTION) == 0 ? new VerticalGreenhouseBlockEntity(pos, state) : null;
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || state.getValue(SECTION) != 0 ? null
                : createTickerHelper(type, AEBlockEntities.VERTICAL_GREENHOUSE.get(), VerticalGreenhouseBlockEntity::serverTick);
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server) {
            var greenhouse = controller(level, pos, state);
            if (greenhouse != null) {
                server.openMenu(greenhouse);
                if (greenhouse.complete()) FarmingMilestones.GREENHOUSE.award(server);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                        Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand == InteractionHand.MAIN_HAND && stack.isEmpty() && player.getOffhandItem().is(Items.WATER_BUCKET))
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if (!stack.is(Items.WATER_BUCKET)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        var before = stack.copy();
        if (!WorkstationAccess.mayCompleteItemUse(level, player, hand, stack, before)) return ItemInteractionResult.FAIL;
        var greenhouse = controller(level, pos, state);
        if (greenhouse == null) return ItemInteractionResult.FAIL;
        var baseState = level.getBlockState(greenhouse.getBlockPos());
        if (!WorkstationAccess.mayUse(level, pos, player, hit.getDirection(), stack)
                || !currentFill(level, pos, state, greenhouse, baseState, player, hand, stack, before)) return ItemInteractionResult.FAIL;
        if (!level.isClientSide) {
            if (!WorkstationAccess.mayUse(level, greenhouse.getBlockPos(), player, hit.getDirection(), stack)
                    || !currentFill(level, pos, state, greenhouse, baseState, player, hand, stack, before))
                return ItemInteractionResult.FAIL;
            if (greenhouse.addBucket()) {
                if (!player.getAbilities().instabuild)
                    player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
            } else player.displayClientMessage(Component.literal("The greenhouse needs a complete column and room for 1000 mB."), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private boolean currentFill(Level level, BlockPos pos, BlockState state, VerticalGreenhouseBlockEntity greenhouse,
                                BlockState baseState, Player player, InteractionHand hand, ItemStack stack, ItemStack before) {
        return WorkstationAccess.mayCompleteItemUse(level, player, hand, stack, before)
                && state.is(this) && controller(level, pos, state) == greenhouse
                && level.getBlockState(greenhouse.getBlockPos()) == baseState;
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (level.isClientSide || newState.is(this)) {
            super.onRemove(state, level, pos, newState, moving);
            return;
        }
        int section = state.getValue(SECTION);
        if (section == 0 && level.getBlockEntity(pos) instanceof VerticalGreenhouseBlockEntity greenhouse) {
            Containers.dropContents(level, pos, greenhouse);
        }
        BlockPos base = pos.below(section);
        for (int other = 0; other <= 2; other++) {
            if (other == section) continue;
            var part = base.above(other);
            var current = level.getBlockState(part);
            if (!matches(current, this, state.getValue(FACING), other)) continue;
            level.setBlock(part, current.getFluidState().createLegacyBlock(), IrrigationNetwork.loadedUpdateFlags(level, part, UPDATE_ALL));
        }
        if (section == 0) {
            IrrigationNetwork.removeFittingEntity(level, pos, this);
            IrrigationNetwork.syncLoadedFittings(level, pos, newState, this);
            Game.hooks().invalidateTransfers(level, pos);
        }
    }
}
