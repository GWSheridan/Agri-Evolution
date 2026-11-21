package com.agrievolution.content;

import com.agrievolution.genetics.CropGenome;
import com.agrievolution.genetics.GenomeStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution") @PrefixGameTestTemplate(false)
public final class GreenhouseOutputCapacityGameTests {
    private static VerticalGreenhouseBlockEntity greenhouse(GameTestHelper h) {
        var level = h.getLevel(); var base = h.absolutePos(new BlockPos(4, 200, 4));
        level.setBlock(base.below(), Blocks.STONE.defaultBlockState(), 2);
        for (int part = 0; part < 3; part++) level.setBlock(base.above(part), AEBlocks.VERTICAL_GREENHOUSE.get()
                .defaultBlockState().setValue(VerticalGreenhouseBlock.SECTION, part), 2);
        var plant = (VerticalGreenhouseBlockEntity) level.getBlockEntity(base);
        h.assertTrue(plant.addBucket(), "A complete column accepts its working water");
        return plant;
    }
    private static void savedOutputs(GameTestHelper h, VerticalGreenhouseBlockEntity plant, ItemStack product, int first, int end) {
        for (int slot = first; slot < end; slot++) plant.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        plant.setItem(first, product.copyWithCount(64)); plant.getItem(first).setCount(99);
        plant.setItem(first + 1, product.copyWithCount(64)); plant.getItem(first + 1).setCount(99);
        var saved = plant.saveWithoutMetadata(h.getLevel().registryAccess());
        plant.loadAdditional(saved, h.getLevel().registryAccess());
        h.assertTrue(plant.getItem(first).getCount() == 99 && plant.getItem(first + 1).getCount() == 99,
                "The fixture retains above-limit stacks from an imported save");
    }
    @GameTest(template="empty") public static void harvestUsesFreeSpaceBesideOverfilledSavedStacks(GameTestHelper h) {
        var plant = greenhouse(h); plant.toggle(1);
        var seed = new ItemStack(AEItems.TOMATO_SEED.get()); GenomeStorage.putOnSeed(seed, new CropGenome(100, 100, 50));
        plant.setItem(0, seed);
        savedOutputs(h, plant, new ItemStack(AEItems.TOMATO.get()), VerticalGreenhouseBlockEntity.HARVEST, VerticalGreenhouseBlockEntity.OFFSPRING);
        int last = VerticalGreenhouseBlockEntity.OFFSPRING - 1; plant.setItem(last, ItemStack.EMPTY);
        for (int second = 0; second < 40; second++) plant.runSecond(h.getLevel());
        h.assertTrue(plant.getItem(last).is(AEItems.TOMATO.get()) && plant.getItem(last).getCount() == 3 && plant.water() == 750,
                "A harvest fits in the empty slot even when other saved stacks exceed their current limits");
        h.assertTrue(plant.getItem(VerticalGreenhouseBlockEntity.HARVEST).getCount() == 99
                        && plant.getItem(VerticalGreenhouseBlockEntity.HARVEST + 1).getCount() == 99,
                "Harvesting leaves existing above-limit stacks intact");
        h.succeed();
    }
    @GameTest(template="empty") public static void queuedOffspringUsesFreedSpaceBesideOverfilledSavedStacks(GameTestHelper h) {
        var plant = greenhouse(h); plant.toggle(0);
        plant.setItem(0, new ItemStack(Items.WHEAT_SEEDS)); plant.setItem(1, new ItemStack(Items.WHEAT_SEEDS));
        savedOutputs(h, plant, new ItemStack(Items.WHEAT_SEEDS), VerticalGreenhouseBlockEntity.OFFSPRING, VerticalGreenhouseBlockEntity.REJECTS);
        for (int second = 0; second < 35; second++) plant.runSecond(h.getLevel());
        h.assertTrue(plant.water() == 1000 && plant.saveWithoutMetadata(h.getLevel().registryAccess()).contains("PendingChild"),
                "A full output buffer retains the child without charging water");
        int last = VerticalGreenhouseBlockEntity.REJECTS - 1; plant.setItem(last, ItemStack.EMPTY);
        plant.runSecond(h.getLevel());
        h.assertTrue(plant.getItem(last).is(Items.WHEAT_SEEDS) && plant.getItem(last).getCount() == 1 && plant.water() == 500,
                "Freeing one output slot releases the queued child and charges water exactly once");
        h.assertTrue(plant.getItem(VerticalGreenhouseBlockEntity.OFFSPRING).getCount() == 99
                        && plant.getItem(VerticalGreenhouseBlockEntity.OFFSPRING + 1).getCount() == 99,
                "Releasing offspring preserves the older saved stacks");
        h.succeed();
    }
}
