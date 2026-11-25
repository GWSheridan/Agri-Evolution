package com.agrievolution.content;

import com.agrievolution.genetics.CropGenome;
import com.agrievolution.genetics.GenomeStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class BreedingStationSafetyGameTests {
    private record Station(BlockPos pos, Player player, SeedBreedingStationBlockEntity entity,
                           SeedBreedingStationMenu menu, ItemStack parent) {}

    private static Station station(GameTestHelper h, GameType mode, boolean nativeSeeds) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        h.getLevel().setBlock(pos, AEBlocks.SEED_BREEDING_STATION.get().defaultBlockState(), 2);
        var entity = (SeedBreedingStationBlockEntity) h.getLevel().getBlockEntity(pos);
        var parent = new ItemStack(nativeSeeds ? Items.WHEAT_SEEDS : AEItems.WILD_SEED.get(), 3);
        if (nativeSeeds) parent.set(DataComponents.CUSTOM_NAME, Component.literal("Saved wheat variety"));
        else GenomeStorage.putOnSeed(parent, new CropGenome(81, 72, 93));
        entity.setItem(0, parent.copy());
        entity.setItem(1, parent.copy());
        entity.setItem(2, new ItemStack(AEItems.NUTRIENT_COMPOST.get(), 3));
        entity.setItem(3, new ItemStack(AEItems.IRON_CROP_SUPPORT.get(), 2));
        var player = h.makeMockPlayer(mode);
        player.setPos(Vec3.atCenterOf(pos));
        return new Station(pos, player, entity, new SeedBreedingStationMenu(1, player.getInventory(), entity), parent);
    }

    private static void ticks(GameTestHelper h, Station station, int count) {
        for (int tick = 0; tick < count; tick++) SeedBreedingStationBlockEntity.serverTick(
                h.getLevel(), station.pos(), station.entity().getBlockState(), station.entity());
    }

    private static void unchanged(GameTestHelper h, Station station, int progress) {
        h.assertTrue(station.entity().data.get(0) == progress && station.entity().getItem(4).isEmpty()
                        && ItemStack.matches(station.entity().getItem(0), station.parent())
                        && ItemStack.matches(station.entity().getItem(1), station.parent())
                        && station.entity().getItem(2).getCount() == 3 && station.entity().getItem(3).getCount() == 2,
                "Rejected ticks preserve progress, both parents, compost, supports and output");
    }

    private static SeedBreedingStationBlockEntity reload(GameTestHelper h, Station station) {
        var saved = new CompoundTag();
        station.entity().saveAdditional(saved, h.getLevel().registryAccess());
        var restored = new SeedBreedingStationBlockEntity(station.pos(), station.entity().getBlockState());
        restored.loadAdditional(saved, h.getLevel().registryAccess());
        return restored;
    }

    @GameTest(template = "empty")
    public static void removedStationCannotFinishAgriBreeding(GameTestHelper h) {
        var station = station(h, GameType.SURVIVAL, false);
        ticks(h, station, 139);
        station.entity().setRemoved();
        ticks(h, station, 1);
        unchanged(h, station, 139);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedStationCannotFinishNativeBreeding(GameTestHelper h) {
        var station = station(h, GameType.SURVIVAL, true);
        ticks(h, station, 139);
        station.entity().setRemoved();
        ticks(h, station, 1);
        unchanged(h, station, 139);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void replacementStationDoesNotReviveOldBreeding(GameTestHelper h) {
        var station = station(h, GameType.SURVIVAL, false);
        ticks(h, station, 139);
        h.getLevel().removeBlockEntity(station.pos());
        var replacement = new SeedBreedingStationBlockEntity(station.pos(), station.entity().getBlockState());
        h.getLevel().setBlockEntity(replacement);
        ticks(h, station, 1);
        unchanged(h, station, 139);
        h.assertTrue(replacement.isEmpty(), "The replacement station does not receive the old machine's output");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void breedingTickRejectsAnotherPosition(GameTestHelper h) {
        var station = station(h, GameType.SURVIVAL, false);
        ticks(h, station, 139);
        SeedBreedingStationBlockEntity.serverTick(h.getLevel(), station.pos().east(), station.entity().getBlockState(), station.entity());
        unchanged(h, station, 139);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void breedingTickRejectsAnotherDimension(GameTestHelper h) {
        var station = station(h, GameType.SURVIVAL, false);
        ticks(h, station, 139);
        var other = h.getLevel().getServer().getLevel(Level.NETHER);
        h.assertTrue(other != null && other != h.getLevel(), "The fixture has a separate dimension");
        SeedBreedingStationBlockEntity.serverTick(other, station.pos(), station.entity().getBlockState(), station.entity());
        unchanged(h, station, 139);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void breedingTickRejectsOldFacingState(GameTestHelper h) {
        var station = station(h, GameType.SURVIVAL, false);
        ticks(h, station, 139);
        var oldState = station.entity().getBlockState();
        h.getLevel().setBlock(station.pos(), oldState.setValue(SeedBreedingStationBlock.FACING, Direction.EAST), 2);
        SeedBreedingStationBlockEntity.serverTick(h.getLevel(), station.pos(), oldState, station.entity());
        unchanged(h, station, 139);
        ticks(h, station, 1);
        h.assertFalse(station.entity().getItem(4).isEmpty(), "A current tick still completes the pending cross");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void savedProgressCannotTransferToNewParents(GameTestHelper h) {
        var station = station(h, GameType.SURVIVAL, false);
        ticks(h, station, 90);
        station.entity().setItem(0, new ItemStack(AEItems.ONION_SEED.get(), 3));
        station.entity().setItem(1, new ItemStack(AEItems.ONION_SEED.get(), 3));
        h.assertTrue(reload(h, station).data.get(0) == 0,
                "Saving before the next tick cannot carry another pair's breeding time into the new pair");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void savedProgressCannotTransferToChangedGenes(GameTestHelper h) {
        var station = station(h, GameType.SURVIVAL, false);
        ticks(h, station, 90);
        var replacement = station.parent().copy();
        GenomeStorage.putOnSeed(replacement, new CropGenome(32, 94, 65));
        station.entity().setItem(0, replacement.copy());
        station.entity().setItem(1, replacement.copy());
        h.assertTrue(reload(h, station).data.get(0) == 0,
                "Changing a seed's genome invalidates saved progress even when the item type stays the same");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void closedBreedingScreenCannotPickUpParents(GameTestHelper h) {
        var station = station(h, GameType.SURVIVAL, false);
        station.menu().removed(station.player());
        station.menu().clicked(0, 0, ClickType.PICKUP, station.player());
        h.assertTrue(station.menu().getCarried().isEmpty() && ItemStack.matches(station.entity().getItem(0), station.parent()),
                "Late pickup cannot use a breeding screen that has closed");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void closedBreedingScreenCannotShiftDeposit(GameTestHelper h) {
        var station = station(h, GameType.SURVIVAL, false);
        station.player().getInventory().setItem(0, station.parent().copyWithCount(2));
        station.menu().removed(station.player());
        h.assertTrue(station.menu().quickMoveStack(station.player(), 32).isEmpty()
                        && station.player().getInventory().getItem(0).getCount() == 2
                        && ItemStack.matches(station.entity().getItem(0), station.parent()),
                "Late shift-click leaves the player's seeds and both parent slots alone");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void foreignPlayerCannotPickUpBreedingParents(GameTestHelper h) {
        var station = station(h, GameType.SURVIVAL, false);
        var other = h.makeMockPlayer(GameType.SURVIVAL);
        other.setPos(Vec3.atCenterOf(station.pos()));
        station.menu().clicked(0, 0, ClickType.PICKUP, other);
        h.assertTrue(station.menu().getCarried().isEmpty() && ItemStack.matches(station.entity().getItem(0), station.parent()),
                "Only the actual viewer can use this breeding inventory");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void spectatorCannotWithdrawBreedingParents(GameTestHelper h) {
        var station = station(h, GameType.SPECTATOR, false);
        station.menu().clicked(0, 0, ClickType.PICKUP, station.player());
        h.assertTrue(station.menu().getCarried().isEmpty() && station.menu().quickMoveStack(station.player(), 0).isEmpty()
                        && ItemStack.matches(station.entity().getItem(0), station.parent()),
                "Spectators cannot use pickup or shift-click to withdraw the parents");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void deadPlayerCannotWithdrawBreedingParents(GameTestHelper h) {
        var station = station(h, GameType.SURVIVAL, false);
        station.player().setHealth(0);
        h.assertTrue(station.menu().quickMoveStack(station.player(), 0).isEmpty()
                        && ItemStack.matches(station.entity().getItem(0), station.parent()),
                "The old breeding screen cannot transfer items after its viewer dies");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedBreedingSlotCannotBeTakenDirectly(GameTestHelper h) {
        var station = station(h, GameType.SURVIVAL, false);
        station.entity().setRemoved();
        h.assertTrue(station.menu().getSlot(0).safeTake(1, 1, station.player()).isEmpty()
                        && ItemStack.matches(station.entity().getItem(0), station.parent()),
                "A retained slot reference cannot withdraw from a removed breeding station");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void closedBreedingSlotCannotAcceptSeedsDirectly(GameTestHelper h) {
        var station = station(h, GameType.SURVIVAL, false);
        var offered = station.parent().copyWithCount(2);
        station.menu().removed(station.player());
        station.menu().getSlot(0).safeInsert(offered);
        h.assertTrue(offered.getCount() == 2 && ItemStack.matches(station.entity().getItem(0), station.parent()),
                "A closed screen's slot cannot consume offered seeds");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void closedBreedingPlayerSlotCannotBeTakenDirectly(GameTestHelper h) {
        var station = station(h, GameType.SURVIVAL, false);
        station.player().getInventory().setItem(0, new ItemStack(Items.DIAMOND, 2));
        station.menu().removed(station.player());
        h.assertTrue(station.menu().getSlot(32).safeTake(1, 1, station.player()).isEmpty()
                        && station.player().getInventory().getItem(0).getCount() == 2,
                "The player-inventory slots obey the closed-screen check too");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedBreedingStationRejectsPlacementQueries(GameTestHelper h) {
        var station = station(h, GameType.SURVIVAL, false);
        station.entity().setRemoved();
        h.assertTrue(!station.entity().canPlaceItem(0, station.parent())
                        && !station.entity().canPlaceItem(2, new ItemStack(AEItems.NUTRIENT_COMPOST.get()))
                        && !station.entity().canPlaceItem(3, new ItemStack(AEItems.IRON_CROP_SUPPORT.get())),
                "Removed machines do not advertise insertable parent or reagent slots");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void unchangedParentTopUpsKeepSavedBreedingProgress(GameTestHelper h) {
        var station = station(h, GameType.SURVIVAL, false);
        ticks(h, station, 90);
        station.entity().setItem(0, station.parent().copyWithCount(4));
        station.entity().setItem(1, station.parent().copyWithCount(4));
        var restored = reload(h, station);
        h.assertTrue(restored.data.get(0) == 90, "Topping up the same genetic lines preserves completed breeding time");
        h.getLevel().removeBlockEntity(station.pos());
        h.getLevel().setBlockEntity(restored);
        for (int i = 0; i < 50; i++) SeedBreedingStationBlockEntity.serverTick(h.getLevel(), station.pos(), restored.getBlockState(), restored);
        h.assertTrue(restored.getItem(4).getCount() == 1 && restored.getItem(0).getCount() == 3
                        && restored.getItem(1).getCount() == 3 && restored.getItem(2).getCount() == 2
                        && restored.getItem(3).getCount() == 2,
                "A resumed ordinary cross charges each parent and compost once and keeps unused supports");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void liveParentChangesRestartBreeding(GameTestHelper h) {
        var station = station(h, GameType.SURVIVAL, false);
        ticks(h, station, 90);
        station.entity().setItem(0, new ItemStack(AEItems.ONION_SEED.get(), 3));
        station.entity().setItem(1, new ItemStack(AEItems.ONION_SEED.get(), 3));
        ticks(h, station, 1);
        h.assertTrue(station.entity().data.get(0) == 1 && station.entity().getItem(4).isEmpty(),
                "Live parent changes start a new cross rather than completing the old one");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void adventureBreedingDepositsStillWork(GameTestHelper h) {
        var station = station(h, GameType.ADVENTURE, false);
        station.player().getAbilities().mayBuild = false;
        station.entity().setItem(0, ItemStack.EMPTY);
        station.entity().setItem(1, ItemStack.EMPTY);
        station.player().getInventory().setItem(0, station.parent().copyWithCount(2));
        h.assertTrue(!station.menu().quickMoveStack(station.player(), 32).isEmpty()
                        && station.entity().getItem(0).getCount() == 2 && station.player().getInventory().getItem(0).isEmpty(),
                "Ordinary inventory use does not require permission to build or configure machines");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void breedingPreviewWorksWithoutAPlayer(GameTestHelper h) {
        var menu = new SeedBreedingStationMenu(1, new Inventory(null));
        h.assertTrue(menu.getSlot(0).mayPlace(new ItemStack(Items.WHEAT_SEEDS))
                        && menu.getSlot(2).mayPlace(new ItemStack(AEItems.NUTRIENT_COMPOST.get()))
                        && menu.getSlot(3).mayPlace(new ItemStack(AEItems.IRON_CROP_SUPPORT.get()))
                        && !menu.getSlot(4).mayPlace(new ItemStack(Items.WHEAT_SEEDS)),
                "World-free previews retain the same parent, reagent and output slot rules");
        h.succeed();
    }
}
