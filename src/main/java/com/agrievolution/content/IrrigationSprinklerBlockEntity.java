package com.agrievolution.content;

import com.agrievolution.content.IrrigationNetwork.Flow;
import com.agrievolution.content.IrrigationNetwork.Route;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.FarmlandWaterManager;
import net.neoforged.neoforge.common.ticket.SimpleTicket;

/** Finite reservoir; never loads chunks or copies water from a filled basin. */
public final class IrrigationSprinklerBlockEntity extends BlockEntity {
    public static final int CAPACITY=4000;
    private static final int WATER_PER_SPRAY = 25;
    private static final int WATERING_INTERVAL = 40;
    private int water;
    private boolean smartWatering;
    private int redstoneMode; // 0: powered pauses (legacy), 1: powered enables, 2: ignore
    public boolean smartWatering(){return smartWatering;}
    public int redstoneMode(){return redstoneMode;}
    public String controls(){return (smartWatering?"Dry-soil only":"Continuous")+" | "+switch(redstoneMode){case 1->"Requires redstone";case 2->"Ignores redstone";default->"Redstone pauses";};}
    public void configure(boolean redstone){if(redstone)redstoneMode=(redstoneMode+1)%3;else smartWatering=!smartWatering;setChanged();checkFlow();}
    private SimpleTicket<Vec3> waterTicket;
    private Set<BlockPos> paidSoils = Set.of();
    private long growthUntil;
    public IrrigationSprinklerBlockEntity(BlockPos pos, BlockState state) {
        super(AEBlockEntities.IRRIGATION_SPRINKLER.get(), pos, state);
    }
    public int water() { return water; }
    int comparatorSignal() { return water == 0 ? 0 : 1 + 14 * water / CAPACITY; }

    @Override public void setChanged() {
        if (level == null || !level.hasChunkAt(worldPosition)) return;
        level.blockEntityChanged(worldPosition);
    }

    private void waterChanged() {
        setChanged();
        IrrigationNetwork.updateComparators(level, worldPosition, getBlockState().getBlock());
    }

    boolean growthActive() {
        return hasCurrentSpray() && flowAllowed();
    }

    boolean hasCurrentSpray() {
        return level instanceof ServerLevel server && server.getGameTime() < growthUntil
                && !isRemoved() && getBlockState().getValue(IrrigationSprinklerBlock.ACTIVE);
    }

    boolean watersPlot(BlockPos soil) {
        return paidSoils.contains(soil) && SprinklerCoverage.reaches(level, worldPosition, soil);
    }

    Set<BlockPos> growthSoils() {
        var soils = new HashSet<BlockPos>();
        for (BlockPos soil : paidSoils) {
            if (SprinklerCoverage.reaches(level, worldPosition, soil)) soils.add(soil);
        }
        return soils;
    }
    public boolean fillBucket() {
        if (!(level instanceof ServerLevel) || water > CAPACITY-1000) return false;
        water += 1000;
        waterChanged();
        return true;
    }

    boolean takeBucket() {
        if (!(level instanceof ServerLevel) || water < 1000) return false;
        water -= 1000;
        waterChanged();
        return true;
    }

    private boolean flowAllowed() {
        return level instanceof ServerLevel && localShutoff() == null
                && networkBlockage(IrrigationNetwork.search(level, worldPosition)) == null;
    }

    record LocalShutoff(Flow flow, BlockPos pos, String message) {
        Route route(BlockPos head) {
            return new Route(head.equals(pos) ? List.of(head) : List.of(head, pos), flow, null);
        }
    }

