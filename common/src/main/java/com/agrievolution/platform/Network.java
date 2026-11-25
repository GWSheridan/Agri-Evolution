package com.agrievolution.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public final class Network {
    private static PlayNetwork transport;

    private Network() {}

    public static void install(PlayNetwork implementation) {
        if (transport != null) throw new IllegalStateException("Network already initialized");
        transport = implementation;
    }

    public static boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return transport.canSend(player, type);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (canSend(player, payload.type())) transport.sendToPlayer(player, payload);
    }

    public static void sendToServer(CustomPacketPayload payload) {
        transport.sendToServer(payload);
    }
}
