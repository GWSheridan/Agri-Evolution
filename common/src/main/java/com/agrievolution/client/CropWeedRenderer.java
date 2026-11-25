package com.agrievolution.client;

import com.agrievolution.content.CropWeedEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class CropWeedRenderer extends EntityRenderer<CropWeedEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("agri_evolution", "textures/block/weed_stage_3.png");
    public CropWeedRenderer(EntityRendererProvider.Context context) { super(context); shadowRadius = 0; }
    @Override public ResourceLocation getTextureLocation(CropWeedEntity entity) { return TEXTURE; }
    @Override public void render(CropWeedEntity entity, float yaw, float partialTick, PoseStack poses, MultiBufferSource buffers, int light) {
        VertexConsumer out = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        var pose = poses.last();
        quad(out, pose, -.46f, -.46f, .46f, .46f, light);
        quad(out, pose, -.46f, .46f, .46f, -.46f, light);
        super.render(entity, yaw, partialTick, poses, buffers, light);
    }
    private static void quad(VertexConsumer out, PoseStack.Pose pose, float x1, float z1, float x2, float z2, int light) {
        vertex(out, pose, x1, 0, z1, 0, 1, light);
        vertex(out, pose, x2, 0, z2, 1, 1, light);
        vertex(out, pose, x2, .75f, z2, 1, 0, light);
        vertex(out, pose, x1, .75f, z1, 0, 0, light);
    }
    private static void vertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, float z, float u, float v, int light) {
        out.addVertex(pose, x, y, z).setColor(255,255,255,255).setUv(u,v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0,1,0);
    }
}
