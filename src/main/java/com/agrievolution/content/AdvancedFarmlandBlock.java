package com.agrievolution.content;

import com.agrievolution.farming.FarmEnvironment;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Permanent cultivated farmland: 3 visually distinct tiers; retains vanilla MOISTURE. */
public final class AdvancedFarmlandBlock extends FarmBlock {
    private static final MapCodec<FarmBlock> CULTIVATED_CODEC =
            simpleCodec(props -> new AdvancedFarmlandBlock(props, 1));
    private static final MapCodec<FarmBlock> NUTRIENT_CODEC =
            simpleCodec(props -> new AdvancedFarmlandBlock(props, 2));
    private static final MapCodec<FarmBlock> MYTHIC_CODEC =
            simpleCodec(props -> new AdvancedFarmlandBlock(props, 3));
    private static final MapCodec<FarmBlock> ARCANE_CODEC =
            simpleCodec(props -> new AdvancedFarmlandBlock(props, 4));
    private static final MapCodec<FarmBlock> CELESTIAL_CODEC =
            simpleCodec(props -> new AdvancedFarmlandBlock(props, 5));
    private final int tier;

    @Override
    public MapCodec<FarmBlock> codec() {
        return switch (tier) {
            case 1 -> CULTIVATED_CODEC;
            case 2 -> NUTRIENT_CODEC;
            case 3 -> MYTHIC_CODEC;
            case 4 -> ARCANE_CODEC;
            default -> CELESTIAL_CODEC;
        };
    }

    public AdvancedFarmlandBlock(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    public int tier() { return tier; }

    /** Does not turn back into dirt when dry; moisture still responds to water/basins. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.hasProperty(MOISTURE)) return;
        int previous = state.getValue(MOISTURE);
        boolean wet = FarmEnvironment.irrigated(level, pos) || hasLocalWater(level, pos);
        int next = wet ? 7 : Math.max(0, previous - (random.nextInt(tier + 1) == 0 ? 1 : 0));
        if (next != previous) level.setBlock(pos, state.setValue(MOISTURE, next), 2);
        // Bonus for vanilla CropBlock plants as well: they do not use the Agri genome logic.
        // Extra vanilla stages increase from +1 at Tier 2 to +4 at Tier 5.
        BlockPos plantPos = pos.above();
        if (level.getRawBrightness(plantPos, 0) >= 9) {
            BlockState plant = level.getBlockState(plantPos);
            if (plant.getBlock() instanceof net.minecraft.world.level.block.CropBlock crop
                    && !(plant.getBlock() instanceof AgriCropBlock)
                    && !crop.isMaxAge(plant) && (tier > 1 || random.nextBoolean())) {
                int extra = Math.max(1, tier - 1);
                level.setBlock(plantPos, crop.getStateForAge(
                        Math.min(crop.getMaxAge(), crop.getAge(plant) + extra)), 2);
            }
        }
    }

    private static boolean hasLocalWater(BlockGetter world, BlockPos pos) {
        // Same 4-block radius and 1-block vertical neighborhood as vanilla farmland.
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
            for (int dy = 0; dy <= 1; dy++) {
                BlockPos probe = pos.offset(dx, dy, dz);
                if (world.getFluidState(probe).is(FluidTags.WATER)) return true;
            }
        }
        return false;
    }
}
