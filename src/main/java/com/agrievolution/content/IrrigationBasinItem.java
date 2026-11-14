package com.agrievolution.content;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import java.util.List;

public final class IrrigationBasinItem extends BlockItem {
    public IrrigationBasinItem(Block block, Properties properties) { super(block, properties); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.literal("Fill with a water bucket. Permanently irrigates 6 blocks."));
        lines.add(Component.literal("Use an empty bucket to retrieve its water."));
        lines.add(Component.literal("Prevents farmland drying while filled."));
        lines.add(Component.literal("Sneak-use Copper Block: Mk II / radius 8."));
        lines.add(Component.literal("Sneak-use Amethyst Block: Mk III / radius 10."));
        lines.add(Component.literal("Mk III auto-refills beside still source water."));
    }
}
