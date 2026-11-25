package com.agrievolution.client.greenhouse;

import com.agrievolution.client.GardenGuiTheme;

import com.agrievolution.content.BotanicalGreenhouseMenu;
import com.agrievolution.content.CropSupportItem;
import com.agrievolution.content.GreenhouseStickStatus;
import com.agrievolution.content.GreenhouseWeedStatus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Compact eight-tab dashboard with native-pixel text and matching input coordinates. */
public final class BotanicalGreenhouseScreen extends com.agrievolution.client.ScrollableGardenScreen<BotanicalGreenhouseMenu> {
    private static final int DARK = 0xFF281D17;
    private static final int GOLD = 0xFFC6A368;
    private static final int GREEN = 0xFF275841;
    private static final int PAPER = 0xFFF0DEB8;
    private static final int INK = 0xFF302518;
    private static final int RED = 0xFF9D463E;
    private static final int BLUE = 0xFF3C91AA;
    private static final int MUTED = 0xFF947A57;
    private static final int STICK_MAP_X = 359, STICK_MAP_Y = 101, STICK_CELL_PITCH = 19, STICK_CELL_SIZE = 17;
    private static final int MAP_TAB_Y = 81, MAP_TAB_WIDTH = 44, MAP_TAB_PITCH = 49, MAP_TAB_HEIGHT = 16;
    private static final String[] TABS = { "Overview", "Water", "Crops", "Enviro", "Climate", "Harvest", "Planting", "Monitor" };
    private int tab;
    private boolean showWeeds;

    public BotanicalGreenhouseScreen(BotanicalGreenhouseMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 410;
        imageHeight = 206;
    }

    // Map layout coordinates to native pixels; never shrink the font pose.
    private int px(int x) { return leftPos + Math.round((x-leftPos)*410f/482f); }
    private int py(int y) { return topPos + Math.round((y-topPos)*206f/228f); }
    private void panel(GuiGraphics g, int x, int y, int w, int h, int color) {
        GardenGuiTheme.panel(g, px(x), py(y), px(x+w)-px(x), py(y+h)-py(y), color);
    }

    private void text(GuiGraphics g, String line, int x, int y, int color) {
        g.drawString(font, com.agrievolution.client.journal.JournalFonts.body(line), px(x), py(y), color, false);
    }

    private void centered(GuiGraphics g, String line, int midX, int y, int color) {
        var text = com.agrievolution.client.journal.JournalFonts.body(line);
        g.drawString(font, text, px(midX)-font.width(text)/2, py(y), color, false);
    }

    private void bar(GuiGraphics g, int x, int y, int w, int value, int max, int color) {
        panel(g, x, y, w, 12, MUTED);
        g.fill(px(x + 2), py(y + 2), px(x + w - 2), py(y + 10), 0xFFD8C69F);
        if (max > 0 && value > 0) {
            int fill = Math.min(w - 4, (int) ((long) (w - 4) * value / max));
            g.fill(px(x + 2), py(y + 2), px(x + 2 + fill), py(y + 10), color);
        }
    }

    @Override protected void renderBg(GuiGraphics g, float delta, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        panel(g, x, y, 482, 228, DARK);
        panel(g, x + 8, y + 8, 482 - 16, 30, GREEN);
        text(g, "AGRI EVOLUTION", x + 17, y + 18, PAPER);
        text(g, "GREENHOUSE MANAGEMENT", x + 183, y + 18, PAPER);

        for (int i = 0; i < TABS.length; i++) {
            int tx = x + 10 + i * 57;
            boolean selected = tab == i;
            panel(g, tx, y + 43, 54, 22, selected ? PAPER : GREEN);
            centered(g, TABS[i], tx + 27, y + 50, selected ? INK : PAPER);
        }

        panel(g, x + 10, y + 72, 482 - 20, 145, PAPER);
        switch (tab) {
            case 1 -> irrigation(g, x, y);
            case 2 -> crops(g, x, y);
            case 3 -> environment(g, x, y);
            case 4 -> climate(g, x, y);
            case 5 -> automation(g, x, y);
            case 6 -> planting(g, x, y);
            case 7 -> monitoring(g, x, y);
            default -> overview(g, x, y);
        }
    }

