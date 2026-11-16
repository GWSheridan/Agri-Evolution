package com.agrievolution.content;

import com.agrievolution.AgriEvolution;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Four extra natural growth opportunities, plus the normal one: x5 in expectation. */
@EventBusSubscriber(modid = AgriEvolution.MOD_ID)
public final class SprinklerGrowth {
    // Positions only: values must not retain a level through block-entity references.
    private static final Map<ServerLevel, Set<BlockPos>> ACTIVE = new WeakHashMap<>();
    static void activate(ServerLevel level, BlockPos pos) {
        ACTIVE.computeIfAbsent(level, ignored -> new HashSet<>()).add(pos.immutable());
    }
    static void deactivate(ServerLevel level, BlockPos pos) {
        var positions = ACTIVE.get(level);
        if (positions != null) {
            positions.remove(pos);
            if (positions.isEmpty()) ACTIVE.remove(level);
        }
    }
    /** Union of loaded coverage: overlapping sprinklers never multiply the bonus. */
    public static boolean covers(ServerLevel level,BlockPos crop){
        var positions=ACTIVE.get(level);if(positions==null)return false;
        // Inspect only candidate heads around this crop, not every sprinkler in the world.
        int radius = SprinklerCoverage.RADIUS;
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) for (int up = 0; up < SprinklerCoverage.DEPTH; up++) {
            var pos=crop.offset(x,up,z);
            if(positions.contains(pos)&&level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof IrrigationSprinklerBlockEntity head
                    && head.watersPlot(crop.below()) && head.growthActive()) return true;
        }
        return false;
    }
    public static Set<BlockPos> coveredPlots(ServerLevel level) {
        var result = new HashSet<BlockPos>();
        var positions = ACTIVE.get(level);
        if (positions == null) return result;
        var iterator = positions.iterator();
        while (iterator.hasNext()) {
            var pos = iterator.next();
            if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof IrrigationSprinklerBlockEntity sprinkler)
                    || !sprinkler.hasCurrentSpray()) {
                iterator.remove();
            } else if (sprinkler.growthActive()) {
                for (BlockPos soil : sprinkler.growthSoils()) result.add(soil.above());
            }
        }
        if (positions.isEmpty()) ACTIVE.remove(level);
        return result;
    }
    static int extraTicks(int randomTickSpeed, RandomSource random) {
        // Each block receives randomTickSpeed / 4096 natural selections per game tick.
        double expected = 4.0 * 40 * Math.max(0, randomTickSpeed) / 4096;
        int whole = (int) expected;
        return whole + (random.nextDouble() < expected - whole ? 1 : 0);
    }
    private static boolean crop(BlockState state) {
        return state.getBlock() instanceof CropBlock || state.getBlock() instanceof StemBlock || state.is(BlockTags.CROPS);
    }
    static void boost(ServerLevel level) {
        int speed = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
        if (speed <= 0) return;
        for (BlockPos pos : coveredPlots(level)) {
            if (!(level.getBlockState(pos.below()).getBlock() instanceof FarmBlock)) continue;
            BlockState initial = level.getBlockState(pos);
            if (!crop(initial) || !initial.isRandomlyTicking()) continue;
            int attempts = extraTicks(speed, level.random);
            for (int i=0; i<attempts; i++) {
                BlockState current = level.getBlockState(pos);
                if (current.getBlock() != initial.getBlock() || !current.isRandomlyTicking()) break;
                // Invoke the crop's own logic: light, soil, genetics, weeds and mod hooks still apply.
                current.randomTick(level, pos, level.random);
            }
        }
    }
    @SubscribeEvent public static void endTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % 40 == 0) boost(level);
    }
    private SprinklerGrowth() {}
}
