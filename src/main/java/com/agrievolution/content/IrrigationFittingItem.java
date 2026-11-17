package com.agrievolution.content;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

class IrrigationFittingItem extends BlockItem {
    IrrigationFittingItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        int flags = IrrigationNetwork.loadedUpdateFlags(level, pos, Block.UPDATE_ALL_IMMEDIATE);
        if (!level.setBlock(pos, state, flags)) return false;
        if (!level.isClientSide && flags != Block.UPDATE_ALL_IMMEDIATE) {
            IrrigationNetwork.syncLoadedFittings(level, pos, level.getBlockState(pos), getBlock());
        }
        return true;
    }
}
