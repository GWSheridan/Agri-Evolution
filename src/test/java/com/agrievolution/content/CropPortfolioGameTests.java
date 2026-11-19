package com.agrievolution.content;

import com.agrievolution.client.journal.MachineGuidePages;
import com.agrievolution.client.journal.MachineRecipeCatalog;
import com.agrievolution.genetics.CropSpecies;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class CropPortfolioGameTests {
    @GameTest(template = "empty")
    public static void everyCropHasConsistentFieldAndGreenhouseHarvests(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(4, 260, 4));
        level.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState(), 2);
        for (var species : CropSpecies.values()) {
            var item = AEItems.seedFor(species);
            h.assertTrue(item instanceof SpeciesSeedItem, "Registered planting item for " + species);
            var seed = new ItemStack(item);
            var harvest = VerticalGreenhouseHarvests.forSeed(seed);
            h.assertTrue(harvest != null && !harvest.output().isEmpty(), "A valid machine harvest exists for " + species);
            var crop = ((SpeciesSeedItem) item).cropBlock();
            var state = crop.defaultBlockState().setValue(AgriCropBlock.AGE, 7);
            level.setBlock(pos, state, 2);
            var drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos));
            h.assertTrue(drops.stream().anyMatch(drop -> drop.is(harvest.product())),
                    "Field and greenhouse harvest the same product for " + species);
            if (species.isResource()) h.assertTrue(harvest.product() == com.agrievolution.farming.ResourceCropRules.produce(species),
                    "Resource progression product agrees with the crop rules for " + species);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void journalsKeepCropsAndMachinesInTheirOwnBooks(GameTestHelper h) {
        Set<String> plants = Arrays.stream(MachineGuidePages.forBook(false)).map(MachineGuidePages.Page::title).collect(Collectors.toSet());
        Set<String> machines = Arrays.stream(MachineGuidePages.forBook(true)).map(MachineGuidePages.Page::title).collect(Collectors.toSet());
        for (String title : new String[]{"FIRST WILD SEEDS", "FOOD GARDEN", "SEED VARIETIES", "COMPANION PLANTING", "CROP MILESTONES"})
            h.assertTrue(plants.contains(title) && !machines.contains(title), "Crop guide belongs in Grandmas Journal: " + title);
        for (String title : new String[]{"IRRIGATION RESERVES", "IRRIGATION CONTROLLER", "BATCH COOKING", "LUNCH BOX", "SEED VAULT", "GENETIC CLIPPERS"})
            h.assertTrue(machines.contains(title) && !plants.contains(title), "Machine/tool guide belongs in Journal: " + title);
        for (int i = 0; i < MachineRecipeCatalog.ALL.length; i++) {
            var recipe = MachineRecipeCatalog.ALL[i];
            String page = "CRAFT " + i;
            boolean food = MachineRecipeCatalog.isFood(recipe.id());
            h.assertTrue(plants.contains(page) == food && machines.contains(page) != food,
                    "Illustrated recipe is in the correct book: " + recipe.id());
        }
        h.succeed();
    }
}
