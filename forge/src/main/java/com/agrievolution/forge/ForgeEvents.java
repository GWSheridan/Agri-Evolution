package com.agrievolution.forge;

import com.agrievolution.content.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent.CropGrowEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;

public final class ForgeEvents {
    private ForgeEvents() {}

    public static void register() {
        var bus = MinecraftForge.EVENT_BUS;
        bus.addListener(ForgeEvents::spannerUse);
        bus.addListener(ForgeEvents::spannerAttack);
        bus.addListener(ForgeEvents::basinUse);
        bus.addListener(ForgeLoot::remap);
        bus.addGenericListener(net.minecraft.world.level.block.entity.BlockEntity.class, ForgeFluidHandler::attach);
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
        bus.addListener((LivingEvent.LivingTickEvent event) -> CropPollination.beeVisit(event.getEntity()));
        bus.addListener((PlayerTickEvent.Post event) -> {
            if (event.player instanceof ServerPlayer player) {
                FarmInspection.tick(player);
                FarmLedgerService.playerTick(player);
                LunchBoxItem.tick(player);
            }
        });
        bus.addListener((LevelTickEvent.Post event) -> {
            if (event.level instanceof ServerLevel level) {
                SoilCareData.get(level).finishHarvests(level);
                CropWeeds.scan(level);
                FarmLedgerService.survey(level);
                SprinklerGrowth.endTick(level);
            }
        });
        bus.addListener((CropGrowEvent.Pre event) -> {
            if (event.getLevel() instanceof ServerLevel level && event.getResult() == Event.Result.DEFAULT
                    && CropWeeds.slowGrowth(level, event.getPos(), event.getState())) {
                event.setResult(Event.Result.DENY);
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
        event.setUseBlock(Event.Result.DENY);
        event.setUseItem(Event.Result.DENY);
    }
}