    LocalShutoff localShutoff() {
        boolean powered = redstoneMode != 2 && IrrigationNetwork.hasLoadedNeighborSignal(level, worldPosition);
        if (redstoneMode == 0 && powered) return new LocalShutoff(Flow.PAUSED_BY_REDSTONE, worldPosition, "Paused by redstone");
        if (redstoneMode == 1 && !powered) return new LocalShutoff(Flow.WAITING_FOR_REDSTONE, worldPosition, "Waiting for redstone");
        for (Direction side : Direction.values()) {
            BlockPos pos = worldPosition.relative(side);
            if (!level.hasChunkAt(pos)) {
                if (getBlockState().getValue(IrrigationSprinklerBlock.connection(side))) {
                    return new LocalShutoff(Flow.UNLOADED_CONNECTION, worldPosition, "Connected chunk not loaded");
                }
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (IrrigationValveBlock.connects(state, side)
                    && !IrrigationValveBlock.permitsFlow(level, pos, state)) {
                String message = state.getValue(IrrigationValveBlock.OPEN) ? "Adjacent valve held shut by redstone" : "Adjacent valve closed";
                return new LocalShutoff(Flow.CLOSED_VALVE, pos, message);
            }
        }
        return null;
    }

    private static String networkBlockage(IrrigationNetwork.Search network) {
        if (network.overflow()) return "Pipe network exceeds " + IrrigationNetwork.MAX_NODES + " nodes";
        if (network.blockedValve() && network.supplies().isEmpty()) return "Valve shut along pipe network";
        return null;
    }

    /** Read-only status for empty-hand inspection and the Spanner preview. */
    String inspectionStatus() {
        return inspectionStatus(null, null);
    }

    String inspectionStatus(IrrigationNetwork.SupplyInspection inspection, Set<BlockPos> inspectedSoils) {
        if (!(level instanceof ServerLevel)) return "Sprinkler unavailable";
        var stop = localShutoff();
        if (stop != null) return stop.message();
        if (inspection == null) inspection = IrrigationNetwork.inspectSupply(level, worldPosition);
        var network = inspection.network();
        String blockage = networkBlockage(network);
        if (blockage != null) return blockage;
        var soils = inspectedSoils == null ? wateredSoils() : inspectedSoils;
        if (hasCurrentSpray()) {
            if (soils.isEmpty()) return "Spraying, no farmland in reach";
            long watered = soils.stream().filter(paidSoils::contains).count();
            if (watered == 0) return "Reachable farmland waits for the next spray";
            if (watered < soils.size()) return "Watering " + watered + "/" + soils.size()
                    + " reachable plots; new plots wait for the next spray";
            return "Watering, x5 natural crop growth";
        }
        if (smartWatering) {
            if (soils.isEmpty()) return "Dry-soil mode: no farmland in reach";
            if (!hasDrySoil(soils)) return "Dry-soil mode: soil already wet";
        }
        if (water >= WATER_PER_SPRAY) return soils.isEmpty() ? "Ready, no farmland in reach" : "Ready for next watering cycle";
        if (network.supplies().isEmpty()) {
            var route = inspection.route(level);
            return switch (route.flow()) {
                case DISABLED_PORT -> "Refill blocked by disabled " + route.disabledSide().getName() + " pipe port";
                case UNLOADED_CONNECTION -> "Refill path leads into an unloaded chunk";
                case INCOMPLETE_RESERVOIR -> ReservoirStructure.Status.INCOMPLETE.message;
                default -> water == 0 ? "Empty, no reachable water source" : "Too little water to spray, add a bucket";
            };
        }
        var amounts = IrrigationNetwork.supplyWater(level, network.supplies());
        if (amounts.ready() > 0) return "Connected source ready to refill";
        return "Connected sources short of a full bucket"
                + (amounts.fullest() > 0 ? " (fullest: " + amounts.fullest() + " mB)" : "");
    }

    private boolean hasDrySoil(Set<BlockPos> soils) {
        for (var pos : soils) {
            var state = level.getBlockState(pos);
            if (state.getBlock() instanceof FarmBlock && state.getValue(FarmBlock.MOISTURE) < 7) return true;
        }
        return false;
    }
    /** Loaded network check; closes water and growth tickets after valve/redstone changes. */
    public void checkFlow() {
        if (hasCurrentSpray() && !flowAllowed()) stopWatering();
    }

    void serverTick() {
        if (!(level instanceof ServerLevel server)) return;
        long now = server.getGameTime();
        if (now % WATERING_INTERVAL == 0) refreshFittings(server);
        if (now < growthUntil) {
            if (!flowAllowed()) stopWatering();
            else if (!getBlockState().getValue(IrrigationSprinklerBlock.ACTIVE)) {
                spray(server, growthSoils(), growthUntil);
            }
            return;
        }
        if (growthUntil == 0 && now % WATERING_INTERVAL != 0) return;
        // Finish the current interval before charging for another. Idle heads retry on the world clock.
        growthUntil = 0;
        paidSoils = Set.of();
        irrigateCycle();
    }

    void startAfterFill() {
        if (!getBlockState().getValue(IrrigationSprinklerBlock.ACTIVE) && level.getGameTime() >= growthUntil) {
            irrigateCycle();
        }
    }

    private void refillForSpray(IrrigationNetwork.Search network) {
        if (water >= WATER_PER_SPRAY) return;
        for(var supply:network.supplies()) if(supply.takeBucket(level)) {
            water += 1000;
            return;
        }
    }
    /** One 25 mB watering interval, called every 40 ticks. Public for real-server tests. */
    public void irrigateCycle() {
        if (!(level instanceof ServerLevel server) || isRemoved()) return;
        if (localShutoff() != null) { stopWatering(); return; }
        var network = IrrigationNetwork.search(server, worldPosition);
        if (networkBlockage(network) != null) { stopWatering(); return; }
        growthUntil = 0;
        var soils=wateredSoils();
        if (smartWatering && !hasDrySoil(soils)) {
            stopWatering();return;
        }
        refillForSpray(network);
        if (water < WATER_PER_SPRAY) { stopWatering(); return; }
        water -= WATER_PER_SPRAY;
        waterChanged();
        paidSoils = Set.copyOf(soils);
        spray(server, soils, server.getGameTime() + WATERING_INTERVAL);
    }

    private void spray(ServerLevel server, Set<BlockPos> soils, long until) {
        clearWaterTicket();
        for (BlockPos soil : soils) {
            BlockState state = server.getBlockState(soil);
            if (state.getBlock() instanceof FarmBlock && state.getValue(FarmBlock.MOISTURE) != 7)
                server.setBlock(soil, state.setValue(FarmBlock.MOISTURE,7),
                        IrrigationNetwork.loadedUpdateFlags(server, soil, Block.UPDATE_CLIENTS));
        }
        setActive(true);
        // A loaded but no-longer-ticking chunk cannot retain a free growth bonus.
        growthUntil = until;
        if (!paidSoils.isEmpty()) {
            var headChunk = new ChunkPos(worldPosition);
            var plotChunks = paidSoils.stream().map(ChunkPos::new).filter(chunk -> !chunk.equals(headChunk))
                    .distinct().toArray(ChunkPos[]::new);
            // NeoForge checks this when farmland ticks. Recheck the path without scanning every plot each tick.
            waterTicket = FarmlandWaterManager.addCustomTicket(server, new SimpleTicket<Vec3>() {
                @Override public boolean matches(Vec3 point) {
                    return watersPlot(BlockPos.containing(point)) && growthActive();
                }
            }, headChunk, plotChunks);
        }
        SprinklerGrowth.activate(server, worldPosition);
    }
    private void setActive(boolean active) {
        if (level instanceof ServerLevel && !isRemoved() && getBlockState().getBlock() instanceof IrrigationSprinklerBlock
                && getBlockState().getValue(IrrigationSprinklerBlock.ACTIVE) != active)
            level.setBlock(worldPosition, getBlockState().setValue(IrrigationSprinklerBlock.ACTIVE, active),
                    IrrigationNetwork.loadedUpdateFlags(level, worldPosition, Block.UPDATE_ALL));
    }
    private void stopWatering() {
        if (level instanceof ServerLevel server) SprinklerGrowth.deactivate(server, worldPosition);
        clearWaterTicket();
        setActive(false);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag,registries);
        tag.putInt("SprinklerWater", water);
        tag.putBoolean("SmartWatering",smartWatering);tag.putInt("RedstoneMode",redstoneMode);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag,registries);
        water = Math.max(0, Math.min(CAPACITY,tag.getInt("SprinklerWater")));
        smartWatering=tag.getBoolean("SmartWatering");redstoneMode=Math.clamp(tag.getInt("RedstoneMode"),0,2);
    }
    @Override public void onLoad() {
        super.onLoad();
        // A fresh placement can already have started a spray before its first load callback.
        if (level instanceof ServerLevel server) {
            if (growthUntil == 0) setActive(false);
            refreshFittings(server);
        }
    }

    private void refreshFittings(ServerLevel server) {
        BlockState state = getBlockState();
        BlockState updated = IrrigationNetwork.loadedConnections(state, server, worldPosition);
        if (state != updated) server.setBlock(worldPosition, updated, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        IrrigationNetwork.syncLoadedFittings(server, worldPosition, updated, state.getBlock());
    }
    @Override public void onChunkUnloaded() {
        resetSpray();
        super.onChunkUnloaded();
    }
    @Override public void setRemoved() {
        resetSpray();
        super.setRemoved();
    }

    private void resetSpray() {
        if (level instanceof ServerLevel server) SprinklerGrowth.deactivate(server, worldPosition);
        clearWaterTicket();
        growthUntil = 0;
        paidSoils = Set.of();
    }

    private void clearWaterTicket() {
        if (waterTicket != null) {
            waterTicket.invalidate();
            waterTicket = null;
        }
    }

    /** First reachable farmland in each column; never water through roofs or load chunks. */
    Set<BlockPos> wateredSoils() {
        var result=new HashSet<BlockPos>();
        if(level!=null) for(var cell:SprinklerCoverage.scan(level,worldPosition)) if(cell.watered())result.add(cell.pos());
        return result;
    }
}
