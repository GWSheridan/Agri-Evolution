package com.agrievolution.content;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public final class IrrigationControllerMenu extends AbstractContainerMenu {
    private static final int ZONES = IrrigationControllerBlockEntity.ZONES;
    private static final int COUNT = 4 + ZONES * 3;
    private final ContainerData data;
    private final String[] labels = new String[ZONES];
    private final IrrigationControllerBlockEntity controller;

    public IrrigationControllerMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (IrrigationControllerBlockEntity) null);
        for (int i = 0; i < ZONES; i++) labels[i] = buf.readUtf(64);
    }

    IrrigationControllerMenu(int id, Inventory inv, IrrigationControllerBlockEntity controller) {
        super(AEMenus.IRRIGATION_CONTROLLER.get(), id);
        this.controller = controller;
        data = controller == null ? new SimpleContainerData(COUNT) : new ContainerData() {
            @Override public int get(int index) {
                if (index == 0) return controller.enabled() ? 1 : 0;
                if (index == 1) return controller.reserveWater();
                if (index == 2) return controller.active() + 1;
                if (index == 3) return controller.manual() + 1;
                int slot = (index - 4) / 3;
                return switch ((index - 4) % 3) {
                    case 0 -> controller.linked(slot) ? 1 : 0;
                    case 1 -> controller.startHour(slot);
                    default -> controller.duration(slot);
                };
            }
            @Override public void set(int index, int value) { }
            @Override public int getCount() { return COUNT; }
        };
        addDataSlots(data);
    }

    public boolean enabled() { return data.get(0) != 0; }
    public int reserve() { return data.get(1); }
    public int active() { return data.get(2) - 1; }
    public int manual() { return data.get(3) - 1; }
    public boolean linked(int row) { return data.get(4 + row * 3) != 0; }
    public int hour(int row) { return data.get(5 + row * 3); }
    public int duration(int row) { return data.get(6 + row * 3); }
    public String label(int row) { return linked(row) ? labels[row] : "Unlinked"; }

    @Override public boolean stillValid(Player player) {
        if (controller == null) return true;
        return !controller.isRemoved() && stillValid(ContainerLevelAccess.create(controller.getLevel(), controller.getBlockPos()),
                player, AEBlocks.IRRIGATION_CONTROLLER.get());
    }

    @Override public boolean clickMenuButton(Player player, int button) {
        if (controller == null || !stillValid(player) || !player.mayBuild()
                || !controller.getLevel().mayInteract(player, controller.getBlockPos()) || button < 0 || button >= 7 * ZONES) return false;
        controller.action(button % ZONES, button / ZONES);
        return true;
    }

    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
}
