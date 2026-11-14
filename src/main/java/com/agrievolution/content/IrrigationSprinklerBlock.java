package com.agrievolution.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class IrrigationSprinklerBlock extends Block implements EntityBlock {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static BooleanProperty connection(Direction side) {
        return switch(side) {
            case NORTH -> BlockStateProperties.NORTH; case SOUTH -> BlockStateProperties.SOUTH;
            case EAST -> BlockStateProperties.EAST; case WEST -> BlockStateProperties.WEST;
            case UP -> BlockStateProperties.UP; case DOWN -> BlockStateProperties.DOWN;
        };
    }
    private static final VoxelShape SHAPE = Shapes.or(Block.box(3,14,3,13,16,13),
            Block.box(6,3,6,10,14,10),Block.box(1,1,7,15,5,9),Block.box(7,1,1,9,5,15));

    public IrrigationSprinklerBlock(Properties properties) {
        super(properties);
        BlockState state=defaultBlockState().setValue(ACTIVE,false);
        for(Direction side:Direction.values()) state=state.setValue(connection(side),false);
        registerDefaultState(state);
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
        for(Direction side:Direction.values()) builder.add(connection(side));
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape=SHAPE;
        for(Direction side:Direction.values()) if(state.getValue(connection(side))) {
            shape=Shapes.or(shape,switch(side) {
                case NORTH -> Block.box(5,5,0,11,11,8); case SOUTH -> Block.box(5,5,8,11,11,16);
                case EAST -> Block.box(8,5,5,16,11,11); case WEST -> Block.box(0,5,5,8,11,11);
                case UP -> Block.box(5,8,5,11,16,11); case DOWN -> Block.box(5,0,5,11,8,11);
            });
        }
        return shape;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state=defaultBlockState();
        for(Direction side:Direction.values()) state=state.setValue(connection(side),
                IrrigationNetwork.port(context.getLevel().getBlockState(context.getClickedPos().relative(side)),side.getOpposite()));
        return state;
    }
    @Override protected BlockState updateShape(BlockState state,Direction side,BlockState neighbor,
                                                LevelAccessor level,BlockPos pos,BlockPos neighborPos) {
        return state.setValue(connection(side),IrrigationNetwork.port(neighbor,side.getOpposite()));
    }
    @Override protected void neighborChanged(BlockState state,Level level,BlockPos pos,Block neighbor,BlockPos from,boolean moved) {
        if(!level.isClientSide && level.getBlockEntity(pos) instanceof IrrigationSprinklerBlockEntity sprinkler)
            sprinkler.checkFlow();
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IrrigationSprinklerBlockEntity(pos, state);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != AEBlockEntities.IRRIGATION_SPRINKLER.get()) return null;
        if (level.isClientSide) return (world,pos,current,entity) -> {
            if (!current.getValue(ACTIVE) || world.getGameTime()%2!=0) return;
            // Emit from each downward nozzle, not from a random area below the head.
            // Client-only emission avoids a particle packet stream for every sprinkler.
            for (Direction d : Direction.Plane.HORIZONTAL) {
                double spread=(world.random.nextDouble()-.5)*.65;
                double speed=.10+world.random.nextDouble()*.07;
                double vx=(d.getStepX()*Math.cos(spread)-d.getStepZ()*Math.sin(spread))*speed;
                double vz=(d.getStepZ()*Math.cos(spread)+d.getStepX()*Math.sin(spread))*speed;
                world.addParticle(AEParticles.SPRINKLER_WATER.get(),
                        pos.getX()+.5+d.getStepX()*.375,pos.getY()+.055,
                        pos.getZ()+.5+d.getStepZ()*.375,vx,-.035-world.random.nextDouble()*.025,vz);
            }
        };
        return (world, pos, current, entity) -> {
            if (entity instanceof IrrigationSprinklerBlockEntity sprinkler) {
                sprinkler.checkFlow();
                if (world.getGameTime() % 40 == 0) sprinkler.irrigateCycle();
            }
        };
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                         Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof IrrigationSprinklerBlockEntity sprinkler)
            player.displayClientMessage(Component.literal(sprinkler.water() + "/" + IrrigationSprinklerBlockEntity.CAPACITY
                    + " mB | " + sprinkler.inspectionStatus()), true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                        Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(Items.WATER_BUCKET) && level.getBlockEntity(pos) instanceof IrrigationSprinklerBlockEntity sprinkler) {
            if (sprinkler.water() > IrrigationSprinklerBlockEntity.CAPACITY-1000) {
                if (!level.isClientSide) player.displayClientMessage(Component.literal("Sprinkler needs space for a full 1000 mB bucket."), true);
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            if (!level.isClientSide && sprinkler.fillBucket()) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);var empty=new ItemStack(Items.BUCKET);
                    if(stack.isEmpty())player.setItemInHand(hand,empty);
                    else if(!player.getInventory().add(empty))player.drop(empty,false);
                }
                if(!state.getValue(ACTIVE))sprinkler.irrigateCycle();
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}

