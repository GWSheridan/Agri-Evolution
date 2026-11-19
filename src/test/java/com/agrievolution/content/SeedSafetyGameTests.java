package com.agrievolution.content;

import com.agrievolution.genetics.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class SeedSafetyGameTests {
    @GameTest(template = "empty")
    public static void smallScreenViewportKeepsEveryEdgeReachable(GameTestHelper h) {
        var viewport = new com.agrievolution.client.NativeGuiViewport();
        viewport.init(320, 240, 410, 267);
        h.assertTrue(viewport.oversized() && viewport.x() == 4 && viewport.y() == 4, "Small screens start at the first controls");
        viewport.scroll(-100, -100, false);
        h.assertTrue(viewport.x() + 410 == 316 && viewport.y() + 267 == 220, "Bottom and right controls can be brought fully above the hint");
        h.assertTrue(viewport.keyPressed(262, 2) && viewport.keyPressed(264, 2), "Panning at a boundary consumes the key instead of changing pages");
        viewport.scroll(100, 100, false);
        h.assertTrue(viewport.x() == 4 && viewport.y() == 4, "Panning back restores exact integer hit coordinates");
        viewport.init(854, 480, 410, 267);
        h.assertFalse(viewport.oversized() || viewport.scroll(-1, -1, false), "Normal viewports retain their centred fixed layout");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void unnamedLootKeepsAllGenetics(GameTestHelper h) {
        var level = h.getLevel();
        var genes = new CropGenome(81, 67, 93);
        var lineage = new HybridLineageStorage.Lineage(CropSpecies.LETTUCE, CropSpecies.PEPPER);
        int x = 2;
        for (var species : new CropSpecies[]{CropSpecies.TOMATO, CropSpecies.DIAMOND}) {
            var pos = h.absolutePos(new BlockPos(x++, 220, 3));
            var block = ((SpeciesSeedItem) AEItems.seedFor(species)).cropBlock();
            level.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState(), 2);
            for (int age : new int[]{0, 7}) {
                var state = block.defaultBlockState().setValue(AgriCropBlock.AGE, age);
                level.setBlock(pos, state, 2);
                var plant = (AECropBlockEntity) level.getBlockEntity(pos);
                plant.setGenome(genes);
                plant.setRareTrait(RareTrait.VIGOROUS);
                plant.setLineage(lineage);
                var drops = Block.getDrops(state, level, pos, plant);
                var seeds = drops.stream().filter(s -> s.is(AEItems.seedFor(species))).toList();
                if (species == CropSpecies.DIAMOND && age == 7 || species == CropSpecies.TOMATO && age == 0)
                    h.assertTrue(!seeds.isEmpty(), "Existing seed loot remains present");
                h.assertTrue(seeds.stream().allMatch(s -> GenomeStorage.fromSeed(s).equals(genes)
                        && RareTraitStorage.fromSeed(s) == RareTrait.VIGOROUS
                        && lineage.equals(HybridLineageStorage.fromSeed(s)) && SeedVariety.name(s).isEmpty()),
                        "Every unnamed loot seed retains genes, trait and lineage at age " + age);
            }
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void sameSpeciesStationKeepsUnnamedHybridLineage(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(3, 220, 3));
        level.setBlock(pos, AEBlocks.SEED_BREEDING_STATION.get().defaultBlockState(), 2);
        var station = (SeedBreedingStationBlockEntity) level.getBlockEntity(pos);
        var seed = new ItemStack(AEItems.TOMATO_SEED.get());
        var lineage = new HybridLineageStorage.Lineage(CropSpecies.LETTUCE, CropSpecies.PEPPER);
        HybridLineageStorage.putOnSeed(seed, lineage);
        station.setItem(0, seed.copy());
        station.setItem(1, seed.copy());
        for (int i = 0; i < station.requiredTicks(); i++)
            SeedBreedingStationBlockEntity.serverTick(level, pos, station.getBlockState(), station);
        h.assertTrue(station.getItem(4).is(seed.getItem())
                && lineage.equals(HybridLineageStorage.fromSeed(station.getItem(4)))
                && SeedVariety.name(station.getItem(4)).isEmpty(), "Unnamed hybrid ancestry survives station breeding");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void storageAcceptsNativeAndOptedInSeeds(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(3, 220, 3));
        h.getLevel().setBlock(pos, AEBlocks.SEED_VAULT.get().defaultBlockState(), 2);
        var vault = (SeedVaultBlockEntity) h.getLevel().getBlockEntity(pos);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var menu = new SeedVaultMenu(1, player.getInventory(), vault);
        for (var item : new net.minecraft.world.item.Item[]{Items.WHEAT_SEEDS, Items.CARROT, Items.POTATO,
                Items.NETHER_WART, Items.COCOA_BEANS, Items.GLOW_BERRIES, Items.BAMBOO}) {
            var stack = new ItemStack(item);
            h.assertTrue(SeedBreedingParents.accepts(stack) && SeedBagItem.accepts(stack)
                    && menu.getSlot(0).mayPlace(stack), "Breeding and storage accept " + item);
        }
        h.assertFalse(SeedBagItem.accepts(new ItemStack(AEItems.SEED_BAG.get())), "Bags cannot contain bags");
        h.assertFalse(SeedBagItem.accepts(new ItemStack(Items.DIAMOND)), "Non-planting resources stay excluded");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void deniedPlantingAndVaultSettingsPreserveItems(GameTestHelper h) {
        var level = h.getLevel();
        var soil = h.absolutePos(new BlockPos(3, 220, 3));
        level.setBlock(soil, Blocks.FARMLAND.defaultBlockState(), 2);
        var player = h.makeMockPlayer(GameType.ADVENTURE);
        player.getAbilities().mayBuild = false;
        var seed = new ItemStack(AEItems.TOMATO_SEED.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, seed);
        var hit = new BlockHitResult(Vec3.atCenterOf(soil), Direction.UP, soil, false);
        h.assertTrue(seed.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)) == net.minecraft.world.InteractionResult.FAIL
                && seed.getCount() == 2 && level.getBlockState(soil.above()).isAir(), "Denied planting leaves the plot and stack intact");
        var pos = soil.east(2);
        level.setBlock(pos, AEBlocks.SEED_VAULT.get().defaultBlockState(), 2);
        var vault = (SeedVaultBlockEntity) level.getBlockEntity(pos);
        vault.setItem(0, seed.copy());
        player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        var menu = new SeedVaultMenu(1, player.getInventory(), vault);
        h.assertFalse(menu.clickMenuButton(player, 0), "Adventure players cannot change vault locks");
        h.assertTrue(!menu.locked(0) && vault.getItem(0).getCount() == 2, "Denied settings preserve locks and seeds");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void fullCreativeInventoryReceivesStarterBooksOnce(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.CREATIVE);
        player.getAbilities().instabuild = true;
        var pos = h.absolutePos(new BlockPos(3, 220, 3));
        player.setPos(Vec3.atCenterOf(pos));
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
        StarterProgressionEvents.giveBooks(player);
        StarterProgressionEvents.giveBooks(player);
        var books = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3)).stream().map(ItemEntity::getItem).toList();
        h.assertTrue(books.stream().filter(s -> s.is(AEItems.JOURNAL.get())).mapToInt(ItemStack::getCount).sum() == 1
                && books.stream().filter(s -> s.is(AEItems.BREEDING_JOURNAL.get())).mapToInt(ItemStack::getCount).sum() == 1,
                "Full Creative inventory drops each starter book exactly once");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void greenhouseSettingsRejectAdventurePlayers(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(3, 225, 3));
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        var builder = h.makeMockPlayer(GameType.SURVIVAL);
        var column = new ItemStack(AEItems.VERTICAL_GREENHOUSE.get());
        builder.setItemInHand(InteractionHand.MAIN_HAND, column);
        column.useOn(new UseOnContext(builder, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false)));
        var greenhouse = (VerticalGreenhouseBlockEntity) level.getBlockEntity(pos);
        var visitor = h.makeMockPlayer(GameType.ADVENTURE);
        visitor.getAbilities().mayBuild = false;
        visitor.setPos(Vec3.atCenterOf(pos));
        var menu = new VerticalGreenhouseMenu(1, visitor.getInventory(), greenhouse);
        int flags = greenhouse.data.get(1);
        h.assertFalse(menu.clickMenuButton(visitor, VerticalGreenhouseMenu.TOGGLE_GROW), "Visitor cannot change greenhouse configuration");
        h.assertTrue(greenhouse.data.get(1) == flags && greenhouse.complete(), "Rejected action preserves the complete machine and settings");
        h.succeed();
    }
}
