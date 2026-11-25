package com.agrievolution.content;

import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** A planning screen has no item slots and cannot transfer inventory contents. */
public final class FarmLedgerMenu extends AbstractContainerMenu {
    public FarmLedgerMenu(int id, Inventory inventory) { super(AEMenus.FARM_LEDGER.get(), id); }
    @Override public boolean stillValid(Player player) { return FarmLedgerService.holding(player); }
    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
    @Override public void removed(Player player) { super.removed(player); if(player instanceof net.minecraft.server.level.ServerPlayer server) FarmLedgerService.close(server); }
}
