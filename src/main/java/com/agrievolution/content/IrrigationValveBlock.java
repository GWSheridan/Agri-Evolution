package com.agrievolution.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

/** Flow-control junction. Facing orients the wheel; neighbouring fittings define the ports. */
public final class IrrigationValveBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public IrrigationValveBlock(Properties properties) {
        super(properties);
        BlockState state=defaultBlockState().setValue(FACING, Direction.NORTH).setValue(OPEN, true).setValue(POWERED,false);
        for(var d:Direction.values())state=state.setValue(IrrigationSprinklerBlock.connection(d),false);
        registerDefaultState(state);
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IrrigationValveBlockEntity(pos, state);
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, POWERED);
        for(var d:Direction.values())builder.add(IrrigationSprinklerBlock.connection(d));
    }

    @Override protected BlockState rotate(BlockState state, Rotation rotation) {
        return IrrigationSprinklerBlock.remapConnections(state, rotation::rotate)
                .setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override protected BlockState mirror(BlockState state, Mirror mirror) {
        return IrrigationSprinklerBlock.remapConnections(state, mirror::mirror)
                .setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction direction = context.getHorizontalDirection().getOpposite();
        BlockPos pos = context.getClickedPos();
        for (Direction side : Direction.values()) {
            BlockPos neighbor = pos.relative(side);
            if (context.getLevel().hasChunkAt(neighbor)
                    && (context.getLevel().getBlockState(neighbor).getBlock() instanceof IrrigationBasinBlock
                    || context.getLevel().getBlockState(neighbor).getBlock() instanceof IrrigationPipeBlock
                    || context.getLevel().getBlockState(neighbor).getBlock() instanceof WaterReservoirBlock))
                direction = side.getOpposite();
        }
        // A sprinkler takes priority over the basin and over the player's view direction.
        for (Direction side : Direction.values()) {
            BlockPos neighbor = pos.relative(side);
            if (context.getLevel().hasChunkAt(neighbor)
                    && context.getLevel().getBlockState(neighbor).getBlock() instanceof IrrigationSprinklerBlock) {
                direction = side;
                break;
            }
        }
        return IrrigationNetwork.loadedConnections(defaultBlockState().setValue(FACING, direction)
                .setValue(POWERED, IrrigationNetwork.hasLoadedNeighborSignal(context.getLevel(), pos)), context.getLevel(), pos);
    }

    public static boolean connects(BlockState state, Direction side) {
        return state.getBlock() instanceof IrrigationValveBlock;
    }
    @Override protected BlockState updateShape(BlockState state,Direction side,BlockState neighbor,
            net.minecraft.world.level.LevelAccessor level,BlockPos pos,BlockPos other) {
        return state.setValue(IrrigationSprinklerBlock.connection(side),IrrigationNetwork.port(neighbor,side.getOpposite()));
    }
    private static Direction alignToSprinkler(Level level,BlockPos pos,Direction fallback) {
        Direction head = null;
        Direction pipe = null;
        boolean alignedPipe = false;
        for (Direction side : Direction.values()) {
            BlockPos neighbor = pos.relative(side);
            if (!level.hasChunkAt(neighbor)) continue;
            var block = level.getBlockState(neighbor).getBlock();
            boolean aligned = side.getAxis() == fallback.getAxis();
            if (block instanceof IrrigationSprinklerBlock) {
                if (aligned) return fallback;
                if (head == null) head = side;
            } else if (block instanceof IrrigationPipeBlock) {
                if (aligned) alignedPipe = true;
                pipe = side;
            }
        }
        if (head != null) return head;
        return alignedPipe || pipe == null ? fallback : pipe;
    }

    public static boolean permitsFlow(Level level, BlockPos pos, BlockState state) {
        return state.getValue(OPEN) && !IrrigationNetwork.hasLoadedNeighborSignal(level, pos);
    }

    private static void notifySprinklers(Level level, BlockPos pos) {
        for (Direction side : Direction.values()) {
            BlockPos target=pos.relative(side);
            if (level.hasChunkAt(target) && level.getBlockEntity(target) instanceof IrrigationSprinklerBlockEntity sprinkler)
                sprinkler.checkFlow();
        }
    }

    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean powered = IrrigationNetwork.hasLoadedNeighborSignal(level, pos);
        if (powered != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, powered), IrrigationNetwork.loadedUpdateFlags(level, pos, Block.UPDATE_ALL));
            notifySprinklers(level, pos);
        }
    }

    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor,
                                              BlockPos fromPos, boolean movedByPiston) {
        if (!level.isClientSide) {
            boolean powered=IrrigationNetwork.hasLoadedNeighborSignal(level, pos);
            BlockState next = IrrigationNetwork.loadedConnections(
                    state.setValue(POWERED, powered).setValue(FACING, alignToSprinkler(level, pos, state.getValue(FACING))), level, pos);
            if (state != next) level.setBlock(pos, next, IrrigationNetwork.loadedUpdateFlags(level, pos, Block.UPDATE_ALL));
            notifySprinklers(level,pos);
        }
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                         Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockState next = IrrigationNetwork.loadedConnections(state.cycle(OPEN).setValue(POWERED,IrrigationNetwork.hasLoadedNeighborSignal(level, pos))
                    .setValue(FACING,alignToSprinkler(level,pos,state.getValue(FACING))),level,pos);
            level.setBlock(pos, next, IrrigationNetwork.loadedUpdateFlags(level, pos, Block.UPDATE_ALL));
            player.displayClientMessage(Component.literal(!next.getValue(OPEN) ? "Valve closed - flow through this valve stopped"
                    : next.getValue(POWERED) ? "Valve OPEN, but REDSTONE POWER is holding it shut"
                    : "Valve open - flow permitted through this valve"), true);
            notifySprinklers(level,pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                        Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        useWithoutItem(state,level,pos,player,hit);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = switch(state.getValue(FACING).getAxis()) {
            case Z -> Shapes.or(Block.box(5,5,0,11,11,16),Block.box(3,10,3,13,15,13));
            case X -> Shapes.or(Block.box(0,5,5,16,11,11),Block.box(3,10,3,13,15,13));
            case Y -> Shapes.or(Block.box(5,0,5,11,16,11),Block.box(3,3,1,13,13,6),Block.box(3,3,10,13,13,15));
        };
        for(var d:Direction.values())if(state.getValue(IrrigationSprinklerBlock.connection(d)))shape=Shapes.or(shape,switch(d) {
            case NORTH -> Block.box(5,5,0,11,11,8); case SOUTH -> Block.box(5,5,8,11,11,16);
            case WEST -> Block.box(0,5,5,8,11,11); case EAST -> Block.box(8,5,5,16,11,11);
            case UP -> Block.box(5,8,5,11,16,11); case DOWN -> Block.box(5,0,5,11,8,11);
        });
        return shape;
    }
}

