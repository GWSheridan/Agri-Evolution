import com.agrievolution.client.journal.BreedingGuidePages;
import com.agrievolution.client.journal.MachineGuidePages;
import com.agrievolution.client.journal.JournalNavigation;
import com.agrievolution.genetics.CropSpecies;
import com.agrievolution.genetics.SpeciesBreeder;
import com.agrievolution.genetics.AdvancedHybridBreeder;

/** Tests page navigation and that recipe tables correspond to Java breeding rules. */
public class JournalProgressionSmoke {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static void field(CropSpecies a, CropSpecies b, CropSpecies result, int chance) {
        SpeciesBreeder.MutationRecipe recipe = SpeciesBreeder.mutationRecipe(a, b);
        check(recipe != null && recipe.offspring() == result && recipe.baseChance() == chance,
                "Incorrect field breeding recipe: " + a + "/" + b);
    }
    private static void lab(CropSpecies a, CropSpecies b, CropSpecies result, int chance) {
        AdvancedHybridBreeder.HybridRecipe recipe = AdvancedHybridBreeder.recipe(a, b);
        check(recipe != null && recipe.offspring() == result && recipe.baseChance() == chance,
                "Incorrect advanced breeding recipe: " + a + "/" + b);
    }
    public static void main(String[] args) {
        check(MachineGuidePages.count() == 36, "Expected 36 instruction pages");
        check(BreedingGuidePages.count() == 18, "Expected 18 breeding pages");
        check(JournalNavigation.Section.MACHINES.title().equals("Instructions"), "Tab not renamed");
        JournalNavigation nav = new JournalNavigation();
        nav.section(JournalNavigation.Section.BREEDING);
        nav.cycleBreedingPage(-1);
        check(nav.breedingPage() == 17, "Breeding previous wrap");
        nav.cycleBreedingPage(1);
        check(nav.breedingPage() == 0, "Breeding next wrap");
        nav.cycleMachinePage(-1);
        check(nav.machinePage() == 35, "Instruction previous wrap");
        nav.cycleMachinePage(1);
        check(nav.machinePage() == 0, "Instruction next wrap");
        for (int i = 0; i < BreedingGuidePages.count(); i++) {
            BreedingGuidePages.Page page = BreedingGuidePages.get(i);
            check(page.left().length <= 6 && page.right().length <= 6, "Breeding page overflow " + i);
            for (String s : page.left()) check(s.length() <= 23, "Long breeding left: " + s);
            for (String s : page.right()) check(s.length() <= 23, "Long breeding right: " + s);
        }
        for (int i = 0; i < MachineGuidePages.count(); i++) {
            MachineGuidePages.Page page = MachineGuidePages.get(i);
            check(page.left().length <= 6 && page.right().length <= 6, "Instructions overflow " + i);
            for (String s : page.left()) check(s.length() <= 23, "Long instructions left: " + s);
            for (String s : page.right()) check(s.length() <= 23, "Long instructions right: " + s);
        }
        field(CropSpecies.WILD, CropSpecies.CRIMSON_CLOVER, CropSpecies.AMBER_MARIGOLD, 20);
        field(CropSpecies.WILD, CropSpecies.AZURE_FLAX, CropSpecies.SILVER_THISTLE, 20);
        field(CropSpecies.CRIMSON_CLOVER, CropSpecies.AZURE_FLAX, CropSpecies.VIOLET_STARFLOWER, 25);
        field(CropSpecies.AMBER_MARIGOLD, CropSpecies.VIOLET_STARFLOWER, CropSpecies.SUNFIRE_LILY, 15);
        field(CropSpecies.SILVER_THISTLE, CropSpecies.VIOLET_STARFLOWER, CropSpecies.MOON_ORCHID, 15);
        field(CropSpecies.DIRT, CropSpecies.WOOD, CropSpecies.STONE, 28);
        field(CropSpecies.STONE, CropSpecies.WOOD, CropSpecies.COAL, 25);
        field(CropSpecies.STONE, CropSpecies.COAL, CropSpecies.COPPER, 22);
        field(CropSpecies.COPPER, CropSpecies.COAL, CropSpecies.IRON, 18);
        field(CropSpecies.IRON, CropSpecies.COPPER, CropSpecies.REDSTONE, 18);
        field(CropSpecies.IRON, CropSpecies.REDSTONE, CropSpecies.GOLD, 15);
        field(CropSpecies.GOLD, CropSpecies.REDSTONE, CropSpecies.LAPIS, 13);
        lab(CropSpecies.SUNFIRE_LILY, CropSpecies.MOON_ORCHID, CropSpecies.ECLIPSE_LOTUS, 12);
        lab(CropSpecies.AMBER_MARIGOLD, CropSpecies.SILVER_THISTLE, CropSpecies.TWILIGHT_SAGE, 16);
        lab(CropSpecies.GOLD, CropSpecies.LAPIS, CropSpecies.DIAMOND, 10);
        lab(CropSpecies.DIAMOND, CropSpecies.LAPIS, CropSpecies.EMERALD, 8);
        lab(CropSpecies.COAL, CropSpecies.LAPIS, CropSpecies.QUARTZ, 12);
        field(CropSpecies.DIRT,CropSpecies.STONE,CropSpecies.GRAVEL,22);
        field(CropSpecies.GRAVEL,CropSpecies.DIRT,CropSpecies.SAND,20);
        field(CropSpecies.SAND,CropSpecies.DIRT,CropSpecies.CLAY,20);
        field(CropSpecies.GRAVEL,CropSpecies.COAL,CropSpecies.FLINT,18);
        lab(CropSpecies.REDSTONE,CropSpecies.QUARTZ,CropSpecies.GLOWSTONE,12);
        lab(CropSpecies.DIAMOND,CropSpecies.QUARTZ,CropSpecies.OBSIDIAN,8);

        lab(CropSpecies.QUARTZ,CropSpecies.OBSIDIAN,CropSpecies.NETHERRACK,14);
        lab(CropSpecies.NETHERRACK,CropSpecies.SAND,CropSpecies.SOUL_SAND,16);
        lab(CropSpecies.NETHERRACK,CropSpecies.GLOWSTONE,CropSpecies.MAGMA_CREAM,12);
        lab(CropSpecies.SOUL_SAND,CropSpecies.CRIMSON_CLOVER,CropSpecies.NETHER_WART,14);
        lab(CropSpecies.MAGMA_CREAM,CropSpecies.NETHER_WART,CropSpecies.BLAZE_ROD,10);
        lab(CropSpecies.OBSIDIAN,CropSpecies.EMERALD,CropSpecies.END_STONE,12);
        lab(CropSpecies.END_STONE,CropSpecies.AZURE_FLAX,CropSpecies.CHORUS_FRUIT,12);
        lab(CropSpecies.END_STONE,CropSpecies.CHORUS_FRUIT,CropSpecies.ENDER_PEARL,8);
        lab(CropSpecies.ENDER_PEARL,CropSpecies.CHORUS_FRUIT,CropSpecies.SHULKER_SHELL,6);
        lab(CropSpecies.BLAZE_ROD,CropSpecies.OBSIDIAN,CropSpecies.ANCIENT_DEBRIS,7);
        lab(CropSpecies.ANCIENT_DEBRIS,CropSpecies.ENDER_PEARL,CropSpecies.NETHERITE,4);
        System.out.println("PASS: 36 instruction pages, 18 breeding pages, recipe checks and navigation");
    }
}
