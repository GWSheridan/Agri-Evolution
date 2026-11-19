package com.agrievolution.content;

import com.agrievolution.genetics.CropGenome;
import com.agrievolution.genetics.GenomeStorage;
import com.agrievolution.genetics.SeedVariety;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class ExportTransactionGameTests {
    private static final Map<BlockPos, Handler> HANDLERS = new HashMap<>();

    @EventBusSubscriber(modid = "agri_evolution", bus = EventBusSubscriber.Bus.MOD)
    public static final class TestCapabilities {
        @SubscribeEvent public static void register(RegisterCapabilitiesEvent event) {
            event.registerBlock(Capabilities.ItemHandler.BLOCK,
                    (level, pos, state, entity, side) -> HANDLERS.get(pos), Blocks.STONE);
        }
    }

    private static final class Handler implements IItemHandler {
        int realLimit = 64, received;
        boolean simulated, inserted;
        Runnable onSimulation = () -> {}, onInsertion = () -> {};
        @Override public int getSlots() { return 1; }
        @Override public ItemStack getStackInSlot(int slot) { return ItemStack.EMPTY; }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (simulate) {
                if (!simulated) { simulated = true; onSimulation.run(); }
                return ItemStack.EMPTY;
            }
            if (!inserted) { inserted = true; onInsertion.run(); }
            int count = Math.min(realLimit, stack.getCount());
            received += count;
            return stack.copyWithCount(stack.getCount() - count);
        }
        @Override public ItemStack extractItem(int slot, int count, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return true; }
    }

    private record Exporter(BlockEntity owner, Container inventory, int sourceSlot, int first, int end,
                            BlockPos target, ItemStack seed) {
        void send(ServerLevel level) {
            try {
                var method = owner.getClass().getDeclaredMethod(owner instanceof BotanicalGreenhouseBlockEntity
                        ? "exportToNeighbor" : "exportSideOutputs", ServerLevel.class);
                method.setAccessible(true);
                method.invoke(owner, level);
            } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
        }
        int remainingSeeds() {
            int count = 0;
            for (int slot = first; slot < end; slot++) {
                var stack = inventory.getItem(slot);
                if (ItemStack.isSameItemSameComponents(seed, stack)) count += stack.getCount();
            }
            return count;
        }
    }

    private static Exporter exporter(GameTestHelper h, boolean vertical, int count) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        var seed = new ItemStack(AEItems.TOMATO_SEED.get(), count);
        GenomeStorage.putOnSeed(seed, new CropGenome(83, 77, 91));
        SeedVariety.apply(seed, "Saved export line");
        if (!vertical) {
            h.getLevel().setBlock(pos, AEBlocks.BOTANICAL_GREENHOUSE.get().defaultBlockState()
                    .setValue(BotanicalGreenhouseBlock.FACING, Direction.NORTH), 3);
            var house = (BotanicalGreenhouseBlockEntity) h.getLevel().getBlockEntity(pos);
            house.exportSettings.set(13, 1);
            house.setItem(0, seed.copy());
            return new Exporter(house, house, 0, 0, house.getContainerSize(), pos.south(), seed);
        }
        var block = AEBlocks.VERTICAL_GREENHOUSE.get();
        for (int part = 0; part < 3; part++) h.getLevel().setBlock(pos.above(part), block.defaultBlockState()
                .setValue(VerticalGreenhouseBlock.SECTION, part), 2);
        var house = (VerticalGreenhouseBlockEntity) h.getLevel().getBlockEntity(pos);
        h.assertTrue(house != null && house.complete(), "The output test has a complete vertical column");
        house.cycleOutputRoute(3);
        house.cycleOutputRoute(3); // South sends accepted offspring.
        house.setItem(VerticalGreenhouseBlockEntity.OFFSPRING, seed.copy());
        return new Exporter(house, house, VerticalGreenhouseBlockEntity.OFFSPRING,
                VerticalGreenhouseBlockEntity.OFFSPRING, VerticalGreenhouseBlockEntity.REJECTS, pos.south(), seed);
    }

    private static void withHandler(GameTestHelper h, Exporter source, Handler handler, Runnable test) {
        h.getLevel().setBlock(source.target(), Blocks.STONE.defaultBlockState(), 3);
        HANDLERS.put(source.target(), handler);
        h.getLevel().invalidateCapabilities(source.target());
        try {
            h.assertTrue(h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, source.target(), Direction.NORTH) == handler,
                    "The destination exposes the callback test handler");
            test.run();
        } finally {
            HANDLERS.remove(source.target());
            h.getLevel().invalidateCapabilities(source.target());
        }
    }

    private static int droppedSeeds(GameTestHelper h, Exporter source) {
        return h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(source.owner().getBlockPos()).inflate(3)).stream()
                .filter(e -> ItemStack.isSameItemSameComponents(source.seed(), e.getItem()))
                .mapToInt(e -> e.getItem().getCount()).sum();
    }

    private static void reentry(GameTestHelper h, boolean vertical) {
        var source = exporter(h, vertical, 1);
        var handler = new Handler();
        handler.onInsertion = () -> source.send(h.getLevel());
        withHandler(h, source, handler, () -> source.send(h.getLevel()));
        h.assertTrue(handler.received == 1 && source.remainingSeeds() == 0,
                "An insertion callback cannot export the same single seed twice");
        h.succeed();
    }

    @GameTest(template = "empty") public static void botanicalExportRejectsRecursiveInsertion(GameTestHelper h) { reentry(h, false); }
    @GameTest(template = "empty") public static void verticalExportRejectsRecursiveInsertion(GameTestHelper h) { reentry(h, true); }

    private static void removed(GameTestHelper h, boolean vertical) {
        var source = exporter(h, vertical, 1);
        source.owner().setRemoved();
        var handler = new Handler();
        withHandler(h, source, handler, () -> source.send(h.getLevel()));
        h.assertTrue(handler.received == 0 && source.remainingSeeds() == 1, "Removed sources cannot send their detached inventory");
        h.succeed();
    }

    @GameTest(template = "empty") public static void removedBotanicalGreenhouseCannotExport(GameTestHelper h) { removed(h, false); }
    @GameTest(template = "empty") public static void removedVerticalGreenhouseCannotExport(GameTestHelper h) { removed(h, true); }

    @GameTest(template = "empty")
    public static void simulationCannotExportAnItemReplacedInTheSource(GameTestHelper h) {
        var source = exporter(h, false, 1);
        var handler = new Handler();
        handler.onSimulation = () -> source.inventory().setItem(source.sourceSlot(), new ItemStack(Items.DIAMOND, 2));
        withHandler(h, source, handler, () -> source.send(h.getLevel()));
        h.assertTrue(handler.received == 0 && source.inventory().getItem(source.sourceSlot()).is(Items.DIAMOND)
                        && source.inventory().getItem(source.sourceSlot()).getCount() == 2,
                "Simulation changes invalidate the offer without deleting the replacement stack");
        h.succeed();
    }

    private static void rejectionAfterReplacement(GameTestHelper h, boolean vertical) {
        var source = exporter(h, vertical, 1);
        var handler = new Handler();
        handler.realLimit = 0;
        handler.onInsertion = () -> source.inventory().setItem(source.sourceSlot(), new ItemStack(Items.DIAMOND, 2));
        withHandler(h, source, handler, () -> source.send(h.getLevel()));
        h.assertTrue(handler.received == 0 && source.remainingSeeds() == 1
                        && source.inventory().getItem(source.sourceSlot()).is(Items.DIAMOND)
                        && source.inventory().getItem(source.sourceSlot()).getCount() == 2,
                "Rejected seeds return to a free output slot and keep their components without replacing callback items");
        h.succeed();
    }

    @GameTest(template = "empty") public static void botanicalRejectedExportPreservesReplacementItems(GameTestHelper h) { rejectionAfterReplacement(h, false); }
    @GameTest(template = "empty") public static void verticalRejectedExportPreservesReplacementItems(GameTestHelper h) { rejectionAfterReplacement(h, true); }

    private static void brokenDuringInsertion(GameTestHelper h, boolean vertical) {
        var source = exporter(h, vertical, 1);
        var handler = new Handler();
        handler.onInsertion = () -> h.getLevel().setBlock(source.owner().getBlockPos(), Blocks.STONE.defaultBlockState(), 3);
        withHandler(h, source, handler, () -> source.send(h.getLevel()));
        h.assertTrue(handler.received == 1 && droppedSeeds(h, source) == 0,
                "Breaking the source during insertion cannot also drop the exported seed");
        h.succeed();
    }

    @GameTest(template = "empty") public static void botanicalExportCannotDuplicateItsBreakDrops(GameTestHelper h) { brokenDuringInsertion(h, false); }
    @GameTest(template = "empty") public static void verticalExportCannotDuplicateItsBreakDrops(GameTestHelper h) { brokenDuringInsertion(h, true); }

    private static void fullOnReturn(GameTestHelper h, boolean vertical) {
        var source = exporter(h, vertical, 1);
        var handler = new Handler();
        handler.realLimit = 0;
        handler.onInsertion = () -> {
            for (int slot = source.first(); slot < source.end(); slot++)
                source.inventory().setItem(slot, new ItemStack(Items.DIAMOND, 64));
        };
        withHandler(h, source, handler, () -> source.send(h.getLevel()));
        h.assertTrue(handler.received == 0 && source.remainingSeeds() == 0 && droppedSeeds(h, source) == 1,
                "If callback items fill every return slot, the rejected seed drops once with its original genetics");
        for (int slot = source.first(); slot < source.end(); slot++)
            h.assertTrue(source.inventory().getItem(slot).is(Items.DIAMOND) && source.inventory().getItem(slot).getCount() == 64,
                    "Returning a rejected seed cannot overwrite newly stored items");
        h.succeed();
    }

    @GameTest(template = "empty") public static void botanicalFullReturnDropsRejectedSeed(GameTestHelper h) { fullOnReturn(h, false); }
    @GameTest(template = "empty") public static void verticalFullReturnDropsRejectedSeed(GameTestHelper h) { fullOnReturn(h, true); }

    @GameTest(template = "empty")
    public static void partialInsertionStillPreservesTheUnacceptedSeeds(GameTestHelper h) {
        var source = exporter(h, false, 4);
        var handler = new Handler();
        handler.realLimit = 1;
        withHandler(h, source, handler, () -> source.send(h.getLevel()));
        h.assertTrue(handler.received == 1 && source.remainingSeeds() == 3 && droppedSeeds(h, source) == 0,
                "A destination that accepts less than simulation keeps exactly the remaining original seeds in storage");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void partialInsertionReturnsRemainderAfterTheSourceBreaks(GameTestHelper h) {
        var source = exporter(h, false, 4);
        var handler = new Handler();
        handler.realLimit = 1;
        handler.onInsertion = () -> h.getLevel().setBlock(source.owner().getBlockPos(), Blocks.STONE.defaultBlockState(), 3);
        withHandler(h, source, handler, () -> source.send(h.getLevel()));
        h.assertTrue(handler.received == 1 && droppedSeeds(h, source) == 3,
                "Breaking the source during a partial insertion leaves one accepted seed and three matching drops");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedVaultRejectsOffspring(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        h.getLevel().setBlock(pos, AEBlocks.SEED_VAULT.get().defaultBlockState(), 3);
        var vault = (SeedVaultBlockEntity) h.getLevel().getBlockEntity(pos);
        vault.setRemoved();
        var seed = new ItemStack(AEItems.TOMATO_SEED.get());
        h.assertFalse(vault.receiveOffspring(seed), "A stale vault reference cannot accept a greenhouse seed");
        h.assertTrue(vault.isEmpty() && seed.getCount() == 1, "Rejected intake leaves both inventories alone");
        h.succeed();
    }
}
