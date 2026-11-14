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
            for(int depth=1;depth<=8;depth++){
                var pos=head.offset(x,-depth,z);
                if(!level.hasChunkAt(pos))break;
                var state=level.getBlockState(pos);
                if(state.getBlock() instanceof FarmBlock){cells.add(new Cell(pos,true,false));break;}
                boolean blocked=!state.getFluidState().isEmpty() || !state.isAir()
                        && !(state.getBlock() instanceof BushBlock) && !state.is(BlockTags.CROPS)
                        && !state.getCollisionShape(level,pos).isEmpty();
                if(blocked || depth==8){cells.add(new Cell(pos,false,blocked));break;}
            }
        }
        return List.copyOf(cells);
    }
    private SprinklerCoverage(){}
}
