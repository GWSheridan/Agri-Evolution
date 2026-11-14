package com.agrievolution.client;

import com.agrievolution.content.LunchBoxMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class LunchBoxScreen extends AbstractContainerScreen<LunchBoxMenu> {
    public LunchBoxScreen(LunchBoxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 268;
        imageHeight = 174;
        inventoryLabelY = 80;
    }
    @Override protected void init(){
        super.init();
        addRenderableWidget(actionButton("Deposit food",leftPos+178,topPos+28,()->send(0)));
        addRenderableWidget(actionButton("Sort foods",leftPos+178,topPos+50,()->send(1)));
        addRenderableWidget(actionButton("Toggle feeding",leftPos+178,topPos+72,()->send(2)));
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
        g.drawWordWrap(font,Component.literal("Auto-feed: "+(menu.feedingEnabled()?"ON":"OFF")+"\n"+menu.feedingStatus()+"\nEvery 4 seconds. Close menus to eat."),x+181,y+98,78,0x302518);
        for(int i=0;i<9;i++){
            int sx=x+8+i*18;
            GardenGuiTheme.panel(g,sx,y+48,16,13,menu.protectedSlot(i)?0xFF936454:0xFF275841);
            g.drawCenteredString(font,menu.protectedSlot(i)?"P":new String[]{"L","N","H"}[menu.priority(i)],sx+8,y+51,0xFFF0DEB8);
        }
        g.drawString(font,"Left: priority | Right: protect",x+8,y+65,0x302518,false);
        for (var slot : menu.slots) {
            GardenGuiTheme.slot(g, x + slot.x, y + slot.y);
        }
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        int slot=(int)((x-leftPos-8)/18);
        if((button==0||button==1)&&x>=leftPos+8&&x<leftPos+170&&y>=topPos+48&&y<topPos+61&&slot>=0&&slot<9){
            send((button==0?3:12)+slot);return true;
        }
        return super.mouseClicked(x,y,button);
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        if(mouseX>=leftPos+8&&mouseX<leftPos+170&&mouseY>=topPos+48&&mouseY<topPos+61){
            int slot=(mouseX-leftPos-8)/18;
            String priority=new String[]{"Low","Normal","High"}[menu.priority(slot)];
            g.renderTooltip(font,font.split(Component.literal("Slot "+(slot+1)+": "+priority+" priority. "+(menu.protectedSlot(slot)?"Protected: never auto-eaten. ":"")+"Left-click cycles priority. Right-click toggles protection. Settings belong to the slot; manual removal is allowed."),220),mouseX,mouseY);
        }
    }
}
