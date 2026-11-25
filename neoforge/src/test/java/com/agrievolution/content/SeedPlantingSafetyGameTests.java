package com.agrievolution.content;

import com.agrievolution.genetics.CropGenome;
import com.agrievolution.genetics.CropSpecies;
import com.agrievolution.genetics.GenomeStorage;
import com.agrievolution.genetics.HybridLineageStorage;
import com.agrievolution.genetics.RareTrait;
import com.agrievolution.genetics.RareTraitStorage;
import com.agrievolution.genetics.SeedVariety;
import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AdventureModePredicate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class SeedPlantingSafetyGameTests {
    private static final CropGenome GENES = new CropGenome(81, 72, 63);
    private static final HybridLineageStorage.Lineage LINEAGE = new HybridLineageStorage.Lineage(CropSpecies.LETTUCE, CropSpecies.PEPPER);
    private static final class HookPlayer extends Player {
        Runnable hook;
        boolean spectator;
        HookPlayer(ServerLevel level, BlockPos pos) {
            super(level, pos, 0, new GameProfile(UUID.randomUUID(), "seed-use"));
            setPos(Vec3.atCenterOf(pos));
            getAbilities().mayBuild = true;
        }
        @Override public boolean isCreative() { return false; }
        @Override public boolean isSpectator() { return spectator; }
        void changeWorld(Level world) { setLevel(world); }
        @Override public boolean mayUseItemAt(BlockPos pos, Direction side, ItemStack stack) {
            if (hook != null) {
                var callback = hook;
                hook = null;
                callback.run();
            }
            return super.mayUseItemAt(pos, side, stack);
        }
    }
    private record Rig(BlockPos soil, HookPlayer player) { BlockPos crop() { return soil.above(); } }
    private static Rig rig(GameTestHelper h) {
        var soil = h.absolutePos(new BlockPos(4, 210, 4));
        h.getLevel().setBlock(soil.above(), Blocks.AIR.defaultBlockState(), 2);
        h.getLevel().setBlock(soil, Blocks.FARMLAND.defaultBlockState(), 2);
        return new Rig(soil, new HookPlayer(h.getLevel(), soil));
    }
    private static ItemStack source(CropSpecies species) {
        var stack = new ItemStack(AEItems.seedFor(species), 3);
        GenomeStorage.putOnSeed(stack, GENES);
        RareTraitStorage.putOnSeed(stack, RareTrait.VIGOROUS);
        HybridLineageStorage.putOnSeed(stack, LINEAGE);
        SeedVariety.apply(stack, "Parent line");
        return stack;
    }
    private static BlockHitResult hit(BlockPos pos) { return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false); }
    private static void use(Rig rig, InteractionHand hand) {
        AEItems.TOMATO_SEED.get().useOn(new UseOnContext(rig.player(), hand, hit(rig.soil())));
    }
    private static CompoundTag saved(GameTestHelper h, AECropBlockEntity plant) {
        var tag = new CompoundTag();
        plant.saveAdditional(tag, h.getLevel().registryAccess());
        return tag;
    }
    private static void traits(GameTestHelper h, BlockPos pos, CropSpecies species) {
        h.assertTrue(h.getLevel().getBlockState(pos).getBlock() == ((SpeciesSeedItem) AEItems.seedFor(species)).cropBlock(), "The seed plants its registered species");
        var plant = (AECropBlockEntity) h.getLevel().getBlockEntity(pos);
        h.assertTrue(plant != null && plant.genome().equals(GENES) && plant.rareTrait() == RareTrait.VIGOROUS
                        && LINEAGE.equals(plant.lineage()) && plant.variety().equals("Parent line"),
                "The planted crop retains one consistent set of seed genes, rare trait, ancestry and variety");
    }
    private static void interrupted(GameTestHelper h, int mutation) {
        var rig = rig(h);
        var hand = mutation % 2 == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        var held = source(CropSpecies.TOMATO);
        rig.player().setItemInHand(hand, held);
        var expectedHeld = new ItemStack[1];
        var expectedSoil = new BlockState[1];
        var expectedTarget = new BlockState[1];
        var expectedPlant = new CompoundTag[1];
        rig.player().hook = () -> {
            switch (mutation) {
                case 0 -> rig.player().setItemInHand(hand, new ItemStack(Items.STICK, 4));
                case 1 -> rig.player().setItemInHand(hand, held.copy());
                case 2 -> SeedVariety.apply(held, "Edited line");
                case 3 -> GenomeStorage.putOnSeed(held, new CropGenome(11, 22, 33));
                case 4 -> held.grow(1);
                case 5 -> RareTraitStorage.putOnSeed(held, RareTrait.TENACIOUS);
                case 6 -> HybridLineageStorage.putOnSeed(held, new HybridLineageStorage.Lineage(CropSpecies.CORN, CropSpecies.PEAS));
                case 7 -> rig.player().setHealth(0);
                case 8 -> rig.player().spectator = true;
                case 9 -> rig.player().changeWorld(h.getLevel().getServer().getLevel(Level.NETHER));
                case 10 -> rig.player().getAbilities().mayBuild = false;
                case 11 -> held.setCount(0);
                case 12 -> h.getLevel().setBlock(rig.crop(), Blocks.STONE.defaultBlockState(), 2);
                case 13 -> {
                    h.getLevel().setBlock(rig.crop(), AEBlocks.LETTUCE_CROP.get().defaultBlockState().setValue(AgriCropBlock.AGE, 4), 2);
                    ((AECropBlockEntity) h.getLevel().getBlockEntity(rig.crop())).setGenome(new CropGenome(41, 52, 63));
                }
                case 14 -> h.getLevel().setBlock(rig.soil(), Blocks.STONE.defaultBlockState(), 2);
                case 15 -> h.getLevel().setBlock(rig.soil(), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7), 2);
                case 16 -> h.getLevel().setBlock(rig.soil(), AEBlocks.CULTIVATED_FARMLAND.get().defaultBlockState(), 2);
                default -> throw new AssertionError("Unknown planting interruption");
            }
            expectedHeld[0] = rig.player().getItemInHand(hand).copy();
            expectedSoil[0] = h.getLevel().getBlockState(rig.soil());
            expectedTarget[0] = h.getLevel().getBlockState(rig.crop());
            if (h.getLevel().getBlockEntity(rig.crop()) instanceof AECropBlockEntity plant) expectedPlant[0] = saved(h, plant);
        };
        use(rig, hand);
        h.assertTrue(rig.player().hook == null, "The item-use permission callback ran");
        h.assertTrue(ItemStack.matches(expectedHeld[0], rig.player().getItemInHand(hand)), "Interrupted planting preserves the newer held seed or replacement item");
        h.assertTrue(h.getLevel().getBlockState(rig.soil()) == expectedSoil[0] && h.getLevel().getBlockState(rig.crop()) == expectedTarget[0],
                "Interrupted planting preserves newer soil, occupied blocks and replacement crops");
        if (expectedPlant[0] != null) h.assertTrue(expectedPlant[0].equals(saved(h, (AECropBlockEntity) h.getLevel().getBlockEntity(rig.crop()))),
                "A replacement plant keeps its saved data");
        h.succeed();
    }
    @GameTest(template = "empty") public static void swappedSeedCancelsPlanting(GameTestHelper h) { interrupted(h, 0); }
    @GameTest(template = "empty") public static void matchingSeedReplacementCancelsPlanting(GameTestHelper h) { interrupted(h, 1); }
    @GameTest(template = "empty") public static void renamedSeedCancelsPlanting(GameTestHelper h) { interrupted(h, 2); }
    @GameTest(template = "empty") public static void changedSeedGenesCancelPlanting(GameTestHelper h) { interrupted(h, 3); }
    @GameTest(template = "empty") public static void changedSeedCountCancelsPlanting(GameTestHelper h) { interrupted(h, 4); }
    @GameTest(template = "empty") public static void changedSeedTraitCancelsPlanting(GameTestHelper h) { interrupted(h, 5); }
    @GameTest(template = "empty") public static void changedSeedAncestryCancelsPlanting(GameTestHelper h) { interrupted(h, 6); }
    @GameTest(template = "empty") public static void deathDuringSeedPermissionCancelsPlanting(GameTestHelper h) { interrupted(h, 7); }
    @GameTest(template = "empty") public static void spectatorDuringSeedPermissionCancelsPlanting(GameTestHelper h) { interrupted(h, 8); }
    @GameTest(template = "empty") public static void dimensionChangeDuringSeedPermissionCancelsPlanting(GameTestHelper h) { interrupted(h, 9); }
    @GameTest(template = "empty") public static void revokedBuildDuringSeedPermissionKeepsPlotEmpty(GameTestHelper h) { interrupted(h, 10); }
    @GameTest(template = "empty") public static void emptiedSeedDuringPermissionCannotCreateFreeCrop(GameTestHelper h) { interrupted(h, 11); }
    @GameTest(template = "empty") public static void occupiedTargetDuringSeedPermissionIsPreserved(GameTestHelper h) { interrupted(h, 12); }
    @GameTest(template = "empty") public static void replacementCropDuringSeedPermissionIsPreserved(GameTestHelper h) { interrupted(h, 13); }
    @GameTest(template = "empty") public static void removedSoilDuringSeedPermissionCancelsPlanting(GameTestHelper h) { interrupted(h, 14); }
    @GameTest(template = "empty") public static void changedSoilMoistureDuringSeedPermissionCancelsPlanting(GameTestHelper h) { interrupted(h, 15); }
    @GameTest(template = "empty") public static void upgradedSoilDuringSeedPermissionCancelsOldPlanting(GameTestHelper h) { interrupted(h, 16); }

    @GameTest(template = "empty")
    public static void deadPlayerCannotPlantCustomSeed(GameTestHelper h) {
        var rig = rig(h);
        var held = source(CropSpecies.TOMATO);
        rig.player().setItemInHand(InteractionHand.MAIN_HAND, held);
        rig.player().setHealth(0);
        use(rig, InteractionHand.MAIN_HAND);
        h.assertTrue(h.getLevel().isEmptyBlock(rig.crop()) && held.getCount() == 3, "Dead players cannot place crops or spend seeds");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void foreignWorldSeedContextCannotPlantCrop(GameTestHelper h) {
        var rig = rig(h);
        var held = source(CropSpecies.TOMATO);
        rig.player().setItemInHand(InteractionHand.OFF_HAND, held);
        rig.player().changeWorld(h.getLevel().getServer().getLevel(Level.NETHER));
        AEItems.TOMATO_SEED.get().useOn(new UseOnContext(h.getLevel(), rig.player(), InteractionHand.OFF_HAND, held, hit(rig.soil())) {});
        h.assertTrue(h.getLevel().isEmptyBlock(rig.crop()) && held.getCount() == 3, "A context in another world cannot plant in this plot");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void wrongSeedSpeciesCannotDispatchAnotherCrop(GameTestHelper h) {
        var rig = rig(h);
        var held = source(CropSpecies.LETTUCE);
        rig.player().setItemInHand(InteractionHand.MAIN_HAND, held);
        use(rig, InteractionHand.MAIN_HAND);
        h.assertTrue(h.getLevel().isEmptyBlock(rig.crop()) && held.getCount() == 3, "A lettuce stack cannot be spent by a tomato seed dispatch");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void detachedSeedContextCannotPlantCrop(GameTestHelper h) {
        var rig = rig(h);
        var held = source(CropSpecies.TOMATO);
        rig.player().setItemInHand(InteractionHand.MAIN_HAND, held);
        var detached = held.copy();
        AEItems.TOMATO_SEED.get().useOn(new UseOnContext(h.getLevel(), rig.player(), InteractionHand.MAIN_HAND, detached, hit(rig.soil())) {});
        h.assertTrue(h.getLevel().isEmptyBlock(rig.crop()) && held.getCount() == 3 && detached.getCount() == 3, "A detached copy cannot create a crop outside the held source stack");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void emptyPlayerlessSeedContextCannotCreateFreeCrop(GameTestHelper h) {
        var rig = rig(h);
        AEItems.TOMATO_SEED.get().useOn(new UseOnContext(h.getLevel(), null, InteractionHand.MAIN_HAND, ItemStack.EMPTY, hit(rig.soil())) {});
        h.assertTrue(h.getLevel().isEmptyBlock(rig.crop()), "An empty automation context cannot create a free crop");
        h.succeed();
    }

    private static void normal(GameTestHelper h, boolean creative) {
        for (var hand : InteractionHand.values()) {
            var rig = rig(h);
            var held = source(CropSpecies.TOMATO);
            rig.player().getAbilities().instabuild = creative;
            rig.player().setItemInHand(hand, held);
            use(rig, hand);
            traits(h, rig.crop(), CropSpecies.TOMATO);
            h.assertTrue(held.getCount() == (creative ? 3 : 2), "Normal planting retains its exact Survival or Creative cost in either hand");
        }
        h.succeed();
    }
    @GameTest(template = "empty") public static void currentSeedPlantingRetainsTraitsAndCostsInBothHands(GameTestHelper h) { normal(h, false); }
    @GameTest(template = "empty") public static void creativeSeedPlantingRetainsTraitsWithoutSpendingSeeds(GameTestHelper h) { normal(h, true); }
    @GameTest(template = "empty")
    public static void validPlayerlessSeedPlantingRetainsAutomationSupport(GameTestHelper h) {
        var rig = rig(h);
        var held = source(CropSpecies.TOMATO);
        AEItems.TOMATO_SEED.get().useOn(new UseOnContext(h.getLevel(), null, InteractionHand.MAIN_HAND, held, hit(rig.soil())) {});
        traits(h, rig.crop(), CropSpecies.TOMATO);
        h.assertTrue(held.getCount() == 2, "A valid automation context still spends exactly one supplied seed");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void adventureSeedCanPlaceOnPermissionStillWorks(GameTestHelper h) {
        var rig = rig(h);
        var held = source(CropSpecies.TOMATO);
        held.set(DataComponents.CAN_PLACE_ON, new AdventureModePredicate(List.of(BlockPredicate.Builder.block().of(Blocks.FARMLAND).build()), true));
        rig.player().getAbilities().mayBuild = false;
        rig.player().setItemInHand(InteractionHand.MAIN_HAND, held);
        use(rig, InteractionHand.MAIN_HAND);
        traits(h, rig.crop(), CropSpecies.TOMATO);
        h.assertTrue(held.getCount() == 2, "Vanilla CanPlaceOn permission remains valid for Adventure planting");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void everyRegisteredSpeciesStillPlantsItsOwnCrop(GameTestHelper h) {
        var rig = rig(h);
        for (var species : CropSpecies.values()) {
            h.getLevel().setBlock(rig.crop(), Blocks.AIR.defaultBlockState(), 2);
            var held = source(species);
            rig.player().setItemInHand(InteractionHand.OFF_HAND, held);
            held.getItem().useOn(new UseOnContext(rig.player(), InteractionHand.OFF_HAND, hit(rig.soil())));
            traits(h, rig.crop(), species);
            h.assertTrue(held.getCount() == 2, "Every registered species retains its one-seed planting cost");
        }
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void customSeedsStillAcceptEveryFarmlandTier(GameTestHelper h) {
        var rig = rig(h);
        for (var soil : List.of(Blocks.FARMLAND, AEBlocks.CULTIVATED_FARMLAND.get(), AEBlocks.NUTRIENT_FARMLAND.get(),
                AEBlocks.MYTHIC_FARMLAND.get(), AEBlocks.ARCANE_FARMLAND.get(), AEBlocks.CELESTIAL_FARMLAND.get())) {
            h.getLevel().setBlock(rig.crop(), Blocks.AIR.defaultBlockState(), 2);
            h.getLevel().setBlock(rig.soil(), soil.defaultBlockState(), 2);
            var held = source(CropSpecies.TOMATO);
            rig.player().setItemInHand(InteractionHand.MAIN_HAND, held);
            use(rig, InteractionHand.MAIN_HAND);
            traits(h, rig.crop(), CropSpecies.TOMATO);
            h.assertTrue(held.getCount() == 2, "Every existing farmland tier still accepts the normal seed");
        }
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void cancelledCustomSeedPlacementRestoresPlotAndSeedData(GameTestHelper h) {
        var rig = rig(h);
        var held = source(CropSpecies.TOMATO);
        rig.player().setItemInHand(InteractionHand.MAIN_HAND, held);
        var before = held.copy();
        boolean[] observed = {false};
        Consumer<BlockEvent.EntityPlaceEvent> listener = event -> {
            if (!event.getPos().equals(rig.crop())) return;
            observed[0] = true;
            event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try { held.useOn(new UseOnContext(rig.player(), InteractionHand.MAIN_HAND, hit(rig.soil()))); }
        finally { NeoForge.EVENT_BUS.unregister(listener); }
        h.assertTrue(observed[0] && h.getLevel().isEmptyBlock(rig.crop()) && ItemStack.matches(before, held),
                "NeoForge placement cancellation restores air and the exact genetic seed stack");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void invalidSoilFaceAndOccupiedPlotsKeepSeeds(GameTestHelper h) {
        var rig = rig(h);
        var held = source(CropSpecies.TOMATO);
        rig.player().setItemInHand(InteractionHand.MAIN_HAND, held);
        h.getLevel().setBlock(rig.crop(), Blocks.STONE.defaultBlockState(), 2);
        use(rig, InteractionHand.MAIN_HAND);
        h.assertTrue(h.getLevel().getBlockState(rig.crop()).is(Blocks.STONE) && held.getCount() == 3, "An occupied plot is unchanged");
        h.getLevel().setBlock(rig.crop(), Blocks.AIR.defaultBlockState(), 2);
        h.getLevel().setBlock(rig.soil(), Blocks.DIRT.defaultBlockState(), 2);
        use(rig, InteractionHand.MAIN_HAND);
        h.getLevel().setBlock(rig.soil(), Blocks.FARMLAND.defaultBlockState(), 2);
        held.getItem().useOn(new UseOnContext(rig.player(), InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(rig.soil()), Direction.NORTH, rig.soil(), false)));
        h.assertTrue(held.getCount() == 3 && h.getLevel().isEmptyBlock(rig.crop()), "Invalid soil and non-top faces keep the seeds");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void plantingAboveBuildHeightKeepsSeed(GameTestHelper h) {
        var rig = rig(h);
        var top = new BlockPos(rig.soil().getX(), h.getLevel().getMaxBuildHeight() - 1, rig.soil().getZ());
        h.getLevel().setBlock(top, Blocks.FARMLAND.defaultBlockState(), 2);
        var held = source(CropSpecies.TOMATO);
        rig.player().setItemInHand(InteractionHand.MAIN_HAND, held);
        held.getItem().useOn(new UseOnContext(rig.player(), InteractionHand.MAIN_HAND, hit(top)));
        h.assertTrue(held.getCount() == 3 && h.getLevel().isEmptyBlock(top.above()), "An out-of-height placement never spends the seed");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void staleSeedContextDoesNotLoadFarawayChunk(GameTestHelper h) {
        var rig = rig(h);
        var far = h.absolutePos(new BlockPos(32768, 210, 32768));
        while (h.getLevel().hasChunkAt(far)) far = far.east(512);
        var held = source(CropSpecies.TOMATO);
        rig.player().setItemInHand(InteractionHand.MAIN_HAND, held);
        held.getItem().useOn(new UseOnContext(rig.player(), InteractionHand.MAIN_HAND, hit(far)));
        h.assertTrue(!h.getLevel().hasChunkAt(far) && held.getCount() == 3, "A stale seed context does not load another chunk to inspect its soil");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void plantingNotificationsCannotMixTwoSeedGenotypes(GameTestHelper h) {
        var rig = rig(h);
        var held = source(CropSpecies.TOMATO);
        rig.player().setItemInHand(InteractionHand.MAIN_HAND, held);
        boolean[] observed = {false};
        Consumer<BlockEvent.NeighborNotifyEvent> listener = event -> {
            if (observed[0] || !event.getPos().equals(rig.crop())) return;
            observed[0] = true;
            GenomeStorage.putOnSeed(held, new CropGenome(11, 22, 33));
            RareTraitStorage.putOnSeed(held, RareTrait.TENACIOUS);
            HybridLineageStorage.putOnSeed(held, new HybridLineageStorage.Lineage(CropSpecies.CORN, CropSpecies.PEAS));
            SeedVariety.apply(held, "New line");
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try { use(rig, InteractionHand.MAIN_HAND); }
        finally { NeoForge.EVENT_BUS.unregister(listener); }
        h.assertTrue(observed[0] && held.getCount() == 2 && GenomeStorage.fromSeed(held).equals(new CropGenome(11, 22, 33)),
                "The notification ran and retained the newer source metadata with the normal cost");
        traits(h, rig.crop(), CropSpecies.TOMATO);
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void plantingNotificationsDoNotRewriteAReplacementSpecies(GameTestHelper h) {
        var rig = rig(h);
        var held = source(CropSpecies.TOMATO);
        rig.player().setItemInHand(InteractionHand.MAIN_HAND, held);
        var expected = new CompoundTag[1];
        Consumer<BlockEvent.NeighborNotifyEvent> listener = event -> {
            if (expected[0] != null || !event.getPos().equals(rig.crop())) return;
            h.getLevel().setBlock(rig.crop(), AEBlocks.LETTUCE_CROP.get().defaultBlockState().setValue(AgriCropBlock.AGE, 4), 18);
            var replacement = (AECropBlockEntity) h.getLevel().getBlockEntity(rig.crop());
            replacement.setGenome(new CropGenome(41, 52, 63));
            expected[0] = saved(h, replacement);
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try { use(rig, InteractionHand.MAIN_HAND); }
        finally { NeoForge.EVENT_BUS.unregister(listener); }
        h.assertTrue(expected[0] != null && h.getLevel().getBlockState(rig.crop()).is(AEBlocks.LETTUCE_CROP.get())
                        && expected[0].equals(saved(h, (AECropBlockEntity) h.getLevel().getBlockEntity(rig.crop()))) && held.getCount() == 2,
                "A notification's replacement crop keeps its own genetics and state after the original placement");
        h.succeed();
    }
}
