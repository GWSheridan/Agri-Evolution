package com.agrievolution.fabric.mixin;

import com.agrievolution.content.CropPollination;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Bee;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
abstract class ServerLevelMixin {
    @Inject(method = "tickNonPassenger", at = @At("TAIL"))
    private void agri$pollination(Entity entity, CallbackInfo callback) {
        if (entity instanceof Bee) CropPollination.beeVisit(entity);
    }
}
