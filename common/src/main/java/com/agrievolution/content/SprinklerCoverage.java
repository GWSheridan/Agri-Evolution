package com.agrievolution.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.tags.BlockTags;

/** Exactly the same loaded-only column scan is used by watering and its preview. */
public final class SprinklerCoverage {
    static final int RADIUS = 4;
    static final int DEPTH = 8;
    public record Cell(BlockPos pos, boolean watered, boolean blocked) {}
    public static List<Cell> scan(Level level,BlockPos head){
        var cells=new ArrayList<Cell>();
        for(int x=-RADIUS;x<=RADIUS;x++)for(int z=-RADIUS;z<=RADIUS;z++){
            Cell cell = scanColumn(level, head.offset(x, 0, z));
            if (cell != null) cells.add(cell);
        }
        return List.copyOf(cells);
    }

    static boolean reaches(Level level, BlockPos head, BlockPos soil) {
        if (Math.abs(soil.getX() - head.getX()) > RADIUS || Math.abs(soil.getZ() - head.getZ()) > RADIUS) return false;
        Cell cell = scanColumn(level, new BlockPos(soil.getX(), head.getY(), soil.getZ()));
        return cell != null && cell.watered() && cell.pos().equals(soil);
    }

    private static Cell scanColumn(Level level, BlockPos top) {
        int depthLimit = Math.min(DEPTH, top.getY() - level.getMinBuildHeight());
        for (int depth = 1; depth <= depthLimit; depth++) {
            var pos = top.below(depth);
            if (!level.hasChunkAt(pos)) return null;
            var state = level.getBlockState(pos);
            if (state.getBlock() instanceof FarmBlock) return new Cell(pos, true, false);
            boolean blocked = !state.getFluidState().isEmpty() || !state.isAir()
                    && !(state.getBlock() instanceof BushBlock) && !state.is(BlockTags.CROPS)
                    && !state.getCollisionShape(level, pos).isEmpty();
            if (blocked || depth == depthLimit) return new Cell(pos, false, blocked);
        }
        return null;
    }
    private SprinklerCoverage(){}
}
