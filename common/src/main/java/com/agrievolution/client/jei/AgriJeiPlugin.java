package com.agrievolution.client.jei;

import com.agrievolution.platform.ModInfo;
import com.agrievolution.client.journal.IllustratedBreedingRecipes;
import com.agrievolution.content.AEItems;
import com.agrievolution.content.GardenWork;
import com.agrievolution.content.VerticalGreenhouseBlockEntity;
import com.agrievolution.content.VerticalGreenhouseHarvests;
import com.agrievolution.farming.ResourceCropRules;
import com.agrievolution.genetics.CropSpecies;
import java.util.Arrays;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public final class AgriJeiPlugin implements IModPlugin {
    private static final RecipeType<Entry> FIELD = type("field_breeding");
    private static final RecipeType<Entry> LAB = type("lab_breeding");
    private static final RecipeType<Entry> PRESERVING = type("preserving");
    private static final RecipeType<Entry> HARVEST = type("greenhouse_harvest");

    private record Entry(List<ItemStack> inputs, ItemStack output, String method, String growingHint) {}

    @Override public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(ModInfo.ID, "jei");
    }

    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        var gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new Category(FIELD, Component.literal("Field Breeding"), gui.createDrawableItemStack(new ItemStack(AEItems.BREEDING_JOURNAL.get()))),
                new Category(LAB, Component.literal("Genetics Laboratory"), gui.createDrawableItemStack(new ItemStack(AEItems.BOTANICAL_GENETICS_LABORATORY.get()))),
                new Category(PRESERVING, Component.literal("Preserving Station"), gui.createDrawableItemStack(new ItemStack(AEItems.PRESERVING_STATION.get()))),
                new Category(HARVEST, Component.literal("Vertical Greenhouse"), gui.createDrawableItemStack(new ItemStack(AEItems.VERTICAL_GREENHOUSE.get()))));
    }

    @Override public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(FIELD, breedingRecipes(IllustratedBreedingRecipes.Method.FIELD));
        registration.addRecipes(LAB, breedingRecipes(IllustratedBreedingRecipes.Method.LAB));
        registration.addRecipes(PRESERVING, GardenWork.preservingRecipes().stream().map(job ->
                new Entry(job.ingredients().stream().map(GardenWork.Ingredient::stack).toList(),
                        job.reward().stack(), "Use ingredients from your inventory.", "")).toList());
        registration.addRecipes(HARVEST, Arrays.stream(CropSpecies.values()).map(species -> {
            var seed = new ItemStack(AEItems.seedFor(species));
            var harvest = VerticalGreenhouseHarvests.forSeed(seed);
            return new Entry(List.of(seed), new ItemStack(harvest.product(), harvest.count()),
                    VerticalGreenhouseBlockEntity.HARVEST_WATER + " mB water per harvest",
                    harvest.seconds() + " seconds with base genetics. The seed stays in its tray.");
        }).toList());
    }

    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(FIELD, AEItems.BREEDING_JOURNAL.get(), AEItems.SEED_BREEDING_STATION.get());
        registration.addRecipeCatalysts(LAB, AEItems.BOTANICAL_GENETICS_LABORATORY.get());
        registration.addRecipeCatalysts(PRESERVING, AEItems.PRESERVING_STATION.get());
        registration.addRecipeCatalysts(HARVEST, AEItems.VERTICAL_GREENHOUSE.get());
    }

    private static RecipeType<Entry> type(String id) {
        return RecipeType.create(ModInfo.ID, id, Entry.class);
    }

    private static List<Entry> breedingRecipes(IllustratedBreedingRecipes.Method method) {
        return IllustratedBreedingRecipes.ALL.stream().filter(recipe -> recipe.method() == method).map(recipe -> {
            var first = new ItemStack(AEItems.seedFor(recipe.first()));
            var second = new ItemStack(AEItems.seedFor(recipe.second()));
            boolean lab = method == IllustratedBreedingRecipes.Method.LAB;
            var inputs = lab ? List.of(first, second, new ItemStack(AEItems.NUTRIENT_COMPOST.get())) : List.of(first, second);
            String instructions = (lab ? "Tier 3 Lab" : "Field / Breeding Station") + " | Base chance: " + recipe.chance() + "%";
            String hint = recipe.result().isResource() ? ResourceCropRules.catalystHint(recipe.result()) : "Grow on hydrated farmland.";
            return new Entry(inputs, new ItemStack(AEItems.seedFor(recipe.result())), instructions, hint);
        }).toList();
    }

    private record Category(RecipeType<Entry> type, Component title, IDrawable icon) implements IRecipeCategory<Entry> {
        @Override public RecipeType<Entry> getRecipeType() { return type; }
        @Override public Component getTitle() { return title; }
        @Override public IDrawable getIcon() { return icon; }
        @Override public int getWidth() { return 158; }
        @Override public int getHeight() { return 74; }

        @Override public void setRecipe(IRecipeLayoutBuilder builder, Entry recipe, IFocusGroup focuses) {
            for (int i = 0; i < recipe.inputs().size(); i++) {
                builder.addInputSlot(10 + 22 * i, 10).addItemStack(recipe.inputs().get(i));
            }
            builder.addOutputSlot(126, 10).addItemStack(recipe.output());
        }

        @Override public void draw(Entry recipe, IRecipeSlotsView slots, GuiGraphics gui, double mouseX, double mouseY) {
            var font = Minecraft.getInstance().font;
            gui.drawString(font, "→", 109, 14, 0xFF463728, false);
            int y = 31;
            for (var line : font.split(Component.literal(recipe.method() + (recipe.growingHint().isEmpty() ? "" : "\n" + recipe.growingHint())), 146)) {
                gui.drawString(font, line, 6, y, 0xFF463728, false);
                y += 10;
            }
        }
    }
}
