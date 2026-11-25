package com.agrievolution.fabric.mixin;

import com.agrievolution.content.AgriCropBlock;
import com.agrievolution.content.CropWeeds;
import com.agrievolution.content.SoilCareData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockBehaviour.BlockStateBase.class)
abstract class CropTickMixin {
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void agri$checkWeeds(ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo callback) {
        var state = (BlockState) (Object) this;
        if (level.getRawBrightness(pos, 0) >= 9 && CropWeeds.isHost(state)
                && CropWeeds.slowGrowth(level, pos, state)) callback.cancel();
    }

    @Inject(method = "randomTick", at = @At("RETURN"))
    private void agri$checkRotation(ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo callback) {
        var state = (BlockState) (Object) this;
        if (state.getBlock() instanceof CropBlock && !(state.getBlock() instanceof AgriCropBlock))
            SoilCareData.nativeGrowth(level, pos, state, level.getBlockState(pos));
    }
}
