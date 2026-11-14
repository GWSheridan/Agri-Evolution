package com.agrievolution.client.greenhouse;

import com.agrievolution.client.GardenGuiTheme;

import com.agrievolution.content.BotanicalGreenhouseSeedBankMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Server-synchronized per-slot planting controls, using the established parchment palette. */
public final class BotanicalGreenhouseSeedBankScreen extends AbstractContainerScreen<BotanicalGreenhouseSeedBankMenu> {
    private boolean plantingRules;
    private static final int DARK = 0xFF281D17, GOLD = 0xFFC6A368;
    private static final int GREEN = 0xFF275841, PAPER = 0xFFF0DEB8;
    private static final int INK = 0xFF302518, MUTED = 0xFF8A755B;
    private static final int OFF = 0xFF936454, AMBER = 0xFFA87836;
    private static final int PRESET_X = 16, PRESET_Y = 129, PRESET_WIDTH = 49, PRESET_PITCH = 55, PRESET_HEIGHT = 19;

    public BotanicalGreenhouseSeedBankScreen(BotanicalGreenhouseSeedBankMenu menu, Inventory inv, Component name) {
        super(menu, inv, name);
        imageWidth = 250;
        imageHeight = 265;
    }

    @Override protected void renderBg(GuiGraphics g, float delta, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        GardenGuiTheme.panel(g,x,y,imageWidth,imageHeight,DARK);
        GardenGuiTheme.panel(g,x+8,y+8,234,35,GREEN);
        GardenGuiTheme.panel(g,x+8,y+46,234,116,PAPER);
        GardenGuiTheme.panel(g,x+8,y+165,234,94,PAPER);
        g.drawString(font, "ADVANCED SEED BANK", x + 14, y + 15, PAPER, false);
        g.drawString(font, plantingRules?"Species locks / minimum genetics":"Ready seeds: "+menu.totalEligibleSeeds()+" | Hover slots", x + 14, y + 29, PAPER, false);
        for (int i = 0; i < 9; i++) {
            int sx = x + 16 + i * 23;
            slotBg(g, sx, y + 53);
            int enabled = (plantingRules?menu.plantingSpecies(i)!=0:menu.slotEnabled(i)) ? GREEN : OFF;
            buttonBg(g, sx, y + 75, enabled);
            center(g, plantingRules?(menu.plantingSpecies(i)==0?"Any":"Set"):menu.slotEnabled(i) ? "ON" : "OFF", sx + 10, y + 78, PAPER);
            int priority = menu.priority(i);
            if(!plantingRules){buttonBg(g, sx, y + 94, priority == 2 ? AMBER : priority == 1 ? GREEN : MUTED);
            center(g, priority == 2 ? "H" : priority == 1 ? "N" : "L", sx + 10, y + 97, PAPER);}
        }
        if(plantingRules)for(int i=0;i<3;i++){
            buttonBg(g,x+16+i*73,y+94,GREEN,68,16);
            g.drawString(font,new String[]{"Growth ","Yield ","Resil. "}[i]+menu.minimumGene(i),x+19+i*73,y+98,PAPER,false);
        }
        g.drawString(font, plantingRules?"Minimum genes: left +10 / right -10":"Enable slots / priority: H > N > L", x + 16, y + 113, INK, false);
        if (plantingRules) {
            for (int index = 0; index < BotanicalGreenhouseSeedBankMenu.PRESET_COUNT; index++) {
                int sx = x + PRESET_X + index * PRESET_PITCH * 2;
                String name = Character.toString('A' + index);
                buttonBg(g, sx, y + PRESET_Y, GREEN, PRESET_WIDTH, PRESET_HEIGHT);
                center(g, "Save " + name, sx + PRESET_WIDTH / 2, y + 135, PAPER);
                int loadX = sx + PRESET_PITCH;
                int color = menu.presetMatches(index) ? GREEN : menu.hasPreset(index) ? AMBER : MUTED;
                buttonBg(g, loadX, y + PRESET_Y, color, PRESET_WIDTH, PRESET_HEIGHT);
                center(g, "Load " + name, loadX + PRESET_WIDTH / 2, y + 135, PAPER);
            }
        } else {
            buttonBg(g, x + 15, y + 129, 0xFF58715B, 103, 19);
            g.drawString(font, "Reserve: " + menu.reservePerSlot(), x + 22, y + 135, PAPER, false);
            buttonBg(g, x + 125, y + 129, menu.bestGenetics() ? GREEN : MUTED, 109, 19);
            g.drawString(font, "Best genes: " + (menu.bestGenetics() ? "ON" : "OFF"), x + 130, y + 135, PAPER, false);
        }
        g.drawString(font, plantingRules?"< Slot controls   |   Seed Bank planting":"Planting rules >  |  Species / genetics", x + 16, y + 152, GREEN, false);
        g.drawString(font, "Inventory", x + 16, y + 169, INK, false);
        for (int i = 0; i < 27; i++) slotBg(g, x + 16 + i % 9 * 23, y + 181 + i / 9 * 18);
        for (int i = 0; i < 9; i++) slotBg(g, x + 16 + i * 23, y + 244);
    }

    private void center(GuiGraphics g, String label, int midX, int y, int color) {
        g.drawString(font, label, midX - font.width(label) / 2, y, color, false);
    }

