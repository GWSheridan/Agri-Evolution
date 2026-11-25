package com.agrievolution.content;

import com.agrievolution.client.SeedAnalyzerClientScreens;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Read-only workstation. Seeds stay safely in the player's hand, so no item handler,
 * client-to-server packet, or persistent data can accidentally destroy their genomes.
 * The ItemStack's synchronized CUSTOM_DATA contains the actual seed genetics.
 */
public final class SeedAnalyzerBlock extends HorizontalDirectionalBlock {
    // NeoForge / Minecraft 1.21.1 requires a codec for every custom directional block.
    public static final MapCodec<SeedAnalyzerBlock> CODEC = simpleCodec(SeedAnalyzerBlock::new);

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    public SeedAnalyzerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                               BlockPos pos, Player player, InteractionHand hand,
                                               BlockHitResult hit) {
        if (!(stack.getItem() instanceof SpeciesSeedItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            SeedAnalyzerClientScreens.open(stack, hand);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        // Do not intercept non-seed tools or prevent normal block placement.
        if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        if (level.isClientSide) {
            SeedAnalyzerClientScreens.open(ItemStack.EMPTY);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
