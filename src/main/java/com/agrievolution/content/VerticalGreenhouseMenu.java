package com.agrievolution.content;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class VerticalGreenhouseMenu extends AbstractContainerMenu {
    public static final int TOGGLE_GROW = 0, TOGGLE_BREED = 1, TOGGLE_PAIRING = 2;
    public static final int CHOOSE_A = 3, CHOOSE_B = CHOOSE_A + VerticalGreenhouseBlockEntity.TRAYS;
    public static final int CYCLE_WATER_INTAKE = CHOOSE_B + VerticalGreenhouseBlockEntity.TRAYS;
    public static final int CYCLE_ROUTE = CYCLE_WATER_INTAKE + 1;
    public static final int CYCLE_BATCH = CYCLE_ROUTE + VerticalGreenhouseBlockEntity.ROUTE_COUNT, RESET_BATCH = CYCLE_BATCH + 1;
    public static final int TARGET_SEED = RESET_BATCH + 1, TARGET_GENE = TARGET_SEED + 1;
    public static final int REJECT_ROUTE = TARGET_GENE + 6;
    public static final int FIRST_TRAY_X = 31, TRAY_PITCH = 46, TRAY_Y = 80;
    public static final int FIRST_OUTPUT_X = 126, HARVEST_Y = 118, OFFSPRING_Y = 136, REJECTS_Y = 154;
    public static final int INVENTORY_X = 81, INVENTORY_Y = 190, HOTBAR_Y = 244;
    private static final VerticalGreenhouseBlockEntity.Status[] STATUSES = VerticalGreenhouseBlockEntity.Status.values();
    private static final VerticalGreenhouseBlockEntity.WaterStatus[] WATER_STATUSES = VerticalGreenhouseBlockEntity.WaterStatus.values();
    private final VerticalGreenhouseBlockEntity greenhouse;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public VerticalGreenhouseMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(VerticalGreenhouseBlockEntity.SLOTS),
                new SimpleContainerData(VerticalGreenhouseBlockEntity.DATA_COUNT), ContainerLevelAccess.NULL, null);
    }

    public VerticalGreenhouseMenu(int id, Inventory inventory, VerticalGreenhouseBlockEntity greenhouse) {
        this(id, inventory, greenhouse, greenhouse.data,
                ContainerLevelAccess.create(greenhouse.getLevel(), greenhouse.getBlockPos()), greenhouse);
    }

    private VerticalGreenhouseMenu(int id, Inventory inventory, Container contents, ContainerData data,
                                   ContainerLevelAccess access, VerticalGreenhouseBlockEntity greenhouse) {
        super(AEMenus.VERTICAL_GREENHOUSE.get(), id);
        this.greenhouse = greenhouse;
        this.access = access;
        this.data = data;
        checkContainerSize(contents, VerticalGreenhouseBlockEntity.SLOTS);
        checkContainerDataCount(data, VerticalGreenhouseBlockEntity.DATA_COUNT);
        for (int tray = 0; tray < VerticalGreenhouseBlockEntity.TRAYS; tray++) {
            addSlot(new Slot(contents, tray, FIRST_TRAY_X + tray * TRAY_PITCH, TRAY_Y) {
                @Override public boolean mayPlace(ItemStack stack) { return VerticalGreenhouseBlockEntity.acceptsSeed(stack); }
                @Override public int getMaxStackSize() { return 1; }
            });
        }
        for (int i = VerticalGreenhouseBlockEntity.HARVEST; i < VerticalGreenhouseBlockEntity.SLOTS; i++) {
            int col = (i - VerticalGreenhouseBlockEntity.HARVEST) % 9;
            int y = i < VerticalGreenhouseBlockEntity.OFFSPRING ? HARVEST_Y
                    : i < VerticalGreenhouseBlockEntity.REJECTS ? OFFSPRING_Y : REJECTS_Y;
            addSlot(new Slot(contents, i, FIRST_OUTPUT_X + col * 18, y) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, 9 + row * 9 + col, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, INVENTORY_X + col * 18, HOTBAR_Y));
        addDataSlots(data);
    }

    public int water() { return data.get(0); }
    public boolean growEnabled() { return (data.get(1) & 1) != 0; }
    public boolean breedEnabled() { return (data.get(1) & 2) != 0; }
    public VerticalGreenhouseBlockEntity.Status growStatus() { return STATUSES[data.get(2)]; }
    public VerticalGreenhouseBlockEntity.Status breedStatus() { return STATUSES[data.get(3)]; }
    public int breedProgress() { return data.get(4); }
    public int parentA() { return data.get(5); }
    public int parentB() { return data.get(6); }
    public int trayProgress(int tray) { return data.get(VerticalGreenhouseBlockEntity.PROGRESS_START + tray); }
    public boolean fixedPair() { return data.get(VerticalGreenhouseBlockEntity.PAIR_MODE) != 0; }
    public int selectedA() { return data.get(VerticalGreenhouseBlockEntity.SELECTED_A); }
    public int selectedB() { return data.get(VerticalGreenhouseBlockEntity.SELECTED_B); }
    public VerticalGreenhouseBlockEntity.WaterIntake waterIntake() {
        return VerticalGreenhouseBlockEntity.WaterIntake.fromId(data.get(VerticalGreenhouseBlockEntity.WATER_INTAKE));
    }
    public VerticalGreenhouseBlockEntity.WaterStatus waterStatus() {
        return WATER_STATUSES[data.get(VerticalGreenhouseBlockEntity.WATER_STATUS)];
    }
    public VerticalGreenhouseBlockEntity.OutputRoute outputRoute(int face) {
        return VerticalGreenhouseBlockEntity.OutputRoute.fromId(data.get(VerticalGreenhouseBlockEntity.ROUTE_START + face));
    }
    public int batchTarget() { return data.get(VerticalGreenhouseBlockEntity.BATCH_TARGET); }
    public int batchCount() { return data.get(VerticalGreenhouseBlockEntity.BATCH_COUNT); }
    public int targetSeed() { return data.get(VerticalGreenhouseBlockEntity.TARGET_SEED); }
    public int minimumGene(int gene) { return data.get(VerticalGreenhouseBlockEntity.MIN_GROWTH + gene) * 10; }

    @Override public boolean stillValid(Player player) {
        return (greenhouse == null || greenhouse.stillValid(player))
                && stillValid(access, player, AEBlocks.VERTICAL_GREENHOUSE.get());
    }

    @Override public void clicked(int index, int button, ClickType type, Player player) {
        if (stillValid(player)) super.clicked(index, button, type, player);
    }

    @Override public boolean clickMenuButton(Player player, int button) {
        if (greenhouse == null || player.isSpectator() || !player.mayBuild()
                || !player.level().mayInteract(player, greenhouse.getBlockPos())
                || !stillValid(player) || !greenhouse.complete()) return false;
        if (button == TOGGLE_GROW || button == TOGGLE_BREED) {
            greenhouse.toggle(button);
        } else if (button == TOGGLE_PAIRING) {
            greenhouse.togglePairingMode();
        } else if (button == CYCLE_WATER_INTAKE) {
            greenhouse.cycleWaterIntake();
        } else if (button == CYCLE_BATCH) {
            greenhouse.cycleBatchTarget();
        } else if (button == RESET_BATCH && batchTarget() > 0) {
            greenhouse.resetBatch();
        } else if (button == TARGET_SEED) {
            if (!greenhouse.setTargetSeed(getCarried())) return false;
        } else if (button >= TARGET_GENE && button < TARGET_GENE + 6) {
            greenhouse.cycleTargetGene((button - TARGET_GENE) % 3, button < TARGET_GENE + 3 ? 1 : -1);
        } else if (button >= REJECT_ROUTE && button < REJECT_ROUTE + VerticalGreenhouseBlockEntity.ROUTE_COUNT) {
            greenhouse.toggleRejectRoute(button - REJECT_ROUTE);
        } else if (button >= CYCLE_ROUTE && button < CYCLE_BATCH) {
            greenhouse.cycleOutputRoute(button - CYCLE_ROUTE);
        } else if (button >= CHOOSE_A && button < CYCLE_WATER_INTAKE) {
            boolean second = button >= CHOOSE_B;
            if (!greenhouse.selectParent(button - (second ? CHOOSE_B : CHOOSE_A), second)) return false;
        } else {
            return false;
        }
        broadcastChanges();
        return true;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = getSlot(index);
        ItemStack source = slot.getItem();
        if (source.isEmpty() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack original = source.copy();
        int firstPlayerSlot = VerticalGreenhouseBlockEntity.SLOTS;
        if (index < firstPlayerSlot) {
            if (!moveItemStackTo(source, firstPlayerSlot, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!VerticalGreenhouseBlockEntity.acceptsSeed(source)
                || !moveItemStackTo(source, 0, VerticalGreenhouseBlockEntity.TRAYS, false)) return ItemStack.EMPTY;
        if (source.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, source);
        return original;
    }
}
