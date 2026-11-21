package com.agrievolution.client;

import com.agrievolution.content.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.util.UUID;

/** World-free previews of all four tabs at two logical sizes. */
@EventBusSubscriber(modid="agri_evolution",value=Dist.CLIENT)
public final class FarmLedgerPreview {
    private static int ticks,index=-1;
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        if(!Boolean.getBoolean("agri.ledgerPreview"))return;var mc=Minecraft.getInstance();
        if(mc.screen==null||mc.getOverlay()!=null||++ticks<30)return;ticks=0;
        if(index>=0)Screenshot.grab(mc.gameDirectory,"ledger-"+index+".png",mc.getMainRenderTarget(),m->System.out.println("LEDGER_CAPTURE: "+m.getString()));
        if(++index>=8){mc.stop();return;}mc.options.guiScale().set(2);mc.resizeDisplay();mc.setScreen(new Preview(index));
    }
    private static final class Preview extends Screen {
        private final int index;private FarmLedgerScreen inner;private FarmLedgerData.Field fixture;private CompoundTag response;private int vw,vh;
        Preview(int index){super(Component.literal("Farm Ledger previews"));this.index=index;}
        @Override protected void init(){
            vw=index<4?320:width;vh=index<4?240:height;
            var tag=new CompoundTag();tag.putUUID("Id",UUID.randomUUID());tag.putUUID("Owner",UUID.randomUUID());tag.putString("Name","North resource beds");
            tag.putString("Dimension","minecraft:overworld");tag.putInt("Width",32);tag.putInt("Depth",32);tag.putString("Name","North resource beds");fixture=FarmLedgerData.readField(tag);
            for(int i=0;i<fixture.size();i++){fixture.layouts[0].seeds[i]=i%2==0?"agri_evolution:iron_seed":"minecraft:wheat_seeds";fixture.actual[i]=i%4==0?"minecraft:wheat":"";fixture.flags[i]=i%7==0?0:3;fixture.observed[i]=System.currentTimeMillis();}
            fixture.surveyedAt=System.currentTimeMillis();fixture.invited.put(UUID.randomUUID(),true);fixture.machineStatus.add("10, 64, 18 — Reservoir: 12000 mB");
            response=new CompoundTag();var list=new ListTag();list.add(fixture.summary());response.put("Fields",list);response.put("Selected",fixture.save());response.putBoolean("Nearby",true);response.putInt("Pages",1);
            install();var inv=new Inventory(null);inner=new FarmLedgerScreen(new FarmLedgerMenu(1,inv),inv,Component.literal("Farm Ledger"));inner.init(minecraft,vw,vh);
            int left=(vw-Math.min(vw-12,600))/2,top=(vh-Math.min(vh-12,366))/2;
            inner.mouseClicked(left+8+(index%4)*(Math.min(vw-12,600)-16)/4+12,top+36,0);
            for(var child:inner.children())if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget){
                if(widget.getX()<0||widget.getY()<0||widget.getX()+widget.getWidth()>vw||widget.getY()+widget.getHeight()>vh)throw new IllegalStateException("Ledger control outside viewport");
            }
        }
        private void install(){FarmLedgerClient.selected=fixture;FarmLedgerClient.latest=response;}
        @Override public void render(GuiGraphics g,int mx,int my,float partial){install();g.fill(0,0,width,height,0xff171d19);g.enableScissor(0,0,vw,vh);inner.render(g,-100,-100,partial);g.disableScissor();if(vw<width)g.drawString(font,"320 x 240 logical pixels",vw+6,8,0xffeeeecc);}
        @Override public boolean isPauseScreen(){return false;}
    }
}
