package com.agrievolution.content;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

final class VerticalGreenhouseItem extends BlockItem {
    VerticalGreenhouseItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        return level.setBlock(pos, state, IrrigationNetwork.loadedUpdateFlags(level, pos, Block.UPDATE_ALL_IMMEDIATE));
    }
}
