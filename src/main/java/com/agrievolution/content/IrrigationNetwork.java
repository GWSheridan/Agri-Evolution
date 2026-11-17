package com.agrievolution.content;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Loaded-only bounded graph. Sources are endpoints, never conduits between branches. */
public final class IrrigationNetwork {
    public static final int MAX_NODES=512;

    static boolean hasLoadedNeighborSignal(Level level, BlockPos pos) {
        // Match SignalGetter's weak and conducted power checks without asking for unloaded blocks.
        for (Direction side : SignalGetter.DIRECTIONS) {
            BlockPos neighbor = pos.relative(side);
            if (!level.hasChunkAt(neighbor)) continue;
            BlockState state = level.getBlockState(neighbor);
            if (state.getSignal(level, neighbor, side) > 0) return true;
            if (state.shouldCheckWeakPower(level, neighbor, side) && hasLoadedDirectSignal(level, neighbor)) return true;
        }
        return false;
    }

    private static boolean hasLoadedDirectSignal(Level level, BlockPos pos) {
        for (Direction side : SignalGetter.DIRECTIONS) {
            BlockPos neighbor = pos.relative(side);
            if (level.hasChunkAt(neighbor) && level.getDirectSignal(neighbor, side) > 0) return true;
        }
        return false;
    }

    static void updateComparators(Level level, BlockPos pos, Block source) {
        if (level.isClientSide) return;
        // Match vanilla comparator notifications, including through solid blocks, within loaded chunks.
        for (Direction side : Direction.values()) {
            BlockPos neighbor = pos.relative(side);
            if (!level.hasChunkAt(neighbor)) continue;
            BlockState state = level.getBlockState(neighbor);
            state.onNeighborChange(level, neighbor, pos);
            BlockPos behind = neighbor.relative(side);
            if (!level.hasChunkAt(behind) || !state.isRedstoneConductor(level, neighbor)) continue;
            BlockState behindState = level.getBlockState(behind);
            if (behindState.getWeakChanges(level, behind)) level.neighborChanged(behindState, behind, source, pos, false);
        }
    }

    static void removeFittingEntity(Level level, BlockPos pos, Block source) {
        // The Level removal helper also searches for comparators beyond the loaded chunk.
        level.getChunkAt(pos).removeBlockEntity(pos);
        updateComparators(level, pos, source);
    }

    static void syncLoadedFittings(Level level, BlockPos pos, BlockState state, Block source) {
        for (Direction side : Direction.values()) syncLoadedFitting(level, pos, state, source, side);
    }

    static void syncLoadedFitting(Level level, BlockPos pos, BlockState state, Block source, Direction side) {
        BlockPos neighbor = pos.relative(side);
        if (!level.hasChunkAt(neighbor)) return;
        BlockState current = level.getBlockState(neighbor);
        if (!(current.getBlock() instanceof IrrigationPipeBlock || current.getBlock() instanceof IrrigationValveBlock
                || current.getBlock() instanceof IrrigationSprinklerBlock)) return;
        BlockState updated = current.updateShape(side.getOpposite(), state, level, neighbor, pos);
        if (current != updated && level.setBlock(neighbor, updated, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE)) {
            level.neighborChanged(updated, neighbor, source, pos, false);
        }
    }

