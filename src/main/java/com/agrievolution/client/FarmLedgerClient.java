package com.agrievolution.client;

import com.agrievolution.AgriEvolution;
import com.agrievolution.content.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid=AgriEvolution.MOD_ID, value=Dist.CLIENT)
public final class FarmLedgerClient {
    static CompoundTag latest = new CompoundTag();
    static FarmLedgerData.Field selected;
    static int layout;
    static boolean preview;
    static long received;
    private static Object world;
    public static void receive(FarmLedgerPayload.Response payload) {
        var mc = Minecraft.getInstance(); if (mc.player == null || !FarmLedgerService.holding(mc.player)) return;
        latest = payload.data(); selected = FarmLedgerData.readField(latest.getCompound("Selected"));
        received = System.nanoTime(); world = mc.level;
        if (selected == null) preview = false;
        if (mc.screen instanceof FarmLedgerScreen screen) screen.receive();
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.level != world || mc.player == null || !FarmLedgerService.holding(mc.player)) { preview = false; selected = null; latest = new CompoundTag(); return; }
        if (preview && selected != null && mc.player.tickCount % 20 == 0 && mc.screen == null) {
            var t = new CompoundTag(); t.putInt("Layout", layout);
            PacketDistributor.sendToServer(new FarmLedgerPayload.Request(-1, "watch", selected.id, selected.revision, t));
        }
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var mc = Minecraft.getInstance(); var f = selected;
        if (!preview || f == null || mc.player == null || mc.level == null || mc.level != world || mc.screen != null
                || mc.options.hideGui || !FarmLedgerService.holding(mc.player) || System.nanoTime()-received > 4_000_000_000L
                || !mc.level.dimension().location().toString().equals(f.dimension)) return;
        var pose = event.getPoseStack(); var camera = event.getCamera().getPosition(); pose.pushPose(); pose.translate(-camera.x,-camera.y,-camera.z);
        var buffers = mc.renderBuffers().bufferSource(); var lines = buffers.getBuffer(RenderType.lines());
        for (int i=0; i<f.size(); i++) {
            var pos = f.soil(i); if (!mc.level.hasChunkAt(pos) || mc.player.distanceToSqr(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5)>4096) continue;
            String status = FarmLedgerService.cellStatus(f, layout, i); if (status.equals("Unassigned")) continue;
            int color = FarmLedgerScreen.color(status); var shape = mc.level.getBlockState(pos).getShape(mc.level,pos);
            double top = pos.getY()+(shape.isEmpty()?1:shape.max(Direction.Axis.Y))+.02;
            LevelRenderer.renderLineBox(pose,lines,new AABB(pos.getX()+.04,top,pos.getZ()+.04,pos.getX()+.96,top+.025,pos.getZ()+.96),
                    ((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f,.9f);
        }
        buffers.endBatch(RenderType.lines()); pose.popPose();
    }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post e) {
        var mc = Minecraft.getInstance(); var f = selected;
        if (!preview || f == null || mc.screen != null || mc.level != world || mc.player == null || !FarmLedgerService.holding(mc.player) || mc.options.hideGui) return;
        String text = f.name + " • " + f.layouts[layout].name + " | green correct • gold missing • red wrong • purple soil • grey unknown";
        if(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit) {
            var pos=hit.getBlockPos();int cx=pos.getX()-f.min.getX(),cz=pos.getZ()-f.min.getZ();
            if(cx>=0&&cz>=0&&cx<f.width&&cz<f.depth&&(pos.getY()==f.min.getY()||pos.getY()==f.min.getY()+1)) {
                int i=cz*f.width+cx;text=FarmLedgerService.cellStatus(f,layout,i)+" | "+f.layouts[layout].seeds[i]+"\n"+text;
            }
        }
        var g = e.getGuiGraphics(); int w = Math.min(g.guiWidth()-16, 490); var rows = mc.font.split(net.minecraft.network.chat.Component.literal(text),w-12);
        g.fill(8,6,w+8,14+rows.size()*10,0xe6275841); int y=10; for(var row:rows) {g.drawString(mc.font,row,14,y,0xfff0dfbb,false);y+=10;}
    }
    private FarmLedgerClient() { }
}
