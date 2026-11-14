package com.agrievolution.preview;

import com.agrievolution.content.*;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Baked-model visual checks, excluded from release JARs and player worlds. */
@EventBusSubscriber(modid="agri_evolution",value=Dist.CLIENT)
public final class CropModelPreview {
    private static int ticks,index=-1;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(!Boolean.getBoolean("agri.cropPreview"))return;
        var mc=Minecraft.getInstance();
        if(mc.screen==null||mc.getOverlay()!=null||++ticks<40)return;
        ticks=0;
        if(index>=0)Screenshot.grab(mc.gameDirectory,"crossed-crops-"+index+".png",mc.getMainRenderTarget(),m->System.out.println(m.getString()));
        if(++index>=4){mc.stop();return;}
        mc.options.guiScale().set(2);mc.resizeDisplay();mc.setScreen(new Preview(index));
    }
    private static final class Preview extends Screen {
        final int scene;
        Preview(int scene){super(Component.literal("Crossed crop model QA"));this.scene=scene;}
        @Override public void render(GuiGraphics g,int mx,int my,float partial){
            g.fill(0,0,width,height,0xFFBAC9B0);
            g.drawCenteredString(font,scene==0?"Centred plants: Diamond / Amethyst / Tomato":scene==1?"Overhead: two intersecting planes":scene==2?"Eight growth stages":"Supports and weeds retained",width/2,12,0xFF203B27);
            g.flush();Lighting.setupFor3DItems();var pose=g.pose();pose.pushPose();
            pose.translate(width/2.0,height/2.0+40,150);float size=scene==2?48:85;pose.scale(size,-size,size);
            pose.mulPose(Axis.XP.rotationDegrees(scene==1?80:25));pose.mulPose(Axis.YP.rotationDegrees(scene==1?0:-25));
            if(scene==2){for(int age=0;age<8;age++)plant(g,AEBlocks.DIAMOND_CROP.get().defaultBlockState().setValue(AgriCropBlock.AGE,age),(age-3.5)*1.05);}
            else {
                var crops=new AgriCropBlock[]{AEBlocks.DIAMOND_CROP.get(),AEBlocks.AMETHYST_CROP.get(),AEBlocks.TOMATO_CROP.get()};
                for(int i=0;i<3;i++){
                    var state=crops[i].defaultBlockState().setValue(AgriCropBlock.AGE,7);
                    if(scene==3)state=state.setValue(AgriCropBlock.SUPPORTED,true).setValue(AgriCropBlock.SUPPORT_TIER,i+1).setValue(AgriCropBlock.WEEDY,true);
                    plant(g,state,(i-1)*1.5);
                }
            }
            pose.popPose();Lighting.setupForFlatItems();
        }
        private void plant(GuiGraphics g,BlockState crop,double x){draw(g,Blocks.FARMLAND.defaultBlockState(),x,-1);draw(g,crop,x,0);}
        private void draw(GuiGraphics g,BlockState state,double x,double y){
            var pose=g.pose();pose.pushPose();pose.translate(x-.5,y,-.5);
            var buffers=minecraft.renderBuffers().bufferSource();
            minecraft.getBlockRenderer().renderSingleBlock(state,pose,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
            buffers.endBatch();pose.popPose();
        }
    }
}
