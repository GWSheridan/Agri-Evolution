package com.agrievolution.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class IrrigationValveBlockEntity extends BlockEntity {
    private String zone = "";

    public IrrigationValveBlockEntity(BlockPos pos, BlockState state) {
        super(AEBlockEntities.IRRIGATION_VALVE.get(), pos, state);
    }

    String zone() { return zone.isEmpty() ? "Irrigation zone" : zone; }

    void rename(String name) {
        zone = shorten(name);
        setChanged();
    }

    private static String shorten(String name) {
        String clean = name.strip();
        return clean.substring(0, clean.offsetByCodePoints(0, Math.min(32, clean.codePointCount(0, clean.length()))));
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!zone.isEmpty()) tag.putString("Zone", zone);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        zone = shorten(tag.getString("Zone"));
    }
}
