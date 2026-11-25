package com.agrievolution.client;

import com.agrievolution.content.SeedBagMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class SeedBagScreen extends AbstractContainerScreen<SeedBagMenu> {
    public SeedBagScreen(SeedBagMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 268;
        imageHeight = 150;
        inventoryLabelY = 56;
    }
    @Override protected void init(){
        super.init();
        addRenderableWidget(actionButton("Deposit seeds",leftPos+178,topPos+28,()->send(0)));
        addRenderableWidget(actionButton("Sort species",leftPos+178,topPos+50,()->send(1)));
    }
    public static net.minecraft.client.gui.components.Button actionButton(String text,int x,int y,Runnable action){
        return new net.minecraft.client.gui.components.Button(x,y,84,18,Component.literal(text),b->action.run(),supplier->supplier.get()){
            @Override protected void renderWidget(GuiGraphics g,int mx,int my,float delta){
                GardenGuiTheme.panel(g,getX(),getY(),getWidth(),getHeight(),isHoveredOrFocused()?0xFF397454:0xFF275841);
                g.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font,getMessage(),getX()+getWidth()/2,getY()+5,0xFFF0DEB8);
            }
        };
    }
    private void send(int action){if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action);}
    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        GardenGuiTheme.panel(g, x-2, y-2, imageWidth+4, imageHeight+4, 0xFFE0D1AE);
        g.drawWordWrap(font,Component.literal("Different genetics stay separate. Full bag? Extras remain in your inventory."),x+181,y+76,78,0x302518);
        for (var slot : menu.slots) {
            GardenGuiTheme.slot(g, x + slot.x, y + slot.y);
        }
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
