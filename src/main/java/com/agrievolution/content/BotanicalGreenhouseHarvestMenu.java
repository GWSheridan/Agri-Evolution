package com.agrievolution.content;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/** Configuration-only screen. Every action is validated on the server controller. */
public final class BotanicalGreenhouseHarvestMenu extends AbstractContainerMenu {
    private final BotanicalGreenhouseBlockEntity greenhouse;
    private final ContainerData settings;

    public BotanicalGreenhouseHarvestMenu(int id, Inventory inv) {
        this(id, inv, null);
    }
    public BotanicalGreenhouseHarvestMenu(int id, Inventory inv, BotanicalGreenhouseBlockEntity greenhouse) {
        super(AEMenus.BOTANICAL_GREENHOUSE_HARVEST.get(), id);
        this.greenhouse = greenhouse;
        settings = greenhouse == null ? new SimpleContainerData(15) : greenhouse.exportSettings;
        checkContainerDataCount(settings, 15);
        addDataSlots(settings);
    }

    public boolean upgraded() { return settings.get(0) != 0; }
    public boolean exporting() { return settings.get(1) != 0; }
    public boolean nearFull() { return settings.get(2) != 0; }
    public int reserve() { return settings.get(3); }
    public int side() { return settings.get(4); }
    public int transferred() { return settings.get(5); }
    public boolean connected() { return settings.get(6) != 0; }
    public boolean rejected() { return settings.get(7) != 0; }
    public int slotsUsed() { return settings.get(8); }
    public boolean categoryEnabled(int c) { return c >= 0 && c < 3 && settings.get(9 + c * 2) != 0; }
    public int categoryPriority(int c) { return c >= 0 && c < 3 ? settings.get(10 + c * 2) : 0; }
    public String sideName() {
        return switch (side()) {
            case 1 -> "RIGHT";
            case 2 -> "LEFT";
            case 3 -> "ABOVE";
            case 4 -> "BELOW";
            case 5 -> "FRONT";
            default -> "REAR";
        };
    }

    @Override public boolean clickMenuButton(Player player, int button) {
        if (greenhouse == null || !greenhouse.isAutomationUpgraded() || !greenhouse.stillValid(player))
            return false;
        if (player.level().isClientSide || button != 4 && !WorkstationAccess.mayConfigure(greenhouse, player)) return false;
        if (button == 0) greenhouse.toggleExport();
        else if (button == 1) greenhouse.toggleExportMode();
        else if (button == 2) greenhouse.cycleExportReserve();
        else if (button == 3) greenhouse.cycleExportSide();
        else if (button >= 10 && button < 13) greenhouse.toggleExportCategory(button - 10);
        else if (button >= 20 && button < 23) greenhouse.cycleExportPriority(button - 20);
        else if (button == 4 && player instanceof ServerPlayer server) {
            greenhouse.openStorage(server);
            return true;
        } else return false;
        broadcastChanges();
        return true;
    }

    @Override public boolean stillValid(Player player) {
        return greenhouse == null || greenhouse.stillValid(player);
    }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
