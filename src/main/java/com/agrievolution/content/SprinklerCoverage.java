package com.agrievolution.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.tags.BlockTags;

/** Exactly the same loaded-only column scan is used by watering and its preview. */
public final class SprinklerCoverage {
    public record Cell(BlockPos pos, boolean watered, boolean blocked) {}
    public static List<Cell> scan(Level level,BlockPos head){
        var cells=new ArrayList<Cell>();
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){
            Cell cell = scanColumn(level, head.offset(x, 0, z));
            if (cell != null) cells.add(cell);
        }
        return List.copyOf(cells);
    }

    static boolean reaches(Level level, BlockPos head, BlockPos soil) {
        if (Math.abs(soil.getX() - head.getX()) > 4 || Math.abs(soil.getZ() - head.getZ()) > 4) return false;
        Cell cell = scanColumn(level, new BlockPos(soil.getX(), head.getY(), soil.getZ()));
        return cell != null && cell.watered() && cell.pos().equals(soil);
    }

    private static Cell scanColumn(Level level, BlockPos top) {
        for (int depth = 1; depth <= 8; depth++) {
            var pos = top.below(depth);
            if (!level.hasChunkAt(pos)) return null;
            var state = level.getBlockState(pos);
            if (state.getBlock() instanceof FarmBlock) return new Cell(pos, true, false);
            boolean blocked = !state.getFluidState().isEmpty() || !state.isAir()
                    && !(state.getBlock() instanceof BushBlock) && !state.is(BlockTags.CROPS)
                    && !state.getCollisionShape(level, pos).isEmpty();
            if (blocked || depth == 8) return new Cell(pos, false, blocked);
        }
        return null;
    }
    private SprinklerCoverage(){}
}
