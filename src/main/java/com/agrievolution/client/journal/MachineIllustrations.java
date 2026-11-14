package com.agrievolution.client.journal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;

/** Native item/model diagrams: these always use current in-game artwork. */
public final class MachineIllustrations {
    private static void text(GuiGraphics g,Font f,String s,int x,int y){g.drawString(f,JournalFonts.body(s),x,y,JournalTheme.INK,false);}
    private static void item(GuiGraphics g,String id,int x,int y){g.renderItem(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id))),x,y);}
    public static boolean render(GuiGraphics g,Font f,String title,boolean compact,int mx,int my) {
        if(title.startsWith("CRAFT ")){recipe(g,f,Integer.parseInt(title.substring(6)),compact,mx,my);return true;}
        int left=compact?22:38,right=compact?202:228,y=compact?89:113;
        if(title.equals("RESERVOIR BLUEPRINT")) {
            String[] labels={"Base","Middle","Roof"};
            for(int layer=0;layer<3;layer++) {
                text(g,f,labels[layer],left+layer*53,y-12);
                for(int row=0;row<3;row++)for(int col=0;col<3;col++)item(g,layer==1&&row==1&&col==1?"minecraft:water_bucket":"minecraft:copper_block",left+layer*53+col*16,y+row*16);
            }
            text(g,f,"26 copper + 1 water source",left,y+57);
            item(g,"agri_evolution:spanner",left+60,y+compactOffset(compact));
            lines(g,f,right,y,new String[]{"Stack these three layers.","Use fresh, unwaxed copper.","Place water in the centre.","Close the top copper layer.","Use the Spanner on shell.","Refill: water buckets."},compact);return true;
        }
        if(title.equals("IRRIGATION LAYOUT")) {
            String[] ids={"water_reservoir","irrigation_pipe","irrigation_valve","irrigation_pipe","irrigation_sprinkler"};
            // Reservoir has no obtainable item: use its copper shell as the source symbol.
            item(g,"minecraft:copper_block",left,y+8);
            for(int i=1;i<5;i++){text(g,f,">",left+i*32-12,y+13);item(g,"agri_evolution:"+ids[i],left+i*32,y+8);}
            text(g,f,"Tank > pipes > valve > head",left,y+39);
            item(g,"minecraft:wheat",left+128,y+59);text(g,f,"Crops below",left+48,y+64);
            lines(g,f,right,y,new String[]{"Use a centre tank port.","Pipes bend and branch.","Valve accepts every side.","Nozzles point downward.","Close valve to stop spray.","Powered valve also stops."},compact);return true;
        }
        if(title.equals("GREENHOUSE PLAN")) {
            for(int row=0;row<7;row++)for(int col=0;col<7;col++) {
                boolean wall=row==0||row==6||col==0||col==6;
                int color=wall?0xFF87B6BA:0xFF786039;
                g.fill(left+col*11,y+row*11,left+col*11+10,y+row*11+10,color);
            }
            item(g,"agri_evolution:botanical_greenhouse",left+29,y+60);
            text(g,f,"7 x 7 shell",left+86,y+15);text(g,f,"5 x 5 beds",left+86,y+31);
            lines(g,f,right,y,new String[]{"Top view: glass perimeter.","Controller and valid door.","Build walls and glass roof.","Follow the hologram height.","Sprinkler can replace", "one roof glass block."},compact);return true;
        }
        return false;
    }
    private static int compactOffset(boolean compact){return compact?70:84;}
    private static void lines(GuiGraphics g,Font f,int x,int y,String[] lines,boolean compact){for(int i=0;i<lines.length;i++)text(g,f,lines[i],x,y+i*(compact?13:17));}
    private static ItemStack fallback(String id) {
        if(id.isEmpty())return ItemStack.EMPTY;
        if(!id.startsWith("#"))return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
        var values=Ingredient.of(TagKey.create(Registries.ITEM,ResourceLocation.parse(id.substring(1)))).getItems();
        if(values.length>0)return values[(int)(System.currentTimeMillis()/1500%values.length)];
        // Representative icons when previewing without a connected world's tag registry.
        return new ItemStack(id.contains("log")?Items.OAK_LOG:id.contains("plank")?Items.OAK_PLANKS:Items.GLASS);
    }
    private static void recipe(GuiGraphics g,Font f,int index,boolean compact,int mx,int my) {
        var spec=MachineRecipeCatalog.ALL[index];var mc=Minecraft.getInstance();
        ItemStack[] inputs=java.util.Arrays.stream(spec.ingredients()).map(MachineIllustrations::fallback).toArray(ItemStack[]::new);
        ItemStack output=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(spec.result())),spec.count());int width=spec.width();
        if(mc.level!=null) {
            var holder=mc.level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("agri_evolution",spec.id())).orElse(null);
            if(holder!=null&&holder.value() instanceof CraftingRecipe recipe) {
                inputs=recipe.getIngredients().stream().map(i->{var a=i.getItems();return a.length==0?ItemStack.EMPTY:a[(int)(System.currentTimeMillis()/1500%a.length)];}).toArray(ItemStack[]::new);
                output=recipe.getResultItem(mc.level.registryAccess());width=recipe instanceof ShapedRecipe shaped?shaped.getWidth():3;
            } else {
                text(g,f,"Recipe unavailable in this world.",compact?22:38,compact?100:120);return;
            }
        }
        int left=compact?22:38,right=compact?202:228,y=compact?87:111;
        ItemStack hovered=ItemStack.EMPTY;
        for(int row=0;row<3;row++)for(int col=0;col<3;col++) {
            int x=left+15+col*20,cy=y+row*20;com.agrievolution.client.GardenGuiTheme.slot(g,x,cy);
            int slot=row*width+col;if(col<width&&slot<inputs.length){g.renderItem(inputs[slot],x,cy);if(mx>=x&&mx<x+16&&my>=cy&&my<cy+16)hovered=inputs[slot];}
        }
        text(g,f,">",left+90,y+24);g.renderItem(output,left+118,y+20);g.renderItemDecorations(f,output,left+118,y+20);
        var name=Component.literal(output.getHoverName().getString()).withStyle(Style.EMPTY.withFont(JournalFonts.BODY_FONT));
        int lineY=y;for(var line:f.split(name,150)){g.drawString(f,line,right,lineY,JournalTheme.GREEN,false);lineY+=12;}
        text(g,f,"Makes "+output.getCount(),right,lineY+12);
        text(g,f,MachineRecipeCatalog.isFood(spec.id())?"Any order in crafting table.":"Use a crafting table.",right,lineY+29);
        var food=output.get(net.minecraft.core.component.DataComponents.FOOD);
        text(g,f,food==null?"Hover ingredients for names.":"Restores "+food.nutrition()+" hunger points.",right,lineY+46);
        if(!hovered.isEmpty())g.renderTooltip(f,hovered.getHoverName(),mx,my);
    }
    private MachineIllustrations(){}
}