    private void overview(GuiGraphics g, int x, int y) {
        var alerts=com.agrievolution.content.GreenhouseStatus.alerts(menu);
        text(g,alerts.isEmpty()?"GREENHOUSE READY":alerts.size()+" ALERTS: "+alerts.getFirst().name()+" "+alerts.getFirst().state()+" - click for help",x+22,y+83,alerts.isEmpty()?GREEN:RED);
        var indicators=com.agrievolution.content.GreenhouseStatus.indicators(menu);
        for(int i=0;i<indicators.size();i++) {
            var status=indicators.get(i);int cx=x+18+(i%3)*150,cy=y+99+(i/3)*43;
            int colour=switch(status.severity()){case 2->RED;case 1->0xFF987024;case 3->MUTED;default->GREEN;};
            panel(g,cx,cy,144,38,PAPER);
            g.fill(px(cx+2),py(cy+2),px(cx+5),py(cy+36),colour);
            var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(status.item()));
            g.renderItem(new net.minecraft.world.item.ItemStack(item),px(cx+9),py(cy+10));
            text(g,status.name(),cx+36,cy+6,INK);text(g,status.state(),cx+36,cy+22,colour);
        }
        text(g,"Click cards for controls; hover for help.",x+180,y+199,MUTED);
        text(g,"Water: "+menu.water()+" / "+menu.capacity()+" mB",x+22,y+199,menu.lowWater()?RED:GREEN);
    }

    private void irrigation(GuiGraphics g, int x, int y) {
        text(g, "AUTOMATIC IRRIGATION", x + 22, y + 83, GREEN);
        text(g, menu.active() ? "RUNNING - native farmland-water support" : "OFFLINE - no water support",
                x + 22, y + 101, menu.active() ? GREEN : RED);
        text(g, "Coverage: 5 x 5 internal farm area", x + 22, y + 119, INK);
        text(g, "Farmland hydrated: " + menu.hydrated() + " / " + menu.farmland(),
                x + 22, y + 135, INK);
        bar(g, x + 22, y + 151, 323, menu.hydrated(), Math.max(1, menu.farmland()), GREEN);
        text(g, "Consumption: 1 mB per second (active)", x + 22, y + 169, INK);
        text(g, "Available runtime: " + clock(menu.secondsRemaining()),
                x + 22, y + 185, menu.lowWater() ? RED : GREEN);
        text(g, menu.lowWater() ? "WARNING: supply another water bucket soon" :
                "Refill: use water buckets on the controller",
                x + 22, y + 202, menu.lowWater() ? RED : MUTED);
    }

    private void crops(GuiGraphics g, int x, int y) {
        text(g, "CROP PERFORMANCE", x + 22, y + 84, GREEN);
        text(g, "Planted: " + menu.planted() + " / 25", x + 22, y + 107, INK);
        text(g, "Growing: " + menu.growing(), x + 207, y + 107, GREEN);
        bar(g, x + 22, y + 122, 323, menu.planted(), 25, GREEN);
        text(g, "Mature: " + menu.mature(), x + 22, y + 144, INK);
        text(g, "Well-lit growing: " + menu.litCrops() + " / " + menu.growing(),
                x + 153, y + 144, INK);
        text(g, menu.active() ? "Bonus: " + menu.climateGrowthChance() + "% chance per crop every 5s"
                : "Bonus inactive until greenhouse is running", x + 22, y + 163,
                menu.active() ? GREEN : MUTED);
        text(g, "Only growing crops with light 9+ qualify", x + 22, y + 179, INK);
        text(g, "Breeding stations inside: " + menu.breedingStations(), x + 22, y + 197, INK);
        plotMap(g, x, y);
    }

    private void plotMap(GuiGraphics g, int x, int y) {
        int centerX = x + STICK_MAP_X + (STICK_CELL_PITCH * 4 + STICK_CELL_SIZE) / 2;
        for (int i = 0; i < 2; i++) {
            int tabX = x + STICK_MAP_X + i * MAP_TAB_PITCH;
            boolean selected = showWeeds == (i == 1);
            panel(g, tabX, y + MAP_TAB_Y, MAP_TAB_WIDTH, MAP_TAB_HEIGHT, selected ? PAPER : GREEN);
            centered(g, i == 0 ? "Sticks" : "Weeds", tabX + MAP_TAB_WIDTH / 2, y + MAP_TAB_Y + 4, selected ? INK : PAPER);
        }
        int attention = 0;
        for (int plot = 0; plot < GreenhouseStickStatus.PLOT_COUNT; plot++) {
            int cellX = x + STICK_MAP_X + plot % GreenhouseStickStatus.SIDE * STICK_CELL_PITCH;
            int cellY = y + STICK_MAP_Y + plot / GreenhouseStickStatus.SIDE * STICK_CELL_PITCH;
            String mark;
            int color;
            if (showWeeds) {
                var weeds = menu.weedStatus(plot);
                if (weeds == GreenhouseWeedStatus.WEEDY) attention++;
                mark = switch (weeds) {
                    case WEEDY -> "!";
                    case CLEAR -> "-";
                    case NO_CROP -> "";
                    case UNAVAILABLE -> "?";
                };
                color = switch (weeds) {
                    case WEEDY -> RED;
                    case CLEAR -> GREEN;
                    case NO_CROP -> 0xFFD7C59C;
                    case UNAVAILABLE -> MUTED;
                };
            } else {
                int status = menu.stickStatus(plot);
                int tier = GreenhouseStickStatus.tier(status);
                boolean dueSoon = GreenhouseStickStatus.dueSoon(status);
                if (dueSoon) attention++;
                mark = switch (tier) {
                    case 1 -> dueSoon ? "W!" : "W";
                    case 2 -> dueSoon ? "I!" : "I";
                    case 3 -> "O";
                    default -> status == GreenhouseStickStatus.UNAVAILABLE ? "?" : status == GreenhouseStickStatus.NO_CROP ? "" : "-";
                };
                color = dueSoon ? RED : switch (tier) {
                    case 1 -> 0xFF855D32;
                    case 2 -> 0xFF546574;
                    case 3 -> 0xFF533D68;
                    default -> status == GreenhouseStickStatus.NO_CROP ? 0xFFD7C59C : MUTED;
                };
            }
            g.fill(px(cellX), py(cellY), px(cellX + STICK_CELL_SIZE), py(cellY + STICK_CELL_SIZE), INK);
            g.fill(px(cellX) + 1, py(cellY) + 1, px(cellX + STICK_CELL_SIZE) - 1, py(cellY + STICK_CELL_SIZE) - 1, color);
            centered(g, mark, cellX + STICK_CELL_SIZE / 2, cellY + 4, PAPER);
        }
        centered(g, "CONTROLLER", centerX, y + 200, MUTED);
        text(g, (showWeeds ? "Weedy plots: " : "Sticks due soon: ") + attention, x + 220, y + 197, attention == 0 ? MUTED : RED);
    }

    private void renderPlotTooltip(GuiGraphics g, int mouseX, int mouseY) {
        for (int plot = 0; plot < GreenhouseStickStatus.PLOT_COUNT; plot++) {
            int col = plot % GreenhouseStickStatus.SIDE;
            int row = plot / GreenhouseStickStatus.SIDE;
            int x = leftPos + STICK_MAP_X + col * STICK_CELL_PITCH;
            int y = topPos + STICK_MAP_Y + row * STICK_CELL_PITCH;
            if (mouseX < px(x) || mouseX >= px(x + STICK_CELL_SIZE) || mouseY < py(y) || mouseY >= py(y + STICK_CELL_SIZE)) continue;
            String details = showWeeds ? switch (menu.weedStatus(plot)) {
                case WEEDY -> "Weeds present\nUse a rake on this plot";
                case CLEAR -> "Crop is clear of weeds";
                case NO_CROP -> "No weed-compatible crop";
                case UNAVAILABLE -> "Plot reading unavailable";
            } : stickDetails(menu.stickStatus(plot));
            var tip = Component.literal("Column " + (col + 1) + ", row " + (GreenhouseStickStatus.SIDE - row) + " from controller\n" + details);
            g.renderTooltip(font, font.split(tip, 220), mouseX, mouseY);
            return;
        }
    }

    private static String stickDetails(int status) {
        return switch (GreenhouseStickStatus.tier(status)) {
                case 1, 2 -> (GreenhouseStickStatus.tier(status) == 1 ? "Wood" : "Iron") + " sticks, "
                        + CropSupportItem.remainingTime(GreenhouseStickStatus.seconds(status) * 20) + " remaining"
                        + (GreenhouseStickStatus.dueSoon(status) ? "\nReplace soon" : "");
                case 3 -> "Obsidian sticks, permanent";
                default -> switch (status) {
                    case GreenhouseStickStatus.NO_CROP -> "No Agri Evolution crop";
                    case GreenhouseStickStatus.UNSUPPORTED -> "Crop has no sticks";
                    default -> "Plot reading unavailable";
                };
        };
    }

    private void environment(GuiGraphics g, int x, int y) {
        text(g, "GREENHOUSE CONDITIONS", x + 22, y + 84, GREEN);
        text(g, "Average inside light: " + menu.averageLight() + " / 15",
                x + 22, y + 107, INK);
        bar(g, x + 22, y + 124, 323, menu.averageLight(), 15,
                menu.averageLight() >= 9 ? GREEN : RED);
        text(g, menu.averageLight() >= 9 ? "GOOD LIGHT FOR CROP GROWTH" :
                "LOW LIGHT - extra crop ticks need light 9+",
                x + 22, y + 144, menu.averageLight() >= 9 ? GREEN : RED);
        text(g, "Glass shell: " + (menu.formed() ? "SEALED" : "INCOMPLETE"),
                x + 22, y + 164, menu.formed() ? GREEN : RED);
        text(g, "Irrigation: " + (menu.active() ? "ON" : "OFF"),
                x + 222, y + 164, menu.active() ? GREEN : RED);
        text(g, "Working farmland: " + menu.farmland() + " / 25", x + 22, y + 181, INK);
        text(g, "Conditions update automatically every 2 seconds", x + 22, y + 200, MUTED);
    }

    /** Shows actual gameplay climate effects and the two server-side toggles. */
    private void climate(GuiGraphics g, int x, int y) {
        text(g, "CLIMATE CONTROL", x + 22, y + 83, GREEN);
        text(g, "Temperature: " + menu.temperatureC() + " C", x + 22, y + 101, INK);
        text(g, "Outside: " + menu.outsideTemperatureC() + " C", x + 219, y + 101, MUTED);
        bar(g, x + 22, y + 115, 365, Math.max(0, menu.temperatureC() + 20), 75,
                menu.temperatureC() >= 19 && menu.temperatureC() <= 28 ? GREEN : RED);
        text(g, "Humidity: " + menu.humidityPct() + "%", x + 22, y + 132, INK);
        text(g, menu.active() ? "Growth bonus: " + menu.climateGrowthChance() + "% / 5s"
                : "Growth bonus: OFF", x + 190, y + 132,
                menu.active() ? GREEN : MUTED);
        // Buttons are rectangles handled in mouseClicked(), synced with vanilla menu packets.
        boolean heating = menu.heaterEnabled();
        boolean vent = menu.ventsEnabled();
        panel(g, x + 22, y + 149, 174, 21, heating ? GREEN : MUTED);
        centered(g, "Heater: " + (heating ? "ON" : "OFF"), x + 109, y + 155, PAPER);
        panel(g, x + 211, y + 149, 176, 21, vent ? GREEN : MUTED);
        centered(g, "Ventilation: " + (vent ? "ON" : "OFF"), x + 299, y + 155, PAPER);
        text(g, "Fuel: " + clock(menu.heaterFuelSeconds()) + " (coal +10m)", x + 22, y + 181,
                menu.heaterEnabled() && menu.heaterFuelSeconds() == 0 ? RED : INK);
        text(g, menu.active() ? "Optimal: 19-28 C, humidity 45-78%" :
                "Activate greenhouse to run the climate equipment", x + 22, y + 201,
                menu.active() ? GREEN : RED);
    }

    /** Module-based automation: deliberately locked on original 0.12 controllers. */
    private void automation(GuiGraphics g, int x, int y) {
        text(g, "AUTOMATED GREENHOUSE FARMING", x + 22, y + 83, GREEN);
        if (!menu.upgraded()) {
            text(g, "CONTROLLER: BASIC - AUTOMATION LOCKED", x + 22, y + 104, RED);
            text(g, "Craft a Greenhouse Automation Upgrade module.", x + 22, y + 130, INK);
            text(g, "Right-click your controller with the module.", x + 22, y + 149, INK);
            text(g, "Climate, irrigation and crop growth stay available.", x + 22, y + 179, MUTED);
            return;
        }
        text(g, "CONTROLLER: AUTOMATION UPGRADE INSTALLED", x + 22, y + 101, GREEN);
        text(g, "Total harvests: " + menu.harvestCount() + "    Replanted: " + menu.replantCount(),
                x + 22, y + 119, INK);
        panel(g, x + 22, y + 138, 128, 23, menu.harvesting() ? GREEN : MUTED);
        centered(g, "Harvest: " + (menu.harvesting() ? "ON" : "OFF"), x + 86, y + 145, PAPER);
        panel(g, x + 164, y + 138, 128, 23, menu.replanting() ? GREEN : MUTED);
        centered(g, "Replant: " + (menu.replanting() ? "ON" : "OFF"), x + 228, y + 145, PAPER);
        panel(g, x + 306, y + 138, 148, 23, GREEN);
        centered(g, "OPEN STORAGE", x + 380, y + 145, PAPER);
        text(g, "Storage slots: " + menu.storageUsed() + " / 27", x + 22, y + 175, INK);
        panel(g, x + 306, y + 168, 148, 23, GREEN);
        centered(g, "HARVEST SETTINGS", x + 380, y + 175, PAPER);
        text(g, menu.storageFull() ? "STORAGE FULL - remove items to resume harvesting" :
                (menu.active() ? "Harvests one mature crop every 4 seconds" : "Paused: greenhouse needs glass + water"),
                x + 22, y + 195, menu.storageFull() ? RED : MUTED);
    }

    /** Tier-three page keeps the already-working 0.13 Harvest page intact. */
    private void planting(GuiGraphics g, int x, int y) {
        text(g, "SMART SEED PLANTING", x + 22, y + 83, GREEN);
        if (!menu.upgraded()) {
            text(g, "CONTROLLER: BASIC", x + 22, y + 104, RED);
            text(g, "Install the Automation Upgrade first (Tier 2).", x + 22, y + 129, INK);
            text(g, "Then craft and install Smart Farming (Tier 3).", x + 22, y + 149, INK);
            text(g, "Your existing greenhouse remains unchanged.", x + 22, y + 183, MUTED);
            return;
        }
        if (!menu.smartFarmingUnlocked()) {
            text(g, "CONTROLLER: TIER 2 - PLANTING LOCKED", x + 22, y + 104, RED);
            text(g, "Craft the Greenhouse Smart Farming Upgrade.", x + 22, y + 130, INK);
            text(g, "Use it on your upgraded controller to unlock.", x + 22, y + 149, INK);
            text(g, "Harvesting and storage continue working.", x + 22, y + 180, MUTED);
            return;
        }
        text(g, "CONTROLLER: SMART FARMING (TIER 3)", x + 22, y + 101, GREEN);
        text(g, "Planted: " + menu.autoPlantedCount(), x + 342, y + 101, GREEN);
        text(g, "Seeds ready: " + menu.seedCount() + "     Empty plots: " + menu.emptyPlots(),
                x + 22, y + 121, INK);
        panel(g, x + 22, y + 138, 180, 23, menu.smartPlantEnabled() ? GREEN : MUTED);
        centered(g, "Auto-plant: " + (menu.smartPlantEnabled() ? "ON" : "OFF"), x + 112, y + 145, PAPER);
        panel(g, x + 213, y + 138, 218, 23, GREEN);
        centered(g, "OPEN SEED BANK", x + 322, y + 145, PAPER);
        // Two independent filters: both ON by default for existing worlds.
        panel(g, x + 22, y + 169, 180, 22, menu.flowerPlantingEnabled() ? GREEN : MUTED);
        centered(g, "Food / Flowers: " + (menu.flowerPlantingEnabled() ? "ON" : "OFF"),
                x + 112, y + 176, PAPER);
        panel(g, x + 213, y + 169, 218, 22, menu.resourcePlantingEnabled() ? GREEN : MUTED);
        centered(g, "Resource Crops: " + (menu.resourcePlantingEnabled() ? "ON" : "OFF"),
                x + 322, y + 176, PAPER);
        panel(g, x + 22, y + 195, 180, 20, menu.autoBreedEnabled() ? GREEN : MUTED);
        centered(g, "Auto-breed: " + (menu.autoBreedEnabled() ? "ON" : "OFF"),
                x + 112, y + 201, PAPER);
        text(g, "Bred: " + menu.autoBredCount() + "  " +
                (menu.autoBreedEnabled() && !menu.active() ? "Need glass + water" :
                 menu.autoBreedEnabled() ? menu.autoBreedStatus() : "Breeding OFF"),
                x + 213, y + 201, menu.autoBreedEnabled() ? GREEN : MUTED);
    }

    /** Diagnostic page only: this tab never changes an automation setting. */
    private void monitoring(GuiGraphics g, int x, int y) {
        text(g, "SMART GREENHOUSE MONITORING", x + 22, y + 83, GREEN);
        if (!menu.smartFarmingUnlocked()) {
            text(g, "Monitoring unlocks with Smart Farming (Tier 3).", x + 22, y + 110, RED);
            text(g, "Your existing harvest and irrigation settings are safe.", x + 22, y + 142, MUTED);
            return;
        }
        String state = !menu.active() ? "PAUSED - glass / water needed"
                : !menu.smartPlantEnabled() ? "PAUSED - auto-plant switched off"
                : menu.emptyPlots() == 0 ? (menu.farmland()==0?"WAITING - add farmland beneath plots":"WAITING - clear crops / blocks above farmland")
                : menu.monitorEligible() == 0 ? "WAITING - no eligible seeds"
                : "RUNNING - seed selection active";
        boolean running = menu.active() && menu.smartPlantEnabled()
                && menu.emptyPlots() > 0 && menu.monitorEligible() > 0;
        text(g, "Current: " + state, x + 22, y + 101, running ? GREEN : MUTED);
        int resultColor = menu.monitorLastResult() == 1 ? GREEN :
                menu.monitorLastResult() == 0 ? MUTED : RED;
        text(g, "Last cycle: " + menu.monitorResultLabel(), x + 22, y + 118, resultColor);
        // Keep the DNA snapshot separate from the most recent attempt: skipped cycles
        // must not replace the genetics of a crop actually planted in the greenhouse.
        if (menu.hasLastPlantedCrop()) {
            text(g, "Last auto-planted: " + menu.lastPlantedCropName() +
                    "   Bank slot: " + menu.lastPlantedSlot(), x + 22, y + 135, GREEN);
            text(g, "Genetics: Growth " + menu.lastPlantedGrowth() +
                    "  Yield " + menu.lastPlantedYield() +
                    "  Resilience " + menu.lastPlantedResilience() +
                    "  Quality " + menu.lastPlantedQuality(), x + 22, y + 151, INK);
            text(g, "Rare trait: " + menu.lastPlantedRareTrait().description(),
                    x + 22, y + 167,
                    menu.lastPlantedRareTrait() == com.agrievolution.genetics.RareTrait.NONE ? MUTED : GREEN);
        } else {
            text(g, "Last auto-planted: No successful planting recorded", x + 22, y + 135, MUTED);
            text(g, "Seed Bank: hover slots to see planting exclusions", x + 22, y + 151, INK);
            text(g, "Rare trait: -", x + 22, y + 167, MUTED);
        }
        text(g, "Recent: " + menu.monitorAttempts() + "  Planted: " +
                menu.monitorSuccessful() + "  Skipped: " + menu.monitorSkipped() +
                "  Efficiency: " + menu.monitorEfficiency() + "%", x + 22, y + 184, INK);
        text(g, "Seed slots - ready: " + menu.monitorEligible() + "  disabled: " +
                menu.monitorDisabled() + "  reserved: " + menu.monitorReserved() +
                "  filtered: " + menu.monitorFiltered(), x + 22, y + 201, MUTED);
    }

    private static String clock(int seconds) {
        int hours = seconds / 3600;
        int minutes = seconds % 3600 / 60;
        int rest = seconds % 60;
        return String.format(java.util.Locale.ROOT, "%d:%02d:%02d", hours, minutes, rest);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double actualX=mouseX, actualY=mouseY;
        if (button == 0 && tab == 2 && mouseY >= py(topPos + MAP_TAB_Y) && mouseY < py(topPos + MAP_TAB_Y + MAP_TAB_HEIGHT)) {
            for (int i = 0; i < 2; i++) {
                int tabX = leftPos + STICK_MAP_X + i * MAP_TAB_PITCH;
                if (mouseX >= px(tabX) && mouseX < px(tabX + MAP_TAB_WIDTH)) {
                    showWeeds = i == 1;
                    return true;
                }
            }
        }
        mouseX=leftPos+(mouseX-leftPos)*482.0/410.0;
        mouseY=topPos+(mouseY-topPos)*228.0/206.0;
        if(button==0&&tab==0){
            var alerts=com.agrievolution.content.GreenhouseStatus.alerts(menu);
            if(!alerts.isEmpty()&&mouseX>=leftPos+18&&mouseX<leftPos+460&&mouseY>=topPos+78&&mouseY<topPos+96){openStatus(alerts.getFirst().name());return true;}
            var cards=com.agrievolution.content.GreenhouseStatus.indicators(menu);
            for(int i=0;i<cards.size();i++){int x=leftPos+18+(i%3)*150,y=topPos+99+(i/3)*43;
                if(mouseX>=x&&mouseX<x+144&&mouseY>=y&&mouseY<y+38){openStatus(cards.get(i).name());return true;}}
        }
        if (button == 0 && mouseY >= topPos + 43 && mouseY < topPos + 65) {
            for (int i = 0; i < TABS.length; i++) {
                int bx = leftPos + 10 + i * 57;
                if (mouseX >= bx && mouseX < bx + 54) {
                    tab = i;
                    return true;
                }
            }
        }
        if (button == 0 && tab == 6 && menu.smartFarmingUnlocked()
                && mouseY >= topPos + 138 && mouseY < topPos + 161
                && minecraft != null && minecraft.gameMode != null) {
            int target = mouseX >= leftPos + 22 && mouseX < leftPos + 202 ? 5 :
                    mouseX >= leftPos + 213 && mouseX < leftPos + 431 ? 6 : -1;
            if (target != -1) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, target);
                return true;
            }
        }
        if (button == 0 && tab == 6 && menu.smartFarmingUnlocked()
                && mouseY >= topPos + 195 && mouseY < topPos + 215
                && mouseX >= leftPos + 22 && mouseX < leftPos + 202
                && minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 10);
            return true;
        }
        if (button == 0 && tab == 6 && menu.smartFarmingUnlocked()
                && mouseY >= topPos + 169 && mouseY < topPos + 191
                && minecraft != null && minecraft.gameMode != null) {
            int target = mouseX >= leftPos + 22 && mouseX < leftPos + 202 ? 7 :
                    mouseX >= leftPos + 213 && mouseX < leftPos + 431 ? 8 : -1;
            if (target != -1) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, target);
                return true;
            }
        }
        if (button == 0 && tab == 5 && menu.upgraded() && mouseY >= topPos + 138
                && mouseY < topPos + 161 && minecraft != null && minecraft.gameMode != null) {
            int target = mouseX >= leftPos + 22 && mouseX < leftPos + 150 ? 2 :
                    mouseX >= leftPos + 164 && mouseX < leftPos + 292 ? 3 :
                    mouseX >= leftPos + 306 && mouseX < leftPos + 454 ? 4 : -1;
            if (target != -1) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, target);
                return true;
            }
        }
        if (button == 0 && tab == 5 && menu.upgraded() && mouseY >= topPos + 168
                && mouseY < topPos + 191 && mouseX >= leftPos + 306 && mouseX < leftPos + 454
                && minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 9);
            return true;
        }
        if (button == 0 && tab == 4 && mouseY >= topPos + 149 && mouseY < topPos + 170
                && minecraft != null && minecraft.gameMode != null) {
            if (mouseX >= leftPos + 22 && mouseX < leftPos + 196) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                return true;
            }
            if (mouseX >= leftPos + 211 && mouseX < leftPos + 387) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
                return true;
            }
        }
        return super.mouseClicked(actualX, actualY, button);
    }
    private void openStatus(String name){
        if(minecraft!=null&&minecraft.gameMode!=null){
            if(name.equals("Storage")&&menu.upgraded()){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,4);return;}
            if(name.equals("Seeds")&&menu.smartFarmingUnlocked()){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,6);return;}
        }
        tab=com.agrievolution.content.GreenhouseStatus.targetTab(name);
    }

    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Render all text once at the native UI resolution, after the blurred world background.
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        renderTooltip(g, mouseX, mouseY);
        if (tab == 2) renderPlotTooltip(g, mouseX, mouseY);
        if(tab==0) {
            var indicators=com.agrievolution.content.GreenhouseStatus.indicators(menu);
            for(int i=0;i<indicators.size();i++) {
                int x=leftPos+18+(i%3)*150,y=topPos+99+(i/3)*43;
                if(mouseX>=px(x)&&mouseX<px(x+144)&&mouseY>=py(y)&&mouseY<py(y+38))
                    g.renderTooltip(font,font.split(Component.literal(indicators.get(i).help()).withStyle(net.minecraft.network.chat.Style.EMPTY.withFont(com.agrievolution.client.journal.JournalFonts.BODY_FONT)),220),mouseX,mouseY);
            }
        }
    }
}
