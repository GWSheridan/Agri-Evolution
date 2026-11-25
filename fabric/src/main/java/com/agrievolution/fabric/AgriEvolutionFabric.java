package com.agrievolution.fabric;

import com.agrievolution.content.*;
import com.agrievolution.platform.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class AgriEvolutionFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Game.install(new FabricGameHooks());
        Network.install(new FabricNetwork());
        ContentRegistration.register(new FabricRegistration());
        FabricNetwork.register();
        FabricWaterStorage.register();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                StarterProgressionEvents.onPlayerJoin(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> FarmLedgerService.logout(handler.player));
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) ->
                StarterProgressionEvents.onClone(oldPlayer, newPlayer));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> CropWeeds.tagsUpdated());
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, manager, success) -> {
            if (success) CropWeeds.tagsUpdated();
        });
        ServerWorldEvents.UNLOAD.register((server, level) -> FabricWaterTickets.unload(level));
        ServerTickEvents.END_WORLD_TICK.register(level -> {
            for (var player : level.players()) {
                FarmInspection.tick(player);
                FarmLedgerService.playerTick(player);
                LunchBoxItem.tick(player);
            }
            SoilCareData.get(level).finishHarvests(level);
            CropWeeds.scan(level);
            FarmLedgerService.survey(level);
            SprinklerGrowth.endTick(level);
        });
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk) ->
                chunk.getBlockEntities().values().forEach(entity -> {
                    if (entity instanceof ChunkAwareBlockEntity aware) aware.onLoad();
                }));
        ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) ->
                chunk.getBlockEntities().values().forEach(entity -> {
                    if (entity instanceof ChunkAwareBlockEntity aware) aware.onChunkUnloaded();
                }));

        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (player.isSpectator()) return InteractionResult.PASS;
            var result = SpannerItem.useFitting(player, level, hand, hit);
            if (result != InteractionResult.PASS) return result;
            result = IrrigationBasinUpgradeHandler.use(player, level, hand, hit);
            if (result != InteractionResult.PASS) return result;
            var context = new UseOnContext(player, hand, hit);
            return context.getItemInHand().getItem() instanceof GardenToolItem tool
                    ? tool.onItemUseFirst(context.getItemInHand(), context) : InteractionResult.PASS;
        });
        AttackBlockCallback.EVENT.register((player, level, hand, pos, face) ->
                !player.isSpectator() && hand == InteractionHand.MAIN_HAND && SpannerItem.inspectOnAttack(player, level, pos, true)
                        ? InteractionResult.SUCCESS : InteractionResult.PASS);
        LootTableEvents.MODIFY.register((key, table, source, registries) -> {
            var id = key.location();
            if (!id.equals(ResourceLocation.withDefaultNamespace("blocks/short_grass"))
                    && !id.equals(ResourceLocation.withDefaultNamespace("blocks/tall_grass"))) return;
            var starter = ResourceKey.create(Registries.LOOT_TABLE, ModInfo.id("starter_wild_seed"));
            table.withPool(LootPool.lootPool().add(NestedLootTable.lootTableReference(starter)));
        });
        reload("garden_orders", new GardenOrderReload());
        reload("vertical_greenhouse_harvests", new VerticalGreenhouseHarvests());
    }

    private static void reload(String name, PreparableReloadListener listener) {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new IdentifiableResourceReloadListener() {
            @Override public ResourceLocation getFabricId() { return ModInfo.id(name); }
            @Override public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager manager,
                    ProfilerFiller prepare, ProfilerFiller apply, Executor background, Executor main) {
                return listener.reload(barrier, manager, prepare, apply, background, main);
            }
        });
    }
}
