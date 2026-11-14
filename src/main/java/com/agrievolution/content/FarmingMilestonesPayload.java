package com.agrievolution.content;

import com.agrievolution.AgriEvolution;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = AgriEvolution.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public record FarmingMilestonesPayload(int mask) implements CustomPacketPayload {
    static final Type<FarmingMilestonesPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AgriEvolution.MOD_ID, "farming_milestones"));
    private static final StreamCodec<RegistryFriendlyByteBuf, FarmingMilestonesPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeVarInt(payload.mask()), buffer -> new FarmingMilestonesPayload(buffer.readVarInt()));

    @Override public Type<FarmingMilestonesPayload> type() { return TYPE; }

    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, CODEC, (payload, context) ->
                com.agrievolution.client.journal.MilestonePage.receive(payload.mask()));
    }
}
