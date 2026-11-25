package com.agrievolution.fabric.mixin;

import com.agrievolution.content.SoilCareData;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
abstract class BlockDropsMixin {
    @Inject(method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)Ljava/util/List;",
            at = @At("RETURN"), cancellable = true)
    private static void agri$naturalDrops(BlockState state, ServerLevel level, BlockPos pos, BlockEntity blockEntity,
                                          CallbackInfoReturnable<List<ItemStack>> result) {
        agri$process(state, level, pos, null, result);
    }

    @Inject(method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;",
            at = @At("RETURN"), cancellable = true)
    private static void agri$playerDrops(BlockState state, ServerLevel level, BlockPos pos, BlockEntity blockEntity,
                                         Entity breaker, ItemStack tool, CallbackInfoReturnable<List<ItemStack>> result) {
        agri$process(state, level, pos, breaker, result);
    }

    private static void agri$process(BlockState state, ServerLevel level, BlockPos pos, Entity breaker,
                                     CallbackInfoReturnable<List<ItemStack>> result) {
        if (state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS)) {
            var drops = result.getReturnValue();
            if (drops.stream().anyMatch(item -> item.is(Items.WHEAT_SEEDS)))
                result.setReturnValue(drops.stream().filter(item -> !item.is(Items.WHEAT_SEEDS)).toList());
        } else {
            SoilCareData.nativeDrops(level, pos, state, breaker,
                    () -> level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS) && !result.getReturnValue().isEmpty());
        }
    }
}
