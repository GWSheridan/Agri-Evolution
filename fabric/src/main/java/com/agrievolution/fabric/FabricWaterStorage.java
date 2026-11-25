package com.agrievolution.fabric;

import com.agrievolution.content.*;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.world.level.material.Fluids;

final class FabricWaterStorage extends SnapshotParticipant<Integer> implements SingleSlotStorage<FluidVariant> {
    private static final long UNITS_PER_MB = FluidConstants.BUCKET / 1000;
    private static final WeakHashMap<WaterTank, WeakReference<FabricWaterStorage>> STORAGES = new WeakHashMap<>();
    private static final FluidVariant WATER = FluidVariant.of(Fluids.WATER);

    private final WaterTank tank;
    private int change;

    private FabricWaterStorage(WaterTank tank) { this.tank = tank; }

    static void register() {
        FluidStorage.SIDED.registerForBlocks((level, pos, state, entity, face) -> {
            if (face == null || !WaterReservoirBlock.port(state, face)) return null;
            var core = WaterReservoirBlock.core(pos, state);
            return level.hasChunkAt(core) && level.getBlockEntity(core) instanceof WaterReservoirBlockEntity tank
                    ? storage(tank) : null;
        }, AEBlocks.WATER_RESERVOIR.get());
        FluidStorage.SIDED.registerForBlocks((level, pos, state, entity, face) ->
                VerticalGreenhouseBlock.port(state, face) && entity instanceof VerticalGreenhouseBlockEntity tank
                        ? storage(tank) : null, AEBlocks.VERTICAL_GREENHOUSE.get());
    }

    private static synchronized FabricWaterStorage storage(WaterTank tank) {
        var reference = STORAGES.get(tank);
        var current = reference == null ? null : reference.get();
        if (current != null) return current;
        var next = new FabricWaterStorage(tank);
        STORAGES.put(tank, new WeakReference<>(next));
        return next;
    }

    @Override public boolean isResourceBlank() { return getAmount() == 0; }
    @Override public FluidVariant getResource() { return isResourceBlank() ? FluidVariant.blank() : WATER; }
    @Override public long getAmount() { return tank.complete() ? (tank.water() + change) * UNITS_PER_MB : 0; }
    @Override public long getCapacity() { return tank.capacity() * UNITS_PER_MB; }
    @Override public boolean supportsExtraction() { return tank instanceof WaterReservoirBlockEntity; }

    @Override
    public long insert(FluidVariant fluid, long maximum, TransactionContext transaction) {
        if (!fluid.equals(WATER) || maximum < UNITS_PER_MB || !tank.complete()) return 0;
        int room = tank.fillWater(tank.capacity(), false);
        int amount = (int) Math.min(maximum / UNITS_PER_MB, room - change);
        if (amount <= 0) return 0;
        updateSnapshots(transaction);
        change += amount;
        return amount * UNITS_PER_MB;
    }

    @Override
    public long extract(FluidVariant fluid, long maximum, TransactionContext transaction) {
        if (!supportsExtraction() || !fluid.equals(WATER) || maximum < UNITS_PER_MB || !tank.complete()) return 0;
        int amount = (int) Math.min(maximum / UNITS_PER_MB, tank.drainWater(tank.capacity(), false) + change);
        if (amount <= 0) return 0;
        updateSnapshots(transaction);
        change -= amount;
        return amount * UNITS_PER_MB;
    }

    @Override protected Integer createSnapshot() { return change; }
    @Override protected void readSnapshot(Integer snapshot) { change = snapshot; }
    @Override protected void onFinalCommit() {
        int amount = change;
        change = 0;
        if (amount > 0) tank.fillWater(amount, true);
        else if (amount < 0) tank.drainWater(-amount, true);
    }
}
