package com.agrievolution.content;

import com.agrievolution.platform.ModInfo;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record IrrigationZoneLabelsPayload(int menuId, List<String> labels) implements CustomPacketPayload {
    public static final Type<IrrigationZoneLabelsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ModInfo.ID, "irrigation_zone_labels"));
    public static final StreamCodec<RegistryFriendlyByteBuf, IrrigationZoneLabelsPayload> CODEC =
            StreamCodec.of((buf, payload) -> {
                buf.writeVarInt(payload.menuId());
                for (String label : payload.labels()) buf.writeUtf(label, 64);
            }, buf -> {
                int id = buf.readVarInt();
                var labels = new ArrayList<String>(IrrigationControllerBlockEntity.ZONES);
                for (int row = 0; row < IrrigationControllerBlockEntity.ZONES; row++) labels.add(buf.readUtf(64));
                return new IrrigationZoneLabelsPayload(id, labels);
            });

    @Override public Type<IrrigationZoneLabelsPayload> type() { return TYPE; }

    public void apply(net.minecraft.world.entity.player.Player player) {
        if (player.containerMenu instanceof IrrigationControllerMenu menu && menu.containerId == menuId) {
            menu.updateLabels(labels);
        }
    }
}
