package com.agrievolution.client;

import com.agrievolution.AgriEvolution;
import com.agrievolution.content.*;
import java.lang.ref.WeakReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

@EventBusSubscriber(modid=AgriEvolution.MOD_ID,value=Dist.CLIENT)
public final class FarmInspectionClient {
    private static InspectionPayload latest;
    private static long received;
    private static WeakReference<ClientLevel> inspectedLevel = new WeakReference<>(null);
    private static Block inspectedBlock;

    public static void receive(InspectionPayload payload) {
        var level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().location().toString().equals(payload.dimension()) || !level.hasChunkAt(payload.pos())) return;
        latest = payload;
        received = System.nanoTime();
        inspectedLevel = new WeakReference<>(level);
        inspectedBlock = level.getBlockState(payload.pos()).getBlock();
    }

    @SubscribeEvent public static void hud(RenderGuiEvent.Post event){
        var mc=Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.screen != null || mc.options.hideGui
                || latest == null || inspectedLevel.get() != mc.level || System.nanoTime() - received > 1_500_000_000L
                || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
                || !hit.getBlockPos().equals(latest.pos()) || !mc.level.hasChunkAt(latest.pos()) || latest.text().isEmpty()) return;
        var state = mc.level.getBlockState(latest.pos());
        if (!state.is(inspectedBlock) || !FarmInspection.canInspect(state, FarmInspection.spanner(mc.player), FarmInspection.sampler(mc.player))) return;
        var gui=event.getGuiGraphics();int width=Math.min(310,gui.guiWidth()-16),y=8;
        var rows=mc.font.split(net.minecraft.network.chat.Component.literal(latest.text()),width-12);
        gui.fill(8,6,8+width,14+rows.size()*10,0xD91B3025);
        for(var row:rows){gui.drawString(mc.font,row,14,y,0xFFF1D5,false);y+=10;}
    }
    @SubscribeEvent public static void preview(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.screen!=null||mc.options.hideGui||!FarmInspection.spanner(mc.player)
            ||!(mc.hitResult instanceof BlockHitResult hit)||hit.getType()!=HitResult.Type.BLOCK
            ||!mc.level.hasChunkAt(hit.getBlockPos())
            ||!(mc.level.getBlockState(hit.getBlockPos()).getBlock() instanceof IrrigationSprinklerBlock))return;
        var pose=event.getPoseStack();var camera=event.getCamera().getPosition();pose.pushPose();
        pose.translate(-camera.x,-camera.y,-camera.z);
        var buffers=mc.renderBuffers().bufferSource();var lines=buffers.getBuffer(RenderType.lines());
        for(var cell:SprinklerCoverage.scan(mc.level,hit.getBlockPos())){
            var p=cell.pos();float r=cell.watered()?.2f:1f,g=cell.watered()?1f:cell.blocked()?.2f:.7f;
            double top = p.getY() + 1;
            if (cell.watered()) {
                var shape = mc.level.getBlockState(p).getShape(mc.level, p);
                if (!shape.isEmpty()) top = p.getY() + shape.max(Direction.Axis.Y);
            }
            LevelRenderer.renderLineBox(pose,lines,new AABB(p.getX()+.03,top+.015,p.getZ()+.03,p.getX()+.97,top+.05,p.getZ()+.97),r,g,.2f,.85f);
        }
        buffers.endBatch(RenderType.lines());pose.popPose();
    }
    private FarmInspectionClient(){}
}
