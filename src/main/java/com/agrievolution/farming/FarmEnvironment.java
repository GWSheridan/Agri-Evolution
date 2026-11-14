package com.agrievolution.farming;

import com.agrievolution.content.AEBlocks;
import com.agrievolution.content.IrrigationBasinBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Server-friendly farming rules shared by every Agri Evolution species. */
public final class FarmEnvironment {
    public static final int IRRIGATION_RANGE = 6;
    public static final int MAX_IRRIGATION_RANGE = 10;
    private FarmEnvironment() {}

    public static int clampNutrition(int nutrition) {
        return Math.max(0, Math.min(100, nutrition));
    }

    /** Matches each basin's actual upgrade radius, in X/Z and +/- one level. */
    public static boolean irrigated(LevelReader level, BlockPos soilPos) {
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        for (int dx = -MAX_IRRIGATION_RANGE; dx <= MAX_IRRIGATION_RANGE; dx++) {
            for (int dz = -MAX_IRRIGATION_RANGE; dz <= MAX_IRRIGATION_RANGE; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    probe.set(soilPos.getX() + dx, soilPos.getY() + dy, soilPos.getZ() + dz);
                    if (!level.hasChunkAt(probe)) continue;
                    BlockState state = level.getBlockState(probe);
                    if (state.is(AEBlocks.IRRIGATION_BASIN.get())
                            && state.getValue(com.agrievolution.content.IrrigationBasinBlock.FILLED)
                            && level.getBlockEntity(probe) instanceof IrrigationBasinBlockEntity basin
                            && Math.abs(dx) <= basin.wateringRadius()
                            && Math.abs(dz) <= basin.wateringRadius()) return true;
                }
            }
        }
        return false;
    }

    public static int growthChance(int genesGrowth, int nutrition, int farmlandMoisture,
                                   boolean support, boolean irrigated) {
        return growthChance(genesGrowth,nutrition,farmlandMoisture,support?1:0,irrigated);
    }
    public static int growthChance(int genesGrowth,int nutrition,int farmlandMoisture,int supportTier,boolean irrigated) {
        // Incremental bonus on top of existing vanilla + genetic growth. Never guaranteed.
        int chance = genesGrowth + (clampNutrition(nutrition) / 10)
                + (Math.max(0, Math.min(7, farmlandMoisture)) * 2)
                + (supportTier==0?0:12+8*(Math.min(3,supportTier)-1)) + (irrigated ? 15 : 0);
        return Math.min(180, chance);
    }

    public static int moisture(LevelReader level, BlockPos soilPos) {
        BlockState state = level.getBlockState(soilPos);
        return state.hasProperty(FarmBlock.MOISTURE) ? state.getValue(FarmBlock.MOISTURE) : 0;
    }
}
