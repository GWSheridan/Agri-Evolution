package com.agrievolution.content;

import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public final class ReservoirStructure {
    enum Status {
        COMPLETE("Structure complete; ports at face centres"),
        UNLOADED("Reservoir extends into an unloaded chunk"),
        INCOMPLETE("Reservoir is incomplete");

        final String message;
        Status(String message) { this.message = message; }
    }

    private static boolean changing;
    public static boolean tryForm(Level level,BlockPos clicked) {
        if(level.isClientSide)return false;
        for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++) {
            var core=clicked.offset(x,y,z);
            if(!validBlueprint(level,core))continue;
            changing=true;
            try {
                for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)for(int dz=-1;dz<=1;dz++)
                    level.setBlock(core.offset(dx,dy,dz),AEBlocks.WATER_RESERVOIR.get().defaultBlockState()
                            .setValue(WaterReservoirBlock.X,dx+1).setValue(WaterReservoirBlock.Y,dy+1).setValue(WaterReservoirBlock.Z,dz+1),2);
                if(level.getBlockEntity(core) instanceof WaterReservoirBlockEntity tank)tank.initialize();
            } finally {changing=false;}
            for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)for(int dz=-1;dz<=1;dz++)
                level.updateNeighborsAt(core.offset(dx,dy,dz),AEBlocks.WATER_RESERVOIR.get());
            return true;
        }
        return false;
    }
    private static boolean validBlueprint(Level level,BlockPos core) {
        for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++) {
            var p=core.offset(x,y,z);if(!level.hasChunkAt(p))return false;
            var s=level.getBlockState(p);
            if(x==0&&y==0&&z==0) {if(!s.is(Blocks.WATER)||!s.getFluidState().isSource())return false;}
            else if(!s.is(Blocks.COPPER_BLOCK))return false;
        }
        return true;
    }
    public static boolean loaded(Level level,BlockPos core) {
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)if(!level.hasChunkAt(core.offset(x,0,z)))return false;
        return true;
    }
    public static boolean complete(Level level,BlockPos core) {
        return status(level, core) == Status.COMPLETE;
    }

    static Status status(Level level, BlockPos core) {
        if (!loaded(level, core)) return Status.UNLOADED;
        for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++) {
            var p=core.offset(x,y,z);var s=level.getBlockState(p);
            if (!(s.getBlock() instanceof WaterReservoirBlock) || !WaterReservoirBlock.core(p,s).equals(core))
                return Status.INCOMPLETE;
        }
        return level.getBlockEntity(core) instanceof WaterReservoirBlockEntity tank && !tank.isRemoved()
                ? Status.COMPLETE : Status.INCOMPLETE;
    }
    public static void dismantle(Level level,BlockPos core,BlockPos removed) {
        if(changing||level.isClientSide)return;
        changing=true;
        try {
            if(level.hasChunkAt(core)&&level.getBlockEntity(core) instanceof WaterReservoirBlockEntity upgraded&&upgraded.removeUpgrade())
                net.minecraft.world.level.block.Block.popResource(level,core,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CONDUIT));
            boolean restoreWater=level.hasChunkAt(core)&&level.getBlockEntity(core) instanceof WaterReservoirBlockEntity tank&&tank.water()>=1000;
            for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++) {
                var p=core.offset(x,y,z);if(p.equals(removed)||!level.hasChunkAt(p))continue;
                var s=level.getBlockState(p);
                if(s.getBlock() instanceof WaterReservoirBlock && WaterReservoirBlock.core(p,s).equals(core))
                    level.setBlock(p,p.equals(core)?(restoreWater?Blocks.WATER:Blocks.AIR).defaultBlockState():Blocks.COPPER_BLOCK.defaultBlockState(),3);
            }
        } finally {changing=false;}
    }
    private ReservoirStructure(){}
}
