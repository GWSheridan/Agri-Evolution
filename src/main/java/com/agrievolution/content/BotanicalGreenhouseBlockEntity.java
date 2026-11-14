package com.agrievolution.content;

import com.agrievolution.farming.BotanicalGreenhouseLayout;
import com.agrievolution.farming.GreenhouseClimate;
import com.agrievolution.farming.SeedBankSelection;
import com.agrievolution.farming.SeedBankMonitoring;
import com.agrievolution.farming.HarvestExportPolicy;
import com.agrievolution.farming.ResourceCropRules;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import com.agrievolution.genetics.CropGenome;
import com.agrievolution.genetics.CropSpecies;
import com.agrievolution.genetics.GenomeStorage;
import com.agrievolution.genetics.HybridLineageStorage;
import com.agrievolution.genetics.RareTrait;
import com.agrievolution.genetics.RareTraitStorage;
import com.agrievolution.genetics.RetiredResourceCrops;
import com.agrievolution.genetics.GenomeBreeder;
import com.agrievolution.genetics.SpeciesBreeder;
import com.agrievolution.farming.ExtraSeedRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerPlayer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.FarmlandWaterManager;
import net.neoforged.neoforge.common.ticket.AABBTicket;
import org.jetbrains.annotations.Nullable;

/** Persistent 4-bucket reservoir + temporary NeoForge farmland-water ticket. */
public final class BotanicalGreenhouseBlockEntity extends BlockEntity implements MenuProvider, Container {
    public static final int SEED_BANK_SIZE = 9;
    public static final int STORAGE_SIZE = 27;
    private NonNullList<ItemStack> storage = NonNullList.withSize(STORAGE_SIZE, ItemStack.EMPTY);
    // Old controllers start in basic mode; this upgrade must be deliberately installed.
    private boolean automationUpgraded;
    private boolean smartFarmingUpgraded;
    private boolean autoPlant;
    // v0.21.0: both categories stay enabled in old saves unless explicitly disabled.
    private boolean plantFlowerSeeds = true;
    private boolean plantResourceSeeds = true;
    private int totalAutoPlanted;
    private int seedSlotCursor;
    private int plotCursor;
    // v0.23: recent attempts are bounded to fit the vanilla 16-bit menu data protocol.
    // Monitoring never changes which seed is selected, planted, or harvested.
    private int monitorAttempts;
    private int monitorSuccessful;
    private int monitorLastResult;
    private int monitorLastSlot; // 1..9; 0 = no slot selected
    private int monitorLastSpecies; // Last attempted species; failures may overwrite it.
    // Most recent SUCCESSFUL Seed Bank planting, retained even when subsequent attempts fail.
    // Read-only status only; no part of crop selection, mutation, or harvest depends on it.
    private int monitorPlantedSpecies; // CropSpecies ordinal + 1, 0 = not recorded
    private int monitorPlantedSlot; // 1..9
    private CropGenome monitorPlantedGenome = CropGenome.wild();
    private RareTrait monitorPlantedRareTrait = RareTrait.NONE;
    // v0.22.0: configuration follows the nine physical slots, including seed genetics.
    // Missing NBT keys in existing saves mean all slots enabled, normal priority, no reserve.
    private int disabledSeedSlots;
    private final int[] seedPriorities = new int[SEED_BANK_SIZE];
    private int reservedPerSlot;
    private final int[] plantingSpecies = new int[SEED_BANK_SIZE]; // synced ordinal + 1; NBT uses stable IDs
    private final int[] minimumPlantingGenes = new int[3];
    public static final int PLANTING_PRESET_COUNT = 2;
    static final int PRESET_STATUS_START = 50;
    static final int PRESET_EMPTY = 0, PRESET_SAVED = 1, PRESET_MATCHES = 2;
    public static final int SEED_SETTINGS_COUNT = PRESET_STATUS_START + PLANTING_PRESET_COUNT;
    private final PlantingPreset[] plantingPresets = new PlantingPreset[PLANTING_PRESET_COUNT];

    private record PlantingPreset(int[] species, int[] minimumGenes) {}

    boolean savePlantingPreset(int index) {
        if (!smartFarmingUpgraded || index < 0 || index >= plantingPresets.length) return false;
        plantingPresets[index] = new PlantingPreset(plantingSpecies.clone(), minimumPlantingGenes.clone());
        setChanged();
        return true;
    }

    boolean loadPlantingPreset(int index) {
        if (!smartFarmingUpgraded || index < 0 || index >= plantingPresets.length) return false;
        PlantingPreset preset = plantingPresets[index];
        if (preset == null) return false;
        System.arraycopy(preset.species(), 0, plantingSpecies, 0, plantingSpecies.length);
        System.arraycopy(preset.minimumGenes(), 0, minimumPlantingGenes, 0, minimumPlantingGenes.length);
        setChanged();
        return true;
    }

    private int presetStatus(int index) {
        PlantingPreset preset = plantingPresets[index];
        if (preset == null) return PRESET_EMPTY;
        return Arrays.equals(preset.species(), plantingSpecies) && Arrays.equals(preset.minimumGenes(), minimumPlantingGenes)
                ? PRESET_MATCHES : PRESET_SAVED;
    }

    private static CompoundTag writeSpeciesLocks(int[] locks) {
        var tag = new CompoundTag();
        for (int i = 0; i < locks.length; i++) tag.putString("Slot" + i, RetiredResourceCrops.monitorId(locks[i]));
        return tag;
    }

    private static void readSpeciesLocks(CompoundTag tag, int[] locks) {
        for (int i = 0; i < locks.length; i++) locks[i] = RetiredResourceCrops.monitorValue(tag.getString("Slot" + i));
    }

    private static void readMinimumGenes(int[] saved, int[] genes) {
        for (int i = 0; i < genes.length; i++) genes[i] = i < saved.length ? Math.clamp(saved[i], 0, 100) : 0;
    }

    public int plantingExclusions(int slot) {
        if(slot<0||slot>=SEED_BANK_SIZE)return 1;
        var stack=seedBank.getItem(slot);
        if(stack.isEmpty()||!(stack.getItem() instanceof SpeciesSeedItem seed))return 1;
        int flags=seedSlotEnabled(slot)?0:2;
        if(!(seed.species().isResource()?plantResourceSeeds:plantFlowerSeeds))flags|=4;
        if(plantingSpecies[slot]!=0&&plantingSpecies[slot]!=seed.species().ordinal()+1)flags|=8;
        var genes=GenomeStorage.fromSeed(stack);
        if(genes.growth()<minimumPlantingGenes[0])flags|=16;
        if(genes.yield()<minimumPlantingGenes[1])flags|=32;
        if(genes.resilience()<minimumPlantingGenes[2])flags|=64;
        if(stack.getCount()<=reservedPerSlot)flags|=128;
        return flags;
    }
    public int eligibleSeedCount(int slot) {
        return plantingExclusions(slot)==0?Math.max(0,seedBank.getItem(slot).getCount()-reservedPerSlot):0;
    }
    public boolean plantingAllowed(int slot, ItemStack stack) {
        if(slot<0||slot>=SEED_BANK_SIZE||!(stack.getItem() instanceof SpeciesSeedItem seed))return false;
        var genome=GenomeStorage.fromSeed(stack);
        return (plantingSpecies[slot]==0||plantingSpecies[slot]==seed.species().ordinal()+1)
            &&genome.growth()>=minimumPlantingGenes[0]&&genome.yield()>=minimumPlantingGenes[1]
            &&genome.resilience()>=minimumPlantingGenes[2];
    }
    public void togglePlantingSpecies(int slot) {
        if(!smartFarmingUpgraded||slot<0||slot>=SEED_BANK_SIZE)return;
        if(plantingSpecies[slot]!=0)plantingSpecies[slot]=0;
        else if(seedBank.getItem(slot).getItem() instanceof SpeciesSeedItem seed)plantingSpecies[slot]=seed.species().ordinal()+1;
        setChanged();
    }
    public void changeMinimumPlantingGene(int gene,int delta) {
        if(!smartFarmingUpgraded||gene<0||gene>=3)return;
        minimumPlantingGenes[gene]=Math.clamp(minimumPlantingGenes[gene]+delta,0,100);
        setChanged();
    }
    private boolean preferStrongestSeeds = true;
    private final SimpleContainer seedBank = new SimpleContainer(SEED_BANK_SIZE) {
        @Override public boolean canPlaceItem(int slot, ItemStack stack) {
            return stack.getItem() instanceof SpeciesSeedItem;
        }
        @Override public void setChanged() {
            super.setChanged();
            BotanicalGreenhouseBlockEntity.this.setChanged();
        }
        @Override public boolean stillValid(Player player) {
            return BotanicalGreenhouseBlockEntity.this.stillValid(player);
        }
    };
    public final ContainerData seedBankSettings = new ContainerData() {
        @Override public int get(int index) {
            if (index >= 0 && index < SEED_BANK_SIZE) return seedSlotEnabled(index) ? 1 : 0;
            if (index >= SEED_BANK_SIZE && index < SEED_BANK_SIZE * 2)
                return seedPriorities[index - SEED_BANK_SIZE];
            if (index == 18) return reservedPerSlot;
            if (index == 19) return preferStrongestSeeds ? 1 : 0;
            if(index>=20&&index<29)return plantingSpecies[index-20];
            if(index>=29&&index<32)return minimumPlantingGenes[index-29];
            if(index>=32&&index<41)return plantingExclusions(index-32);
            if(index>=41&&index<50)return eligibleSeedCount(index-41);
            if (index >= PRESET_STATUS_START && index < SEED_SETTINGS_COUNT) return presetStatus(index - PRESET_STATUS_START);
            return 0;
        }
        @Override public void set(int index, int value) {
            // ContainerData also synchronizes client menu copies; the server remains authoritative.
            if (index >= 0 && index < SEED_BANK_SIZE) {
                if (value != 0) disabledSeedSlots &= ~(1 << index);
                else disabledSeedSlots |= 1 << index;
            } else if (index >= 9 && index < 18) seedPriorities[index - 9] = Math.max(0, Math.min(2, value));
            else if (index == 18) reservedPerSlot = Math.max(0, Math.min(64, value));
            else if (index == 19) preferStrongestSeeds = value != 0;
            else if(index>=20&&index<29)plantingSpecies[index-20]=Math.clamp(value,0,CropSpecies.values().length);
            else if(index>=29&&index<32)minimumPlantingGenes[index-29]=Math.clamp(value,0,100);
        }
        @Override public int getCount() { return SEED_SETTINGS_COUNT; }
    };
    // v0.30: separate from auto-plant, auto-harvest and greenhouse irrigation.
    // Defaults OFF for existing worlds until the player chooses to breed.
    private boolean autoBreed;
    private int totalAutoBred;
    private int lastBreedResult; // 0 waiting, 1 success, 2 no eligible parents, 3 output blocked
    private int breedSlotCursor;
    private boolean autoHarvest;
    private boolean autoReplant = true;
    private int totalHarvests;
    private int totalReplants;
    private boolean storageBlocked;

    // 0.24: opt-in inventory transfer; no extra upgrade item is necessary at Tier 2.
    // Old worlds default to export OFF. No item is removed unless a neighbor accepts it.
    private boolean autoExport;
    private boolean exportNearFullOnly;
    private boolean exportResourceItems = true;
    private boolean exportBotanicalItems = true;
    private boolean exportSeeds; // genetic seeds stay at home unless explicitly enabled
    private final int[] exportPriorities = {2, 1, 0}; // high resources, normal botanicals, low seeds
    private int exportReserve; // 0/16/32/64 items per identical item & data components
    private int exportSide; // 0 rear; 1 right; 2 left; 3 above; 4 below; 5 front
    private int exportCursor;
    private int exportedItems;
    // Transient diagnostics; no need to save world-dependent neighbor connection status.
    private boolean exportTargetConnected;
    private boolean exportTargetRejected;

