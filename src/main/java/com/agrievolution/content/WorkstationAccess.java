package com.agrievolution.content;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Shared checks for menus that keep a reference to a placed workstation. */
final class WorkstationAccess {
    private WorkstationAccess() {}

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
