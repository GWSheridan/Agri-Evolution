package com.agrievolution.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class IrrigationControllerBlockEntity extends BlockEntity {
    public static final int ZONES = 6;
    public static final int HOUR = 1000;
    private static final int RANGE = 32;
    private static final int[] DURATIONS = {200, 400, 800, 1600};
    private static final int[] RESERVES = {0, 1000, 2000, 4000, 8000, 16000};
    private final BlockPos[] valves = new BlockPos[ZONES];
    private final int[] hours = new int[ZONES];
    private final int[] durations = new int[ZONES];
    private int reserve;
    private boolean enabled = true;
    private int manual = -1;
    private long manualUntil;
    private int active = -1;

    public IrrigationControllerBlockEntity(BlockPos pos, BlockState state) {
        super(AEBlockEntities.IRRIGATION_CONTROLLER.get(), pos, state);
    }

    private IrrigationValveBlockEntity valve(int slot) {
        BlockPos pos = valves[slot];
        if (pos != null && level instanceof ServerLevel server && server.hasChunkAt(pos)
                && server.getBlockEntity(pos) instanceof IrrigationValveBlockEntity valve
                && valve.controlledBy(worldPosition)) return valve;
        return null;
    }

    boolean contains(BlockPos pos) {
        for (BlockPos valve : valves) if (pos.equals(valve)) return true;
        return false;
    }

    String link(IrrigationValveBlockEntity valve) {
        if (valve.zoneName().isEmpty()) return "Name the valve before linking it.";
        if (!worldPosition.closerThan(valve.getBlockPos(), RANGE + 1)) return "The valve must be within 32 blocks.";
        if (contains(valve.getBlockPos())) return "This valve is already linked.";
        if (valve.controller() != null && !valve.controlledBy(worldPosition)) {
            return "The valve already belongs to another controller.";
        }
        for (int slot = 0; slot < ZONES; slot++) {
            if (valves[slot] != null) continue;
            valves[slot] = valve.getBlockPos();
            hours[slot] = slot * 2;
            durations[slot] = 1;
            valve.setController(worldPosition);
            refresh();
            return "Linked irrigation zone " + valve.zoneName() + ".";
        }
        return "This controller already has six zones.";
    }

    String label(int slot) {
        var valve = valve(slot);
        return valve == null ? (valves[slot] == null ? "Unlinked" : "Valve unavailable") : valve.zone();
    }

    boolean linked(int slot) { return valves[slot] != null; }
    int startHour(int slot) { return hours[slot]; }
    int duration(int slot) { return DURATIONS[durations[slot]]; }
    int reserveWater() { return RESERVES[reserve]; }
    int active() { return active; }
    boolean enabled() { return enabled; }
    int manual() { return manual; }

    void action(int slot, int action) {
        if (action == 4) {
            reserve = (reserve + 1) % RESERVES.length;
        } else if (action == 5) {
            enabled = !enabled;
            manual = -1;
        } else if (linked(slot)) {
            switch (action) {
                case 0 -> hours[slot] = (hours[slot] + 1) % 24;
                case 1 -> durations[slot] = (durations[slot] + 1) % DURATIONS.length;
                case 2 -> {
                    manual = manual == slot ? -1 : slot;
                    manualUntil = level.getGameTime() + duration(slot);
                }
                case 3 -> unlink(slot);
                case 6 -> hours[slot] = Math.floorMod(hours[slot] - 1, 24);
                default -> { return; }
            }
        } else return;
        refresh();
    }

    private void unlink(int slot) {
        var valve = valve(slot);
        valves[slot] = null;
        if (manual == slot) manual = -1;
        if (valve != null) valve.setController(null);
    }

    void release() {
        for (int slot = 0; slot < ZONES; slot++) unlink(slot);
    }

    private void refresh() {
        setChanged();
        checkSchedules();
    }

    void serverTick() {
        if (level instanceof ServerLevel && level.getGameTime() % 20 == 0) checkSchedules();
    }

    private boolean canRun(int slot, boolean override) {
        var valve = valve(slot);
        if (valve == null || !IrrigationValveBlock.hardwareOpen(level, valve.getBlockPos(), valve.getBlockState())) {
            return false;
        }
        var status = IrrigationNetwork.inspectZone(level, valve.getBlockPos());
        if (status.limit()) return false;
        var amounts = IrrigationNetwork.supplyWater(level, status.supplies());
        return amounts.ready() > 0 && (override || amounts.total() >= 1000 + reserveWater());
    }

    void checkSchedules() {
        if (!(level instanceof ServerLevel server) || isRemoved()) return;
        if (manual >= 0 && server.getGameTime() >= manualUntil) manual = -1;
        int next = -1;
        if (manual >= 0) {
            if (canRun(manual, true)) next = manual;
        } else if (enabled) {
            int time = (int) Math.floorMod(server.getDayTime(), 24000L);
            for (int slot = 0; slot < ZONES && next < 0; slot++) {
                if (linked(slot) && Math.floorMod(time - hours[slot] * HOUR, 24000) < duration(slot)
                        && canRun(slot, false)) next = slot;
            }
        }
        active = next;
        // Close the other linked valves before opening the selected zone.
        for (int slot = 0; slot < ZONES; slot++) {
            if (slot == active) continue;
            var valve = valve(slot);
            if (valve != null) valve.setScheduledOpen(false);
        }
        if (active >= 0) {
            var valve = valve(active);
            if (valve != null) valve.setScheduledOpen(true);
        }
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        var zones = new ListTag();
        for (int i = 0; i < ZONES; i++) {
            if (!linked(i)) continue;
            var zone = new CompoundTag();
            zone.putInt("Slot", i);
            zone.putLong("Valve", valves[i].asLong());
            zone.putInt("Hour", hours[i]);
            zone.putInt("Duration", durations[i]);
            zones.add(zone);
        }
        tag.put("Zones", zones);
        tag.putInt("Reserve", reserve);
        tag.putBoolean("Enabled", enabled);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        java.util.Arrays.fill(valves, null);
        for (Tag entry : tag.getList("Zones", Tag.TAG_COMPOUND)) {
            var zone = (CompoundTag) entry;
            int slot = zone.getInt("Slot");
            if (slot < 0 || slot >= ZONES || !zone.contains("Valve", Tag.TAG_LONG)) continue;
            BlockPos valve = BlockPos.of(zone.getLong("Valve"));
            if (!worldPosition.closerThan(valve, RANGE + 1) || contains(valve)) continue;
            valves[slot] = valve;
            hours[slot] = Math.floorMod(zone.getInt("Hour"), 24);
            durations[slot] = Math.clamp(zone.getInt("Duration"), 0, DURATIONS.length - 1);
        }
        reserve = Math.clamp(tag.getInt("Reserve"), 0, RESERVES.length - 1);
        enabled = !tag.contains("Enabled") || tag.getBoolean("Enabled");
        active = manual = -1;
    }
}
