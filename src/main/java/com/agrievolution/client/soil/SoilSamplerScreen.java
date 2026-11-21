package com.agrievolution.client.soil;

import com.agrievolution.client.GardenGuiTheme;

import com.agrievolution.client.journal.JournalFonts;
import com.agrievolution.farming.SoilSurveyData;
import com.agrievolution.farming.SoilResearch;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Read-only parchment field report; no packets, soil mutation or inventory access. */
public final class SoilSamplerScreen extends Screen {
    private static final int W = 406, H = 246;
    private static final int DARK = 0xFF281C17;
    private static final int BORDER = 0xFFBE9554;
    private static final int PARCHMENT = 0xFFF0DFBA;
    private static final int INK = 0xFF352719;
    private static final int GREEN = 0xFF275B47;
    private static final int MUTED = 0xFF856D55;
    private static final int YELLOW = 0xFFB78938;
    private static final int RED = 0xFF9E4544;
    private static final int BLUE = 0xFF3679A1;

    private enum Page { PLOT, AREA, ADVICE, RESEARCH, CARE }
    private final CompoundTag sample;
    private final int research;
    private final int samplerTier;
    private Page page = Page.PLOT;
    private int left, top;
    private final com.agrievolution.client.NativeGuiViewport viewport=new com.agrievolution.client.NativeGuiViewport();

    public SoilSamplerScreen(ItemStack sampler) {
        super(Component.literal("Agri Evolution - Soil Sampler"));
        sample = SoilSurveyData.report(sampler);
        research = SoilResearch.analyzed(sampler);
        samplerTier = SoilResearch.tier(sampler);
    }

    @Override
    protected void init() {
        super.init();
        viewport.init(width,height,W,H);left=viewport.x();top=viewport.y();
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        super.render(gui, mouseX, mouseY, partialTick); // background blur BEFORE sharp UI
        gui.fill(0, 0, width, height, 0x900A100D);
        gui.pose().pushPose();
        gui.pose().translate(left, top, 0);
        panel(gui, 0, 0, W, H, DARK, BORDER);
        panel(gui, 7, 7, W - 14, 40, GREEN, BORDER);
        line(gui, "AGRI EVOLUTION", 17, 15, PARCHMENT);
        line(gui, "SOIL ANALYSIS / FIELD REPORT", 163, 15, PARCHMENT);
        line(gui, "FIELD NOTES  /  SOIL HEALTH", 18, 30, 0xFFE9D8B5);
        tab(gui, "PLOT", 12, page == Page.PLOT);
        tab(gui, "AREA", 90, page == Page.AREA);
        tab(gui, "ADVICE", 168, page == Page.ADVICE);
        tab(gui, "RESEARCH", 246, page == Page.RESEARCH);
        tab(gui, "CARE", 324, page == Page.CARE);
        panel(gui, 12, 82, 382, 135, PARCHMENT, BORDER);
        if (page == Page.RESEARCH) {
            researchPage(gui);
        } else if (page == Page.CARE) {
            care(gui);
        } else if (sample.isEmpty()) {
            line(gui, "NO FIELD SAMPLE RECORDED", 26, 96, GREEN);
            line(gui, "Right-click farmland or a planted Agri crop", 26, 119, INK);
            line(gui, "to record moisture, irrigation and crop health.", 26, 134, INK);
            line(gui, "Sneak-click crops for a 5x5 soil survey.", 26, 155, MUTED);
            line(gui, "Analyze soil specimens at a Research Table.", 26, 172, GREEN);
        } else {
            switch (page) {
                case PLOT -> plot(gui);
                case AREA -> area(gui);
                case ADVICE -> advice(gui);
                case RESEARCH -> researchPage(gui);
                case CARE -> care(gui);
            }
        }
        panel(gui, 12, 222, 382, 16, 0xFFB9A37B, BORDER);
        String footer = sample.isEmpty() ? "READY FOR A NEW SOIL SAMPLE"
                : "SAVED REPORT  |  " + sample.getString("Dimension")
                + "  |  " + sample.getInt("X") + ", " + sample.getInt("Y") + ", " + sample.getInt("Z");
        if (footer.length() > 55) footer = footer.substring(0, 52) + "...";
        line(gui, footer, 20, 226, GREEN);
        gui.pose().popPose();
        if(viewport.oversized()) {
            gui.fill(0,height-14,width,height,0xEF281D17);
            gui.drawCenteredString(font,"Ctrl+scroll or arrows  |  Shift: sideways",width/2,height-11,PARCHMENT);
        }
    }

