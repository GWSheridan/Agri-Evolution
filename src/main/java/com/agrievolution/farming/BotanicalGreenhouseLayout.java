package com.agrievolution.farming;

import com.agrievolution.content.AEBlocks;
import com.agrievolution.content.BotanicalGreenhouseBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Orientation-independent greenhouse layout. Controller sits in the centre of the
 * front glass wall, one block above the soil. Its FACING points towards outside.
 * 7 wide x 7 deep, two wall layers and a solid glass roof above them; 5x5 interior.
 */
public final class BotanicalGreenhouseLayout {
    private BotanicalGreenhouseLayout() {}

    /** The renderer uses the same dimensions as isComplete: 7x7 glass roof,
     * perimeter glass at heights 0 and 1, and the possible 5x5 farm floor.
     * FLOOR slots are suggestions: only nine of those 25 positions are required.
     */
    public enum PreviewPart { WALL_GLASS, ROOF_GLASS, ENTRANCE_OR_GLASS, OPTIONAL_FARMLAND }
    public record PreviewCell(BlockPos pos, PreviewPart part) {}

    public static java.util.List<PreviewCell> previewCells(BlockPos controller, Direction facing) {
        java.util.List<PreviewCell> cells = new java.util.ArrayList<>(121);
        for (int across = -3; across <= 3; across++) {
            for (int depth = 0; depth <= 6; depth++) {
                cells.add(new PreviewCell(at(controller, facing, across, depth, 2), PreviewPart.ROOF_GLASS));
                if (across == -3 || across == 3 || depth == 0 || depth == 6) {
                    for (int height = 0; height <= 1; height++) {
                        if (across == 0 && depth == 0 && height == 0) continue; // real controller
                        cells.add(new PreviewCell(at(controller, facing, across, depth, height),
                                (across == 1 && depth == 0) ? PreviewPart.ENTRANCE_OR_GLASS : PreviewPart.WALL_GLASS));
                    }
                }
            }
        }
        for (int across = -2; across <= 2; across++) {
            for (int depth = 1; depth <= 5; depth++) {
                cells.add(new PreviewCell(at(controller, facing, across, depth, -1), PreviewPart.OPTIONAL_FARMLAND));
            }
        }
        return cells;
    }

    public static boolean previewMatches(BlockState state, PreviewPart part) {
        return switch (part) {
            case WALL_GLASS -> glassWall(state);
            case ENTRANCE_OR_GLASS -> glassWall(state) || isDoor(state);
            case ROOF_GLASS -> glassRoof(state);
            case OPTIONAL_FARMLAND -> state.getBlock() instanceof net.minecraft.world.level.block.FarmBlock;
        };
    }

    public static BlockPos at(BlockPos controller, Direction facing, int across, int depth, int height) {
        return controller.relative(facing.getClockWise(), across)
                .relative(facing.getOpposite(), depth).above(height);
    }

    public static boolean inside(BlockPos controller, Direction facing, BlockPos target) {
        Direction inward = facing.getOpposite();
        Direction right = facing.getClockWise();
        int dx = target.getX() - controller.getX(), dz = target.getZ() - controller.getZ();
        int depth = dx * inward.getStepX() + dz * inward.getStepZ();
        int across = dx * right.getStepX() + dz * right.getStepZ();
        return depth >= 1 && depth <= 5 && across >= -2 && across <= 2
                && target.getY() == controller.getY();
    }

    private static boolean glassWall(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.GLASS || block == Blocks.TINTED_GLASS
                || block instanceof StainedGlassBlock
                || block == Blocks.GLASS_PANE
                || block instanceof StainedGlassPaneBlock;
    }

    private static boolean glassRoof(BlockState state) {
        Block block = state.getBlock();
        return block == AEBlocks.IRRIGATION_SPRINKLER.get() || block == Blocks.GLASS || block == Blocks.TINTED_GLASS
                || block instanceof StainedGlassBlock;
    }

    /** Every vanilla door and every modded door tagged minecraft:doors can be used.
     *  The entry must be a real two-high matching pair; one isolated door half
     *  does not seal the structure. */
    public static boolean isDoor(BlockState state) {
        return state.getBlock() instanceof DoorBlock || state.is(BlockTags.DOORS);
    }

