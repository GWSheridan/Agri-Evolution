package com.agrievolution.preview;

import com.agrievolution.client.journal.*;
import com.agrievolution.content.AEItems;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Disposable world-free visual checks; never shipped. */
@EventBusSubscriber(modid="agri_evolution",value=Dist.CLIENT)
public final class JournalIndexPreview {
    private static int ticks,index=-1;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(!Boolean.getBoolean("agri.journalIndexPreview"))return;
        var mc=Minecraft.getInstance();if(mc.screen==null||mc.getOverlay()!=null||++ticks<40)return;ticks=0;
        if(index>=0)Screenshot.grab(mc.gameDirectory,"journal-index-"+index+".png",mc.getMainRenderTarget(),m->System.out.println(m.getString()));
        if(++index>=8){mc.stop();return;}
        mc.options.guiScale().set(2);mc.resizeDisplay();mc.setScreen(new Preview(index));
    }
    private static final class Preview extends Screen {
        final int scene;Screen shown;
        Preview(int scene){super(Component.literal("Journal index QA"));this.scene=scene;}
        @Override protected void init(){
            boolean machines=scene==2||scene==3||scene==6||scene==7;
            var stack=new ItemStack(machines?AEItems.JOURNAL.get():AEItems.BREEDING_JOURNAL.get());
            var parent=new JournalScreen(stack);int sw=scene%2==1?400:width,sh=scene%2==1?240:height;
            if(scene<2){shown=parent;shown.init(minecraft,sw,sh);return;}
            shown=new JournalIndexScreen(parent,new JournalNavigation(machines),JournalSnapshot.from(stack));shown.init(minecraft,sw,sh);
            int w=Math.min(420,sw-16),h=Math.min(260,sh-16),left=(sw-w)/2,top=(sh-h)/2;
            if(scene==3||scene==7){shown.mouseClicked(left+25,top+34,0);for(char c:(scene==3?"iron":"no-such-page").toCharArray())shown.charTyped(c,0);}
            if(scene==4||scene==5)shown.mouseClicked(left+w/2,top+56,0);
            if(scene==6){
                boolean saved=shown.children().stream().anyMatch(c->c instanceof net.minecraft.client.gui.components.Button b&&b.getX()==left+w-34&&b.getY()==top+73&&b.getMessage().getString().equals("*"));
                if(!saved)shown.mouseClicked(left+w-22,top+81,0);
                shown=new JournalIndexScreen(parent,new JournalNavigation(true),JournalSnapshot.from(stack));shown.init(minecraft,sw,sh);
                shown.mouseClicked(left+50,top+56,0);
                if(shown.children().stream().noneMatch(c->c instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().equals("*")))throw new AssertionError("Bookmark did not persist after reopening");
            }
        }
        @Override public void render(GuiGraphics g,int x,int y,float partial){shown.render(g,-100,-100,partial);}
    }
}
