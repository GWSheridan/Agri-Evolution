package com.agrievolution.content;

import com.agrievolution.platform.ModInfo;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record FarmingMilestonesPayload(int mask) implements CustomPacketPayload {
    public static final Type<FarmingMilestonesPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ModInfo.ID, "farming_milestones"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FarmingMilestonesPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeVarInt(payload.mask()), buffer -> new FarmingMilestonesPayload(buffer.readVarInt()));

    @Override public Type<FarmingMilestonesPayload> type() { return TYPE; }

}
