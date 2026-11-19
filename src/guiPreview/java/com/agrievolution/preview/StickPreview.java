package com.agrievolution.preview;
import com.agrievolution.content.AEBlocks;
import com.agrievolution.content.AgriCropBlock;
import com.agrievolution.content.CropSupportItem;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
@EventBusSubscriber(modid="agri_evolution",value=Dist.CLIENT)
public final class StickPreview {
    static int ticks,stage;
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        if(!Boolean.getBoolean("agri.stickPreview"))return;
        var mc=Minecraft.getInstance();if(mc.screen==null||mc.getOverlay()!=null||++ticks<50)return;ticks=0;
        if(stage++==0){mc.options.guiScale().set(2);mc.resizeDisplay();mc.setScreen(new Preview());}
        else{Screenshot.grab(mc.gameDirectory,"crop-stick-art.png",mc.getMainRenderTarget(),m->System.out.println(m.getString()));mc.stop();}
    }
    static class Preview extends Screen{
        Preview(){super(Component.literal("Crop sticks"));}
        public void render(GuiGraphics g,int mx,int my,float partial){
            g.fill(0,0,width,height,0xFF30271F);
            var labels = new String[]{"Wood", "Iron", "Obsidian"};
            for(int i=1;i<=3;i++){
                int x=width/2+(i-2)*140;
                g.fill(x-65,12,x+65,height-12,0xFF4A3B2C);
                g.drawCenteredString(font,labels[i-1],x,24,0xFFF2E2B8);
                var icon = new ItemStack(CropSupportItem.itemFor(i));
                g.pose().pushPose();g.pose().translate(x-39,40,0);g.pose().scale(4,4,1);
                g.renderItem(icon,0,0);g.pose().popPose();
                g.fill(x+31,78,x+51,98,0xFF7B6F5C);
                g.renderItem(icon,x+33,80);
                g.flush();
                Lighting.setupFor3DItems();
                var pose = g.pose();
                pose.pushPose();
                pose.translate(x,height-62,150);
                pose.scale(75,-75,75);
                pose.mulPose(Axis.XP.rotationDegrees(25));
                pose.mulPose(Axis.YP.rotationDegrees(-30));
                draw(g,Blocks.FARMLAND.defaultBlockState().setValue(net.minecraft.world.level.block.FarmBlock.MOISTURE,7),-1);
                var crop = AEBlocks.TOMATO_CROP.get().defaultBlockState().setValue(AgriCropBlock.AGE,4)
                        .setValue(AgriCropBlock.SUPPORTED,true).setValue(AgriCropBlock.SUPPORT_TIER,i);
                draw(g,crop,0);
                pose.popPose();
                Lighting.setupForFlatItems();
            }
        }
        private void draw(GuiGraphics g,BlockState state,int y){
            var pose=g.pose();
            pose.pushPose();
            pose.translate(-0.5,y,-0.5);
            var buffers=minecraft.renderBuffers().bufferSource();
            minecraft.getBlockRenderer().renderSingleBlock(state,pose,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
            buffers.endBatch();
            pose.popPose();
        }
    }
}
