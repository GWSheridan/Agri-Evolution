package com.agrievolution.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class IrrigationValveBlockEntity extends BlockEntity {
    private String zone = "";
    private BlockPos controller;
    private boolean scheduledOpen;

    public IrrigationValveBlockEntity(BlockPos pos, BlockState state) {
        super(AEBlockEntities.IRRIGATION_VALVE.get(), pos, state);
    }

    @Override public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server && getBlockState().getBlock() instanceof IrrigationValveBlock valve) {
            server.scheduleTick(worldPosition, valve, 1);
        }
    }

    String zone() { return zone.isEmpty() ? "Irrigation zone" : zone; }
    String zoneName() { return zone; }

    BlockPos controller() { return controller; }
    boolean controlledBy(BlockPos pos) { return pos.equals(controller); }

    boolean scheduleAllowsFlow(boolean refill) {
        if (controller == null) return true;
        if (level == null || !level.hasChunkAt(controller)) return false;
        return !(level.getBlockEntity(controller) instanceof IrrigationControllerBlockEntity owner)
                || !owner.contains(worldPosition)
                || (scheduledOpen && owner.zoneActive(worldPosition) && (!refill || owner.permitsRefill(worldPosition)));
    }

    String reserveStatus() {
        return controller != null && level != null && level.hasChunkAt(controller)
                && level.getBlockEntity(controller) instanceof IrrigationControllerBlockEntity owner
                ? owner.reserveStatus(worldPosition) : "Refill held in reserve";
    }

    void setController(BlockPos pos) {
        controller = pos;
        scheduledOpen = false;
        if (level instanceof ServerLevel && level.hasChunkAt(worldPosition)) {
            level.blockEntityChanged(worldPosition);
            IrrigationValveBlock.notifySprinklers(level, worldPosition);
        }
    }

    void setScheduledOpen(boolean value) {
        if (scheduledOpen == value) return;
        scheduledOpen = value;
        IrrigationValveBlock.notifySprinklers(level, worldPosition);
    }

    void clearMissingController() {
        if (controller != null && level.hasChunkAt(controller) && (
                !(level.getBlockEntity(controller) instanceof IrrigationControllerBlockEntity owner)
                        || !owner.contains(worldPosition))) setController(null);
    }

    static String storedZone(ItemStack stack) {
        return shorten(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("IrrigationZone"));
    }

    void rename(String name) {
        String updated = shorten(name);
        if (zone.equals(updated)) return;
        zone = updated;
        // A name change cannot affect redstone, so it only needs to mark the chunk for saving.
        if (level instanceof ServerLevel && level.hasChunkAt(worldPosition)) level.blockEntityChanged(worldPosition);
    }

    private static String shorten(String name) {
        String clean = name.strip();
        return clean.substring(0, clean.offsetByCodePoints(0, Math.min(32, clean.codePointCount(0, clean.length()))));
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!zone.isEmpty()) tag.putString("Zone", zone);
        if (controller != null) tag.putLong("Controller", controller.asLong());
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        zone = shorten(tag.getString("Zone"));
        controller = tag.contains("Controller", net.minecraft.nbt.Tag.TAG_LONG) ? BlockPos.of(tag.getLong("Controller")) : null;
        scheduledOpen = false;
    }
}
