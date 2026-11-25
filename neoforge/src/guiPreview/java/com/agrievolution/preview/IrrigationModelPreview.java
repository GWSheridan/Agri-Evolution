package com.agrievolution.preview;

import com.agrievolution.content.*;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Actual baked block models, isolated from all saves. Excluded from release JAR. */
@EventBusSubscriber(modid="agri_evolution",value=Dist.CLIENT)
public final class IrrigationModelPreview {
    private static int ticks,index=-1;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("agri.irrigationPreview")) return;
        var mc=Minecraft.getInstance();
        if(mc.screen==null || mc.getOverlay()!=null || ++ticks<50) return;
        ticks=0;
        if(index>=0 && index<5) Screenshot.grab(mc.gameDirectory,(Boolean.getBoolean("agri.farmlandPreview")?"farmland-v2-":Boolean.getBoolean("agri.basinPreview")?"wooden-basins-":Boolean.getBoolean("agri.reservoirPreview")?"reservoir-":"irrigation-fixes-")+index+".png",mc.getMainRenderTarget(),m->System.out.println(m.getString()));
        if(++index>=5) {mc.stop();return;}
        mc.options.guiScale().set(2); mc.resizeDisplay(); mc.setScreen(new Preview(index));
    }
    private static final class Preview extends Screen {
        private final int scene;
        Preview(int scene){super(Component.literal("Irrigation model QA"));this.scene=scene;}
        @Override public void render(GuiGraphics g,int mx,int my,float partial) {
            if(Boolean.getBoolean("agri.farmlandPreview")){renderFarmland(g);return;}
            if(Boolean.getBoolean("agri.basinPreview")){renderBasins(g);return;}
            if(Boolean.getBoolean("agri.reservoirPreview")){renderReservoir(g);return;}
            g.fill(0,0,width,height,0xFFBAC9B0);
            g.drawCenteredString(font,scene==0?"Downward sprinkler":scene==1?"Roof fitting / valve above":"Crop supports: wood / iron / obsidian",width/2,16,0xFFFFFFFF);
            g.flush(); Lighting.setupFor3DItems();
            var pose=g.pose(); pose.pushPose();
            pose.translate(width/2.0,height/2.0+50,150);
            pose.scale(65,-65,65); pose.mulPose(Axis.XP.rotationDegrees(-20));pose.mulPose(Axis.YP.rotationDegrees(-35));
            if(scene==0) {
                draw(g,AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState().setValue(IrrigationSprinklerBlock.connection(Direction.WEST),true),0,0,0);
                draw(g,AEBlocks.IRRIGATION_VALVE.get().defaultBlockState().setValue(IrrigationValveBlock.FACING,Direction.EAST),-1,0,0);
            } else if(scene==1) {
                for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++) if(x!=0 || z!=0)
                    draw(g,net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState(),x,0,z);
                draw(g,AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState().setValue(IrrigationSprinklerBlock.connection(Direction.UP),true),0,0,0);
                draw(g,AEBlocks.IRRIGATION_VALVE.get().defaultBlockState().setValue(IrrigationValveBlock.FACING,Direction.DOWN),0,1,0);
            } else if(scene==3) {
                var junction=AEBlocks.IRRIGATION_PIPE.get().defaultBlockState();
                for(var d:Direction.values())junction=junction.setValue(IrrigationSprinklerBlock.connection(d),true);
                draw(g,junction,0,0,0);
                for(var d:Direction.values())for(int n=1;n<=2;n++)draw(g,AEBlocks.IRRIGATION_PIPE.get().defaultBlockState()
                        .setValue(IrrigationSprinklerBlock.connection(d),true).setValue(IrrigationSprinklerBlock.connection(d.getOpposite()),true),d.getStepX()*n,d.getStepY()*n,d.getStepZ()*n);
            } else if(scene==4) {
                draw(g,AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState().setValue(IrrigationSprinklerBlock.connection(Direction.UP),true),0,-1,0);
                var valve=AEBlocks.IRRIGATION_VALVE.get().defaultBlockState().setValue(IrrigationValveBlock.FACING,Direction.DOWN);
                for(var d:new Direction[]{Direction.WEST,Direction.EAST,Direction.DOWN})valve=valve.setValue(IrrigationSprinklerBlock.connection(d),true);
                draw(g,valve,0,0,0);
                for(int x:new int[]{-2,-1,1,2})draw(g,AEBlocks.IRRIGATION_PIPE.get().defaultBlockState()
                        .setValue(IrrigationSprinklerBlock.connection(Direction.WEST),true).setValue(IrrigationSprinklerBlock.connection(Direction.EAST),true),x,0,0);
            } else {
                for(int tier=1;tier<=3;tier++)draw(g,AEBlocks.AGRI_CROP.get().defaultBlockState().setValue(AgriCropBlock.AGE,7).setValue(AgriCropBlock.SUPPORTED,true).setValue(AgriCropBlock.SUPPORT_TIER,tier),(tier-2)*1.5,0,0);
            }
            pose.popPose(); Lighting.setupForFlatItems();
        }
        private void renderFarmland(GuiGraphics g){
            g.fill(0,0,width,height,0xFFB9C4AF);
            var soils=new net.minecraft.world.level.block.Block[]{AEBlocks.CULTIVATED_FARMLAND.get(),AEBlocks.NUTRIENT_FARMLAND.get(),AEBlocks.MYTHIC_FARMLAND.get(),AEBlocks.ARCANE_FARMLAND.get(),AEBlocks.CELESTIAL_FARMLAND.get()};
            g.drawCenteredString(font,scene==0?"Dry soil - Cultivated / Nutrient / Mythic / Arcane / Celestial":scene==1?"Watered soil - all five tiers":"Joined farmland - furrows and side faces",width/2,18,0xFF203B27);
            g.flush();Lighting.setupFor3DItems();
            if(scene<2){for(int i=0;i<5;i++){
                var pose=g.pose();pose.pushPose();pose.translate((i+.5)*width/5.0,height/2.0+35,150);pose.scale(60,-60,60);pose.mulPose(Axis.XP.rotationDegrees(30));pose.mulPose(Axis.YP.rotationDegrees(-28));
                draw(g,soils[i].defaultBlockState().setValue(net.minecraft.world.level.block.FarmBlock.MOISTURE,scene==0?0:7),0,0,0);pose.popPose();
            }}else{
                var pose=g.pose();pose.pushPose();pose.translate(width/2.0,height/2.0+42,150);pose.scale(52,-52,52);pose.mulPose(Axis.XP.rotationDegrees(40));pose.mulPose(Axis.YP.rotationDegrees(-20));
                for(int x=-2;x<=2;x++)for(int z=-1;z<=1;z++)draw(g,soils[scene==2?x+2:scene==3?0:4].defaultBlockState().setValue(net.minecraft.world.level.block.FarmBlock.MOISTURE,7),x,0,z);
                pose.popPose();
            }Lighting.setupForFlatItems();
        }
        private void renderBasins(GuiGraphics g){
            g.fill(0,0,width,height,0xFFBAC9B0);
            g.drawCenteredString(font,scene<2?(scene==0?"Wood / Copper Mk II / Amethyst Mk III - filled":"Empty basins - wooden interiors"):("Basin Mk "+(scene-1)+" - animated water"),width/2,20,0xFF203B27);
            g.flush();Lighting.setupFor3DItems();var pose=g.pose();pose.pushPose();
            pose.translate(width/2.0,height/2.0+35,150);float size=scene<2?85:160;pose.scale(size,-size,size);
            pose.mulPose(Axis.XP.rotationDegrees(32));pose.mulPose(Axis.YP.rotationDegrees(-25));
            if(scene<2)for(int tier=0;tier<3;tier++)draw(g,AEBlocks.IRRIGATION_BASIN.get().defaultBlockState().setValue(IrrigationBasinBlock.UPGRADE,tier).setValue(IrrigationBasinBlock.FILLED,scene==0),(tier-1)*1.7,0,0);
            else draw(g,AEBlocks.IRRIGATION_BASIN.get().defaultBlockState().setValue(IrrigationBasinBlock.UPGRADE,scene-2).setValue(IrrigationBasinBlock.FILLED,true),0,0,0);
            pose.popPose();Lighting.setupForFlatItems();
        }
        private void renderReservoir(GuiGraphics g){
            g.fill(0,0,width,height,0xFFBAC9B0);
            g.drawCenteredString(font,scene==0?"Reservoir - Pipes - Valve - Sprinkler":scene==1?"Full reservoir":"Empty reservoir",width/2,16,0xFFFFFFFF);
            g.flush();Lighting.setupFor3DItems();var pose=g.pose();pose.pushPose();
            pose.translate(width/2.0,height/2.0+70,150);pose.scale(40,-40,40);pose.mulPose(Axis.XP.rotationDegrees(20));pose.mulPose(Axis.YP.rotationDegrees(scene==0?-35:145));
            int offset=scene==0?-3:-1;
            for(int x=0;x<3;x++)for(int y=0;y<3;y++)for(int z=0;z<3;z++)draw(g,AEBlocks.WATER_RESERVOIR.get().defaultBlockState()
                    .setValue(WaterReservoirBlock.X,x).setValue(WaterReservoirBlock.Y,y).setValue(WaterReservoirBlock.Z,z).setValue(WaterReservoirBlock.FILL,scene==2?0:4),offset+x,y,z-1);
            if(scene==0){
                for(int x=0;x<=1;x++)draw(g,AEBlocks.IRRIGATION_PIPE.get().defaultBlockState().setValue(IrrigationSprinklerBlock.connection(Direction.WEST),true).setValue(IrrigationSprinklerBlock.connection(Direction.EAST),true),x,1,0);
                draw(g,AEBlocks.IRRIGATION_VALVE.get().defaultBlockState().setValue(IrrigationValveBlock.FACING,Direction.EAST),2,1,0);
                draw(g,AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState().setValue(IrrigationSprinklerBlock.connection(Direction.WEST),true),3,1,0);
            }
            pose.popPose();Lighting.setupForFlatItems();
        }
        private void draw(GuiGraphics g,BlockState state,double x,double y,double z) {
            var pose=g.pose();pose.pushPose();pose.translate(x-.5,y,z-.5);
            var buffers=minecraft.renderBuffers().bufferSource();
            minecraft.getBlockRenderer().renderSingleBlock(state,pose,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
            buffers.endBatch();pose.popPose();
        }
    }
}

