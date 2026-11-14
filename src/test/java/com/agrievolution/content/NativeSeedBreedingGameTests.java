package com.agrievolution.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class NativeSeedBreedingGameTests {
    private static SeedBreedingStationBlockEntity station(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(5, 190, 5));
        level.setBlock(pos, AEBlocks.SEED_BREEDING_STATION.get().defaultBlockState(), 3);
        return (SeedBreedingStationBlockEntity) level.getBlockEntity(pos);
    }

    private static void cycle(GameTestHelper helper, SeedBreedingStationBlockEntity station) {
        for (int tick = 0; tick < station.requiredTicks() && station.getItem(4).isEmpty(); tick++) {
            SeedBreedingStationBlockEntity.serverTick(helper.getLevel(), station.getBlockPos(), station.getBlockState(), station);
        }
    }

    @GameTest(template = "empty")
    public static void breedsVanillaSeedsAndTagOptIns(GameTestHelper helper) {
        var station = station(helper);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu = new SeedBreedingStationMenu(1, player.getInventory(), station);
        helper.assertTrue(SeedBagItem.accepts(new ItemStack(Items.CHORUS_FLOWER)), "Shared seed tag loads in the test datapack");
        helper.assertTrue(new ItemStack(Items.BAMBOO).is(SeedBreedingParents.EXTERNAL_SEEDS), "The optional legacy seed tag joins the opt-in tag");

        for (Item item : new Item[]{Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.MELON_SEEDS, Items.PUMPKIN_SEEDS,
                Items.TORCHFLOWER_SEEDS, Items.PITCHER_POD, Items.CARROT, Items.POTATO, Items.NETHER_WART,
                Items.COCOA_BEANS, Items.SWEET_BERRIES, Items.GLOW_BERRIES, Items.CHORUS_FLOWER, Items.BAMBOO}) {
            ItemStack parent = new ItemStack(item);
            parent.set(DataComponents.CUSTOM_NAME, Component.literal("Seed variety A"));
            helper.assertTrue(menu.getSlot(0).mayPlace(parent) && station.canPlaceItem(1, parent), "Both parent paths accept " + item);
            station.setItem(0, parent.copy());
            station.setItem(1, parent.copy());
            station.setItem(2, new ItemStack(AEItems.NUTRIENT_COMPOST.get()));
            station.setItem(4, ItemStack.EMPTY);
            cycle(helper, station);
            var child = station.getItem(4);
            helper.assertTrue(child.getCount() == 1 && ItemStack.isSameItemSameComponents(parent, child), "Native offspring preserves item and variant for " + item);
            helper.assertTrue(station.getItem(0).getCount() == 1 && station.getItem(1).getCount() == 1 && station.getItem(2).isEmpty(),
                    "Only compost is consumed in native breeding");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rejectsMismatchedParentsAndKeepsAgriCrosses(GameTestHelper helper) {
        var station = station(helper);
        var wheat = new ItemStack(Items.WHEAT_SEEDS);
        station.setItem(0, wheat);
        station.setItem(2, new ItemStack(AEItems.NUTRIENT_COMPOST.get()));
        var otherVariant = wheat.copy();
        otherVariant.set(DataComponents.CUSTOM_NAME, Component.literal("Variant B"));
        for (var other : new ItemStack[]{new ItemStack(Items.BEETROOT_SEEDS), otherVariant, new ItemStack(AEItems.WILD_SEED.get()), new ItemStack(Items.DIAMOND)}) {
            station.setItem(1, other);
            cycle(helper, station);
            helper.assertTrue(station.getItem(4).isEmpty() && station.getItem(2).getCount() == 1 && other.getCount() == 1,
                    "Invalid pairs do not make output or consume materials");
        }
        station.setItem(1, wheat.copy());
        station.setItem(2, ItemStack.EMPTY);
        cycle(helper, station);
        helper.assertTrue(station.getItem(4).isEmpty() && station.data.get(0) == 0, "Native parents wait for compost");
        station.setItem(0, new ItemStack(AEItems.WILD_SEED.get()));
        station.setItem(1, new ItemStack(AEItems.WILD_SEED.get()));
        cycle(helper, station);
        helper.assertTrue(station.getItem(4).is(AEItems.WILD_SEED.get()) && station.getItem(0).isEmpty() && station.getItem(1).isEmpty(),
                "Agri breeding still combines and consumes parents with optional compost");
        helper.succeed();
    }
}