    private void care(GuiGraphics gui) {
        line(gui, "ROTATION & MULCH  /  SAVED SAMPLE", 23, 92, GREEN);
        line(gui, "Previous harvest: " + (sample.contains("PreviousFamily") ? sample.getString("PreviousFamily") : "Unknown"), 23, 111, INK);
        line(gui, "Current family: " + (sample.contains("CropFamily") ? sample.getString("CropFamily") : "Unknown"), 23, 126, INK);
        line(gui, sample.getInt("RotationBonus") > 0 ? sample.getBoolean("HasCrop") ? "Rotation active: +10 to the Agri growth roll." : "Rotation active: natural growth bonus." : "Harvest, then switch crop families for a growth bonus.", 23, 141, GREEN);
        int seconds=(sample.getInt("MulchTicks")+19)/20;
        line(gui, seconds>0 ? "Mulch: " + seconds/60 + "m " + seconds%60 + "s left when sampled." : "Mulch: none. Use Compost Mulch on the soil.", 23, 156, INK);
        line(gui, "Mulch slows weeds, nutrition loss and upgraded-soil drying.", 23, 172, MUTED);
        panel(gui, 23, 190, 356, 19, GREEN, BORDER);
        line(gui, "OPEN RESOURCE CATALYST GUIDE", 35, 196, PARCHMENT);
    }

    private void researchPage(GuiGraphics gui) {
        line(gui, SoilResearch.rankName(samplerTier).toUpperCase(), 23, 92, GREEN);
        line(gui, "UNIQUE SAMPLES: " + research + "/96", 23, 109, INK);
        progress(gui, 23, 124, 346, research, 96, GREEN);
        line(gui, "8 samples: CULTIVATED / green", 23, 137, research >= 8 ? GREEN : MUTED);
        line(gui, "24: NUTRIENT / cyan", 23, 149, research >= 24 ? GREEN : MUTED);
        line(gui, "48: MYTHIC / purple", 23, 161, research >= 48 ? GREEN : MUTED);
        line(gui, "72: ARCANE / gold (Table Mk II)", 23, 173, research >= 72 ? GREEN : MUTED);
        line(gui, "96: CELESTIAL / icy (Table Mk III)", 23, 185, research >= 96 ? GREEN : MUTED);
        line(gui, "Research + refine in the Soil Research Table GUI", 23, 200, INK);
    }

    private void plot(GuiGraphics gui) {
        int moisture = SoilSurveyData.average(sample, "MoistureSum", "Valid");
        boolean wet = sample.getInt("Irrigated") > 0;
        line(gui, "SOIL CONDITION", 23, 92, GREEN);
        gui.fill(23, 106, 196, 107, BORDER);
        line(gui, "Soil Tier: " + (sample.getInt("SoilTier") == 0 ? "VANILLA"
                : "UPGRADED " + sample.getInt("SoilTier")), 23, 112, INK);
        line(gui, "Moisture: " + moisture + "/7", 23, 130, moisture <= 2 && !wet ? RED : GREEN);
        progress(gui, 23, 144, 170, moisture, 7, moisture <= 2 && !wet ? RED : BLUE);
        line(gui, "Irrigation: " + (wet ? "ACTIVE" : "NOT DETECTED"), 23, 157, wet ? GREEN : MUTED);
        line(gui, "Light: " + (sample.getBoolean("HasCrop") ? sample.getInt("Light") + "/15" : "not measured"),
                23, 174, sample.getBoolean("HasCrop") && sample.getInt("Light") < 9 ? RED : INK);
        line(gui, "PLANTED CROP", 211, 92, GREEN);
        gui.fill(211, 106, 384, 107, BORDER);
        if (!sample.getBoolean("HasCrop")) {
            line(gui, "No Agri crop above soil", 211, 120, MUTED);
            line(gui, "Nutrition is stored on", 211, 144, INK);
            line(gui, "a planted crop, not", 211, 159, INK);
            line(gui, "on empty farmland.", 211, 174, INK);
            return;
        }
        String name = sample.getString("CropName");
        if (name.length() > 22) name = name.substring(0, 19) + "...";
        line(gui, name, 211, 112, INK);
        line(gui, "Growth stage: " + sample.getInt("Age") + "/7", 211, 127, INK);
        if (sample.contains("Nutrition")) {
            int nutrition = sample.getInt("Nutrition");
            line(gui, "Nutrition: " + nutrition + "/100", 211, 144, nutrition < 35 ? RED : GREEN);
            progress(gui, 211, 158, 170, nutrition, 100, nutrition < 35 ? RED : GREEN);
        } else {
            line(gui, "Nutrition: not available", 211, 144, MUTED);
        }
        line(gui, "Crop support: " + (sample.getBoolean("Supported") ? "YES" : "NO"), 211, 171, INK);
        if (sample.getBoolean("Resource")) {
            int catalyst = sample.getInt("CatalystX100");
            line(gui, "Catalyst: x" + decimal(catalyst), 211, 188, catalyst > 100 ? GREEN : RED);
        } else {
            line(gui, "Growth roll: " + (sample.contains("GrowthRoll")
                    ? sample.getInt("GrowthRoll") + "/200" : "unavailable"), 211, 188, MUTED);
        }
        if (sample.contains("Companion")) line(gui, sample.getString("Companion"), 211, 201, GREEN);
    }

