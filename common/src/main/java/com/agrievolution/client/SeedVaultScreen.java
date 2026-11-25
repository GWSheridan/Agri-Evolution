package com.agrievolution.client;

import com.agrievolution.content.SeedVaultMenu;
import com.agrievolution.content.SpeciesSeedItem;
import com.agrievolution.genetics.GenomeStorage;
import com.agrievolution.genetics.RareTrait;
import com.agrievolution.genetics.RareTraitStorage;
import com.agrievolution.genetics.SeedVariety;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class SeedVaultScreen extends com.agrievolution.client.ScrollableGardenScreen<SeedVaultMenu> {
    private static final int PAPER = 0xFFF1DFB8, INK = 0xFF342519, GREEN = 0xFF285947, MUTED = 0xFF806849;
    private EditBox search;
    private String query = "";
    private int compareA = -1, compareB = -1;
    private Button speciesFilter;
    private final Button[] minimumFilters = new Button[3];
    private static final String[] TRAITS = {"Growth", "Yield", "Resilience"};

    public SeedVaultScreen(SeedVaultMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 320;
        imageHeight = 214;
    }

    @Override protected void init() {
        super.init();
        search = new EditBox(font, leftPos + 21, topPos + 35, 161, 16, Component.literal("Search seeds"));
        search.setHint(Component.literal("Search name, species, or trait"));
        search.setMaxLength(50);
        search.setValue(query);
        search.setResponder(value -> query = value);
        addRenderableWidget(search);
        speciesFilter = intakeButton(192, 140, 111, () -> {
            var carried = menu.getCarried();
            if (carried.isEmpty()) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, SeedVaultMenu.CLEAR_SPECIES);
            else if (carried.getItem() instanceof SpeciesSeedItem) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, SeedVaultMenu.SET_SPECIES);
        });
        speciesFilter.setTooltip(Tooltip.create(Component.literal("Click with a seed on your cursor to filter by its species. Click with an empty cursor to clear.")));
        for (int i = 0; i < minimumFilters.length; i++) {
            int trait = i;
            minimumFilters[i] = intakeButton(192 + 38 * i, 162, 35, () ->
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, SeedVaultMenu.CYCLE_MINIMUM + trait));
            minimumFilters[i].setTooltip(Tooltip.create(Component.literal("Minimum " + TRAITS[i] + " for greenhouse offspring. Click to increase by 10. Zero accepts any value.")));
        }
    }

    private Button intakeButton(int x, int y, int width, Runnable action) {
        return addRenderableWidget(new Button(leftPos + x, topPos + y, width, 18, Component.empty(),
                button -> action.run(), narration -> narration.get()) {
            @Override protected void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float delta) {
                GardenGuiTheme.panel(gui, getX(), getY(), getWidth(), getHeight(), isHoveredOrFocused() ? 0xFF397454 : GREEN);
                gui.drawCenteredString(font, getMessage(), getX() + getWidth() / 2, getY() + 5, PAPER);
            }
        });
    }

    @Override protected void renderBg(GuiGraphics gui, float delta, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        GardenGuiTheme.panel(gui, x, y, imageWidth, imageHeight, 0xFF302419);
        GardenGuiTheme.panel(gui, x + 8, y + 7, 304, 23, GREEN);
        gui.drawCenteredString(font, title, x + imageWidth / 2, y + 14, PAPER);
        GardenGuiTheme.panel(gui, x + 8, y + 31, 304, 83, PAPER);
        GardenGuiTheme.panel(gui, x + 8, y + 118, 304, 88, PAPER);
        gui.drawString(font, "Inventory", x + 21, y + 121, INK, false);
        for (var slot : menu.slots) GardenGuiTheme.slot(gui, x + slot.x, y + slot.y);
        gui.drawString(font, "Compare seeds", x + 193, y + 36, GREEN, false);
        comparison(gui, "A", compareA, y + 48);
        comparison(gui, "B", compareB, y + 78);
        gui.drawString(font, "Greenhouse intake", x + 193, y + 124, INK, false);
        gui.drawString(font, "Middle-click compares", x + 193, y + 185, MUTED, false);
        gui.drawString(font, "Shift + right-click locks", x + 193, y + 195, MUTED, false);
    }

    private void comparison(GuiGraphics gui, String label, int index, int y) {
        int x = leftPos + 193;
        gui.drawString(font, label, x, y, GREEN, false);
        ItemStack stack = index < 0 ? ItemStack.EMPTY : menu.getSlot(index).getItem();
        if (stack.isEmpty()) {
            gui.drawString(font, "Select a seed", x + 12, y, MUTED, false);
            return;
        }
        String name = stack.getItem() instanceof SpeciesSeedItem seed && SeedVariety.name(stack).isEmpty()
                ? seed.species().displayName() : stack.getHoverName().getString();
        if (font.width(name) > 100) name = font.plainSubstrByWidth(name, 91) + "...";
        gui.drawString(font, name, x + 12, y, INK, false);
        if (stack.getItem() instanceof SpeciesSeedItem) {
            var genes = GenomeStorage.fromSeed(stack);
            var trait = RareTraitStorage.fromSeed(stack);
            gui.drawString(font, "G " + genes.growth() + "  Y " + genes.yield() + "  R " + genes.resilience(), x + 12, y + 10, INK, false);
            if (trait != RareTrait.NONE) gui.drawString(font, trait.name(), x + 12, y + 19, GREEN, false);
        } else gui.drawString(font, "Native seed", x + 12, y + 10, MUTED, false);
    }

    private boolean matches(int index, ItemStack stack) {
        String text = query.strip().toLowerCase(Locale.ROOT);
        if (text.isEmpty()) return true;
        if (stack.isEmpty()) return false;
        if (text.equals("locked")) return menu.locked(index);
        var rare = RareTraitStorage.fromSeed(stack);
        if (text.equals("rare")) return rare != RareTrait.NONE;
        if (text.contains(">=")) {
            var pieces = text.split(">=", 2);
            if (!(stack.getItem() instanceof SpeciesSeedItem)) return false;
            var genes = GenomeStorage.fromSeed(stack);
            int value = switch (pieces[0].strip()) {
                case "growth" -> genes.growth();
                case "yield" -> genes.yield();
                case "resilience" -> genes.resilience();
                default -> -1;
            };
            try { return value >= 0 && value >= Integer.parseInt(pieces[1].strip()); }
            catch (NumberFormatException ignored) { return false; }
        }
        return stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(text)
                || BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().contains(text)
                || rare != RareTrait.NONE && rare.name().toLowerCase(Locale.ROOT).contains(text);
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        if (search.isMouseOver(x, y)) {
            setFocused(search);
            search.setFocused(true);
            return search.mouseClicked(x, y, button);
        }
        search.setFocused(false);
        setFocused(null);
        for (int index = 0; index < SeedVaultMenu.SIZE; index++) {
            var slot = menu.getSlot(index);
            if (x < leftPos + slot.x || x >= leftPos + slot.x + 16 || y < topPos + slot.y || y >= topPos + slot.y + 16) continue;
            if (button == 2 && slot.hasItem()) {
                if (index == compareA) compareA = -1;
                else if (index == compareB) compareB = -1;
                else if (compareA < 0 || compareB >= 0) { compareA = index; compareB = -1; }
                else compareB = index;
                return true;
            }
            if (button == 1 && hasShiftDown() && minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, index);
                return true;
            }
            break;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (search.isFocused() && key != org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            search.keyPressed(key, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {}

    @Override public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        var species = menu.intakeSpecies();
        speciesFilter.setMessage(Component.literal(species == null ? "All species" : font.plainSubstrByWidth(species.displayName(), 103)));
        for (int i = 0; i < minimumFilters.length; i++) {
            minimumFilters[i].setMessage(Component.literal(TRAITS[i].charAt(0) + " " + menu.minimum(i)));
        }
        super.render(gui, mouseX, mouseY, delta);
        for (int index = 0; index < SeedVaultMenu.SIZE; index++) {
            var slot = menu.getSlot(index);
            int x = leftPos + slot.x, y = topPos + slot.y;
            if (!matches(index, slot.getItem())) gui.fill(x, y, x + 16, y + 16, 0xA6302419);
            if (menu.locked(index)) gui.fill(x, y, x + 4, y + 4, 0xFFFFBE63);
            if (index == compareA || index == compareB) gui.drawString(font, index == compareA ? "A" : "B", x + 1, y + 1, PAPER);
        }
        renderTooltip(gui, mouseX, mouseY);
    }
}
