package com.agrievolution.client;

import com.agrievolution.content.BatchCookingBlockEntity;
import com.agrievolution.content.BatchCookingMenu;
import com.agrievolution.content.CookingFoods;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class BatchCookingScreen extends AbstractContainerScreen<BatchCookingMenu> {
    private static final int PAPER = 0xffeeddb5, DARK = 0xff39281d, INK = 0xff35251b, GREEN = 0xff22533c;
    private final Button[] cancel = new Button[BatchCookingBlockEntity.QUEUE_LIMIT];

    public BatchCookingScreen(BatchCookingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 318;
        imageHeight = 264;
    }

    @Override protected void init() {
        super.init();
        button("<", 12, 34, 20, BatchCookingMenu.SELECT_PREVIOUS);
        button(">", 285, 34, 20, BatchCookingMenu.SELECT_NEXT);
        button("+1", 196, 74, 33, BatchCookingMenu.QUEUE_ONE);
        button("+4", 233, 74, 33, BatchCookingMenu.QUEUE_FOUR);
        button("+16", 270, 74, 35, BatchCookingMenu.QUEUE_SIXTEEN);
        for (int i = 0; i < cancel.length; i++) {
            int row = i;
            cancel[i] = addRenderableWidget(GardenGuiTheme.button(Component.literal("×"), leftPos + 13,
                    topPos + 110 + 17 * i, 14, 14, ignored -> send(menu.cancelAction(row))));
        }
    }

    private void button(String label, int x, int y, int width, int action) {
        addRenderableWidget(GardenGuiTheme.button(Component.literal(label), leftPos + x, topPos + y,
                width, 18, ignored -> send(action)));
    }

    private void send(int action) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
    }

    @Override protected void renderBg(GuiGraphics g, float delta, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        GardenGuiTheme.panel(g, x, y, imageWidth, imageHeight, DARK);
        GardenGuiTheme.panel(g, x + 7, y + 7, 304, 22, GREEN);
        GardenGuiTheme.panel(g, x + 7, y + 32, 304, 61, PAPER);
        GardenGuiTheme.panel(g, x + 7, y + 95, 74, 162, PAPER);
        GardenGuiTheme.panel(g, x + 84, y + 95, 227, 162, PAPER);
        g.drawCenteredString(font, title, x + 159, y + 14, PAPER);
        var dish = CookingFoods.ALL.get(menu.selection());
        var meal = new ItemStack(dish.item().get());
        g.renderItem(meal, x + 37, y + 35);
        g.drawString(font, meal.getHoverName(), x + 60, y + 40, INK, false);
        g.drawString(font, status(), x + 13, y + 79, INK, false);
        g.drawString(font, (menu.selection() + 1) + " / " + CookingFoods.ALL.size(), x + 248, y + 40, INK, false);
        var ingredients = previewIngredients();
        for (int i = 0; i < ingredients.size(); i++) {
            int px = x + 15 + i * 19;
            GardenGuiTheme.slot(g, px, y + 56);
            g.renderItem(ingredients.get(i), px, y + 56);
        }
        g.drawString(font, "Queue", x + 13, y + 100, INK, false);
        g.drawString(font, "Ingredients", x + 89, y + 99, INK, false);
        g.drawString(font, "Finished meals", x + 89, y + 146, INK, false);
        g.drawString(font, "Containers", x + 206, y + 146, INK, false);
        g.drawString(font, "Inventory", x + 89, y + 173, INK, false);
        for (var slot : menu.slots) GardenGuiTheme.slot(g, x + slot.x, y + slot.y);
        for (int row = 0; row < cancel.length; row++) {
            int id = menu.dish(row);
            cancel[row].visible = id >= 0;
            if (id < 0) continue;
            g.renderItem(new ItemStack(CookingFoods.ALL.get(id).item().get()), x + 31, y + 109 + 17 * row);
            g.drawString(font, "×" + menu.queued(row), x + 51, y + 113 + 17 * row, INK, false);
        }
    }

    private String status() {
        return switch (menu.status()) {
            case IDLE -> "Pick a recipe to queue";
            case COOKING -> "Cooking";
            case MISSING_INGREDIENTS -> "Waiting for ingredients";
            case OUTPUT_FULL -> "Clear an output slot";
            case RECIPE_UNAVAILABLE -> "Recipe unavailable";
        };
    }

    private java.util.List<ItemStack> previewIngredients() {
        var dish = CookingFoods.ALL.get(menu.selection());
        if (minecraft != null && minecraft.level != null) {
            var recipe = minecraft.level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("agri_evolution", dish.id()));
            if (recipe.isPresent()) {
                long frame = minecraft.level.getGameTime() / 35;
                return recipe.get().value().getIngredients().stream().map(ingredient -> ingredient.getItems())
                        .filter(choices -> choices.length > 0).limit(9)
                        .map(choices -> choices[Math.floorMod(frame, choices.length)]).toList();
            }
        }
        return java.util.Arrays.stream(dish.ingredients()).map(id -> new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id))))
                .limit(9).toList();
    }

    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {}
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
        int ingredient = Math.floorDiv(mouseX - leftPos - 15, 19);
        if (ingredient >= 0 && mouseY >= topPos + 56 && mouseY < topPos + 72) {
            var ingredients = previewIngredients();
            if (ingredient < ingredients.size()) g.renderTooltip(font, ingredients.get(ingredient), mouseX, mouseY);
        }
        int row = Math.floorDiv(mouseY - topPos - 109, 17);
        if (row >= 0 && row < cancel.length && mouseX >= leftPos + 31 && mouseX < leftPos + 74 && menu.dish(row) >= 0) {
            g.renderTooltip(font, new ItemStack(CookingFoods.ALL.get(menu.dish(row)).item().get()), mouseX, mouseY);
        }
    }
}
