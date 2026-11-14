package com.agrievolution.content;

import com.mojang.serialization.MapCodec;
import com.agrievolution.farming.SoilResearch;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Persistent, directional workstation. Upgrade a placed table without changing its registry ID. */
public final class SoilResearchTableBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty TIER = IntegerProperty.create("tier", 1, 3);
    private static final MapCodec<SoilResearchTableBlock> CODEC = simpleCodec(SoilResearchTableBlock::new);

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    public SoilResearchTableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TIER, 1));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, TIER);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SoilResearchTableBlockEntity(pos, state);
    }

    private InteractionResult open(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide && player instanceof ServerPlayer server
                && level.getBlockEntity(pos) instanceof SoilResearchTableBlockEntity workstation)
            server.openMenu(workstation);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                          Player player, BlockHitResult hit) {
        return open(level, pos, player);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                              BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        boolean upgradeTwo = stack.is(AEItems.SOIL_RESEARCH_REFINEMENT_UPGRADE.get());
        boolean upgradeThree = stack.is(AEItems.SOIL_RESEARCH_MASTERY_UPGRADE.get());
        if (upgradeTwo || upgradeThree) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof SoilResearchTableBlockEntity lab) {
                int next = state.getValue(TIER) + 1;
                int samples = SoilResearch.analyzed(lab.getItem(SoilResearchTableBlockEntity.SAMPLER));
                int expected = upgradeTwo ? 2 : 3;
                if (next > 3) lab.setStatus(14);
                else if (next != expected) lab.setStatus(15);
                else if (samples < (expected == 2 ? 48 : 72)) lab.setStatus(16);
                else {
                    level.setBlock(pos, state.setValue(TIER, expected), 3);
                    lab.setStatus(13);
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        open(level, pos, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos,
                                     BlockState newer, boolean moving) {
        // A Tier upgrade changes only properties and MUST NOT drop the workstation inventory.
        if (!state.is(newer.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof SoilResearchTableBlockEntity work) {
            Containers.dropContents(level, pos, work);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, newer, moving);
    }
}
