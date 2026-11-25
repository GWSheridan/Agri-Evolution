package com.agrievolution.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.FarmlandWaterManager;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class IrrigationBasinGameTests {
    @GameTest(template = "empty")
    public static void movingBasinKeepsTheUpgradeAndLosesStoredWater(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(4, 120, 4));
        var block = AEBlocks.IRRIGATION_BASIN.get();
        level.setBlock(pos, block.defaultBlockState().setValue(IrrigationBasinBlock.FILLED, true), Block.UPDATE_ALL);
        var basin = (IrrigationBasinBlockEntity) level.getBlockEntity(pos);
        var mkOne = Block.getDrops(level.getBlockState(pos), level, pos, basin).getFirst();
        h.assertTrue(ItemStack.isSameItemSameComponents(mkOne, new ItemStack(AEItems.IRRIGATION_BASIN.get())),
                "Base basin drops still stack with the crafted item");
        h.assertTrue(basin.upgradeTo(1) && basin.upgradeTo(2), "Apply both basin upgrades");
        var drop = Block.getDrops(level.getBlockState(pos), level, pos, basin).getFirst();
        h.assertTrue(level.removeBlock(pos, false), "Remove the filled basin before relocation");

        var target = pos.east(2);
        level.setBlock(target.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, drop);
        var hit = new BlockHitResult(Vec3.atCenterOf(target.below()), Direction.UP, target.below(), false);
        h.assertTrue(drop.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(), "Place the dropped basin item");
        var placed = (IrrigationBasinBlockEntity) level.getBlockEntity(target);
        h.assertTrue(placed != null && placed.upgradeTier() == 2 && placed.wateringRadius() == 10
                        && level.getBlockState(target).getValue(IrrigationBasinBlock.UPGRADE) == 2
                        && !level.getBlockState(target).getValue(IrrigationBasinBlock.FILLED),
                "The new basin preserves Mk III and starts empty");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void basinUpgradeRespectsPermissionAndKeepsWater(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(4, 125, 4));
        var extraSoil = pos.west(8);
        level.setBlock(extraSoil, Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL);
        var block = AEBlocks.IRRIGATION_BASIN.get();
        level.setBlock(pos, block.defaultBlockState().setValue(IrrigationBasinBlock.FILLED, true), Block.UPDATE_ALL);
        var basin = (IrrigationBasinBlockEntity) level.getBlockEntity(pos);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var copper = new ItemStack(Items.COPPER_BLOCK, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, copper);
        player.setShiftKeyDown(true);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        player.getAbilities().mayBuild = false;
        var denied = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit);
        com.agrievolution.platform.NeoForgeEvents.basinUse(denied);
        h.assertTrue(denied.isCanceled() && basin.upgradeTier() == 0 && copper.getCount() == 2,
                "A denied upgrade leaves the tier and materials alone");

        player.getAbilities().mayBuild = true;
        com.agrievolution.platform.NeoForgeEvents.basinUse(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit));
        h.assertTrue(basin.upgradeTier() == 1 && copper.getCount() == 1 && level.getBlockState(pos).getValue(IrrigationBasinBlock.FILLED)
                        && level.getBlockState(extraSoil).getValue(FarmBlock.MOISTURE) == 7,
                "One copper block upgrades the basin and immediately waters the wider area");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void legacyBasinMigratesAndBreaksWithoutLoadingAdjacentChunk(GameTestHelper h) {
        var level = h.getLevel();
        var chunk = new ChunkPos(h.absolutePos(new BlockPos(3840, 100, 4)));
        var edge = new BlockPos(chunk.getMaxBlockX(), 100, chunk.getMiddleBlockZ());
        var pos = edge.west();
        var soil = pos.west(8);
        level.getChunkAt(pos);
        int quiet = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        level.setBlock(edge, Blocks.STONE.defaultBlockState(), quiet);
        level.setBlock(soil, Blocks.FARMLAND.defaultBlockState(), quiet);
        level.setBlock(pos, AEBlocks.IRRIGATION_BASIN.get().defaultBlockState().setValue(IrrigationBasinBlock.FILLED, true), quiet);
        var basin = (IrrigationBasinBlockEntity) level.getBlockEntity(pos);
        var legacy = new CompoundTag();
        legacy.putInt("IrrigationUpgradeTier", 1);
        basin.loadAdditional(legacy, level.registryAccess());
        basin.onLoad();
        h.assertTrue(level.getBlockState(pos).getValue(IrrigationBasinBlock.UPGRADE) == 1
                        && FarmlandWaterManager.hasBlockWaterTicket(level, soil) && !level.hasChunkAt(edge.east()),
                "The NBT-only upgrade sets the correct block state and range using loaded chunks");
        h.assertTrue(basin.upgradeTo(2) && level.getBlockState(pos).getValue(IrrigationBasinBlock.UPGRADE) == 2
                        && FarmlandWaterManager.hasBlockWaterTicket(level, soil) && !level.hasChunkAt(edge.east()),
                "Saving the next upgrade preserves hydration without checking behind the solid block");
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), quiet);
        h.assertTrue(level.getBlockEntity(pos) == null && !FarmlandWaterManager.hasBlockWaterTicket(level, soil)
                        && !level.hasChunkAt(edge.east()),
                "Breaking the migrated basin stops hydration and avoids a comparator lookup across the chunk edge");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void mkThreeReportDistinguishesFlowingWaterAndASource(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(4, 135, 4));
        var waterPos = pos.east();
        var block = AEBlocks.IRRIGATION_BASIN.get();
        level.setBlock(pos, block.defaultBlockState().setValue(IrrigationBasinBlock.UPGRADE, 2), Block.UPDATE_ALL);
        var basin = (IrrigationBasinBlockEntity) level.getBlockEntity(pos);
        level.setBlock(waterPos, Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 4), Block.UPDATE_ALL);
        h.assertTrue(basin.inspectionStatus().contains("Needs adjacent source water"), "Flowing water cannot refill a Mk III basin");
        level.setBlock(waterPos, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
        h.assertTrue(FarmInspection.describe(level, pos, true, false).stream().anyMatch(line -> line.contains("Auto-refill ready")),
                "The Spanner reports the usable source without refilling the basin");
        h.assertFalse(level.getBlockState(pos).getValue(IrrigationBasinBlock.FILLED), "Reading the status leaves the basin empty");
        h.runAfterDelay(40 - level.getGameTime() % 40, () -> {
            IrrigationBasinBlockEntity.serverTick(level, pos, level.getBlockState(pos), basin);
            h.assertTrue(level.getBlockState(pos).getValue(IrrigationBasinBlock.FILLED) && level.getFluidState(waterPos).isSource(),
                    "The normal refill tick fills the basin without consuming its source");
            h.succeed();
        });
    }
}
