package com.agrievolution.platform;

import com.agrievolution.client.FarmInspectionClient;
import com.agrievolution.client.FarmLedgerClient;
import com.agrievolution.client.journal.MilestonePage;
import com.agrievolution.content.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class NeoForgeNetwork implements PlayNetwork {
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(FarmLedgerPayload.Request.TYPE, FarmLedgerPayload.Request.CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) FarmLedgerService.request(player, payload);
                });
        registrar.playToClient(FarmLedgerPayload.Response.TYPE, FarmLedgerPayload.Response.CODEC,
                (payload, context) -> FarmLedgerClient.receive(payload));
        registrar.playToClient(FarmingMilestonesPayload.TYPE, FarmingMilestonesPayload.CODEC,
                (payload, context) -> MilestonePage.receive(payload.mask()));
        registrar.playToClient(InspectionPayload.TYPE, InspectionPayload.CODEC,
                (payload, context) -> FarmInspectionClient.receive(payload));
        registrar.playToClient(IrrigationZoneLabelsPayload.TYPE, IrrigationZoneLabelsPayload.CODEC,
                (payload, context) -> payload.apply(context.player()));
    }

    @Override
    public boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return player.connection != null && player.connection.hasChannel(type);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }
}
