package com.agrievolution.fabric;

import com.agrievolution.content.*;
import com.agrievolution.platform.PlayNetwork;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

final class FabricNetwork implements PlayNetwork {
    static void register() {
        PayloadTypeRegistry.playC2S().register(FarmLedgerPayload.Request.TYPE, FarmLedgerPayload.Request.CODEC);
        PayloadTypeRegistry.playS2C().register(FarmLedgerPayload.Response.TYPE, FarmLedgerPayload.Response.CODEC);
        PayloadTypeRegistry.playS2C().register(FarmingMilestonesPayload.TYPE, FarmingMilestonesPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(InspectionPayload.TYPE, InspectionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(IrrigationZoneLabelsPayload.TYPE, IrrigationZoneLabelsPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(FarmLedgerPayload.Request.TYPE,
                (payload, context) -> FarmLedgerService.request(context.player(), payload));
    }

    @Override public boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return player.connection != null && ServerPlayNetworking.canSend(player, type.id());
    }
    @Override public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }
    @Override public void sendToServer(CustomPacketPayload payload) {
        if (ClientPlayNetworking.canSend(payload.type().id())) ClientPlayNetworking.send(payload);
    }
}
