package com.agrievolution.client;

import com.agrievolution.content.AERecipes;
import com.agrievolution.content.ResourceWorkshopMenu;
import com.agrievolution.content.ResourceWorkshopRecipe;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

public final class ResourceWorkshopScreen extends ScrollableGardenScreen<ResourceWorkshopMenu> {
    private static final int INK = 0xFF3B2A20, PAPER = 0xFFF1DEB6, GREEN = 0xFF234334, WOOD = 0xFF2E1D15;
    private static final int PREVIEW_Y = 61;
    private static final int[] PREVIEW_X = {51, 101, 186};
    private List<RecipeHolder<ResourceWorkshopRecipe>> recipes = List.of();
    private List<ItemStack> shown = List.of();
    private int page;

    public ResourceWorkshopScreen(ResourceWorkshopMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 252;
        imageHeight = 238;
    }

    @Override protected void init() {
        super.init();
        if (minecraft.level != null) {
            recipes = minecraft.level.getRecipeManager().getAllRecipesFor(AERecipes.RESOURCE_WORKSHOP.get()).stream()
                    .sorted(Comparator.comparingInt((RecipeHolder<ResourceWorkshopRecipe> holder) -> holder.value().tier())
                            .thenComparing(RecipeHolder::id))
                    .toList();
        }
        addPageButton("‹", 14, -1);
        addPageButton("›", 216, 1);
    }

    private void addPageButton(String glyph, int x, int direction) {
        Button button = GardenGuiTheme.button(Component.literal(glyph), leftPos + x, topPos + 43, 21, 18,
                ignored -> page = Math.floorMod(page + direction, recipes.size()));
        button.active = recipes.size() > 1;
        addRenderableWidget(button);
    }

    @Override protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        GardenGuiTheme.panel(graphics, x, y, imageWidth, imageHeight, WOOD);
        GardenGuiTheme.panel(graphics, x + 7, y + 7, 238, 23, GREEN);
        GardenGuiTheme.panel(graphics, x + 7, y + 35, 238, 51, PAPER);
        GardenGuiTheme.panel(graphics, x + 7, y + 89, 238, 56, PAPER);
        GardenGuiTheme.panel(graphics, x + 7, y + 148, 238, 83, PAPER);
        graphics.drawCenteredString(font, label("title", grade(menu.tier())), x + 126, y + 15, PAPER);
        renderRecipe(graphics, x, y);
        text(graphics, label("material"), x + 43, y + 94);
        text(graphics, label("binder"), x + 94, y + 94);
        text(graphics, label("output"), x + 174, y + 94);
        graphics.fill(x + 133, y + 110, x + 177, y + 117, 0xFF6D4D37);
        int fill = (int) (42 * menu.progress());
        if (fill > 0) graphics.fill(x + 134, y + 111, x + 134 + fill, y + 116, 0xFF3B7954);
        text(graphics, label("status." + menu.status().name().toLowerCase(Locale.ROOT)), x + 43, y + 132);
        text(graphics, playerInventoryTitle, x + 46, y + 149);
        for (var slot : menu.slots) GardenGuiTheme.slot(graphics, x + slot.x, y + slot.y);
    }

    private void renderRecipe(GuiGraphics graphics, int x, int y) {
        if (recipes.isEmpty()) {
            graphics.drawCenteredString(font, label("no_recipes"), x + 126, y + 53, INK);
            return;
        }
        var recipe = recipes.get(page).value();
        long frame = minecraft.level == null ? 0 : minecraft.level.getGameTime() / 30;
        shown = List.of(example(recipe.material(), recipe.materialCount(), frame),
                example(recipe.binder(), recipe.binderCount(), frame), recipe.result());
        String name = font.plainSubstrByWidth(shown.get(2).getHoverName().getString(), 112);
        text(graphics, Component.literal(name), x + 44, y + 47);
        text(graphics, label("tier", grade(recipe.tier())), x + 165, y + 47);
        for (int i = 0; i < shown.size(); i++) {
            var item = shown.get(i);
            int px = x + PREVIEW_X[i];
            GardenGuiTheme.slot(graphics, px, y + PREVIEW_Y);
            graphics.renderItem(item, px, y + PREVIEW_Y);
            graphics.renderItemDecorations(font, item, px, y + PREVIEW_Y);
        }
        text(graphics, Component.literal("+"), x + 80, y + 66);
        text(graphics, Component.literal("→"), x + 147, y + 66);
    }

    private static ItemStack example(Ingredient ingredient, int count, long frame) {
        var choices = ingredient.getItems();
        return choices.length == 0 ? ItemStack.EMPTY : choices[Math.floorMod(frame, choices.length)].copyWithCount(count);
    }

    private void text(GuiGraphics graphics, Component component, int x, int y) {
        graphics.drawString(font, component, x, y, INK, false);
    }

    private static Component label(String key, Object... arguments) {
        return Component.translatable("screen.agri_evolution.resource_workshop." + key, arguments);
    }

    private static String grade(int tier) {
        return switch (tier) { case 2 -> "II"; case 3 -> "III"; default -> "I"; };
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        super.render(graphics, mouseX, mouseY, partial);
        renderTooltip(graphics, mouseX, mouseY);
        if (mouseY < topPos + PREVIEW_Y || mouseY >= topPos + PREVIEW_Y + 16) return;
        for (int i = 0; i < shown.size(); i++) {
            int x = leftPos + PREVIEW_X[i];
            if (mouseX >= x && mouseX < x + 16) graphics.renderTooltip(font, shown.get(i), mouseX, mouseY);
        }
    }
}
