package com.agrievolution.fabric.mixin;

import com.agrievolution.platform.ChunkAwareBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelChunk.class)
abstract class LevelChunkMixin {
    @Inject(method = "addAndRegisterBlockEntity", at = @At("RETURN"))
    private void agri$loadPlacedBlockEntity(BlockEntity entity, CallbackInfo callback) {
        var chunk = (LevelChunk) (Object) this;
        if (!(entity instanceof ChunkAwareBlockEntity aware) || !(chunk.getLevel() instanceof ServerLevel level)
                || !level.getServer().isSameThread()) return;
        var pos = chunk.getPos();
        if (level.getChunkSource().getChunkNow(pos.x, pos.z) == chunk
                && chunk.getBlockEntities().get(entity.getBlockPos()) == entity) aware.onLoad();
    }
}
