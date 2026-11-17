package com.agrievolution.content;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

public final class IrrigationComponentItem extends BlockItem {
    public IrrigationComponentItem(Block block, Properties properties) { super(block, properties); }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        if (getBlock() instanceof IrrigationSprinklerBlock) {
            lines.add(Component.literal("Waters 9 x 9 farmland, up to 8 blocks below."));
            lines.add(Component.literal("Active watering: x5 natural crop growth rate."));
            lines.add(Component.literal("Replaces greenhouse roof glass; nozzles face down."));
            lines.add(Component.literal("Solid blocks stop watering; overlapping heads do not stack."));
            lines.add(Component.literal("Uses 25 mB / 2 seconds; holds 4000 mB."));
            lines.add(Component.literal("Refill empty reservoir with a water bucket,"));
            lines.add(Component.literal("or connect Basin -> Valve -> Sprinkler."));
        } else if(getBlock() instanceof IrrigationPipeBlock) {
            lines.add(Component.literal("Connects reservoirs, basins, valves and sprinklers."));
            lines.add(Component.literal("Bends, branches and vertical runs connect automatically."));
            lines.add(Component.literal("Reservoir ports: centre of each face."));
            lines.add(Component.literal("Sneak-click to build against valves and sprinklers."));
            lines.add(Component.literal("Loaded networks only; maximum 512 connected nodes."));
        } else {
            String zone = IrrigationValveBlockEntity.storedZone(stack);
            if (!zone.isEmpty()) lines.add(Component.literal("Zone: " + zone));
            lines.add(Component.literal("Connect a basin or reservoir through pipes to a sprinkler."));
            lines.add(Component.literal("Auto-aligns to the head, including above or below."));
            lines.add(Component.literal("Right-click toggles flow; sneak-click to place against it."));
            lines.add(Component.literal("External redstone power shuts off spray and x5 growth."));
            lines.add(Component.literal("Connect pipes on any face, including side branches."));
        }
    }
}

