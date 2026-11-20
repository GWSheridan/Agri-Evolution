package com.agrievolution.content;

import com.agrievolution.genetics.*;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class TrowelPlantingSafetyGameTests {
    private static final CropGenome GENES = new CropGenome(81, 72, 63);
    private static final class HookPlayer extends Player {
        BlockPos trigger;
        Runnable hook;
        boolean spectator, creative;
        HookPlayer(ServerLevel level, BlockPos pos) {
            super(level, pos, 0, new GameProfile(UUID.randomUUID(), "trowel-plant"));
            setPos(Vec3.atCenterOf(pos));
            getAbilities().mayBuild = true;
        }
        @Override public boolean isCreative() { return creative; }
        @Override public boolean isSpectator() { return spectator; }
        void changeWorld(Level level) { setLevel(level); }
        @Override public boolean mayUseItemAt(BlockPos pos, Direction side, ItemStack stack) {
            if (pos.equals(trigger) && hook != null) {
                var callback = hook;
                hook = null;
                callback.run();
            }
            return super.mayUseItemAt(pos, side, stack);
        }
    }
    private record Rig(BlockPos soil, HookPlayer player, ItemStack tool, ItemStack seeds) {}
    private static Rig rig(GameTestHelper h, boolean patch, boolean nativeSeed, int count) {
        var soil = h.absolutePos(new BlockPos(4, 190, 4));
        for (var pos : new BlockPos[]{soil, soil.east()}) {
            h.getLevel().setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 2);
            h.getLevel().setBlock(pos, Blocks.FARMLAND.defaultBlockState(), 2);
        }
        var player = new HookPlayer(h.getLevel(), soil);
        player.setShiftKeyDown(patch);
        var tool = new ItemStack(AEItems.TROWEL.get());
        var seeds = new ItemStack(nativeSeed ? Items.WHEAT_SEEDS : AEItems.WILD_SEED.get(), count);
        if (!nativeSeed) {
            GenomeStorage.putOnSeed(seeds, GENES);
            SeedVariety.apply(seeds, "Field trial");
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        player.setItemInHand(InteractionHand.OFF_HAND, seeds);
        return new Rig(soil, player, tool, seeds);
    }
    private static void use(Rig r) {
        AEItems.TROWEL.get().onItemUseFirst(r.player().getMainHandItem(),
                new UseOnContext(r.player(), InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(r.soil()), Direction.UP, r.soil(), false)));
    }
    private static void mutate(GameTestHelper h, Rig r, int mutation) {
        switch (mutation) {
            case 0 -> r.player().setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AEItems.TROWEL.get()));
            case 1 -> r.tool().set(DataComponents.CUSTOM_NAME, Component.literal("New trowel"));
            case 2 -> CustomData.update(DataComponents.CUSTOM_DATA, r.tool(), tag -> tag.putInt("TrowelTrial", 2));
            case 3 -> r.player().setItemInHand(InteractionHand.OFF_HAND, r.seeds().copy());
            case 4 -> GenomeStorage.putOnSeed(r.seeds(), new CropGenome(42, 53, 64));
            case 5 -> r.seeds().set(DataComponents.CUSTOM_NAME, Component.literal("New seed line"));
            case 6 -> r.seeds().shrink(1);
            case 7 -> r.player().setHealth(0);
            case 8 -> r.player().spectator = true;
            case 9 -> r.player().getAbilities().mayBuild = false;
            case 10 -> r.player().changeWorld(h.getLevel().getServer().getLevel(Level.NETHER));
            case 11 -> r.player().setShiftKeyDown(!r.player().isShiftKeyDown());
            default -> throw new AssertionError("Unknown interruption");
        }
    }
    private static void interruption(GameTestHelper h, boolean patch, int mutation, boolean initial, boolean secondPlot) {
        var r = rig(h, patch, false, 4);
        r.player().trigger = initial ? r.soil() : (secondPlot ? r.soil().east().above() : r.soil().above());
        var expectedTool = new ItemStack[1];
        var expectedSeed = new ItemStack[1];
        r.player().hook = () -> {
            mutate(h, r, mutation);
            expectedTool[0] = r.player().getMainHandItem().copy();
            expectedSeed[0] = r.player().getOffhandItem().copy();
        };
        use(r);
        h.assertTrue(r.player().hook == null, "The intended permission callback ran");
        h.assertTrue(h.getLevel().isEmptyBlock((secondPlot ? r.soil().east() : r.soil()).above()), "Interrupted planting leaves the checked plot empty");
        if (secondPlot) assertCrop(h, r.soil().above(), true);
        else h.assertTrue(h.getLevel().isEmptyBlock(r.soil().east().above()), "An interrupted input stops later patch plots");
        h.assertTrue(ItemStack.matches(expectedSeed[0], r.player().getOffhandItem()), "Newer seeds, counts and metadata are preserved");
        h.assertTrue(ItemStack.matches(expectedTool[0], r.player().getMainHandItem()), "Only completed plots cost durability; no old active appearance is written");
        h.assertTrue(!r.player().getCooldowns().isOnCooldown(AEItems.TROWEL.get()), "Interrupted work adds no cooldown");
        h.succeed();
    }
    @GameTest(template = "empty") public static void initialSeedReplacementCancelsSinglePlanting(GameTestHelper h) { interruption(h, false, 3, true, false); }
    @GameTest(template = "empty") public static void initialSeedGenesChangeCancelsPatch(GameTestHelper h) { interruption(h, true, 4, true, false); }
    @GameTest(template = "empty") public static void initialSeedNameChangeCancelsSinglePlanting(GameTestHelper h) { interruption(h, false, 5, true, false); }
    @GameTest(template = "empty") public static void swappedTrowelAtSeedPermissionCancelsSinglePlanting(GameTestHelper h) { interruption(h, false, 0, false, false); }
    @GameTest(template = "empty") public static void renamedTrowelAtSeedPermissionCancelsPatch(GameTestHelper h) { interruption(h, true, 1, false, false); }
    @GameTest(template = "empty") public static void changedTrowelDataAtSeedPermissionCancelsSinglePlanting(GameTestHelper h) { interruption(h, false, 2, false, false); }
    @GameTest(template = "empty") public static void matchingSeedReplacementCancelsSinglePlanting(GameTestHelper h) { interruption(h, false, 3, false, false); }
    @GameTest(template = "empty") public static void changedSeedGenesCancelPatch(GameTestHelper h) { interruption(h, true, 4, false, false); }
    @GameTest(template = "empty") public static void changedSeedCountCancelsPatch(GameTestHelper h) { interruption(h, true, 6, false, false); }
    @GameTest(template = "empty") public static void deathDuringSeedPermissionCancelsSinglePlanting(GameTestHelper h) { interruption(h, false, 7, false, false); }
    @GameTest(template = "empty") public static void spectatorDuringSeedPermissionStopsPatch(GameTestHelper h) { interruption(h, true, 8, false, false); }
    @GameTest(template = "empty") public static void revokedBuildAccessStopsPatch(GameTestHelper h) { interruption(h, true, 9, false, false); }
    @GameTest(template = "empty") public static void worldChangeCancelsSinglePlanting(GameTestHelper h) { interruption(h, false, 10, false, false); }
    @GameTest(template = "empty") public static void crouchChangeCancelsSinglePlanting(GameTestHelper h) { interruption(h, false, 11, false, false); }
    @GameTest(template = "empty") public static void leavingCrouchStopsPatch(GameTestHelper h) { interruption(h, true, 11, false, false); }
    @GameTest(template = "empty") public static void secondPlotToolReplacementKeepsOnlyFirstPlanting(GameTestHelper h) { interruption(h, true, 0, false, true); }
    @GameTest(template = "empty") public static void secondPlotSeedLineChangeKeepsOnlyFirstPlanting(GameTestHelper h) { interruption(h, true, 5, false, true); }
    @GameTest(template = "empty") public static void secondPlotDeathKeepsOnlyFirstPlanting(GameTestHelper h) { interruption(h, true, 7, false, true); }

    private static void changedPlot(GameTestHelper h, boolean patch, boolean soil) {
        var r = rig(h, patch, true, 4);
        r.player().trigger = r.soil().above();
        var replacement = soil ? Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7) : Blocks.TALL_GRASS.defaultBlockState();
        r.player().hook = () -> h.getLevel().setBlock(soil ? r.soil() : r.soil().above(), replacement, 2);
        use(r);
        h.assertTrue(r.player().hook == null, "Plot permission callback ran");
        h.assertTrue(h.getLevel().getBlockState(soil ? r.soil() : r.soil().above()) == replacement, "Changed soil or occupied space is retained");
        if (soil) h.assertTrue(h.getLevel().isEmptyBlock(r.soil().above()), "Changed soil does not receive the older planting");
        h.assertTrue(h.getLevel().getBlockState(r.soil().east().above()).is(patch ? Blocks.WHEAT : Blocks.AIR), "Only patches continue to the unchanged neighbor");
        h.assertTrue(r.seeds().getCount() == (patch ? 3 : 4) && r.tool().getDamageValue() == (patch ? 1 : 0), "Only the unchanged plot pays seed and tool costs");
        h.succeed();
    }
    @GameTest(template = "empty") public static void newOccupantCannotBeReplacedBySinglePlanting(GameTestHelper h) { changedPlot(h, false, false); }
    @GameTest(template = "empty") public static void newOccupantIsSkippedByPatch(GameTestHelper h) { changedPlot(h, true, false); }
    @GameTest(template = "empty") public static void changedMoistureCancelsSinglePlanting(GameTestHelper h) { changedPlot(h, false, true); }
    @GameTest(template = "empty") public static void changedMoistureSkipsOnlyThatPatchPlot(GameTestHelper h) { changedPlot(h, true, true); }

    private static void placementEvent(GameTestHelper h, boolean patch, int mutation) {
        var r = rig(h, patch, false, 4);
        var eventRan = new boolean[1];
        Consumer<BlockEvent.EntityPlaceEvent> listener = event -> {
            if (!event.getPos().equals(r.soil().above())) return;
            eventRan[0] = true;
            if (mutation < 3) mutate(h, r, mutation);
            else if (mutation == 3) r.player().setItemInHand(InteractionHand.OFF_HAND, r.seeds().copy());
            else if (mutation == 4) h.getLevel().setBlock(r.soil().above(), AEBlocks.LETTUCE_CROP.get().defaultBlockState(), 2);
            else ((AECropBlockEntity) h.getLevel().getBlockEntity(r.soil().above())).setGenome(new CropGenome(32, 43, 54));
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try { use(r); } finally { NeoForge.EVENT_BUS.unregister(listener); }
        h.assertTrue(eventRan[0], "The actual NeoForge placement event ran");
        h.assertTrue(h.getLevel().isEmptyBlock(r.soil().east().above()), "An interrupted placement stops later plots");
        var state = h.getLevel().getBlockState(r.soil().above());
        var plant = (AECropBlockEntity) h.getLevel().getBlockEntity(r.soil().above());
        h.assertTrue(state.getValue(AgriCropBlock.AGE) == 0 && !plant.hasBeenTended() && plant.soilNutrition() == 40,
                "A committed placement with changed inputs or crop receives no old trowel bonus");
        h.assertTrue(r.player().getMainHandItem().getDamageValue() == 0 && !GardenToolItem.recentlyUsed(r.player().getMainHandItem(), h.getLevel())
                        && !r.player().getCooldowns().isOnCooldown(AEItems.TROWEL.get()), "An interrupted event cannot charge or activate the tool");
        h.assertTrue(r.seeds().getCount() == 3, "The committed seed placement retains its normal seed cost");
        if (mutation == 4) h.assertTrue(state.is(AEBlocks.LETTUCE_CROP.get()), "The replacement species remains intact");
        if (mutation == 5) h.assertTrue(plant.genome().equals(new CropGenome(32, 43, 54)), "Newer crop genetics remain intact");
        h.succeed();
    }
    @GameTest(template = "empty") public static void placementToolSwapCannotGiveSingleBonus(GameTestHelper h) { placementEvent(h, false, 0); }
    @GameTest(template = "empty") public static void placementToolRenameStopsPatch(GameTestHelper h) { placementEvent(h, true, 1); }
    @GameTest(template = "empty") public static void placementToolDataChangeCannotGiveSingleBonus(GameTestHelper h) { placementEvent(h, false, 2); }
    @GameTest(template = "empty") public static void placementSeedReplacementStopsPatch(GameTestHelper h) { placementEvent(h, true, 3); }
    @GameTest(template = "empty") public static void replacementSpeciesCannotReceiveSingleBonus(GameTestHelper h) { placementEvent(h, false, 4); }
    @GameTest(template = "empty") public static void replacementGenesCannotReceivePatchBonus(GameTestHelper h) { placementEvent(h, true, 5); }

    private static void assertCrop(GameTestHelper h, BlockPos pos, boolean bonus) {
        var state = h.getLevel().getBlockState(pos);
        var plant = (AECropBlockEntity) h.getLevel().getBlockEntity(pos);
        h.assertTrue(state.is(AEBlocks.AGRI_CROP.get()) && state.getValue(AgriCropBlock.AGE) == (bonus ? 1 : 0)
                        && plant.hasBeenTended() == bonus && plant.soilNutrition() == (bonus ? 50 : 40), "Correct species receives exactly one planting bonus");
        h.assertTrue(plant.genome().equals(GENES) && plant.variety().equals("Field trial"), "Planting retains the original genes and named seed line");
    }
    private static void normal(GameTestHelper h, boolean patch, boolean nativeSeed, boolean creative, boolean lastSeed, boolean breakTool) {
        var r = rig(h, patch, nativeSeed, lastSeed ? 1 : 4);
        r.player().creative = creative;
        r.player().getAbilities().instabuild = creative;
        if (breakTool) r.tool().setDamageValue(r.tool().getMaxDamage() - 1);
        use(r);
        int planted = patch && !lastSeed && !breakTool ? 2 : 1;
        if (nativeSeed) h.assertTrue(h.getLevel().getBlockState(r.soil().above()).is(Blocks.WHEAT), "Native seeds retain normal placement");
        else {
            assertCrop(h, r.soil().above(), true);
            if (planted == 2) assertCrop(h, r.soil().east().above(), true);
        }
        h.assertTrue(r.seeds().getCount() == (creative ? 4 : (lastSeed ? 1 : 4) - planted), "Seeds are spent exactly once per committed plant");
        h.assertTrue(breakTool ? r.tool().isEmpty() : r.tool().getDamageValue() == (creative ? 0 : planted), "Tool wear respects Creative and stops at breakage");
        h.assertTrue(r.player().getCooldowns().isOnCooldown(AEItems.TROWEL.get()), "Completed planting retains its cooldown");
        if (!breakTool) h.assertTrue(GardenToolItem.recentlyUsed(r.tool(), h.getLevel()), "Completed planting retains the active trowel appearance");
        h.succeed();
    }
    @GameTest(template = "empty") public static void normalGeneticSinglePlantingKeepsCostsAndBonus(GameTestHelper h) { normal(h, false, false, false, false, false); }
    @GameTest(template = "empty") public static void normalGeneticPatchKeepsCostsAndBonus(GameTestHelper h) { normal(h, true, false, false, false, false); }
    @GameTest(template = "empty") public static void normalNativePatchKeepsCosts(GameTestHelper h) { normal(h, true, true, false, false, false); }
    @GameTest(template = "empty") public static void creativePatchKeepsSeedsAndDurability(GameTestHelper h) { normal(h, true, false, true, false, false); }
    @GameTest(template = "empty") public static void finalSeedStillGetsSinglePlantingBonus(GameTestHelper h) { normal(h, false, false, false, true, false); }
    @GameTest(template = "empty") public static void finalSeedStopsPatchAfterOnePlant(GameTestHelper h) { normal(h, true, false, false, true, false); }
    @GameTest(template = "empty") public static void finalToolDurabilityStopsPatchAfterOnePlant(GameTestHelper h) { normal(h, true, false, false, false, true); }

    @GameTest(template = "empty") public static void cancelledCenterRefundsSeedAndContinuesPatch(GameTestHelper h) {
        var r = rig(h, true, false, 4);
        Consumer<BlockEvent.EntityPlaceEvent> cancel = event -> { if (event.getPos().equals(r.soil().above())) event.setCanceled(true); };
        NeoForge.EVENT_BUS.addListener(cancel);
        try { use(r); } finally { NeoForge.EVENT_BUS.unregister(cancel); }
        h.assertTrue(h.getLevel().isEmptyBlock(r.soil().above()), "Cancelled center remains empty");
        assertCrop(h, r.soil().east().above(), true);
        h.assertTrue(r.seeds().getCount() == 3 && r.tool().getDamageValue() == 1, "Only the permitted neighbor pays costs");
        h.succeed();
    }

    @GameTest(template = "empty") public static void nestedPermissionPlantingCompletesOnlyOnce(GameTestHelper h) {
        var r = rig(h, false, false, 4);
        r.player().trigger = r.soil().above();
        r.player().hook = () -> use(r);
        use(r);
        assertCrop(h, r.soil().above(), true);
        h.assertTrue(r.seeds().getCount() == 3 && r.tool().getDamageValue() == 1, "The nested completed planting cannot be charged or repeated by the older click");
        h.succeed();
    }
}
