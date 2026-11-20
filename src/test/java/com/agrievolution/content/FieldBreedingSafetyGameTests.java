package com.agrievolution.content;

import com.agrievolution.genetics.CropGenome;
import com.agrievolution.genetics.CropSpecies;
import com.agrievolution.genetics.GenomeStorage;
import com.agrievolution.genetics.HybridLineageStorage;
import com.agrievolution.genetics.SeedVariety;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class FieldBreedingSafetyGameTests {
    private static final class HookPlayer extends Player {
        BlockPos trigger, denied;
        Runnable hook;
        boolean spectator;
        HookPlayer(ServerLevel level, BlockPos pos) {
            super(level, pos, 0, new GameProfile(UUID.randomUUID(), "field-cross"));
            setPos(Vec3.atCenterOf(pos));
            getAbilities().mayBuild = true;
        }
        @Override public boolean isCreative() { return false; }
        @Override public boolean isSpectator() { return spectator; }
        void changeWorld(Level world) { setLevel(world); }
        @Override public boolean mayUseItemAt(BlockPos pos, Direction side, ItemStack stack) {
            if (pos.equals(trigger) && hook != null) {
                var callback = hook;
                hook = null;
                callback.run();
            }
            return !pos.equals(denied) && super.mayUseItemAt(pos, side, stack);
        }
    }
    private record Rig(BlockPos first, BlockPos second, AECropBlockEntity a, AECropBlockEntity b,
                       HookPlayer player, InteractionHand hand, ItemStack book) {}
    private static final HybridLineageStorage.Lineage LINEAGE = new HybridLineageStorage.Lineage(CropSpecies.LETTUCE, CropSpecies.PEPPER);
    private static AECropBlockEntity plant(GameTestHelper h, BlockPos pos, CropGenome genes) {
        h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        h.getLevel().setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState(), 2);
        h.getLevel().setBlock(pos, AEBlocks.TOMATO_CROP.get().defaultBlockState().setValue(AgriCropBlock.AGE, 7)
                .setValue(AgriCropBlock.SUPPORTED, true).setValue(AgriCropBlock.SUPPORT_TIER, 2), 2);
        var plant = (AECropBlockEntity) h.getLevel().getBlockEntity(pos);
        plant.setGenome(genes);
        plant.setLineage(LINEAGE);
        plant.setVariety("Green line");
        plant.setSupportTicks(1800);
        return plant;
    }
    private static Rig rig(GameTestHelper h, InteractionHand hand) {
        var first = h.absolutePos(new BlockPos(4, 210, 4));
        var second = first.east(2);
        var a = plant(h, first, new CropGenome(81, 72, 63));
        var b = plant(h, second, new CropGenome(63, 82, 71));
        var player = new HookPlayer(h.getLevel(), second);
        var book = new ItemStack(AEItems.BREEDING_JOURNAL.get());
        book.set(DataComponents.CUSTOM_NAME, Component.literal("Garden breeding book"));
        CustomData.update(DataComponents.CUSTOM_DATA, book, tag -> tag.putInt("PreserveRecord", 17));
        player.setItemInHand(hand, book);
        var rig = new Rig(first, second, a, b, player, hand, book);
        player.setShiftKeyDown(true);
        use(rig, first);
        player.setShiftKeyDown(false);
        h.assertTrue(data(book).contains("AgriSelectedParent"), "The normal journal selects the first mature parent");
        return rig;
    }
    private static BlockHitResult hit(BlockPos pos) { return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false); }
    private static void use(Rig rig, BlockPos pos) {
        AEItems.BREEDING_JOURNAL.get().useOn(new UseOnContext(rig.player(), rig.hand(), hit(pos)));
    }
    private static CompoundTag data(ItemStack stack) { return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }
    private static CompoundTag saved(GameTestHelper h, AECropBlockEntity plant) {
        var tag = new CompoundTag();
        plant.saveAdditional(tag, h.getLevel().registryAccess());
        return tag;
    }
    private static int offspring(GameTestHelper h, Rig rig) {
        return rig.player().getInventory().items.stream().filter(stack -> stack.getItem() instanceof SpeciesSeedItem).mapToInt(ItemStack::getCount).sum()
                + h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(rig.second()).inflate(3)).stream()
                .filter(entity -> entity.getItem().getItem() instanceof SpeciesSeedItem).mapToInt(entity -> entity.getItem().getCount()).sum();
    }
    private static void interrupted(GameTestHelper h, boolean atParent, int mutation) {
        var rig = rig(h, atParent ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        var affected = atParent ? rig.first() : rig.second();
        rig.player().trigger = affected;
        var expectedHeld = new ItemStack[1];
        var expectedBook = new ItemStack[1];
        var expectedA = new CompoundTag[1];
        var expectedB = new CompoundTag[1];
        rig.player().hook = () -> {
            var plant = (AECropBlockEntity) h.getLevel().getBlockEntity(affected);
            switch (mutation) {
                case 0 -> rig.player().setItemInHand(rig.hand(), new ItemStack(Items.STICK, 4));
                case 1 -> rig.book().set(DataComponents.CUSTOM_NAME, Component.literal("New journal name"));
                case 2 -> CustomData.update(DataComponents.CUSTOM_DATA, rig.book(), tag -> tag.putInt("PreserveRecord", 99));
                case 3 -> rig.book().grow(1);
                case 4 -> rig.player().setItemInHand(rig.hand(), rig.book().copy());
                case 5 -> rig.player().setHealth(0);
                case 6 -> rig.player().spectator = true;
                case 7 -> rig.player().getAbilities().mayBuild = false;
                case 8 -> rig.player().changeWorld(h.getLevel().getServer().getLevel(Level.NETHER));
                case 9 -> rig.player().setShiftKeyDown(true);
                case 10 -> {
                    var before = saved(h, plant);
                    h.getLevel().removeBlockEntity(affected);
                    var replacement = new AECropBlockEntity(affected, h.getLevel().getBlockState(affected));
                    replacement.loadAdditional(before, h.getLevel().registryAccess());
                    replacement.setGenome(new CropGenome(41, 52, 63));
                    h.getLevel().setBlockEntity(replacement);
                }
                case 11 -> h.getLevel().setBlock(affected, h.getLevel().getBlockState(affected).setValue(AgriCropBlock.AGE, 3), 2);
                case 12 -> h.getLevel().setBlock(affected, h.getLevel().getBlockState(affected).setValue(AgriCropBlock.SUPPORT_TIER, 3), 2);
                case 13 -> plant.setGenome(new CropGenome(91, 62, 83));
                case 14 -> plant.setCrossed(true);
                case 15 -> CustomData.update(DataComponents.CUSTOM_DATA, rig.book(), tag -> {
                    var selected = tag.getCompound("AgriSelectedParent");
                    selected.putLong("Pos", rig.second().asLong());
                    tag.put("AgriSelectedParent", selected);
                });
                case 16 -> plant.setVariety("New line");
                case 17 -> plant.addNutrition(5);
                default -> throw new AssertionError("Unknown field breeding interruption");
            }
            expectedHeld[0] = rig.player().getItemInHand(rig.hand()).copy();
            expectedBook[0] = rig.book().copy();
            expectedA[0] = saved(h, (AECropBlockEntity) h.getLevel().getBlockEntity(rig.first()));
            expectedB[0] = saved(h, (AECropBlockEntity) h.getLevel().getBlockEntity(rig.second()));
        };
        use(rig, rig.second());
        h.assertTrue(rig.player().hook == null, "The requested clicked-parent or selected-parent permission callback ran");
        h.assertTrue(offspring(h, rig) == 0, "An interrupted field cross produces no offspring");
        h.assertTrue(ItemStack.matches(expectedHeld[0], rig.player().getItemInHand(rig.hand())) && ItemStack.matches(expectedBook[0], rig.book()),
                "The interrupted cross cannot overwrite newer journal data or the replacement held item");
        h.assertTrue(expectedA[0].equals(saved(h, (AECropBlockEntity) h.getLevel().getBlockEntity(rig.first())))
                        && expectedB[0].equals(saved(h, (AECropBlockEntity) h.getLevel().getBlockEntity(rig.second()))),
                "Both parents retain their newer or original genetics, state and breeding rights");
        h.succeed();
    }
    @GameTest(template = "empty") public static void swappedJournalAtClickedParentCancelsCross(GameTestHelper h) { interrupted(h, false, 0); }
    @GameTest(template = "empty") public static void swappedJournalAtSelectedParentCancelsCross(GameTestHelper h) { interrupted(h, true, 0); }
    @GameTest(template = "empty") public static void renamedJournalAtClickedParentCancelsCross(GameTestHelper h) { interrupted(h, false, 1); }
    @GameTest(template = "empty") public static void renamedJournalAtSelectedParentCancelsCross(GameTestHelper h) { interrupted(h, true, 1); }
    @GameTest(template = "empty") public static void editedJournalRecordsAtClickedParentRemainIntact(GameTestHelper h) { interrupted(h, false, 2); }
    @GameTest(template = "empty") public static void editedJournalRecordsAtSelectedParentRemainIntact(GameTestHelper h) { interrupted(h, true, 2); }
    @GameTest(template = "empty") public static void changedJournalCountCancelsCross(GameTestHelper h) { interrupted(h, true, 3); }
    @GameTest(template = "empty") public static void matchingJournalReplacementCancelsCross(GameTestHelper h) { interrupted(h, false, 4); }
    @GameTest(template = "empty") public static void deathDuringParentPermissionCancelsCross(GameTestHelper h) { interrupted(h, true, 5); }
    @GameTest(template = "empty") public static void spectatorDuringParentPermissionCancelsCross(GameTestHelper h) { interrupted(h, true, 6); }
    @GameTest(template = "empty") public static void revokedBuildDuringParentPermissionPreservesJournal(GameTestHelper h) { interrupted(h, true, 7); }
    @GameTest(template = "empty") public static void dimensionChangeDuringFieldCrossCancelsAction(GameTestHelper h) { interrupted(h, false, 8); }
    @GameTest(template = "empty") public static void switchingToSelectionModeCancelsPendingCross(GameTestHelper h) { interrupted(h, true, 9); }
    @GameTest(template = "empty") public static void replacedClickedParentCannotInheritFieldCross(GameTestHelper h) { interrupted(h, false, 10); }
    @GameTest(template = "empty") public static void replacedSelectedParentCannotInheritFieldCross(GameTestHelper h) { interrupted(h, true, 10); }
    @GameTest(template = "empty") public static void immatureClickedParentCannotReceiveOldCross(GameTestHelper h) { interrupted(h, false, 11); }
    @GameTest(template = "empty") public static void immatureSelectedParentCannotReceiveOldCross(GameTestHelper h) { interrupted(h, true, 11); }
    @GameTest(template = "empty") public static void changedParentSupportsCancelOldFieldCross(GameTestHelper h) { interrupted(h, true, 12); }
    @GameTest(template = "empty") public static void editedClickedParentGenesCancelOldFieldCross(GameTestHelper h) { interrupted(h, false, 13); }
    @GameTest(template = "empty") public static void editedSelectedParentGenesCancelOldFieldCross(GameTestHelper h) { interrupted(h, true, 13); }
    @GameTest(template = "empty") public static void spentClickedParentRightsCannotProduceAnotherChild(GameTestHelper h) { interrupted(h, false, 14); }
    @GameTest(template = "empty") public static void spentSelectedParentRightsCannotProduceAnotherChild(GameTestHelper h) { interrupted(h, true, 14); }
    @GameTest(template = "empty") public static void newerJournalSelectionSurvivesInterruptedCross(GameTestHelper h) { interrupted(h, true, 15); }
    @GameTest(template = "empty") public static void changedParentVarietyCancelsOldFieldCross(GameTestHelper h) { interrupted(h, false, 16); }
    @GameTest(template = "empty") public static void changedParentNutritionCancelsOldFieldCross(GameTestHelper h) { interrupted(h, true, 17); }

    @GameTest(template = "empty")
    public static void deadPlayerCannotSpendFieldBreedingRights(GameTestHelper h) {
        var rig = rig(h, InteractionHand.MAIN_HAND);
        var before = rig.book().copy();
        rig.player().setHealth(0);
        use(rig, rig.second());
        h.assertTrue(!rig.a().hasCrossed() && !rig.b().hasCrossed() && offspring(h, rig) == 0 && ItemStack.matches(before, rig.book()),
                "A dead player's old context cannot breed or change journal records");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void foreignWorldContextCannotCrossFieldParents(GameTestHelper h) {
        var rig = rig(h, InteractionHand.OFF_HAND);
        var before = rig.book().copy();
        rig.player().changeWorld(h.getLevel().getServer().getLevel(Level.NETHER));
        AEItems.BREEDING_JOURNAL.get().useOn(new UseOnContext(h.getLevel(), rig.player(), rig.hand(), rig.book(), hit(rig.second())) {});
        h.assertTrue(!rig.a().hasCrossed() && !rig.b().hasCrossed() && offspring(h, rig) == 0 && ItemStack.matches(before, rig.book()),
                "A context in another world cannot cross these plants or save their records");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void unrelatedHeldItemCannotBecomeAFieldJournal(GameTestHelper h) {
        var rig = rig(h, InteractionHand.MAIN_HAND);
        var held = new ItemStack(Items.STICK);
        rig.player().setItemInHand(rig.hand(), held);
        var before = held.copy();
        use(rig, rig.second());
        h.assertTrue(ItemStack.matches(before, held) && offspring(h, rig) == 0 && !rig.a().hasCrossed() && !rig.b().hasCrossed(),
                "An unrelated item cannot gain journal components or cross crops through an invalid dispatch");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void detachedJournalContextCannotCrossFieldParents(GameTestHelper h) {
        var rig = rig(h, InteractionHand.OFF_HAND);
        var detached = rig.book().copy();
        var before = detached.copy();
        AEItems.BREEDING_JOURNAL.get().useOn(new UseOnContext(h.getLevel(), rig.player(), rig.hand(), detached, hit(rig.second())) {});
        h.assertTrue(!rig.a().hasCrossed() && !rig.b().hasCrossed() && offspring(h, rig) == 0 && ItemStack.matches(before, detached),
                "A detached copy cannot produce a child or receive new records as the held journal");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void currentFieldCrossKeepsPlantDataAndOffspringMetadata(GameTestHelper h) {
        for (var hand : InteractionHand.values()) {
            var rig = rig(h, hand);
            var a = saved(h, rig.a());
            var b = saved(h, rig.b());
            use(rig, rig.second());
            var child = rig.player().getInventory().items.stream().filter(stack -> stack.is(AEItems.TOMATO_SEED.get())).findFirst().orElse(ItemStack.EMPTY);
            h.assertTrue(offspring(h, rig) == 1 && rig.a().hasCrossed() && rig.b().hasCrossed() && !data(rig.book()).contains("AgriSelectedParent"),
                    "A valid cross commits both parents and clears the selection before returning one seed");
            var afterA = saved(h, rig.a());
            var afterB = saved(h, rig.b());
            a.remove("AgriCrossed"); b.remove("AgriCrossed"); afterA.remove("AgriCrossed"); afterB.remove("AgriCrossed");
            h.assertTrue(a.equals(afterA) && b.equals(afterB) && rig.book().getCount() == 1 && data(rig.book()).getInt("PreserveRecord") == 17,
                    "Breeding retains growth, supports, nutrition, genes and unrelated journal records in either hand");
            h.assertTrue(LINEAGE.equals(HybridLineageStorage.fromSeed(child)) && SeedVariety.name(child).equals("Green line")
                            && GenomeStorage.fromSeed(child).growth() > 40, "Offspring retains inherited genes, ancestry and the shared variety name");
        }
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void creativeFieldCrossWithFullInventoryReturnsOneChild(GameTestHelper h) {
        var rig = rig(h, InteractionHand.OFF_HAND);
        rig.player().getAbilities().instabuild = true;
        for (int slot = 0; slot < 36; slot++) rig.player().getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
        use(rig, rig.second());
        use(rig, rig.second());
        h.assertTrue(offspring(h, rig) == 1 && rig.a().hasCrossed() && rig.b().hasCrossed() && rig.player().getOffhandItem() == rig.book(),
                "A full Creative inventory receives one child and retains the book without reusing the parents");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void ordinaryParentPermissionDenialRetainsAuthorizedRetry(GameTestHelper h) {
        var rig = rig(h, InteractionHand.MAIN_HAND);
        rig.player().denied = rig.first();
        use(rig, rig.second());
        h.assertTrue(offspring(h, rig) == 0 && !rig.a().hasCrossed() && !rig.b().hasCrossed() && data(rig.book()).contains("AgriSelectedParent"),
                "An ordinary permission denial retains both rights and the selected parent");
        rig.player().denied = null;
        use(rig, rig.second());
        h.assertTrue(offspring(h, rig) == 1 && rig.a().hasCrossed() && rig.b().hasCrossed(), "A later authorized retry still works");
        h.succeed();
    }
    private static void inspect(GameTestHelper h, boolean spectator) {
        var rig = rig(h, InteractionHand.OFF_HAND);
        rig.player().spectator = spectator;
        rig.player().getAbilities().mayBuild = false;
        use(rig, rig.second());
        h.assertTrue(data(rig.book()).getCompound("AgriSpeciesRecords").getCompound("tomato").getInt("Yield") >= 82
                        && !rig.a().hasCrossed() && !rig.b().hasCrossed() && offspring(h, rig) == 0,
                "Reading and recording remains available without permission to change field crops");
        h.succeed();
    }
    @GameTest(template = "empty") public static void adventureJournalInspectionStillRecordsGenes(GameTestHelper h) { inspect(h, false); }
    @GameTest(template = "empty") public static void spectatorJournalInspectionStillRecordsGenes(GameTestHelper h) { inspect(h, true); }
    @GameTest(template = "empty")
    public static void recursivePermissionCrossCannotPayTwiceOrRestoreSelection(GameTestHelper h) {
        var rig = rig(h, InteractionHand.MAIN_HAND);
        var expected = new ItemStack[1];
        rig.player().trigger = rig.second();
        rig.player().hook = () -> {
            use(rig, rig.second());
            expected[0] = rig.book().copy();
        };
        use(rig, rig.second());
        h.assertTrue(expected[0] != null && offspring(h, rig) == 1 && rig.a().hasCrossed() && rig.b().hasCrossed()
                        && ItemStack.matches(expected[0], rig.book()) && !data(rig.book()).contains("AgriSelectedParent"),
                "A cross completed inside permission checks stays committed and the older attempt cannot pay again or overwrite it");
        h.succeed();
    }
}
