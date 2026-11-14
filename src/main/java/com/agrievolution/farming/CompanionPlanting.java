package com.agrievolution.farming;

import com.agrievolution.content.AECropBlockEntity;
import com.agrievolution.content.AgriCropBlock;
import com.agrievolution.genetics.CropSpecies;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public final class CompanionPlanting {
    private static final int CORN_NUTRITION = 8;

    private CompanionPlanting() {}

    public static int weeds(Level level, BlockPos pos, CropSpecies species, int denominator) {
        return denominator > 0 && species == CropSpecies.TOMATO && beside(level, pos, CropSpecies.BASIL, 4)
                ? denominator * 2 : denominator;
    }

    public static void harvested(Level level, BlockPos pos, CropSpecies species) {
        if (level.isClientSide || species != CropSpecies.BEANS) return;
        for (var side : Direction.Plane.HORIZONTAL) {
            var neighbor = pos.relative(side);
            if (isCrop(level, neighbor, CropSpecies.CORN, 0)
                    && level.getBlockEntity(neighbor) instanceof AECropBlockEntity corn) corn.addNutrition(CORN_NUTRITION);
        }
    }

    public static String describe(Level level, BlockPos pos, CropSpecies species) {
        return switch (species) {
            case TOMATO -> beside(level, pos, CropSpecies.BASIL, 4) ? "Basil halves new weeds" : "";
            case BASIL -> isCrop(level, pos, species, 4) && beside(level, pos, CropSpecies.TOMATO, 0) ? "Protecting nearby tomatoes" : "";
            case CORN -> beside(level, pos, CropSpecies.BEANS, 0) ? "Beans feed corn at harvest" : "";
            case BEANS -> beside(level, pos, CropSpecies.CORN, 0) ? "Feeds corn at harvest" : "";
            default -> "";
        };
    }

    private static boolean beside(Level level, BlockPos pos, CropSpecies species, int minAge) {
        for (var side : Direction.Plane.HORIZONTAL) {
            if (isCrop(level, pos.relative(side), species, minAge)) return true;
        }
        return false;
    }

    private static boolean isCrop(Level level, BlockPos pos, CropSpecies species, int minAge) {
        if (!level.hasChunkAt(pos)) return false;
        var state = level.getBlockState(pos);
        return state.getBlock() instanceof AgriCropBlock crop && crop.species() == species
                && state.getValue(AgriCropBlock.AGE) >= minAge;
    }
}
