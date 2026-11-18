package com.agrievolution.client;

import com.agrievolution.content.BatchCookingBlockEntity;
import com.agrievolution.content.BatchCookingMenu;
import com.agrievolution.content.CookingFoods;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntSupplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class BatchCookingScreen extends AbstractContainerScreen<BatchCookingMenu> {
    private static final int PAPER = 0xffeeddb5, DARK = 0xff39281d, INK = 0xff35251b, GREEN = 0xff22533c;
    private static final Tooltip FULL_QUEUE = Tooltip.create(Component.literal("The queue already has eight batches."));
    private static final Tooltip NO_RECIPE = Tooltip.create(Component.literal("This recipe is unavailable at the station."));
    private final Button[] cancel = new Button[BatchCookingBlockEntity.QUEUE_LIMIT];
    private final Button[] orderButtons = new Button[3];
    private final int[] shownCancelActions = new int[BatchCookingBlockEntity.QUEUE_LIMIT];
    private List<ItemStack> shownIngredients = List.of();
    private int shownRecipe;

    public BatchCookingScreen(BatchCookingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 318;
        imageHeight = 264;
    }

    @Override protected void init() {
        super.init();
        button("<", 12, 34, 20, () -> BatchCookingMenu.SELECT_PREVIOUS);
        button(">", 285, 34, 20, () -> BatchCookingMenu.SELECT_NEXT);
        orderButtons[0] = button("+1", 196, 74, 33, () -> BatchCookingMenu.queueAction(shownRecipe, 0));
        orderButtons[1] = button("+4", 233, 74, 33, () -> BatchCookingMenu.queueAction(shownRecipe, 1));
        orderButtons[2] = button("+16", 270, 74, 35, () -> BatchCookingMenu.queueAction(shownRecipe, 2));
        for (var button : orderButtons) button.active = false;
        for (int i = 0; i < cancel.length; i++) {
            int row = i;
            cancel[i] = addRenderableWidget(GardenGuiTheme.button(Component.literal("×"), leftPos + 13,
                    topPos + 110 + 17 * i, 14, 14, ignored -> send(shownCancelActions[row])));
            cancel[i].visible = false;
        }
    }

    private Button button(String label, int x, int y, int width, IntSupplier action) {
        return addRenderableWidget(GardenGuiTheme.button(Component.literal(label), leftPos + x, topPos + y,
                width, 18, ignored -> send(action.getAsInt())));
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
        shownRecipe = menu.selection();
        var dish = CookingFoods.ALL.get(shownRecipe);
        var meal = new ItemStack(dish.item().get());
        g.renderItem(meal, x + 37, y + 35);
        g.drawString(font, font.plainSubstrByWidth(meal.getHoverName().getString(), 180), x + 60, y + 40, INK, false);
        g.drawString(font, status(), x + 13, y + 79, INK, false);
        g.drawString(font, (shownRecipe + 1) + " / " + CookingFoods.ALL.size(), x + 248, y + 40, INK, false);
        shownIngredients = previewIngredients(dish);
        for (int i = 0; i < shownIngredients.size(); i++) {
            int px = x + 15 + i * 19;
            GardenGuiTheme.slot(g, px, y + 56);
            g.renderItem(shownIngredients.get(i), px, y + 56);
        }
        boolean full = menu.queueFull();
        boolean available = menu.selectedRecipeAvailable();
        g.drawString(font, full ? "Queue full" : "Queue", x + 13, y + 100, INK, false);
        for (var button : orderButtons) {
            button.active = available && !full;
            button.setTooltip(!available ? NO_RECIPE : full ? FULL_QUEUE : null);
        }
        g.drawString(font, "Ingredients", x + 89, y + 99, INK, false);
        g.drawString(font, "Finished meals", x + 89, y + 146, INK, false);
        g.drawString(font, "Containers", x + 206, y + 146, INK, false);
        g.drawString(font, "Inventory", x + 89, y + 173, INK, false);
        for (var slot : menu.slots) GardenGuiTheme.slot(g, x + slot.x, y + slot.y);
        for (int row = 0; row < cancel.length; row++) {
            int id = menu.dish(row);
            cancel[row].visible = id >= 0;
            if (id < 0) continue;
            shownCancelActions[row] = menu.cancelAction(row);
            g.renderItem(new ItemStack(CookingFoods.ALL.get(id).item().get()), x + 31, y + 109 + 17 * row);
            g.drawString(font, "×" + menu.queued(row), x + 51, y + 113 + 17 * row, INK, false);
        }
    }

    private String status() {
        return switch (menu.status()) {
            case IDLE -> menu.selectedRecipeAvailable() ? "Pick a recipe to queue" : "Recipe unavailable";
            case QUEUED -> "Batch queued";
            case COOKING -> "Cooking";
            case MISSING_INGREDIENTS -> "Waiting for ingredients";
            case OUTPUT_FULL -> "Clear a finished meal slot";
            case CONTAINERS_FULL -> "Clear a container slot";
            case RECIPE_UNAVAILABLE -> "Recipe unavailable";
        };
    }

    private List<ItemStack> previewIngredients(CookingFoods.Dish dish) {
        if (minecraft != null && minecraft.level != null) {
            var level = minecraft.level;
            var recipe = BatchCookingBlockEntity.recipe(level.getRecipeManager(), level.registryAccess(), dish);
            if (recipe == null) return List.of();
            long frame = level.getGameTime() / 35;
            return recipe.getIngredients().stream().map(ingredient -> ingredient.getItems())
                    .filter(choices -> choices.length > 0)
                    .map(choices -> choices[Math.floorMod(frame, choices.length)]).toList();
        }
        return Arrays.stream(dish.ingredients()).map(id -> new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id))))
                .limit(9).toList();
    }

    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {}
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
        if (mouseX >= leftPos + 37 && mouseX < leftPos + 240 && mouseY >= topPos + 35 && mouseY < topPos + 51) {
            g.renderTooltip(font, new ItemStack(CookingFoods.ALL.get(shownRecipe).item().get()), mouseX, mouseY);
        }
        int relativeX = mouseX - leftPos - 15;
        if (relativeX >= 0 && relativeX % 19 < 16 && mouseY >= topPos + 56 && mouseY < topPos + 72) {
            int ingredient = relativeX / 19;
            if (ingredient < shownIngredients.size()) g.renderTooltip(font, shownIngredients.get(ingredient), mouseX, mouseY);
        }
        int row = Math.floorDiv(mouseY - topPos - 109, 17);
        if (row >= 0 && row < cancel.length && mouseX >= leftPos + 31 && mouseX < leftPos + 74 && menu.dish(row) >= 0) {
            g.renderTooltip(font, new ItemStack(CookingFoods.ALL.get(menu.dish(row)).item().get()), mouseX, mouseY);
        }
    }
}
