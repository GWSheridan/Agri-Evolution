package com.agrievolution.content;

import com.agrievolution.platform.ModInfo;
import java.util.Collections;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.agrievolution.content.ResourceWorkshopBlockEntity.*;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class ResourceWorkshopGameTests {
    private static final BlockPos POSITION = new BlockPos(1, 2, 1);

    @GameTest(template = "empty")
    public static void outputCapacityAndSaveRoundTrip(GameTestHelper test) {
        var workshop = place(test);
        workshop.setItem(MATERIAL, new ItemStack(Items.CLAY_BALL));
        workshop.setItem(BINDER, new ItemStack(Items.BONE_MEAL));
        workshop.setItem(OUTPUT, new ItemStack(AEItems.SOIL_CONDITIONER.get(), 63));
        run(workshop, 100);
        test.assertTrue(workshop.status() == Status.OUTPUT_FULL && workshop.progress() == 0, "A full output must pause");
        test.assertTrue(workshop.getItem(MATERIAL).getCount() == 1 && workshop.getItem(BINDER).getCount() == 1,
                "Blocked processing must preserve both inputs");
        workshop.setItem(OUTPUT, ItemStack.EMPTY);
        run(workshop, 37);
        var registries = test.getLevel().registryAccess();
        var saved = workshop.saveWithoutMetadata(registries);
        workshop.loadWithComponents(saved, registries);
        test.assertTrue(workshop.progress() == 37, "Work in progress survives serialization");
        run(workshop, 42);
        test.assertTrue(workshop.getItem(OUTPUT).isEmpty(), "Output waits until the last tick");
        run(workshop, 1);
        test.assertTrue(workshop.getItem(OUTPUT).is(AEItems.SOIL_CONDITIONER.get())
                && workshop.getItem(OUTPUT).getCount() == 2, "Processing creates two Soil Conditioner");
        test.assertTrue(workshop.getItem(MATERIAL).isEmpty() && workshop.getItem(BINDER).isEmpty(), "Inputs are consumed once");
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void upgradesGateRecipesAndKeepInventory(GameTestHelper test) {
        var level = test.getLevel();
        var workshop = place(test);
        var pos = workshop.getBlockPos();
        var block = AEBlocks.RESOURCE_WORKSHOP.get();
        var player = test.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(pos.south()));
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.SOUTH, pos, false);
        workshop.setItem(MATERIAL, new ItemStack(Items.BLAZE_ROD));
        workshop.setItem(BINDER, new ItemStack(Items.MAGMA_CREAM));
        run(workshop, 1);
        test.assertTrue(workshop.status() == Status.TIER_REQUIRED && workshop.progress() == 0, "Mk I cannot work a Mk III recipe");
        var mastery = new ItemStack(AEItems.WORKSHOP_MASTERY_UPGRADE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, mastery);
        block.useItemOn(mastery, workshop.getBlockState(), level, pos, player, InteractionHand.MAIN_HAND, hit);
        test.assertTrue(mastery.getCount() == 1 && workshop.tier() == 1, "Mk III cannot skip Mk II");

        var refinement = new ItemStack(AEItems.WORKSHOP_REFINEMENT_UPGRADE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, refinement);
        block.useItemOn(refinement, workshop.getBlockState(), level, pos, player, InteractionHand.MAIN_HAND, hit);
        test.assertTrue(refinement.isEmpty() && workshop.tier() == 2, "Mk II installs once");
        player.setItemInHand(InteractionHand.OFF_HAND, mastery);
        block.useItemOn(mastery, workshop.getBlockState(), level, pos, player, InteractionHand.OFF_HAND, hit);
        test.assertTrue(mastery.isEmpty() && workshop.tier() == 3 && level.getBlockEntity(pos) == workshop,
                "Mk III keeps the placed block entity");
        run(workshop, 160);
        test.assertTrue(workshop.getItem(OUTPUT).is(AEItems.NETHER_HEAT_CORE.get()), "Saved inputs complete at Mk III");
        test.assertTrue(workshop.canPlaceItemThroughFace(MATERIAL, new ItemStack(Items.ENDER_PEARL), Direction.UP)
                        && workshop.canPlaceItemThroughFace(BINDER, new ItemStack(Items.POPPED_CHORUS_FRUIT), Direction.SOUTH)
                        && workshop.canTakeItemThroughFace(OUTPUT, workshop.getItem(OUTPUT), Direction.DOWN)
                        && !workshop.canTakeItemThroughFace(MATERIAL, ItemStack.EMPTY, Direction.DOWN),
                "Automation routes material, binder and output through separate faces");
        test.succeed();
    }

    @GameTest(template = "empty")
    public static void craftingRecipesHaveUniqueMatches(GameTestHelper test) {
        String[] ids = {"resource_workshop", "workshop_refinement_upgrade", "workshop_mastery_upgrade",
                "compost_mulch_from_conditioner", "irrigation_pipe_from_couplings", "soil_research_refinement_from_lens",
                "irrigation_controller_from_brackets", "greenhouse_smart_farming_from_heat_core", "soil_research_mastery_from_relay"};
        var manager = test.getLevel().getRecipeManager();
        for (String id : ids) {
            var key = ModInfo.id(id);
            var recipe = (CraftingRecipe) manager.byKey(key).orElseThrow().value();
            var ingredients = recipe.getIngredients();
            if (recipe instanceof ShapedRecipe shaped) {
                for (int y = 0; y <= 3 - shaped.getHeight(); y++) {
                    for (int x = 0; x <= 3 - shaped.getWidth(); x++) {
                        for (boolean mirror : new boolean[]{false, true}) {
                            var grid = NonNullList.withSize(9, ItemStack.EMPTY);
                            for (int row = 0; row < shaped.getHeight(); row++) {
                                for (int col = 0; col < shaped.getWidth(); col++) {
                                    int source = row * shaped.getWidth() + (mirror ? shaped.getWidth() - 1 - col : col);
                                    var choices = ingredients.get(source).getItems();
                                    if (choices.length != 0) grid.set((y + row) * 3 + x + col, choices[0].copy());
                                }
                            }
                            assertRecipe(test, id, grid);
                        }
                    }
                }
            } else {
                var grid = NonNullList.withSize(9, ItemStack.EMPTY);
                for (int i = 0; i < ingredients.size(); i++) grid.set(i, ingredients.get(i).getItems()[0].copy());
                for (int offset = 0; offset < 9; offset++) {
                    assertRecipe(test, id, grid);
                    Collections.rotate(grid, 1);
                }
            }
        }
        test.succeed();
    }

    private static void assertRecipe(GameTestHelper test, String id, NonNullList<ItemStack> grid) {
        var level = test.getLevel();
        var matches = level.getRecipeManager().getRecipesFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, grid), level);
        test.assertTrue(matches.size() == 1 && matches.getFirst().id().equals(ModInfo.id(id)), "Recipe conflict for " + id);
    }

    private static ResourceWorkshopBlockEntity place(GameTestHelper test) {
        test.setBlock(POSITION, AEBlocks.RESOURCE_WORKSHOP.get().defaultBlockState());
        return (ResourceWorkshopBlockEntity) test.getLevel().getBlockEntity(test.absolutePos(POSITION));
    }

    private static void run(ResourceWorkshopBlockEntity workshop, int ticks) {
        for (int tick = 0; tick < ticks; tick++) workshop.process();
    }
}