    private static void slotBg(GuiGraphics g, int x, int y) {
        GardenGuiTheme.slot(g,x,y);
    }

    private static void buttonBg(GuiGraphics g, int x, int y, int color) {
        buttonBg(g, x, y, color, 20, 15);
    }
    private static void buttonBg(GuiGraphics g, int x, int y, int color, int w, int h) {
        GardenGuiTheme.panel(g,x-1,y-1,w+2,h+2,color);
    }

    private static int presetButtonAt(int x, int y) {
        if (y < PRESET_Y || y >= PRESET_Y + PRESET_HEIGHT || x < PRESET_X) return -1;
        int offset = x - PRESET_X;
        int button = offset / PRESET_PITCH;
        return button < BotanicalGreenhouseSeedBankMenu.PRESET_COUNT * 2 && offset % PRESET_PITCH < PRESET_WIDTH ? button : -1;
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int localX=(int)(mouseX-leftPos),localY=(int)(mouseY-topPos);
        if(button==0&&localX>=15&&localX<235&&localY>=149&&localY<163){plantingRules=!plantingRules;return true;}
        if(plantingRules&&minecraft!=null&&minecraft.gameMode!=null){
            int presetButton = presetButtonAt(localX, localY);
            if (button == 0 && presetButton >= 0) {
                int index = presetButton / 2;
                if (presetButton % 2 == 0) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, BotanicalGreenhouseSeedBankMenu.SAVE_PRESET_START + index);
                } else if (menu.hasPreset(index)) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, BotanicalGreenhouseSeedBankMenu.LOAD_PRESET_START + index);
                }
                return true;
            }
            if((button==0||button==1)&&localY>=94&&localY<111){for(int i=0;i<3;i++)if(localX>=16+i*73&&localX<84+i*73){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,(button==0?29:32)+i);return true;}}
            if(button==0&&localY>=75&&localY<91){for(int i=0;i<9;i++)if(localX>=16+i*23&&localX<36+i*23){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,20+i);return true;}}
            // Priority controls are hidden on the rules page.
            if(localY>=75&&localY<111)return false;
        }
        if (button == 0 && minecraft != null && minecraft.gameMode != null) {
            int x = (int) (mouseX - leftPos), y = (int) (mouseY - topPos);
            if (y >= 75 && y < 91 || y >= 94 && y < 110) {
                for (int slot = 0; slot < 9; slot++) {
                    int sx = 16 + slot * 23;
                    if (x >= sx && x < sx + 20) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                                (y < 91 ? 0 : 9) + slot);
                        return true;
                    }
                }
            }
            if (!plantingRules && y >= 129 && y < 148) {
                if (x >= 15 && x < 118) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 18);
                    return true;
                }
                if (x >= 125 && x < 234) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 19);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override protected void renderLabels(GuiGraphics g, int mx, int my) { }

    @Override public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        if(hoveredSlot==null||hoveredSlot.index>=9)renderTooltip(g, mx, my);
        if(hoveredSlot!=null&&hoveredSlot.index<9){
            int slot=hoveredSlot.index;
            var lines=new java.util.ArrayList<net.minecraft.util.FormattedCharSequence>();
            if(hoveredSlot.hasItem())for(var line:getTooltipFromItem(minecraft,hoveredSlot.getItem()))lines.addAll(font.split(line,220));
            lines.addAll(font.split(Component.literal("Eligible seeds: "+menu.eligibleSeeds(slot)+" (after reserve)"),220));
            for(String reason:com.agrievolution.farming.PlantingDiagnostics.reasons(menu.exclusions(slot)))
                lines.addAll(font.split(Component.literal(reason),220));
            if(menu.exclusions(slot)==0)lines.addAll(font.split(Component.literal("Ready for selection; greenhouse and plot checks still apply."),220));
            g.renderTooltip(font,lines,mx,my);
        }
        int x=mx-leftPos,y=my-topPos;
        if(plantingRules&&y>=75&&y<91)for(int i=0;i<9;i++)if(x>=16+i*23&&x<36+i*23){
            int id=menu.plantingSpecies(i);
            String text=id==0?"Any species. Insert the desired seed, then click to lock this slot to its species.":"Only "+com.agrievolution.genetics.CropSpecies.values()[id-1].displayName()+". Click to clear the lock. Other seeds can be stored here but will not be planted.";
            g.renderTooltip(font,font.split(Component.literal(text),220),mx,my);
        }
        if (!plantingRules && y>=129&&y<148&&x>=15&&x<118)g.renderTooltip(font,font.split(Component.literal("Keep this many seeds in each occupied slot. Automatic Seed Bank planting stops at the reserve. Manual removal is allowed."),220),mx,my);
        int presetButton = plantingRules ? presetButtonAt(x, y) : -1;
        if (presetButton >= 0) {
            int index = presetButton / 2;
            String text;
            if (presetButton % 2 == 0) {
                text = menu.hasPreset(index) ? "Overwrite this preset with the current species locks and gene minimums."
                        : "Save the current species locks and gene minimums to this preset.";
            } else if (!menu.hasPreset(index)) {
                text = "Empty preset. Save your planting rules here first.";
            } else {
                text = menu.presetMatches(index) ? "This preset matches your current planting rules."
                        : "Replace your current species locks and gene minimums with this preset.";
            }
            g.renderTooltip(font, font.split(Component.literal(text), 220), mx, my);
        }
    }
}
