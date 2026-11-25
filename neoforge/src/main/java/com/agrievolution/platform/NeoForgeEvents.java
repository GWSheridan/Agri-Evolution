package com.agrievolution.platform;

import com.agrievolution.content.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class NeoForgeEvents {
    private NeoForgeEvents() {}

    public static void register() {
        var bus = NeoForge.EVENT_BUS;
        bus.addListener(NeoForgeEvents::spannerUse);
        bus.addListener(NeoForgeEvents::spannerAttack);
        bus.addListener(NeoForgeEvents::basinUse);
        bus.addListener((BlockDropsEvent event) ->
                StarterProgressionEvents.onGrassDrops(event.getState(), event.getDrops()));
        bus.addListener(EventPriority.LOWEST, (BlockDropsEvent event) ->
                SoilCareData.nativeDrops(event.getLevel(), event.getPos(), event.getState(), event.getBreaker(),
                        () -> !event.isCanceled() && !event.getDrops().isEmpty()));
        bus.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) StarterProgressionEvents.onPlayerJoin(player);
        });
        bus.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) FarmLedgerService.logout(player);
        });
        bus.addListener((PlayerEvent.Clone event) -> StarterProgressionEvents.onClone(event.getOriginal(), event.getEntity()));
        bus.addListener((TagsUpdatedEvent event) -> CropWeeds.tagsUpdated());
        bus.addListener((AddReloadListenerEvent event) -> {
            event.addListener(new GardenOrderReload());
            event.addListener(new VerticalGreenhouseHarvests());
        });
        bus.addListener((EntityTickEvent.Post event) -> CropPollination.beeVisit(event.getEntity()));
        bus.addListener((PlayerTickEvent.Post event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                FarmInspection.tick(player);
                FarmLedgerService.playerTick(player);
                LunchBoxItem.tick(player);
            }
        });
        bus.addListener((LevelTickEvent.Post event) -> {
            if (event.getLevel() instanceof ServerLevel level) {
                SoilCareData.get(level).finishHarvests(level);
                CropWeeds.scan(level);
                FarmLedgerService.survey(level);
                SprinklerGrowth.endTick(level);
            }
        });
        bus.addListener((CropGrowEvent.Pre event) -> {
            if (event.getLevel() instanceof ServerLevel level && event.getResult() == CropGrowEvent.Pre.Result.DEFAULT
                    && CropWeeds.slowGrowth(level, event.getPos(), event.getState())) {
                event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
            }
        });
        bus.addListener((CropGrowEvent.Post event) -> {
            if (event.getLevel() instanceof ServerLevel level)
                SoilCareData.nativeGrowth(level, event.getPos(), event.getOriginalState(), event.getState());
        });
    }

    public static void spannerUse(PlayerInteractEvent.RightClickBlock event) {
        handle(event, SpannerItem.useFitting(event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec()));
    }

    public static void basinUse(PlayerInteractEvent.RightClickBlock event) {
        handle(event, IrrigationBasinUpgradeHandler.use(event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec()));
    }

    private static void handle(PlayerInteractEvent.RightClickBlock event, InteractionResult result) {
        if (result == InteractionResult.PASS) return;
        event.setCancellationResult(result);
        event.setCanceled(true);
    }

    public static void spannerAttack(PlayerInteractEvent.LeftClickBlock event) {
        if (!SpannerItem.inspectOnAttack(event.getEntity(), event.getLevel(), event.getPos(),
                event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START)) return;
        event.setCanceled(true);
        event.setUseBlock(TriState.FALSE);
        event.setUseItem(TriState.FALSE);
    }
}
