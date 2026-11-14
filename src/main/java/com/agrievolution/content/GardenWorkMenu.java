package com.agrievolution.content;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class GardenWorkMenu extends AbstractContainerMenu {
    private final Inventory inventory;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final boolean serverPreserving;
    private int page;
    private static final int PAGE_BUTTON = 32;

    public GardenWorkMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL, false, new SimpleContainerData(6));
    }

    GardenWorkMenu(int id, Inventory inventory, ContainerLevelAccess access, boolean preserving) {
        this(id, inventory, access, preserving, null);
    }

    private GardenWorkMenu(int id, Inventory inventory, ContainerLevelAccess access, boolean preserving, ContainerData data) {
        super(AEMenus.GARDEN_WORK.get(), id);
        this.inventory = inventory;
        this.access = access;
        this.serverPreserving = preserving;
        this.data = data != null ? data : new ContainerData() {
            @Override public int get(int index) {
                return switch (index) {
                    case 0 -> preserving ? 1 : 0;
                    case 4 -> page;
                    case 5 -> GardenWork.commissions(inventory.player);
                    default -> GardenWork.completed(inventory.player, index - 1);
                };
            }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return 6; }
        };
        addDataSlots(this.data);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, 9 + row * 9 + col, 76 + col * 18, 171 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 76 + col * 18, 229));
    }

    public boolean preserving() { return data.get(0) != 0; }
    public int completed(int row) { return data.get(row + 1); }
    public int page() { return data.get(4); }
    public boolean commissions() { return page() == GardenWork.ROWS; }
    public int tier(int row) { return commissions() ? row : page(); }
    public boolean unlocked(int row) {
        int tier = tier(row);
        return preserving() || tier == 0 || completed(tier - 1) >= GardenWork.UNLOCK_AT;
    }
    public boolean claimed(int row) { return commissions() && (data.get(5) & (1 << row)) != 0; }
    public GardenWork.Job job(int row) { return GardenWork.job(preserving(), page(), row); }
    public boolean ready(int row) { return unlocked(row) && !claimed(row) && job(row).ready(inventory); }
    public int action(int row) { return page() * GardenWork.ROWS + row; }
    public int pageAction(int selected) { return PAGE_BUTTON + selected; }

    @Override public boolean stillValid(Player player) {
        return stillValid(access, player, serverPreserving ? AEBlocks.PRESERVING_STATION.get() : AEBlocks.GARDEN_ORDER_BOARD.get());
    }

    @Override public boolean clickMenuButton(Player player, int button) {
        if (player.level().isClientSide || player.isSpectator() || player != inventory.player || button < 0 || !stillValid(player)) return false;
        if (!serverPreserving && button >= PAGE_BUTTON && button <= PAGE_BUTTON + GardenWork.ROWS) {
            page = button - PAGE_BUTTON;
            broadcastChanges();
            return true;
        }
        if (button >= GardenWork.ROWS * (GardenWork.ROWS + 1)) return false;
        int row = button % GardenWork.ROWS;
        if (button != action(row) || !ready(row) || !job(row).complete(player)) return false;
        if (serverPreserving) FarmingMilestones.PRESERVE.award(player);
        else if (commissions()) GardenWork.recordCommission(player, row);
        else GardenWork.recordOrder(player, page());
        broadcastChanges();
        return true;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = getSlot(index);
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack original = stack.copy();
        if (!(index < 27 ? moveItemStackTo(stack, 27, 36, false) : moveItemStackTo(stack, 0, 27, false))) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }
}
