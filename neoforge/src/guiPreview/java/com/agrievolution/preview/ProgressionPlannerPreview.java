package com.agrievolution.preview;

import com.agrievolution.client.journal.*;
import com.agrievolution.content.AEItems;
import com.agrievolution.genetics.CropSpecies;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Isolated UI checks; no worlds are opened and this source set is never packaged. */
@EventBusSubscriber(modid="agri_evolution",value=Dist.CLIENT)
public final class ProgressionPlannerPreview {
    private static int ticks,index=-1;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(!Boolean.getBoolean("agri.plannerPreview"))return;
        var mc=Minecraft.getInstance();if(mc.screen==null||mc.getOverlay()!=null||++ticks<40)return;ticks=0;
        if(index>=0)Screenshot.grab(mc.gameDirectory,"progression-planner-"+index+".png",mc.getMainRenderTarget(),m->System.out.println(m.getString()));
        if(++index>=12){mc.stop();return;}
        mc.options.guiScale().set(2);mc.resizeDisplay();mc.setScreen(new Preview(index));
    }
    private static final class Preview extends Screen {
        final int scene;BreedingPlannerScreen shown;
        Preview(int scene){super(Component.literal("Progression planner QA"));this.scene=scene;}
        @Override protected void init(){
            var stack=new ItemStack(AEItems.BREEDING_JOURNAL.get());
            var root=new CompoundTag();var found=new ListTag();
            if(scene>=2&&scene<=5||scene>=8)for(var species:CropSpecies.values())
                if(scene>=8&&scene<10||species!=CropSpecies.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE&&species!=CropSpecies.AZURE_FLAX)found.add(StringTag.valueOf(species.id()));
            root.put("AgriDiscoveredSpecies",found);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(root));
            int sw=scene%2==1?400:width,sh=scene%2==1?240:height;
            shown=new BreedingPlannerScreen(new JournalScreen(stack),JournalSnapshot.from(stack));shown.init(minecraft,sw,sh);
            int w=Math.min(460,sw-16),h=Math.min(310,sh-16),left=(sw-w)/2,top=(sh-h)/2;
            var search=(EditBox)shown.children().stream().filter(c->c instanceof EditBox).findFirst().orElseThrow();
            search.setValue(scene<2?"Diamond":scene<6?"Silence":scene<8?"not-a-crop":scene<10?"Diamond":"Azure Flax");
            if(scene>=2&&scene<6||scene>=8)shown.mouseClicked(left+40,top+81,0);
            if(scene>=4&&scene<6||scene>=10){shown.mouseClicked(left+w-60,top+81,0);
                // Detail pagination must respond without losing the chosen target.
                shown.mouseClicked(left+50,top+h-16,0);shown.mouseClicked(left+20,top+h-16,0);
            }
        }
        @Override public void render(GuiGraphics g,int x,int y,float partial){shown.render(g,-100,-100,partial);}
    }
}
