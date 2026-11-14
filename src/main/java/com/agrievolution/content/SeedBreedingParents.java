package com.agrievolution.content;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Shared parent rules for the station inventory, preview and server tick. */
public final class SeedBreedingParents {
    public static final TagKey<Item> EXTERNAL_SEEDS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("agri_evolution", "external_breeding_seeds"));

    public enum Pair { INCOMPLETE, AGRI, NATIVE, MIXED, DIFFERENT_SEEDS, DIFFERENT_VARIANTS, INVALID }

    private SeedBreedingParents() {}

    public static boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && !(stack.getItem() instanceof SeedBagItem)
                && (SeedBagItem.accepts(stack) || stack.is(EXTERNAL_SEEDS)
                || stack.getItem() instanceof BlockItem plant && CropWeeds.isHost(plant.getBlock().defaultBlockState()));
    }

    public static Pair pair(ItemStack first, ItemStack second) {
        if (first.isEmpty() || second.isEmpty()) return Pair.INCOMPLETE;
        if (!accepts(first) || !accepts(second)) return Pair.INVALID;
        boolean firstAgri = first.getItem() instanceof SpeciesSeedItem;
        boolean secondAgri = second.getItem() instanceof SpeciesSeedItem;
        if (firstAgri && secondAgri) return Pair.AGRI;
        if (firstAgri || secondAgri) return Pair.MIXED;
        if (!first.is(second.getItem())) return Pair.DIFFERENT_SEEDS;
        return ItemStack.isSameItemSameComponents(first, second) ? Pair.NATIVE : Pair.DIFFERENT_VARIANTS;
    }
}
