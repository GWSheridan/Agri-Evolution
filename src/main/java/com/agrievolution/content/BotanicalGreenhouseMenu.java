package com.agrievolution.content;

import com.agrievolution.farming.SeedBankMonitoring;
import com.agrievolution.genetics.CropSpecies;
import com.agrievolution.genetics.RareTrait;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/** Network-synchronized greenhouse dashboard; irrigation is controlled with buckets on the block. */
public final class BotanicalGreenhouseMenu extends AbstractContainerMenu {
    private final ContainerData data;
    private final BotanicalGreenhouseBlockEntity greenhouse;
    private final ContainerLevelAccess access;

    public BotanicalGreenhouseMenu(int id, Inventory inventory) {
        this(id, new SimpleContainerData(BotanicalGreenhouseBlockEntity.DASHBOARD_DATA_COUNT), ContainerLevelAccess.NULL, null);
    }

    public BotanicalGreenhouseMenu(int id, Inventory inventory, BotanicalGreenhouseBlockEntity greenhouse) {
        this(id, greenhouse.data, ContainerLevelAccess.create(greenhouse.getLevel(), greenhouse.getBlockPos()), greenhouse);
    }

    private BotanicalGreenhouseMenu(int id, ContainerData data, ContainerLevelAccess access,
                                    BotanicalGreenhouseBlockEntity greenhouse) {
        super(AEMenus.BOTANICAL_GREENHOUSE.get(), id);
        checkContainerDataCount(data, BotanicalGreenhouseBlockEntity.DASHBOARD_DATA_COUNT);
        this.greenhouse = greenhouse;
        this.data = data;
        this.access = access;
        addDataSlots(data);
    }

    public int water() { return data.get(0); }
    public int capacity() { return Math.max(1, data.get(1)); }
    public boolean formed() { return data.get(2) != 0; }
    public boolean active() { return data.get(3) != 0; }
    public int farmland() { return data.get(4); }
    public int hydrated() { return data.get(5); }
    public int planted() { return data.get(6); }
    public int stickStatus(int plot) { return data.get(BotanicalGreenhouseBlockEntity.STICK_MAP_START + plot); }
    public GreenhouseWeedStatus weedStatus(int plot) {
        int value = data.get(BotanicalGreenhouseBlockEntity.WEED_MAP_START + GreenhouseWeedStatus.dataIndex(plot));
        return GreenhouseWeedStatus.decode(value, plot);
    }
    public int growing() { return data.get(7); }
    public int mature() { return data.get(8); }
    public int litCrops() { return data.get(9); }
    public int averageLight() { return data.get(10); }
    public int breedingStations() { return data.get(11); }
    public int temperatureC() { return data.get(12); }
    public int humidityPct() { return data.get(13); }
    public boolean heaterEnabled() { return data.get(14) != 0; }
    public boolean ventsEnabled() { return data.get(15) != 0; }
    public int heaterFuelSeconds() { return data.get(16); }
    public int climateGrowthChance() { return data.get(17); }
    public int outsideTemperatureC() { return data.get(18); }
    public boolean upgraded() { return data.get(19) != 0; }
    public boolean harvesting() { return data.get(20) != 0; }
    public boolean replanting() { return data.get(21) != 0; }
    public int storageUsed() { return data.get(22); }
    public int harvestCount() { return data.get(23); }
    public int replantCount() { return data.get(24); }
    public boolean storageFull() { return data.get(25) != 0; }
    public boolean smartFarmingUnlocked() { return data.get(26) != 0; }
    public boolean smartPlantEnabled() { return data.get(27) != 0; }
    public int seedCount() { return data.get(28); }
    public int autoPlantedCount() { return data.get(29); }
    public int emptyPlots() { return data.get(30); }
    public boolean flowerPlantingEnabled() { return data.get(31) != 0; }
    public boolean resourcePlantingEnabled() { return data.get(32) != 0; }
    // All monitoring fields are vanilla menu-synchronized; read-only on the client.
    public int monitorAttempts() { return data.get(33); }
    public int monitorSuccessful() { return data.get(34); }
    public int monitorSkipped() { return Math.max(0, monitorAttempts() - monitorSuccessful()); }
    public int monitorLastResult() { return data.get(35); }
    public int monitorLastSlot() { return data.get(36); }
    public int monitorEligible() { return data.get(38); }
    public int monitorDisabled() { return data.get(39); }
    public int monitorReserved() { return data.get(40); }
    public int monitorFiltered() { return data.get(41); }
    public int monitorEfficiency() {
        return SeedBankMonitoring.efficiencyPercent(monitorAttempts(), monitorSuccessful());
    }
    public String monitorSpeciesName() {
        int ordinalPlusOne = data.get(37);
        if (ordinalPlusOne <= 0 || ordinalPlusOne > CropSpecies.values().length) return "None";
        return CropSpecies.values()[ordinalPlusOne - 1].displayName();
    }
    /** Snapshot of the last successful Seed Bank placement, not the most recent attempted seed. */
    public boolean hasLastPlantedCrop() { return data.get(42) > 0; }
    public String lastPlantedCropName() {
        int id = data.get(42);
        if (id <= 0 || id > CropSpecies.values().length) return "None";
        return CropSpecies.values()[id - 1].displayName();
    }
    public int lastPlantedGrowth() { return data.get(43); }
    public int lastPlantedYield() { return data.get(44); }
    public int lastPlantedResilience() { return data.get(45); }
    public int lastPlantedQuality() {
        return Math.round((lastPlantedGrowth() + lastPlantedYield() + lastPlantedResilience()) / 3.0f);
    }
    public RareTrait lastPlantedRareTrait() {
        int id = data.get(46);
        return RareTrait.values()[Math.clamp(id, 0, RareTrait.values().length - 1)];
    }
    public int lastPlantedSlot() { return data.get(47); }
    public boolean autoBreedEnabled() { return data.get(48) != 0; }
    public int autoBredCount() { return data.get(49); }
    public String autoBreedStatus() {
        return switch (data.get(50)) {
            case 1 -> "Last cross: SUCCESS";
            case 2 -> "Need 2 eligible bank slots";
            case 3 -> "Storage full - breed paused";
            default -> "Ready for compatible seeds";
        };
    }

