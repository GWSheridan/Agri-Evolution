package com.agrievolution.farming;

import com.agrievolution.content.AEBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Per-tool research. Each soil coordinate counts once, across dimensions. No global save mutations. */
public final class SoilResearch {
    private static final String ROOT = "AgriSoilResearch";
    public static final int MAX = 96;
    private SoilResearch() {}

    private static CompoundTag stored(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
    private static ListTag discovered(CompoundTag all) {
        return all.getCompound(ROOT).getList("Plots", Tag.TAG_STRING);
    }
    public static int progress(ItemStack stack) { return Math.min(MAX, discovered(stored(stack)).size()); }
    /** 0 = basic, 1 = cultivated, 2 = nutrient, 3 = mythic, 4 = arcane, 5 = celestial. */
    public static int tier(ItemStack stack) {
        return researchedTier(stack);
    }
    public static String rankName(int tier) {
        return switch (tier) {
            case 0 -> "Soil Sampler Mk I";
            case 1 -> "Soil Sampler Mk II";
            case 2 -> "Soil Sampler Mk III";
            case 3 -> "Soil Sampler Mk IV";
            case 4 -> "Soil Sampler Mk V";
            default -> "Soil Sampler Mk VI";
        };
    }
    public static int nextUnlock(int tier) {
        return requirement(Math.min(5, tier + 1));
    }
    public static int requirement(int tier) {
        return switch (tier) { case 1 -> 8; case 2 -> 24; case 3 -> 48;
            case 4 -> 72; case 5 -> 96; default -> 0; };
    }
    private static final String RESEARCH_KEY = "AgriAnalyzedSoil";
    private static final String COLLECTED_KEY = "AgriCollectedSoil";

    private static boolean addUnique(ItemStack sampler, String key, String field) {
        CompoundTag data = stored(sampler);
        ListTag collected = data.getList(field, Tag.TAG_STRING);
        for (int i = 0; i < collected.size(); i++) {
            if (collected.getString(i).equals(key)) return false;
        }
        if (collected.size() >= MAX) return false;
        collected.add(StringTag.valueOf(key));
        data.put(field, collected);
        sampler.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return true;
    }

    /** Prevents collecting infinite specimens from one plot. */
    public static boolean collect(ItemStack sampler, Level level, BlockPos pos) {
        if (!addUnique(sampler, level.dimension().location() + ":" + pos.asLong(), COLLECTED_KEY)) return false;
        String kind = com.agrievolution.content.SoilSpecimenItem.kind(level.getBlockState(pos));
        CompoundTag stored = stored(sampler);
        CompoundTag pending = stored.getCompound("AgriPendingSpecimens");
        pending.putInt(kind, Math.min(MAX, pending.getInt(kind) + 1));
        stored.put("AgriPendingSpecimens", pending);
        sampler.set(DataComponents.CUSTOM_DATA, CustomData.of(stored));
        return true;
    }

    public static int analyzed(ItemStack sampler) {
        return Math.min(MAX, stored(sampler).getList(RESEARCH_KEY, Tag.TAG_STRING).size());
    }

    public static int researchedTier(ItemStack sampler) {
        int count = analyzed(sampler);
        return count >= 96 ? 5 : count >= 72 ? 4 : count >= 48 ? 3
                : count >= 24 ? 2 : count >= 8 ? 1 : 0;
    }

    /** Requires a specimen actually collected by the Soil Sampler; returns false for repeat samples. */
    public static boolean analyze(ItemStack sampler, ItemStack specimen) {
        String origin = com.agrievolution.content.SoilSpecimenItem.origin(specimen);
        // Previously collected v0.29.0 specimens keep their unique origin data.
        if (!origin.isEmpty()) return addUnique(sampler, origin, RESEARCH_KEY);
        String kind = com.agrievolution.content.SoilSpecimenItem.kind(specimen);
        if (kind.isEmpty() || analyzed(sampler) >= MAX) return false;
        CompoundTag data = stored(sampler);
        CompoundTag pending = data.getCompound("AgriPendingSpecimens");
        if (pending.getInt(kind) <= 0) return false;
        // Each authorized physical specimen consumes one unique collection credit.
        int count = analyzed(sampler);
        if (!addUnique(sampler, "stacked:" + count + ":" + kind, RESEARCH_KEY)) return false;
        data = stored(sampler);
        pending = data.getCompound("AgriPendingSpecimens");
        pending.putInt(kind, pending.getInt(kind) - 1);
        data.put("AgriPendingSpecimens", pending);
        sampler.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return true;
    }

    public static int soilTier(BlockState state) {
        if (state.is(AEBlocks.CELESTIAL_FARMLAND.get())) return 5;
        if (state.is(AEBlocks.ARCANE_FARMLAND.get())) return 4;
        if (state.is(AEBlocks.MYTHIC_FARMLAND.get())) return 3;
        if (state.is(AEBlocks.NUTRIENT_FARMLAND.get())) return 2;
        if (state.is(AEBlocks.CULTIVATED_FARMLAND.get())) return 1;
        return 0;
    }
    public static boolean isSoil(BlockState state) { return state.getBlock() instanceof FarmBlock; }
    public static BlockState nextState(BlockState old) {
        int newTier = soilTier(old) + 1;
        BlockState state = switch (newTier) {
            case 1 -> AEBlocks.CULTIVATED_FARMLAND.get().defaultBlockState();
            case 2 -> AEBlocks.NUTRIENT_FARMLAND.get().defaultBlockState();
            case 3 -> AEBlocks.MYTHIC_FARMLAND.get().defaultBlockState();
            case 4 -> AEBlocks.ARCANE_FARMLAND.get().defaultBlockState();
            case 5 -> AEBlocks.CELESTIAL_FARMLAND.get().defaultBlockState();
            default -> old;
        };
        if (old.hasProperty(FarmBlock.MOISTURE) && state.hasProperty(FarmBlock.MOISTURE))
            state = state.setValue(FarmBlock.MOISTURE, old.getValue(FarmBlock.MOISTURE));
        return state;
    }
    public static int gather(ItemStack sampler, Level level, BlockPos center, boolean area) {
        return gather(sampler, level, center, area, pos -> true);
    }
    public static int gather(ItemStack sampler, Level level, BlockPos center, boolean area,
                             java.util.function.Predicate<BlockPos> available) {
        if (level.isClientSide) return 0;
        CompoundTag all = stored(sampler);
        ListTag found = discovered(all);
        int gained = 0;
        int radius = area ? 2 : 0;
        for (int dz = -radius; dz <= radius; dz++) for (int dx = -radius; dx <= radius; dx++) {
            BlockPos plot = center.offset(dx, 0, dz);
            if (!level.hasChunkAt(plot) || !available.test(plot) || !isSoil(level.getBlockState(plot))) continue;
            String key = level.dimension().location() + ":" + plot.asLong();
            boolean known = false;
            for (int i = 0; i < found.size(); i++) if (found.getString(i).equals(key)) { known = true; break; }
            if (!known && found.size() < MAX) { found.add(StringTag.valueOf(key)); gained++; }
        }
        if (gained > 0) {
            CompoundTag research = all.getCompound(ROOT);
            research.put("Plots", found);
            all.put(ROOT, research);
            sampler.set(DataComponents.CUSTOM_DATA, CustomData.of(all));
        }
        return gained;
    }
}
