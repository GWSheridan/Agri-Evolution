package com.agrievolution.content;

import java.util.function.Predicate;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class ReservoirStructure {
    enum Status {
        COMPLETE("Structure complete; ports at face centres"),
        UNLOADED("Reservoir extends into an unloaded chunk"),
        INCOMPLETE("Reservoir is incomplete");

        final String message;
        Status(String message) { this.message = message; }
    }
    enum Formation { FORMED, PROTECTED, INVALID }

    private static boolean changing;
    public static boolean tryForm(Level level,BlockPos clicked) {
        return tryForm(level, clicked, pos -> true) == Formation.FORMED;
    }

    static Formation tryForm(Level level, BlockPos clicked, Predicate<BlockPos> canEdit) {
        if (level.isClientSide) return Formation.INVALID;
        boolean protectedShell = false;
        for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++) {
            var core=clicked.offset(x,y,z);
            if(!validBlueprint(level,core))continue;
            if (!canEditShell(core, canEdit)) {
                protectedShell = true;
                continue;
            }
            changing=true;
            try {
                for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                    BlockPos part = core.offset(dx, dy, dz);
                    BlockState shell = AEBlocks.WATER_RESERVOIR.get().defaultBlockState()
                            .setValue(WaterReservoirBlock.X, dx + 1).setValue(WaterReservoirBlock.Y, dy + 1).setValue(WaterReservoirBlock.Z, dz + 1);
                    level.setBlock(part, shell, IrrigationNetwork.loadedUpdateFlags(level, part, Block.UPDATE_CLIENTS));
                }
                if(level.getBlockEntity(core) instanceof WaterReservoirBlockEntity tank)tank.initialize();
            } finally {changing=false;}
            for (BlockPos cursor : BlockPos.betweenClosed(core.offset(-1, -1, -1), core.offset(1, 1, 1))) {
                BlockPos part = cursor.immutable();
                if (IrrigationNetwork.loadedUpdateFlags(level, part, Block.UPDATE_ALL) == Block.UPDATE_ALL) {
                    level.updateNeighborsAt(part, AEBlocks.WATER_RESERVOIR.get());
                } else {
                    syncLoadedFittings(level, part, level.getBlockState(part));
                }
            }
            return Formation.FORMED;
        }
        return protectedShell ? Formation.PROTECTED : Formation.INVALID;
    }

    private static boolean canEditShell(BlockPos core, Predicate<BlockPos> canEdit) {
        for (BlockPos pos : BlockPos.betweenClosed(core.offset(-1, -1, -1), core.offset(1, 1, 1))) {
            if (!canEdit.test(pos)) return false;
        }
        return true;
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
        if (changing || level.isClientSide) return;
        // A remaining shell part will retry once every chunk is loaded, so the core can return its water and upgrade together.
        if (!core.equals(removed) && !loaded(level, core)) return;
        changing=true;
        try {
            if(level.hasChunkAt(core)&&level.getBlockEntity(core) instanceof WaterReservoirBlockEntity upgraded&&upgraded.removeUpgrade())
                net.minecraft.world.level.block.Block.popResource(level,core,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CONDUIT));
            boolean restoreWater=level.hasChunkAt(core)&&level.getBlockEntity(core) instanceof WaterReservoirBlockEntity tank&&tank.water()>=1000;
            for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++) {
                var p=core.offset(x,y,z);if(p.equals(removed)||!level.hasChunkAt(p))continue;
                var s=level.getBlockState(p);
                if (s.getBlock() instanceof WaterReservoirBlock && WaterReservoirBlock.core(p, s).equals(core)) {
                    BlockState replacement = (p.equals(core) ? (restoreWater ? Blocks.WATER : Blocks.AIR) : Blocks.COPPER_BLOCK).defaultBlockState();
                    int flags = IrrigationNetwork.loadedUpdateFlags(level, p, Block.UPDATE_ALL);
                    if (level.setBlock(p, replacement, flags) && flags != Block.UPDATE_ALL) syncLoadedFittings(level, p, replacement);
                }
            }
        } finally {changing=false;}
    }

    private static void syncLoadedFittings(Level level, BlockPos pos, BlockState part) {
        for (Direction side : Direction.values()) {
            BlockPos neighbor = pos.relative(side);
            if (!level.hasChunkAt(neighbor)) continue;
            BlockState current = level.getBlockState(neighbor);
            if (!(current.getBlock() instanceof IrrigationPipeBlock || current.getBlock() instanceof IrrigationValveBlock
                    || current.getBlock() instanceof IrrigationSprinklerBlock)) continue;
            BlockState updated = current.updateShape(side.getOpposite(), part, level, neighbor, pos);
            if (current != updated) {
                level.setBlock(neighbor, updated, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                level.neighborChanged(updated, neighbor, AEBlocks.WATER_RESERVOIR.get(), pos, false);
            }
        }
    }
    private ReservoirStructure(){}
}
