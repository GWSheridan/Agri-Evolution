package com.agrievolution.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class IrrigationControllerSafetyGameTests {
    private static final int ZONES = IrrigationControllerBlockEntity.ZONES;
    private record Rig(BlockPos pos, IrrigationControllerBlockEntity controller, IrrigationValveBlockEntity valve,
                       Player player, IrrigationControllerMenu menu) {}

    private static Rig rig(GameTestHelper h) {
        var world = h.getLevel();
        var pos = h.absolutePos(new BlockPos(4, 210, 4));
        world.setBlock(pos, AEBlocks.IRRIGATION_CONTROLLER.get().defaultBlockState(), 3);
        var controller = (IrrigationControllerBlockEntity) world.getBlockEntity(pos);
        var valve = namedValve(h, pos.above(2));
        world.setBlock(pos.above(3), AEBlocks.IRRIGATION_BASIN.get().defaultBlockState()
                .setValue(IrrigationBasinBlock.FILLED, true), 3);
        controller.link(valve);
        controller.action(0, 2);
        h.assertTrue(controller.active() == 0 && controller.manual() == 0 && valve.scheduleAllowsFlow(false),
                "The fixture has a live manual zone and a loaded source in its own chunk");
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(pos));
        return new Rig(pos, controller, valve, player, new IrrigationControllerMenu(1, player.getInventory(), controller));
    }

    private static IrrigationValveBlockEntity namedValve(GameTestHelper h, BlockPos pos) {
        h.getLevel().setBlock(pos, AEBlocks.IRRIGATION_VALVE.get().defaultBlockState(), 3);
        var valve = (IrrigationValveBlockEntity) h.getLevel().getBlockEntity(pos);
        valve.rename("Kitchen garden");
        return valve;
    }

    private static IrrigationControllerBlockEntity replacement(GameTestHelper h, Rig rig) {
        var saved = new CompoundTag();
        rig.controller().saveAdditional(saved, h.getLevel().registryAccess());
        h.getLevel().removeBlockEntity(rig.pos());
        var current = new IrrigationControllerBlockEntity(rig.pos(), h.getLevel().getBlockState(rig.pos()));
        current.loadAdditional(saved, h.getLevel().registryAccess());
        h.getLevel().setBlockEntity(current);
        return current;
    }

    @GameTest(template = "empty")
    public static void closedControllerCannotChangePower(GameTestHelper h) {
        var rig = rig(h);
        rig.menu().removed(rig.player());
        h.assertFalse(rig.menu().clickMenuButton(rig.player(), 5 * ZONES), "Closed controls cannot switch the controller off");
        h.assertTrue(rig.controller().enabled() && rig.controller().manual() == 0, "The running zone is preserved");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void closedControllerCannotEditZoneHours(GameTestHelper h) {
        var rig = rig(h);
        rig.menu().removed(rig.player());
        h.assertFalse(rig.menu().clickMenuButton(rig.player(), 0), "Closed controls cannot edit zone hours");
        h.assertTrue(rig.controller().startHour(0) == 0, "The saved start hour is unchanged");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void foreignControllerViewerCannotChangeReserve(GameTestHelper h) {
        var rig = rig(h);
        var other = h.makeMockPlayer(GameType.SURVIVAL);
        other.setPos(Vec3.atCenterOf(rig.pos()));
        h.assertFalse(rig.menu().clickMenuButton(other, 4 * ZONES), "Another nearby player cannot use this viewer's controls");
        h.assertTrue(rig.controller().reserveWater() == 0, "The reserve is unchanged");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void deadControllerViewerCannotStartManualRun(GameTestHelper h) {
        var rig = rig(h);
        rig.controller().action(0, 2);
        rig.player().setHealth(0);
        h.assertFalse(rig.menu().clickMenuButton(rig.player(), 2 * ZONES), "Dead players cannot start manual runs");
        h.assertTrue(rig.controller().manual() == -1, "No manual override was created");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void spectatorControllerScreenIsInvalid(GameTestHelper h) {
        var rig = rig(h);
        var spectator = h.makeMockPlayer(GameType.SPECTATOR);
        spectator.setPos(Vec3.atCenterOf(rig.pos()));
        var menu = new IrrigationControllerMenu(1, spectator.getInventory(), rig.controller());
        h.assertFalse(menu.stillValid(spectator), "A spectator cannot retain an editable controller screen");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void foreignControllerCloseDoesNotCloseOwnersScreen(GameTestHelper h) {
        var rig = rig(h);
        var other = h.makeMockPlayer(GameType.SURVIVAL);
        other.setPos(Vec3.atCenterOf(rig.pos()));
        rig.menu().removed(other);
        h.assertTrue(rig.menu().stillValid(rig.player()) && rig.menu().clickMenuButton(rig.player(), 4 * ZONES)
                && rig.controller().reserveWater() == 1000, "Only the owning viewer can close this menu");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void adventureControllerScreenRemainsReadable(GameTestHelper h) {
        var rig = rig(h);
        var player = h.makeMockPlayer(GameType.ADVENTURE);
        player.getAbilities().mayBuild = false;
        player.setPos(Vec3.atCenterOf(rig.pos()));
        var menu = new IrrigationControllerMenu(1, player.getInventory(), rig.controller());
        h.assertTrue(menu.stillValid(player) && menu.label(0).equals("Kitchen garden")
                        && !menu.clickMenuButton(player, 4 * ZONES) && rig.controller().reserveWater() == 0,
                "Adventure viewers can read schedules while edits still require build permission");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void controllerPreviewWorksWithoutWorldOrPlayer(GameTestHelper h) {
        var menu = new IrrigationControllerMenu(1, new Inventory(null), (IrrigationControllerBlockEntity) null);
        h.assertTrue(menu.stillValid(null) && !menu.linked(0) && menu.label(0).equals("Unlinked")
                        && !menu.clickMenuButton(null, 0), "World-free previews retain read access without editing a machine");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedControllerRejectsDirectSettings(GameTestHelper h) {
        var rig = rig(h);
        rig.controller().setRemoved();
        rig.controller().action(0, 4);
        rig.controller().action(0, 5);
        h.assertTrue(rig.controller().reserveWater() == 0 && rig.controller().enabled()
                        && rig.controller().manual() == 0, "Removed references cannot edit reserve, power or manual overrides");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void replacedControllerCannotLinkAnotherValve(GameTestHelper h) {
        var rig = rig(h);
        var current = replacement(h, rig);
        var candidate = namedValve(h, rig.pos().above(5));
        rig.controller().link(candidate);
        h.assertTrue(candidate.controller() == null && !rig.controller().contains(candidate.getBlockPos())
                        && !current.contains(candidate.getBlockPos()), "An old controller cannot reserve new valve links");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void controllerCannotLinkRemovedValve(GameTestHelper h) {
        var rig = rig(h);
        var candidate = namedValve(h, rig.pos().above(5));
        h.getLevel().removeBlockEntity(candidate.getBlockPos());
        rig.controller().link(candidate);
        h.assertTrue(candidate.controller() == null && !rig.controller().contains(candidate.getBlockPos()),
                "A removed valve cannot occupy one of the controller's six slots");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void oldControllerCannotReleaseReplacementZones(GameTestHelper h) {
        var rig = rig(h);
        var current = replacement(h, rig);
        rig.controller().release();
        h.assertTrue(rig.valve().controlledBy(rig.pos()) && current.contains(rig.valve().getBlockPos()),
                "Releasing an old controller cannot detach the replacement controller's valves");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedControllerCannotAuthorizeWaterRefills(GameTestHelper h) {
        var rig = rig(h);
        rig.controller().setRemoved();
        h.assertFalse(rig.controller().permitsRefill(rig.valve().getBlockPos()), "A removed controller cannot grant a refill through an old reference");
        h.assertFalse(rig.controller().zoneActive(rig.valve().getBlockPos()), "A removed controller cannot report an active zone");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void controllerRejectsInvalidDirectControlIndices(GameTestHelper h) {
        var rig = rig(h);
        for (int slot : new int[]{-1, ZONES, Integer.MAX_VALUE}) {
            for (int action = 0; action <= 6; action++) rig.controller().action(slot, action);
        }
        for (int action : new int[]{-1, 7, Integer.MAX_VALUE}) rig.controller().action(0, action);
        h.assertTrue(rig.controller().reserveWater() == 0 && rig.controller().enabled()
                        && rig.controller().manual() == 0 && rig.controller().startHour(0) == 0
                        && rig.controller().duration(0) == 400 && rig.controller().linked(0),
                "Invalid indices cannot throw, wrap into another zone or change global settings");
        h.succeed();
    }

    private static CompoundTag zone(int slot, BlockPos valve, int hour, int duration) {
        var zone = new CompoundTag();
        zone.putInt("Slot", slot);
        zone.putLong("Valve", valve.asLong());
        zone.putInt("Hour", hour);
        zone.putInt("Duration", duration);
        return zone;
    }

    private static void loadZones(GameTestHelper h, Rig rig, CompoundTag... entries) {
        var saved = new CompoundTag();
        var zones = new ListTag();
        for (var entry : entries) zones.add(entry);
        saved.put("Zones", zones);
        rig.controller().loadAdditional(saved, h.getLevel().registryAccess());
    }

    @GameTest(template = "empty")
    public static void controllerReloadRejectsLinksBeyondThirtyTwoBlocks(GameTestHelper h) {
        var rig = rig(h);
        var tooFar = rig.pos().above(32).east();
        loadZones(h, rig, zone(0, tooFar, 3, 1));
        h.assertFalse(rig.controller().linked(0), "Reloading enforces the same 32-block sphere as a new Spanner link");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void controllerReloadKeepsExactRangeAndSanitizesSchedule(GameTestHelper h) {
        var rig = rig(h);
        var edge = rig.pos().above(32);
        loadZones(h, rig, zone(0, edge, -1, Integer.MAX_VALUE));
        h.assertTrue(rig.controller().contains(edge) && rig.controller().startHour(0) == 23
                        && rig.controller().duration(0) == 1600 && rig.controller().manual() == -1 && rig.controller().active() == -1,
                "The exact range boundary and saved schedule clamps still work, without restoring a manual run");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void controllerReloadKeepsFirstValidEntryForEachSlot(GameTestHelper h) {
        var rig = rig(h);
        var first = rig.valve().getBlockPos();
        var duplicate = rig.pos().above(5);
        loadZones(h, rig, zone(0, first, 3, 1), zone(0, duplicate, 9, 3));
        h.assertTrue(rig.controller().contains(first) && !rig.controller().contains(duplicate)
                        && rig.controller().startHour(0) == 3 && rig.controller().duration(0) == 400,
                "Duplicate saved slots cannot replace an already accepted valve and schedule");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void controllerReleaseClearsRunningZoneStatus(GameTestHelper h) {
        var rig = rig(h);
        rig.controller().release();
        h.assertTrue(rig.controller().active() == -1 && rig.controller().manual() == -1 && !rig.controller().linked(0)
                        && rig.valve().controller() == null && rig.valve().scheduleAllowsFlow(false),
                "Releasing zones clears the running status and returns valves to ordinary manual operation");
        h.succeed();
    }

    private static void tick(GameTestHelper h, Rig rig, Level suppliedWorld, BlockPos suppliedPos, BlockState suppliedState) {
        long previous = h.getLevel().getGameTime();
        try {
            ((ServerLevelData) h.getLevel().getLevelData()).setGameTime(400);
            var ticker = AEBlocks.IRRIGATION_CONTROLLER.get().getTicker(h.getLevel(), rig.controller().getBlockState(), AEBlockEntities.IRRIGATION_CONTROLLER.get());
            h.assertTrue(ticker != null, "The placed controller has a server ticker");
            ticker.tick(suppliedWorld, suppliedPos, suppliedState, rig.controller());
        } finally { ((ServerLevelData) h.getLevel().getLevelData()).setGameTime(previous); }
    }

    private static void emptySource(GameTestHelper h, Rig rig) {
        h.getLevel().setBlock(rig.pos().above(3), AEBlocks.IRRIGATION_BASIN.get().defaultBlockState(), 2);
    }

    @GameTest(template = "empty")
    public static void controllerTickerRejectsWrongWorld(GameTestHelper h) {
        var rig = rig(h);
        emptySource(h, rig);
        var other = h.getLevel().getServer().getLevel(Level.NETHER);
        h.assertTrue(other != null, "The Nether exists for this dimension check");
        tick(h, rig, other, rig.pos(), rig.controller().getBlockState());
        h.assertTrue(rig.controller().active() == 0 && rig.valve().scheduleAllowsFlow(false), "A tick for another world cannot change this controller's valves");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void controllerTickerRejectsWrongPosition(GameTestHelper h) {
        var rig = rig(h);
        emptySource(h, rig);
        tick(h, rig, h.getLevel(), rig.pos().above(), rig.controller().getBlockState());
        h.assertTrue(rig.controller().active() == 0 && rig.valve().scheduleAllowsFlow(false), "A tick for another position cannot change this controller's valves");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void controllerTickerRejectsOldFacingAndResumesCurrentTick(GameTestHelper h) {
        var rig = rig(h);
        var old = rig.controller().getBlockState();
        var current = old.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST);
        h.getLevel().setBlock(rig.pos(), current, 2);
        emptySource(h, rig);
        tick(h, rig, h.getLevel(), rig.pos(), old);
        h.assertTrue(rig.controller().active() == 0 && rig.valve().scheduleAllowsFlow(false), "A rotated controller rejects the old tick state");
        tick(h, rig, h.getLevel(), rig.pos(), current);
        h.assertTrue(rig.controller().active() == -1 && !rig.valve().scheduleAllowsFlow(false), "The next current tick closes the zone when its source is empty");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void manualControllerRunExpiresAndProtectsReserveAgain(GameTestHelper h) {
        var rig = rig(h);
        rig.controller().action(0, 4);
        h.assertTrue(rig.controller().permitsRefill(rig.valve().getBlockPos()), "Manual watering can use the protected bucket");
        long before = h.getLevel().getGameTime();
        try {
            ((ServerLevelData) h.getLevel().getLevelData()).setGameTime(before + rig.controller().duration(0));
            rig.controller().checkSchedules();
            h.assertTrue(rig.controller().manual() == -1 && rig.controller().active() == -1
                            && !rig.valve().scheduleAllowsFlow(true), "The manual run ends at its duration and returns the last bucket to reserve");
        } finally { ((ServerLevelData) h.getLevel().getLevelData()).setGameTime(before); }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void controllerMenusKeepIndependentViewers(GameTestHelper h) {
        var rig = rig(h);
        var other = h.makeMockPlayer(GameType.SURVIVAL);
        other.setPos(Vec3.atCenterOf(rig.pos()));
        var second = new IrrigationControllerMenu(2, other.getInventory(), rig.controller());
        rig.menu().removed(rig.player());
        h.assertTrue(second.stillValid(other) && second.clickMenuButton(other, 4 * ZONES)
                        && rig.controller().reserveWater() == 1000, "Closing one viewer leaves another player's own controller screen usable");
        h.succeed();
    }
}
