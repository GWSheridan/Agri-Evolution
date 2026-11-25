package com.agrievolution.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

/**
 * New specimens stack by soil KIND instead of individual coordinates. The collector's
 * sampler retains unique collection positions and unprocessed credit, so combining stacks
 * does not permit infinite research. Old per-plot specimen items remain accepted.
 */
public final class SoilSpecimenItem extends Item {
    public SoilSpecimenItem(Properties properties) { super(properties); }

    public static String kind(BlockState state) {
        if (state.is(AEBlocks.CELESTIAL_FARMLAND.get())) return "celestial";
        if (state.is(AEBlocks.ARCANE_FARMLAND.get())) return "arcane";
        if (state.is(AEBlocks.MYTHIC_FARMLAND.get())) return "mythic";
        if (state.is(AEBlocks.NUTRIENT_FARMLAND.get())) return "nutrient";
        if (state.is(AEBlocks.CULTIVATED_FARMLAND.get())) return "cultivated";
        if (state.is(Blocks.FARMLAND)) return "farmland";
        if (state.is(Blocks.COARSE_DIRT)) return "coarse";
        if (state.is(Blocks.ROOTED_DIRT)) return "rooted";
        if (state.is(Blocks.PODZOL)) return "podzol";
        if (state.is(Blocks.GRASS_BLOCK)) return "grass";
        return "dirt";
    }
    public static int visual(String kind) {
        return switch (kind) {
            case "grass" -> 1; case "coarse" -> 2; case "rooted" -> 3;
            case "podzol" -> 4; case "farmland" -> 5; case "cultivated" -> 6;
            case "nutrient" -> 7; case "mythic" -> 8; case "arcane" -> 9;
            case "celestial" -> 10; default -> 0;
        };
    }
    public static ItemStack from(Level level, BlockPos pos) {
        ItemStack stack = new ItemStack(AEItems.SOIL_SPECIMEN.get());
        String kind = kind(level.getBlockState(pos));
        CompoundTag tag = new CompoundTag();
        tag.putString("Kind", kind);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(visual(kind)));
        return stack;
    }
    public static String kind(ItemStack specimen) {
        if (!specimen.is(AEItems.SOIL_SPECIMEN.get())) return "";
        return specimen.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getString("Kind");
    }
    /** Legacy specimens retained distinct origins so existing worlds retain those items. */
    public static String origin(ItemStack specimen) {
        if (!specimen.is(AEItems.SOIL_SPECIMEN.get())) return "";
        return specimen.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getString("Origin");
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context,
                                           List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        String kind = kind(stack);
        CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        lines.add(Component.literal("Soil type: " + (kind.isEmpty() ? data.getString("Soil") : kind)));
        lines.add(Component.literal("Stack with matching soil type (up to 64)"));
        lines.add(Component.literal("Research needs the sampler that collected this specimen"));
        if (kind.isEmpty() && origin(stack).isEmpty())
            lines.add(Component.literal("Uncollected specimen - no research value"));
    }
}
