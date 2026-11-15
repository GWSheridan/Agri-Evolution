package com.agrievolution.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.FarmlandWaterManager;
import net.neoforged.neoforge.common.ticket.AABBTicket;

/** Finite reservoir; never loads chunks or copies water from a filled basin. */
public final class IrrigationSprinklerBlockEntity extends BlockEntity {
    public static final int CAPACITY=4000;
    private static final int WATERING_INTERVAL = 40;
    private int water;
    private boolean smartWatering;
    private int redstoneMode; // 0: powered pauses (legacy), 1: powered enables, 2: ignore
    public boolean smartWatering(){return smartWatering;}
    public int redstoneMode(){return redstoneMode;}
    public String controls(){return (smartWatering?"Dry-soil only":"Continuous")+" | "+switch(redstoneMode){case 1->"Requires redstone";case 2->"Ignores redstone";default->"Redstone pauses";};}
    public void configure(boolean redstone){if(redstone)redstoneMode=(redstoneMode+1)%3;else smartWatering=!smartWatering;setChanged();checkFlow();}
    private final java.util.List<AABBTicket> tickets = new java.util.ArrayList<>();
    private long growthUntil;
    public IrrigationSprinklerBlockEntity(BlockPos pos, BlockState state) {
        super(AEBlockEntities.IRRIGATION_SPRINKLER.get(), pos, state);
    }
    public int water() { return water; }
    boolean growthActive() {
        return level instanceof ServerLevel server && server.getGameTime() < growthUntil
                && !isRemoved() && flowAllowed()
                && getBlockState().getValue(IrrigationSprinklerBlock.ACTIVE);
    }
    public boolean fillBucket() {
        if (!(level instanceof ServerLevel) || water > CAPACITY-1000) return false;
        water += 1000;
        setChanged();
        return true;
    }

    private boolean flowAllowed() {
        return localBlockage() == null && networkBlockage(IrrigationNetwork.search(level, worldPosition)) == null;
    }

