package com.agrievolution.preview;

import com.agrievolution.client.soil.*;
import com.agrievolution.content.*;
import com.agrievolution.farming.SoilSurveyData;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** World-free checks of care guidance, compact hitboxes and the new item. */
@EventBusSubscriber(modid="agri_evolution",value=Dist.CLIENT)
public final class GrowingConditionsPreview {
    private static int ticks,index=-1;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("agri.growingPreview"))return;var mc=Minecraft.getInstance();
        if(mc.screen==null||mc.getOverlay()!=null||++ticks<30)return;ticks=0;
        if(index>=0)Screenshot.grab(mc.gameDirectory,"growing-"+index+".png",mc.getMainRenderTarget(),m->System.out.println("GROWING_CAPTURE: "+m.getString()));
        if(++index>=8){mc.stop();return;}mc.options.guiScale().set(2);mc.resizeDisplay();mc.setScreen(new Preview(index));
    }
    private static final class Preview extends Screen {
        private final int index;private Screen inner;private int vw,vh;
        Preview(int index){super(Component.literal("Growing conditions"));this.index=index;}
        @Override protected void init() {
            vw=index<4?320:width;vh=index<4?240:height;
            var sampler=new ItemStack(AEItems.SOIL_SAMPLE.get());var report=new CompoundTag();
            report.putString("PreviousFamily","Legumes");report.putString("CropFamily","Resources");report.putInt("RotationBonus",10);
            report.putInt("MulchTicks",19280);report.putString("ResourceId","iron");report.putString("Dimension","minecraft:overworld");SoilSurveyData.save(sampler,report);
            inner=switch(index%4){case 0->new SoilSamplerScreen(sampler);case 1->new CropCareGuideScreen(null,"netherite_upgrade_smithing_template");case 2->new CropCareGuideScreen(null,"iron");default->{var inv=new Inventory(null);yield new SoilResearchTableScreen(new SoilResearchTableMenu(1,inv),inv,Component.literal("Soil Research"));}};
            inner.init(minecraft,vw,vh);
            if(index%4==0){
                if(vw==320){for(int i=0;i<5;i++)inner.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT,0,org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL);inner.mouseClicked(260,66,0);for(int i=0;i<5;i++)inner.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT,0,org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL);inner.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN,0,org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL);}
                else inner.mouseClicked((vw-406)/2+350,(vh-16-246)/2+62,0);
            }
            if(index%4==2)for(var child:inner.children())if(child instanceof net.minecraft.client.gui.components.EditBox edit)edit.setValue("coal");
            if(index%4==3&&vw==320)for(int i=0;i<3;i++)inner.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN,0,org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL);
            if(index%4==1||index%4==2)for(var child:inner.children())if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget)
                if(widget.getX()<0||widget.getY()<0||widget.getRight()>vw||widget.getBottom()>vh)throw new IllegalStateException("Crop care control outside viewport");
        }
        @Override public void render(GuiGraphics g,int mx,int my,float partial) {
            g.fill(0,0,width,height,0xff171d19);g.enableScissor(0,0,vw,vh);inner.render(g,-100,-100,partial);g.disableScissor();
            if(vw<width)g.drawString(font,"320 x 240 logical pixels",vw+6,8,0xffeeeecc);
            g.pose().pushPose();g.pose().translate(vw+16,40,0);g.pose().scale(4,4,1);g.renderItem(new ItemStack(AEItems.COMPOST_MULCH.get()),0,0);g.pose().popPose();
        }
        @Override public boolean isPauseScreen(){return false;}
    }
}
