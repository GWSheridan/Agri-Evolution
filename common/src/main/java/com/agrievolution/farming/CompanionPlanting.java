package com.agrievolution.farming;

import com.agrievolution.content.AECropBlockEntity;
import com.agrievolution.content.AgriCropBlock;
import com.agrievolution.genetics.CropSpecies;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

public final class CompanionPlanting {
    public record Trial(String id, CropSpecies helper, CropSpecies beneficiary, int weeds, int nutrition) {
        public int readyAge() { return weeds > 0 ? 4 : 7; }
        public String bonus() {
            return weeds > 0 ? "At stage 4, " + helper.displayName() + " halves new weeds on " + beneficiary.displayName() + "."
                    : "Harvest ripe " + helper.displayName() + " to give adjacent " + beneficiary.displayName() + " +" + nutrition + " nutrition.";
        }
    }

    private static final String JOURNAL_KEY = "AgriCompanionTrials";
    public static final List<Trial> TRIALS = List.of(
            new Trial("basil_tomato", CropSpecies.BASIL, CropSpecies.TOMATO, 2, 0),
            new Trial("beans_corn", CropSpecies.BEANS, CropSpecies.CORN, 0, 8),
            new Trial("marigold_cabbage", CropSpecies.AMBER_MARIGOLD, CropSpecies.CABBAGE, 2, 0),
            new Trial("thyme_broccoli", CropSpecies.THYME, CropSpecies.BROCCOLI, 2, 0),
            new Trial("clover_strawberry", CropSpecies.CRIMSON_CLOVER, CropSpecies.STRAWBERRY, 0, 6),
            new Trial("peas_cucumber", CropSpecies.PEAS, CropSpecies.CUCUMBER, 0, 6),
            new Trial("garlic_pepper", CropSpecies.GARLIC, CropSpecies.PEPPER, 2, 0),
            new Trial("rosemary_aubergine", CropSpecies.ROSEMARY, CropSpecies.AUBERGINE, 2, 0),
            new Trial("mint_coal", CropSpecies.MINT, CropSpecies.COAL, 2, 0),
            new Trial("coriander_redstone", CropSpecies.CORIANDER, CropSpecies.REDSTONE, 2, 0),
            new Trial("lentil_iron", CropSpecies.LENTIL, CropSpecies.IRON, 0, 8),
            new Trial("chickpea_copper", CropSpecies.CHICKPEA, CropSpecies.COPPER, 0, 8)
    );

    private CompanionPlanting() {}

    public static int weeds(Level level, BlockPos pos, CropSpecies species, int denominator) {
        if (denominator <= 0) return denominator;
        for (var trial : TRIALS) {
            if (trial.beneficiary() == species && trial.weeds() > 0 && beside(level, pos, trial.helper(), trial.readyAge())) {
                return denominator * trial.weeds();
            }
        }
        return denominator;
    }

    public static void harvested(Level level, BlockPos pos, CropSpecies species) {
        if (level.isClientSide) return;
        com.agrievolution.content.SoilCareData.agriHarvest(level,pos,species);
        for (var trial : TRIALS) {
            if (trial.helper() != species || trial.nutrition() == 0) continue;
            for (var side : Direction.Plane.HORIZONTAL) {
                var neighbor = pos.relative(side);
                if (isCrop(level, neighbor, trial.beneficiary(), 0)
                        && level.getBlockEntity(neighbor) instanceof AECropBlockEntity crop) crop.addNutrition(trial.nutrition());
            }
        }
    }

    public static String describe(Level level, BlockPos pos, CropSpecies species) {
        for (var trial : TRIALS) {
            if (species == trial.beneficiary() && beside(level, pos, trial.helper(), trial.weeds() > 0 ? 4 : 0)) {
                return trial.weeds() > 0 ? trial.helper().displayName() + " halves new weeds"
                        : trial.helper().displayName() + " feeds crop at harvest";
            }
            if (species == trial.helper() && (trial.weeds() == 0 || isCrop(level, pos, species, 4))
                    && beside(level, pos, trial.beneficiary(), 0)) {
                return trial.weeds() > 0 ? "Protecting nearby " + trial.beneficiary().displayName()
                        : "Feeds " + trial.beneficiary().displayName() + " at harvest";
            }
        }
        return "";
    }

    public static int record(CompoundTag journal, Level level, BlockPos pos, CropSpecies species) {
        var records = journal.getCompound(JOURNAL_KEY);
        int discoveries = 0;
        for (var trial : TRIALS) {
            if (records.getBoolean(trial.id())) continue;
            boolean observed = species == trial.beneficiary() && beside(level, pos, trial.helper(), trial.readyAge())
                    || species == trial.helper() && isCrop(level, pos, species, trial.readyAge()) && beside(level, pos, trial.beneficiary(), 0);
            if (!observed) continue;
            records.putBoolean(trial.id(), true);
            discoveries++;
        }
        if (discoveries > 0) journal.put(JOURNAL_KEY, records);
        return discoveries;
    }

    public static java.util.Set<String> discovered(CompoundTag journal) {
        return TRIALS.stream().filter(trial -> journal.getCompound(JOURNAL_KEY).getBoolean(trial.id()))
                .map(Trial::id).collect(java.util.stream.Collectors.toUnmodifiableSet());
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
