package com.agrievolution.client.journal;

import com.agrievolution.genetics.CropSpecies;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.Font;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Native-size approved crop sprites and Minecraft item pictures: no resampled GUI artwork. */
public final class JournalIllustrations {
    private static void text(GuiGraphics g,Font f,String s,int x,int y){g.drawString(f,JournalFonts.body(s),x,y,JournalTheme.INK,false);}
    private static void wrap(GuiGraphics g,Font f,String s,int x,int y,int width){
        var component=net.minecraft.network.chat.Component.literal(s).withStyle(net.minecraft.network.chat.Style.EMPTY.withFont(JournalFonts.BODY_FONT));
        for(var line:f.split(component,width)){g.drawString(f,line,x,y,JournalTheme.INK,false);y+=10;}
    }
    private static void crop(GuiGraphics g,CropSpecies species,int x,int y,boolean compact){
        if(compact)JournalRenderer.currentCrop(g,species,x,y,16);
        else JournalRenderer.speciesIcon(g,species,true,x,y,1);
    }
    private static void seed(GuiGraphics g,CropSpecies species,int x,int y){g.renderItem(new ItemStack(com.agrievolution.content.AEItems.seedFor(species)),x,y);}
    private static void item(GuiGraphics g,String id,int x,int y){g.renderItem(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id))),x,y);}
    public static void recipe(GuiGraphics g,Font f,IllustratedBreedingRecipes.Recipe r,boolean compact){
        int l=compact?22:38,right=compact?202:228,y=compact?85:108;
        if(r.method()==IllustratedBreedingRecipes.Method.FIELD)crop(g,r.first(),l+8,y,compact);else seed(g,r.first(),l+8,y);text(g,f,"+",l+72,y+8);
        if(r.second()!=null){if(r.method()==IllustratedBreedingRecipes.Method.FIELD)crop(g,r.second(),l+100,y,compact);else seed(g,r.second(),l+100,y);}else item(g,r.material(),l+100,y);
        int names=y+(compact?22:38);
        wrap(g,f,r.method()==IllustratedBreedingRecipes.Method.CRAFT?"Wild Seed":r.first().displayName(),l,names,80);
        wrap(g,f,r.second()!=null?r.second().displayName():r.material().endsWith("dirt")?"Dirt":"Any Log",l+90,names,80);
        text(g,f,r.method()==IllustratedBreedingRecipes.Method.CRAFT?"Use a crafting grid.":"Either parent order works.",l,compact?138:186);
        text(g,f,r.method()==IllustratedBreedingRecipes.Method.CRAFT?"One Wild Seed + material.":r.method()==IllustratedBreedingRecipes.Method.LAB?"Use two parent seed stacks.":"Cross mature field crops.",l,compact?152:201);
        seed(g,r.result(),right+5,y);
        wrap(g,f,new ItemStack(com.agrievolution.content.AEItems.seedFor(r.result())).getHoverName().getString(),right+(compact?29:45),y+3,compact?130:123);
        text(g,f,"Base success: "+r.chance()+"%",right,compact?116:153);
        String method=switch(r.method()){case CRAFT->"Crafting recipe";case FIELD->"Field or Breeding Station";case LAB->"Tier 3 Genetics Lab ONLY";};
        text(g,f,method,right,compact?130:171);
        text(g,f,r.method()==IllustratedBreedingRecipes.Method.LAB?"Needs 1 Nutrient Compost":r.method()==IllustratedBreedingRecipes.Method.FIELD?"Supports add 5/8/12 points.":"No random roll required.",right,compact?144:188);
        if(r.method()!=IllustratedBreedingRecipes.Method.CRAFT)text(g,f,"Miss: one parent species.",right,compact?158:205);
    }
    public static void tips(GuiGraphics g,Font f,int page,boolean compact){
        var tip=GrandmasTips.PAGES[page];int l=compact?22:38,r=compact?202:228,y=compact?84:105;
        text(g,f,tip.title(),l,compact?64:85);text(g,f,tip.subtitle(),r,compact?64:85);
        if(tip.diagram()==3){
            crop(g,CropSpecies.IRON,l+60,y,true);item(g,"minecraft:farmland",l+60,y+17);item(g,"minecraft:iron_block",l+60,y+34);
            text(g,f,"Crop",l+84,y+3);text(g,f,"Soil",l+84,y+20);text(g,f,"Catalyst",l+84,y+37);
        }else if(tip.diagram()==4){
            crop(g,CropSpecies.DIRT,l,y,compact);text(g,f,"+",l+36,y+8);crop(g,CropSpecies.WOOD,l+52,y,compact);
            text(g,f,">",l+91,y+8);crop(g,CropSpecies.STONE,l+112,y,compact);
        }else {
            crop(g,CropSpecies.CRIMSON_CLOVER,l+6,y,compact);crop(g,CropSpecies.AZURE_FLAX,l+112,y,compact);
            text(g,f,tip.diagram()==0?"A  >  B":tip.diagram()==1?"COMPARE":"25% base",l+44,y+9);
            if(tip.diagram()==0){text(g,f,"Sneak-use A",l,compact?111:144);text(g,f,"Use on B",l+98,compact?111:144);}
        }
        for(int i=0;i<tip.left().length;i++)text(g,f,tip.left()[i],l,(compact?139:177)+i*(compact?11:15));
        for(int i=0;i<tip.right().length;i++)text(g,f,tip.right()[i],r,(compact?85:110)+i*(compact?16:19));
        text(g,f,"TIP "+(page+1)+" / "+GrandmasTips.PAGES.length,r,compact?177:232);
        JournalRenderer.sprite(g,"arrow_left",compact?323:357,compact?171:223,compact?14:19,compact?14:19);
        JournalRenderer.sprite(g,"arrow_right",compact?347:381,compact?171:223,compact?14:19,compact?14:19);
    }
    private JournalIllustrations(){}
}
