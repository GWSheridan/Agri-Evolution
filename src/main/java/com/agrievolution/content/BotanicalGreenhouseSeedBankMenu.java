package com.agrievolution.content;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Nine real inventory slots; all seed settings are synchronized and changed on the server. */
public final class BotanicalGreenhouseSeedBankMenu extends AbstractContainerMenu {
    private static final int INPUTS = BotanicalGreenhouseBlockEntity.SEED_BANK_SIZE;
    public static final int PRESET_COUNT = BotanicalGreenhouseBlockEntity.PLANTING_PRESET_COUNT;
    public static final int SAVE_PRESET_START = 35;
    public static final int LOAD_PRESET_START = SAVE_PRESET_START + PRESET_COUNT;
    private final Container seeds;
    private final ContainerData settings;
    private final BotanicalGreenhouseBlockEntity greenhouse;

    public BotanicalGreenhouseSeedBankMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(INPUTS), new SimpleContainerData(BotanicalGreenhouseBlockEntity.SEED_SETTINGS_COUNT), null);
    }

    public BotanicalGreenhouseSeedBankMenu(int id, Inventory inventory, BotanicalGreenhouseBlockEntity greenhouse) {
        this(id, inventory, greenhouse.seedBank(), greenhouse.seedBankSettings, greenhouse);
    }

    private BotanicalGreenhouseSeedBankMenu(int id, Inventory inventory, Container seeds,
                                            ContainerData settings, BotanicalGreenhouseBlockEntity greenhouse) {
        super(AEMenus.BOTANICAL_GREENHOUSE_SEED_BANK.get(), id);
        this.seeds = seeds;
        this.settings = settings;
        this.greenhouse = greenhouse;
        checkContainerSize(seeds, INPUTS);
        checkContainerDataCount(settings, BotanicalGreenhouseBlockEntity.SEED_SETTINGS_COUNT);
        addDataSlots(settings);
        seeds.startOpen(inventory.player);
        for (int slot = 0; slot < INPUTS; slot++) {
            addSlot(new Slot(seeds, slot, 16 + slot * 23, 53) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof SpeciesSeedItem;
                }
            });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, 9 + row * 9 + col, 16 + col * 23, 181 + row * 18));
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 16 + col * 23, 244));
        }
    }

    public boolean slotEnabled(int slot) {
        return slot >= 0 && slot < INPUTS && settings.get(slot) != 0;
    }
    public int priority(int slot) {
        return slot >= 0 && slot < INPUTS ? settings.get(9 + slot) : 1;
    }
    public int reservePerSlot() { return settings.get(18); }
    public boolean bestGenetics() { return settings.get(19) != 0; }
    public int plantingSpecies(int slot) {return slot>=0&&slot<INPUTS?settings.get(20+slot):0;}
    public int minimumGene(int gene) {return gene>=0&&gene<3?settings.get(29+gene):0;}

    public int exclusions(int slot) { return settings.get(32+slot); }
    public int eligibleSeeds(int slot) { return settings.get(41+slot); }
    public int totalEligibleSeeds() { int count=0; for(int i=0;i<INPUTS;i++)count+=eligibleSeeds(i); return count; }

    public boolean hasPreset(int index) {
        return settings.get(BotanicalGreenhouseBlockEntity.PRESET_STATUS_START + index) != BotanicalGreenhouseBlockEntity.PRESET_EMPTY;
    }

    public boolean presetMatches(int index) {
        return settings.get(BotanicalGreenhouseBlockEntity.PRESET_STATUS_START + index) == BotanicalGreenhouseBlockEntity.PRESET_MATCHES;
    }

    @Override public boolean clickMenuButton(Player player, int button) {
        if (greenhouse == null || !greenhouse.isSmartFarmingUpgraded()
                || !WorkstationAccess.mayConfigure(greenhouse, player)) return false;
        if (button >= 0 && button < INPUTS) greenhouse.toggleSeedSlot(button);
        else if (button >= INPUTS && button < INPUTS * 2) greenhouse.cycleSeedPriority(button - INPUTS);
        else if (button == 18) greenhouse.cycleSeedReserve();
        else if (button == 19) greenhouse.toggleBestGenetics();
        else if(button>=20&&button<29)greenhouse.togglePlantingSpecies(button-20);
        else if(button>=29&&button<32)greenhouse.changeMinimumPlantingGene(button-29,10);
        else if(button>=32&&button<35)greenhouse.changeMinimumPlantingGene(button-32,-10);
        else if (button >= SAVE_PRESET_START && button < LOAD_PRESET_START) {
            if (!greenhouse.savePlantingPreset(button - SAVE_PRESET_START)) return false;
        } else if (button >= LOAD_PRESET_START && button < LOAD_PRESET_START + PRESET_COUNT) {
            if (!greenhouse.loadPlantingPreset(button - LOAD_PRESET_START)) return false;
        }
        else return false;
        broadcastChanges();
        return true;
    }

    @Override public boolean stillValid(Player player) { return seeds.stillValid(player); }

    @Override public void clicked(int index, int button, ClickType type, Player player) {
        if (stillValid(player)) super.clicked(index, button, type, player);
    }

    @Override public void removed(Player player) {
        super.removed(player);
        seeds.stopOpen(player);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < INPUTS) {
            if (!moveItemStackTo(stack, INPUTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof SpeciesSeedItem) {
            if (!moveItemStackTo(stack, 0, INPUTS, false)) return ItemStack.EMPTY;
        } else return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
