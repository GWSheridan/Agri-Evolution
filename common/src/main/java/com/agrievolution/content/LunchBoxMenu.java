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
public final class LunchBoxMenu extends AbstractContainerMenu {
    private final Inventory inventory;
    private final int bagSlot;
    private final ItemStack bag;
    private final PortableStorageAccess access;
    private final SimpleContainer foods = new SimpleContainer(LunchBoxItem.SLOTS);
    private boolean checkingFood;
    private ItemStack approvedFood;
    private boolean inserting, clickAccepts;
    private ItemStack clickInput;
    private final net.minecraft.world.inventory.ContainerData controls;
    public LunchBoxMenu(int id, Inventory inventory, int bagSlot) {
        super(AEMenus.LUNCH_BOX.get(), id);
        this.inventory = inventory;
        this.bagSlot = bagSlot;
        access = new PortableStorageAccess(inventory, bagSlot, LunchBoxItem.class);
        this.bag = access.stack();
        controls=(inventory.player==null||inventory.player.level().isClientSide)?new net.minecraft.world.inventory.SimpleContainerData(20):new net.minecraft.world.inventory.ContainerData(){
            public int get(int i){
                if(i>=0&&i<9)return LunchBoxItem.priority(bag,i);
                if(i>=9&&i<18)return LunchBoxItem.protectedSlot(bag,i-9)?1:0;
                if(i==18)return LunchBoxItem.enabled(bag)?1:0;
                return i==19?LunchBoxItem.feedingStatus(bag,inventory.player):0;
            }
            public void set(int i,int value){} // Server-derived; only button actions may mutate settings.
            public int getCount(){return 20;}
        };
        addDataSlots(controls);
        if (inventory.player!=null && !inventory.player.level().isClientSide) {
            bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(foods.getItems());
            foods.addListener(ignored -> save());
        }
        for (int c = 0; c < 9; c++) addSlot(new Slot(foods, c, 8 + c * 18, 28) {
            @Override public boolean mayPlace(ItemStack stack) { return acceptsFood(stack); }
            @Override public boolean mayPickup(Player player) { return !checkingFood && approvedFood == null && stillValid(player); }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addPlayerSlot(col + row * 9 + 9, 8 + col * 18, 92 + row * 18);
        for (int col = 0; col < 9; col++) addPlayerSlot(col, 8 + col * 18, 150);
    }
    public int priority(int slot){return controls.get(slot);}
    public boolean protectedSlot(int slot){return controls.get(9+slot)!=0;}
    public boolean feedingEnabled(){return controls.get(18)!=0;}
    public String feedingStatus(){return LunchBoxItem.statusLabel(controls.get(19));}
    private void addPlayerSlot(int inventoryIndex, int x, int y) {
        addSlot(new Slot(inventory, inventoryIndex, x, y) {
            @Override public boolean mayPickup(Player player) { return inventoryIndex != bagSlot && !checkingFood && approvedFood == null && stillValid(player); }
            @Override public boolean mayPlace(ItemStack stack) { return inventoryIndex != bagSlot && !checkingFood && approvedFood == null && stillValid(inventory.player); }
        });
    }
    private boolean acceptsFood(ItemStack stack) {
        if (!stillValid(inventory.player)) return false;
        // Shift-click has already checked this stack before moving any items.
        if (approvedFood != null) return stack == approvedFood;
        if (inserting) return clickAccepts && stack == clickInput;
        if (checkingFood) return false;
        var before = stack.copy();
        checkingFood = true;
        try {
            return LunchBoxItem.accepts(stack) && stillValid(inventory.player) && ItemStack.matches(before, stack);
        } finally { checkingFood = false; }
    }
    private void save() {
        access.save(foods.getItems());
    }
    @Override public boolean stillValid(Player player) {
        return access.current(player);
    }
    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (player == null || inserting || checkingFood || approvedFood != null || !stillValid(player)) return;
        if (type == ClickType.SWAP && button == bagSlot) return;
        if (slot >= 9 && slot < slots.size() && slots.get(slot).getContainerSlot() == bagSlot) return;
        if (MenuInsertion.applies(slot, 9, type)) {
            var input = new MenuInsertion(this, inventory, slot, button, type);
            boolean accepted = acceptsFood(input.input());
            if (!stillValid(player) || !input.current()) return;
            clickInput = input.input();
            clickAccepts = accepted;
            inserting = true;
            try { super.clicked(slot, button, type, player); }
            finally { inserting = false; clickInput = null; }
        } else super.clicked(slot, button, type, player);
        save();
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (player == null || inserting || checkingFood || approvedFood != null || !stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < 9) {
            if (!moveItemStackTo(stack, 9, 45, true)) return ItemStack.EMPTY;
        } else {
            if (!acceptsFood(stack) || slot.getItem() != stack || !ItemStack.matches(original, stack)) return ItemStack.EMPTY;
            approvedFood = stack;
            try {
                if (!moveItemStackTo(stack, 0, 9, false)) return ItemStack.EMPTY;
            } finally { approvedFood = null; }
        }
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
        if(player==null||player.level().isClientSide||inserting||checkingFood||approvedFood!=null||!stillValid(player)||action<0||action>20)return false;
        if(action==2)LunchBoxItem.toggle(bag);
        else if(action>=3&&action<12)LunchBoxItem.cyclePriority(bag,action-3);
        else if(action>=12)LunchBoxItem.toggleProtected(bag,action-12);
        else if(action==0){for(int index=9;index<slots.size();index++)quickMoveStack(player,index);}
        else {
            // Sort only within the same priority; protected slots never move.
            for(int priority=0;priority<3;priority++){
            var sorted=new java.util.ArrayList<ItemStack>();
            for(int i=0;i<foods.getContainerSize();i++)if(!LunchBoxItem.protectedSlot(bag,i)&&LunchBoxItem.priority(bag,i)==priority&&!foods.getItem(i).isEmpty())sorted.add(foods.getItem(i).copy());
            // Stable ordering within a food type; never reconstruct stacks or discard components.
            sorted.sort(java.util.Comparator.comparing(s->net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).toString()));
            int next=0;
            for(int i=0;i<foods.getContainerSize();i++)if(!LunchBoxItem.protectedSlot(bag,i)&&LunchBoxItem.priority(bag,i)==priority)foods.setItem(i,next<sorted.size()?sorted.get(next++):ItemStack.EMPTY);
            }
        }
        save();broadcastChanges();return true;
    }
}


