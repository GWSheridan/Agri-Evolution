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
import net.minecraft.world.level.block.RenderShape;
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

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos base = context.getClickedPos();
        Level level = context.getLevel();
        if (level.isOutsideBuildHeight(base.above(2)) || !level.isEmptyBlock(base.above()) || !level.isEmptyBlock(base.above(2))) return null;
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level.isClientSide || state.getValue(SECTION) != 0) return;
        level.setBlock(pos.above(), state.setValue(SECTION, 1), UPDATE_ALL);
        level.setBlock(pos.above(2), state.setValue(SECTION, 2), UPDATE_ALL);
    }

    static boolean matches(BlockState state, Block block, Direction facing, int section) {
        return state.is(block) && state.getValue(SECTION) == section && state.getValue(FACING) == facing;
    }

    public static boolean port(BlockState state, Direction side) { return state.getValue(SECTION) == 0 && side != Direction.UP; }

    static VerticalGreenhouseBlockEntity controller(Level level, BlockPos pos, BlockState state) {
        BlockPos base = pos.below(state.getValue(SECTION));
        return matches(level.getBlockState(base), state.getBlock(), state.getValue(FACING), 0)
                && level.getBlockEntity(base) instanceof VerticalGreenhouseBlockEntity greenhouse ? greenhouse : null;
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
        if (!stack.is(Items.WATER_BUCKET)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            var greenhouse = controller(level, pos, state);
            if (greenhouse != null && greenhouse.addBucket()) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
            } else player.displayClientMessage(Component.literal("The greenhouse needs a complete column and room for 1000 mB."), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!level.isClientSide && !newState.is(this)) {
            int section = state.getValue(SECTION);
            if (section == 0 && level.getBlockEntity(pos) instanceof VerticalGreenhouseBlockEntity greenhouse) {
                Containers.dropContents(level, pos, greenhouse);
            }
            BlockPos base = pos.below(section);
            for (int other = 0; other <= 2; other++) {
                if (other == section) continue;
                var part = base.above(other);
                if (matches(level.getBlockState(part), this, state.getValue(FACING), other)) level.removeBlock(part, false);
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