    private void area(GuiGraphics gui) {
        if (!sample.getBoolean("Area")) {
            line(gui, "SINGLE-PLOT SCAN", 23, 95, GREEN);
            line(gui, "Sneak + right-click on farmland or a crop", 23, 121, INK);
            line(gui, "to survey the surrounding 5x5 plots.", 23, 136, INK);
            line(gui, "This will replace your current saved report.", 23, 165, MUTED);
            return;
        }
        int valid = sample.getInt("Valid");
        int crops = sample.getInt("Crops");
        int nutritionCount = sample.getInt("NutritionCount");
        line(gui, "5x5 SOIL COVERAGE", 23, 91, GREEN);
        line(gui, "Farmland: " + valid + "/25", 23, 109, INK);
        line(gui, "Crops: " + crops, 23, 123, INK);
        line(gui, "Avg moisture: " + (valid == 0 ? "N/A" : SoilSurveyData.average(sample, "MoistureSum", "Valid") + "/7"), 23, 137, INK);
        line(gui, "Irrigated: " + sample.getInt("Irrigated") + "/" + valid, 23, 151, INK);
        line(gui, "Avg nutrition: " + (nutritionCount == 0 ? "N/A" : SoilSurveyData.average(sample,
                "NutritionSum", "NutritionCount") + "/100"), 23, 165, INK);
        line(gui, "Measured crops: " + nutritionCount, 23, 179, MUTED);
        line(gui, "FIELD MOISTURE MAP", 229, 91, GREEN);
        ListTag tiles = SoilSurveyData.plots(sample);
        for (int i = 0; i < Math.min(25, tiles.size()); i++) {
            CompoundTag tile = tiles.getCompound(i);
            int x = 240 + (i % 5) * 18;
            int y = 108 + (i / 5) * 17;
            int color = !tile.getBoolean("Valid") ? 0xFF9B9080
                    : tile.getBoolean("Irrigated") ? GREEN
                    : tile.getInt("Moisture") <= 2 ? RED
                    : tile.getInt("Moisture") >= 6 ? BLUE : YELLOW;
            gui.fill(x, y, x + 15, y + 14, BORDER);
            gui.fill(x + 2, y + 2, x + 13, y + 12, color);
            if (tile.getBoolean("Crop")) gui.fill(x + 6, y + 5, x + 9, y + 9, PARCHMENT);
        }
        line(gui, "Red=dry  Blue=wet", 223, 192, MUTED);
        line(gui, "Green=basin  White=crop", 223, 204, MUTED);
    }