    private String localBlockage() {
        if (!(level instanceof ServerLevel)) return "Sprinkler unavailable";
        boolean powered = redstoneMode != 2 && IrrigationNetwork.hasLoadedNeighborSignal(level, worldPosition);
        if (redstoneMode == 0 && powered) return "Paused by redstone";
        if (redstoneMode == 1 && !powered) return "Waiting for redstone";
        for (Direction side : Direction.values()) {
            BlockPos pos = worldPosition.relative(side);
            if (!level.hasChunkAt(pos)) {
                if (getBlockState().getValue(IrrigationSprinklerBlock.connection(side))) return "Connected chunk not loaded";
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (IrrigationValveBlock.connects(state, side)
                    && !IrrigationValveBlock.permitsFlow(level, pos, state)) {
                return state.getValue(IrrigationValveBlock.OPEN) ? "Adjacent valve held shut by redstone" : "Adjacent valve closed";
            }
        }
        return null;
    }

    private static String networkBlockage(IrrigationNetwork.Search network) {
        if (network.overflow()) return "Pipe network exceeds 512 nodes";
        if (network.blockedValve() && network.supplies().isEmpty()) return "Valve shut along pipe network";
        return null;
    }

    /** Read-only status for an empty-hand inspection. */
    String inspectionStatus() {
        String blockage = localBlockage();
        if (blockage != null) return blockage;
        var network = IrrigationNetwork.search(level, worldPosition);
        blockage = networkBlockage(network);
        if (blockage != null) return blockage;
        var soils = wateredSoils();
        if (level.getGameTime() < growthUntil && getBlockState().getValue(IrrigationSprinklerBlock.ACTIVE)) {
            return soils.isEmpty() ? "Spraying, no farmland in reach" : "Watering, x5 natural crop growth";
        }
        if (smartWatering) {
            if (soils.isEmpty()) return "Dry-soil mode: no farmland in reach";
            if (!hasDrySoil(soils)) return "Dry-soil mode: soil already wet";
        }
        if (water >= 25) return soils.isEmpty() ? "Ready, no farmland in reach" : "Ready for next watering cycle";
        if (water > 0) return "Too little water to spray, add a bucket";
        if (network.supplies().isEmpty()) return "Empty, no reachable water source";
        for (var supply : network.supplies()) {
            if (supply.hasBucket(level)) return "Connected source ready to refill";
        }
        return "Connected sources short of a full bucket";
    }

    private boolean hasDrySoil(java.util.Set<BlockPos> soils) {
        for (var pos : soils) {
            var state = level.getBlockState(pos);
            if (state.getBlock() instanceof FarmBlock && state.getValue(FarmBlock.MOISTURE) < 7) return true;
        }
        return false;
    }
    /** Loaded network check; closes water and growth tickets after valve/redstone changes. */
    public void checkFlow() {
        if (level instanceof ServerLevel && !flowAllowed()) stopWatering();
    }

    void serverTick() {
        if (!(level instanceof ServerLevel server)) return;
        long now = server.getGameTime();
        if (now < growthUntil) {
            if (!flowAllowed()) stopWatering();
            else if (!getBlockState().getValue(IrrigationSprinklerBlock.ACTIVE)) {
                // Reopening the valve can use the rest of the paid interval.
                spray(server, wateredSoils(), growthUntil);
            }
            return;
        }
        if (growthUntil == 0 && now % WATERING_INTERVAL != 0) return;
        // Finish the current interval before charging for another. Idle heads retry on the world clock.
        growthUntil = 0;
        irrigateCycle();
    }

    void startAfterFill() {
        if (!getBlockState().getValue(IrrigationSprinklerBlock.ACTIVE) && level.getGameTime() >= growthUntil) {
            irrigateCycle();
        }
    }

    private void drawFromBasin() {
        if (water != 0 || !(level instanceof ServerLevel)) return;
        var network=IrrigationNetwork.search(level,worldPosition);
        if(network.overflow())return;
        for(var supply:network.supplies()) if(supply.takeBucket(level)) {
            water=1000;setChanged();return;
        }
    }
    /** One 25 mB watering interval, called every 40 ticks. Public for real-server tests. */
    public void irrigateCycle() {
        if (!(level instanceof ServerLevel server) || isRemoved()) return;
        if (!flowAllowed()) { stopWatering(); return; }
        growthUntil = 0;
        var soils=wateredSoils();
        if (smartWatering && !hasDrySoil(soils)) {
            stopWatering();return;
        }
        drawFromBasin();
        if (water < 25) { stopWatering(); return; }
        water -= 25;
        setChanged();
        spray(server, soils, server.getGameTime() + WATERING_INTERVAL);
    }

    private void spray(ServerLevel server, java.util.Set<BlockPos> soils, long until) {
        clearTickets();
        for (BlockPos soil : soils) {
            tickets.add(FarmlandWaterManager.addAABBTicket(server,new AABB(soil)));
            BlockState state = server.getBlockState(soil);
            if (state.getBlock() instanceof FarmBlock && state.getValue(FarmBlock.MOISTURE) != 7)
                server.setBlock(soil, state.setValue(FarmBlock.MOISTURE,7), loadedUpdateFlags(server, soil, Block.UPDATE_CLIENTS));
        }
        setActive(true);
        // A loaded but no-longer-ticking chunk cannot retain a free growth bonus.
        growthUntil = until;
        SprinklerGrowth.activate(server, worldPosition);
    }
    private void setActive(boolean active) {
        if (level instanceof ServerLevel && !isRemoved() && getBlockState().getBlock() instanceof IrrigationSprinklerBlock
                && getBlockState().getValue(IrrigationSprinklerBlock.ACTIVE) != active)
            level.setBlock(worldPosition, getBlockState().setValue(IrrigationSprinklerBlock.ACTIVE, active),
                    loadedUpdateFlags(level, worldPosition, Block.UPDATE_ALL));
    }

    private static int loadedUpdateFlags(Level level, BlockPos pos, int flags) {
        // Moisture and spray state leave the block shape alone. Avoid notifying unloaded neighbors.
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (!level.hasChunkAt(pos.relative(side))) return Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        }
        return flags;
    }
    private void stopWatering() {
        if (level instanceof ServerLevel server) SprinklerGrowth.deactivate(server, worldPosition);
        clearTickets();
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
    @Override public void onLoad() { super.onLoad(); if (level instanceof ServerLevel) setActive(false); }
    @Override public void onChunkUnloaded() {
        if (level instanceof ServerLevel server) SprinklerGrowth.deactivate(server, worldPosition);
        clearTickets();
        super.onChunkUnloaded();
    }
    @Override public void setRemoved() {
        if (level instanceof ServerLevel server) SprinklerGrowth.deactivate(server, worldPosition);
        clearTickets();
        super.setRemoved();
    }

    private void clearTickets() {
        for(var ticket:tickets) ticket.invalidate();
        tickets.clear();
    }

    /** First reachable farmland in each column; never water through roofs or load chunks. */
    java.util.Set<BlockPos> wateredSoils() {
        var result=new java.util.HashSet<BlockPos>();
        if(level!=null) for(var cell:SprinklerCoverage.scan(level,worldPosition)) if(cell.watered())result.add(cell.pos());
        return result;
    }
}
