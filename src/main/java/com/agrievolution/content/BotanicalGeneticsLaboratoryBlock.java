package com.agrievolution.content;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Independent upgradable genetics workstation, no changes to existing block IDs. */
public final class BotanicalGeneticsLaboratoryBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final MapCodec<BotanicalGeneticsLaboratoryBlock> CODEC = simpleCodec(BotanicalGeneticsLaboratoryBlock::new);

    public BotanicalGeneticsLaboratoryBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BotanicalGeneticsLaboratoryBlockEntity(pos, state);
    }
    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, AEBlockEntities.BOTANICAL_GENETICS_LABORATORY.get(),
                BotanicalGeneticsLaboratoryBlockEntity::serverTick);
    }
    private InteractionResult open(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide && player instanceof ServerPlayer server
                && level.getBlockEntity(pos) instanceof BotanicalGeneticsLaboratoryBlockEntity lab) {
            server.openMenu(lab);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                         Player player, BlockHitResult hit) {
        return open(level, pos, player);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                       Player player, InteractionHand hand, BlockHitResult hit) {
        boolean upgrade = stack.is(AEItems.LABORATORY_RESEARCH_UPGRADE.get()) || stack.is(AEItems.LABORATORY_HYBRIDIZATION_UPGRADE.get());
        if (upgrade && !level.isClientSide && (!(level.getBlockEntity(pos) instanceof BotanicalGeneticsLaboratoryBlockEntity target)
                || !WorkstationAccess.mayConfigure(target, player))) return ItemInteractionResult.FAIL;
        if (stack.is(AEItems.LABORATORY_HYBRIDIZATION_UPGRADE.get())
                && level.getBlockEntity(pos) instanceof BotanicalGeneticsLaboratoryBlockEntity lab) {
            if (!level.isClientSide) {
                if (lab.installHybridUpgrade()) {
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                    player.displayClientMessage(Component.literal("Tier 3: Advanced Hybrid Breeding unlocked!"), true);
                } else player.displayClientMessage(Component.literal(lab.isHybridUpgraded()
                        ? "Advanced hybridisation already installed" : "Install the Advanced Research upgrade first"), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.is(AEItems.LABORATORY_RESEARCH_UPGRADE.get())
                && level.getBlockEntity(pos) instanceof BotanicalGeneticsLaboratoryBlockEntity lab) {
            if (!lab.isUpgraded()) {
                if (!level.isClientSide) {
                    if (lab.installUpgrade()) {
                        if (!player.getAbilities().instabuild) stack.shrink(1);
                        player.displayClientMessage(Component.literal("Advanced Research unlocked: EVOLVE"), true);
                    }
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            if (!level.isClientSide) player.displayClientMessage(Component.literal("Laboratory already upgraded"), true);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        open(level, pos, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof BotanicalGeneticsLaboratoryBlockEntity lab) {
            Containers.dropContents(level, pos, lab);
            if (lab.isUpgraded()) Block.popResource(level, pos,
                    new ItemStack(AEItems.LABORATORY_RESEARCH_UPGRADE.get()));
            if (lab.isHybridUpgraded()) Block.popResource(level, pos,
                    new ItemStack(AEItems.LABORATORY_HYBRIDIZATION_UPGRADE.get()));
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, next, moving);
    }
}
