package com.agrievolution.content;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** Server-owned inventory saved after every mutation; the open bag cannot be moved. */
public final class SeedBagMenu extends AbstractContainerMenu {
    private final Inventory inventory;
    private final int bagSlot;
    private final ItemStack bag;
    private final PortableStorageAccess access;
    private final SimpleContainer seeds = new SimpleContainer(SeedBagItem.SLOTS);
    public SeedBagMenu(int id, Inventory inventory, int bagSlot) {
        super(AEMenus.SEED_BAG.get(), id);
        this.inventory = inventory;
        this.bagSlot = bagSlot;
        access = new PortableStorageAccess(inventory, bagSlot, SeedBagItem.class);
        this.bag = access.stack();
        if (!inventory.player.level().isClientSide) {
            bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(seeds.getItems());
            seeds.addListener(ignored -> save());
        }
        for (int c = 0; c < 9; c++) addSlot(new Slot(seeds, c, 8 + c * 18, 28) {
            @Override public boolean mayPlace(ItemStack stack) { return stillValid(inventory.player) && SeedBagItem.accepts(stack); }
            @Override public boolean mayPickup(Player player) { return stillValid(player); }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addPlayerSlot(col + row * 9 + 9, 8 + col * 18, 68 + row * 18);
        for (int col = 0; col < 9; col++) addPlayerSlot(col, 8 + col * 18, 126);
    }
    private void addPlayerSlot(int index, int x, int y) {
        addSlot(new Slot(inventory, index, x, y) {
            @Override public boolean mayPickup(Player player) { return index != bagSlot && stillValid(player); }
            @Override public boolean mayPlace(ItemStack stack) { return index != bagSlot && stillValid(inventory.player); }
        });
    }
    private void save() {
        access.save(seeds.getItems());
    }
    @Override public boolean stillValid(Player player) {
        return access.current(player);
    }
    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (!stillValid(player)) return;
        if (type == ClickType.SWAP && button == bagSlot) return;
        if (slot >= 9 && slot < slots.size() && slots.get(slot).getContainerSlot() == bagSlot) return;
        super.clicked(slot, button, type, player);
        save();
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < 9) {
            if (!moveItemStackTo(stack, 9, 45, true)) return ItemStack.EMPTY;
        } else if (!SeedBagItem.accepts(stack) || !moveItemStackTo(stack, 0, 9, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        save();
        return original;
    }
    @Override public void removed(Player player) {
        if (!access.ownedBy(player)) return;
        save();
        access.close();
        super.removed(player);
    }
    @Override public boolean clickMenuButton(Player player,int action){
        if(player.level().isClientSide||!stillValid(player)||action<0||action>1)return false;
        if(action==0){for(int index=9;index<slots.size();index++)quickMoveStack(player,index);}
        else {
            var sorted=new java.util.ArrayList<ItemStack>();
            for(int i=0;i<seeds.getContainerSize();i++)if(!seeds.getItem(i).isEmpty())sorted.add(seeds.getItem(i).copy());
            // Stable ordering within a species; never reconstruct stacks or discard components.
            sorted.sort(java.util.Comparator.comparing(s->net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).toString()));
            for(int i=0;i<seeds.getContainerSize();i++)seeds.setItem(i,i<sorted.size()?sorted.get(i):ItemStack.EMPTY);
        }
        save();broadcastChanges();return true;
    }
}
