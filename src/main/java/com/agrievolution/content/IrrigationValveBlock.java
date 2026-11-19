package com.agrievolution.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
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
        return connectedState(context.getLevel(), context.getClickedPos(),
                defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()));
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof IrrigationValveBlockEntity valve) {
            String name = IrrigationValveBlockEntity.storedZone(stack);
            if (!name.isEmpty()) valve.rename(name);
        }
    }

    public static boolean connects(BlockState state, Direction side) {
        return state.getBlock() instanceof IrrigationValveBlock;
    }
    @Override protected BlockState updateShape(BlockState state,Direction side,BlockState neighbor,
            net.minecraft.world.level.LevelAccessor level,BlockPos pos,BlockPos other) {
        return state.setValue(IrrigationSprinklerBlock.connection(side),IrrigationNetwork.port(neighbor,side.getOpposite()));
    }
    private static BlockState connectedState(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        Direction best = facing;
        int bestPriority = 0;
        for (Direction side : Direction.values()) {
            BlockPos neighbor = pos.relative(side);
            if (!level.hasChunkAt(neighbor)) continue;
            BlockState fitting = level.getBlockState(neighbor);
            boolean connected = IrrigationNetwork.port(fitting, side.getOpposite());
            state = state.setValue(IrrigationSprinklerBlock.connection(side), connected);
            if (!connected) continue;

            int priority = switch (fitting.getBlock()) {
                case IrrigationSprinklerBlock head -> 3;
                case IrrigationPipeBlock pipe -> 2;
                default -> 1;
            };
            boolean aligned = side.getAxis() == facing.getAxis();
            if (priority > bestPriority || (priority == bestPriority && aligned)) {
                best = aligned ? facing : side;
                bestPriority = priority;
            }
        }
        return state.setValue(FACING, best)
                .setValue(POWERED, IrrigationNetwork.hasLoadedNeighborSignal(level, pos));
    }

    public static boolean permitsFlow(Level level, BlockPos pos, BlockState state) {
        return permitsFlow(level, pos, state, false, null);
    }

    static boolean permitsFlow(Level level, BlockPos pos, BlockState state, boolean refill, BlockPos closedController) {
        return hardwareOpen(level, pos, state) && (!(level.getBlockEntity(pos) instanceof IrrigationValveBlockEntity valve)
                || ((closedController == null || !valve.controlledBy(closedController)) && valve.scheduleAllowsFlow(refill)));
    }

    static boolean hardwareOpen(Level level, BlockPos pos, BlockState state) {
        return state.getValue(OPEN) && !IrrigationNetwork.hasLoadedNeighborSignal(level, pos);
    }

    static String flowStatus(Level level, BlockPos pos, BlockState state) {
        return "Valve " + flowState(level, pos, state);
    }

    static String namedFlowStatus(Level level, BlockPos pos, String label) {
        String status = label + " " + flowState(level, pos, level.getBlockState(pos));
        if (level.getBlockEntity(pos) instanceof IrrigationValveBlockEntity valve && !valve.zoneName().isEmpty()) {
            return status + " (" + valve.zoneName() + ")";
        }
        return status;
    }

    private static String flowState(Level level, BlockPos pos, BlockState state) {
        if (!state.getValue(OPEN)) return "closed";
        if (!hardwareOpen(level, pos, state)) return "paused by redstone";
        if (!permitsFlow(level, pos, state)) return "waiting for schedule";
        if (!permitsFlow(level, pos, state, true, null)
                && level.getBlockEntity(pos) instanceof IrrigationValveBlockEntity valve) {
            return "open; " + valve.reserveStatus();
        }
        return "open";
    }

    static void notifySprinklers(Level level, BlockPos pos) {
        for (Direction side : Direction.values()) {
            BlockPos target=pos.relative(side);
            if (level.hasChunkAt(target) && level.getBlockEntity(target) instanceof IrrigationSprinklerBlockEntity sprinkler) {
                sprinkler.checkFlow();
            }
        }
    }

    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof IrrigationValveBlockEntity valve) valve.clearMissingController();
        refreshConnections(level, pos, state);
        IrrigationNetwork.syncLoadedFittings(level, pos, level.getBlockState(pos), this);
        notifySprinklers(level, pos);
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (state.is(replacement.getBlock())) return;
        IrrigationNetwork.removeFittingEntity(level, pos, this);
        IrrigationNetwork.syncLoadedFittings(level, pos, replacement, this);
    }

    private static void refreshConnections(Level level, BlockPos pos, BlockState state) {
        BlockState next = connectedState(level, pos, state);
        if (state != next) level.setBlock(pos, next, IrrigationNetwork.loadedUpdateFlags(level, pos, Block.UPDATE_ALL));
    }

    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor,
                                              BlockPos fromPos, boolean movedByPiston) {
        if (!level.isClientSide) {
            refreshConnections(level, pos, state);
            notifySprinklers(level, pos);
        }
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                         Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockState next = connectedState(level, pos, state.cycle(OPEN));
            level.setBlock(pos, next, IrrigationNetwork.loadedUpdateFlags(level, pos, Block.UPDATE_ALL));
            player.displayClientMessage(Component.literal(!next.getValue(OPEN) ? "Valve closed - flow through this valve stopped"
                    : flowStatus(level, pos, next)), true);
            notifySprinklers(level, pos);
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

