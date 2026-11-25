package com.agrievolution.farming;

import com.agrievolution.content.AECropBlockEntity;
import com.agrievolution.content.AgriCropBlock;
import com.agrievolution.genetics.RareTrait;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Read-only, server-measured field report stored on the reusable Soil Sampler.
 * Keeps all world inspection server-side. CUSTOM_DATA then syncs via vanilla's
 * normal held-item synchronization; the client screen never reads remote crop BEs.
 * Nutrition is PER PLANTED CROP in 0.24.x, not persistent data on empty farmland.
 */
public final class SoilSurveyData {
    private static final String KEY = "AgriSoilSurvey";
    private SoilSurveyData() {}

    public static CompoundTag report(ItemStack sampler) {
        return sampler.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getCompound(KEY);
    }

    public static boolean hasReport(ItemStack sampler) {
        return !report(sampler).isEmpty();
    }

    public static void save(ItemStack sampler, CompoundTag survey) {
        CompoundTag data = sampler.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        data.put(KEY, survey);
        sampler.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }

    /** @param middle farmland coordinate; area=false: only this plot, area=true: 5x5. */
    public static CompoundTag survey(Level level, BlockPos middle, boolean area) {
        return survey(level, middle, area, pos -> true);
    }
    public static CompoundTag survey(Level level, BlockPos middle, boolean area,
                                      java.util.function.Predicate<BlockPos> available) {
        CompoundTag out = new CompoundTag();
        int radius = area ? 2 : 0;
        out.putInt("Version", 1);
        out.putBoolean("Area", area);
        out.putInt("X", middle.getX());
        out.putInt("Y", middle.getY());
        out.putInt("Z", middle.getZ());
        out.putString("Dimension", level.dimension().location().toString());
        out.putLong("GameTime", level.getGameTime());
        out.putInt("Total", area ? 25 : 1);

        int valid = 0, moistureSum = 0, irrigationCount = 0;
        int crops = 0, nutritionSum = 0, nutritionCount = 0, supports = 0, lowLight = 0;
        ListTag plots = new ListTag();
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                BlockPos soil = middle.offset(dx, 0, dz);
                CompoundTag plot = new CompoundTag();
                if (!level.hasChunkAt(soil) || !available.test(soil) || !(level.getBlockState(soil).getBlock() instanceof FarmBlock)) {
                    plot.putBoolean("Valid", false);
                    plots.add(plot);
                    continue;
                }
                plot.putBoolean("Valid", true);
                plot.putInt("Tier", SoilResearch.soilTier(level.getBlockState(soil)));
                if (soil.equals(middle)) out.putInt("SoilTier", plot.getInt("Tier"));
                valid++;
                int moisture = FarmEnvironment.moisture(level, soil);
                boolean irrigated = FarmEnvironment.irrigated(level, soil);
                moistureSum += moisture;
                if (irrigated) irrigationCount++;
                plot.putInt("Moisture", moisture);
                plot.putBoolean("Irrigated", irrigated);
                plot.putInt("MulchTicks",com.agrievolution.content.SoilCareData.mulchRemaining(level,soil));
                plot.putString("PreviousFamily",com.agrievolution.content.SoilCareData.previous(level,soil).label);
                if(soil.equals(middle)) {
                    out.putInt("MulchTicks",plot.getInt("MulchTicks"));out.putString("PreviousFamily",plot.getString("PreviousFamily"));
                    out.putString("CropFamily",CropFamily.of(level.getBlockState(soil.above())).label);
                    out.putInt("RotationBonus",com.agrievolution.content.SoilCareData.rotationBonus(level,soil.above()));
                }
                BlockPos cropPos = soil.above();
                BlockState state = level.getBlockState(cropPos);
                if (state.getBlock() instanceof AgriCropBlock crop) {
                    crops++;
                    plot.putBoolean("Crop", true);
                    if (state.getValue(AgriCropBlock.SUPPORTED)) supports++;
                    if (level.getRawBrightness(cropPos, 0) < 9) lowLight++;
                    if (level.getBlockEntity(cropPos) instanceof AECropBlockEntity plant) {
                        nutritionCount++;
                        nutritionSum += plant.soilNutrition();
                        plot.putInt("Nutrition", plant.soilNutrition());
                    }
                    if (soil.equals(middle)) {
                        out.putBoolean("HasCrop", true);
                        out.putString("CropName", crop.species().displayName());
                        out.putInt("Age", state.getValue(AgriCropBlock.AGE));
                        String companion = CompanionPlanting.describe(level, cropPos, crop.species());
                        if (!companion.isEmpty()) out.putString("Companion", companion);
                        out.putBoolean("Supported", state.getValue(AgriCropBlock.SUPPORTED));
                        out.putInt("Light", level.getRawBrightness(cropPos, 0));
                        out.putBoolean("Resource", crop.species().isResource());
                        out.putString("ResourceId",crop.species().isResource()?crop.species().id():"");
                        if(crop.species().isResource())out.putString("CatalystHint",ResourceCropRules.catalystHint(crop.species()));
                        out.putInt("CatalystX100", Math.round(ResourceCropRules.growthMultiplier(
                                crop.species(), level, cropPos) * 100));
                        if (level.getBlockEntity(cropPos) instanceof AECropBlockEntity plant) {
                            out.putInt("Nutrition", plant.soilNutrition());
                            out.putInt("GeneGrowth", plant.genome().growth());
                            int roll = FarmEnvironment.growthChance(
                                    plant.genome().growth(), plant.soilNutrition(), moisture,
                                    com.agrievolution.content.CropSupportItem.tier(state), irrigated);
                            if (plant.rareTrait() == RareTrait.VIGOROUS) roll += 18;
                            roll = Math.min(195, roll + SoilResearch.soilTier(level.getBlockState(soil)) * 20 + out.getInt("RotationBonus"));
                            out.putInt("GrowthRoll", roll);
                        }
                    }
                }
                plots.add(plot);
            }
        }
        out.put("Plots", plots);
        out.putInt("Valid", valid);
        out.putInt("MoistureSum", moistureSum);
        out.putInt("Irrigated", irrigationCount);
        out.putInt("Crops", crops);
        out.putInt("NutritionSum", nutritionSum);
        out.putInt("NutritionCount", nutritionCount);
        out.putInt("Supports", supports);
        out.putInt("LowLight", lowLight);
        // Missing nutrition is intentionally UNKNOWN, never synthesized as 40.
        return out;
    }


    public static int average(CompoundTag report, String total, String count) {
        int denominator = report.getInt(count);
        return denominator <= 0 ? 0 : Math.round((float) report.getInt(total) / denominator);
    }

    /** Bound length; screen cannot modify survey data. */
    public static ListTag plots(CompoundTag report) {
        return report.getList("Plots", Tag.TAG_COMPOUND);
    }
}
