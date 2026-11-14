package com.agrievolution.content;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class FarmWorkflowGameTests {
    @GameTest(template = "empty")
    public static void irrigationTraceFollowsLiveConnections(GameTestHelper helper) {
        var level = helper.getLevel();
        var head = helper.absolutePos(new BlockPos(4, 220, 4));
        var pipe = head.east();
        var valve = head.east(2);
        var basin = head.east(3);
        level.setBlock(head, AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(), 3);
        level.setBlock(pipe, AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(), 3);
        level.setBlock(valve, AEBlocks.IRRIGATION_VALVE.get().defaultBlockState(), 3);
        level.setBlock(basin, AEBlocks.IRRIGATION_BASIN.get().defaultBlockState().setValue(IrrigationBasinBlock.FILLED, true), 3);
        var ready = IrrigationNetwork.trace(level, head);
        helper.assertTrue(ready.flow() == IrrigationNetwork.Flow.READY && ready.blocks().equals(List.of(head, pipe, valve, basin))
                        && level.getBlockState(basin).getValue(IrrigationBasinBlock.FILLED), "Trace finds the actual source without consuming it");
        IrrigationPipeBlock.toggle(level, pipe, Direction.WEST);
        var disabled = IrrigationNetwork.trace(level, head);
        helper.assertTrue(disabled.flow() == IrrigationNetwork.Flow.DISABLED_PORT && disabled.disabledSide() == Direction.WEST
                        && disabled.blocks().getLast().equals(pipe), "The blocked pipe face is identified at its actual position");
        IrrigationPipeBlock.toggle(level, pipe, Direction.WEST);
        level.setBlock(valve.above(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        var closed = IrrigationNetwork.trace(level, head);
        helper.assertTrue(closed.flow() == IrrigationNetwork.Flow.CLOSED_VALVE && closed.blocks().getLast().equals(valve),
                "The trace stops at a powered valve");
        level.removeBlock(valve.above(), false);
        level.setBlock(basin, level.getBlockState(basin).setValue(IrrigationBasinBlock.FILLED, false), 3);
        helper.assertTrue(IrrigationNetwork.trace(level, head).flow() == IrrigationNetwork.Flow.EMPTY, "The reachable source is empty");
        level.removeBlock(basin, false);
        helper.assertTrue(IrrigationNetwork.trace(level, head).flow() == IrrigationNetwork.Flow.DISCONNECTED, "Missing source is diagnosed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ordersAndPreservingUsePlayerInventory(GameTestHelper helper) {
        var level = helper.getLevel();
        var board = helper.absolutePos(new BlockPos(4, 225, 4));
        var station = board.east();
        level.setBlock(board, AEBlocks.GARDEN_ORDER_BOARD.get().defaultBlockState(), 3);
        level.setBlock(station, AEBlocks.PRESERVING_STATION.get().defaultBlockState(), 3);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(board.getX() + 0.5, board.getY(), board.getZ() + 1.5);
        var inventory = player.getInventory();
        var orders = new GardenWorkMenu(1, inventory, ContainerLevelAccess.create(level, board), false);
        var preserving = new GardenWorkMenu(2, inventory, ContainerLevelAccess.create(level, station), true);

        orders.clickMenuButton(player, orders.pageAction(1));
        helper.assertFalse(orders.unlocked(0), "Mid-game orders begin locked");
        orders.clickMenuButton(player, orders.pageAction(0));
        inventory.add(new ItemStack(AEItems.TOMATO.get(), 6));
        helper.assertFalse(orders.clickMenuButton(player, orders.action(0)), "Incomplete orders cannot consume ingredients");
        helper.assertTrue(count(player, AEItems.TOMATO.get()) == 6, "A failed delivery leaves inventory intact");
        inventory.clearContent();
        for (int row = 0; row < 2; row++) {
            orders.clickMenuButton(player, orders.pageAction(row));
            for (int order = 0; order < 2; order++) {
                for (var ingredient : orders.job(order).ingredients()) inventory.add(ingredient.stack());
                int button = orders.action(order);
                helper.assertTrue(orders.clickMenuButton(player, button), "A complete order delivers");
                helper.assertFalse(orders.clickMenuButton(player, button), "A repeat order still requires another full set of goods");
            }
        }
        orders.clickMenuButton(player, orders.pageAction(2));
        helper.assertTrue(orders.unlocked(0) && orders.unlocked(2) && count(player, Items.EMERALD) == 6, "Completing four orders unlocks all late-game choices");
        int lateAction = orders.action(0);
        orders.clickMenuButton(player, orders.pageAction(3));
        var commission = orders.job(0);
        for (var ingredient : commission.ingredients()) inventory.add(ingredient.stack());
        helper.assertFalse(orders.clickMenuButton(player, lateAction), "A button from a different tab cannot submit a commission");
        helper.assertTrue(orders.clickMenuButton(player, orders.action(0))
                        && count(player, AEItems.NUTRIENT_COMPOST.get()) == 12, "A commission awards its listed supplies");
        for (var ingredient : commission.ingredients()) inventory.add(ingredient.stack());
        helper.assertTrue(orders.claimed(0) && !orders.clickMenuButton(player, orders.action(0)), "Commissions can only be claimed once");
        for (int row = 0; row < 3; row++) {
            var job = preserving.job(row);
            for (var ingredient : job.ingredients()) inventory.add(ingredient.stack());
            helper.assertTrue(preserving.clickMenuButton(player, preserving.action(row))
                            && count(player, job.reward().item().get()) == 1 && count(player, Items.GLASS_BOTTLE) == 0,
                    "One preserve consumes its listed ingredients and bottle");
        }
        var nextPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
        var next = new net.minecraft.nbt.CompoundTag();
        GardenWork.copyOrders(player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG), next);
        nextPlayer.getPersistentData().put(Player.PERSISTED_NBT_TAG, next);
        helper.assertTrue(GardenWork.completed(nextPlayer, 0) == 2 && GardenWork.completed(nextPlayer, 1) == 2
                        && GardenWork.commissions(nextPlayer) == 1, "Completed orders and commissions carry across respawns");
        level.removeBlock(station, false);
        for (var ingredient : preserving.job(0).ingredients()) inventory.add(ingredient.stack());
        helper.assertFalse(preserving.clickMenuButton(player, preserving.action(0)), "Removed stations reject old menu clicks");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void newCraftingInputsHaveOneMatch(GameTestHelper helper) {
        checkCraft(helper, 3, AEItems.GARDEN_ORDER_BOARD.get(), List.of(
                stack(Items.COPPER_INGOT), stack(Items.PAPER), stack(Items.COPPER_INGOT),
                stack(Items.BIRCH_PLANKS), stack(Items.ITEM_FRAME), stack(Items.BIRCH_PLANKS),
                ItemStack.EMPTY, stack(Items.STICK), ItemStack.EMPTY), false);
        checkCraft(helper, 3, AEItems.PRESERVING_STATION.get(), List.of(
                stack(Items.GLASS_BOTTLE), stack(Items.COPPER_INGOT), stack(Items.GLASS_BOTTLE),
                stack(Items.SPRUCE_PLANKS), stack(Items.CAMPFIRE), stack(Items.SPRUCE_PLANKS),
                stack(Items.SPRUCE_PLANKS), stack(Items.IRON_INGOT), stack(Items.SPRUCE_PLANKS)), false);
        checkCraft(helper, 3, AEItems.COMPOST_BIN.get(), List.of(
                stack(Items.OAK_PLANKS), ItemStack.EMPTY, stack(Items.OAK_PLANKS),
                stack(Items.OAK_PLANKS), stack(Items.COMPOSTER), stack(Items.OAK_PLANKS),
                stack(Items.OAK_PLANKS), stack(Items.COPPER_INGOT), stack(Items.OAK_PLANKS)), false);
        checkCraft(helper, 3, AEItems.SEED_VAULT.get(), List.of(
                stack(Items.COPPER_INGOT), stack(Items.GLASS_PANE), stack(Items.COPPER_INGOT),
                stack(Items.BIRCH_PLANKS), stack(Items.BARREL), stack(Items.BIRCH_PLANKS),
                stack(Items.BIRCH_PLANKS), stack(Items.IRON_INGOT), stack(Items.BIRCH_PLANKS)), false);
        checkCraft(helper, 2, GardenWork.job(false, 2, 0).ingredients().get(1).item().get(),
                List.of(stack(Items.WHEAT), stack(Items.EGG), stack(Items.SUGAR), stack(AEItems.BERRY_JAM.get())), true);
        checkCraft(helper, 3, GardenWork.job(false, 1, 0).ingredients().getFirst().item().get(),
                List.of(stack(Items.BREAD), stack(AEItems.LETTUCE.get()), stack(AEItems.TOMATO.get()),
                        stack(AEItems.DILL_PICKLES.get()), stack(AEItems.RADISH.get()), ItemStack.EMPTY), true);
        checkCraft(helper, 2, GardenWork.job(false, 1, 1).ingredients().getFirst().item().get(),
                List.of(stack(Items.BOWL), stack(AEItems.LEEK.get()), stack(Items.BAKED_POTATO), stack(AEItems.GARDEN_SEASONING.get())), true);
        helper.succeed();
    }

    private static void checkCraft(GameTestHelper helper, int width, Item result, List<ItemStack> stacks, boolean returnBottle) {
        var level = helper.getLevel();
        var input = CraftingInput.of(width, stacks.size() / width, stacks);
        var matches = level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream()
                .filter(recipe -> recipe.value().matches(input, level)).toList();
        helper.assertTrue(matches.size() == 1, "One match among all vanilla and Agri recipes for " + result);
        var recipe = matches.getFirst().value();
        helper.assertTrue(recipe.assemble(input, level.registryAccess()).is(result), "Correct crafting result");
        if (returnBottle) helper.assertTrue(recipe.getRemainingItems(input).stream().anyMatch(item -> item.is(Items.GLASS_BOTTLE)),
                "Using a preserve in a meal returns its bottle");
    }

    private static ItemStack stack(Item item) { return new ItemStack(item); }

    private static int count(Player player, Item item) {
        return player.getInventory().items.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }
}
