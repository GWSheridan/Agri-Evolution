package com.agrievolution.forge;

import com.agrievolution.content.AEBlocks;
import com.agrievolution.content.AEItems;
import com.agrievolution.content.SoilCareData;
import com.agrievolution.platform.ModInfo;
import com.agrievolution.platform.RegistrationSet;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;

final class ForgeLoot implements IGlobalLootModifier {
    static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> CODECS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, ModInfo.ID);
    private static final MapCodec<ForgeLoot> CODEC = MapCodec.unit(new ForgeLoot());
    private static final ResourceLocation SHORT_GRASS = ResourceLocation.withDefaultNamespace("blocks/short_grass");
    private static final ResourceLocation TALL_GRASS = ResourceLocation.withDefaultNamespace("blocks/tall_grass");
    private static final ResourceKey<net.minecraft.world.level.storage.loot.LootTable> STARTER =
            ResourceKey.create(Registries.LOOT_TABLE, ModInfo.id("starter_wild_seed"));
    static { CODECS.register("crop_drops", () -> CODEC); }

    @Override
    public ObjectArrayList<ItemStack> apply(ObjectArrayList<ItemStack> loot, LootContext context) {
        var table = context.getQueriedLootTableId();
        if (SHORT_GRASS.equals(table) || TALL_GRASS.equals(table)) {
            loot.removeIf(stack -> stack.is(Items.WHEAT_SEEDS));
            context.getLevel().getServer().reloadableRegistries().getLootTable(STARTER).getRandomItemsRaw(context, loot::add);
        }
        var state = context.getParamOrNull(LootContextParams.BLOCK_STATE);
        var origin = context.getParamOrNull(LootContextParams.ORIGIN);
        if (state != null && origin != null && !loot.isEmpty()) {
            SoilCareData.nativeDrops(context.getLevel(), BlockPos.containing(origin), state,
                    context.getParamOrNull(LootContextParams.THIS_ENTITY), () -> !loot.isEmpty());
        }
        return loot;
    }

    @Override public MapCodec<? extends IGlobalLootModifier> codec() { return CODEC; }

    static void remap(MissingMappingsEvent event) {
        remap(event, AEBlocks.BLOCKS, BuiltInRegistries.BLOCK);
        remap(event, AEItems.ITEMS, BuiltInRegistries.ITEM);
    }

    private static <T> void remap(MissingMappingsEvent event, RegistrationSet<T> entries, Registry<T> registry) {
        for (var mapping : event.getMappings(entries.registryKey(), ModInfo.ID)) {
            var target = entries.aliases().get(mapping.getKey());
            if (target != null) registry.getOptional(target).ifPresent(mapping::remap);
        }
    }
}
