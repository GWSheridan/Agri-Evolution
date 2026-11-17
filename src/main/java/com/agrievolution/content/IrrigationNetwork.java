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
    private static final int BUCKET = 1000;

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
            return state.getBlock() instanceof IrrigationBasinBlock && state.getValue(IrrigationBasinBlock.FILLED) ? BUCKET : 0;
        }

        int capacity() { return reservoir == null ? BUCKET : WaterReservoirBlockEntity.CAPACITY; }

        boolean takeBucket(Level level) {
            if (reservoir != null) return reservoir.takeBucket();
            if (!level.hasChunkAt(basin)) return false;
            var state = level.getBlockState(basin);
            return state.getBlock() instanceof IrrigationBasinBlock && state.getValue(IrrigationBasinBlock.FILLED)
                    && IrrigationBasinBlock.setFilled(level, basin, state, false);
        }
    }
    record SupplyWater(int total, int capacity, int ready, int fullest) {}

    static SupplyWater supplyWater(Level level, List<Supply> supplies) {
        int total = 0, capacity = 0, ready = 0, fullest = 0;
        for (Supply supply : supplies) {
            int water = supply.water(level);
            total += water;
            capacity += supply.capacity();
            if (water >= BUCKET) ready++;
            fullest = Math.max(fullest, water);
        }
        return new SupplyWater(total, capacity, ready, fullest);
    }

    public record Search(boolean attached,List<Supply> supplies,boolean overflow,boolean blockedValve) {}
    public enum Flow {
        READY, EMPTY, CLOSED_VALVE, DISABLED_PORT, DISCONNECTED, TOO_LARGE, UNLOADED_CONNECTION, INCOMPLETE_RESERVOIR,
        PAUSED_BY_REDSTONE, WAITING_FOR_REDSTONE, INCOMPLETE_GREENHOUSE, UNLOADED_RESERVOIR
    }
    public record Route(List<BlockPos> blocks, Flow flow, Direction disabledSide, Direction boundarySide, int sourceWater) {
        public Route(List<BlockPos> blocks, Flow flow, Direction disabledSide) {
            this(blocks, flow, disabledSide, null, -1);
        }
    }
    record ZoneStatus(List<Supply> supplies, List<BlockPos> sprinklers, boolean limit, BlockPos closedValve,
                      BlockPos disabledPipe, Direction disabledSide, UnloadedStop unloaded, boolean incompleteReservoir) {
        boolean unloadedConnection() { return unloaded != null; }
        boolean blockedPath() {
            return closedValve != null || disabledSide != null;
        }
    }
    private record Endpoint(BlockPos pos, Supply supply) {}
    private record ClosedPort(BlockPos from, BlockPos to, BlockPos pipe, Direction side) {}
    record UnloadedStop(BlockPos fitting, Direction side) {
        Flow flow() { return side == null ? Flow.UNLOADED_RESERVOIR : Flow.UNLOADED_CONNECTION; }
    }

    static String unloadedMessage(Direction side) {
        return side == null ? "Connection leads into an unloaded chunk"
                : "Connection leads " + side.getName() + " into an unloaded chunk";
    }
    private static final class Trace {
        final Map<BlockPos, BlockPos> parents = new HashMap<>();
        final List<Endpoint> endpoints = new ArrayList<>();
        final List<BlockPos> sprinklers = new ArrayList<>();
        BlockPos valve, lastFitting, incompleteReservoir;
        UnloadedStop unloaded;
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
        BlockState state = level.getBlockState(head);
        if (state.getBlock() instanceof VerticalGreenhouseBlock) {
            var greenhouse = VerticalGreenhouseBlock.controller(level, head, state);
            if (greenhouse == null || !greenhouse.complete()) return new Route(List.of(head), Flow.INCOMPLETE_GREENHOUSE, null);
            head = greenhouse.getBlockPos();
        }
        if (level.getBlockEntity(head) instanceof IrrigationSprinklerBlockEntity sprinkler) {
            var stop = sprinkler.localShutoff();
            if (stop != null) return stop.route(head);
        }
        return inspectSupply(level, head).route(level);
    }

    static final class SupplyInspection {
        private final BlockPos origin;
        private final Search network;
        private final Trace trace;

        private SupplyInspection(BlockPos origin, Search network, Trace trace) {
            this.origin = origin;
            this.network = network;
            this.trace = trace;
        }

        Search network() { return network; }

        Route route(Level level) { return supplyRoute(level, origin, network, trace); }
    }

    static SupplyInspection inspectSupply(Level level, BlockPos origin) {
        var trace = new Trace();
        return new SupplyInspection(origin, search(level, origin, trace), trace);
    }

    private static Route supplyRoute(Level level, BlockPos head, Search network, Trace trace) {
        if (network.overflow()) return new Route(List.of(head), Flow.TOO_LARGE, null);
        Endpoint fullest = null;
        int mostWater = -1;
        for (var endpoint : trace.endpoints) {
            int water = endpoint.supply().water(level);
            if (water >= BUCKET) return new Route(path(trace, endpoint.pos()), Flow.READY, null, null, water);
            if (water > mostWater) {
                mostWater = water;
                fullest = endpoint;
            }
        }
        if (fullest != null) return new Route(path(trace, fullest.pos()), Flow.EMPTY, null, null, mostWater);
        if (trace.valve != null) return route(trace, trace.valve, Flow.CLOSED_VALVE);
        ClosedPort port = trace.blockingPort();
        if (port != null) {
            var blocks = new ArrayList<>(path(trace, port.from()));
            if (!blocks.getLast().equals(port.pipe())) blocks.add(port.pipe());
            return new Route(List.copyOf(blocks), Flow.DISABLED_PORT, port.side());
        }
        if (trace.unloaded != null) {
            return new Route(path(trace, trace.unloaded.fitting()), trace.unloaded.flow(), null, trace.unloaded.side(), -1);
        }
        if (trace.incompleteReservoir != null) return route(trace, trace.incompleteReservoir, Flow.INCOMPLETE_RESERVOIR);
        return route(trace, trace.lastFitting == null ? head : trace.lastFitting, Flow.DISCONNECTED);
    }

    static ZoneStatus inspectZone(Level level, BlockPos valve) {
        var trace = new Trace();
        var network = search(level, valve, trace, true);
        if (network.overflow()) return new ZoneStatus(List.of(), List.of(), true, null, null, null, null, false);
        var port = trace.blockingPort();
        return new ZoneStatus(network.supplies(), List.copyOf(trace.sprinklers), false,
                trace.valve, port == null ? null : port.pipe(), port == null ? null : port.side(),
                trace.unloaded, trace.incompleteReservoir != null);
    }

    private static Route route(Trace trace, BlockPos end, Flow flow) {
        return new Route(path(trace, end), flow, null);
    }

    private static List<BlockPos> path(Trace trace, BlockPos end) {
        var blocks = new ArrayList<BlockPos>();
        for (BlockPos pos = end; pos != null; pos = trace.parents.get(pos)) blocks.add(pos);
        Collections.reverse(blocks);
        return List.copyOf(blocks);
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
                    if (trace != null && trace.unloaded == null && outgoing) {
                        // The stored connection tells us the last fitting before the unloaded chunk.
                        var connection = IrrigationSprinklerBlock.connection(side);
                        if (state.hasProperty(connection) && state.getValue(connection)) trace.unloaded = new UnloadedStop(pos, side);
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
                if (seen.size() > MAX_NODES) return new Search(true, List.of(), true, blockedValve);
                attached = true;
                if (trace != null) trace.parents.put(next, pos);
                if(neighbor.getBlock() instanceof IrrigationPipeBlock) queue.add(next);
                else if(neighbor.getBlock() instanceof IrrigationValveBlock) {
                    if(IrrigationValveBlock.permitsFlow(level,next,neighbor))queue.add(next);
                    else {blockedValve=true;if (trace != null && trace.valve == null) trace.valve = next;}
                } else if(neighbor.getBlock() instanceof IrrigationBasinBlock) {
                    if(sources.add(next))addSupply(supplies, trace, next, new Supply(next,null));
                } else if(neighbor.getBlock() instanceof WaterReservoirBlock) {
                    var core = WaterReservoirBlock.core(next, neighbor);
                    if (!sources.add(core)) continue;
                    var status = ReservoirStructure.status(level, core);
                    if (status == ReservoirStructure.Status.COMPLETE && level.getBlockEntity(core) instanceof WaterReservoirBlockEntity tank) {
                        addSupply(supplies, trace, next, new Supply(null, tank));
                    } else if (trace != null) {
                        if (status == ReservoirStructure.Status.UNLOADED && trace.unloaded == null) trace.unloaded = new UnloadedStop(next, null);
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
