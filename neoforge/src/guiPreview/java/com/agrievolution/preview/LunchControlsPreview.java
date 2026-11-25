package com.agrievolution.preview;
import com.agrievolution.client.LunchBoxScreen;
import com.agrievolution.content.LunchBoxMenu;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
@EventBusSubscriber(modid="agri_evolution",value=Dist.CLIENT)
public final class LunchControlsPreview {
    private static int ticks,index=-1;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(!Boolean.getBoolean("agri.lunchControlsPreview"))return;
        var mc=Minecraft.getInstance();if(mc.screen==null||mc.getOverlay()!=null||++ticks<40)return;ticks=0;
        if(index>=0)Screenshot.grab(mc.gameDirectory,"lunch-controls-"+index+".png",mc.getMainRenderTarget(),m->System.out.println(m.getString()));
        if(++index>=3){mc.stop();return;}mc.options.guiScale().set(2);mc.resizeDisplay();mc.setScreen(new Preview(index));
    }
    private static final class Preview extends Screen{
        final int scene;LunchBoxScreen screen;
        Preview(int scene){super(Component.literal("Lunch controls QA"));this.scene=scene;}
        @Override protected void init(){
            var inv=new Inventory(null);var menu=new LunchBoxMenu(1,inv,-1);
            for(int i=0;i<9;i++)menu.setData(i,i%3);
            menu.setData(9,1);menu.setData(18,scene==1?0:1);menu.setData(19,scene==1?1:0);
            menu.getSlot(0).set(new ItemStack(Items.GOLDEN_APPLE,2));menu.getSlot(1).set(new ItemStack(Items.BREAD,8));menu.getSlot(2).set(new ItemStack(Items.COOKED_BEEF,4));
            screen=new LunchBoxScreen(menu,inv,Component.literal("Lunch Box"));screen.init(minecraft,width,height);
        }
        @Override public void render(GuiGraphics g,int x,int y,float partial){
            screen.render(g,scene==2?(width-268)/2+14:-100,scene==2?(height-174)/2+53:-100,partial);
        }
    }
}