    public String monitorResultLabel() {
        return switch (monitorLastResult()) {
            case SeedBankMonitoring.PLANTED -> "PLANTED";
            case SeedBankMonitoring.EMPTY_BANK -> "Seed Bank empty";
            case SeedBankMonitoring.DISABLED -> "All seed slots disabled";
            case SeedBankMonitoring.RESERVED -> "All seeds protected by reserve";
            case SeedBankMonitoring.FILTERED -> "Category, species or genetics filtered";
            case SeedBankMonitoring.NO_PLOTS -> "No empty farmland available";
            case SeedBankMonitoring.PLACEMENT_FAILED -> "Crop placement blocked";
            case SeedBankMonitoring.MIXED_EXCLUSIONS -> "Multiple seed-bank exclusions";
            default -> "No planting cycles yet";
        };
    }
    public boolean heaterBurning() { return active() && heaterEnabled() && heaterFuelSeconds() > 0; }
    /** Currently drains exactly 1 mB per second while active. */
    public int secondsRemaining() { return water(); }
    public boolean lowWater() { return water() > 0 && water() <= 600; }

    /** Vanilla menu-button packet, no custom network registration needed. */
    @Override public boolean clickMenuButton(Player player, int button) {
        if (greenhouse == null || !stillValid(player)) return false;
        if (button == 0) greenhouse.toggleHeater();
        else if (button == 1) greenhouse.toggleVentilation();
        else if (button == 2 && greenhouseUpgradeAllowed()) greenhouse.toggleAutomation();
        else if (button == 3 && greenhouseUpgradeAllowed()) greenhouse.toggleReplant();
        else if (button == 5 && greenhouse.isSmartFarmingUpgraded()) greenhouse.toggleAutoPlant();
        else if (button == 10 && greenhouse.isSmartFarmingUpgraded()) greenhouse.toggleAutoBreed();
        else if (button == 7 && greenhouse.isSmartFarmingUpgraded()) greenhouse.toggleFlowerPlanting();
        else if (button == 8 && greenhouse.isSmartFarmingUpgraded()) greenhouse.toggleResourcePlanting();
        else if (button == 6 && greenhouse.isSmartFarmingUpgraded()
                && player instanceof net.minecraft.server.level.ServerPlayer server) {
            greenhouse.openSeedBank(server);
            return true;
        }
        else if (button == 9 && greenhouseUpgradeAllowed() && player instanceof net.minecraft.server.level.ServerPlayer server) {
            greenhouse.openHarvestManagement(server);
            return true;
        }
        else if (button == 4 && greenhouseUpgradeAllowed() && player instanceof net.minecraft.server.level.ServerPlayer server) {
            greenhouse.openStorage(server);
            return true; // opening a different menu closes this dashboard
        } else return false;
        broadcastChanges();
        return true;
    }

    private boolean greenhouseUpgradeAllowed() { return greenhouse != null && upgraded(); }

    @Override public boolean stillValid(Player player) {
        return stillValid(access, player, AEBlocks.BOTANICAL_GREENHOUSE.get());
    }

    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
