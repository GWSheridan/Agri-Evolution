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

@EventBusSubscriber(modid="agri_evolution",value=Dist.CLIENT)
public final class GrandmasJournalPreview {
    private static int ticks,index=-1;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(!Boolean.getBoolean("agri.grandmasPreview")&&!Boolean.getBoolean("agri.machinePreview"))return;
        var mc=Minecraft.getInstance();if(mc.screen==null||mc.getOverlay()!=null||++ticks<40)return;ticks=0;
        if(index>=0)Screenshot.grab(mc.gameDirectory,(Boolean.getBoolean("agri.cookingPreview")?"cooking2-":Boolean.getBoolean("agri.machinePreview")?"machine-guides-":"resource16-grandmas-")+index+".png",mc.getMainRenderTarget(),m->System.out.println(m.getString()));
        if(++index>=(Boolean.getBoolean("agri.cookingPreview")?(Boolean.getBoolean("agri.foodPreview")?24:88):Boolean.getBoolean("agri.foodPreview")?24:10)){mc.stop();return;}
        mc.options.guiScale().set(2);mc.resizeDisplay();mc.setScreen(new Preview(index));
    }
    private static final class Preview extends Screen {
        final int scene;JournalScreen book;
        Preview(int scene){super(Component.literal("Grandmas Journal visual QA"));this.scene=scene;}
        @Override protected void init(){
            if(Boolean.getBoolean("agri.machinePreview")){
                book=new JournalScreen(new ItemStack(AEItems.JOURNAL.get()));book.init(minecraft,scene%2==1?400:width,scene%2==1?240:height);
                String title=scene<2?"RESERVOIR BLUEPRINT":scene<4?"IRRIGATION LAYOUT":scene<6?"GREENHOUSE PLAN":"";
                if(scene>=6){String id=scene<8?"iron_crop_support":"botanical_greenhouse";for(int i=0;i<MachineRecipeCatalog.ALL.length;i++)if(MachineRecipeCatalog.ALL[i].id().equals(id))title="CRAFT "+i;}
                var pages=MachineGuidePages.forBook(true);int page=0;while(page<pages.length&&!pages[page].title().equals(title))page++;
                for(int i=0;i<page;i++)book.keyPressed(262,0,0);return;
            }
            book=new JournalScreen(new ItemStack(AEItems.BREEDING_JOURNAL.get()));book.init(minecraft,scene%2==1?400:width,scene%2==1?240:height);
            if(Boolean.getBoolean("agri.cookingPreview")){
                book.keyPressed(53,0,0);
                var food=java.util.Arrays.stream(MachineRecipeCatalog.ALL).filter(r->MachineRecipeCatalog.isFood(r.id())).filter(r->!Boolean.getBoolean("agri.foodPreview")||java.util.Set.of("salmon_dill_sandwich","pulled_pork_slaw_roll","chickpea_salad_wrap","mutton_barley_soup","creamy_cauliflower_soup","spicy_lentil_soup","rhubarb_custard_tart","grape_oat_cookie","melon_berry_sundae","beef_garden_pie","garlic_cod_dinner","chicken_sesame_rice").contains(r.id())).toList().get(scene/2);
                int recipe=java.util.Arrays.asList(MachineRecipeCatalog.ALL).indexOf(food);
                var pages=MachineGuidePages.forBook(false);int page=0;
                while(page<pages.length&&!pages[page].title().equals("CRAFT "+recipe))page++;
                for(int i=0;i<page;i++)book.keyPressed(262,0,0);return;
            }
            if(Boolean.getBoolean("agri.foodPreview")){
                book.keyPressed(51,0,0); var foods=java.util.Arrays.stream(com.agrievolution.genetics.CropSpecies.values()).filter(c->c.ordinal()>=com.agrievolution.genetics.CropSpecies.OAK_SAPLING.ordinal()).toList();
                int page=IllustratedBreedingRecipes.ALL.indexOf(IllustratedBreedingRecipes.ALL.stream().filter(r->r.result()==foods.get(scene/2)).findFirst().orElseThrow())+1;
                for(int i=0;i<page;i++)book.keyPressed(262,0,0); return;
            }
            if(scene<6){
                book.keyPressed(51,0,0);
                int page=scene<2?1:scene<4?3:IllustratedBreedingRecipes.ALL.indexOf(IllustratedBreedingRecipes.ALL.stream().filter(r->r.method()==IllustratedBreedingRecipes.Method.LAB).findFirst().orElseThrow())+1;
                for(int i=0;i<page;i++)book.keyPressed(262,0,0);
            }else {book.keyPressed(54,0,0);if(scene>=8)for(int i=0;i<3;i++)book.keyPressed(262,0,0);}
        }
        @Override public void render(GuiGraphics g,int x,int y,float partial){book.render(g,-100,-100,partial);}
    }
}





