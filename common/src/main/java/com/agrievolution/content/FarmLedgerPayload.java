package com.agrievolution.content;

import com.agrievolution.platform.ModInfo;
import java.util.UUID;
import net.minecraft.nbt.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class FarmLedgerPayload {
    public record Request(int menu, String action, UUID field, int revision, CompoundTag data) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ModInfo.ID, "ledger_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of(
                (b,p) -> { b.writeVarInt(p.menu); b.writeUtf(p.action, 16); b.writeUUID(p.field); b.writeInt(p.revision); b.writeNbt(p.data); },
                b -> new Request(b.readVarInt(), b.readUtf(16), b.readUUID(), b.readInt(), read(b, 262144)));
        @Override public Type<Request> type() { return TYPE; }
    }
    public record Response(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Response> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ModInfo.ID, "ledger_response"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Response> CODEC = StreamCodec.of((b,p) -> b.writeNbt(p.data), b -> new Response(read(b, 1048576)));
        @Override public Type<Response> type() { return TYPE; }
    }
    private static CompoundTag read(RegistryFriendlyByteBuf b, long limit) {
        var tag = b.readNbt(NbtAccounter.create(limit));
        if (tag != null && !(tag instanceof CompoundTag)) throw new IllegalArgumentException("Ledger payload must be a compound");
        return tag instanceof CompoundTag compound ? compound : new CompoundTag();
    }
    private FarmLedgerPayload() { }
}