    /** Front-wall positions beside the controller. Avoid corners and the controller. */
    private static boolean eligibleDoorColumn(int across, int depth) {
        return depth == 0 && across >= -2 && across <= 2 && across != 0;
    }

    private static boolean validDoorPair(LevelReader level, BlockPos lowerPos) {
        BlockPos upperPos = lowerPos.above();
        if (!level.hasChunkAt(lowerPos) || !level.hasChunkAt(upperPos)) return false;
        BlockState lower = level.getBlockState(lowerPos);
        BlockState upper = level.getBlockState(upperPos);
        if (!isDoor(lower) || !isDoor(upper) || lower.getBlock() != upper.getBlock()) return false;
        // Vanilla and most modded doors expose the standard HALF property.
        if (lower.hasProperty(DoorBlock.HALF) && upper.hasProperty(DoorBlock.HALF)) {
            return lower.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
                    && upper.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER;
        }
        // Support modded tagged two-high door implementations without the vanilla HALF property.
        return true;
    }

    /** A door may be placed at any of four front-wall columns (both halves required). */
    public static boolean hasValidDoor(LevelReader level, BlockPos controller, Direction facing) {
        for (int across = -2; across <= 2; across++) {
            if (eligibleDoorColumn(across, 0)
                    && validDoorPair(level, at(controller, facing, across, 0, 0))) return true;
        }
        return false;
    }

    /** Used for the safe controller-held door retrofit: never overwrite unrelated blocks. */
    public static boolean canReplaceWithDoor(BlockState state) {
        return state.isAir() || glassWall(state);
    }

    /** Inspect ONLY loaded positions; an incomplete/unloaded wall never activates the machine. */
    public static boolean isComplete(LevelReader level, BlockPos controller, Direction facing) {
        int wallCount = 0;
        for (int across = -3; across <= 3; across++) {
            for (int depth = 0; depth <= 6; depth++) {
                // Roof glass can be replaced with a downward-facing sprinkler fitting.
                BlockPos roof = at(controller, facing, across, depth, 2);
                if (!level.hasChunkAt(roof) || !glassRoof(level.getBlockState(roof))) return false;
                if (across == -3 || across == 3 || depth == 0 || depth == 6) {
                    for (int height = 0; height <= 1; height++) {
                        if (across == 0 && depth == 0 && height == 0) continue; // controller
                        BlockPos wall = at(controller, facing, across, depth, height);
                        if (!level.hasChunkAt(wall)) return false;
                        BlockState wallState = level.getBlockState(wall);
                        if (isDoor(wallState)) {
                            if (!eligibleDoorColumn(across, depth)
                                    || !validDoorPair(level, at(controller, facing, across, 0, 0)))
                                return false;
                        } else if (!glassWall(wallState)) return false;
                        wallCount++;
                    }
                }
            }
        }
        if (wallCount < 47 || !hasValidDoor(level, controller, facing)) return false;

        // Require farmable interior; the breeding station may occupy some floor spots.
        int farmland = 0;
        for (int across = -2; across <= 2; across++) for (int depth = 1; depth <= 5; depth++) {
            BlockPos soil = at(controller, facing, across, depth, -1);
            if (!level.hasChunkAt(soil)) return false;
            if (level.getBlockState(soil).getBlock() instanceof net.minecraft.world.level.block.FarmBlock)
                farmland++;
        }
        return farmland >= 9;
    }

    /** Check at most 25 possible controller positions, never load chunks. */
    public static boolean hasActiveGreenhouse(LevelReader level, BlockPos target) {
        for (Direction outward : Direction.Plane.HORIZONTAL) {
            for (int depth = 1; depth <= 5; depth++) for (int across = -2; across <= 2; across++) {
                BlockPos candidate = target.relative(outward, depth)
                        .relative(outward.getClockWise(), -across);
                if (!level.hasChunkAt(candidate)) continue;
                BlockState state = level.getBlockState(candidate);
                if (state.is(AEBlocks.BOTANICAL_GREENHOUSE.get())
                        && state.getValue(BotanicalGreenhouseBlock.ACTIVE)
                        && state.getValue(BotanicalGreenhouseBlock.FACING) == outward
                        && inside(candidate, outward, target)) return true;
            }
        }
        return false;
    }
}
