package com.agrievolution.fabric.mixin;

import com.agrievolution.fabric.FabricWaterTickets;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.FarmBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FarmBlock.class)
abstract class FarmBlockMixin {
    @Inject(method = "isNearWater", at = @At("HEAD"), cancellable = true)
    private static void agri$irrigationWater(LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> result) {
        if (level instanceof ServerLevel server && FabricWaterTickets.contains(server, pos)) result.setReturnValue(true);
    }
}
