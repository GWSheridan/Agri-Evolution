package com.agrievolution.content;

import com.agrievolution.genetics.GenomeBreeder;
import com.agrievolution.genetics.GenomeStorage;
import com.agrievolution.genetics.HybridLineageStorage;
import com.agrievolution.genetics.RareTrait;
import com.agrievolution.genetics.RareTraitStorage;
import com.agrievolution.genetics.SpeciesBreeder;
import java.util.Random;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** The base owns the trays, output buffers and water for all three blocks. */
public final class VerticalGreenhouseBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, IFluidHandler {
    public static final int TRAYS = 6, HARVEST = TRAYS, OFFSPRING = HARVEST + 9, SLOTS = OFFSPRING + 9;
    public static final int CAPACITY = 16000, HARVEST_WATER = 250, BREED_WATER = 500;
    public static final int PROGRESS_START = 7, PAIR_MODE = PROGRESS_START + TRAYS;
    public static final int SELECTED_A = PAIR_MODE + 1, SELECTED_B = PAIR_MODE + 2;
    public static final int WATER_INTAKE = PAIR_MODE + 3, WATER_STATUS = PAIR_MODE + 4;
    public static final int ROUTE_START = WATER_STATUS + 1, ROUTE_COUNT = 5;
    public static final int BATCH_TARGET = ROUTE_START + ROUTE_COUNT, BATCH_COUNT = BATCH_TARGET + 1, DATA_COUNT = BATCH_COUNT + 1;
    private static final int BUCKET = 1000;
    private static final int BREED_SECONDS = 35;
    private static final int[] OUTPUT_SLOTS = IntStream.range(HARVEST, SLOTS).toArray();
    private static final int[] HARVEST_SLOTS = IntStream.range(HARVEST, OFFSPRING).toArray();
    private static final int[] OFFSPRING_SLOTS = IntStream.range(OFFSPRING, SLOTS).toArray();
    private static final int[] NO_SLOTS = {};
    private static final Direction[] OUTPUT_FACES = {Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private static final int[] BATCH_TARGETS = {0, 1, 4, 8, 12, 16, 32, 64};

    public enum Status { INCOMPLETE, OFF, NO_SEEDS, NO_HARVEST, NO_PAIR, NEED_WATER, OUTPUT_FULL, RUNNING, BATCH_COMPLETE }
    public enum OutputRoute {
        OFF, PRODUCE, SEEDS, BOTH;

        private static final OutputRoute[] ROUTES = values();
        static OutputRoute fromId(int id) { return id >= 0 && id < ROUTES.length ? ROUTES[id] : OFF; }
        OutputRoute next() { return ROUTES[(ordinal() + 1) % ROUTES.length]; }
    }

    public enum WaterIntake {
        DEMAND, KEEP_FULL, OFF;

        private static final WaterIntake[] MODES = values();

        static WaterIntake fromId(int id) {
            return id >= 0 && id < MODES.length ? MODES[id] : DEMAND;
        }

        WaterIntake next() {
            return MODES[(ordinal() + 1) % MODES.length];
        }
    }

    public enum WaterStatus {
        CHECKING, OFF, WAITING, READY, REFILLING, NO_SOURCE, EMPTY_SOURCE, BLOCKED_VALVE, NETWORK_LIMIT, INCOMPLETE,
        BLOCKED_PORT, UNLOADED_SUPPLY, INCOMPLETE_RESERVOIR, UNLOADED_RESERVOIR
    }

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private final NonNullList<ItemStack> tracked = NonNullList.withSize(TRAYS, ItemStack.EMPTY);
    private final int[] growth = new int[TRAYS];
    private final int[] progress = new int[TRAYS];
    private int water, growCursor, breedCursor, breeding;
    private int batchIndex, batchCount;
    private final OutputRoute[] routes = {OutputRoute.BOTH, OutputRoute.OFF, OutputRoute.OFF, OutputRoute.OFF, OutputRoute.OFF};
    private int parentA = -1, parentB = -1;
    private int selectedA = -1, selectedB = -1;
    private ItemStack pendingChild = ItemStack.EMPTY;
    private boolean growEnabled = true, breedEnabled = true;
    private boolean fixedPair;
    private Status growStatus = Status.NO_SEEDS, breedStatus = Status.NO_PAIR;
    private WaterIntake waterIntake = WaterIntake.DEMAND;
    private WaterStatus waterStatus = WaterStatus.CHECKING;

    final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> water;
                case 1 -> (growEnabled ? 1 : 0) | (breedEnabled ? 2 : 0);
                case 2 -> growStatus.ordinal();
                case 3 -> breedStatus.ordinal();
                case 4 -> 100 * breeding / BREED_SECONDS;
                case 5 -> parentA + 1;
                case 6 -> parentB + 1;
                case PAIR_MODE -> fixedPair ? 1 : 0;
                case SELECTED_A -> selectedA + 1;
                case SELECTED_B -> selectedB + 1;
                case WATER_INTAKE -> waterIntake.ordinal();
                case WATER_STATUS -> waterStatus.ordinal();
                case BATCH_TARGET -> BATCH_TARGETS[batchIndex];
                case BATCH_COUNT -> batchCount;
                default -> index >= ROUTE_START ? routes[index - ROUTE_START].ordinal() : progress[index - PROGRESS_START];
            };
        }
        @Override public void set(int index, int value) {}
        @Override public int getCount() { return DATA_COUNT; }
    };

    public VerticalGreenhouseBlockEntity(BlockPos pos, BlockState state) {
        super(AEBlockEntities.VERTICAL_GREENHOUSE.get(), pos, state);
    }

    @Override public void setChanged() {
        if (level == null || !level.hasChunkAt(worldPosition)) return;
        level.blockEntityChanged(worldPosition);
        IrrigationNetwork.updateComparators(level, worldPosition, getBlockState().getBlock());
    }

    @Override public int getContainerSize() { return SLOTS; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> items) { this.items = items; }
    @Override protected Component getDefaultName() { return Component.translatable("block.agri_evolution.vertical_greenhouse"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new VerticalGreenhouseMenu(id, inventory, this); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot < TRAYS && acceptsSeed(stack); }
    @Override public int[] getSlotsForFace(Direction side) {
        for (int face = 0; face < ROUTE_COUNT; face++) {
            if (side != OUTPUT_FACES[face]) continue;
            return switch (routes[face]) {
                case OFF -> NO_SLOTS;
                case PRODUCE -> HARVEST_SLOTS;
                case SEEDS -> OFFSPRING_SLOTS;
                case BOTH -> OUTPUT_SLOTS;
            };
        }
        return NO_SLOTS;
    }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        for (int output : getSlotsForFace(side)) if (slot == output) return true;
        return false;
    }

    public static Direction outputFace(int face) { return OUTPUT_FACES[face]; }

    void cycleOutputRoute(int face) {
        routes[face] = routes[face].next();
        setChanged();
        level.invalidateCapabilities(worldPosition);
    }

    void cycleBatchTarget() {
        batchIndex = (batchIndex + 1) % BATCH_TARGETS.length;
        resetBatch();
    }

    void resetBatch() {
        batchCount = 0;
        resetPair();
        setChanged();
    }

    private boolean batchComplete() {
        int target = BATCH_TARGETS[batchIndex];
        return target > 0 && batchCount >= target;
    }

    static boolean acceptsSeed(ItemStack stack) {
        return SeedBreedingParents.accepts(stack);
    }

    void toggle(int button) {
        if (button == 0) growEnabled = !growEnabled;
        else if (button == 1) breedEnabled = !breedEnabled;
        setChanged();
    }

    void togglePairingMode() {
        fixedPair = !fixedPair;
        resetPair();
        setChanged();
    }

    void cycleWaterIntake() {
        waterIntake = waterIntake.next();
        waterStatus = waterIntake == WaterIntake.OFF ? WaterStatus.OFF : WaterStatus.CHECKING;
        setChanged();
    }

    boolean selectParent(int tray, boolean second) {
        if (!fixedPair || tray < 0 || tray >= TRAYS) return false;
        if (second) {
            if (selectedA == tray) selectedA = -1;
            selectedB = selectedB == tray ? -1 : tray;
        } else {
            if (selectedB == tray) selectedB = -1;
            selectedA = selectedA == tray ? -1 : tray;
        }
        resetPair();
        setChanged();
        return true;
    }

    public boolean complete() {
        if (level == null || isRemoved() || !level.hasChunkAt(worldPosition) || level.getBlockEntity(worldPosition) != this) return false;
        BlockState state = getBlockState();
        Direction facing = state.getValue(VerticalGreenhouseBlock.FACING);
        return state.getValue(VerticalGreenhouseBlock.SECTION) == 0
                && VerticalGreenhouseBlock.matches(level.getBlockState(worldPosition.above()), state.getBlock(), facing, 1)
                && VerticalGreenhouseBlock.matches(level.getBlockState(worldPosition.above(2)), state.getBlock(), facing, 2);
    }

    public int water() { return water; }
    boolean addBucket() { return fill(new FluidStack(Fluids.WATER, BUCKET), FluidAction.SIMULATE) == BUCKET
            && fill(new FluidStack(Fluids.WATER, BUCKET), FluidAction.EXECUTE) == BUCKET; }

    @Override public int getTanks() { return 1; }
    @Override public FluidStack getFluidInTank(int tank) {
        return tank == 0 && water > 0 && complete() ? new FluidStack(Fluids.WATER, water) : FluidStack.EMPTY;
    }
    @Override public int getTankCapacity(int tank) { return tank == 0 ? CAPACITY : 0; }
    @Override public boolean isFluidValid(int tank, FluidStack fluid) { return tank == 0 && fluid.is(Fluids.WATER); }
    @Override public int fill(FluidStack fluid, FluidAction action) {
        if (!(level instanceof ServerLevel) || !complete() || !isFluidValid(0, fluid)) return 0;
        int amount = Math.min(CAPACITY - water, fluid.getAmount());
        if (action.execute() && amount > 0) {
            water += amount;
            setChanged();
        }
        return amount;
    }
    @Override public FluidStack drain(FluidStack fluid, FluidAction action) { return FluidStack.EMPTY; }
    @Override public FluidStack drain(int amount, FluidAction action) { return FluidStack.EMPTY; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, VerticalGreenhouseBlockEntity greenhouse) {
        if (level instanceof ServerLevel server && server.getGameTime() % 20 == 0) greenhouse.runSecond(server);
    }

    void runSecond(ServerLevel server) {
        if (!complete()) {
            growStatus = breedStatus = Status.INCOMPLETE;
            waterStatus = WaterStatus.INCOMPLETE;
            return;
        }
        reconcileSeeds();
        var harvests = new VerticalGreenhouseHarvests.Harvest[TRAYS];
        boolean hasHarvest = false;
        for (int tray = 0; tray < TRAYS; tray++) {
            harvests[tray] = VerticalGreenhouseHarvests.forSeed(items.get(tray));
            hasHarvest |= harvests[tray] != null;
        }
        boolean breedingActive = breedEnabled && !batchComplete();
        if (breedingActive) selectPair();
        waterStatus = refillFromNetwork(server, growEnabled && hasHarvest || breedingActive && parentA >= 0);
        // Alternate which job gets first access to a nearly empty tank.
        if ((growCursor & 1) == 0) {
            growStatus = grow(harvests);
            breedStatus = breed(server);
        } else {
            breedStatus = breed(server);
            growStatus = grow(harvests);
        }
        growCursor = (growCursor + 1) % TRAYS;
        for (int tray = 0; tray < TRAYS; tray++) {
            var harvest = harvests[tray];
            progress[tray] = harvest == null ? (items.get(tray).isEmpty() ? 0 : -1) : Math.min(100, 100 * growth[tray] / harvest.seconds());
        }
        exportSideOutputs(server);
        setChanged();
    }

    private void exportSideOutputs(ServerLevel server) {
        for (int face = 1; face < ROUTE_COUNT; face++) {
            if (routes[face] == OutputRoute.OFF) continue;
            Direction side = OUTPUT_FACES[face];
            BlockPos destination = worldPosition.relative(side);
            if (!server.hasChunkAt(destination)) continue;
            var vault = server.getBlockEntity(destination) instanceof SeedVaultBlockEntity seedVault ? seedVault : null;
            var handler = vault == null ? server.getCapability(Capabilities.ItemHandler.BLOCK, destination, side.getOpposite()) : null;
            if (vault == null && handler == null) continue;
            for (int slot : getSlotsForFace(side)) {
                ItemStack stack = items.get(slot);
                if (stack.isEmpty()) continue;
                boolean exported = vault != null
                        ? slot >= OFFSPRING && vault.receiveOffspring(stack)
                        : ItemHandlerHelper.insertItemStacked(handler, stack.copyWithCount(1), false).isEmpty();
                if (exported) {
                    stack.shrink(1);
                    if (stack.isEmpty()) items.set(slot, ItemStack.EMPTY);
                    break;
                }
            }
        }
    }

    private void reconcileSeeds() {
        for (int tray = 0; tray < TRAYS; tray++) {
            ItemStack seed = items.get(tray);
            if (ItemStack.isSameItemSameComponents(seed, tracked.get(tray))) continue;
            growth[tray] = progress[tray] = 0;
            tracked.set(tray, seed.copyWithCount(1));
            if (tray == parentA || tray == parentB) resetPair();
        }
    }

    private WaterStatus refillFromNetwork(ServerLevel server, boolean needsWater) {
        if (waterIntake == WaterIntake.OFF) return WaterStatus.OFF;
        if (waterIntake == WaterIntake.DEMAND) {
            if (!needsWater) return WaterStatus.WAITING;
            if (water >= BUCKET) return WaterStatus.READY;
        }
        if (CAPACITY - water < BUCKET) return WaterStatus.READY;

        var inspection = IrrigationNetwork.inspectSupply(server, worldPosition);
        var network = inspection.network();
        if (network.overflow()) return WaterStatus.NETWORK_LIMIT;
        for (var supply : network.supplies()) {
            if (supply.takeBucket(server)) {
                water += BUCKET;
                return WaterStatus.REFILLING;
            }
        }
        if (!network.supplies().isEmpty()) return WaterStatus.EMPTY_SOURCE;
        if (network.blockedValve()) return WaterStatus.BLOCKED_VALVE;
        return switch (inspection.route(server).flow()) {
            case DISABLED_PORT -> WaterStatus.BLOCKED_PORT;
            case UNLOADED_CONNECTION -> WaterStatus.UNLOADED_SUPPLY;
            case UNLOADED_RESERVOIR -> WaterStatus.UNLOADED_RESERVOIR;
            case INCOMPLETE_RESERVOIR -> WaterStatus.INCOMPLETE_RESERVOIR;
            default -> WaterStatus.NO_SOURCE;
        };
    }

    private Status grow(VerticalGreenhouseHarvests.Harvest[] harvests) {
        if (!growEnabled) return Status.OFF;
        boolean hasCrop = false, waitingForWater = false, full = false, worked = false;
        for (int step = 0; step < TRAYS; step++) {
            int tray = (growCursor + step) % TRAYS;
            var harvest = harvests[tray];
            if (harvest == null) continue;
            hasCrop = true;
            if (water < HARVEST_WATER) {
                waitingForWater = true;
                continue;
            }
            if (growth[tray] < harvest.seconds()) growth[tray]++;
            if (growth[tray] >= harvest.seconds()) {
                if (!store(harvest.output(), HARVEST, OFFSPRING)) {
                    full = true;
                    continue;
                }
                water -= HARVEST_WATER;
                growth[tray] = 0;
            }
            worked = true;
        }
        if (worked) return Status.RUNNING;
        if (full) return Status.OUTPUT_FULL;
        if (waitingForWater) return Status.NEED_WATER;
        if (!hasCrop && items.subList(0, TRAYS).stream().anyMatch(seed -> !seed.isEmpty())) return Status.NO_HARVEST;
        return Status.NO_SEEDS;
    }

    private void selectPair() {
        if (parentA >= 0 && compatible(parentA, parentB)) return;
        resetPair();
        if (fixedPair) {
            if (selectedA >= 0 && selectedB >= 0 && compatible(selectedA, selectedB)) {
                parentA = Math.min(selectedA, selectedB);
                parentB = Math.max(selectedA, selectedB);
            }
            return;
        }
        for (int step = 0; step < TRAYS * TRAYS; step++) {
            int index = (breedCursor + step) % (TRAYS * TRAYS);
            int first = index / TRAYS, second = index % TRAYS;
            if (first >= second || !compatible(first, second)) continue;
            parentA = first;
            parentB = second;
            return;
        }
    }

    private boolean compatible(int first, int second) {
        var pair = SeedBreedingParents.pair(items.get(first), items.get(second));
        return pair == SeedBreedingParents.Pair.AGRI || pair == SeedBreedingParents.Pair.NATIVE;
    }

    private void resetPair() {
        parentA = parentB = -1;
        breeding = 0;
        pendingChild = ItemStack.EMPTY;
    }

    private Status breed(ServerLevel server) {
        if (!breedEnabled) return Status.OFF;
        if (batchComplete()) return Status.BATCH_COMPLETE;
        if (parentA < 0) return Status.NO_PAIR;
        if (water < BREED_WATER) return Status.NEED_WATER;
        if (breeding < BREED_SECONDS) breeding++;
        if (breeding < BREED_SECONDS) return Status.RUNNING;
        if (pendingChild.isEmpty()) pendingChild = offspring(server, items.get(parentA), items.get(parentB));
        if (!store(pendingChild, OFFSPRING, SLOTS)) return Status.OUTPUT_FULL;
        if (batchIndex > 0) batchCount += pendingChild.getCount();
        water -= BREED_WATER;
        if (!fixedPair) breedCursor = (parentA * TRAYS + parentB + 1) % (TRAYS * TRAYS);
        resetPair();
        return batchComplete() ? Status.BATCH_COMPLETE : Status.RUNNING;
    }

    private static ItemStack offspring(ServerLevel server, ItemStack first, ItemStack second) {
        if (!(first.getItem() instanceof SpeciesSeedItem a) || !(second.getItem() instanceof SpeciesSeedItem b)) return first.copyWithCount(1);
        var species = SpeciesBreeder.cross(a.species(), b.species(), server.random.nextLong());
        var child = new ItemStack(AEItems.seedFor(species));
        com.agrievolution.genetics.SeedVariety.inherit(child, first, second);
        GenomeStorage.putOnSeed(child, GenomeBreeder.cross(GenomeStorage.fromSeed(first), GenomeStorage.fromSeed(second), server.random.nextLong()));
        RareTraitStorage.putOnSeed(child, RareTrait.inherit(RareTraitStorage.fromSeed(first), RareTraitStorage.fromSeed(second), new Random(server.random.nextLong())));
        var lineage = a.species() != b.species() ? new HybridLineageStorage.Lineage(a.species(), b.species()) : HybridLineageStorage.fromSeed(first);
        if (lineage == null) lineage = HybridLineageStorage.fromSeed(second);
        if (lineage != null) HybridLineageStorage.putOnSeed(child, lineage);
        return child;
    }

    /** Store atomically. The caller charges water only after the complete result fits. */
    private boolean store(ItemStack result, int first, int end) {
        int space = 0;
        for (int i = first; i < end; i++) {
            ItemStack current = items.get(i);
            if (current.isEmpty()) space += result.getMaxStackSize();
            else if (ItemStack.isSameItemSameComponents(result, current)) space += current.getMaxStackSize() - current.getCount();
        }
        if (space < result.getCount()) return false;
        int remaining = result.getCount();
        for (int i = first; i < end && remaining > 0; i++) {
            ItemStack current = items.get(i);
            if (!ItemStack.isSameItemSameComponents(result, current)) continue;
            int amount = Math.min(remaining, current.getMaxStackSize() - current.getCount());
            current.grow(amount);
            remaining -= amount;
        }
        for (int i = first; i < end && remaining > 0; i++) {
            if (!items.get(i).isEmpty()) continue;
            int amount = Math.min(remaining, result.getMaxStackSize());
            items.set(i, result.copyWithCount(amount));
            remaining -= amount;
        }
        return true;
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        reconcileSeeds();
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("Water", water);
        tag.putInt("WaterIntake", waterIntake.ordinal());
        tag.putIntArray("OutputRoutes", java.util.Arrays.stream(routes).mapToInt(OutputRoute::ordinal).toArray());
        tag.putInt("BatchIndex", batchIndex);
        tag.putInt("BatchCount", batchCount);
        tag.putBoolean("Grow", growEnabled);
        tag.putBoolean("Breed", breedEnabled);
        tag.putBoolean("FixedPair", fixedPair);
        tag.putInt("SelectedA", selectedA + 1);
        tag.putInt("SelectedB", selectedB + 1);
        tag.putIntArray("Growth", growth.clone());
        tag.putInt("GrowthCursor", growCursor);
        tag.putInt("BreedingCursor", breedCursor);
        tag.putInt("Breeding", breeding);
        tag.putInt("ParentA", parentA);
        tag.putInt("ParentB", parentB);
        if (!pendingChild.isEmpty()) tag.put("PendingChild", pendingChild.save(registries));
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        water = Math.clamp(tag.getInt("Water"), 0, CAPACITY);
        waterIntake = WaterIntake.fromId(tag.getInt("WaterIntake"));
        waterStatus = waterIntake == WaterIntake.OFF ? WaterStatus.OFF : WaterStatus.CHECKING;
        int[] savedRoutes = tag.getIntArray("OutputRoutes");
        for (int face = 0; face < ROUTE_COUNT; face++) {
            routes[face] = face < savedRoutes.length ? OutputRoute.fromId(savedRoutes[face])
                    : face == 0 ? OutputRoute.BOTH : OutputRoute.OFF;
        }
        batchIndex = Math.clamp(tag.getInt("BatchIndex"), 0, BATCH_TARGETS.length - 1);
        batchCount = Math.clamp(tag.getInt("BatchCount"), 0, BATCH_TARGETS[batchIndex]);
        growEnabled = !tag.contains("Grow") || tag.getBoolean("Grow");
        breedEnabled = !tag.contains("Breed") || tag.getBoolean("Breed");
        fixedPair = tag.getBoolean("FixedPair");
        selectedA = readSelectedTray(tag.getInt("SelectedA"));
        selectedB = readSelectedTray(tag.getInt("SelectedB"));
        if (selectedB == selectedA) selectedB = -1;
        int[] savedGrowth = tag.getIntArray("Growth");
        for (int tray = 0; tray < TRAYS; tray++) {
            tracked.set(tray, items.get(tray).copyWithCount(1));
            growth[tray] = tray < savedGrowth.length ? Math.clamp(savedGrowth[tray], 0, 3600) : 0;
        }
        growCursor = Math.floorMod(tag.getInt("GrowthCursor"), TRAYS);
        breedCursor = Math.floorMod(tag.getInt("BreedingCursor"), TRAYS * TRAYS);
        resetPair();
        int first = tag.getInt("ParentA"), second = tag.getInt("ParentB");
        if (first >= 0 && first < second && second < TRAYS
                && (!fixedPair || first == Math.min(selectedA, selectedB) && second == Math.max(selectedA, selectedB))
                && compatible(first, second)) {
            parentA = first;
            parentB = second;
            breeding = Math.clamp(tag.getInt("Breeding"), 0, BREED_SECONDS);
            if (breeding == BREED_SECONDS) pendingChild = ItemStack.parseOptional(registries, tag.getCompound("PendingChild"));
        }
    }

    private static int readSelectedTray(int savedTray) {
        return savedTray >= 1 && savedTray <= TRAYS ? savedTray - 1 : -1;
    }
}
