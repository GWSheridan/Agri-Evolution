package com.agrievolution.content;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;
import java.util.List;

public final class IrrigationBasinItem extends IrrigationFittingItem {
    public IrrigationBasinItem(Block block, Properties properties) { super(block, properties); }

    static int storedTier(ItemStack stack) {
        Integer tier = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).get(IrrigationBasinBlock.UPGRADE);
        return tier == null ? 0 : tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        int tier = storedTier(stack);
        lines.add(Component.literal("Mk " + (tier + 1) + " | Watering radius " + IrrigationBasinBlockEntity.wateringRadius(tier) + " blocks"));
        lines.add(Component.literal("Fill with a water bucket to hydrate farmland."));
        lines.add(Component.literal("Use an empty bucket to retrieve its water."));
        lines.add(Component.literal("Prevents farmland drying while filled."));
        if (tier == 0) lines.add(Component.literal("Sneak-use Copper Block: Mk II / radius 8."));
        if (tier < 2) lines.add(Component.literal("Sneak-use Amethyst Block on Mk II: Mk III / radius 10."));
        lines.add(Component.literal("Mk III auto-refills beside still source water."));
    }
}
