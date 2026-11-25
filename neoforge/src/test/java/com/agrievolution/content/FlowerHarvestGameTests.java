package com.agrievolution.content;

import com.agrievolution.farming.BotanicalGreenhouseLayout;
import com.agrievolution.genetics.CropGenome;
import com.agrievolution.genetics.CropSpecies;
import com.agrievolution.genetics.RareTrait;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class FlowerHarvestGameTests {
    private static final Map<CropSpecies, Item> EXPECTED = Map.ofEntries(
            Map.entry(CropSpecies.WILD, Items.WHEAT),
            Map.entry(CropSpecies.CRIMSON_CLOVER, Items.RED_DYE),
            Map.entry(CropSpecies.AZURE_FLAX, Items.BLUE_DYE),
            Map.entry(CropSpecies.VIOLET_STARFLOWER, Items.PURPLE_DYE),
            Map.entry(CropSpecies.AMBER_MARIGOLD, Items.ORANGE_DYE),
            Map.entry(CropSpecies.SILVER_THISTLE, Items.LIGHT_GRAY_DYE),
            Map.entry(CropSpecies.SUNFIRE_LILY, Items.YELLOW_DYE),
            Map.entry(CropSpecies.MOON_ORCHID, Items.LIGHT_BLUE_DYE),
            Map.entry(CropSpecies.ECLIPSE_LOTUS, Items.PINK_DYE),
            Map.entry(CropSpecies.TWILIGHT_SAGE, Items.MAGENTA_DYE),
            Map.entry(CropSpecies.ENDER_PEARL, Items.ENDER_PEARL),
            Map.entry(CropSpecies.AMETHYST, Items.AMETHYST_SHARD),
            Map.entry(CropSpecies.GLOW_BERRIES, Items.GLOW_BERRIES),
            Map.entry(CropSpecies.GLOWSTONE, Items.GLOWSTONE_DUST));

    @GameTest(template = "empty")
    public static void ripeFlowersAndDedicatedCropsGiveTheirOwnProduce(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(4, 241, 4));
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        level.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState(), 2);
        for (var entry : EXPECTED.entrySet()) {
            var species = entry.getKey();
            Item produce = entry.getValue();
            var seed = (SpeciesSeedItem) AEItems.seedFor(species);
            var block = (AgriCropBlock) seed.cropBlock();
            var ripe = block.getStateForAge(block.getMaxAge());
            level.setBlock(pos, ripe, 2);
            var entity = (AECropBlockEntity) level.getBlockEntity(pos);
            var normal = Block.getDrops(ripe, level, pos, entity);
            helper.assertTrue(normal.stream().anyMatch(stack -> stack.is(produce))
                            && normal.stream().allMatch(stack -> stack.is(produce) || stack.is(seed)),
                    species.displayName() + " drops its expected produce from standard loot");

            entity.setGenome(new CropGenome(50, 100, 50));
            entity.setRareTrait(RareTrait.ABUNDANT);
            level.removeBlock(pos, false);
            block.playerDestroy(level, player, pos, ripe, entity, ItemStack.EMPTY);
            var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
            helper.assertTrue(drops.stream().allMatch(drop -> drop.getItem().is(produce) || drop.getItem().is(seed))
                            && drops.stream().filter(drop -> drop.getItem().is(produce))
                            .mapToInt(drop -> drop.getItem().getCount()).sum() >= 5,
                    species.displayName() + " genetics add more of that same produce");
            drops.forEach(ItemEntity::discard);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void botanicalGreenhouseStoresFlowerDyesAndDedicatedProduce(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        var controller = helper.absolutePos(new BlockPos(4, 241, 4));
        var facing = Direction.NORTH;
        level.setBlock(controller, AEBlocks.BOTANICAL_GREENHOUSE.get().defaultBlockState()
                .setValue(BotanicalGreenhouseBlock.FACING, facing), 2);
        var greenhouse = (BotanicalGreenhouseBlockEntity) level.getBlockEntity(controller);
        var pos = BotanicalGreenhouseLayout.at(controller, facing, 0, 1, 0);
        level.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState(), 2);
        var harvest = BotanicalGreenhouseBlockEntity.class.getDeclaredMethod("harvestAndReplant", ServerLevel.class);
        harvest.setAccessible(true);
        for (var entry : EXPECTED.entrySet()) {
            var seed = (SpeciesSeedItem) AEItems.seedFor(entry.getKey());
            var block = (AgriCropBlock) seed.cropBlock();
            level.setBlock(pos, block.getStateForAge(block.getMaxAge()), 2);
            harvest.invoke(greenhouse, level);
            boolean stored = false;
            for (int slot = 0; slot < greenhouse.getContainerSize(); slot++) {
                var stack = greenhouse.getItem(slot);
                if (stack.is(entry.getValue())) stored = true;
                helper.assertTrue(stack.isEmpty() || stack.is(seed) || stack.is(entry.getValue()),
                        entry.getKey().displayName() + " should not leave other produce in the greenhouse");
            }
            helper.assertTrue(stored, "Botanical Greenhouse harvested " + entry.getKey().displayName());
            greenhouse.clearContent();
        }
        helper.succeed();
    }
}
