package com.agrievolution.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Shared access checks for placed workstations and their direct interactions. */
final class WorkstationAccess {
    private WorkstationAccess() {}

    static boolean mayUse(Level level, BlockPos pos, Player player, Direction side, ItemStack stack) {
        return player != null && player.level() == level && !player.isSpectator() && player.mayBuild()
                && level.hasChunkAt(pos) && level.mayInteract(player, pos) && player.mayUseItemAt(pos, side, stack);
    }

    static boolean stillValid(BlockEntity workstation, Player player) {
        var level = workstation.getLevel();
        var pos = workstation.getBlockPos();
        return !workstation.isRemoved() && level != null && player.level() == level
                && level.hasChunkAt(pos) && level.getBlockEntity(pos) == workstation
                && player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64;
    }

    static boolean mayConfigure(BlockEntity workstation, Player player) {
        return !player.level().isClientSide && !player.isSpectator() && player.mayBuild()
                && stillValid(workstation, player)
                && player.level().mayInteract(player, workstation.getBlockPos());
    }
}