    public final ContainerData exportSettings = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> automationUpgraded ? 1 : 0;
                case 1 -> autoExport ? 1 : 0;
                case 2 -> exportNearFullOnly ? 1 : 0;
                case 3 -> exportReserve;
                case 4 -> exportSide;
                case 5 -> Math.min(30000, exportedItems);
                case 6 -> exportTargetConnected ? 1 : 0;
                case 7 -> exportTargetRejected ? 1 : 0;
                case 8 -> occupiedStorageSlots();
                case 9 -> exportResourceItems ? 1 : 0;
                case 10 -> exportPriorities[0];
                case 11 -> exportBotanicalItems ? 1 : 0;
                case 12 -> exportPriorities[1];
                case 13 -> exportSeeds ? 1 : 0;
                case 14 -> exportPriorities[2];
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {
            // Server-authoritative; clients receive these values via normal menu data slots.
            switch (index) {
                case 1 -> autoExport = value != 0;
                case 2 -> exportNearFullOnly = value != 0;
                case 3 -> exportReserve = Math.max(0, Math.min(64, value));
                case 4 -> exportSide = Math.floorMod(value, 6);
                case 5 -> exportedItems = Math.max(0, value);
                case 6 -> exportTargetConnected = value != 0;
                case 7 -> exportTargetRejected = value != 0;
                case 9 -> exportResourceItems = value != 0;
                case 10 -> exportPriorities[0] = Math.clamp(value, 0, 2);
                case 11 -> exportBotanicalItems = value != 0;
                case 12 -> exportPriorities[1] = Math.clamp(value, 0, 2);
                case 13 -> exportSeeds = value != 0;
                case 14 -> exportPriorities[2] = Math.clamp(value, 0, 2);
                default -> { }
            }
        }
        @Override public int getCount() { return 15; }
    };

    public static final int CAPACITY = 4000;
    private int water;
    private int tickCount;
    private boolean formed;
    private AABBTicket waterTicket;

    // Live dashboard values; derived from loaded world state, never persisted.
    public static final int STICK_MAP_START = 51;
    public static final int WEED_MAP_START = STICK_MAP_START + GreenhouseStickStatus.PLOT_COUNT;
    public static final int DASHBOARD_DATA_COUNT = WEED_MAP_START + GreenhouseWeedStatus.DATA_COUNT;
    private final int[] stickMap = new int[GreenhouseStickStatus.PLOT_COUNT];
    private final int[] weedMap = new int[GreenhouseWeedStatus.DATA_COUNT];
    private int farmlandCount;
    private int hydratedCount;
    private int plantedCount;
    private int growingCount;
    private int matureCount;
    private int adequatelyLitCount;
    private int averageLight;
    private int breedingStationCount;
    private int emptyPlotCount;

    // 0.12.0: persistent, player-controlled heating and ventilation.
    // Fuel is measured in seconds and burns only while the sealed greenhouse is active.
    private boolean heaterEnabled;
    private boolean ventsEnabled;
    private int heaterFuelSeconds;
    private int estimatedTemperatureC;
    private int simulatedHumidity;
    private int growthBonusChance = 25;
    private int outsideTemperatureC;


    public final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            if (index >= STICK_MAP_START && index < WEED_MAP_START) return stickMap[index - STICK_MAP_START];
            if (index >= WEED_MAP_START && index < DASHBOARD_DATA_COUNT) return weedMap[index - WEED_MAP_START];
            return switch (index) {
                case 0 -> water;
                case 1 -> CAPACITY;
                case 2 -> formed ? 1 : 0;
                case 3 -> formed && water > 0 ? 1 : 0;
                case 4 -> farmlandCount;
                case 5 -> hydratedCount;
                case 6 -> plantedCount;
                case 7 -> growingCount;
                case 8 -> matureCount;
                case 9 -> adequatelyLitCount;
                case 10 -> averageLight;
                case 11 -> breedingStationCount;
                case 12 -> estimatedTemperatureC;
                case 13 -> simulatedHumidity;
                case 14 -> heaterEnabled ? 1 : 0;
                case 15 -> ventsEnabled ? 1 : 0;
                case 16 -> heaterFuelSeconds;
                case 17 -> growthBonusChance;
                case 18 -> outsideTemperatureC;
                case 19 -> automationUpgraded ? 1 : 0;
                case 20 -> autoHarvest ? 1 : 0;
                case 21 -> autoReplant ? 1 : 0;
                case 22 -> occupiedStorageSlots();
                case 23 -> Math.min(30000, totalHarvests);
                case 24 -> Math.min(30000, totalReplants);
                case 25 -> storageBlocked ? 1 : 0;
                case 26 -> smartFarmingUpgraded ? 1 : 0;
                case 27 -> autoPlant ? 1 : 0;
                case 28 -> availableSeedCount();
                case 29 -> Math.min(30000, totalAutoPlanted);
                case 30 -> emptyPlotCount;
                case 31 -> plantFlowerSeeds ? 1 : 0;
                case 32 -> plantResourceSeeds ? 1 : 0;
                case 33 -> monitorAttempts;
                case 34 -> monitorSuccessful;
                case 35 -> monitorLastResult;
                case 36 -> monitorLastSlot;
                case 37 -> monitorLastSpecies;
                case 38 -> seedBankSummary().eligible();
                case 39 -> seedBankSummary().disabled();
                case 40 -> seedBankSummary().reserved();
                case 41 -> seedBankSummary().filtered();
                case 42 -> monitorPlantedSpecies;
                case 43 -> monitorPlantedGenome.growth();
                case 44 -> monitorPlantedGenome.yield();
                case 45 -> monitorPlantedGenome.resilience();
                case 46 -> monitorPlantedRareTrait.ordinal();
                case 47 -> monitorPlantedSlot;
                case 48 -> autoBreed ? 1 : 0;
                case 49 -> Math.min(30000, totalAutoBred);
                case 50 -> lastBreedResult;
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {
            switch (index) {
                case 0 -> water = value;
                case 2 -> formed = value != 0;
                case 4 -> farmlandCount = value;
                case 5 -> hydratedCount = value;
                case 6 -> plantedCount = value;
                case 7 -> growingCount = value;
                case 8 -> matureCount = value;
                case 9 -> adequatelyLitCount = value;
                case 10 -> averageLight = value;
                case 11 -> breedingStationCount = value;
                case 12 -> estimatedTemperatureC = value;
                case 13 -> simulatedHumidity = value;
                case 14 -> heaterEnabled = value != 0;
                case 15 -> ventsEnabled = value != 0;
                case 16 -> heaterFuelSeconds = value;
                case 17 -> growthBonusChance = value;
                case 18 -> outsideTemperatureC = value;
                case 19 -> automationUpgraded = value != 0;
                case 20 -> autoHarvest = value != 0;
                case 21 -> autoReplant = value != 0;
                case 23 -> totalHarvests = value;
                case 24 -> totalReplants = value;
                case 25 -> storageBlocked = value != 0;
                case 26 -> smartFarmingUpgraded = value != 0;
                case 27 -> autoPlant = value != 0;
                case 29 -> totalAutoPlanted = value;
                case 31 -> plantFlowerSeeds = value != 0;
                case 32 -> plantResourceSeeds = value != 0;
                case 33 -> monitorAttempts = value;
                case 34 -> monitorSuccessful = value;
                case 35 -> monitorLastResult = value;
                case 36 -> monitorLastSlot = value;
                case 37 -> monitorLastSpecies = value;
                case 42 -> monitorPlantedSpecies = Math.clamp(value, 0, CropSpecies.values().length);
                case 43 -> monitorPlantedGenome = new CropGenome(value,
                        monitorPlantedGenome.yield(), monitorPlantedGenome.resilience());
                case 44 -> monitorPlantedGenome = new CropGenome(monitorPlantedGenome.growth(),
                        value, monitorPlantedGenome.resilience());
                case 45 -> monitorPlantedGenome = new CropGenome(monitorPlantedGenome.growth(),
                        monitorPlantedGenome.yield(), value);
                case 46 -> monitorPlantedRareTrait = RareTrait.values()[Math.clamp(value, 0,
                        RareTrait.values().length - 1)];
                case 47 -> monitorPlantedSlot = Math.clamp(value, 0, SEED_BANK_SIZE);
                case 48 -> autoBreed = value != 0;
                case 49 -> totalAutoBred = Math.max(0, value);
                case 50 -> lastBreedResult = Math.clamp(value, 0, 3);
                default -> { }
            }
        }
        @Override public int getCount() { return DASHBOARD_DATA_COUNT; }
    };

    public BotanicalGreenhouseBlockEntity(BlockPos pos, BlockState state) {
        super(AEBlockEntities.BOTANICAL_GREENHOUSE.get(), pos, state);
        java.util.Arrays.fill(seedPriorities, 1);
    }

    @Override public Component getDisplayName() {
        return Component.translatable("container.agri_evolution.botanical_greenhouse");
    }

    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BotanicalGreenhouseMenu(id, inventory, this);
    }

    public boolean addWater(int amount) {
        if (amount <= 0 || water > CAPACITY - amount) return false;
        water += amount;
        refresh();
        setChanged();
        return true;
    }

    public boolean removeWater(int amount) {
        if (amount <= 0 || water < amount) return false;
        water -= amount;
        refresh();
        setChanged();
        return true;
    }

    public boolean addHeaterFuel() {
        if (heaterFuelSeconds > GreenhouseClimate.MAX_FUEL_SECONDS - GreenhouseClimate.COAL_SECONDS)
            return false;
        heaterFuelSeconds += GreenhouseClimate.COAL_SECONDS;
        setChanged();
        refresh();
        return true;
    }

    public void toggleHeater() {
        heaterEnabled = !heaterEnabled;
        setChanged();
        refresh();
    }

    public void toggleVentilation() {
        ventsEnabled = !ventsEnabled;
        setChanged();
        refresh();
    }

    /** The upgrade is applied to the EXISTING block entity and kept across world saves. */
    public boolean installAutomationUpgrade() {
        if (automationUpgraded) return false;
        automationUpgraded = true;
        autoHarvest = true;
        autoReplant = true;
        setChanged();
        refresh();
        return true;
    }

    public boolean isAutomationUpgraded() { return automationUpgraded; }

    public boolean installSmartFarmingUpgrade() {
        if (!automationUpgraded || smartFarmingUpgraded) return false;
        smartFarmingUpgraded = true;
        autoPlant = true;
        setChanged();
        refresh();
        return true;
    }

    public boolean isSmartFarmingUpgraded() { return smartFarmingUpgraded; }
    public Container seedBank() { return seedBank; }

    public boolean seedSlotEnabled(int slot) {
        return slot >= 0 && slot < SEED_BANK_SIZE && (disabledSeedSlots & (1 << slot)) == 0;
    }

    public void toggleSeedSlot(int slot) {
        if (!smartFarmingUpgraded || slot < 0 || slot >= SEED_BANK_SIZE) return;
        disabledSeedSlots ^= (1 << slot);
        setChanged();
    }

    public void cycleSeedPriority(int slot) {
        if (!smartFarmingUpgraded || slot < 0 || slot >= SEED_BANK_SIZE) return;
        seedPriorities[slot] = (seedPriorities[slot] + 1) % 3;
        setChanged();
    }

    public void cycleSeedReserve() {
        if (!smartFarmingUpgraded) return;
        reservedPerSlot = switch (reservedPerSlot) {
            case 0 -> 1;
            case 1 -> 4;
            case 4 -> 8;
            case 8 -> 16;
            case 16 -> 32;
            case 32 -> 64;
            default -> 0;
        };
        setChanged();
    }

    public void toggleBestGenetics() {
        if (!smartFarmingUpgraded) return;
        preferStrongestSeeds = !preferStrongestSeeds;
        setChanged();
    }

    public void toggleFlowerPlanting() {
        if (!smartFarmingUpgraded) return;
        plantFlowerSeeds = !plantFlowerSeeds;
        setChanged();
    }

    public void toggleResourcePlanting() {
        if (!smartFarmingUpgraded) return;
        plantResourceSeeds = !plantResourceSeeds;
        setChanged();
    }

    public void toggleAutoBreed() {
        if (!smartFarmingUpgraded) return;
        autoBreed = !autoBreed;
        lastBreedResult = 0;
        setChanged();
    }

    public void toggleAutoPlant() {
        if (!smartFarmingUpgraded) return;
        autoPlant = !autoPlant;
        setChanged();
    }

    public void openSeedBank(ServerPlayer player) {
        if (!smartFarmingUpgraded || !stillValid(player)) return;
        player.openMenu(new SimpleMenuProvider((id, inv, holder) ->
                new BotanicalGreenhouseSeedBankMenu(id, inv, this),
                Component.literal("Smart Farming Seed Bank")));
    }

    private int availableSeedCount() {
        int count = 0;
        for (int i = 0; i < seedBank.getContainerSize(); i++) {
            ItemStack seed = seedBank.getItem(i);
            if (seed.getItem() instanceof SpeciesSeedItem) count += seed.getCount();
        }
        return Math.min(30000, count);
    }

    public void toggleAutomation() {
        if (!automationUpgraded) return;
        autoHarvest = !autoHarvest;
        setChanged();
    }

    public void toggleReplant() {
        if (!automationUpgraded) return;
        autoReplant = !autoReplant;
        setChanged();
    }

    public void openStorage(ServerPlayer player) {
        if (!automationUpgraded) return;
        player.openMenu(new SimpleMenuProvider((id, inv, holder) ->
                new BotanicalGreenhouseStorageMenu(id, inv, this),
                Component.literal("Automated Greenhouse Storage")));
    }

    /** Opens independent export settings, leaving the existing inventory and seed bank intact. */
    public void openHarvestManagement(ServerPlayer player) {
        if (!automationUpgraded || !stillValid(player)) return;
        player.openMenu(new SimpleMenuProvider((id, inv, holder) ->
                new BotanicalGreenhouseHarvestMenu(id, inv, this),
                Component.literal("Greenhouse Harvest Management")));
    }

    public void toggleExport() {
        if (!automationUpgraded) return;
        autoExport = !autoExport;
        exportTargetRejected = false;
        setChanged();
    }

    public void toggleExportMode() {
        if (!automationUpgraded) return;
        exportNearFullOnly = !exportNearFullOnly;
        setChanged();
    }

    public void cycleExportReserve() {
        if (!automationUpgraded) return;
        exportReserve = switch (exportReserve) {
            case 0 -> 16; case 16 -> 32; case 32 -> 64; default -> 0;
        };
        setChanged();
    }

    public void cycleExportSide() {
        if (!automationUpgraded) return;
        exportSide = (exportSide + 1) % 6;
        exportTargetConnected = exportTargetRejected = false;
        setChanged();
    }

    public void toggleExportCategory(int category) {
        if (!automationUpgraded) return;
        switch (category) {
            case 0 -> exportResourceItems = !exportResourceItems;
            case 1 -> exportBotanicalItems = !exportBotanicalItems;
            case 2 -> exportSeeds = !exportSeeds;
            default -> { return; }
        }
        setChanged();
    }

    public void cycleExportPriority(int category) {
        if (!automationUpgraded || category < 0 || category > 2) return;
        exportPriorities[category] = (exportPriorities[category] + 1) % 3;
        setChanged();
    }

    private int occupiedStorageSlots() {
        int result = 0;
        for (ItemStack stack : storage) if (!stack.isEmpty()) result++;
        return result;
    }

    @Override public void onLoad() {
        super.onLoad();
        refresh();
    }

    /** Perform structure checks periodically; a removed glass pane shuts off irrigation. */
    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  BotanicalGreenhouseBlockEntity greenhouse) {
        if (!(level instanceof ServerLevel server)) return;
        greenhouse.tickCount++;
        if (greenhouse.tickCount % 40 == 0) greenhouse.refresh();
        // Transferring stored output does not require consuming greenhouse irrigation water.
        if (greenhouse.tickCount % 20 == 0 && greenhouse.automationUpgraded && greenhouse.autoExport)
            greenhouse.exportToNeighbor(server);
        if (!greenhouse.formed || greenhouse.water == 0) return;
        if (greenhouse.tickCount % 20 == 0) {
            // Heat consumes stored solid fuel; the ventilation fan is passive.
            if (greenhouse.heaterEnabled && greenhouse.heaterFuelSeconds > 0) {
                greenhouse.heaterFuelSeconds--;
            }
            greenhouse.water--; // ~16m40s per water bucket, up to 66m40s total
            greenhouse.setChanged();
            if (greenhouse.water == 0 || (greenhouse.heaterEnabled && greenhouse.heaterFuelSeconds == 0)) {
                greenhouse.refresh();
                if (greenhouse.water == 0) return;
            }
        }
        if (greenhouse.tickCount % 100 == 0) greenhouse.helpCrops(server);
        if (greenhouse.tickCount % 60 == 0 && greenhouse.smartFarmingUpgraded && greenhouse.autoPlant) {
            greenhouse.plantFromSeedBank(server);
        }
        // Independently cross a pair of allowed seed-bank stacks every 15 seconds.
        if (greenhouse.tickCount % 300 == 0 && greenhouse.smartFarmingUpgraded && greenhouse.autoBreed) {
            greenhouse.breedFromSeedBank(server);
        }
        if (greenhouse.tickCount % 80 == 0 && greenhouse.automationUpgraded && greenhouse.autoHarvest) {
            greenhouse.harvestAndReplant(server);
        }
        // Gradually migrate older 0.30.0 stored seeds into the bank, preserving
        // every genetic profile even when all nine bank slots are occupied.
        if (greenhouse.tickCount % 100 == 0 && greenhouse.smartFarmingUpgraded) {
            greenhouse.recycleStoredSeeds();
        }
    }

    private void helpCrops(ServerLevel level) {
        Direction facing = getBlockState().getValue(BotanicalGreenhouseBlock.FACING);
        for (int across = -2; across <= 2; across++) for (int depth = 1; depth <= 5; depth++) {
            BlockPos plantPos = BotanicalGreenhouseLayout.at(worldPosition, facing, across, depth, 0);
            if (!level.hasChunkAt(plantPos) || level.getRawBrightness(plantPos, 0) < 9
                    || level.random.nextInt(100) >= growthBonusChance) continue;
            BlockState state = level.getBlockState(plantPos);
            if (state.getBlock() instanceof AgriCropBlock crop
                    && state.getValue(CropBlock.AGE) < crop.getMaxAge()) {
                // Upgraded soil speeds greenhouse-assisted growth too, not just random ticks.
                int tier = com.agrievolution.farming.SoilResearch.soilTier(level.getBlockState(plantPos.below()));
                int steps = 1 + tier; // Vanilla +1; upgraded +2/+3/+4 per successful assist.
                level.setBlock(plantPos, state.setValue(CropBlock.AGE,
                        Math.min(crop.getMaxAge(), state.getValue(CropBlock.AGE) + steps)), 2);
            }
        }
    }

    /** Harvest one mature crop into bank + output, and consume the strongest available
     * seed to plant its plot again. A failed capacity simulation never destroys the crop. */
    private void harvestAndReplant(ServerLevel server) {
        Direction facing = getBlockState().getValue(BotanicalGreenhouseBlock.FACING);
        storageBlocked = false;
        for (int depth = 1; depth <= 5; depth++) for (int across = -2; across <= 2; across++) {
            BlockPos cropPos = BotanicalGreenhouseLayout.at(worldPosition, facing, across, depth, 0);
            if (!server.hasChunkAt(cropPos) || !server.hasChunkAt(cropPos.below())) continue;
            BlockState planted = server.getBlockState(cropPos);
            if (!(planted.getBlock() instanceof AgriCropBlock crop)
                    || planted.getValue(CropBlock.AGE) < crop.getMaxAge()) continue;
            if (!(server.getBlockEntity(cropPos) instanceof AECropBlockEntity entity)) continue;
            CropGenome genes = entity.genome();
            RareTrait rare = entity.rareTrait();
            HybridLineageStorage.Lineage lineage = entity.lineage();
            int supportTier = CropSupportItem.tier(planted);
            int supportTicks = entity.supportTicks();
            ItemStack recoveredSupport = entity.recoverSupport();
            boolean supported = supportTier>0;

            // Tier 3 returns a primary seed to the bank, then consumes a seed
            // for replanting. Legacy Tier 2 lacks a bank and consumes its own
            // harvest seed directly, avoiding a duplicated free seed.
            ItemStack seed = new ItemStack(AEItems.seedFor(crop.species()));
            GenomeStorage.putOnSeed(seed, genes);
            RareTraitStorage.putOnSeed(seed, rare);
            if (lineage != null) HybridLineageStorage.putOnSeed(seed, lineage);
            com.agrievolution.genetics.SeedVariety.apply(seed, entity.variety());
            List<ItemStack> drops = new ArrayList<>();
            if (!autoReplant || smartFarmingUpgraded) drops.add(seed);
            if (server.random.nextInt(100) < ExtraSeedRules.chancePercent(
                    genes.yield(), rare == RareTrait.ABUNDANT)) drops.add(seed.copy());

            int productAmount = 1;
            if (genes.yield() > 25 && server.random.nextInt(100) < genes.yield())
                productAmount += 1 + genes.yield() / 50;
            if (rare == RareTrait.ABUNDANT) productAmount++;
            drops.add(new ItemStack(harvestProduce(crop.species()), productAmount));
            if (!autoReplant && supported) drops.add(recoveredSupport.copy());

            // Simulate BOTH inventories before changing the world. The new seed can
            // displace a weaker same-species stack; displaced seeds are kept in storage.
            NonNullList<ItemStack> futureBank = copySlots(seedBank.getItems());
            NonNullList<ItemStack> futureStorage = copySlots(storage);
            if (!distributeDrops(drops, futureBank, futureStorage)) {
                storageBlocked = true;
                return;
            }
            server.setBlock(cropPos, Blocks.AIR.defaultBlockState(), 3);
            com.agrievolution.farming.CompanionPlanting.harvested(server, cropPos, crop.species());
            installInventories(futureBank, futureStorage);
            if (autoReplant) {
                if (!smartFarmingUpgraded) {
                    // Tier 2 keeps its existing harvest/replant ability even before
                    // a Seed Bank is unlocked. No free primary seed is stored.
                    if (server.setBlock(cropPos, crop.defaultBlockState()
                            .setValue(AgriCropBlock.SUPPORTED, supportTier>0).setValue(AgriCropBlock.SUPPORT_TIER,supportTier), 3)) {
                        if (server.getBlockEntity(cropPos) instanceof AECropBlockEntity replanted) {
                            replanted.setSupportTicks(supportTicks);
                            replanted.setGenome(genes);
                            replanted.setRareTrait(rare);
                            replanted.setLineage(lineage);
                            replanted.setVariety(entity.variety());
                        }
                        totalReplants++;
                    }
                } else {
                    // Select AFTER returning seeds, including improved harvested genes.
                    int slot = chooseSeedBankSlot();
                    if (slot >= 0 && placeBankSeed(server, cropPos, slot, supportTier)) {
                        if(server.getBlockEntity(cropPos) instanceof AECropBlockEntity replanted)replanted.setSupportTicks(supportTicks);
                        totalReplants++;
                        seedSlotCursor = (slot + 1) % SEED_BANK_SIZE;
                        recordPlantCycle(SeedBankMonitoring.PLANTED, slot + 1, monitorPlantedSpecies);
                    } else if (supported) {
                        // No suitable seed: preserve the spent crop support as a drop.
                        net.minecraft.world.level.block.Block.popResource(server, cropPos,
                                recoveredSupport.copy());
                    }
                }
            }
            totalHarvests++;
            setChanged();
            updateDashboardStats(server, facing);
            return;
        }
    }

    /** Tier-3 greenhouse cross using the same baseline inheritance as a breeding station.
     *  The seed-bank configuration (disabled slots, reserves, H/N/L, best genes)
     *  is honoured, but the flower/resource *planting* filters do not stop breeding.
     *  More advanced Tier-3 lab-only recipes remain exclusive to that laboratory.
     *  Storage is checked before consuming either parent, so no offspring is lost.
     */
    private void breedFromSeedBank(ServerLevel server) {
        List<SeedBankSelection.Candidate> eligible = new ArrayList<>();
        for (int slot = 0; slot < SEED_BANK_SIZE; slot++) {
            ItemStack stack = seedBank.getItem(slot);
            if (!seedSlotEnabled(slot) || !(stack.getItem() instanceof SpeciesSeedItem)
                    || stack.getCount() <= reservedPerSlot) continue;
            eligible.add(new SeedBankSelection.Candidate(slot, seedPriorities[slot],
                    GenomeStorage.fromSeed(stack).totalScore()));
        }
        if (eligible.size() < 2) {
            lastBreedResult = 2;
            setChanged();
            return;
        }
        int first = SeedBankSelection.choose(eligible, breedSlotCursor, SEED_BANK_SIZE, preferStrongestSeeds);
        List<SeedBankSelection.Candidate> others = new ArrayList<>();
        for (SeedBankSelection.Candidate candidate : eligible)
            if (candidate.slot() != first) others.add(candidate);
        int second = SeedBankSelection.choose(others, (first + 1) % SEED_BANK_SIZE,
                SEED_BANK_SIZE, preferStrongestSeeds);
        if (first < 0 || second < 0 || first == second) return;
        ItemStack a = seedBank.getItem(first);
        ItemStack b = seedBank.getItem(second);
        CropSpecies kindA = ((SpeciesSeedItem) a.getItem()).species();
        CropSpecies kindB = ((SpeciesSeedItem) b.getItem()).species();
        CropGenome inherited = GenomeBreeder.cross(GenomeStorage.fromSeed(a),
                GenomeStorage.fromSeed(b), server.random.nextLong());
        CropSpecies offspringKind = SpeciesBreeder.cross(kindA, kindB, server.random.nextLong());
        ItemStack offspring = new ItemStack(AEItems.seedFor(offspringKind));
        GenomeStorage.putOnSeed(offspring, inherited);
        RareTraitStorage.putOnSeed(offspring,
                RareTrait.inherit(RareTraitStorage.fromSeed(a), RareTraitStorage.fromSeed(b),
                        new java.util.Random(server.random.nextLong())));
        com.agrievolution.genetics.SeedVariety.inherit(offspring, a, b);
        if (kindA != kindB) {
            HybridLineageStorage.putOnSeed(offspring, new HybridLineageStorage.Lineage(kindA, kindB));
        } else {
            // Do not discard lineage of an already-hybrid parent with the same species.
            HybridLineageStorage.Lineage lineage = HybridLineageStorage.fromSeed(a);
            if (lineage == null) lineage = HybridLineageStorage.fromSeed(b);
            if (lineage != null) HybridLineageStorage.putOnSeed(offspring, lineage);
        }
        // Consume parents only in simulated inventories. A genetically stronger
        // offspring can replace a weaker seed from its own species in the bank.
        NonNullList<ItemStack> futureBank = copySlots(seedBank.getItems());
        NonNullList<ItemStack> futureStorage = copySlots(storage);
        futureBank.get(first).shrink(1);
        futureBank.get(second).shrink(1);
        if (futureBank.get(first).isEmpty()) futureBank.set(first, ItemStack.EMPTY);
        if (futureBank.get(second).isEmpty()) futureBank.set(second, ItemStack.EMPTY);
        if (!distributeDrops(List.of(offspring), futureBank, futureStorage)) {
            lastBreedResult = 3;
            setChanged();
            return;
        }
        installInventories(futureBank, futureStorage);
        breedSlotCursor = (second + 1) % SEED_BANK_SIZE;
        if (totalAutoBred >= 30000) totalAutoBred = 0; // vanilla 16-bit menu sync
        totalAutoBred++;
        lastBreedResult = 1;
        seedBank.setChanged();
        setChanged();
    }

    /** Plant one valid empty farmland plot per cycle. A seed is only consumed after successful
     *  block placement and its genome is copied without modification. Other mods' crops untouched. */
    /** Current server-side exclusions: one occupied slot contributes to only one category. */
    private SeedBankMonitoring.Summary seedBankSummary() {
        List<SeedBankMonitoring.Slot> slots = new ArrayList<>(SEED_BANK_SIZE);
        for (int i = 0; i < SEED_BANK_SIZE; i++) {
            ItemStack stack = seedBank.getItem(i);
            if (!(stack.getItem() instanceof SpeciesSeedItem seedType) || stack.isEmpty()) {
                slots.add(new SeedBankMonitoring.Slot(false, true, true, false));
                continue;
            }
            boolean allowed = (seedType.species().isResource() ? plantResourceSeeds : plantFlowerSeeds) && plantingAllowed(i,stack);
            slots.add(new SeedBankMonitoring.Slot(true, seedSlotEnabled(i), allowed,
                    stack.getCount() > reservedPerSlot));
        }
        return SeedBankMonitoring.summarize(slots);
    }

    /** Records completed automation cycles; the rolling window prevents integer overflow
     * and 16-bit ContainerData truncation during prolonged server play. */
    private void recordPlantCycle(int result, int selectedSlot, int speciesOrdinal) {
        if (monitorAttempts >= 30000) {
            monitorAttempts /= 2;
            monitorSuccessful /= 2;
        }
        monitorAttempts++;
        if (result == SeedBankMonitoring.PLANTED) monitorSuccessful++;
        monitorLastResult = result;
        monitorLastSlot = selectedSlot;
        monitorLastSpecies = speciesOrdinal;
        setChanged();
    }

    private int chooseSeedBankSlot() {
        List<SeedBankSelection.Candidate> eligible = new ArrayList<>();
        for (int slot = 0; slot < SEED_BANK_SIZE; slot++) {
            ItemStack stack = seedBank.getItem(slot);
            if (!seedSlotEnabled(slot) || !(stack.getItem() instanceof SpeciesSeedItem seedType)
                    || stack.getCount() <= reservedPerSlot) continue;
            boolean resource = seedType.species().isResource();
            if (resource ? !plantResourceSeeds : !plantFlowerSeeds) continue;
            if (!plantingAllowed(slot,stack)) continue;
            eligible.add(new SeedBankSelection.Candidate(slot, seedPriorities[slot],
                    GenomeStorage.fromSeed(stack).totalScore()));
        }
        // The automation always uses the highest genetic score among eligible
        // seeds; player H/N/L priority and rotating cursor resolve equal scores.
        return SeedBankSelection.chooseStrongest(eligible, seedSlotCursor, SEED_BANK_SIZE);
    }

    private void plantFromSeedBank(ServerLevel server) {
        int slot = chooseSeedBankSlot();
        if (slot < 0) {
            recordPlantCycle(SeedBankMonitoring.noEligibleReason(seedBankSummary()), 0, 0);
            return;
        }
        ItemStack seed = seedBank.getItem(slot);
        if (!(seed.getItem() instanceof SpeciesSeedItem seedType)) {
            recordPlantCycle(SeedBankMonitoring.PLACEMENT_FAILED, slot + 1, 0);
            return;
        }
        int speciesOrdinal = seedType.species().ordinal() + 1;
        Direction facing = getBlockState().getValue(BotanicalGreenhouseBlock.FACING);
        boolean foundEmptyFarmland = false;
        for (int n = 0; n < 25; n++) {
            int plot = (plotCursor + n) % 25;
            BlockPos cropPos = BotanicalGreenhouseLayout.at(worldPosition, facing,
                    (plot % 5) - 2, (plot / 5) + 1, 0);
            if (!server.hasChunkAt(cropPos) || !server.hasChunkAt(cropPos.below())) continue;
            if (!server.getBlockState(cropPos).isAir()
                    || !com.agrievolution.farming.SoilResearch.isSoil(server.getBlockState(cropPos.below()))) continue;
            foundEmptyFarmland = true;
            AgriCropBlock block = switch (seedType.species()) {
                    case WILD -> AEBlocks.AGRI_CROP.get();
                    case CRIMSON_CLOVER -> AEBlocks.CRIMSON_CLOVER_CROP.get();
                    case AZURE_FLAX -> AEBlocks.AZURE_FLAX_CROP.get();
                    case VIOLET_STARFLOWER -> AEBlocks.VIOLET_STARFLOWER_CROP.get();
                    case AMBER_MARIGOLD -> AEBlocks.AMBER_MARIGOLD_CROP.get();
                    case SILVER_THISTLE -> AEBlocks.SILVER_THISTLE_CROP.get();
                    case SUNFIRE_LILY -> AEBlocks.SUNFIRE_LILY_CROP.get();
                    case MOON_ORCHID -> AEBlocks.MOON_ORCHID_CROP.get();
                    case ECLIPSE_LOTUS -> AEBlocks.ECLIPSE_LOTUS_CROP.get();
                    case TWILIGHT_SAGE -> AEBlocks.TWILIGHT_SAGE_CROP.get();
                    case DIRT -> AEBlocks.DIRT_CROP.get();
                    case WOOD -> AEBlocks.WOOD_CROP.get();
                    case STONE -> AEBlocks.STONE_CROP.get();
                    case COAL -> AEBlocks.COAL_CROP.get();
                    case COPPER -> AEBlocks.COPPER_CROP.get();
                    case IRON -> AEBlocks.IRON_CROP.get();
                    case REDSTONE -> AEBlocks.REDSTONE_CROP.get();
                    case GOLD -> AEBlocks.GOLD_CROP.get();
                    case LAPIS -> AEBlocks.LAPIS_CROP.get();
                    case DIAMOND -> AEBlocks.DIAMOND_CROP.get();
                    case EMERALD -> AEBlocks.EMERALD_CROP.get();
                    case QUARTZ -> AEBlocks.QUARTZ_CROP.get();
                    case GRAVEL -> AEBlocks.GRAVEL_CROP.get();
                    case SAND -> AEBlocks.SAND_CROP.get();
                    case CLAY -> AEBlocks.CLAY_CROP.get();
                    case FLINT -> AEBlocks.FLINT_CROP.get();
                    case GLOWSTONE -> AEBlocks.GLOWSTONE_CROP.get();
                    case OBSIDIAN -> AEBlocks.OBSIDIAN_CROP.get();
                    case NETHERRACK -> AEBlocks.NETHERRACK_CROP.get();
                    case SOUL_SAND -> AEBlocks.SOUL_SAND_CROP.get();
                    case MAGMA_CREAM -> AEBlocks.MAGMA_CREAM_CROP.get();
                    case NETHER_WART -> AEBlocks.NETHER_WART_CROP.get();
                    case BLAZE_ROD -> AEBlocks.BLAZE_ROD_CROP.get();
                    case END_STONE -> AEBlocks.END_STONE_CROP.get();
                    case CHORUS_FRUIT -> AEBlocks.CHORUS_FRUIT_CROP.get();
                    case ENDER_PEARL -> AEBlocks.ENDER_PEARL_CROP.get();
                    case SHULKER_SHELL -> AEBlocks.SHULKER_SHELL_CROP.get();
                    case ANCIENT_DEBRIS -> AEBlocks.ANCIENT_DEBRIS_CROP.get();
                    case NETHERITE -> AEBlocks.NETHERITE_CROP.get();
                    case OAK_SAPLING -> AEBlocks.OAK_SAPLING_CROP.get();
                    case SPRUCE_SAPLING -> AEBlocks.SPRUCE_SAPLING_CROP.get();
                    case BIRCH_SAPLING -> AEBlocks.BIRCH_SAPLING_CROP.get();
                    case DARK_OAK_SAPLING -> AEBlocks.DARK_OAK_SAPLING_CROP.get();
                    case TUBE_CORAL -> AEBlocks.TUBE_CORAL_CROP.get();
                    case BRAIN_CORAL -> AEBlocks.BRAIN_CORAL_CROP.get();
                    case BUBBLE_CORAL -> AEBlocks.BUBBLE_CORAL_CROP.get();
                    case FIRE_CORAL -> AEBlocks.FIRE_CORAL_CROP.get();
                    case COAST_ARMOR_TRIM_SMITHING_TEMPLATE -> AEBlocks.COAST_ARMOR_TRIM_SMITHING_TEMPLATE_CROP.get();
                    case TIDE_ARMOR_TRIM_SMITHING_TEMPLATE -> AEBlocks.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE_CROP.get();
                    case SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE -> AEBlocks.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE_CROP.get();
                    case SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE -> AEBlocks.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE_CROP.get();
                    case SUGAR_CANE -> AEBlocks.SUGAR_CANE_CROP.get();
                    case SWEET_BERRIES -> AEBlocks.SWEET_BERRIES_CROP.get();
                    case SEAGRASS -> AEBlocks.SEAGRASS_CROP.get();
                    case HANGING_ROOTS -> AEBlocks.HANGING_ROOTS_CROP.get();
                    case TORCHFLOWER -> AEBlocks.TORCHFLOWER_CROP.get();
                    case PITCHER_PLANT -> AEBlocks.PITCHER_PLANT_CROP.get();
                    case SPORE_BLOSSOM -> AEBlocks.SPORE_BLOSSOM_CROP.get();
                    case BIG_DRIPLEAF -> AEBlocks.BIG_DRIPLEAF_CROP.get();
                    case TRIAL_KEY -> AEBlocks.TRIAL_KEY_CROP.get();
                    case OMINOUS_TRIAL_KEY -> AEBlocks.OMINOUS_TRIAL_KEY_CROP.get();
                    case NETHERITE_UPGRADE_SMITHING_TEMPLATE -> AEBlocks.NETHERITE_UPGRADE_SMITHING_TEMPLATE_CROP.get();
                    case DISC_FRAGMENT_5 -> AEBlocks.DISC_FRAGMENT_5_CROP.get();
                    case GRASS_BLOCK -> AEBlocks.GRASS_BLOCK_CROP.get();
                    case PODZOL -> AEBlocks.PODZOL_CROP.get();
                    case MYCELIUM -> AEBlocks.MYCELIUM_CROP.get();
                    case DEAD_BUSH -> AEBlocks.DEAD_BUSH_CROP.get();
                    
                    
                    case MUD_BRICKS -> AEBlocks.MUD_BRICKS_CROP.get();
                    case DEEPSLATE_BRICKS -> AEBlocks.DEEPSLATE_BRICKS_CROP.get();
                    case CALIBRATED_SCULK_SENSOR -> AEBlocks.CALIBRATED_SCULK_SENSOR_CROP.get();
                    case END_CRYSTAL -> AEBlocks.END_CRYSTAL_CROP.get();
                    case TOTEM_OF_UNDYING -> AEBlocks.TOTEM_OF_UNDYING_CROP.get();
                    case ENCHANTED_GOLDEN_APPLE -> AEBlocks.ENCHANTED_GOLDEN_APPLE_CROP.get();
                    case JUNGLE_LOG -> AEBlocks.JUNGLE_LOG_CROP.get();
                    case COARSE_DIRT -> AEBlocks.COARSE_DIRT_CROP.get();
                    case ROOTED_DIRT -> AEBlocks.ROOTED_DIRT_CROP.get();
                    case RED_SANDSTONE -> AEBlocks.RED_SANDSTONE_CROP.get();
                    case PRISMARINE_BRICKS -> AEBlocks.PRISMARINE_BRICKS_CROP.get();
                    
                    case WARPED_WART_BLOCK -> AEBlocks.WARPED_WART_BLOCK_CROP.get();
                    case RED_NETHER_BRICKS -> AEBlocks.RED_NETHER_BRICKS_CROP.get();
                    case SCULK_SENSOR -> AEBlocks.SCULK_SENSOR_CROP.get();
                    case SCULK_SHRIEKER -> AEBlocks.SCULK_SHRIEKER_CROP.get();
                    case CONDUIT -> AEBlocks.CONDUIT_CROP.get();
                    case RESPAWN_ANCHOR -> AEBlocks.RESPAWN_ANCHOR_CROP.get();
                    case STICK -> AEBlocks.STICK_CROP.get();
                    case DRIED_KELP -> AEBlocks.DRIED_KELP_CROP.get();
                    case GRAY_DYE -> AEBlocks.GRAY_DYE_CROP.get();
                    case LIGHT_GRAY_DYE -> AEBlocks.LIGHT_GRAY_DYE_CROP.get();
                    
                    
                    
                    
                    case SKELETON_SKULL -> AEBlocks.SKELETON_SKULL_CROP.get();
                    case WITHER_SKELETON_SKULL -> AEBlocks.WITHER_SKELETON_SKULL_CROP.get();
                    case CREEPER_HEAD -> AEBlocks.CREEPER_HEAD_CROP.get();
                    case ZOMBIE_HEAD -> AEBlocks.ZOMBIE_HEAD_CROP.get();
                    case WHITE_DYE -> AEBlocks.WHITE_DYE_CROP.get();
                    case BLACK_DYE -> AEBlocks.BLACK_DYE_CROP.get();
                    case ORANGE_DYE -> AEBlocks.ORANGE_DYE_CROP.get();
                    case PINK_DYE -> AEBlocks.PINK_DYE_CROP.get();
                    case PURPLE_DYE -> AEBlocks.PURPLE_DYE_CROP.get();
                    case MAGENTA_DYE -> AEBlocks.MAGENTA_DYE_CROP.get();
                    case LIGHT_BLUE_DYE -> AEBlocks.LIGHT_BLUE_DYE_CROP.get();
                    case LIME_DYE -> AEBlocks.LIME_DYE_CROP.get();
                    case HEART_OF_THE_SEA -> AEBlocks.HEART_OF_THE_SEA_CROP.get();
                    case NETHER_STAR -> AEBlocks.NETHER_STAR_CROP.get();
                    case DRAGON_HEAD -> AEBlocks.DRAGON_HEAD_CROP.get();
                    case HEAVY_CORE -> AEBlocks.HEAVY_CORE_CROP.get();
                    case GREEN_DYE -> AEBlocks.GREEN_DYE_CROP.get();
                    case RED_DYE -> AEBlocks.RED_DYE_CROP.get();
                    case YELLOW_DYE -> AEBlocks.YELLOW_DYE_CROP.get();
                    case BROWN_DYE -> AEBlocks.BROWN_DYE_CROP.get();
                    case HONEY_BOTTLE -> AEBlocks.HONEY_BOTTLE_CROP.get();
                    case GLOW_BERRIES -> AEBlocks.GLOW_BERRIES_CROP.get();
                    case BLUE_DYE -> AEBlocks.BLUE_DYE_CROP.get();
                    case CYAN_DYE -> AEBlocks.CYAN_DYE_CROP.get();
                    case FIRE_CHARGE -> AEBlocks.FIRE_CHARGE_CROP.get();
                    case WIND_CHARGE -> AEBlocks.WIND_CHARGE_CROP.get();
                    case DRAGON_BREATH -> AEBlocks.DRAGON_BREATH_CROP.get();
                    case EXPERIENCE_BOTTLE -> AEBlocks.EXPERIENCE_BOTTLE_CROP.get();
                    case SUGAR -> AEBlocks.SUGAR_CROP.get();
                    case COCOA_BEANS -> AEBlocks.COCOA_BEANS_CROP.get();
                    case EGG -> AEBlocks.EGG_CROP.get();
                    case ROTTEN_FLESH -> AEBlocks.ROTTEN_FLESH_CROP.get();
                    case RABBIT_HIDE -> AEBlocks.RABBIT_HIDE_CROP.get();
                    case RABBIT_FOOT -> AEBlocks.RABBIT_FOOT_CROP.get();
                    case ARMADILLO_SCUTE -> AEBlocks.ARMADILLO_SCUTE_CROP.get();
                    case FERMENTED_SPIDER_EYE -> AEBlocks.FERMENTED_SPIDER_EYE_CROP.get();
                    case WITHER_ROSE -> AEBlocks.WITHER_ROSE_CROP.get();
                    case CHORUS_FLOWER -> AEBlocks.CHORUS_FLOWER_CROP.get();
                    case CRIMSON_FUNGUS -> AEBlocks.CRIMSON_FUNGUS_CROP.get();
                    case WARPED_FUNGUS -> AEBlocks.WARPED_FUNGUS_CROP.get();
                    case SPRUCE_LOG -> AEBlocks.SPRUCE_LOG_CROP.get();
                    case BIRCH_LOG -> AEBlocks.BIRCH_LOG_CROP.get();
                    case ACACIA_LOG -> AEBlocks.ACACIA_LOG_CROP.get();
                    case DARK_OAK_LOG -> AEBlocks.DARK_OAK_LOG_CROP.get();
                    case MANGROVE_LOG -> AEBlocks.MANGROVE_LOG_CROP.get();
                    case CHERRY_LOG -> AEBlocks.CHERRY_LOG_CROP.get();
                    case SEA_PICKLE -> AEBlocks.SEA_PICKLE_CROP.get();
                    case GLOW_LICHEN -> AEBlocks.GLOW_LICHEN_CROP.get();
                    case CRIMSON_STEM -> AEBlocks.CRIMSON_STEM_CROP.get();
                    case WARPED_STEM -> AEBlocks.WARPED_STEM_CROP.get();
                    case VERDANT_FROGLIGHT -> AEBlocks.VERDANT_FROGLIGHT_CROP.get();
                    case PEARLESCENT_FROGLIGHT -> AEBlocks.PEARLESCENT_FROGLIGHT_CROP.get();
                    case CHARCOAL -> AEBlocks.CHARCOAL_CROP.get();
                    case GLASS -> AEBlocks.GLASS_CROP.get();
                    case BROWN_MUSHROOM -> AEBlocks.BROWN_MUSHROOM_CROP.get();
                    case RED_MUSHROOM -> AEBlocks.RED_MUSHROOM_CROP.get();
                    
                    
                    case MOSSY_COBBLESTONE -> AEBlocks.MOSSY_COBBLESTONE_CROP.get();
                    case LILY_PAD -> AEBlocks.LILY_PAD_CROP.get();
                    case DARK_PRISMARINE -> AEBlocks.DARK_PRISMARINE_CROP.get();
                    case GILDED_BLACKSTONE -> AEBlocks.GILDED_BLACKSTONE_CROP.get();
                    case SCULK_CATALYST -> AEBlocks.SCULK_CATALYST_CROP.get();
                    case OCHRE_FROGLIGHT -> AEBlocks.OCHRE_FROGLIGHT_CROP.get();
                    case RED_SAND -> AEBlocks.RED_SAND_CROP.get();
                    case SANDSTONE -> AEBlocks.SANDSTONE_CROP.get();
                    case CACTUS -> AEBlocks.CACTUS_CROP.get();
                    case VINE -> AEBlocks.VINE_CROP.get();
                    case PACKED_MUD -> AEBlocks.PACKED_MUD_CROP.get();
                    case BRICK -> AEBlocks.BRICK_CROP.get();
                    case ICE -> AEBlocks.ICE_CROP.get();
                    case PACKED_ICE -> AEBlocks.PACKED_ICE_CROP.get();
                    case BLUE_ICE -> AEBlocks.BLUE_ICE_CROP.get();
                    
                    case NETHER_BRICK -> AEBlocks.NETHER_BRICK_CROP.get();
                    case ENDER_EYE -> AEBlocks.ENDER_EYE_CROP.get();
                    case GRANITE -> AEBlocks.GRANITE_CROP.get();
                    case DIORITE -> AEBlocks.DIORITE_CROP.get();
                    case ANDESITE -> AEBlocks.ANDESITE_CROP.get();
                    case MUD -> AEBlocks.MUD_CROP.get();
                    case TUFF -> AEBlocks.TUFF_CROP.get();
                    case DRIPSTONE -> AEBlocks.DRIPSTONE_CROP.get();
                    case DEEPSLATE -> AEBlocks.DEEPSLATE_CROP.get();
                    case SOUL_SOIL -> AEBlocks.SOUL_SOIL_CROP.get();
                    case SEA_LANTERN -> AEBlocks.SEA_LANTERN_CROP.get();
                    case SHROOMLIGHT -> AEBlocks.SHROOMLIGHT_CROP.get();
                    case END_ROD -> AEBlocks.END_ROD_CROP.get();
                    case SCULK -> AEBlocks.SCULK_CROP.get();
                    case PAPER -> AEBlocks.PAPER_CROP.get();
                    case KELP -> AEBlocks.KELP_CROP.get();
                    case BAMBOO -> AEBlocks.BAMBOO_CROP.get();
                    case BLACKSTONE -> AEBlocks.BLACKSTONE_CROP.get();
                    case BASALT -> AEBlocks.BASALT_CROP.get();
                    case PURPUR -> AEBlocks.PURPUR_CROP.get();
                    case TURTLE_SCUTE -> AEBlocks.TURTLE_SCUTE_CROP.get();
                    case SPONGE -> AEBlocks.SPONGE_CROP.get();
                    case SNOWBALL -> AEBlocks.SNOWBALL_CROP.get();
                    case MOSS -> AEBlocks.MOSS_CROP.get();
                    case CALCITE -> AEBlocks.CALCITE_CROP.get();
                    case TERRACOTTA -> AEBlocks.TERRACOTTA_CROP.get();
                    case SPIDER_EYE -> AEBlocks.SPIDER_EYE_CROP.get();
                    case GLOW_INK_SAC -> AEBlocks.GLOW_INK_SAC_CROP.get();
                    case CRYING_OBSIDIAN -> AEBlocks.CRYING_OBSIDIAN_CROP.get();
                    case NAUTILUS_SHELL -> AEBlocks.NAUTILUS_SHELL_CROP.get();
                    case FEATHER -> AEBlocks.FEATHER_CROP.get();
                    case INK_SAC -> AEBlocks.INK_SAC_CROP.get();
                    case HONEYCOMB -> AEBlocks.HONEYCOMB_CROP.get();
                    case SLIME_BALL -> AEBlocks.SLIME_BALL_CROP.get();
                    case GUNPOWDER -> AEBlocks.GUNPOWDER_CROP.get();
                    case PRISMARINE_CRYSTALS -> AEBlocks.PRISMARINE_CRYSTALS_CROP.get();
                    case PHANTOM_MEMBRANE -> AEBlocks.PHANTOM_MEMBRANE_CROP.get();
                    case ECHO_SHARD -> AEBlocks.ECHO_SHARD_CROP.get();
                    case STRING -> AEBlocks.STRING_CROP.get();
                    case WOOL -> AEBlocks.WOOL_CROP.get();
                    case LEATHER -> AEBlocks.LEATHER_CROP.get();
                    case BONE -> AEBlocks.BONE_CROP.get();
                    case AMETHYST -> AEBlocks.AMETHYST_CROP.get();
                    case PRISMARINE -> AEBlocks.PRISMARINE_CROP.get();
                    case GHAST_TEAR -> AEBlocks.GHAST_TEAR_CROP.get();
                    case BREEZE_ROD -> AEBlocks.BREEZE_ROD_CROP.get();
                    case BASIL -> AEBlocks.BASIL_CROP.get();
                    case MINT -> AEBlocks.MINT_CROP.get();
                    case ROSEMARY -> AEBlocks.ROSEMARY_CROP.get();
                    case THYME -> AEBlocks.THYME_CROP.get();
                    case PARSLEY -> AEBlocks.PARSLEY_CROP.get();
                    case CORIANDER -> AEBlocks.CORIANDER_CROP.get();
                    case GINGER -> AEBlocks.GINGER_CROP.get();
                    case TURMERIC -> AEBlocks.TURMERIC_CROP.get();
            case KALE -> AEBlocks.KALE_CROP.get();
            case PAK_CHOI -> AEBlocks.PAK_CHOI_CROP.get();
            case BRUSSELS_SPROUTS -> AEBlocks.BRUSSELS_SPROUTS_CROP.get();
            case SWEDE -> AEBlocks.SWEDE_CROP.get();
            case CELERIAC -> AEBlocks.CELERIAC_CROP.get();
            case LENTIL -> AEBlocks.LENTIL_CROP.get();
            case CHICKPEA -> AEBlocks.CHICKPEA_CROP.get();
            case BARLEY -> AEBlocks.BARLEY_CROP.get();
            case BUCKWHEAT -> AEBlocks.BUCKWHEAT_CROP.get();
            case RHUBARB -> AEBlocks.RHUBARB_CROP.get();
            case GRAPE -> AEBlocks.GRAPE_CROP.get();
            case CANTALOUPE -> AEBlocks.CANTALOUPE_CROP.get();
                    case SPINACH -> AEBlocks.SPINACH_CROP.get();
                    case CAULIFLOWER -> AEBlocks.CAULIFLOWER_CROP.get();
                    case ARTICHOKE -> AEBlocks.ARTICHOKE_CROP.get();
                    case CHILLI -> AEBlocks.CHILLI_CROP.get();
                    case SPRING_ONION -> AEBlocks.SPRING_ONION_CROP.get();
                    case FENNEL -> AEBlocks.FENNEL_CROP.get();
                    case DILL -> AEBlocks.DILL_CROP.get();
                    case PEANUT -> AEBlocks.PEANUT_CROP.get();
                    case SESAME -> AEBlocks.SESAME_CROP.get();
                    case OATS -> AEBlocks.OATS_CROP.get();
                    case GOOSEBERRY -> AEBlocks.GOOSEBERRY_CROP.get();
                    case BLACKCURRANT -> AEBlocks.BLACKCURRANT_CROP.get();
                    case PUMPKIN -> AEBlocks.PUMPKIN_CROP.get();
                    case BUTTERNUT_SQUASH -> AEBlocks.BUTTERNUT_SQUASH_CROP.get();
                    case COURGETTE -> AEBlocks.COURGETTE_CROP.get();
                    case TURNIP -> AEBlocks.TURNIP_CROP.get();
                    case PARSNIP -> AEBlocks.PARSNIP_CROP.get();
                    case LEEK -> AEBlocks.LEEK_CROP.get();
                    case CELERY -> AEBlocks.CELERY_CROP.get();
                    case ASPARAGUS -> AEBlocks.ASPARAGUS_CROP.get();
                    case BLUEBERRY -> AEBlocks.BLUEBERRY_CROP.get();
                    case RASPBERRY -> AEBlocks.RASPBERRY_CROP.get();
                    case BLACKBERRY -> AEBlocks.BLACKBERRY_CROP.get();
                    case CRANBERRY -> AEBlocks.CRANBERRY_CROP.get();
                    case CORN -> AEBlocks.CORN_CROP.get();
                    case PEAS -> AEBlocks.PEAS_CROP.get();
                    case BEANS -> AEBlocks.BEANS_CROP.get();
                    case BROCCOLI -> AEBlocks.BROCCOLI_CROP.get();
                    case CABBAGE -> AEBlocks.CABBAGE_CROP.get();
                    case RADISH -> AEBlocks.RADISH_CROP.get();
                    case AUBERGINE -> AEBlocks.AUBERGINE_CROP.get();
                    case SWEET_POTATO -> AEBlocks.SWEET_POTATO_CROP.get();
                    case TOMATO -> AEBlocks.TOMATO_CROP.get();
                    case LETTUCE -> AEBlocks.LETTUCE_CROP.get();
                    case CUCUMBER -> AEBlocks.CUCUMBER_CROP.get();
                    case ONION -> AEBlocks.ONION_CROP.get();
                    case GARLIC -> AEBlocks.GARLIC_CROP.get();
                    case PEPPER -> AEBlocks.PEPPER_CROP.get();
                    case STRAWBERRY -> AEBlocks.STRAWBERRY_CROP.get();
                    case RICE -> AEBlocks.RICE_CROP.get();
                };
                CropGenome genes = GenomeStorage.fromSeed(seed);
                if (!server.setBlock(cropPos, block.defaultBlockState(), 3)) continue;
                if (server.getBlockEntity(cropPos) instanceof AECropBlockEntity plant) {
                    plant.setGenome(genes);
                    plant.setRareTrait(RareTraitStorage.fromSeed(seed));
                    plant.setLineage(HybridLineageStorage.fromSeed(seed));
                    plant.setVariety(com.agrievolution.genetics.SeedVariety.name(seed));
                }
                // Capture what was actually placed while the original seed still holds its data.
                // Unsuccessful cycles never clear the last successful planting record.
                monitorPlantedSpecies = speciesOrdinal;
                monitorPlantedSlot = slot + 1;
                monitorPlantedGenome = genes;
                monitorPlantedRareTrait = RareTraitStorage.fromSeed(seed);
                seed.shrink(1);
                if (seed.isEmpty()) seedBank.setItem(slot, ItemStack.EMPTY);
                else seedBank.setChanged();
                plotCursor = (plot + 1) % 25;
                seedSlotCursor = (slot + 1) % SEED_BANK_SIZE;
                totalAutoPlanted++;
                recordPlantCycle(SeedBankMonitoring.PLANTED, slot + 1, speciesOrdinal);
                updateDashboardStats(server, facing);
                return;
        }
        recordPlantCycle(foundEmptyFarmland ? SeedBankMonitoring.PLACEMENT_FAILED :
                SeedBankMonitoring.NO_PLOTS, slot + 1, speciesOrdinal);
    }

    /** Shared placement for both empty plots and post-harvest replacements. */
    private boolean placeBankSeed(ServerLevel server, BlockPos cropPos, int slot, int supportTier) {
        ItemStack source = seedBank.getItem(slot);
        if (!(source.getItem() instanceof SpeciesSeedItem type) || source.getCount() <= reservedPerSlot
                || !plantingAllowed(slot,source)
                || !server.getBlockState(cropPos).isAir()
                || !com.agrievolution.farming.SoilResearch.isSoil(server.getBlockState(cropPos.below()))) return false;
        CropGenome genes = GenomeStorage.fromSeed(source);
        RareTrait rare = RareTraitStorage.fromSeed(source);
        HybridLineageStorage.Lineage lineage = HybridLineageStorage.fromSeed(source);
        if (!server.setBlock(cropPos, type.cropBlock().defaultBlockState()
                .setValue(AgriCropBlock.SUPPORTED, supportTier>0).setValue(AgriCropBlock.SUPPORT_TIER,supportTier), 3)) return false;
        if (server.getBlockEntity(cropPos) instanceof AECropBlockEntity entity) {
            entity.setGenome(genes);
            entity.setRareTrait(rare);
            entity.setLineage(lineage);
            entity.setVariety(com.agrievolution.genetics.SeedVariety.name(source));
        }
        monitorPlantedSpecies = type.species().ordinal() + 1;
        monitorPlantedSlot = slot + 1;
        monitorPlantedGenome = genes;
        monitorPlantedRareTrait = rare;
        source.shrink(1);
        if (source.isEmpty()) seedBank.setItem(slot, ItemStack.EMPTY);
        else seedBank.setChanged();
        totalAutoPlanted++;
        return true;
    }

    static net.minecraft.world.item.Item harvestProduce(CropSpecies species) {
        return switch (species) {
            case WILD -> Items.WHEAT;
            case CRIMSON_CLOVER -> Items.RED_DYE;
            case AZURE_FLAX -> Items.BLUE_DYE;
            case VIOLET_STARFLOWER -> Items.PURPLE_DYE;
            case AMBER_MARIGOLD -> Items.ORANGE_DYE;
            case SILVER_THISTLE -> Items.LIGHT_GRAY_DYE;
            case SUNFIRE_LILY -> Items.GLOW_BERRIES;
            case MOON_ORCHID -> Items.AMETHYST_SHARD;
            case ECLIPSE_LOTUS -> Items.ENDER_PEARL;
            case TWILIGHT_SAGE -> Items.GLOWSTONE_DUST;
            case DIRT -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case WOOD -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case STONE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case COAL -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case COPPER -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case IRON -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case REDSTONE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case GOLD -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case LAPIS -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case DIAMOND -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case EMERALD -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case QUARTZ -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case GRAVEL -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SAND -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case CLAY -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case FLINT -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case GLOWSTONE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case OBSIDIAN -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case NETHERRACK -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SOUL_SAND -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case MAGMA_CREAM -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case NETHER_WART -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case BLAZE_ROD -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case END_STONE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case CHORUS_FRUIT -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case ENDER_PEARL -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SHULKER_SHELL -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case ANCIENT_DEBRIS -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case NETHERITE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case OAK_SAPLING -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SPRUCE_SAPLING -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case BIRCH_SAPLING -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case DARK_OAK_SAPLING -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case TUBE_CORAL -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case BRAIN_CORAL -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case BUBBLE_CORAL -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case FIRE_CORAL -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case COAST_ARMOR_TRIM_SMITHING_TEMPLATE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case TIDE_ARMOR_TRIM_SMITHING_TEMPLATE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SUGAR_CANE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SWEET_BERRIES -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SEAGRASS -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case HANGING_ROOTS -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case TORCHFLOWER -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case PITCHER_PLANT -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SPORE_BLOSSOM -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case BIG_DRIPLEAF -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case TRIAL_KEY -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case OMINOUS_TRIAL_KEY -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case NETHERITE_UPGRADE_SMITHING_TEMPLATE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case DISC_FRAGMENT_5 -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case GRASS_BLOCK -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case PODZOL -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case MYCELIUM -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case DEAD_BUSH -> com.agrievolution.farming.ResourceCropRules.produce(species);
            
            
            case MUD_BRICKS -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case DEEPSLATE_BRICKS -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case CALIBRATED_SCULK_SENSOR -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case END_CRYSTAL -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case TOTEM_OF_UNDYING -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case ENCHANTED_GOLDEN_APPLE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case JUNGLE_LOG -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case COARSE_DIRT -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case ROOTED_DIRT -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case RED_SANDSTONE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case PRISMARINE_BRICKS -> com.agrievolution.farming.ResourceCropRules.produce(species);
            
            case WARPED_WART_BLOCK -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case RED_NETHER_BRICKS -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SCULK_SENSOR -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SCULK_SHRIEKER -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case CONDUIT -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case RESPAWN_ANCHOR -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case STICK -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case DRIED_KELP -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case GRAY_DYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case LIGHT_GRAY_DYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            
            
            
            
            case SKELETON_SKULL -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case WITHER_SKELETON_SKULL -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case CREEPER_HEAD -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case ZOMBIE_HEAD -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case WHITE_DYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case BLACK_DYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case ORANGE_DYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case PINK_DYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case PURPLE_DYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case MAGENTA_DYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case LIGHT_BLUE_DYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case LIME_DYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case HEART_OF_THE_SEA -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case NETHER_STAR -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case DRAGON_HEAD -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case HEAVY_CORE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case GREEN_DYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case RED_DYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case YELLOW_DYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case BROWN_DYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case HONEY_BOTTLE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case GLOW_BERRIES -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case BLUE_DYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case CYAN_DYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case FIRE_CHARGE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case WIND_CHARGE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case DRAGON_BREATH -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case EXPERIENCE_BOTTLE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SUGAR -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case COCOA_BEANS -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case EGG -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case ROTTEN_FLESH -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case RABBIT_HIDE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case RABBIT_FOOT -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case ARMADILLO_SCUTE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case FERMENTED_SPIDER_EYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case WITHER_ROSE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case CHORUS_FLOWER -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case CRIMSON_FUNGUS -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case WARPED_FUNGUS -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SPRUCE_LOG -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case BIRCH_LOG -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case ACACIA_LOG -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case DARK_OAK_LOG -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case MANGROVE_LOG -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case CHERRY_LOG -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SEA_PICKLE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case GLOW_LICHEN -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case CRIMSON_STEM -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case WARPED_STEM -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case VERDANT_FROGLIGHT -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case PEARLESCENT_FROGLIGHT -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case CHARCOAL -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case GLASS -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case BROWN_MUSHROOM -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case RED_MUSHROOM -> com.agrievolution.farming.ResourceCropRules.produce(species);
            
            
            case MOSSY_COBBLESTONE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case LILY_PAD -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case DARK_PRISMARINE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case GILDED_BLACKSTONE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SCULK_CATALYST -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case OCHRE_FROGLIGHT -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case RED_SAND -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SANDSTONE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case CACTUS -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case VINE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case PACKED_MUD -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case BRICK -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case ICE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case PACKED_ICE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case BLUE_ICE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            
            case NETHER_BRICK -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case ENDER_EYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case GRANITE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case DIORITE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case ANDESITE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case MUD -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case TUFF -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case DRIPSTONE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case DEEPSLATE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SOUL_SOIL -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SEA_LANTERN -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SHROOMLIGHT -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case END_ROD -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SCULK -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case PAPER -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case KELP -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case BAMBOO -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case BLACKSTONE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case BASALT -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case PURPUR -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case TURTLE_SCUTE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SPONGE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SNOWBALL -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case MOSS -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case CALCITE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case TERRACOTTA -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SPIDER_EYE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case GLOW_INK_SAC -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case CRYING_OBSIDIAN -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case NAUTILUS_SHELL -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case FEATHER -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case INK_SAC -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case HONEYCOMB -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case SLIME_BALL -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case GUNPOWDER -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case PRISMARINE_CRYSTALS -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case PHANTOM_MEMBRANE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case ECHO_SHARD -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case STRING -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case WOOL -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case LEATHER -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case BONE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case AMETHYST -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case PRISMARINE -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case GHAST_TEAR -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case BREEZE_ROD -> com.agrievolution.farming.ResourceCropRules.produce(species);
            case BASIL -> AEItems.BASIL.get();
            case MINT -> AEItems.MINT.get();
            case ROSEMARY -> AEItems.ROSEMARY.get();
            case THYME -> AEItems.THYME.get();
            case PARSLEY -> AEItems.PARSLEY.get();
            case CORIANDER -> AEItems.CORIANDER.get();
            case GINGER -> AEItems.GINGER.get();
            case TURMERIC -> AEItems.TURMERIC.get();
            case KALE -> AEItems.KALE.get();
            case PAK_CHOI -> AEItems.PAK_CHOI.get();
            case BRUSSELS_SPROUTS -> AEItems.BRUSSELS_SPROUTS.get();
            case SWEDE -> AEItems.SWEDE.get();
            case CELERIAC -> AEItems.CELERIAC.get();
            case LENTIL -> AEItems.LENTIL.get();
            case CHICKPEA -> AEItems.CHICKPEA.get();
            case BARLEY -> AEItems.BARLEY.get();
            case BUCKWHEAT -> AEItems.BUCKWHEAT.get();
            case RHUBARB -> AEItems.RHUBARB.get();
            case GRAPE -> AEItems.GRAPE.get();
            case CANTALOUPE -> AEItems.CANTALOUPE.get();
            case SPINACH -> AEItems.SPINACH.get();
            case CAULIFLOWER -> AEItems.CAULIFLOWER.get();
            case ARTICHOKE -> AEItems.ARTICHOKE.get();
            case CHILLI -> AEItems.CHILLI.get();
            case SPRING_ONION -> AEItems.SPRING_ONION.get();
            case FENNEL -> AEItems.FENNEL.get();
            case DILL -> AEItems.DILL.get();
            case PEANUT -> AEItems.PEANUT.get();
            case SESAME -> AEItems.SESAME.get();
            case OATS -> AEItems.OATS.get();
            case GOOSEBERRY -> AEItems.GOOSEBERRY.get();
            case BLACKCURRANT -> AEItems.BLACKCURRANT.get();
            case PUMPKIN -> AEItems.PUMPKIN.get();
            case BUTTERNUT_SQUASH -> AEItems.BUTTERNUT_SQUASH.get();
            case COURGETTE -> AEItems.COURGETTE.get();
            case TURNIP -> AEItems.TURNIP.get();
            case PARSNIP -> AEItems.PARSNIP.get();
            case LEEK -> AEItems.LEEK.get();
            case CELERY -> AEItems.CELERY.get();
            case ASPARAGUS -> AEItems.ASPARAGUS.get();
            case BLUEBERRY -> AEItems.BLUEBERRY.get();
            case RASPBERRY -> AEItems.RASPBERRY.get();
            case BLACKBERRY -> AEItems.BLACKBERRY.get();
            case CRANBERRY -> AEItems.CRANBERRY.get();
            case CORN -> AEItems.CORN.get();
            case PEAS -> AEItems.PEAS.get();
            case BEANS -> AEItems.BEANS.get();
            case BROCCOLI -> AEItems.BROCCOLI.get();
            case CABBAGE -> AEItems.CABBAGE.get();
            case RADISH -> AEItems.RADISH.get();
            case AUBERGINE -> AEItems.AUBERGINE.get();
            case SWEET_POTATO -> AEItems.SWEET_POTATO.get();
            case TOMATO -> AEItems.TOMATO.get();
            case LETTUCE -> AEItems.LETTUCE.get();
            case CUCUMBER -> AEItems.CUCUMBER.get();
            case ONION -> AEItems.ONION.get();
            case GARLIC -> AEItems.GARLIC.get();
            case PEPPER -> AEItems.PEPPER.get();
            case STRAWBERRY -> AEItems.STRAWBERRY.get();
            case RICE -> AEItems.RICE.get();
        };
    }

    private static NonNullList<ItemStack> copySlots(NonNullList<ItemStack> source) {
        NonNullList<ItemStack> copy = NonNullList.withSize(source.size(), ItemStack.EMPTY);
        for (int i = 0; i < source.size(); i++) copy.set(i, source.get(i).copy());
        return copy;
    }

    /** Distribute genetics to bank first. If all nine slots are occupied, keep
     * the candidate in storage, unless it is stronger than an existing same-
     * species line: then archive that weaker stack and promote the new seed.
     * Item component equality prevents merging different genetic profiles. */
    private static boolean distributeDrops(List<ItemStack> drops, NonNullList<ItemStack> bank,
                                            NonNullList<ItemStack> output) {
        for (ItemStack item : drops) {
            ItemStack remaining = item.copy();
            if (remaining.getItem() instanceof SpeciesSeedItem) {
                int existing = -1;
                int empty = -1;
                int weakest = -1;
                int weakestScore = Integer.MAX_VALUE;
                CropSpecies species = ((SpeciesSeedItem) remaining.getItem()).species();
                for (int slot = 0; slot < bank.size(); slot++) {
                    ItemStack current = bank.get(slot);
                    if (current.isEmpty()) {
                        if (empty == -1) empty = slot;
                        continue;
                    }
                    if (ItemStack.isSameItemSameComponents(current, remaining)
                            && current.getCount() < current.getMaxStackSize()) { existing = slot; break; }
                    if (current.getItem() instanceof SpeciesSeedItem seed && seed.species() == species) {
                        int score = GenomeStorage.fromSeed(current).totalScore();
                        if (score < weakestScore) { weakestScore = score; weakest = slot; }
                    }
                }
                if (existing >= 0) {
                    ItemStack current = bank.get(existing);
                    int moved = Math.min(remaining.getCount(), current.getMaxStackSize() - current.getCount());
                    current.grow(moved);
                    remaining.shrink(moved);
                } else if (empty >= 0) {
                    bank.set(empty, remaining.copy());
                    remaining = ItemStack.EMPTY;
                } else if (weakest >= 0 && GenomeStorage.fromSeed(remaining).totalScore() > weakestScore) {
                    ItemStack displaced = bank.get(weakest).copy();
                    // If output cannot retain the old line, keep the bank untouched.
                    NonNullList<ItemStack> check = copySlots(output);
                    if (insert(check, displaced)) {
                        for (int k = 0; k < check.size(); k++) output.set(k, check.get(k));
                        bank.set(weakest, remaining.copy());
                        remaining = ItemStack.EMPTY;
                    }
                }
            }
            if (!remaining.isEmpty() && !insert(output, remaining)) return false;
        }
        return true;
    }

    private void installInventories(NonNullList<ItemStack> newBank, NonNullList<ItemStack> newStorage) {
        for (int i = 0; i < SEED_BANK_SIZE; i++) seedBank.setItem(i, newBank.get(i));
        for (int i = 0; i < STORAGE_SIZE; i++) storage.set(i, newStorage.get(i));
        seedBank.setChanged();
        setChanged();
    }

    /** One archived 0.30.0 seed stack per pass is safely re-offered to the
     * Seed Bank. Seeds unable to fit remain in Automated Storage unchanged. */
    private void recycleStoredSeeds() {
        for (int slot = 0; slot < STORAGE_SIZE; slot++) {
            ItemStack candidate = storage.get(slot);
            if (!(candidate.getItem() instanceof SpeciesSeedItem) || candidate.isEmpty()) continue;
            NonNullList<ItemStack> futureBank = copySlots(seedBank.getItems());
            NonNullList<ItemStack> futureStorage = copySlots(storage);
            futureStorage.set(slot, ItemStack.EMPTY);
            if (!distributeDrops(List.of(candidate.copy()), futureBank, futureStorage)) return;
            // Avoid needless screen/inventory resynchronization when nothing moves.
            boolean changed = false;
            for (int i = 0; i < SEED_BANK_SIZE; i++)
                if (!ItemStack.matches(seedBank.getItem(i), futureBank.get(i))) { changed = true; break; }
            if (changed) { installInventories(futureBank, futureStorage); return; }
            return;
        }
    }

    /** Simulates or performs atomic insertion; comparisons include genome item components. */
    private static boolean insert(NonNullList<ItemStack> slots, ItemStack incoming) {
        if (incoming.isEmpty()) return true;
        for (int i = 0; i < slots.size(); i++) {
            ItemStack current = slots.get(i);
            if (current.isEmpty() || !ItemStack.isSameItemSameComponents(current, incoming)) continue;
            int moved = Math.min(incoming.getCount(), current.getMaxStackSize() - current.getCount());
            if (moved > 0) { current.grow(moved); incoming.shrink(moved); }
            if (incoming.isEmpty()) return true;
        }
        for (int i = 0; i < slots.size(); i++) {
            if (!slots.get(i).isEmpty()) continue;
            int moved = Math.min(incoming.getCount(), incoming.getMaxStackSize());
            slots.set(i, incoming.copyWithCount(moved));
            incoming.shrink(moved);
            if (incoming.isEmpty()) return true;
        }
        return false;
    }

    /** Adjacent output direction is explicit; never rummage through unrelated nearby inventories. */
    private Direction outputDirection() {
        Direction front = getBlockState().getValue(BotanicalGreenhouseBlock.FACING);
        return switch (exportSide) {
            case 1 -> front.getClockWise();
            case 2 -> front.getCounterClockWise();
            case 3 -> Direction.UP;
            case 4 -> Direction.DOWN;
            case 5 -> front;
            default -> front.getOpposite();
        };
    }

    private boolean isResourceOutput(ItemStack stack) {
        // The exported output is identified by its actual item type. A material that
        // can also come from a flower (such as pearls) is treated as a resource.
        for (CropSpecies species : CropSpecies.values()) {
            if (species.isResource() && stack.is(ResourceCropRules.produce(species))) return true;
        }
        return false;
    }

    private int categoryOf(ItemStack stack) {
        if (stack.getItem() instanceof SpeciesSeedItem) return HarvestExportPolicy.SEEDS;
        return isResourceOutput(stack) ? HarvestExportPolicy.RESOURCES : HarvestExportPolicy.BOTANICAL;
    }

    /** Move at most one eligible stack per second, after simulating destination acceptance.
     * Item components (genetics/traits) remain unmodified. The destination can reject
     * items, including via a modded sided handler; unaccepted items remain in storage. */
    private void exportToNeighbor(ServerLevel server) {
        // Consolidate only exact-matching item components; unlike item-only stacking,
        // this never mixes distinct genetics, rare traits or other mod data.
        if (compactStorage()) setChanged();
        Direction direction = outputDirection();
        BlockPos destination = worldPosition.relative(direction);
        exportTargetConnected = false;
        exportTargetRejected = false;
        if (!server.hasChunkAt(destination)) return; // no chunk loading
        IItemHandler handler = server.getCapability(Capabilities.ItemHandler.BLOCK,
                destination, direction.getOpposite());
        if (handler == null) return;
        exportTargetConnected = true;
        if (exportNearFullOnly && occupiedStorageSlots() < HarvestExportPolicy.NEAR_FULL_SLOTS) return;
        boolean[] categoriesEnabled = {exportResourceItems, exportBotanicalItems, exportSeeds};
        List<HarvestExportPolicy.Candidate> choices = new ArrayList<>();
        for (int slot = 0; slot < STORAGE_SIZE; slot++) {
            ItemStack stack = storage.get(slot);
            if (stack.isEmpty()) continue;
            int totalSame = 0;
            for (ItemStack existing : storage) {
                if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(stack, existing))
                    totalSame += existing.getCount();
            }
            choices.add(new HarvestExportPolicy.Candidate(slot, categoryOf(stack),
                    stack.getCount(), totalSame));
        }
        var selection = HarvestExportPolicy.choose(choices, categoriesEnabled,
                exportPriorities, exportReserve, exportCursor, STORAGE_SIZE);
        if (selection == null) return;
        ItemStack inSlot = storage.get(selection.slot());
        if (inSlot.isEmpty()) return;
        ItemStack remaining = inSlot.copyWithCount(selection.amount());
        // Handle partial insertion safely: the source shrinks by exactly the amount
        // the target really accepted. Simulation never changes the source.
        for (int slot = 0; slot < handler.getSlots() && !remaining.isEmpty(); slot++) {
            ItemStack simulated = handler.insertItem(slot, remaining, true);
            if (simulated.getCount() < remaining.getCount()) {
                int accepted = remaining.getCount() - simulated.getCount();
                ItemStack actualRemainder = handler.insertItem(slot, remaining.copyWithCount(accepted), false);
                int moved = accepted - actualRemainder.getCount();
                if (moved > 0) {
                    inSlot.shrink(moved);
                    if (inSlot.isEmpty()) storage.set(selection.slot(), ItemStack.EMPTY);
                    exportedItems = Math.min(30000, exportedItems + moved);
                    setChanged();
                    remaining.shrink(moved);
                }
                // If a handler's real insertion differs from simulation, never remove
                // more items; retry any unaccepted portion in a subsequent cycle.
                if (moved < accepted) break;
            }
        }
        if (remaining.getCount() == selection.amount()) exportTargetRejected = true;
        exportCursor = (selection.slot() + 1) % STORAGE_SIZE;
    }

    private boolean compactStorage() {
        boolean changed = false;
        for (int i = 0; i < STORAGE_SIZE; i++) {
            ItemStack target = storage.get(i);
            if (target.isEmpty() || target.getCount() >= target.getMaxStackSize()) continue;
            for (int j = i + 1; j < STORAGE_SIZE; j++) {
                ItemStack source = storage.get(j);
                if (source.isEmpty() || !ItemStack.isSameItemSameComponents(target, source)) continue;
                int amount = Math.min(source.getCount(), target.getMaxStackSize() - target.getCount());
                if (amount <= 0) break;
                target.grow(amount);
                source.shrink(amount);
                if (source.isEmpty()) storage.set(j, ItemStack.EMPTY);
                changed = true;
                if (target.getCount() == target.getMaxStackSize()) break;
            }
        }
        return changed;
    }

    /** Controller storage is output-only: no items can be inserted from the UI. */
    @Override public int getContainerSize() { return STORAGE_SIZE; }
    @Override public boolean isEmpty() { return storage.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) {
        return slot >= 0 && slot < STORAGE_SIZE ? storage.get(slot) : ItemStack.EMPTY;
    }
    @Override public ItemStack removeItem(int slot, int count) {
        ItemStack removed = ContainerHelper.removeItem(storage, slot, count);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = ContainerHelper.takeItem(storage, slot);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }
    @Override public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= STORAGE_SIZE) return;
        storage.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
    }
    @Override public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this &&
                player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5) <= 64.0;
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return false; }
    @Override public void clearContent() {
        storage.clear();
        storage = NonNullList.withSize(STORAGE_SIZE, ItemStack.EMPTY);
        setChanged();
    }

    /** Refreshes both persisted blockstate and the transient chunk-safe water ticket. */
    public void refresh() {
        if (!(level instanceof ServerLevel server) || isRemoved()) return;
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof BotanicalGreenhouseBlock)) {
            stopWatering();
            return;
        }
        Direction facing = state.getValue(BotanicalGreenhouseBlock.FACING);
        BlockState upgradedVisual = state
                .setValue(BotanicalGreenhouseBlock.UPGRADED, automationUpgraded)
                .setValue(BotanicalGreenhouseBlock.SMART, smartFarmingUpgraded);
        if (state != upgradedVisual) {
            state = upgradedVisual;
            server.setBlock(worldPosition, state, 3);
        }
        boolean valid = BotanicalGreenhouseLayout.isComplete(server, worldPosition, facing);
        if (formed != valid) { formed = valid; setChanged(); }
        boolean active = formed && water > 0;
        if (state.getValue(BotanicalGreenhouseBlock.ACTIVE) != active)
            server.setBlock(worldPosition, state.setValue(BotanicalGreenhouseBlock.ACTIVE, active), 3);
        if (!active) {
            stopWatering();
            updateDashboardStats(server, facing);
            return;
        }
        if (waterTicket == null || !waterTicket.isValid()) {
            BlockPos a = BotanicalGreenhouseLayout.at(worldPosition, facing, -2, 1, -1);
            BlockPos b = BotanicalGreenhouseLayout.at(worldPosition, facing, 2, 5, -1);
            waterTicket = FarmlandWaterManager.addAABBTicket(server, new AABB(
                    Math.min(a.getX(), b.getX()), a.getY(), Math.min(a.getZ(), b.getZ()),
                    Math.max(a.getX(), b.getX()) + 1, a.getY() + 2,
                    Math.max(a.getZ(), b.getZ()) + 1));
        }
        // Keep existing farmland dark/moist even before its next random tick.
        for (int across = -2; across <= 2; across++) for (int depth = 1; depth <= 5; depth++) {
            BlockPos soil = BotanicalGreenhouseLayout.at(worldPosition, facing, across, depth, -1);
            if (!server.hasChunkAt(soil)) continue;
            BlockState ground = server.getBlockState(soil);
            if (ground.getBlock() instanceof FarmBlock && ground.getValue(FarmBlock.MOISTURE) != 7)
                server.setBlock(soil, ground.setValue(FarmBlock.MOISTURE, 7), 2);
        }
        updateDashboardStats(server, facing);
    }

    /** Count real interior conditions without forcing chunk loads.
     *  These fields sync to the open controller GUI through ContainerData. */
    private void updateDashboardStats(ServerLevel server, Direction facing) {
        farmlandCount = hydratedCount = plantedCount = 0;
        growingCount = matureCount = adequatelyLitCount = 0;
        breedingStationCount = averageLight = emptyPlotCount = 0;
        Arrays.fill(weedMap, 0);
        int brightnessSum = 0;
        int measured = 0;
        for (int across = -2; across <= 2; across++) {
            for (int depth = 1; depth <= 5; depth++) {
                BlockPos plantPos = BotanicalGreenhouseLayout.at(worldPosition, facing, across, depth, 0);
                BlockPos soilPos = plantPos.below();
                int plot = GreenhouseStickStatus.index(across, depth);
                stickMap[plot] = GreenhouseStickStatus.UNAVAILABLE;
                if (!server.hasChunkAt(plantPos) || !server.hasChunkAt(soilPos)) continue;
                int light = server.getRawBrightness(plantPos, 0);
                brightnessSum += light;
                measured++;
                BlockState soil = server.getBlockState(soilPos);
                if (soil.getBlock() instanceof FarmBlock) {
                    farmlandCount++;
                    if (soil.getValue(FarmBlock.MOISTURE) == 7) hydratedCount++;
                }
                BlockState plant = server.getBlockState(plantPos);
                GreenhouseWeedStatus weeds = GreenhouseWeedStatus.NO_CROP;
                if (plant.getBlock() instanceof AgriCropBlock) {
                    weeds = plant.getValue(AgriCropBlock.WEEDY) ? GreenhouseWeedStatus.WEEDY : GreenhouseWeedStatus.CLEAR;
                } else if (CropWeeds.isHost(plant)) {
                    weeds = CropWeeds.hasWeeds(server, plantPos) ? GreenhouseWeedStatus.WEEDY : GreenhouseWeedStatus.CLEAR;
                }
                weedMap[GreenhouseWeedStatus.dataIndex(plot)] |= weeds.bits(plot);
                stickMap[plot] = GreenhouseStickStatus.NO_CROP;
                if (plant.getBlock() instanceof AgriCropBlock crop) {
                    int tier = CropSupportItem.tier(plant);
                    if (tier == 0) stickMap[plot] = GreenhouseStickStatus.UNSUPPORTED;
                    else if (tier == 3) stickMap[plot] = GreenhouseStickStatus.OBSIDIAN;
                    else if (server.getBlockEntity(plantPos) instanceof AECropBlockEntity planted) {
                        stickMap[plot] = GreenhouseStickStatus.encode(tier, planted.supportTicks());
                    } else stickMap[plot] = GreenhouseStickStatus.UNAVAILABLE;
                    plantedCount++;
                    if (plant.getValue(CropBlock.AGE) < crop.getMaxAge()) {
                        growingCount++;
                        if (light >= 9) adequatelyLitCount++;
                    } else {
                        matureCount++;
                    }
                }
                if (plant.isAir() && com.agrievolution.farming.SoilResearch.isSoil(soil)) emptyPlotCount++;
                if (plant.getBlock() instanceof SeedBreedingStationBlock) breedingStationCount++;
            }
        }
        if (measured > 0) averageLight = brightnessSum / measured;
        updateClimate(server);
    }

    /** World-derived ambient readings and simulated internal climate. */
    private void updateClimate(ServerLevel server) {
        var biome = server.getBiome(worldPosition).value();
        boolean rain = server.isRaining();
        long time = server.getDayTime() % 24000L;
        boolean night = time >= 13000L && time <= 23000L;
        outsideTemperatureC = GreenhouseClimate.ambientTemperature(biome.getBaseTemperature(), rain, night);
        boolean working = formed && water > 0;
        boolean heat = working && heaterEnabled && heaterFuelSeconds > 0;
        boolean vent = working && ventsEnabled;
        estimatedTemperatureC = GreenhouseClimate.temperature(outsideTemperatureC, heat, vent);
        simulatedHumidity = GreenhouseClimate.humidity(biome.hasPrecipitation() ? 0.65f : 0.10f, rain, water,
                farmlandCount, hydratedCount, heat, vent);
        growthBonusChance = GreenhouseClimate.extraGrowthChance(estimatedTemperatureC, simulatedHumidity);
    }

    private void stopWatering() {
        if (waterTicket != null) { waterTicket.invalidate(); waterTicket = null; }
    }

    @Override public void onChunkUnloaded() { stopWatering(); super.onChunkUnloaded(); }
    @Override public void setRemoved() { stopWatering(); super.setRemoved(); }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("GreenhouseWater", water);
        tag.putBoolean("ClimateHeater", heaterEnabled);
        tag.putBoolean("ClimateVents", ventsEnabled);
        tag.putInt("ClimateFuelSeconds", heaterFuelSeconds);
        tag.putBoolean("AutomationUpgraded", automationUpgraded);
        tag.putBoolean("SmartFarmingUpgraded", smartFarmingUpgraded);
        tag.putBoolean("SmartFarmingAutoPlant", autoPlant);
        tag.putBoolean("SmartFarmingAutoBreed", autoBreed);
        tag.putInt("SmartFarmingAutoBred", totalAutoBred);
        tag.putInt("SmartFarmingBreedResult", lastBreedResult);
        tag.putInt("SmartFarmingBreedCursor", breedSlotCursor);
        tag.putBoolean("SmartFarmingFlowerFilter", plantFlowerSeeds);
        tag.putBoolean("SmartFarmingResourceFilter", plantResourceSeeds);
        tag.putInt("SmartFarmingPlanted", totalAutoPlanted);
        tag.putInt("SmartFarmingSeedCursor", seedSlotCursor);
        tag.putInt("SmartFarmingPlotCursor", plotCursor);
        tag.putInt("SmartFarmingSeedDisabled", disabledSeedSlots);
        tag.putIntArray("SmartFarmingSeedPriorities", seedPriorities);
        tag.putInt("SmartFarmingSeedReserve", reservedPerSlot);
        tag.put("SelectivePlantingSpecies", writeSpeciesLocks(plantingSpecies));
        tag.putIntArray("SelectivePlantingMinimumGenes",minimumPlantingGenes);
        var presets = new CompoundTag();
        for (int i = 0; i < plantingPresets.length; i++) {
            PlantingPreset preset = plantingPresets[i];
            if (preset == null) continue;
            var saved = new CompoundTag();
            saved.put("Species", writeSpeciesLocks(preset.species()));
            saved.putIntArray("MinimumGenes", preset.minimumGenes());
            presets.put("Preset" + i, saved);
        }
        tag.put("PlantingPresets", presets);
        tag.putBoolean("SmartFarmingPreferGenes", preferStrongestSeeds);
        tag.putInt("MonitoringAttempts", monitorAttempts);
        tag.putInt("MonitoringSuccessful", monitorSuccessful);
        tag.putInt("MonitoringLastResult", monitorLastResult);
        tag.putInt("MonitoringLastSlot", monitorLastSlot);
        tag.putString("MonitoringLastSpeciesId", com.agrievolution.genetics.RetiredResourceCrops.monitorId(monitorLastSpecies));
        tag.putString("MonitoringPlantedSpeciesId", com.agrievolution.genetics.RetiredResourceCrops.monitorId(monitorPlantedSpecies));
        tag.putInt("MonitoringPlantedSlot", monitorPlantedSlot);
        tag.put("MonitoringPlantedGenome", GenomeStorage.writeGenome(monitorPlantedGenome));
        tag.putString("MonitoringPlantedRareTrait", monitorPlantedRareTrait.name());
        CompoundTag seedsTag = new CompoundTag();
        ContainerHelper.saveAllItems(seedsTag, seedBank.getItems(), registries);
        tag.put("SmartFarmingSeeds", seedsTag);
        tag.putBoolean("AutomationHarvest", autoHarvest);
        tag.putBoolean("AutomationReplant", autoReplant);
        tag.putInt("AutomationHarvests", totalHarvests);
        tag.putInt("AutomationReplants", totalReplants);
        tag.putBoolean("HarvestExportEnabled", autoExport);
        tag.putBoolean("HarvestExportNearFullOnly", exportNearFullOnly);
        tag.putBoolean("HarvestExportResource", exportResourceItems);
        tag.putBoolean("HarvestExportBotanical", exportBotanicalItems);
        tag.putBoolean("HarvestExportSeeds", exportSeeds);
        tag.putIntArray("HarvestExportPriorities", exportPriorities);
        tag.putInt("HarvestExportReserve", exportReserve);
        tag.putInt("HarvestExportSide", exportSide);
        tag.putInt("HarvestExportCursor", exportCursor);
        tag.putInt("HarvestExportTransferred", exportedItems);
        ContainerHelper.saveAllItems(tag, storage, registries);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        water = Math.max(0, Math.min(CAPACITY, tag.getInt("GreenhouseWater")));
        heaterEnabled = tag.getBoolean("ClimateHeater");
        ventsEnabled = tag.getBoolean("ClimateVents");
        heaterFuelSeconds = GreenhouseClimate.clamp(tag.getInt("ClimateFuelSeconds"), 0,
                GreenhouseClimate.MAX_FUEL_SECONDS);
        automationUpgraded = tag.getBoolean("AutomationUpgraded");
        smartFarmingUpgraded = automationUpgraded && tag.getBoolean("SmartFarmingUpgraded");
        autoPlant = smartFarmingUpgraded && tag.getBoolean("SmartFarmingAutoPlant");
        autoBreed = smartFarmingUpgraded && tag.getBoolean("SmartFarmingAutoBreed");
        totalAutoBred = Math.clamp(tag.getInt("SmartFarmingAutoBred"), 0, 30000);
        lastBreedResult = Math.clamp(tag.getInt("SmartFarmingBreedResult"), 0, 3);
        breedSlotCursor = Math.floorMod(tag.getInt("SmartFarmingBreedCursor"), SEED_BANK_SIZE);
        // Missing flags mean an older world: preserve its existing auto-plant behaviour.
        plantFlowerSeeds = !tag.contains("SmartFarmingFlowerFilter")
                || tag.getBoolean("SmartFarmingFlowerFilter");
        plantResourceSeeds = !tag.contains("SmartFarmingResourceFilter")
                || tag.getBoolean("SmartFarmingResourceFilter");
        totalAutoPlanted = Math.max(0, tag.getInt("SmartFarmingPlanted"));
        seedSlotCursor = Math.floorMod(tag.getInt("SmartFarmingSeedCursor"), SEED_BANK_SIZE);
        plotCursor = Math.floorMod(tag.getInt("SmartFarmingPlotCursor"), 25);
        disabledSeedSlots = tag.getInt("SmartFarmingSeedDisabled") & 0x1FF;
        java.util.Arrays.fill(seedPriorities, 1);
        int[] savedPriorities = tag.getIntArray("SmartFarmingSeedPriorities");
        for (int i = 0; i < Math.min(savedPriorities.length, SEED_BANK_SIZE); i++)
            seedPriorities[i] = Math.max(0, Math.min(2, savedPriorities[i]));
        reservedPerSlot = Math.max(0, Math.min(64, tag.getInt("SmartFarmingSeedReserve")));
        readSpeciesLocks(tag.getCompound("SelectivePlantingSpecies"), plantingSpecies);
        readMinimumGenes(tag.getIntArray("SelectivePlantingMinimumGenes"), minimumPlantingGenes);
        Arrays.fill(plantingPresets, null);
        var presets = tag.getCompound("PlantingPresets");
        for (int i = 0; i < plantingPresets.length; i++) {
            String key = "Preset" + i;
            if (!presets.contains(key, net.minecraft.nbt.Tag.TAG_COMPOUND)) continue;
            var saved = presets.getCompound(key);
            int[] species = new int[SEED_BANK_SIZE];
            int[] minimums = new int[minimumPlantingGenes.length];
            readSpeciesLocks(saved.getCompound("Species"), species);
            readMinimumGenes(saved.getIntArray("MinimumGenes"), minimums);
            plantingPresets[i] = new PlantingPreset(species, minimums);
        }
        preferStrongestSeeds = !tag.contains("SmartFarmingPreferGenes") || tag.getBoolean("SmartFarmingPreferGenes");
        monitorAttempts = Math.max(0, Math.min(30000, tag.getInt("MonitoringAttempts")));
        monitorSuccessful = Math.max(0, Math.min(monitorAttempts, tag.getInt("MonitoringSuccessful")));
        monitorLastResult = Math.max(0, Math.min(SeedBankMonitoring.MIXED_EXCLUSIONS,
                tag.getInt("MonitoringLastResult")));
        monitorLastSlot = Math.max(0, Math.min(SEED_BANK_SIZE, tag.getInt("MonitoringLastSlot")));
        monitorLastSpecies = tag.contains("MonitoringLastSpeciesId", 8)
                ? com.agrievolution.genetics.RetiredResourceCrops.monitorValue(tag.getString("MonitoringLastSpeciesId"))
                : com.agrievolution.genetics.RetiredResourceCrops.legacyMonitorValue(tag.getInt("MonitoringLastSpecies"));
        monitorPlantedSpecies = tag.contains("MonitoringPlantedSpeciesId", 8)
                ? com.agrievolution.genetics.RetiredResourceCrops.monitorValue(tag.getString("MonitoringPlantedSpeciesId"))
                : com.agrievolution.genetics.RetiredResourceCrops.legacyMonitorValue(tag.getInt("MonitoringPlantedSpecies"));
        monitorPlantedSlot = Math.clamp(tag.getInt("MonitoringPlantedSlot"), 0, SEED_BANK_SIZE);
        monitorPlantedGenome = tag.contains("MonitoringPlantedGenome", 10)
                ? GenomeStorage.readGenome(tag.getCompound("MonitoringPlantedGenome"))
                : CropGenome.wild();
        monitorPlantedRareTrait = RareTrait.fromName(tag.getString("MonitoringPlantedRareTrait"));
        seedBank.clearContent();
        if (tag.contains("SmartFarmingSeeds", 10))
            ContainerHelper.loadAllItems(tag.getCompound("SmartFarmingSeeds"), seedBank.getItems(), registries);
        autoHarvest = automationUpgraded && tag.getBoolean("AutomationHarvest");
        autoReplant = !tag.contains("AutomationReplant") || tag.getBoolean("AutomationReplant");
        totalHarvests = Math.max(0, tag.getInt("AutomationHarvests"));
        totalReplants = Math.max(0, tag.getInt("AutomationReplants"));
        autoExport = automationUpgraded && tag.getBoolean("HarvestExportEnabled");
        exportNearFullOnly = tag.getBoolean("HarvestExportNearFullOnly");
        exportResourceItems = !tag.contains("HarvestExportResource") || tag.getBoolean("HarvestExportResource");
        exportBotanicalItems = !tag.contains("HarvestExportBotanical") || tag.getBoolean("HarvestExportBotanical");
        exportSeeds = tag.getBoolean("HarvestExportSeeds");
        exportPriorities[0] = 2;
        exportPriorities[1] = 1;
        exportPriorities[2] = 0;
        int[] savedExportPriority = tag.getIntArray("HarvestExportPriorities");
        for (int i = 0; i < Math.min(3, savedExportPriority.length); i++)
            exportPriorities[i] = Math.clamp(savedExportPriority[i], 0, 2);
        exportReserve = Math.max(0, Math.min(64, tag.getInt("HarvestExportReserve")));
        exportSide = Math.floorMod(tag.getInt("HarvestExportSide"), 6);
        exportCursor = Math.floorMod(tag.getInt("HarvestExportCursor"), STORAGE_SIZE);
        exportedItems = Math.max(0, Math.min(30000, tag.getInt("HarvestExportTransferred")));
        exportTargetConnected = exportTargetRejected = false;
        storage = NonNullList.withSize(STORAGE_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, storage, registries);
        formed = false; // always revalidate the real glass structure after loading
    }
}
