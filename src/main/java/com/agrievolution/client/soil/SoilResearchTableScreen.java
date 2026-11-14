package com.agrievolution.client.soil;

import com.agrievolution.client.GardenGuiTheme;

import com.agrievolution.content.SoilResearchTableMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Pixel-sharp research/processing GUI using the established Agri Evolution palette. */
public final class SoilResearchTableScreen extends AbstractContainerScreen<SoilResearchTableMenu> {
    private static final int DARK = 0xFF291D17, GOLD = 0xFFC49B5A, GREEN = 0xFF255747;
    private static final int PAPER = 0xFFF1DFB8, INK = 0xFF37281A, MUTED = 0xFF8C7659;
    public SoilResearchTableScreen(SoilResearchTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 348;
        imageHeight = 264;
        titleLabelX = 12;
        titleLabelY = 8;
        inventoryLabelX = 13;
        inventoryLabelY = 166;
    }
    private void panel(GuiGraphics ui, int x, int y, int w, int h, int color, int border) {
        GardenGuiTheme.panel(ui, x, y, w, h, color);
    }
    private void txt(GuiGraphics ui, String text, int x, int y, int color) {
        ui.drawString(font, text, x, y, color, false);
    }
    private void tab(GuiGraphics ui, String label, int x, int y, int width) {
        panel(ui, x, y, width, 22, GREEN, GOLD);
        txt(ui, label, x+(width-font.width(label))/2, y+7, PAPER);
    }
    @Override protected void renderBg(GuiGraphics ui, float partial, int mouseX, int mouseY) {
        int x=leftPos, y=topPos;
        panel(ui, x, y, imageWidth, imageHeight, DARK, GOLD);
        panel(ui, x+7, y+6, imageWidth-14, 28, GREEN, GOLD);
        txt(ui, "AGRI EVOLUTION", x+15, y+15, PAPER);
        txt(ui, "SOIL RESEARCH TABLE  MK " + menu.tableTier(), x+139, y+15, PAPER);
        panel(ui, x+8, y+38, imageWidth-16, 53, PAPER, GOLD);
        String[] titles={"SAMPLER", "SAMPLE", "SOIL", "CATALYST", "RESULT"};
        int[] positions={30,83,152,202,294};
        for(int i=0;i<titles.length;i++) {
            txt(ui, titles[i], x+positions[i]-2, y+45, i==4?GREEN:INK);
            panel(ui, x+positions[i]-1, y+61, 18,18, 0xFFDBC9A5, MUTED);
        }
        tab(ui, "ANALYZE SPECIMEN", x+12, y+95, 151);
        tab(ui, "REFINE FARMLAND", x+172, y+95, 163);
        txt(ui, "SAMPLES  " + menu.analyzed() + "/96", x+13, y+124, PAPER);
        panel(ui, x+116, y+126, 218, 9, 0xFFE6D1A7, GOLD);
        ui.fill(x+118, y+128, x+118+(214*Math.max(0,Math.min(96,menu.analyzed()))/96), y+133, GREEN);
        int value=menu.analyzed();
        String next=value>=96?"RESEARCH COMPLETE":value>=72?"NEXT: 96 | CELESTIAL":value>=48?
                "NEXT: 72 | ARCANE":value>=24?"NEXT: 48 | MYTHIC":value>=8?
                "NEXT: 24 | NUTRIENT":"NEXT: 8 | CULTIVATED";
        txt(ui, next, x+13, y+139, 0xFFE3D2AB);
        txt(ui, "INVENTORY", x+13, y+163, PAPER);
        panel(ui, x+90, y+155, 171, 102, 0xFFAD9472, GOLD);
        for(int row=0;row<3;row++) for(int col=0;col<9;col++)
            panel(ui, x+93+col*18, y+158+row*18, 18,18, 0xFFDBCBA9, MUTED);
        for(int col=0;col<9;col++)
            panel(ui, x+93+col*18, y+228, 18,18, 0xFFDBCBA9, MUTED);
        txt(ui, "LAB STATUS", x+13, y+180, PAPER);
        String status = switch(menu.status()) {
            case 1 -> "Need a Soil Sampler.";
            case 2 -> "Add a collected soil sample.";
            case 3 -> "Already studied or not from this sampler.";
            case 4 -> "All 96 samples completed!";
            case 5 -> "Analyzed " + menu.lastBatch() + " specimen(s).";
            case 6 -> "Take the previous result first.";
            case 7 -> "Insert Dirt or prior farmland.";
            case 8 -> requiredCatalystStatus();
            case 9 -> "Upgrade table with crafted module.";
            case 10 -> "Research more unique soil plots.";
            case 11 -> "Refined x" + menu.lastBatch() + ". Take result.";
            case 13 -> "Table upgraded!";
            case 14 -> "Already at Mk III.";
            case 15 -> "Wrong module upgrade order.";
            case 16 -> "Need 48 / 72 researched samples.";
            default -> "Add sampler + specimen. Click ANALYZE.";
        };
        ui.drawWordWrap(font, Component.literal(status), x+13, y+195, 75, PAPER);
        txt(ui, "ANALYZE x" + (menu.tableTier() == 1 ? 1 : menu.tableTier() == 2 ? 2 : 4), x+267, y+166, PAPER);
        txt(ui, "REFINE x" + (menu.tableTier() == 1 ? 1 : menu.tableTier() == 2 ? 2 : 4), x+267, y+181, PAPER);
        txt(ui, "MK2: 48", x+267, y+199, PAPER);
        txt(ui, "MK3: 72", x+267, y+214, PAPER);
        txt(ui, "USE MODULE", x+267, y+229, PAPER);
    }
    @Override protected void renderLabels(GuiGraphics ui, int mouseX, int mouseY) { }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(button==0 && mouseY>=topPos+95 && mouseY<topPos+117) {
            int code=mouseX>=leftPos+12 && mouseX<leftPos+163 ? 0
                    : mouseX>=leftPos+172 && mouseX<leftPos+335 ? 1 : -1;
            if(code>=0) {
                if(minecraft!=null && minecraft.gameMode!=null)
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId,code);
                return true;
            }
        }
        return super.mouseClicked(mouseX,mouseY,button);
    }
    @Override public void render(GuiGraphics ui, int mouseX, int mouseY, float delta) {
        super.render(ui,mouseX,mouseY,delta);
        // Vanilla item tooltips are exclusive: never stack our help tooltip over them.
        renderTooltip(ui,mouseX,mouseY);
        if (hoveredSlot != null) return;
        int rx=mouseX-leftPos, ry=mouseY-topPos;
        String tip = null;
        // Labels are help targets. Actual slots keep Minecraft's normal item tooltip.
        if (ry>=43 && ry<59) {
            if (rx>=25 && rx<76) tip="SOIL SAMPLER: keep the same tool. Research is saved to it.";
            else if (rx>=77 && rx<138) tip="SPECIMEN: collected samples stack by soil type. Each analysis consumes one.";
            else if (rx>=145 && rx<194) tip="FARMLAND: Dirt, or the previous farmland tier.";
            else if (rx>=195 && rx<285) tip=catalystHint();
            else if (rx>=287 && rx<337) tip="RESULT: take refined farmland from this slot.";
        }
        if (ry>=95 && ry<117) {
            if (rx>=12 && rx<163) tip="ANALYZE: Mk I studies 1 sample, Mk II studies 2, Mk III studies 4 per click.";
            else if (rx>=172 && rx<335) tip="REFINE: Mk I makes 1, Mk II up to 2, Mk III up to 4 per click. Each output costs one input and catalyst.";
        }
        if (ry>=163 && ry<245 && rx>=264 && rx<338)
            tip="Upgrade a placed table using a crafted module. Refinement: 48 samples for Mk II. Mastery: 72 for Mk III.";
        if (tip!=null) ui.renderTooltip(font, font.split(Component.literal(tip), 170), mouseX, mouseY);
    }
    /** Visible status replaces the obsolete instruction to hover the catalyst slot. */
    private String requiredCatalystStatus() {
        var input = menu.getSlot(2).getItem();
        if (input.is(net.minecraft.world.item.Items.DIRT)) return "Need Nutrient Compost.";
        if (input.is(com.agrievolution.content.AEItems.CULTIVATED_FARMLAND.get())) return "Need Copper Ingot.";
        if (input.is(com.agrievolution.content.AEItems.NUTRIENT_FARMLAND.get())) return "Need Amethyst Shard.";
        if (input.is(com.agrievolution.content.AEItems.MYTHIC_FARMLAND.get())) return "Need Gold Ingot.";
        if (input.is(com.agrievolution.content.AEItems.ARCANE_FARMLAND.get())) return "Need Echo Shard.";
        return "Wrong catalyst. See Journal recipes.";
    }
    private String catalystHint() {
        var input=menu.getSlot(2).getItem();
        if (input.is(net.minecraft.world.item.Items.DIRT)) return "CATALYST: Nutrient Compost converts Dirt into Cultivated Farmland.";
        if (input.is(com.agrievolution.content.AEItems.CULTIVATED_FARMLAND.get())) return "CATALYST: Copper Ingot -> Nutrient Farmland.";
        if (input.is(com.agrievolution.content.AEItems.NUTRIENT_FARMLAND.get())) return "CATALYST: Amethyst Shard -> Mythic Farmland.";
        if (input.is(com.agrievolution.content.AEItems.MYTHIC_FARMLAND.get())) return "CATALYST: Gold Ingot -> Arcane Farmland (table Mk II).";
        if (input.is(com.agrievolution.content.AEItems.ARCANE_FARMLAND.get())) return "CATALYST: Echo Shard -> Celestial Farmland (table Mk III).";
        return "CATALYST: Dirt=compost; Cultivated=copper; Nutrient=amethyst; Mythic=gold; Arcane=echo.";
    }
}
