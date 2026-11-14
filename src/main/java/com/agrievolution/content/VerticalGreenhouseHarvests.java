package com.agrievolution.content;

import com.agrievolution.AgriEvolution;
import com.agrievolution.genetics.GenomeStorage;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/** Server-side harvest rules for growing native crops in a tray. */
@EventBusSubscriber(modid = AgriEvolution.MOD_ID)
public final class VerticalGreenhouseHarvests extends SimpleJsonResourceReloadListener {
    private static volatile Map<Item, Harvest> nativeHarvests = Map.of();

    public record Harvest(Item product, int count, int seconds) {
        ItemStack output() { return new ItemStack(product, count); }
    }

    private VerticalGreenhouseHarvests() { super(new Gson(), "vertical_greenhouse_harvests"); }

    @SubscribeEvent public static void register(AddReloadListenerEvent event) {
        event.addListener(new VerticalGreenhouseHarvests());
    }

    public static Harvest forSeed(ItemStack seed) {
        if (seed.isEmpty()) return null;
        if (seed.getItem() instanceof SpeciesSeedItem agri) {
            var genes = GenomeStorage.fromSeed(seed);
            boolean resource = agri.species().isResource();
            return new Harvest(BotanicalGreenhouseBlockEntity.harvestProduce(agri.species()),
                    resource ? 1 : 1 + genes.yield() / 50, (resource ? 150 : 90) - genes.growth() / 2);
        }
        return nativeHarvests.get(seed.getItem());
    }

    @Override protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager resources, ProfilerFiller profiler) {
        var parsed = new HashMap<Item, Harvest>();
        for (var entry : entries.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            try {
                var json = entry.getValue().getAsJsonObject();
                Item seed = item(GsonHelper.getAsString(json, "seed"));
                Item product = item(GsonHelper.getAsString(json, "product"));
                int count = GsonHelper.getAsInt(json, "count", 1);
                int seconds = GsonHelper.getAsInt(json, "seconds", 90);
                if (count < 1 || count > new ItemStack(product).getMaxStackSize() || seconds < 1 || seconds > 3600) {
                    throw new IllegalArgumentException("count or seconds out of range");
                }
                parsed.put(seed, new Harvest(product, count, seconds));
            } catch (RuntimeException exception) {
                LogUtils.getLogger().warn("Skipping vertical greenhouse harvest {}: {}", entry.getKey(), exception.getMessage());
            }
        }
        nativeHarvests = Map.copyOf(parsed);
    }

    private static Item item(String id) {
        var key = ResourceLocation.tryParse(id);
        if (key == null || !BuiltInRegistries.ITEM.containsKey(key) || BuiltInRegistries.ITEM.get(key) == Items.AIR) {
            throw new IllegalArgumentException("Unknown item " + id);
        }
        return BuiltInRegistries.ITEM.get(key);
    }
}
