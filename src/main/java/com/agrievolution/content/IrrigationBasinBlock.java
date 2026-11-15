package com.agrievolution.content;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A refillable irrigation basin that keeps nearby farmland permanently hydrated while filled. */
public final class IrrigationBasinBlock extends Block implements EntityBlock {
    public static final BooleanProperty FILLED = BooleanProperty.create("filled");
    public static final IntegerProperty UPGRADE = IntegerProperty.create("upgrade", 0, 2);
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1, 0, 1, 15, 2, 15),
            Block.box(1, 2, 1, 3, 11, 15),
            Block.box(13, 2, 1, 15, 11, 15),
            Block.box(3, 2, 1, 13, 11, 3),
            Block.box(3, 2, 13, 13, 11, 15));

    public IrrigationBasinBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FILLED, false).setValue(UPGRADE, 0));
    }

    static boolean setFilled(Level level, BlockPos pos, BlockState state, boolean filled) {
        return level.setBlock(pos, state.setValue(FILLED, filled),
                IrrigationNetwork.loadedUpdateFlags(level, pos, Block.UPDATE_ALL));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FILLED, UPGRADE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IrrigationBasinBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != AEBlockEntities.IRRIGATION_BASIN.get()) return null;
        return (world, pos, current, entity) -> {
            if (entity instanceof IrrigationBasinBlockEntity basin) {
                IrrigationBasinBlockEntity.serverTick(world, pos, current, basin);
            }
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof IrrigationBasinBlockEntity basin) {
            player.displayClientMessage(Component.literal("Irrigation Basin Mk " + (basin.upgradeTier() + 1)
                    + " - watering radius " + basin.wateringRadius() + " blocks"
                    + (basin.upgradeTier() == 2 ? " - source-water auto-refill" : "")), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        boolean filling = stack.is(Items.WATER_BUCKET);
        if ((!filling && !stack.is(Items.BUCKET)) || filling == state.getValue(FILLED)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && setFilled(level, pos, state, filling)) {
            if (!player.getAbilities().instabuild) {
                var result = new ItemStack(filling ? Items.BUCKET : Items.WATER_BUCKET);
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, result));
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

}
