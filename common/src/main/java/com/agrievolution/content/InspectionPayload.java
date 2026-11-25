package com.agrievolution.content;

import com.agrievolution.platform.ModInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record InspectionPayload(String dimension,BlockPos pos,String text) implements CustomPacketPayload {
    public static final Type<InspectionPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(ModInfo.ID,"inspection"));
    public static final StreamCodec<RegistryFriendlyByteBuf,InspectionPayload> CODEC=StreamCodec.of(
        (buf,p)->{buf.writeUtf(p.dimension,256);buf.writeBlockPos(p.pos);buf.writeUtf(p.text,4096);},
        buf->new InspectionPayload(buf.readUtf(256),buf.readBlockPos(),buf.readUtf(4096)));
    @Override public Type<InspectionPayload> type(){return TYPE;}
}
