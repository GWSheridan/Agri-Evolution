package com.agrievolution.content;

import com.agrievolution.farming.FarmEnvironment;
import com.agrievolution.farming.SoilResearch;
import com.agrievolution.farming.SoilSurveyData;
import com.agrievolution.genetics.CropGenome;
import com.agrievolution.genetics.RareTrait;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class SoilSamplingSafetyGameTests {
    private static BlockPos soil(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        h.getLevel().setBlock(pos, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7), 2);
        return pos;
    }

    private static UseOnContext context(Player player, BlockPos pos) {
        return new UseOnContext(player, InteractionHand.OFF_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }

    private static ItemStack equip(Player player, BlockPos pos) {
        player.setPos(Vec3.atCenterOf(pos));
        var sampler = new ItemStack(AEItems.SOIL_SAMPLE.get());
        player.setItemInHand(InteractionHand.OFF_HAND, sampler);
        return sampler;
    }

    private static class AccessPlayer extends Player {
        BlockPos denied;
        Runnable onPermission = () -> {};
        AccessPlayer(ServerLevel level) { super(level, BlockPos.ZERO, 0, new GameProfile(UUID.randomUUID(), "soil-access")); }
        @Override public boolean isCreative() { return false; }
        @Override public boolean isSpectator() { return false; }
        @Override public boolean mayUseItemAt(BlockPos pos, Direction face, ItemStack stack) {
            var callback = onPermission;
            onPermission = () -> {};
            callback.run();
            return !pos.equals(denied);
        }
    }

    private static void deniedPlayer(GameTestHelper h, GameType mode) {
        var pos = soil(h);
        var player = h.makeMockPlayer(mode);
        if (mode == GameType.ADVENTURE) player.getAbilities().mayBuild = false;
        var sampler = equip(player, pos);
        var before = sampler.copy();
        // Exercise the item's own handler; vanilla can return PASS before delegating Adventure use.
        var result = sampler.getItem().useOn(context(player, pos));
        h.assertTrue(result == InteractionResult.FAIL && ItemStack.matches(sampler, before)
                        && player.getInventory().countItem(AEItems.SOIL_SPECIMEN.get()) == 0,
                "Denied sampling changes neither the sampler nor physical specimens");
        h.succeed();
    }

    @GameTest(template = "empty") public static void spectatorsCannotCollectSoil(GameTestHelper h) { deniedPlayer(h, GameType.SPECTATOR); }
    @GameTest(template = "empty") public static void soilCollectionRespectsBuildPermission(GameTestHelper h) { deniedPlayer(h, GameType.ADVENTURE); }

    private static void deniedPlot(GameTestHelper h, boolean clickCrop) {
        var pos = soil(h);
        h.getLevel().setBlock(pos.above(), AEBlocks.AGRI_CROP.get().defaultBlockState(), 2);
        var player = new AccessPlayer(h.getLevel());
        player.denied = clickCrop ? pos : pos.above();
        var sampler = equip(player, pos);
        var result = sampler.useOn(context(player, clickCrop ? pos.above() : pos));
        h.assertTrue(result == InteractionResult.FAIL && !SoilSurveyData.hasReport(sampler)
                        && player.getInventory().countItem(AEItems.SOIL_SPECIMEN.get()) == 0,
                "Sampling checks access to both the soil and its planted crop");
        h.succeed();
    }

    @GameTest(template = "empty") public static void cropSamplingChecksTheSoilPermission(GameTestHelper h) { deniedPlot(h, true); }
    @GameTest(template = "empty") public static void soilSamplingChecksTheCropPermission(GameTestHelper h) { deniedPlot(h, false); }

    @GameTest(template = "empty")
    public static void changedSoilCannotProduceAStaleSpecimen(GameTestHelper h) {
        var pos = soil(h);
        var player = new AccessPlayer(h.getLevel());
        var sampler = equip(player, pos);
        player.onPermission = () -> h.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
        var result = sampler.useOn(context(player, pos));
        h.assertTrue(result == InteractionResult.FAIL && !SoilSurveyData.hasReport(sampler)
                        && player.getInventory().countItem(AEItems.SOIL_SPECIMEN.get()) == 0,
                "A soil replacement during permission checking earns neither a specimen nor collection credit");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void changedHeldSamplerCancelsCollection(GameTestHelper h) {
        var pos = soil(h);
        var player = new AccessPlayer(h.getLevel());
        var sampler = equip(player, pos);
        var before = sampler.copy();
        player.onPermission = () -> player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.DIRT, 3));
        var result = sampler.useOn(context(player, pos));
        h.assertTrue(result == InteractionResult.FAIL && ItemStack.matches(sampler, before)
                        && player.getOffhandItem().is(Items.DIRT) && player.getOffhandItem().getCount() == 3
                        && player.getInventory().countItem(AEItems.SOIL_SPECIMEN.get()) == 0,
                "A stale sampler context cannot issue a specimen after the held tool changes");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void areaSurveySkipsDeniedPlotsAndTheirProgress(GameTestHelper h) {
        var center = soil(h);
        for (int z = -2; z <= 2; z++) for (int x = -2; x <= 2; x++)
            h.getLevel().setBlock(center.offset(x, 0, z), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7), 2);
        var player = new AccessPlayer(h.getLevel());
        player.denied = center.east();
        player.setShiftKeyDown(true);
        var sampler = equip(player, center);
        h.assertTrue(sampler.useOn(context(player, center)).consumesAction(), "The accessible centre permits a survey");
        var report = SoilSurveyData.report(sampler);
        h.assertTrue(report.getInt("Valid") == 24 && SoilResearch.progress(sampler) == 24
                        && !SoilSurveyData.plots(report).getCompound(13).getBoolean("Valid")
                        && player.getInventory().countItem(AEItems.SOIL_SPECIMEN.get()) == 0,
                "The 5x5 report and discovery credit omit the denied plot without issuing physical specimens");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void areaSurveyCannotSaveToAToolChangedDuringANeighborCheck(GameTestHelper h) {
        var center = soil(h);
        h.getLevel().setBlock(center.east(), Blocks.FARMLAND.defaultBlockState(), 2);
        var player = new AccessPlayer(h.getLevel()) {
            @Override public boolean mayUseItemAt(BlockPos pos, Direction face, ItemStack stack) {
                if (pos.equals(center.east())) setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.DIRT));
                return true;
            }
        };
        player.setShiftKeyDown(true);
        var sampler = equip(player, center);
        var before = sampler.copy();
        var result = sampler.useOn(context(player, center));
        h.assertTrue(result == InteractionResult.FAIL && ItemStack.matches(sampler, before)
                        && player.getOffhandItem().is(Items.DIRT),
                "Neighbor checks cannot save a completed area report into a detached old sampler");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void vigorousCropReportUsesTheActualGrowthCap(GameTestHelper h) {
        var pos = soil(h);
        h.getLevel().setBlock(pos, AEBlocks.CELESTIAL_FARMLAND.get().defaultBlockState().setValue(FarmBlock.MOISTURE, 7), 2);
        h.getLevel().setBlock(pos.above(), AEBlocks.AGRI_CROP.get().defaultBlockState()
                .setValue(AgriCropBlock.SUPPORTED, true).setValue(AgriCropBlock.SUPPORT_TIER, 3), 2);
        var crop = (AECropBlockEntity) h.getLevel().getBlockEntity(pos.above());
        crop.setGenome(new CropGenome(100, 100, 100));
        crop.addNutrition(100);
        crop.setRareTrait(RareTrait.VIGOROUS);
        var report = SoilSurveyData.survey(h.getLevel(), pos, false);
        int expected = Math.min(195, FarmEnvironment.growthChance(100, 100, 7, 3,
                FarmEnvironment.irrigated(h.getLevel(), pos)) + 18 + 5 * 20);
        h.assertTrue(report.getInt("GrowthRoll") == expected && expected == 195,
                "The displayed growth roll matches the crop's capped chance including Vigorous and soil bonuses");
        h.succeed();
    }

    private static SoilResearchTableBlockEntity table(GameTestHelper h, BlockPos pos) {
        h.getLevel().setBlock(pos, AEBlocks.SOIL_RESEARCH_TABLE.get().defaultBlockState(), 2);
        return (SoilResearchTableBlockEntity) h.getLevel().getBlockEntity(pos);
    }

    private static ItemStack studiedSampler(int count) {
        var stack = new ItemStack(AEItems.SOIL_SAMPLE.get());
        var tag = new CompoundTag();
        var plots = new ListTag();
        for (int i = 0; i < count; i++) plots.add(StringTag.valueOf("test:studied:" + i));
        tag.put("AgriAnalyzedSoil", plots);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    private static void tableAccess(GameTestHelper h, boolean refinement, boolean removed) {
        var pos = soil(h).above(3);
        var table = table(h, pos);
        var player = h.makeMockPlayer(removed ? GameType.SURVIVAL : GameType.SPECTATOR);
        player.setPos(Vec3.atCenterOf(pos));
        ItemStack sampler;
        if (refinement) {
            sampler = studiedSampler(8);
            table.setItem(SoilResearchTableBlockEntity.SOIL, new ItemStack(Items.DIRT, 2));
            table.setItem(SoilResearchTableBlockEntity.CATALYST, new ItemStack(AEItems.NUTRIENT_COMPOST.get(), 2));
        } else {
            sampler = new ItemStack(AEItems.SOIL_SAMPLE.get());
            SoilResearch.collect(sampler, h.getLevel(), pos.below(3));
            table.setItem(SoilResearchTableBlockEntity.SPECIMEN, SoilSpecimenItem.from(h.getLevel(), pos.below(3)));
        }
        table.setItem(SoilResearchTableBlockEntity.SAMPLER, sampler);
        var before = sampler.copy();
        if (removed) table.setRemoved();
        if (refinement) table.refine(player); else table.analyze(player);
        h.assertTrue(ItemStack.matches(sampler, before) && table.getItem(SoilResearchTableBlockEntity.OUTPUT).isEmpty()
                        && (refinement ? table.getItem(SoilResearchTableBlockEntity.SOIL).getCount() == 2
                        && table.getItem(SoilResearchTableBlockEntity.CATALYST).getCount() == 2
                        : table.getItem(SoilResearchTableBlockEntity.SPECIMEN).getCount() == 1),
                "Direct research calls respect access and retain every input when rejected");
        h.succeed();
    }

    @GameTest(template = "empty") public static void removedTableCannotAnalyze(GameTestHelper h) { tableAccess(h, false, true); }
    @GameTest(template = "empty") public static void removedTableCannotRefine(GameTestHelper h) { tableAccess(h, true, true); }
    @GameTest(template = "empty") public static void spectatorCannotDirectlyAnalyze(GameTestHelper h) { tableAccess(h, false, false); }
    @GameTest(template = "empty") public static void spectatorCannotDirectlyRefine(GameTestHelper h) { tableAccess(h, true, false); }

    @GameTest(template = "empty")
    public static void authorizedMasteryTableRefinesTheExactBatch(GameTestHelper h) {
        var pos = soil(h).above(3);
        h.getLevel().setBlock(pos, AEBlocks.SOIL_RESEARCH_TABLE.get().defaultBlockState().setValue(SoilResearchTableBlock.TIER, 3), 2);
        var table = (SoilResearchTableBlockEntity) h.getLevel().getBlockEntity(pos);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(pos));
        var sampler = studiedSampler(96);
        table.setItem(SoilResearchTableBlockEntity.SAMPLER, sampler);
        table.setItem(SoilResearchTableBlockEntity.SOIL, new ItemStack(AEItems.ARCANE_FARMLAND.get(), 5));
        table.setItem(SoilResearchTableBlockEntity.CATALYST, new ItemStack(Items.ECHO_SHARD, 4));
        table.refine(player);
        h.assertTrue(table.getItem(SoilResearchTableBlockEntity.OUTPUT).is(AEItems.CELESTIAL_FARMLAND.get())
                        && table.getItem(SoilResearchTableBlockEntity.OUTPUT).getCount() == 4
                        && table.getItem(SoilResearchTableBlockEntity.SOIL).getCount() == 1
                        && table.getItem(SoilResearchTableBlockEntity.CATALYST).isEmpty() && SoilResearch.analyzed(sampler) == 96,
                "Authorized Mk III refinement still spends one soil and catalyst per block in its four-block batch");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void authorizedOffhandSamplingStillAnalyzesOnce(GameTestHelper h) {
        var pos = soil(h);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var sampler = equip(player, pos);
        h.assertTrue(sampler.useOn(context(player, pos)).consumesAction(), "Offhand sampling succeeds");
        sampler.useOn(context(player, pos));
        h.assertTrue(player.getInventory().countItem(AEItems.SOIL_SPECIMEN.get()) == 1 && SoilSurveyData.hasReport(sampler),
                "Repeated collection yields exactly one specimen and retains the report");
        var tablePos = pos.above(3);
        var table = table(h, tablePos);
        player.setPos(Vec3.atCenterOf(tablePos));
        table.setItem(SoilResearchTableBlockEntity.SAMPLER, sampler);
        var specimen = player.getInventory().items.stream().filter(s -> s.is(AEItems.SOIL_SPECIMEN.get())).findFirst().orElseThrow();
        table.setItem(SoilResearchTableBlockEntity.SPECIMEN, specimen.copy());
        table.analyze(player);
        h.assertTrue(SoilResearch.analyzed(sampler) == 1 && table.getItem(SoilResearchTableBlockEntity.SPECIMEN).isEmpty(),
                "The authorized specimen still grants exactly one analysis credit");
        h.succeed();
    }
}
