package com.agrievolution.preview;
import com.agrievolution.client.greenhouse.BotanicalGreenhouseSeedBankScreen;
import com.agrievolution.content.*;
import com.agrievolution.genetics.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
@EventBusSubscriber(modid="agri_evolution",value=Dist.CLIENT)
public final class SelectivePlantingPreview {
    private static int ticks,index=-1;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(!Boolean.getBoolean("agri.selectivePlantingPreview"))return;
        var mc=Minecraft.getInstance();if(mc.screen==null||mc.getOverlay()!=null||++ticks<40)return;ticks=0;
        if(index>=0)Screenshot.grab(mc.gameDirectory,"planting-presets-"+index+".png",mc.getMainRenderTarget(),m->System.out.println(m.getString()));
        if(++index>=4){mc.stop();return;}mc.options.guiScale().set(2);mc.resizeDisplay();mc.setScreen(new Preview(index));
    }
    private static final class Preview extends Screen{
        final int scene;BotanicalGreenhouseSeedBankScreen screen;
        Preview(int scene){super(Component.literal("Selective planting QA"));this.scene=scene;}
        @Override protected void init(){
            var inv=new Inventory(null);var menu=new BotanicalGreenhouseSeedBankMenu(1,inv);
            for(int i=0;i<9;i++){menu.setData(i,1);menu.setData(9+i,1);}
            menu.setData(18,32);menu.setData(19,1);
            var species=new CropSpecies[]{CropSpecies.COPPER,CropSpecies.TOMATO,CropSpecies.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE};
            for(int i=0;i<3;i++){var seed=new ItemStack(AEItems.seedFor(species[i]),64);GenomeStorage.putOnSeed(seed,new CropGenome(60,70,80));menu.getSlot(i).set(seed);menu.setData(20+i,species[i].ordinal()+1);}
            menu.setData(29,50);menu.setData(30,60);menu.setData(31,70);
            for(int i=0;i<9;i++){menu.setData(32+i,i<3?0:1);menu.setData(41+i,i<3?32:0);}
            if(scene==2){menu.setData(32,24);menu.setData(41,0);}
            if(scene==1||scene==3){menu.setData(50,2);menu.setData(51,1);}
            screen=new BotanicalGreenhouseSeedBankScreen(menu,inv,Component.literal("Seed Bank"));screen.init(minecraft,width,height);
            if(scene>0)screen.mouseClicked((width-250)/2+25,(height-265)/2+155,0);
        }
        @Override public void render(GuiGraphics g,int x,int y,float partial){
            int mx=scene>=2?(width-250)/2+205:-100,my=scene>=2?(height-265)/2+136:-100;
            screen.render(g,mx,my,partial);
        }
    }
}
