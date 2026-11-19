package com.agrievolution.content;

import java.util.function.UnaryOperator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Reserve an export before a destination can call back into the source inventory. */
final class GreenhouseItemTransfer {
    private GreenhouseItemTransfer() {}

    static int insert(ServerLevel level, BlockEntity owner, Container source, int slot, int amount,
                      int firstReturnSlot, int endReturnSlot, UnaryOperator<ItemStack> destination) {
        if (amount <= 0 || !current(level, owner)) return 0;
        ItemStack reserved = source.removeItem(slot, amount);
        if (reserved.isEmpty()) return 0;
        int offered = reserved.getCount();
        ItemStack remainder = current(level, owner) ? destination.apply(reserved.copy()) : reserved;
        int moved = offered - remainder.getCount();
        if (!remainder.isEmpty()) restore(level, owner, source, slot, firstReturnSlot, endReturnSlot, remainder.copy());
        return moved;
    }

    private static boolean current(ServerLevel level, BlockEntity owner) {
        return owner.getLevel() == level && WorkstationAccess.isCurrent(owner);
    }

    private static void restore(ServerLevel level, BlockEntity owner, Container source, int preferred,
                                int first, int end, ItemStack stack) {
        // Prefer the vacated slot, then matching stacks, then empty slots in the same output buffer.
        if (current(level, owner)) put(source, preferred, stack);
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            for (int slot = first; slot < end && !stack.isEmpty() && current(level, owner); slot++) {
                if (slot == preferred || source.getItem(slot).isEmpty() != (pass == 1)) continue;
                put(source, slot, stack);
            }
        }
        if (!stack.isEmpty()) {
            var pos = owner.getBlockPos();
            Containers.dropItemStack(level, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, stack);
        }
    }

    private static void put(Container source, int slot, ItemStack remainder) {
        var stored = source.getItem(slot);
        if (!stored.isEmpty() && !ItemStack.isSameItemSameComponents(stored, remainder)) return;
        int limit = Math.min(source.getMaxStackSize(remainder), remainder.getMaxStackSize());
        int moved = Math.min(remainder.getCount(), Math.max(0, limit - stored.getCount()));
        if (moved == 0) return;
        source.setItem(slot, remainder.copyWithCount(stored.getCount() + moved));
        remainder.shrink(moved);
    }
}
