package com.agrievolution.content;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
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

    public static boolean port(BlockState state,Direction side) {
        return state.getBlock() instanceof IrrigationPipeBlock && IrrigationPipeBlock.enabled(state,side)
                || state.getBlock() instanceof IrrigationSprinklerBlock
                || state.getBlock() instanceof IrrigationBasinBlock
                || state.getBlock() instanceof VerticalGreenhouseBlock && VerticalGreenhouseBlock.port(state, side)
                || state.getBlock() instanceof WaterReservoirBlock && WaterReservoirBlock.port(state,side)
                || IrrigationValveBlock.connects(state,side);
    }
    public record Supply(BlockPos basin,WaterReservoirBlockEntity reservoir) {
        boolean hasBucket(Level level) {
            if (reservoir != null) return reservoir.water() >= 1000;
            var state = level.getBlockState(basin);
            return state.getBlock() instanceof IrrigationBasinBlock && state.getValue(IrrigationBasinBlock.FILLED);
        }

        boolean takeBucket(Level level) {
            if(reservoir!=null)return reservoir.takeBucket();
            var state=level.getBlockState(basin);
            return state.getBlock() instanceof IrrigationBasinBlock && state.getValue(IrrigationBasinBlock.FILLED)
                    && level.setBlock(basin,state.setValue(IrrigationBasinBlock.FILLED,false),3);
        }
    }
    public record Search(boolean attached,List<Supply> supplies,boolean overflow,boolean blockedValve) {}
    public enum Flow { READY, EMPTY, CLOSED_VALVE, DISABLED_PORT, DISCONNECTED, TOO_LARGE, UNLOADED_CONNECTION }
    public record Route(List<BlockPos> blocks, Flow flow, Direction disabledSide) {}
    record ZoneStatus(List<Supply> supplies, List<BlockPos> sprinklers, boolean limit, boolean blockedPath,
                      boolean unloadedConnection) {}
    private record Endpoint(BlockPos pos, Supply supply) {}
    private record ClosedPort(BlockPos from, BlockPos pipe, Direction side) {}
    private static final class Trace {
        final Map<BlockPos, BlockPos> parents = new HashMap<>();
        final List<Endpoint> endpoints = new ArrayList<>();
        final List<BlockPos> sprinklers = new ArrayList<>();
        BlockPos valve, lastFitting, unloadedFitting;
        ClosedPort port;
    }

    public static Route trace(Level level, BlockPos head) {
        var trace = new Trace();
        var network = search(level, head, trace);
        // A sprinkler pauses for either of these adjacent blockages even with a second supply.
        boolean adjacentValve = trace.valve != null && head.equals(trace.parents.get(trace.valve));
        boolean adjacentUnloaded = head.equals(trace.unloadedFitting);
        if ((adjacentValve || adjacentUnloaded)
                && level.getBlockState(head).getBlock() instanceof IrrigationSprinklerBlock) {
            return adjacentValve ? route(trace, trace.valve, Flow.CLOSED_VALVE)
                    : route(trace, head, Flow.UNLOADED_CONNECTION);
        }
        if (network.overflow()) return new Route(List.of(head), Flow.TOO_LARGE, null);
        for (var endpoint : trace.endpoints) {
            if (endpoint.supply().hasBucket(level)) return route(trace, endpoint.pos(), Flow.READY);
        }
        if (!trace.endpoints.isEmpty()) return route(trace, trace.endpoints.getFirst().pos(), Flow.EMPTY);
        if (trace.valve != null) return route(trace, trace.valve, Flow.CLOSED_VALVE);
        if (trace.port != null) {
            var blocks = new ArrayList<>(route(trace, trace.port.from(), Flow.DISABLED_PORT).blocks());
            if (!blocks.getLast().equals(trace.port.pipe())) blocks.add(trace.port.pipe());
            return new Route(List.copyOf(blocks), Flow.DISABLED_PORT, trace.port.side());
        }
        if (trace.unloadedFitting != null) return route(trace, trace.unloadedFitting, Flow.UNLOADED_CONNECTION);
        return route(trace, trace.lastFitting == null ? head : trace.lastFitting, Flow.DISCONNECTED);
    }

    static ZoneStatus inspectZone(Level level, BlockPos valve) {
        var trace = new Trace();
        var network = search(level, valve, trace, true);
        return new ZoneStatus(network.supplies(), List.copyOf(trace.sprinklers), network.overflow(),
                network.blockedValve() || trace.port != null, trace.unloadedFitting != null);
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
        if(!level.hasChunkAt(head))return new Search(true,List.of(),false,true);
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
                    if (trace != null && trace.port == null) {
                        if (!outgoing && incoming) trace.port = new ClosedPort(pos, pos, side);
                        else if (outgoing && neighbor.getBlock() instanceof IrrigationPipeBlock) {
                            trace.port = new ClosedPort(pos, next, side.getOpposite());
                        } else if (!outgoing && neighbor.getBlock() instanceof IrrigationPipeBlock) {
                            trace.port = new ClosedPort(pos, pos, side);
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
                    var core=WaterReservoirBlock.core(next,neighbor);
                    if(sources.add(core) && level.hasChunkAt(core)
                            && level.getBlockEntity(core) instanceof WaterReservoirBlockEntity tank && tank.complete())
                        addSupply(supplies, trace, next, new Supply(null,tank));
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
