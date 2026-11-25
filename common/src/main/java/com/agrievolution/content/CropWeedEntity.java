package com.agrievolution.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Saved and tracked by Minecraft; never occupies or replaces the crop block. */
public final class CropWeedEntity extends Entity {
    private String host = "";
    private int lastAge = -1;
    public CropWeedEntity(EntityType<? extends CropWeedEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }
    public void attach(BlockPos pos, BlockState state) {
        setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        host = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        lastAge = CropWeeds.age(state);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        host = tag.getString("CropHost");
        lastAge = tag.contains("CropAge") ? tag.getInt("CropAge") : -1;
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("CropHost", host);
        tag.putInt("CropAge", lastAge);
    }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide || !level().hasChunkAt(blockPosition())) return;
        BlockState state = level().getBlockState(blockPosition());
        int age = CropWeeds.age(state);
        if (!BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals(host)
                || !CropWeeds.isHost(state) || (age >= 0 && lastAge >= 0 && age < lastAge)) {
            discard();
        } else lastAge = age;
    }
    // Entity defaults are non-pickable and non-collidable: crop interaction stays intact.
}
