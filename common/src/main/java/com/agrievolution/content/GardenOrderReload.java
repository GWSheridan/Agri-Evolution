package com.agrievolution.content;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class GardenOrderReload extends SimpleJsonResourceReloadListener {
    static final int LIMIT = 24;
    private static final int MAX_TITLE = 48;
    private static final int MAX_INGREDIENTS = 4;
    private static volatile List<GardenWork.Job> orders = List.of();

    public GardenOrderReload() { super(new Gson(), "garden_orders"); }

    static List<GardenWork.Job> current() { return orders; }

    @Override protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resources, ProfilerFiller profiler) {
        var loaded = new ArrayList<GardenWork.Job>();
        for (var file : files.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            if (loaded.size() == LIMIT) {
                LogUtils.getLogger().warn("Loaded the first {} valid garden orders. Remaining definitions were skipped.", LIMIT);
                break;
            }
            try {
                loaded.add(parse(file.getValue().getAsJsonObject()));
            } catch (RuntimeException exception) {
                LogUtils.getLogger().warn("Skipping garden order {}: {}", file.getKey(), exception.getMessage());
            }
        }
        orders = List.copyOf(loaded);
    }

    static GardenWork.Job parse(JsonObject definition) {
        var titleValue = definition.getAsJsonPrimitive("title");
        if (titleValue == null || !titleValue.isString()) throw new IllegalArgumentException("Title must be text");
        String title = titleValue.getAsString().strip();
        if (title.isEmpty() || title.length() > MAX_TITLE || title.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Title must contain 1–48 characters on one line");
        }
        var entries = GsonHelper.getAsJsonArray(definition, "ingredients");
        if (entries.isEmpty() || entries.size() > MAX_INGREDIENTS) {
            throw new IllegalArgumentException("Orders require 1–4 distinct ingredients");
        }
        var ingredients = new ArrayList<GardenWork.Ingredient>();
        var seen = new HashSet<Item>();
        for (var entry : entries) {
            var ingredient = parseItem(entry.getAsJsonObject());
            if (!seen.add(ingredient.item().get())) throw new IllegalArgumentException("Combine counts for duplicate ingredients");
            ingredients.add(ingredient);
        }
        return new GardenWork.Job(title, List.copyOf(ingredients), parseItem(GsonHelper.getAsJsonObject(definition, "reward")));
    }

    private static GardenWork.Ingredient parseItem(JsonObject json) {
        String id = GsonHelper.getAsString(json, "item");
        if (id.length() > 128) throw new IllegalArgumentException("Item ID is too long");
        Item item = lookup(ResourceLocation.parse(id));
        var quantity = json.getAsJsonPrimitive("count");
        if (quantity == null || !quantity.isNumber()) throw new IllegalArgumentException("Count must be an integer");
        return item(item, quantity.getAsBigDecimal().intValueExact());
    }

    private static Item lookup(ResourceLocation id) {
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == null || item == Items.AIR) throw new IllegalArgumentException("Unknown item " + id);
        return item;
    }

    private static GardenWork.Ingredient item(Item item, int count) {
        if (count < 1 || count > new ItemStack(item).getMaxStackSize()) {
            throw new IllegalArgumentException("Count must fit in one stack");
        }
        return new GardenWork.Ingredient(() -> item, count);
    }

    static void write(FriendlyByteBuf buffer, List<GardenWork.Job> jobs) {
        buffer.writeVarInt(jobs.size());
        for (var job : jobs) {
            buffer.writeUtf(job.title(), MAX_TITLE);
            buffer.writeVarInt(job.ingredients().size());
            for (var ingredient : job.ingredients()) writeItem(buffer, ingredient);
            writeItem(buffer, job.reward());
        }
    }

    static List<GardenWork.Job> read(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > LIMIT) throw new IllegalArgumentException("Invalid custom order count");
        var jobs = new ArrayList<GardenWork.Job>(count);
        for (int i = 0; i < count; i++) {
            String title = buffer.readUtf(MAX_TITLE);
            int ingredientCount = buffer.readVarInt();
            if (ingredientCount < 1 || ingredientCount > MAX_INGREDIENTS) throw new IllegalArgumentException("Invalid ingredient count");
            var ingredients = new ArrayList<GardenWork.Ingredient>(ingredientCount);
            for (int j = 0; j < ingredientCount; j++) {
                ingredients.add(readItem(buffer));
            }
            jobs.add(new GardenWork.Job(title, List.copyOf(ingredients), readItem(buffer)));
        }
        return List.copyOf(jobs);
    }

    private static void writeItem(FriendlyByteBuf buffer, GardenWork.Ingredient ingredient) {
        buffer.writeResourceLocation(BuiltInRegistries.ITEM.getKey(ingredient.item().get()));
        buffer.writeVarInt(ingredient.count());
    }

    private static GardenWork.Ingredient readItem(FriendlyByteBuf buffer) {
        return item(lookup(buffer.readResourceLocation()), buffer.readVarInt());
    }
}
