package com.agrievolution.forge;

import com.agrievolution.content.*;
import com.agrievolution.platform.ModInfo;
import com.agrievolution.platform.PlayNetwork;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

final class ForgeNetwork implements PlayNetwork {
    private final SimpleChannel channel = ChannelBuilder.named(ModInfo.id("play"))
            .networkProtocolVersion(1).simpleChannel();

    ForgeNetwork() {
        channel.messageBuilder(FarmLedgerPayload.Request.class, NetworkDirection.PLAY_TO_SERVER)
                .codec(FarmLedgerPayload.Request.CODEC).consumerMainThread((payload, context) -> {
                    if (context.getSender() != null) FarmLedgerService.request(context.getSender(), payload);
                }).add();
        channel.messageBuilder(FarmLedgerPayload.Response.class, NetworkDirection.PLAY_TO_CLIENT)
                .codec(FarmLedgerPayload.Response.CODEC)
                .consumerMainThread((payload, context) -> ForgeClient.receive(payload)).add();
        channel.messageBuilder(FarmingMilestonesPayload.class, NetworkDirection.PLAY_TO_CLIENT)
                .codec(FarmingMilestonesPayload.CODEC)
                .consumerMainThread((payload, context) -> ForgeClient.receive(payload)).add();
        channel.messageBuilder(InspectionPayload.class, NetworkDirection.PLAY_TO_CLIENT)
                .codec(InspectionPayload.CODEC)
                .consumerMainThread((payload, context) -> ForgeClient.receive(payload)).add();
        channel.messageBuilder(IrrigationZoneLabelsPayload.class, NetworkDirection.PLAY_TO_CLIENT)
                .codec(IrrigationZoneLabelsPayload.CODEC)
                .consumerMainThread((payload, context) -> ForgeClient.receive(payload)).add();
        channel.build();
    }

    @Override public boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return player.connection != null && channel.isRemotePresent(player.connection.getConnection());
    }

    @Override public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        channel.send(payload, PacketDistributor.PLAYER.with(player));
    }

    @Override public void sendToServer(CustomPacketPayload payload) {
        channel.send(payload, PacketDistributor.SERVER.noArg());
    }
}
