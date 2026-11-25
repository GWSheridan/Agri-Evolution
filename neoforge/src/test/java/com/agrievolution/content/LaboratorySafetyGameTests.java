package com.agrievolution.content;

import com.agrievolution.genetics.CropGenome;
import com.agrievolution.genetics.GenomeStorage;
import com.agrievolution.genetics.HybridLineageStorage;
import com.agrievolution.genetics.RareTrait;
import com.agrievolution.genetics.RareTraitStorage;
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
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class LaboratorySafetyGameTests {
    private record Lab(BlockPos pos, BotanicalGeneticsLaboratoryBlockEntity entity, Player player,
                       BotanicalGeneticsLaboratoryMenu menu, ItemStack seed, ItemStack sample) {}

    private static Lab lab(GameTestHelper h, GameType mode) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        h.getLevel().removeBlock(pos, false);
        h.getLevel().setBlock(pos, AEBlocks.BOTANICAL_GENETICS_LABORATORY.get().defaultBlockState(), 2);
        var entity = (BotanicalGeneticsLaboratoryBlockEntity) h.getLevel().getBlockEntity(pos);
        var seed = new ItemStack(AEItems.TOMATO_SEED.get(), 3);
        GenomeStorage.putOnSeed(seed, new CropGenome(40, 50, 60));
        RareTraitStorage.putOnSeed(seed, RareTrait.VIGOROUS);
        var donor = seed.copyWithCount(1);
        GenomeStorage.putOnSeed(donor, new CropGenome(80, 90, 85));
        var sample = GeneticSampleItem.capture(donor);
        entity.setItem(0, seed.copy());
        entity.setItem(1, new ItemStack(AEItems.NUTRIENT_COMPOST.get(), 3));
        entity.setItem(2, sample.copy());
        entity.setItem(3, new ItemStack(AEItems.IRON_CROP_SUPPORT.get(), 3));
        var player = h.makeMockPlayer(mode);
        player.setPos(Vec3.atCenterOf(pos));
        return new Lab(pos, entity, player, new BotanicalGeneticsLaboratoryMenu(1, player.getInventory(), entity), seed, sample);
    }

    private static void ticks(GameTestHelper h, Lab lab, int count) {
        for (int i = 0; i < count; i++) BotanicalGeneticsLaboratoryBlockEntity.serverTick(
                h.getLevel(), lab.pos(), lab.entity().getBlockState(), lab.entity());
    }

    private static void unchanged(GameTestHelper h, Lab lab, int progress) {
        h.assertTrue(lab.entity().data.get(0) == progress && lab.entity().getItem(4).isEmpty()
                        && ItemStack.matches(lab.entity().getItem(0), lab.seed())
                        && ItemStack.matches(lab.entity().getItem(2), lab.sample())
                        && lab.entity().getItem(1).getCount() == 3 && lab.entity().getItem(3).getCount() == 3,
                "Rejected research keeps progress, seed genetics, donor samples, compost, supports and output");
    }

    private static CompoundTag save(GameTestHelper h, Lab lab) {
        var tag = new CompoundTag();
        lab.entity().saveAdditional(tag, h.getLevel().registryAccess());
        return tag;
    }

    private static BotanicalGeneticsLaboratoryBlockEntity reload(GameTestHelper h, Lab lab, CompoundTag tag) {
        var restored = new BotanicalGeneticsLaboratoryBlockEntity(lab.pos(), lab.entity().getBlockState());
        restored.loadAdditional(tag, h.getLevel().registryAccess());
        return restored;
    }

    @GameTest(template = "empty")
    public static void removedLaboratoryCannotFinishEnhancement(GameTestHelper h) {
        var lab = lab(h, GameType.SURVIVAL);
        ticks(h, lab, 239);
        lab.entity().setRemoved();
        ticks(h, lab, 1);
        unchanged(h, lab, 239);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedLaboratoryCannotFinishGraft(GameTestHelper h) {
        var lab = lab(h, GameType.SURVIVAL);
        lab.entity().selectMode(BotanicalGeneticsLaboratoryBlockEntity.GRAFT);
        ticks(h, lab, 239);
        lab.entity().setRemoved();
        ticks(h, lab, 1);
        unchanged(h, lab, 239);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void replacementLaboratoryDoesNotReviveOldResearch(GameTestHelper h) {
        var lab = lab(h, GameType.SURVIVAL);
        ticks(h, lab, 239);
        h.getLevel().removeBlockEntity(lab.pos());
        var replacement = new BotanicalGeneticsLaboratoryBlockEntity(lab.pos(), lab.entity().getBlockState());
        h.getLevel().setBlockEntity(replacement);
        ticks(h, lab, 1);
        unchanged(h, lab, 239);
        h.assertTrue(replacement.isEmpty(), "The replacement owns its own inventory");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void laboratoryTickRejectsAnotherPosition(GameTestHelper h) {
        var lab = lab(h, GameType.SURVIVAL);
        ticks(h, lab, 239);
        BotanicalGeneticsLaboratoryBlockEntity.serverTick(h.getLevel(), lab.pos().east(), lab.entity().getBlockState(), lab.entity());
        unchanged(h, lab, 239);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void laboratoryTickRejectsAnotherDimension(GameTestHelper h) {
        var lab = lab(h, GameType.SURVIVAL);
        ticks(h, lab, 239);
        var other = h.getLevel().getServer().getLevel(Level.NETHER);
        h.assertTrue(other != null && other != h.getLevel(), "The fixture has another dimension");
        BotanicalGeneticsLaboratoryBlockEntity.serverTick(other, lab.pos(), lab.entity().getBlockState(), lab.entity());
        unchanged(h, lab, 239);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void laboratoryTickRejectsOldFacingState(GameTestHelper h) {
        var lab = lab(h, GameType.SURVIVAL);
        ticks(h, lab, 239);
        var old = lab.entity().getBlockState();
        h.getLevel().setBlock(lab.pos(), old.setValue(BotanicalGeneticsLaboratoryBlock.FACING, Direction.EAST), 2);
        BotanicalGeneticsLaboratoryBlockEntity.serverTick(h.getLevel(), lab.pos(), old, lab.entity());
        unchanged(h, lab, 239);
        ticks(h, lab, 1);
        h.assertFalse(lab.entity().getItem(4).isEmpty(), "The next current tick can finish the research");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void changedSeedCannotInheritSavedResearchProgress(GameTestHelper h) {
        var lab = lab(h, GameType.SURVIVAL);
        ticks(h, lab, 70);
        lab.entity().setItem(0, new ItemStack(AEItems.ONION_SEED.get(), 3));
        h.assertTrue(reload(h, lab, save(h, lab)).data.get(0) == 0, "Saving before the next tick cannot transfer research time to another seed");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void changedDonorCannotInheritSavedResearchProgress(GameTestHelper h) {
        var lab = lab(h, GameType.SURVIVAL);
        lab.entity().selectMode(BotanicalGeneticsLaboratoryBlockEntity.GRAFT);
        ticks(h, lab, 70);
        var donor = lab.seed().copyWithCount(1);
        GenomeStorage.putOnSeed(donor, new CropGenome(95, 96, 97));
        lab.entity().setItem(2, GeneticSampleItem.capture(donor));
        h.assertTrue(reload(h, lab, save(h, lab)).data.get(0) == 0, "A changed donor starts a new saved grafting attempt");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void lockedOrInvalidSavedLabModeClearsProgress(GameTestHelper h) {
        var lab = lab(h, GameType.SURVIVAL);
        for (int mode : new int[]{BotanicalGeneticsLaboratoryBlockEntity.HYBRID, 999}) {
            var tag = save(h, lab);
            tag.putInt("ResearchMode", mode);
            tag.putInt("ResearchProgress", 319);
            var restored = reload(h, lab, tag);
            h.assertTrue(restored.mode() == BotanicalGeneticsLaboratoryBlockEntity.GROWTH && restored.data.get(0) == 0,
                    "Falling back from a locked or invalid mode cannot complete another mode's research");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void sameGeneticLineTopUpsKeepSavedResearchProgress(GameTestHelper h) {
        var lab = lab(h, GameType.SURVIVAL);
        ticks(h, lab, 70);
        lab.entity().setItem(0, lab.seed().copyWithCount(4));
        lab.entity().setItem(2, lab.sample().copy());
        var restored = reload(h, lab, save(h, lab));
        h.assertTrue(restored.data.get(0) == 70, "Topping up unchanged genetic lines preserves their research time");
        h.getLevel().removeBlockEntity(lab.pos());
        h.getLevel().setBlockEntity(restored);
        for (int i = 0; i < 170; i++) BotanicalGeneticsLaboratoryBlockEntity.serverTick(h.getLevel(), lab.pos(), restored.getBlockState(), restored);
        h.assertTrue(GenomeStorage.fromSeed(restored.getItem(4)).equals(new CropGenome(46, 50, 60))
                        && RareTraitStorage.fromSeed(restored.getItem(4)) == RareTrait.VIGOROUS
                        && restored.getItem(0).getCount() == 3 && restored.getItem(1).getCount() == 2
                        && restored.getItem(2).getCount() == 1 && restored.getItem(3).getCount() == 2,
                "Resumed enhancement preserves other genetics and charges only its actual inputs");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void ordinaryExtractionPreservesSampleGenesAndSupports(GameTestHelper h) {
        var lab = lab(h, GameType.SURVIVAL);
        lab.entity().selectMode(BotanicalGeneticsLaboratoryBlockEntity.EXTRACT);
        ticks(h, lab, 240);
        var sample = lab.entity().getItem(4);
        h.assertTrue(GeneticSampleItem.isValid(sample) && GeneticSampleItem.genome(sample).equals(new CropGenome(40, 50, 60))
                        && GeneticSampleItem.rareTrait(sample) == RareTrait.VIGOROUS && lab.entity().getItem(0).getCount() == 2
                        && lab.entity().getItem(1).getCount() == 2 && lab.entity().getItem(2).getCount() == 1
                        && lab.entity().getItem(3).getCount() == 3,
                "Extraction keeps the captured genes and rare trait and does not charge optional supports or donors");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void ordinaryAdvancedHybridStillRecordsAncestry(GameTestHelper h) {
        var lab = lab(h, GameType.SURVIVAL);
        lab.entity().installUpgrade();
        lab.entity().installHybridUpgrade();
        lab.entity().selectMode(BotanicalGeneticsLaboratoryBlockEntity.HYBRID);
        lab.entity().setItem(2, new ItemStack(AEItems.LETTUCE_SEED.get(), 3));
        ticks(h, lab, 320);
        h.assertTrue(!lab.entity().getItem(4).isEmpty() && HybridLineageStorage.fromSeed(lab.entity().getItem(4)) != null
                        && lab.entity().getItem(0).getCount() == 2 && lab.entity().getItem(1).getCount() == 2
                        && lab.entity().getItem(2).getCount() == 2 && lab.entity().getItem(3).getCount() == 2,
                "Unlocked hybrid research records ancestry and charges each input once");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void closedLaboratoryCannotPickUpSeeds(GameTestHelper h) {
        var lab = lab(h, GameType.SURVIVAL);
        lab.menu().removed(lab.player());
        lab.menu().clicked(0, 0, ClickType.PICKUP, lab.player());
        h.assertTrue(lab.menu().getCarried().isEmpty(), "A closed screen cannot pick up seeds");
        unchanged(h, lab, 0);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void closedLaboratoryCannotChangeMode(GameTestHelper h) {
        var lab = lab(h, GameType.SURVIVAL);
        lab.menu().removed(lab.player());
        h.assertTrue(!lab.menu().clickMenuButton(lab.player(), BotanicalGeneticsLaboratoryBlockEntity.GRAFT)
                        && lab.entity().mode() == BotanicalGeneticsLaboratoryBlockEntity.GROWTH, "Late mode buttons cannot configure a closed screen");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void foreignViewerCannotChangeLaboratoryMode(GameTestHelper h) {
        var lab = lab(h, GameType.SURVIVAL);
        var other = h.makeMockPlayer(GameType.SURVIVAL);
        other.setPos(Vec3.atCenterOf(lab.pos()));
        h.assertTrue(!lab.menu().clickMenuButton(other, BotanicalGeneticsLaboratoryBlockEntity.GRAFT)
                        && lab.entity().mode() == BotanicalGeneticsLaboratoryBlockEntity.GROWTH, "A nearby player cannot use another viewer's controls");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void spectatorCannotWithdrawLaboratorySeeds(GameTestHelper h) {
        var lab = lab(h, GameType.SPECTATOR);
        lab.menu().clicked(0, 0, ClickType.PICKUP, lab.player());
        h.assertTrue(lab.menu().getCarried().isEmpty() && lab.menu().quickMoveStack(lab.player(), 0).isEmpty(), "Spectators cannot withdraw through pickup or shift-click");
        unchanged(h, lab, 0);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void deadViewerCannotWithdrawLaboratorySeeds(GameTestHelper h) {
        var lab = lab(h, GameType.SURVIVAL);
        lab.player().setHealth(0);
        h.assertTrue(lab.menu().quickMoveStack(lab.player(), 0).isEmpty(), "A dead viewer cannot use the old laboratory inventory");
        unchanged(h, lab, 0);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedLaboratorySlotCannotBeTakenDirectly(GameTestHelper h) {
        var lab = lab(h, GameType.SURVIVAL);
        lab.entity().setRemoved();
        h.assertTrue(lab.menu().getSlot(0).safeTake(1, 1, lab.player()).isEmpty(), "A retained slot reference cannot withdraw from a removed laboratory");
        unchanged(h, lab, 0);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void closedLaboratorySlotsRejectDirectTransfers(GameTestHelper h) {
        var lab = lab(h, GameType.SURVIVAL);
        lab.player().getInventory().setItem(0, new ItemStack(Items.DIAMOND, 2));
        lab.menu().removed(lab.player());
        var offered = lab.seed().copyWithCount(2);
        lab.menu().getSlot(0).safeInsert(offered);
        h.assertTrue(offered.getCount() == 2 && lab.menu().getSlot(32).safeTake(1, 1, lab.player()).isEmpty(), "Machine and player slots share the screen's access rules");
        unchanged(h, lab, 0);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void adventureLaboratoryInventoryWorksWithoutConfiguring(GameTestHelper h) {
        var lab = lab(h, GameType.ADVENTURE);
        lab.player().getAbilities().mayBuild = false;
        h.assertTrue(!lab.menu().quickMoveStack(lab.player(), 0).isEmpty()
                        && !lab.menu().clickMenuButton(lab.player(), BotanicalGeneticsLaboratoryBlockEntity.GRAFT)
                        && lab.player().getInventory().countItem(AEItems.TOMATO_SEED.get()) == 3,
                "Ordinary inventory use remains available while configuration still respects build permission");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void laboratoryPreviewWorksWithoutPlayer(GameTestHelper h) {
        var menu = new BotanicalGeneticsLaboratoryMenu(1, new Inventory(null));
        h.assertTrue(menu.getSlot(0).mayPlace(new ItemStack(AEItems.TOMATO_SEED.get()))
                        && menu.getSlot(1).mayPlace(new ItemStack(AEItems.NUTRIENT_COMPOST.get()))
                        && !menu.getSlot(4).mayPlace(new ItemStack(AEItems.TOMATO_SEED.get())), "World-free previews retain the same typed slots");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedLaboratoryRejectsDirectSettingsAndIntake(GameTestHelper h) {
        var lab = lab(h, GameType.SURVIVAL);
        lab.entity().setRemoved();
        h.assertTrue(!lab.entity().installUpgrade() && !lab.entity().installHybridUpgrade(), "Removed laboratories reject direct upgrades");
        lab.entity().selectMode(BotanicalGeneticsLaboratoryBlockEntity.GRAFT);
        h.assertTrue(lab.entity().mode() == BotanicalGeneticsLaboratoryBlockEntity.GROWTH
                        && !lab.entity().canPlaceItem(0, lab.seed()) && !lab.entity().canPlaceItem(2, lab.sample()), "Removed references cannot change modes or advertise valid inputs");
        h.succeed();
    }

    private static final class UpgradePlayer extends Player {
        boolean allow = true;
        Runnable hook;
        UpgradePlayer(ServerLevel level, BlockPos pos) {
            super(level, pos, 0, new GameProfile(UUID.randomUUID(), "lab-upgrade"));
            setPos(Vec3.atCenterOf(pos));
            getAbilities().mayBuild = true;
        }
        @Override public boolean isCreative() { return false; }
        @Override public boolean isSpectator() { return false; }
        @Override public boolean mayUseItemAt(BlockPos pos, Direction side, ItemStack stack) {
            var callback = hook;
            hook = null;
            if (callback != null) callback.run();
            return allow && super.mayUseItemAt(pos, side, stack);
        }
    }

    private static ItemStack module(boolean hybrid) {
        return new ItemStack(hybrid ? AEItems.LABORATORY_HYBRIDIZATION_UPGRADE.get() : AEItems.LABORATORY_RESEARCH_UPGRADE.get(), 2);
    }

    private static ItemInteractionResult upgrade(GameTestHelper h, Lab lab, Player player, InteractionHand hand, ItemStack stack) {
        return AEBlocks.BOTANICAL_GENETICS_LABORATORY.get().useItemOn(stack, h.getLevel().getBlockState(lab.pos()),
                h.getLevel(), lab.pos(), player, hand, new BlockHitResult(Vec3.atCenterOf(lab.pos()), Direction.UP, lab.pos(), false));
    }

    private static boolean installed(Lab lab, boolean hybrid) { return hybrid ? lab.entity().isHybridUpgraded() : lab.entity().isUpgraded(); }

    @GameTest(template = "empty")
    public static void laboratoryUpgradeRespectsItemUsePermission(GameTestHelper h) {
        for (boolean hybrid : new boolean[]{false, true}) {
            var lab = lab(h, GameType.SURVIVAL);
            if (hybrid) lab.entity().installUpgrade();
            var player = new UpgradePlayer(h.getLevel(), lab.pos());
            player.allow = false;
            var module = module(hybrid);
            player.setItemInHand(InteractionHand.MAIN_HAND, module);
            h.assertTrue(upgrade(h, lab, player, InteractionHand.MAIN_HAND, module) == ItemInteractionResult.FAIL
                            && !installed(lab, hybrid) && module.getCount() == 2, "Denied item use preserves the module and laboratory tier");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void changedHeldModuleCancelsLaboratoryUpgrade(GameTestHelper h) {
        for (boolean hybrid : new boolean[]{false, true}) {
            var lab = lab(h, GameType.SURVIVAL);
            if (hybrid) lab.entity().installUpgrade();
            var player = new UpgradePlayer(h.getLevel(), lab.pos());
            var module = module(hybrid);
            player.setItemInHand(InteractionHand.MAIN_HAND, module);
            player.hook = () -> player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
            upgrade(h, lab, player, InteractionHand.MAIN_HAND, module);
            h.assertTrue(!installed(lab, hybrid) && module.getCount() == 2 && player.getMainHandItem().is(Items.STICK),
                    "A permission callback that changes the held item cancels the old upgrade");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void replacedLaboratoryCancelsOldUpgrade(GameTestHelper h) {
        var lab = lab(h, GameType.SURVIVAL);
        var player = new UpgradePlayer(h.getLevel(), lab.pos());
        var module = module(false);
        player.setItemInHand(InteractionHand.MAIN_HAND, module);
        player.hook = () -> {
            h.getLevel().removeBlockEntity(lab.pos());
            h.getLevel().setBlockEntity(new BotanicalGeneticsLaboratoryBlockEntity(lab.pos(), lab.entity().getBlockState()));
        };
        upgrade(h, lab, player, InteractionHand.MAIN_HAND, module);
        var replacement = (BotanicalGeneticsLaboratoryBlockEntity) h.getLevel().getBlockEntity(lab.pos());
        h.assertTrue(replacement != lab.entity() && !replacement.isUpgraded() && !lab.entity().isUpgraded() && module.getCount() == 2,
                "A replacement machine does not inherit the old upgrade click");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void changedModuleComponentsCancelLaboratoryUpgrade(GameTestHelper h) {
        var lab = lab(h, GameType.SURVIVAL);
        var player = new UpgradePlayer(h.getLevel(), lab.pos());
        var module = module(false);
        player.setItemInHand(InteractionHand.MAIN_HAND, module);
        player.hook = () -> module.set(DataComponents.CUSTOM_NAME, Component.literal("Changed module"));
        upgrade(h, lab, player, InteractionHand.MAIN_HAND, module);
        h.assertTrue(!lab.entity().isUpgraded() && module.getCount() == 2
                        && Component.literal("Changed module").equals(module.get(DataComponents.CUSTOM_NAME)), "Changed module data is preserved without installing the old upgrade");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void deadPlayerCannotUpgradeLaboratory(GameTestHelper h) {
        var lab = lab(h, GameType.SURVIVAL);
        var module = module(false);
        lab.player().setItemInHand(InteractionHand.MAIN_HAND, module);
        lab.player().setHealth(0);
        upgrade(h, lab, lab.player(), InteractionHand.MAIN_HAND, module);
        h.assertTrue(!lab.entity().isUpgraded() && module.getCount() == 2, "Dead players cannot install or spend upgrade modules");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void offhandLaboratoryUpgradesReachTheUsedHand(GameTestHelper h) {
        for (boolean hybrid : new boolean[]{false, true}) {
            var lab = lab(h, GameType.SURVIVAL);
            if (hybrid) lab.entity().installUpgrade();
            var module = module(hybrid);
            lab.player().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            lab.player().setItemInHand(InteractionHand.OFF_HAND, module);
            h.assertTrue(upgrade(h, lab, lab.player(), InteractionHand.MAIN_HAND, ItemStack.EMPTY) == ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION,
                    "An empty main hand passes the click to the offhand upgrade instead of opening the screen");
            upgrade(h, lab, lab.player(), InteractionHand.OFF_HAND, module);
            h.assertTrue(installed(lab, hybrid) && module.getCount() == 1, "The used offhand installs and charges exactly one module");
        }
        h.succeed();
    }
}