    private void advice(GuiGraphics gui) {
        line(gui, "SOIL SAMPLER RECOMMENDATIONS", 23, 92, GREEN);
        gui.fill(23, 106, 379, 107, BORDER);
        List<String> notes = new ArrayList<>();
        if (sample.getInt("Valid") == 0) {
            notes.add("No accessible farmland in the selected area.");
        } else {
            int moisture = SoilSurveyData.average(sample, "MoistureSum", "Valid");
            if (moisture <= 3 && sample.getInt("Irrigated") < sample.getInt("Valid"))
                notes.add("Dry soil: add water or a filled Irrigation Basin.");
            if (sample.contains("Nutrition") && sample.getInt("Nutrition") < 35)
                notes.add("Low crop nutrition: use Nutrient Compost.");
            if (sample.getBoolean("HasCrop") && sample.getInt("Light") < 9)
                notes.add("Low crop light: provide more illumination.");
            if (sample.getBoolean("HasCrop") && !sample.getBoolean("Supported"))
                notes.add("Add crop support for growth and breeding bonuses.");
            if (sample.getBoolean("Resource") && sample.getInt("CatalystX100") <= 100)
                notes.add("No matching resource catalyst below farmland.");
            if (sample.getBoolean("Area") && sample.getInt("LowLight") > 0)
                notes.add(sample.getInt("LowLight") + " nearby crops have low lighting.");
            if (sample.getBoolean("Area") && sample.getInt("NutritionCount") > 0
                    && SoilSurveyData.average(sample, "NutritionSum", "NutritionCount") < 35)
                notes.add("Survey crops average low nutrition: use compost.");
            if (sample.getBoolean("Area") && sample.getInt("Valid") < 25)
                notes.add("Only " + sample.getInt("Valid") + "/25 cells have accessible farmland.");
        }
        if (notes.isEmpty()) notes.add("No obvious issues in the sampled crop/soil.");
        for (int i = 0; i < Math.min(5, notes.size()); i++) {
            String note = notes.get(i);
            if (note.length() > 55) note = note.substring(0, 52) + "...";
            line(gui, (i + 1) + ". " + note, 23, 117 + i * 17, i == 0 ? GREEN : INK);
        }
        line(gui, "Sampling never changes soil, crops or genetics.", 23, 199, MUTED);
    }

    private static String decimal(int scaled) {
        return String.format(java.util.Locale.ROOT, "%.1f", scaled / 100.0);
    }

    private void tab(GuiGraphics gui, String caption, int x, boolean active) {
        panel(gui, x, 52, 70, 23, active ? PARCHMENT : GREEN, BORDER);
        line(gui, caption, x + 8, 59, active ? INK : PARCHMENT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double x=mouseX-left, y=mouseY-top;
        if (button == 0 && y >= 52 && y < 75) {
            int tab=(int)((x-12)/78);
            if(x>=12 && tab>=0 && tab<Page.values().length && x<12+tab*78+70) {page=Page.values()[tab];return true;}
        }
        if(button==0 && page==Page.CARE && x>=23 && x<379 && y>=190 && y<209) {
            minecraft.setScreen(new CropCareGuideScreen(this,sample.getString("ResourceId")));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void line(GuiGraphics gui, String text, int x, int y, int color) {
        gui.drawString(font, JournalFonts.body(text), x, y, color, false);
    }

    private static void panel(GuiGraphics gui, int x, int y, int w, int h, int fill, int edge) {
        GardenGuiTheme.panel(gui, x, y, w, h, fill);
    }

    private static void progress(GuiGraphics gui, int x, int y, int width,
                                 int value, int max, int fill) {
        gui.fill(x, y, x + width, y + 7, BORDER);
        gui.fill(x + 1, y + 1, x + width - 1, y + 6, 0xFFDAC7A0);
        gui.fill(x + 1, y + 1, x + 1 + (width - 2) * Math.max(0, Math.min(max, value)) / max, y + 6, fill);
    }

    @Override
    public boolean isPauseScreen() { return false; }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        if(viewport.keyPressed(key,modifiers)){left=viewport.x();top=viewport.y();return true;}
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public boolean mouseScrolled(double mx,double my,double dx,double dy) {
        if(hasControlDown()&&viewport.oversized()){viewport.scroll(dx,dy,hasShiftDown());left=viewport.x();top=viewport.y();return true;}
        return super.mouseScrolled(mx,my,dx,dy);
    }
}
