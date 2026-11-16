package com.agrievolution.content;

import java.util.function.UnaryOperator;
import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.shapes.*;

public final class IrrigationPipeBlock extends Block {
    private static final net.minecraft.world.level.block.state.properties.BooleanProperty[] BLOCKED=java.util.Arrays.stream(Direction.values()).map(d->net.minecraft.world.level.block.state.properties.BooleanProperty.create("blocked_"+d.getName())).toArray(net.minecraft.world.level.block.state.properties.BooleanProperty[]::new);
    public static boolean enabled(BlockState state,Direction side){return !state.getValue(BLOCKED[side.ordinal()]);}
    public static boolean toggle(Level level, BlockPos pos, Direction side) {
        BlockPos neighborPos = pos.relative(side);
        if (!level.hasChunkAt(pos) || !level.hasChunkAt(neighborPos)) return false;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof IrrigationPipeBlock)) return false;
        var neighbor = level.getBlockState(neighborPos);
        boolean connected = IrrigationNetwork.port(neighbor, side.getOpposite());
        var next = state.cycle(BLOCKED[side.ordinal()]);
        next = next.setValue(IrrigationSprinklerBlock.connection(side), enabled(next, side) && connected);
        for (Direction other : Direction.Plane.HORIZONTAL) {
            if (!level.hasChunkAt(pos.relative(other))) {
                // Only the selected face changed. Sync its loaded neighbor without broadcasting across the border.
                int quietUpdate = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
                if (!level.setBlock(pos, next, quietUpdate)) return false;
                IrrigationNetwork.syncLoadedFitting(level, pos, next, state.getBlock(), side);
                return true;
            }
        }
        return level.setBlock(pos, next, Block.UPDATE_ALL);
    }

    @Override protected BlockState rotate(BlockState state, Rotation rotation) {
        return remapPorts(state, rotation::rotate);
    }

    @Override protected BlockState mirror(BlockState state, Mirror mirror) {
        return remapPorts(state, mirror::mirror);
    }

    private static BlockState remapPorts(BlockState state, UnaryOperator<Direction> transform) {
        BlockState result = IrrigationSprinklerBlock.remapConnections(state, transform);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            result = result.setValue(BLOCKED[transform.apply(side).ordinal()], state.getValue(BLOCKED[side.ordinal()]));
        }
        return result;
    }

    public IrrigationPipeBlock(Properties props) {
        super(props);
        BlockState state=defaultBlockState();
        for(var d:Direction.values())state=state.setValue(IrrigationSprinklerBlock.connection(d),false).setValue(BLOCKED[d.ordinal()],false);
        registerDefaultState(state);
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {
        for(var d:Direction.values())builder.add(IrrigationSprinklerBlock.connection(d));
        builder.add(BLOCKED);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return IrrigationNetwork.loadedConnections(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }
    @Override protected BlockState updateShape(BlockState state,Direction side,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos other) {
        return state.setValue(IrrigationSprinklerBlock.connection(side),enabled(state,side)&&IrrigationNetwork.port(neighbor,side.getOpposite()));
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        VoxelShape shape=Block.box(5,5,5,11,11,11);
        for(var d:Direction.values())if(state.getValue(IrrigationSprinklerBlock.connection(d)))shape=Shapes.or(shape,switch(d) {
            case NORTH -> Block.box(5,5,0,11,11,8);case SOUTH -> Block.box(5,5,8,11,11,16);
            case WEST -> Block.box(0,5,5,8,11,11);case EAST -> Block.box(8,5,5,16,11,11);
            case UP -> Block.box(5,8,5,11,16,11);case DOWN -> Block.box(5,0,5,11,8,11);
        });
        return shape;
    }
}
