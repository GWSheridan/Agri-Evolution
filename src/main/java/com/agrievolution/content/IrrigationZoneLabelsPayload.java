package com.agrievolution.content;

import com.agrievolution.AgriEvolution;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = AgriEvolution.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public record IrrigationZoneLabelsPayload(int menuId, List<String> labels) implements CustomPacketPayload {
    private static final Type<IrrigationZoneLabelsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AgriEvolution.MOD_ID, "irrigation_zone_labels"));
    private static final StreamCodec<RegistryFriendlyByteBuf, IrrigationZoneLabelsPayload> CODEC =
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

    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, CODEC, (payload, context) -> {
            if (context.player().containerMenu instanceof IrrigationControllerMenu menu
                    && menu.containerId == payload.menuId()) menu.updateLabels(payload.labels());
        });
    }
}
