package com.agrievolution.content;

import com.agrievolution.genetics.CropGenome;
import com.agrievolution.genetics.CropSpecies;
import com.agrievolution.genetics.HybridLineageStorage;
import com.agrievolution.genetics.RareTrait;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class ClippingSafetyGameTests {
    private static final CropGenome GENES = new CropGenome(83, 72, 61);
    private static final class HookPlayer extends Player {
        Runnable hook;
        boolean spectator;
        HookPlayer(ServerLevel level, BlockPos pos) {
            super(level, pos, 0, new GameProfile(UUID.randomUUID(), "clippers-use"));
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
    private record Rig(BlockPos pos, AECropBlockEntity plant, HookPlayer player) {}
    private static Rig rig(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 210, 4));
        h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        h.getLevel().setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState(), 2);
        h.getLevel().setBlock(pos, AEBlocks.TOMATO_CROP.get().defaultBlockState().setValue(AgriCropBlock.AGE, 7)
                .setValue(AgriCropBlock.SUPPORTED, true).setValue(AgriCropBlock.SUPPORT_TIER, 2).setValue(AgriCropBlock.WEEDY, true), 2);
        var plant = (AECropBlockEntity) h.getLevel().getBlockEntity(pos);
        plant.setGenome(GENES);
        plant.setRareTrait(RareTrait.VIGOROUS);
        plant.setVariety("Kitchen Plot");
        plant.setLineage(new HybridLineageStorage.Lineage(CropSpecies.WILD, CropSpecies.AZURE_FLAX));
        plant.setSupportTicks(1800);
        return new Rig(pos, plant, new HookPlayer(h.getLevel(), pos));
    }
    private static ItemStack tool() {
        var stack = new ItemStack(AEItems.GENETIC_CLIPPERS.get());
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Field Clippers"));
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("ClippingUse", 7));
        return stack;
    }
    private static BlockHitResult hit(BlockPos pos) { return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false); }
    private static InteractionResult use(Rig rig, InteractionHand hand) {
        return AEItems.GENETIC_CLIPPERS.get().useOn(new UseOnContext(rig.player(), hand, hit(rig.pos())));
    }
    private static CompoundTag saved(GameTestHelper h, AECropBlockEntity plant) {
        var tag = new CompoundTag();
        plant.saveAdditional(tag, h.getLevel().registryAccess());
        return tag;
    }
    private static void noSample(GameTestHelper h, Rig rig) {
        h.assertTrue(rig.player().getInventory().countItem(AEItems.GENETIC_SAMPLE.get()) == 0
                        && h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(rig.pos()).inflate(3)).stream()
                        .noneMatch(entity -> entity.getItem().is(AEItems.GENETIC_SAMPLE.get())),
                "A rejected clipping produces no stored or dropped sample");
    }
    private static ItemStack sample(GameTestHelper h, Rig rig) {
        var samples = rig.player().getInventory().items.stream().filter(stack -> stack.is(AEItems.GENETIC_SAMPLE.get())).toList();
        h.assertTrue(samples.stream().mapToInt(ItemStack::getCount).sum() == 1, "Exactly one genetic sample is returned");
        return samples.getFirst();
    }
    private static void accurate(GameTestHelper h, ItemStack sample, CropGenome genes, RareTrait trait, String variety) {
        var data = sample.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound("AgriEvolutionGeneticSample");
        h.assertTrue(GeneticSampleItem.isValid(sample) && GeneticSampleItem.genome(sample).equals(genes)
                        && GeneticSampleItem.rareTrait(sample) == trait && GeneticSampleItem.speciesName(sample).equals(CropSpecies.TOMATO.displayName())
                        && data.getString("Variety").equals(variety), "The sample keeps the actual crop species, genes, rare trait and variety");
    }

    private static void interrupted(GameTestHelper h, int mutation) {
        var rig = rig(h);
        var hand = mutation % 2 == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        var held = tool();
        rig.player().setItemInHand(hand, held);
        var expectedHeld = new ItemStack[]{held.copy()};
        var expectedState = new BlockState[1];
        var expectedPlant = new CompoundTag[1];
        var expectedOriginal = new CompoundTag[1];
        rig.player().hook = () -> {
            switch (mutation) {
                case 0 -> rig.player().setItemInHand(hand, new ItemStack(Items.STICK, 4));
                case 1 -> held.set(DataComponents.CUSTOM_NAME, Component.literal("Changed field tool"));
                case 2 -> CustomData.update(DataComponents.CUSTOM_DATA, held, tag -> tag.putInt("ClippingUse", 8));
                case 3 -> held.grow(1);
                case 4 -> rig.player().setItemInHand(hand, held.copy());
                case 5 -> rig.player().setHealth(0);
                case 6 -> rig.player().spectator = true;
                case 7 -> rig.player().getAbilities().mayBuild = false;
                case 8 -> rig.player().changeWorld(h.getLevel().getServer().getLevel(Level.NETHER));
                case 9 -> h.getLevel().setBlock(rig.pos(), h.getLevel().getBlockState(rig.pos()).setValue(AgriCropBlock.AGE, 3), 2);
                case 10 -> h.getLevel().setBlock(rig.pos(), h.getLevel().getBlockState(rig.pos()).setValue(AgriCropBlock.WEEDY, false), 2);
                case 11 -> {
                    var before = saved(h, rig.plant());
                    h.getLevel().removeBlockEntity(rig.pos());
                    var replacement = new AECropBlockEntity(rig.pos(), h.getLevel().getBlockState(rig.pos()));
                    replacement.loadAdditional(before, h.getLevel().registryAccess());
                    replacement.setGenome(new CropGenome(41, 52, 63));
                    h.getLevel().setBlockEntity(replacement);
                }
                case 12 -> h.getLevel().setBlock(rig.pos(), AEBlocks.LETTUCE_CROP.get().defaultBlockState().setValue(AgriCropBlock.AGE, 7), 2);
                case 13 -> {
                    var state = h.getLevel().getBlockState(rig.pos());
                    h.getLevel().setBlock(rig.pos(), Blocks.AIR.defaultBlockState(), 2);
                    h.getLevel().setBlock(rig.pos(), state, 2);
                }
                default -> throw new AssertionError("Unknown clipping interruption");
            }
            expectedHeld[0] = rig.player().getItemInHand(hand).copy();
            expectedState[0] = h.getLevel().getBlockState(rig.pos());
            expectedPlant[0] = saved(h, (AECropBlockEntity) h.getLevel().getBlockEntity(rig.pos()));
            expectedOriginal[0] = saved(h, rig.plant());
        };
        use(rig, hand);
        h.assertTrue(rig.player().hook == null, "The access callback ran");
        noSample(h, rig);
        h.assertTrue(ItemStack.matches(expectedHeld[0], rig.player().getItemInHand(hand)) && h.getLevel().getBlockState(rig.pos()) == expectedState[0],
                "The cancelled clipping retains the newer tool and block state");
        h.assertTrue(expectedPlant[0].equals(saved(h, (AECropBlockEntity) h.getLevel().getBlockEntity(rig.pos())))
                        && expectedOriginal[0].equals(saved(h, rig.plant())),
                "The cancelled clipping preserves both the original and replacement plants' sample credits and saved data");
        h.succeed();
    }
    @GameTest(template = "empty") public static void swappedClippersCancelClipping(GameTestHelper h) { interrupted(h, 0); }
    @GameTest(template = "empty") public static void renamedClippersCancelClipping(GameTestHelper h) { interrupted(h, 1); }
    @GameTest(template = "empty") public static void changedClippersDataCancelsClipping(GameTestHelper h) { interrupted(h, 2); }
    @GameTest(template = "empty") public static void changedClippersCountCancelsClipping(GameTestHelper h) { interrupted(h, 3); }
    @GameTest(template = "empty") public static void matchingClippersReplacementCancelsClipping(GameTestHelper h) { interrupted(h, 4); }
    @GameTest(template = "empty") public static void deathDuringClippingCancelsSample(GameTestHelper h) { interrupted(h, 5); }
    @GameTest(template = "empty") public static void spectatorDuringClippingCancelsSample(GameTestHelper h) { interrupted(h, 6); }
    @GameTest(template = "empty") public static void revokedBuildDuringClippingCancelsSample(GameTestHelper h) { interrupted(h, 7); }
    @GameTest(template = "empty") public static void dimensionChangeDuringClippingCancelsSample(GameTestHelper h) { interrupted(h, 8); }
    @GameTest(template = "empty") public static void youngerGrowthDuringClippingKeepsSampleCredit(GameTestHelper h) { interrupted(h, 9); }
    @GameTest(template = "empty") public static void newerCropStateDuringClippingKeepsSampleCredit(GameTestHelper h) { interrupted(h, 10); }
    @GameTest(template = "empty") public static void replacementCropCannotInheritPendingClipping(GameTestHelper h) { interrupted(h, 11); }
    @GameTest(template = "empty") public static void changedCropSpeciesCannotReceiveOldClipping(GameTestHelper h) { interrupted(h, 12); }
    @GameTest(template = "empty") public static void replantedCropCannotReceiveOldClipping(GameTestHelper h) { interrupted(h, 13); }

    @GameTest(template = "empty")
    public static void deadPlayerCannotClipCrop(GameTestHelper h) {
        var rig = rig(h);
        rig.player().setItemInHand(InteractionHand.MAIN_HAND, tool());
        rig.player().setHealth(0);
        use(rig, InteractionHand.MAIN_HAND);
        noSample(h, rig);
        h.assertFalse(rig.plant().hasBeenClipped(), "Dead players cannot spend a plant's clipping credit");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void foreignWorldPlayerCannotClipCrop(GameTestHelper h) {
        var rig = rig(h);
        var held = tool();
        rig.player().setItemInHand(InteractionHand.OFF_HAND, held);
        rig.player().changeWorld(h.getLevel().getServer().getLevel(Level.NETHER));
        AEItems.GENETIC_CLIPPERS.get().useOn(new UseOnContext(h.getLevel(), rig.player(), InteractionHand.OFF_HAND, held, hit(rig.pos())) {});
        noSample(h, rig);
        h.assertFalse(rig.plant().hasBeenClipped(), "A context in another world cannot spend this plant's credit");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void unrelatedToolCannotDispatchGeneticClipping(GameTestHelper h) {
        var rig = rig(h);
        rig.player().setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        use(rig, InteractionHand.MAIN_HAND);
        noSample(h, rig);
        h.assertFalse(rig.plant().hasBeenClipped(), "A direct dispatch with another item cannot take a sample");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void detachedClippersContextCannotTakeSample(GameTestHelper h) {
        var rig = rig(h);
        var held = tool();
        rig.player().setItemInHand(InteractionHand.MAIN_HAND, held);
        AEItems.GENETIC_CLIPPERS.get().useOn(new UseOnContext(h.getLevel(), rig.player(), InteractionHand.MAIN_HAND, held.copy(), hit(rig.pos())) {});
        noSample(h, rig);
        h.assertFalse(rig.plant().hasBeenClipped(), "A detached copy cannot act as the tool in the used hand");
        h.succeed();
    }

    private static void normalClipping(GameTestHelper h, boolean creative) {
        for (var hand : InteractionHand.values()) {
            var rig = rig(h);
            rig.player().getAbilities().instabuild = creative;
            var held = tool();
            rig.player().setItemInHand(hand, held);
            var beforeHeld = held.copy();
            var before = saved(h, rig.plant());
            var state = h.getLevel().getBlockState(rig.pos());
            use(rig, hand);
            accurate(h, sample(h, rig), GENES, RareTrait.VIGOROUS, "Kitchen Plot");
            var after = saved(h, rig.plant());
            before.remove("AgriGeneticClipped");
            after.remove("AgriGeneticClipped");
            h.assertTrue(rig.plant().hasBeenClipped() && before.equals(after) && h.getLevel().getBlockState(rig.pos()) == state
                            && ItemStack.matches(beforeHeld, rig.player().getItemInHand(hand)),
                    "Clipping retains growth, weeds, supports, ancestry, nutrition and the named tool in either hand");
        }
        h.succeed();
    }
    @GameTest(template = "empty") public static void survivalClippingPreservesPlantAndTool(GameTestHelper h) { normalClipping(h, false); }
    @GameTest(template = "empty") public static void creativeClippingPreservesPlantAndTool(GameTestHelper h) { normalClipping(h, true); }
    @GameTest(template = "empty")
    public static void immatureCropRetainsClippingCredit(GameTestHelper h) {
        var rig = rig(h);
        h.getLevel().setBlock(rig.pos(), h.getLevel().getBlockState(rig.pos()).setValue(AgriCropBlock.AGE, 3), 2);
        rig.player().setItemInHand(InteractionHand.OFF_HAND, tool());
        use(rig, InteractionHand.OFF_HAND);
        noSample(h, rig);
        h.assertFalse(rig.plant().hasBeenClipped(), "A normally immature plant keeps its unused sample");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void repeatedClippingReturnsOnlyOneSample(GameTestHelper h) {
        var rig = rig(h);
        rig.player().setItemInHand(InteractionHand.OFF_HAND, tool());
        use(rig, InteractionHand.OFF_HAND);
        var after = saved(h, rig.plant());
        use(rig, InteractionHand.OFF_HAND);
        accurate(h, sample(h, rig), GENES, RareTrait.VIGOROUS, "Kitchen Plot");
        h.assertTrue(after.equals(saved(h, rig.plant())), "An already clipped crop keeps its saved data and yields no second sample");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void playerlessClippingRetainsPlant(GameTestHelper h) {
        var rig = rig(h);
        AEItems.GENETIC_CLIPPERS.get().useOn(new UseOnContext(h.getLevel(), null, InteractionHand.MAIN_HAND, tool(), hit(rig.pos())) {});
        noSample(h, rig);
        h.assertFalse(rig.plant().hasBeenClipped(), "Playerless use cannot spend clipping credit");
        h.succeed();
    }
    private static void fullInventory(Rig rig) {
        for (int slot = 0; slot < 36; slot++) rig.player().getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
        rig.player().setItemInHand(InteractionHand.OFF_HAND, tool());
    }
    private static void droppedSample(GameTestHelper h, Rig rig) {
        var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(rig.pos()).inflate(3)).stream()
                .map(ItemEntity::getItem).filter(stack -> stack.is(AEItems.GENETIC_SAMPLE.get())).toList();
        h.assertTrue(drops.stream().mapToInt(ItemStack::getCount).sum() == 1, "A full inventory receives exactly one dropped sample");
        accurate(h, drops.getFirst(), GENES, RareTrait.VIGOROUS, "Kitchen Plot");
    }
    @GameTest(template = "empty")
    public static void fullSurvivalInventoryReceivesAccurateClipping(GameTestHelper h) {
        var rig = rig(h);
        fullInventory(rig);
        use(rig, InteractionHand.OFF_HAND);
        use(rig, InteractionHand.OFF_HAND);
        droppedSample(h, rig);
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void clippingRewardCallbacksCannotDuplicateSample(GameTestHelper h) {
        var rig = rig(h);
        fullInventory(rig);
        boolean[] observed = {false};
        Consumer<EntityJoinLevelEvent> listener = event -> {
            if (observed[0] || !(event.getEntity() instanceof ItemEntity item) || !item.getItem().is(AEItems.GENETIC_SAMPLE.get())) return;
            observed[0] = true;
            h.assertTrue(rig.plant().hasBeenClipped(), "The spent credit is recorded before giving the sample");
            use(rig, InteractionHand.OFF_HAND);
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try { use(rig, InteractionHand.OFF_HAND); }
        finally { NeoForge.EVENT_BUS.unregister(listener); }
        h.assertTrue(observed[0], "The sample delivery callback ran");
        droppedSample(h, rig);
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void currentPlantGeneticEditsRemainAccurateInClipping(GameTestHelper h) {
        var rig = rig(h);
        rig.player().setItemInHand(InteractionHand.MAIN_HAND, tool());
        var updated = new CropGenome(91, 82, 73);
        rig.player().hook = () -> {
            rig.plant().setGenome(updated);
            rig.plant().setRareTrait(RareTrait.TENACIOUS);
            rig.plant().setVariety("Grafted line");
        };
        use(rig, InteractionHand.MAIN_HAND);
        accurate(h, sample(h, rig), updated, RareTrait.TENACIOUS, "Grafted line");
        h.assertTrue(rig.plant().genome().equals(updated) && rig.plant().hasBeenClipped(), "Edits on the same mature plant remain intact and the sample reflects them");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void unrelatedBlockKeepsClippersAndGivesNoSample(GameTestHelper h) {
        var rig = rig(h);
        h.getLevel().setBlock(rig.pos(), Blocks.STONE.defaultBlockState(), 2);
        var held = tool();
        rig.player().setItemInHand(InteractionHand.MAIN_HAND, held);
        h.assertTrue(use(rig, InteractionHand.MAIN_HAND) == InteractionResult.PASS && held.getCount() == 1,
                "An ordinary unrelated block passes the click without changing the tool");
        noSample(h, rig);
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void clippingClaimedDuringPermissionsCannotPayAgain(GameTestHelper h) {
        var rig = rig(h);
        rig.player().setItemInHand(InteractionHand.MAIN_HAND, tool());
        rig.player().hook = () -> rig.plant().setClipped(true);
        use(rig, InteractionHand.MAIN_HAND);
        noSample(h, rig);
        h.assertTrue(rig.plant().hasBeenClipped(), "The older click cannot collect credit spent during permissions");
        h.succeed();
    }
}
