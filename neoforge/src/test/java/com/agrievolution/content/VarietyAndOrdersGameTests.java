package com.agrievolution.content;

import com.agrievolution.farming.CompanionPlanting;
import com.agrievolution.farming.SoilSurveyData;
import com.agrievolution.genetics.CropGenome;
import com.agrievolution.genetics.CropSpecies;
import com.agrievolution.genetics.GenomeStorage;
import com.agrievolution.genetics.HybridLineageStorage;
import com.agrievolution.genetics.SeedVariety;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class VarietyAndOrdersGameTests {
    @GameTest(template = "empty")
    public static void namedSeedsSurvivePlantingAndHarvest(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(4, 245, 4));
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        level.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7), 2);
        var seed = new ItemStack(AEItems.TOMATO_SEED.get(), 2);
        var genes = new CropGenome(35, 74, 52);
        var parents = new HybridLineageStorage.Lineage(CropSpecies.LETTUCE, CropSpecies.PEPPER);
        GenomeStorage.putOnSeed(seed, genes);
        HybridLineageStorage.putOnSeed(seed, parents);
        SeedVariety.apply(seed, "Ruby Row");
        useOn(player, seed, pos.below());
        var crop = (AECropBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(crop != null && "Ruby Row".equals(crop.variety()), "Sowing transfers the variety to the crop");
        var journal = new ItemStack(AEItems.BREEDING_JOURNAL.get());
        useOn(player, journal, pos);
        var records = SeedVariety.records(journal.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag());
        helper.assertTrue(records.size() == 1 && records.getFirst().name().equals("Ruby Row")
                && records.getFirst().best().equals(genes) && parents.equals(records.getFirst().parents()),
                "Grandmas Journal records the variety, genes, and known parents");
        var offspring = new ItemStack(AEItems.TOMATO_SEED.get());
        SeedVariety.inherit(offspring, seed, seed.copy());
        helper.assertTrue(SeedVariety.name(offspring).equals("Ruby Row") && parents.equals(HybridLineageStorage.fromSeed(offspring)),
                "Matching parent varieties pass their name and lineage to offspring of the same species");
        var immature = level.getBlockState(pos);
        helper.assertTrue(Block.getDrops(immature, level, pos, crop).stream()
                .anyMatch(drop -> drop.is(AEItems.TOMATO_SEED.get()) && SeedVariety.name(drop).equals("Ruby Row")),
                "An early harvest returns the named seed");
        var mature = immature.setValue(AgriCropBlock.AGE, 7);
        level.setBlock(pos, mature, 2);
        level.removeBlock(pos, false);
        ((AgriCropBlock) mature.getBlock()).playerDestroy(level, player, pos, mature, crop, ItemStack.EMPTY);
        var dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1)).stream()
                .map(ItemEntity::getItem).filter(stack -> stack.is(AEItems.TOMATO_SEED.get())).toList();
        helper.assertTrue(!dropped.isEmpty() && dropped.stream().allMatch(stack -> "Ruby Row".equals(SeedVariety.name(stack))
                && GenomeStorage.fromSeed(stack).equals(genes)), "Mature harvest returns the named line with its original genes");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void companionsUseAdjacentPlots(GameTestHelper helper) {
        var level = helper.getLevel();
        var tomato = helper.absolutePos(new BlockPos(4, 245, 4));
        var basil = tomato.east();
        var beans = tomato.south(2);
        var corn = beans.east();
        for (var pos : new BlockPos[]{tomato, basil, beans, corn}) {
            level.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7), 2);
        }
        level.setBlock(tomato, AEBlocks.TOMATO_CROP.get().defaultBlockState(), 2);
        var basilState = AEBlocks.BASIL_CROP.get().defaultBlockState();
        level.setBlock(basil, basilState.setValue(AgriCropBlock.AGE, 3), 2);
        helper.assertTrue(CompanionPlanting.weeds(level, tomato, CropSpecies.TOMATO, 10) == 10, "Young basil leaves the chance alone");
        level.setBlock(basil, basilState.setValue(AgriCropBlock.AGE, 4), 2);
        helper.assertTrue(CompanionPlanting.weeds(level, tomato, CropSpecies.TOMATO, 10) == 20, "Established basil halves the weed chance");
        var survey = SoilSurveyData.survey(level, tomato.below(), false);
        helper.assertTrue(survey.getString("Companion").contains("Basil"), "The saved plot report includes companion status");
        var ripe = AEBlocks.BEANS_CROP.get().defaultBlockState().setValue(AgriCropBlock.AGE, 7);
        level.setBlock(beans, ripe, 2);
        level.setBlock(corn, AEBlocks.CORN_CROP.get().defaultBlockState(), 2);
        var cornPlant = (AECropBlockEntity) level.getBlockEntity(corn);
        int before = cornPlant.soilNutrition();
        ((AgriCropBlock) ripe.getBlock()).playerDestroy(level, helper.makeMockPlayer(GameType.SURVIVAL), beans,
                ripe, level.getBlockEntity(beans), ItemStack.EMPTY);
        helper.assertTrue(cornPlant.soilNutrition() == before + 8, "Harvesting ripe beans feeds the adjacent corn");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void seedAndDatapackOrdersDeliver(GameTestHelper helper) {
        var level = helper.getLevel();
        var board = helper.absolutePos(new BlockPos(4, 245, 4));
        level.setBlock(board, AEBlocks.GARDEN_ORDER_BOARD.get().defaultBlockState(), 2);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(board.getX() + 0.5, board.getY(), board.getZ() + 1.5);
        var inventory = player.getInventory();
        var menu = new GardenWorkMenu(1, inventory, ContainerLevelAccess.create(level, board), false);
        helper.assertTrue(menu.clickMenuButton(player, menu.pageAction(GardenWorkMenu.SEED_PAGE)), "Seed commissions have a board page");
        var low = new ItemStack(AEItems.TOMATO_SEED.get(), 4);
        GenomeStorage.putOnSeed(low, new CropGenome(40, 69, 40));
        var high = new ItemStack(AEItems.TOMATO_SEED.get(), 4);
        GenomeStorage.putOnSeed(high, new CropGenome(40, 70, 40));
        inventory.setItem(0, low);
        inventory.setItem(1, high.copyWithCount(2));
        helper.assertFalse(menu.clickMenuButton(player, menu.action(0)), "An incomplete qualified order consumes no seeds");
        helper.assertTrue(inventory.getItem(0).getCount() == 4 && inventory.getItem(1).getCount() == 2, "Both stacks remain intact");
        inventory.setItem(2, high.copyWithCount(2));
        helper.assertTrue(menu.clickMenuButton(player, menu.action(0)) && menu.claimed(0), "Qualified seeds can be delivered across stacks");
        helper.assertTrue(inventory.getItem(0).getCount() == 4 && inventory.countItem(AEItems.NUTRIENT_COMPOST.get()) == 12
                && GardenWork.commissions(player) == 0, "The other seeds and commission progress are preserved");
        inventory.setItem(1, high.copy());
        helper.assertFalse(menu.clickMenuButton(player, menu.action(0)), "A seed commission can be claimed only once");

        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            GardenOrderReload.write(buffer, GardenOrderReload.current());
            var decoded = GardenOrderReload.read(buffer);
            helper.assertTrue(decoded.size() == 1 && decoded.getFirst().title().equals("Test Beet Delivery")
                    && decoded.getFirst().ingredients().getFirst().stack().is(Items.BEETROOT)
                    && decoded.getFirst().reward().stack().is(Items.EMERALD), "Datapack orders load and serialize for clients");
        } finally {
            buffer.release();
        }
        helper.assertTrue(menu.clickMenuButton(player, menu.pageAction(GardenWorkMenu.CUSTOM_PAGE)), "Datapacks enable the Custom tab");
        inventory.setItem(3, new ItemStack(Items.BEETROOT, 6));
        helper.assertTrue(menu.clickMenuButton(player, menu.action(0)) && menu.clickMenuButton(player, menu.action(0))
                && inventory.getItem(3).isEmpty() && inventory.countItem(Items.EMERALD) == 4, "Custom orders deliver and repeat");
        helper.assertTrue(GardenWork.completed(player, 0) == 0, "Custom orders have independent progression");
        helper.succeed();
    }

    private static void useOn(Player player, ItemStack stack, BlockPos pos) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
    }
}