    /** Water and activity properties leave the block's shape and ports intact. */
    static int loadedUpdateFlags(Level level, BlockPos pos, int flags) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = pos.relative(side);
            if (!level.hasChunkAt(neighbor) || ((flags & Block.UPDATE_NEIGHBORS) != 0
                    && !level.hasChunkAt(neighbor.relative(side)) && level.getBlockState(pos).hasAnalogOutputSignal()
                    && level.getBlockState(neighbor).isRedstoneConductor(level, neighbor))) {
                return Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
            }
        }
        return flags;
    }

    public static boolean port(BlockState state,Direction side) {
        return state.getBlock() instanceof IrrigationPipeBlock && IrrigationPipeBlock.enabled(state,side)
                || state.getBlock() instanceof IrrigationSprinklerBlock
                || state.getBlock() instanceof IrrigationBasinBlock
                || state.getBlock() instanceof VerticalGreenhouseBlock && VerticalGreenhouseBlock.port(state, side)
                || state.getBlock() instanceof WaterReservoirBlock && WaterReservoirBlock.port(state,side)
                || IrrigationValveBlock.connects(state,side);
    }

    static BlockState loadedConnections(BlockState state, LevelAccessor level, BlockPos pos) {
        for (Direction side : Direction.values()) {
            BlockPos neighbor = pos.relative(side);
            if (level.hasChunkAt(neighbor)) {
                state = state.setValue(IrrigationSprinklerBlock.connection(side),
                        port(state, side) && port(level.getBlockState(neighbor), side.getOpposite()));
            }
        }
        return state;
    }

    public record Supply(BlockPos basin,WaterReservoirBlockEntity reservoir) {
        int water(Level level) {
            if (reservoir != null) return reservoir.water();
            if (!level.hasChunkAt(basin)) return 0;
            var state = level.getBlockState(basin);
            return state.getBlock() instanceof IrrigationBasinBlock && state.getValue(IrrigationBasinBlock.FILLED) ? 1000 : 0;
        }

        int capacity() { return reservoir == null ? 1000 : WaterReservoirBlockEntity.CAPACITY; }

        boolean hasBucket(Level level) {
            return water(level) >= 1000;
        }

        boolean takeBucket(Level level) {
            if (reservoir != null) return reservoir.takeBucket();
            if (!level.hasChunkAt(basin)) return false;
            var state = level.getBlockState(basin);
            return state.getBlock() instanceof IrrigationBasinBlock && state.getValue(IrrigationBasinBlock.FILLED)
                    && IrrigationBasinBlock.setFilled(level, basin, state, false);
        }
    }
    public record Search(boolean attached,List<Supply> supplies,boolean overflow,boolean blockedValve) {}
    public enum Flow {
        READY, EMPTY, CLOSED_VALVE, DISABLED_PORT, DISCONNECTED, TOO_LARGE, UNLOADED_CONNECTION, INCOMPLETE_RESERVOIR,
        PAUSED_BY_REDSTONE, WAITING_FOR_REDSTONE
    }
    public record Route(List<BlockPos> blocks, Flow flow, Direction disabledSide) {}
    record ZoneStatus(List<Supply> supplies, List<BlockPos> sprinklers, boolean limit, BlockPos closedValve,
                      BlockPos disabledPipe, Direction disabledSide, boolean unloadedConnection, boolean incompleteReservoir) {
        boolean blockedPath() {
            return closedValve != null || disabledSide != null;
        }
    }
    private record Endpoint(BlockPos pos, Supply supply) {}
    private record ClosedPort(BlockPos from, BlockPos to, BlockPos pipe, Direction side) {}
    private static final class Trace {
        final Map<BlockPos, BlockPos> parents = new HashMap<>();
        final List<Endpoint> endpoints = new ArrayList<>();
        final List<BlockPos> sprinklers = new ArrayList<>();
        BlockPos valve, lastFitting, unloadedFitting, incompleteReservoir;
        final List<ClosedPort> closedPorts = new ArrayList<>();

        ClosedPort blockingPort() {
            // A loop may reach the other fitting after this closed face was first seen.
            for (ClosedPort port : closedPorts) {
                if (!parents.containsKey(port.to())) return port;
            }
            return null;
        }
    }

    public static Route trace(Level level, BlockPos head) {
        if (!level.hasChunkAt(head)) return new Route(List.of(head), Flow.UNLOADED_CONNECTION, null);
        if (level.getBlockEntity(head) instanceof IrrigationSprinklerBlockEntity sprinkler) {
            var stop = sprinkler.localShutoff();
            if (stop != null) return stop.route(head);
        }
        var trace = new Trace();
        var network = search(level, head, trace);
        if (network.overflow()) return new Route(List.of(head), Flow.TOO_LARGE, null);
        for (var endpoint : trace.endpoints) {
            if (endpoint.supply().hasBucket(level)) return route(trace, endpoint.pos(), Flow.READY);
        }
        if (!trace.endpoints.isEmpty()) return route(trace, trace.endpoints.getFirst().pos(), Flow.EMPTY);
        if (trace.valve != null) return route(trace, trace.valve, Flow.CLOSED_VALVE);
        ClosedPort port = trace.blockingPort();
        if (port != null) {
            var blocks = new ArrayList<>(route(trace, port.from(), Flow.DISABLED_PORT).blocks());
            if (!blocks.getLast().equals(port.pipe())) blocks.add(port.pipe());
            return new Route(List.copyOf(blocks), Flow.DISABLED_PORT, port.side());
        }
        if (trace.unloadedFitting != null) return route(trace, trace.unloadedFitting, Flow.UNLOADED_CONNECTION);
        if (trace.incompleteReservoir != null) return route(trace, trace.incompleteReservoir, Flow.INCOMPLETE_RESERVOIR);
        return route(trace, trace.lastFitting == null ? head : trace.lastFitting, Flow.DISCONNECTED);
    }

    static ZoneStatus inspectZone(Level level, BlockPos valve) {
        var trace = new Trace();
        var network = search(level, valve, trace, true);
        if (network.overflow()) return new ZoneStatus(List.of(), List.of(), true, null, null, null, false, false);
        var port = trace.blockingPort();
        return new ZoneStatus(network.supplies(), List.copyOf(trace.sprinklers), false,
                trace.valve, port == null ? null : port.pipe(), port == null ? null : port.side(),
                trace.unloadedFitting != null, trace.incompleteReservoir != null);
    }

    private static Route route(Trace trace, BlockPos end, Flow flow) {
        var blocks = new ArrayList<BlockPos>();
        for (BlockPos pos = end; pos != null; pos = trace.parents.get(pos)) blocks.add(pos);
        Collections.reverse(blocks);
        return new Route(List.copyOf(blocks), flow, null);
    }

    public static Search search(Level level,BlockPos head) {
        return search(level, head, null);
    }

    private static Search search(Level level, BlockPos head, Trace trace) {
        return search(level, head, trace, false);
    }

    private static Search search(Level level, BlockPos head, Trace trace, boolean inspectClosedOrigin) {
        if (!level.hasChunkAt(head)) return new Search(false, List.of(), false, false);
        var initial=level.getBlockState(head);
        if(!inspectClosedOrigin && initial.getBlock() instanceof IrrigationValveBlock && !IrrigationValveBlock.permitsFlow(level,head,initial)){
            if (trace != null) trace.valve = head;
            return new Search(true,List.of(),false,true);
        }
        Set<BlockPos> seen=new HashSet<>(),sources=new HashSet<>();
        ArrayDeque<BlockPos> queue=new ArrayDeque<>(); List<Supply> supplies=new ArrayList<>();
        seen.add(head);queue.add(head);boolean attached=false,blockedValve=false;
        while(!queue.isEmpty()) {
            var pos=queue.removeFirst();var state=level.getBlockState(pos);
            if (trace != null) trace.lastFitting = pos;
            for(var side:Direction.values()) {
                boolean outgoing = port(state,side);
                if (!outgoing && (trace == null || !(state.getBlock() instanceof IrrigationPipeBlock))) continue;
                var next=pos.relative(side);
                if (seen.contains(next)) continue;
                if (!level.hasChunkAt(next)) {
                    if (trace != null && trace.unloadedFitting == null && outgoing) {
                        // The stored connection tells us the last fitting before the unloaded chunk.
                        var connection = IrrigationSprinklerBlock.connection(side);
                        if (state.hasProperty(connection) && state.getValue(connection)) trace.unloadedFitting = pos;
                    }
                    continue;
                }
                var neighbor=level.getBlockState(next);
                boolean incoming = port(neighbor,side.getOpposite());
                if (!outgoing || !incoming) {
                    if (trace != null) {
                        if (!outgoing && incoming) trace.closedPorts.add(new ClosedPort(pos, next, pos, side));
                        else if (outgoing && neighbor.getBlock() instanceof IrrigationPipeBlock) {
                            trace.closedPorts.add(new ClosedPort(pos, next, next, side.getOpposite()));
                        } else if (!outgoing && neighbor.getBlock() instanceof IrrigationPipeBlock) {
                            trace.closedPorts.add(new ClosedPort(pos, next, pos, side));
                        }
                    }
                    continue;
                }
                seen.add(next);
                if(seen.size()>MAX_NODES)return new Search(true,List.of(),true,true);
                if (trace != null) trace.parents.put(next, pos);
                if(neighbor.getBlock() instanceof IrrigationPipeBlock) {attached=true;queue.add(next);}
                else if(neighbor.getBlock() instanceof IrrigationValveBlock) {
                    if(IrrigationValveBlock.permitsFlow(level,next,neighbor))queue.add(next);
                    else {attached=true;blockedValve=true;if (trace != null && trace.valve == null) trace.valve = next;}
                } else if(neighbor.getBlock() instanceof IrrigationBasinBlock) {
                    if(sources.add(next))addSupply(supplies, trace, next, new Supply(next,null));
                } else if(neighbor.getBlock() instanceof WaterReservoirBlock) {
                    var core = WaterReservoirBlock.core(next, neighbor);
                    if (!sources.add(core)) continue;
                    var status = ReservoirStructure.status(level, core);
                    if (status == ReservoirStructure.Status.COMPLETE && level.getBlockEntity(core) instanceof WaterReservoirBlockEntity tank) {
                        addSupply(supplies, trace, next, new Supply(null, tank));
                    } else if (trace != null) {
                        if (status == ReservoirStructure.Status.UNLOADED && trace.unloadedFitting == null) trace.unloadedFitting = next;
                        if (status == ReservoirStructure.Status.INCOMPLETE && trace.incompleteReservoir == null) trace.incompleteReservoir = next;
                    }
                } else if (trace != null && neighbor.getBlock() instanceof IrrigationSprinklerBlock) {
                    trace.sprinklers.add(next);
                }
            }
        }
        return new Search(attached,List.copyOf(supplies),false,blockedValve);
    }
    private static void addSupply(List<Supply> supplies, Trace trace, BlockPos pos, Supply supply) {
        supplies.add(supply);
        if (trace != null) trace.endpoints.add(new Endpoint(pos, supply));
    }
    private IrrigationNetwork() {}
}
