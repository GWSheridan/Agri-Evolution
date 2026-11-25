package com.agrievolution.content;

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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class SoilSurveySafetyGameTests {
    private static final class HookPlayer extends Player {
        BlockPos trigger, denied;
        Runnable hook;
        int permissions;
        HookPlayer(ServerLevel level, BlockPos pos) {
            super(level, pos, 0, new GameProfile(UUID.randomUUID(), "survey-use"));
            setPos(Vec3.atCenterOf(pos));
            getAbilities().mayBuild = true;
        }
        @Override public boolean isCreative() { return false; }
        @Override public boolean isSpectator() { return false; }
        @Override public boolean mayUseItemAt(BlockPos pos, Direction side, ItemStack stack) {
            permissions++;
            if (pos.equals(trigger) && hook != null) {
                var callback = hook;
                hook = null;
                callback.run();
            }
            return !pos.equals(denied);
        }
    }
    private record Rig(BlockPos soil, HookPlayer player, ItemStack sampler) {}
    private static Rig rig(GameTestHelper h, boolean area) {
        var soil = h.absolutePos(new BlockPos(4, 200, 4));
        for (var pos : new BlockPos[]{soil.west(), soil, soil.east()}) {
            h.getLevel().setBlock(pos, Blocks.FARMLAND.defaultBlockState(), 2);
            h.getLevel().setBlock(pos.above(), AEBlocks.AGRI_CROP.get().defaultBlockState().setValue(AgriCropBlock.AGE, 3)
                    .setValue(AgriCropBlock.SUPPORTED, true).setValue(AgriCropBlock.SUPPORT_TIER, 1), 2);
            ((AECropBlockEntity) h.getLevel().getBlockEntity(pos.above())).setSupportTicks(500);
        }
        var player = new HookPlayer(h.getLevel(), soil);
        player.setShiftKeyDown(area);
        var sampler = new ItemStack(AEItems.SOIL_SAMPLE.get());
        CustomData.update(DataComponents.CUSTOM_DATA, sampler, tag -> tag.putInt("SamplerTrial", 7));
        var oldReport = new CompoundTag();
        oldReport.putInt("Valid", 77);
        SoilSurveyData.save(sampler, oldReport);
        player.setItemInHand(InteractionHand.OFF_HAND, sampler);
        return new Rig(soil, player, sampler);
    }
    private static UseOnContext context(Rig r, BlockPos clicked, ItemStack stack) {
        return new UseOnContext(r.player().level(), r.player(), InteractionHand.OFF_HAND, stack,
                new BlockHitResult(Vec3.atCenterOf(clicked), Direction.UP, clicked, false)) {};
    }
    private static InteractionResult use(Rig r, boolean cropClick) {
        return AEItems.SOIL_SAMPLE.get().useOn(context(r, cropClick ? r.soil().above() : r.soil(), r.sampler()));
    }
    private static void mutateCrop(GameTestHelper h, BlockPos crop, int mutation) {
        var plant = (AECropBlockEntity) h.getLevel().getBlockEntity(crop);
        switch (mutation) {
            case 0 -> {
                var tag = new CompoundTag();
                plant.saveAdditional(tag, h.getLevel().registryAccess());
                h.getLevel().removeBlockEntity(crop);
                var replacement = new AECropBlockEntity(crop, h.getLevel().getBlockState(crop));
                replacement.loadAdditional(tag, h.getLevel().registryAccess());
                h.getLevel().setBlockEntity(replacement);
            }
            case 1 -> h.getLevel().setBlock(crop, Blocks.AIR.defaultBlockState(), 2);
            case 2 -> h.getLevel().setBlock(crop, h.getLevel().getBlockState(crop).setValue(AgriCropBlock.AGE, 7), 2);
            case 3 -> plant.addNutrition(11);
            case 4 -> plant.setGenome(new CropGenome(12, 34, 56));
            case 5 -> plant.setRareTrait(RareTrait.VIGOROUS);
            case 6 -> plant.setVariety("New field line");
            case 7 -> plant.setSupportTicks(22);
            case 8 -> plant.setCrossed(true);
            default -> throw new AssertionError("Unknown plot change");
        }
    }
    private static void cancelledCenter(GameTestHelper h, boolean area, boolean cropClick, int mutation, boolean later) {
        var r = rig(h, area);
        var before = r.sampler().copy();
        r.player().trigger = later ? r.soil().east().above() : r.soil().above();
        r.player().hook = () -> mutateCrop(h, r.soil().above(), mutation);
        var result = use(r, cropClick);
        h.assertTrue(r.player().hook == null, "The intended plot permission callback ran");
        h.assertTrue(result == InteractionResult.FAIL && ItemStack.matches(before, r.sampler())
                        && r.player().getInventory().countItem(AEItems.SOIL_SPECIMEN.get()) == 0,
                "A changed center retains the old report and research data and issues no specimen");
        h.succeed();
    }
    @GameTest(template = "empty") public static void replacementCenterEntityCancelsSoilCollection(GameTestHelper h) { cancelledCenter(h, false, false, 0, false); }
    @GameTest(template = "empty") public static void replacementCenterEntityCancelsCropCollection(GameTestHelper h) { cancelledCenter(h, false, true, 0, false); }
    @GameTest(template = "empty") public static void removedCenterCropCancelsSoilCollection(GameTestHelper h) { cancelledCenter(h, false, false, 1, false); }
    @GameTest(template = "empty") public static void newerCenterGrowthCancelsSoilCollection(GameTestHelper h) { cancelledCenter(h, false, false, 2, false); }
    @GameTest(template = "empty") public static void newerCenterNutritionCancelsCropCollection(GameTestHelper h) { cancelledCenter(h, false, true, 3, false); }
    @GameTest(template = "empty") public static void changedCenterGenesCancelSurvey(GameTestHelper h) { cancelledCenter(h, true, false, 4, false); }
    @GameTest(template = "empty") public static void changedCenterRareTraitCancelsSurvey(GameTestHelper h) { cancelledCenter(h, true, true, 5, false); }
    @GameTest(template = "empty") public static void changedCenterVarietyCancelsSurvey(GameTestHelper h) { cancelledCenter(h, true, false, 6, false); }
    @GameTest(template = "empty") public static void changedCenterWearDuringNeighborCheckCancelsSurvey(GameTestHelper h) { cancelledCenter(h, true, false, 7, true); }
    @GameTest(template = "empty") public static void changedCenterCreditDuringNeighborCheckCancelsSurvey(GameTestHelper h) { cancelledCenter(h, true, true, 8, true); }

    private static void neighbor(GameTestHelper h, int mutation, boolean later) {
        var r = rig(h, true);
        r.player().trigger = (later ? r.soil().east() : r.soil().west()).above();
        r.player().hook = () -> mutateCrop(h, r.soil().west().above(), mutation);
        var result = use(r, false);
        var report = SoilSurveyData.report(r.sampler());
        h.assertTrue(r.player().hook == null && result.consumesAction(), "An independent neighbor change leaves the center usable");
        h.assertTrue(report.getInt("Valid") == 2 && report.getInt("Crops") == 2 && SoilResearch.progress(r.sampler()) == 2
                        && !SoilSurveyData.plots(report).getCompound(11).getBoolean("Valid"),
                "Changed plots are excluded from both measurements and discovery credit, even after an earlier permission passed");
        h.assertTrue(r.player().getInventory().countItem(AEItems.SOIL_SPECIMEN.get()) == 0, "An area survey never collects a physical specimen");
        h.succeed();
    }
    @GameTest(template = "empty") public static void replacementNeighborEntityIsExcludedFromSurvey(GameTestHelper h) { neighbor(h, 0, false); }
    @GameTest(template = "empty") public static void changedNeighborNutritionIsExcludedFromSurvey(GameTestHelper h) { neighbor(h, 3, false); }
    @GameTest(template = "empty") public static void changedNeighborGenesAreExcludedFromSurvey(GameTestHelper h) { neighbor(h, 4, false); }
    @GameTest(template = "empty") public static void changedNeighborVarietyIsExcludedFromSurvey(GameTestHelper h) { neighbor(h, 6, false); }
    @GameTest(template = "empty") public static void changedNeighborWearIsExcludedFromSurvey(GameTestHelper h) { neighbor(h, 7, false); }
    @GameTest(template = "empty") public static void earlierReplacementEntityIsExcludedAfterLaterPermissions(GameTestHelper h) { neighbor(h, 0, true); }
    @GameTest(template = "empty") public static void earlierNutritionChangeIsExcludedAfterLaterPermissions(GameTestHelper h) { neighbor(h, 3, true); }
    @GameTest(template = "empty") public static void earlierGenesChangeIsExcludedAfterLaterPermissions(GameTestHelper h) { neighbor(h, 4, true); }
    @GameTest(template = "empty") public static void earlierVarietyChangeIsExcludedAfterLaterPermissions(GameTestHelper h) { neighbor(h, 6, true); }
    @GameTest(template = "empty") public static void earlierWearChangeIsExcludedAfterLaterPermissions(GameTestHelper h) { neighbor(h, 7, true); }
    @GameTest(template = "empty") public static void earlierRemovedCropIsExcludedAfterLaterPermissions(GameTestHelper h) { neighbor(h, 1, true); }

    private static void modeChanged(GameTestHelper h, boolean area, boolean later) {
        var r = rig(h, area);
        var before = r.sampler().copy();
        r.player().trigger = later ? r.soil().west().above() : r.soil();
        r.player().hook = () -> r.player().setShiftKeyDown(!area);
        use(r, false);
        h.assertTrue(r.player().hook == null && ItemStack.matches(before, r.sampler())
                        && r.player().getInventory().countItem(AEItems.SOIL_SPECIMEN.get()) == 0,
                "Switching modes cannot turn an older survey into collection or write a different report");
        h.succeed();
    }
    @GameTest(template = "empty") public static void startingCrouchDuringCollectionCancelsOldClick(GameTestHelper h) { modeChanged(h, false, false); }
    @GameTest(template = "empty") public static void leavingCrouchDuringSurveyCannotCollectSpecimen(GameTestHelper h) { modeChanged(h, true, false); }
    @GameTest(template = "empty") public static void leavingCrouchDuringNeighborCheckCancelsSurvey(GameTestHelper h) { modeChanged(h, true, true); }

    @GameTest(template = "empty") public static void newCropDuringSoilPermissionCancelsCollection(GameTestHelper h) {
        var r = rig(h, false);
        h.getLevel().setBlock(r.soil().above(), Blocks.AIR.defaultBlockState(), 2);
        var before = r.sampler().copy();
        r.player().trigger = r.soil();
        r.player().hook = () -> h.getLevel().setBlock(r.soil().above(), AEBlocks.TOMATO_CROP.get().defaultBlockState(), 2);
        use(r, false);
        h.assertTrue(ItemStack.matches(before, r.sampler()) && r.player().getInventory().countItem(AEItems.SOIL_SPECIMEN.get()) == 0,
                "A newly planted crop cannot inherit the old empty-plot sampling click");
        h.succeed();
    }
    @GameTest(template = "empty") public static void deadSamplerUseInvokesNoPlotPermissions(GameTestHelper h) {
        var r = rig(h, false);
        r.player().setHealth(0);
        var before = r.sampler().copy();
        use(r, false);
        h.assertTrue(r.player().permissions == 0 && ItemStack.matches(before, r.sampler()), "Dead players are rejected before inspecting plot permissions");
        h.succeed();
    }
    @GameTest(template = "empty") public static void detachedSamplerUseInvokesNoPlotPermissions(GameTestHelper h) {
        var r = rig(h, false);
        AEItems.SOIL_SAMPLE.get().useOn(context(r, r.soil(), r.sampler().copy()));
        h.assertTrue(r.player().permissions == 0, "Detached context copies cannot invoke plot permission callbacks");
        h.succeed();
    }
    @GameTest(template = "empty") public static void unloadedSamplerClicksDoNotLoadDistantChunks(GameTestHelper h) {
        var r = rig(h, false);
        var remote = r.soil().offset(24000, 0, 24000);
        h.assertTrue(!h.getLevel().hasChunkAt(remote), "The target starts unloaded");
        AEItems.SOIL_SAMPLE.get().useOn(context(r, remote, r.sampler()));
        h.assertTrue(!h.getLevel().hasChunkAt(remote), "Stale sampling contexts cannot load distant terrain");
        h.succeed();
    }
    @GameTest(template = "empty") public static void mismatchedCropWorldCannotEnterSoilReport(GameTestHelper h) {
        var r = rig(h, false);
        var before = r.sampler().copy();
        h.getLevel().getBlockEntity(r.soil().above()).setLevel(h.getLevel().getServer().getLevel(Level.NETHER));
        use(r, false);
        h.assertTrue(r.player().permissions == 0 && ItemStack.matches(before, r.sampler()), "A crop attached to another world cannot enter a report or issue collection credit");
        h.succeed();
    }

    @GameTest(template = "empty") public static void normalSingleSurveyCollectsOnlyOnce(GameTestHelper h) {
        var r = rig(h, false);
        use(r, false);
        use(r, false);
        var report = SoilSurveyData.report(r.sampler());
        h.assertTrue(!report.getBoolean("Area") && report.getInt("Valid") == 1 && report.getInt("Nutrition") == 40
                        && SoilResearch.progress(r.sampler()) == 1 && r.player().getInventory().countItem(AEItems.SOIL_SPECIMEN.get()) == 1,
                "Normal sampling keeps measurements and one specimen per plot");
        h.succeed();
    }
    @GameTest(template = "empty") public static void normalAreaSurveyKeepsEveryUnchangedPlot(GameTestHelper h) {
        var r = rig(h, true);
        use(r, false);
        var report = SoilSurveyData.report(r.sampler());
        h.assertTrue(report.getBoolean("Area") && report.getInt("Total") == 25 && report.getInt("Valid") == 3
                        && report.getInt("Crops") == 3 && SoilResearch.progress(r.sampler()) == 3
                        && r.player().getInventory().countItem(AEItems.SOIL_SPECIMEN.get()) == 0, "A normal survey keeps all permitted crops and grants no specimen");
        h.succeed();
    }
    @GameTest(template = "empty") public static void normalCropClickMeasuresItsSoil(GameTestHelper h) {
        var r = rig(h, false);
        use(r, true);
        h.assertTrue(SoilSurveyData.report(r.sampler()).getInt("Y") == r.soil().getY()
                        && r.player().getInventory().countItem(AEItems.SOIL_SPECIMEN.get()) == 1, "Clicking the crop still samples the soil below it");
        h.succeed();
    }
    @GameTest(template = "empty") public static void deniedNeighborAddsNoSurveyOrResearchCredit(GameTestHelper h) {
        var r = rig(h, true);
        r.player().denied = r.soil().west().above();
        use(r, false);
        h.assertTrue(SoilSurveyData.report(r.sampler()).getInt("Valid") == 2 && SoilResearch.progress(r.sampler()) == 2,
                "An ordinary denied neighbor is omitted while the permitted field remains usable");
        h.succeed();
    }
    @GameTest(template = "empty") public static void naturalSoilCollectionRetainsItsKindAndOldReport(GameTestHelper h) {
        var r = rig(h, false);
        h.getLevel().setBlock(r.soil().above(), Blocks.AIR.defaultBlockState(), 2);
        h.getLevel().setBlock(r.soil(), Blocks.COARSE_DIRT.defaultBlockState(), 2);
        use(r, false);
        var specimen = r.player().getInventory().items.stream().filter(s -> s.is(AEItems.SOIL_SPECIMEN.get())).findFirst().orElseThrow();
        h.assertTrue(SoilSpecimenItem.kind(specimen).equals("coarse") && SoilSurveyData.report(r.sampler()).getInt("Valid") == 77
                        && SoilResearch.progress(r.sampler()) == 0, "Natural soil collection preserves the specimen kind and existing farmland report");
        h.succeed();
    }
    @GameTest(template = "empty") public static void emptyFarmlandStillProducesAValidSoilReport(GameTestHelper h) {
        var r = rig(h, false);
        h.getLevel().setBlock(r.soil().above(), Blocks.AIR.defaultBlockState(), 2);
        use(r, false);
        var report = SoilSurveyData.report(r.sampler());
        h.assertTrue(report.getInt("Valid") == 1 && report.getInt("Crops") == 0 && report.getInt("NutritionCount") == 0
                        && r.player().getInventory().countItem(AEItems.SOIL_SPECIMEN.get()) == 1, "Empty farmland retains soil measurements without invented crop nutrition");
        h.succeed();
    }
}
