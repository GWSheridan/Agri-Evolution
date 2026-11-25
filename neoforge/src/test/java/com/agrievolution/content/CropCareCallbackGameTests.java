package com.agrievolution.content;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.RegisterEvent;

@GameTestHolder("agri_evolution") @PrefixGameTestTemplate(false)
public final class CropCareCallbackGameTests {
    private static final ResourceLocation OBSERVER = ResourceLocation.fromNamespaceAndPath("agri_evolution", "test_crop_care_observer");
    private static BlockPos watched;
    private static Predicate<BlockState> onShape;
    private static Runnable shapeAction, comparatorAction;

    @EventBusSubscriber(modid="agri_evolution", bus=EventBusSubscriber.Bus.MOD)
    public static final class Registry {
        @SubscribeEvent public static void register(RegisterEvent event) {
            event.register(Registries.BLOCK, OBSERVER, () -> new Block(BlockBehaviour.Properties.of()) {
                @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                        LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
                    if (neighborPos.equals(watched) && shapeAction != null && onShape.test(neighbor)) {
                        var action = shapeAction; shapeAction = null; action.run();
                    }
                    return super.updateShape(state, side, neighbor, level, pos, neighborPos);
                }
                @Override public void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighbor) {
                    if (neighbor.equals(watched) && comparatorAction != null) {
                        var action = comparatorAction; comparatorAction = null; action.run();
                    }
                }
            });
        }
    }
    private static void observe(GameTestHelper h, BlockPos target) {
        watched = target;
        h.getLevel().setBlock(target.east(), BuiltInRegistries.BLOCK.get(OBSERVER).defaultBlockState(), 2);
    }
    private static BlockPos crop(GameTestHelper h, int tier) {
        var pos = h.absolutePos(new BlockPos(4, 210, 4));
        h.getLevel().setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState(), 2);
        h.getLevel().setBlock(pos, AEBlocks.TOMATO_CROP.get().defaultBlockState()
                .setValue(AgriCropBlock.SUPPORTED, tier > 0).setValue(AgriCropBlock.SUPPORT_TIER, tier), 2);
        return pos;
    }
    private static void expiry(GameTestHelper h, int tier) {
        var pos = crop(h, 1); var level = h.getLevel(); var plant = (AECropBlockEntity) level.getBlockEntity(pos);
        plant.setSupportTicks(1); observe(h, pos);
        onShape = state -> CropSupportItem.tier(state) == 0;
        shapeAction = () -> {
            level.setBlock(pos, level.getBlockState(pos).setValue(AgriCropBlock.SUPPORTED, true)
                    .setValue(AgriCropBlock.SUPPORT_TIER, tier), 2);
            plant.setSupportTicks(37);
        };
        try { AECropBlockEntity.tickSupport(level, pos, level.getBlockState(pos), plant); }
        finally { shapeAction = null; }
        h.assertTrue(CropSupportItem.tier(level.getBlockState(pos)) == tier && plant.supportTicks() == 37,
                "An expiry notification cannot restore the full lifetime of newly installed worn sticks");
        h.succeed();
    }
    @GameTest(template="empty") public static void expiringSupportPreservesNewInstallationWear(GameTestHelper h) { expiry(h, 2); }
    @GameTest(template="empty") public static void expiringSupportPreservesNewWoodInstallationWear(GameTestHelper h) { expiry(h, 1); }
    @GameTest(template="empty") public static void tendingAvoidsUnsupportedComparatorNotifications(GameTestHelper h) {
        var pos = crop(h, 0); var level = h.getLevel(); var plant = (AECropBlockEntity) level.getBlockEntity(pos);
        observe(h, pos); int[] calls = {0};
        comparatorAction = () -> { calls[0]++; level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2); };
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AEItems.TROWEL.get()));
        try { player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false))); }
        finally { comparatorAction = null; }
        h.assertTrue(calls[0] == 0 && level.getBlockEntity(pos) == plant && plant.hasBeenTended()
                        && level.getBlockState(pos).getValue(AgriCropBlock.AGE) == 1,
                "Crop record saves do not issue comparator notifications that can interrupt tending");
        h.succeed();
    }
    private static void rakeInterruption(GameTestHelper h, int change) {
        var pos = crop(h, 0); var level = h.getLevel(); var soil = pos.below();
        var plant = (AECropBlockEntity) level.getBlockEntity(pos);
        level.setBlock(pos, level.getBlockState(pos).setValue(AgriCropBlock.WEEDY, true), 2);
        observe(h, soil);
        onShape = state -> state.hasProperty(FarmBlock.MOISTURE) && state.getValue(FarmBlock.MOISTURE) == 7;
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AEItems.RAKE_IRON.get()));
        int[] calls = {0};
        shapeAction = () -> {
            calls[0]++;
            if (change == 0) level.setBlock(pos, level.getBlockState(pos).setValue(AgriCropBlock.AGE, 7), 2);
            if (change <= 1) {
                var saved = plant.saveWithoutMetadata(level.registryAccess()); saved.putInt("AgriSoilNutrition", 5);
                plant.loadAdditional(saved, level.registryAccess());
            }
            if (change == 2) level.setBlock(soil, AEBlocks.NUTRIENT_FARMLAND.get().defaultBlockState().setValue(FarmBlock.MOISTURE, 7), 2);
            if (change == 3) player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.DIAMOND, 7));
            if (change == 4) player.setHealth(0);
        };
        var rake = player.getMainHandItem();
        try { rake.getItem().onItemUseFirst(rake, new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false))); }
        finally { shapeAction = null; }
        h.assertTrue(calls[0] == 1 && level.getBlockState(pos).getValue(AgriCropBlock.AGE) == (change == 0 ? 7 : 0)
                        && level.getBlockState(pos).getValue(AgriCropBlock.WEEDY) && plant.soilNutrition() == (change <= 1 ? 5 : 40)
                        && player.getInventory().countItem(AEItems.PULLED_WEEDS.get()) == 0,
                "An interrupted rake action preserves the crop and grants no weeds; change=" + change
                        + ", state=" + level.getBlockState(pos) + ", nutrition=" + plant.soilNutrition());
        h.succeed();
    }
    @GameTest(template="empty") public static void rakePreservesCropChangedByWatering(GameTestHelper h) { rakeInterruption(h, 0); }
    @GameTest(template="empty") public static void rakePreservesRecordsChangedByWatering(GameTestHelper h) { rakeInterruption(h, 1); }
    @GameTest(template="empty") public static void rakeStopsAfterSoilReplacement(GameTestHelper h) { rakeInterruption(h, 2); }
    @GameTest(template="empty") public static void rakeStopsAfterHeldToolReplacement(GameTestHelper h) { rakeInterruption(h, 3); }
    @GameTest(template="empty") public static void rakeStopsAfterPlayerDies(GameTestHelper h) { rakeInterruption(h, 4); }
    private static void basinInterruption(GameTestHelper h, int change) {
        var level = h.getLevel(); var basinPos = h.absolutePos(new BlockPos(8, 190, 8));
        var first = basinPos.offset(-6, 0, -6); var last = basinPos.offset(change == 2 ? 8 : 6, 0, 6);
        for (var pos : new BlockPos[]{first, last, first.east(), basinPos}) level.getChunkAt(pos);
        level.setBlock(first, Blocks.FARMLAND.defaultBlockState(), 2);
        level.setBlock(last, Blocks.FARMLAND.defaultBlockState(), 2);
        level.setBlock(basinPos, AEBlocks.IRRIGATION_BASIN.get().defaultBlockState(), 2);
        observe(h, first); onShape = state -> state.hasProperty(FarmBlock.MOISTURE) && state.getValue(FarmBlock.MOISTURE) == 7;
        shapeAction = () -> {
            if (change == 0) level.setBlock(basinPos, Blocks.STONE.defaultBlockState(), 2);
            else if (change == 1) IrrigationBasinBlock.setFilled(level, basinPos, level.getBlockState(basinPos), false);
            else h.assertTrue(((IrrigationBasinBlockEntity) level.getBlockEntity(basinPos)).upgradeTo(1), "A current basin can upgrade during the soil update");
        };
        try { IrrigationBasinBlock.setFilled(level, basinPos, level.getBlockState(basinPos), true); }
        finally { shapeAction = null; }
        h.assertTrue(level.getBlockState(first).getValue(FarmBlock.MOISTURE) == 7
                        && level.getBlockState(last).getValue(FarmBlock.MOISTURE) == (change == 2 ? 7 : 0),
                "The old hydration sweep stops after removal or emptying; an upgraded basin starts its own larger sweep");
        h.succeed();
    }
    @GameTest(template="empty") public static void basinHydrationStopsAfterRemoval(GameTestHelper h) { basinInterruption(h, 0); }
    @GameTest(template="empty") public static void basinHydrationStopsAfterEmptying(GameTestHelper h) { basinInterruption(h, 1); }
    @GameTest(template="empty") public static void basinUpgradeStillHydratesItsExpandedRadius(GameTestHelper h) { basinInterruption(h, 2); }
    @GameTest(template="empty") public static void cropRecordChangesStillMarkTheChunkForSaving(GameTestHelper h) {
        var pos = crop(h, 0); var level = h.getLevel(); var chunk = level.getChunkAt(pos);
        var plant = (AECropBlockEntity) level.getBlockEntity(pos);
        chunk.setUnsaved(false);
        plant.setVariety("Saved crop line");
        h.assertTrue(chunk.isUnsaved(), "Changing crop records still marks the owning chunk for saving");
        var saved = plant.saveWithoutMetadata(level.registryAccess());
        var restored = new AECropBlockEntity(pos, level.getBlockState(pos)); restored.loadAdditional(saved, level.registryAccess());
        h.assertTrue(restored.variety().equals("Saved crop line"), "The saved crop record survives reload");
        h.succeed();
    }
}
