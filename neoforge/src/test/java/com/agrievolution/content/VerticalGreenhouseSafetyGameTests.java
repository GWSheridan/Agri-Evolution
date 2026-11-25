package com.agrievolution.content;

import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class VerticalGreenhouseSafetyGameTests {
    private record House(BlockPos pos, VerticalGreenhouseBlockEntity entity, Player player, VerticalGreenhouseMenu menu) {}

    private static House house(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        h.getLevel().setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(pos));
        var machine = new ItemStack(AEItems.VERTICAL_GREENHOUSE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, machine);
        var hit = new BlockHitResult(Vec3.atBottomCenterOf(pos), Direction.UP, pos.below(), false);
        h.assertTrue(machine.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(), "The greenhouse places from its normal item");
        var entity = (VerticalGreenhouseBlockEntity) h.getLevel().getBlockEntity(pos);
        h.assertTrue(entity != null && entity.complete() && entity.addBucket(), "The fixture has a complete column and one bucket of water");
        entity.setItem(0, new ItemStack(AEItems.TOMATO_SEED.get()));
        entity.setItem(1, new ItemStack(AEItems.LETTUCE_SEED.get()));
        entity.togglePairingMode();
        entity.selectParent(0, false);
        entity.selectParent(1, true);
        for (int second = 0; second < 5; second++) entity.runSecond(h.getLevel());
        return new House(pos, entity, player, new VerticalGreenhouseMenu(1, player.getInventory(), entity));
    }

    private static int[] data(VerticalGreenhouseBlockEntity entity) {
        int[] values = new int[VerticalGreenhouseBlockEntity.DATA_COUNT];
        for (int i = 0; i < values.length; i++) values[i] = entity.data.get(i);
        return values;
    }

    private static void unchanged(GameTestHelper h, House house, int[] before) {
        h.assertTrue(Arrays.equals(before, data(house.entity())) && house.entity().getItem(0).getCount() == 1
                        && house.entity().getItem(1).getCount() == 1, "Rejected actions retain water, settings, parent seeds and processing progress");
    }

    @GameTest(template = "empty")
    public static void closedVerticalGreenhouseCannotToggleGrowing(GameTestHelper h) {
        var house = house(h);
        int[] before = data(house.entity());
        house.menu().removed(house.player());
        h.assertFalse(house.menu().clickMenuButton(house.player(), VerticalGreenhouseMenu.TOGGLE_GROW), "A closed screen cannot toggle growing");
        unchanged(h, house, before);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void closedVerticalGreenhouseCannotChangeParents(GameTestHelper h) {
        var house = house(h);
        int[] before = data(house.entity());
        house.menu().removed(house.player());
        h.assertFalse(house.menu().clickMenuButton(house.player(), VerticalGreenhouseMenu.CHOOSE_A + 2), "A closed screen cannot change the selected breeding trays");
        unchanged(h, house, before);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void foreignVerticalGreenhouseViewerCannotEditGenes(GameTestHelper h) {
        var house = house(h);
        int[] before = data(house.entity());
        var other = h.makeMockPlayer(GameType.SURVIVAL);
        other.setPos(Vec3.atCenterOf(house.pos()));
        h.assertFalse(house.menu().clickMenuButton(other, VerticalGreenhouseMenu.TARGET_GENE), "Another nearby player cannot edit this viewer's target genes");
        unchanged(h, house, before);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void deadVerticalGreenhouseViewerCannotEditBreeding(GameTestHelper h) {
        var house = house(h);
        int[] before = data(house.entity());
        house.player().setHealth(0);
        h.assertFalse(house.menu().clickMenuButton(house.player(), VerticalGreenhouseMenu.TOGGLE_BREED), "A dead viewer cannot change breeding controls");
        unchanged(h, house, before);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void spectatorCannotWithdrawVerticalGreenhouseSeeds(GameTestHelper h) {
        var house = house(h);
        var player = h.makeMockPlayer(GameType.SPECTATOR);
        player.setPos(Vec3.atCenterOf(house.pos()));
        var menu = new VerticalGreenhouseMenu(2, player.getInventory(), house.entity());
        menu.clicked(0, 0, ClickType.PICKUP, player);
        h.assertTrue(menu.getCarried().isEmpty() && menu.quickMoveStack(player, 0).isEmpty()
                        && house.entity().getItem(0).getCount() == 1, "Spectators cannot withdraw planted seeds through either transfer path");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void closedVerticalGreenhouseSlotsCannotTakeSeeds(GameTestHelper h) {
        var house = house(h);
        house.menu().removed(house.player());
        h.assertTrue(house.menu().getSlot(0).safeTake(1, 1, house.player()).isEmpty()
                        && house.entity().getItem(0).getCount() == 1, "An old direct tray slot cannot withdraw after its screen closes");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedVerticalGreenhouseSlotsCannotTakeOutputs(GameTestHelper h) {
        var house = house(h);
        house.entity().setItem(VerticalGreenhouseBlockEntity.HARVEST, new ItemStack(AEItems.TOMATO.get(), 3));
        house.entity().setRemoved();
        h.assertTrue(house.menu().getSlot(VerticalGreenhouseBlockEntity.HARVEST).safeTake(1, 1, house.player()).isEmpty()
                        && house.entity().getItem(VerticalGreenhouseBlockEntity.HARVEST).getCount() == 3,
                "An old output slot cannot withdraw from a removed greenhouse");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void closedVerticalGreenhouseSlotsRejectSeedInsertion(GameTestHelper h) {
        var house = house(h);
        house.menu().removed(house.player());
        var source = new ItemStack(Items.WHEAT_SEEDS, 3);
        house.menu().getSlot(2).safeInsert(source);
        h.assertTrue(source.getCount() == 3 && house.entity().getItem(2).isEmpty(), "A closed tray cannot consume a direct insertion stack");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void closedVerticalGreenhouseCannotTransferPlayerInventory(GameTestHelper h) {
        var house = house(h);
        house.player().getInventory().setItem(9, new ItemStack(Items.APPLE, 4));
        house.menu().removed(house.player());
        h.assertTrue(house.menu().getSlot(VerticalGreenhouseBlockEntity.SLOTS).safeTake(1, 1, house.player()).isEmpty()
                        && house.player().getInventory().getItem(9).getCount() == 4, "Player inventory slots also stop transferring when the greenhouse screen closes");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void verticalGreenhousePreviewRetainsTypedSlotsWithoutPlayer(GameTestHelper h) {
        var menu = new VerticalGreenhouseMenu(1, new Inventory(null));
        h.assertTrue(menu.stillValid(null) && menu.getSlot(0).mayPlace(new ItemStack(Items.WHEAT_SEEDS))
                        && !menu.getSlot(0).mayPlace(new ItemStack(Items.DIRT))
                        && !menu.getSlot(VerticalGreenhouseBlockEntity.HARVEST).mayPlace(new ItemStack(Items.WHEAT)),
                "World-free previews retain typed tray and output rules");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void adventureVerticalGreenhouseInventoryAccessStillWorks(GameTestHelper h) {
        var house = house(h);
        var player = h.makeMockPlayer(GameType.ADVENTURE);
        player.getAbilities().mayBuild = false;
        player.setPos(Vec3.atCenterOf(house.pos()));
        var menu = new VerticalGreenhouseMenu(2, player.getInventory(), house.entity());
        h.assertTrue(!menu.quickMoveStack(player, 0).isEmpty() && !menu.clickMenuButton(player, VerticalGreenhouseMenu.TOGGLE_GROW)
                        && player.getInventory().countItem(AEItems.TOMATO_SEED.get()) == 1,
                "Adventure viewers can use inventory slots while configuration still needs build permission");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void foreignVerticalGreenhouseCloseLeavesOwnerScreenOpen(GameTestHelper h) {
        var house = house(h);
        var other = h.makeMockPlayer(GameType.SURVIVAL);
        other.setPos(Vec3.atCenterOf(house.pos()));
        house.menu().removed(other);
        h.assertTrue(house.menu().stillValid(house.player()) && house.menu().clickMenuButton(house.player(), VerticalGreenhouseMenu.CYCLE_WATER_INTAKE),
                "Only the owning viewer can close this screen");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedVerticalGreenhouseRejectsRouteChanges(GameTestHelper h) {
        var house = house(h);
        int[] before = data(house.entity());
        house.entity().setRemoved();
        house.entity().cycleOutputRoute(0);
        house.entity().toggleRejectRoute(1);
        unchanged(h, house, before);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedVerticalGreenhouseRejectsBatchAndTargetChanges(GameTestHelper h) {
        var house = house(h);
        int[] before = data(house.entity());
        house.entity().setRemoved();
        house.entity().cycleBatchTarget();
        house.entity().resetBatch();
        house.entity().cycleTargetGene(0, 1);
        h.assertFalse(house.entity().setTargetSeed(new ItemStack(AEItems.TOMATO_SEED.get())), "A removed greenhouse rejects a new target species");
        unchanged(h, house, before);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void replacedVerticalGreenhouseRejectsOldModeChanges(GameTestHelper h) {
        var house = house(h);
        var saved = new CompoundTag();
        house.entity().saveAdditional(saved, h.getLevel().registryAccess());
        int[] before = data(house.entity());
        h.getLevel().removeBlockEntity(house.pos());
        var replacement = new VerticalGreenhouseBlockEntity(house.pos(), h.getLevel().getBlockState(house.pos()));
        replacement.loadAdditional(saved, h.getLevel().registryAccess());
        h.getLevel().setBlockEntity(replacement);
        int[] newer = data(replacement);
        house.entity().toggle(0);
        house.entity().togglePairingMode();
        house.entity().cycleWaterIntake();
        h.assertFalse(house.entity().selectParent(2, false), "An old greenhouse cannot change its selected parent");
        unchanged(h, house, before);
        h.assertTrue(Arrays.equals(newer, data(replacement)), "The replacement greenhouse's settings and water are untouched");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void verticalGreenhouseRejectsInvalidRouteIndices(GameTestHelper h) {
        var house = house(h);
        int[] before = data(house.entity());
        for (int face : new int[]{-1, VerticalGreenhouseBlockEntity.ROUTE_COUNT, Integer.MAX_VALUE}) {
            house.entity().cycleOutputRoute(face);
            house.entity().toggleRejectRoute(face);
        }
        unchanged(h, house, before);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void verticalGreenhouseRejectsInvalidGeneIndices(GameTestHelper h) {
        var house = house(h);
        int[] before = data(house.entity());
        for (int gene : new int[]{-1, 3, Integer.MAX_VALUE}) house.entity().cycleTargetGene(gene, 1);
        unchanged(h, house, before);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void automationCannotRewriteVerticalGreenhouseSlotLists(GameTestHelper h) {
        var house = house(h);
        int[] offered = house.entity().getSlotsForFace(Direction.DOWN);
        h.assertTrue(offered.length == 18, "The default bottom route exposes both output rows");
        int first = offered[0];
        try {
            offered[0] = 0;
            h.assertTrue(house.entity().getSlotsForFace(Direction.DOWN)[0] == VerticalGreenhouseBlockEntity.HARVEST
                            && !house.entity().canTakeItemThroughFace(0, house.entity().getItem(0), Direction.DOWN),
                    "A caller cannot mutate a returned slot list into permission to extract planted parents");
        } finally { offered[0] = first; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedVerticalGreenhouseExposesNoAutomationSlots(GameTestHelper h) {
        var house = house(h);
        house.entity().setRemoved();
        for (var side : Direction.values()) h.assertTrue(house.entity().getSlotsForFace(side).length == 0, "Removed automation references expose no output slots");
        h.succeed();
    }

    private static void misalignCrown(GameTestHelper h, House house) {
        var crown = h.getLevel().getBlockState(house.pos().above(2));
        h.getLevel().setBlock(house.pos().above(2), crown.setValue(VerticalGreenhouseBlock.FACING, crown.getValue(VerticalGreenhouseBlock.FACING).getOpposite()), 2);
        h.assertFalse(house.entity().complete(), "The misaligned crown makes this current column incomplete");
    }

    @GameTest(template = "empty")
    public static void incompleteVerticalGreenhouseExposesNoAutomationSlots(GameTestHelper h) {
        var house = house(h);
        misalignCrown(h, house);
        h.assertTrue(house.entity().getSlotsForFace(Direction.DOWN).length == 0, "Incomplete columns expose no output slots");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void incompleteVerticalGreenhouseRejectsDirectConfiguration(GameTestHelper h) {
        var house = house(h);
        misalignCrown(h, house);
        int[] before = data(house.entity());
        house.entity().toggle(0);
        house.entity().cycleWaterIntake();
        house.entity().cycleBatchTarget();
        house.entity().toggleRejectRoute(0);
        unchanged(h, house, before);
        h.succeed();
    }

    private static void tick(GameTestHelper h, House house, Level world, BlockPos pos, BlockState state) {
        long time = h.getLevel().getGameTime();
        try {
            ((ServerLevelData) h.getLevel().getLevelData()).setGameTime(20);
            VerticalGreenhouseBlockEntity.serverTick(world, pos, state, house.entity());
        } finally { ((ServerLevelData) h.getLevel().getLevelData()).setGameTime(time); }
    }

    @GameTest(template = "empty")
    public static void verticalGreenhouseTickRejectsWrongWorld(GameTestHelper h) {
        var house = house(h);
        int[] before = data(house.entity());
        var other = h.getLevel().getServer().getLevel(Level.NETHER);
        h.assertTrue(other != null, "The Nether exists for the tick dimension check");
        tick(h, house, other, house.pos(), house.entity().getBlockState());
        unchanged(h, house, before);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void verticalGreenhouseTickRejectsWrongPosition(GameTestHelper h) {
        var house = house(h);
        int[] before = data(house.entity());
        tick(h, house, h.getLevel(), house.pos().above(), house.entity().getBlockState());
        unchanged(h, house, before);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void verticalGreenhouseTickRejectsOldFacingAndResumes(GameTestHelper h) {
        var house = house(h);
        var old = house.entity().getBlockState();
        var facing = old.getValue(VerticalGreenhouseBlock.FACING).getOpposite();
        for (int section = 0; section < 3; section++) {
            var pos = house.pos().above(section);
            h.getLevel().setBlock(pos, h.getLevel().getBlockState(pos).setValue(VerticalGreenhouseBlock.FACING, facing), 2);
        }
        int[] before = data(house.entity());
        tick(h, house, h.getLevel(), house.pos(), old);
        unchanged(h, house, before);
        tick(h, house, h.getLevel(), house.pos(), house.entity().getBlockState());
        h.assertFalse(Arrays.equals(before, data(house.entity())), "The next current tick resumes processing after rotation");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void directVerticalGreenhouseWorkRejectsWrongWorld(GameTestHelper h) {
        var house = house(h);
        int[] before = data(house.entity());
        ServerLevel other = h.getLevel().getServer().getLevel(Level.NETHER);
        h.assertTrue(other != null, "The Nether exists for the direct processing check");
        house.entity().runSecond(other);
        unchanged(h, house, before);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedVerticalGreenhouseTicksKeepOldStatusAndProgress(GameTestHelper h) {
        var house = house(h);
        int[] before = data(house.entity());
        house.entity().setRemoved();
        tick(h, house, h.getLevel(), house.pos(), house.entity().getBlockState());
        house.entity().runSecond(h.getLevel());
        unchanged(h, house, before);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void currentVerticalGreenhouseOutputRoutesStillFilterRows(GameTestHelper h) {
        var house = house(h);
        house.entity().cycleOutputRoute(0); // Both -> Off.
        house.entity().cycleOutputRoute(0); // Produce only.
        int[] produce = house.entity().getSlotsForFace(Direction.DOWN);
        h.assertTrue(produce.length == 9 && produce[0] == VerticalGreenhouseBlockEntity.HARVEST
                        && !house.entity().canTakeItemThroughFace(VerticalGreenhouseBlockEntity.OFFSPRING, ItemStack.EMPTY, Direction.DOWN),
                "Produce routes still expose only the harvest row");
        house.entity().toggleRejectRoute(0);
        int[] rejects = house.entity().getSlotsForFace(Direction.DOWN);
        h.assertTrue(rejects.length == 9 && rejects[0] == VerticalGreenhouseBlockEntity.REJECTS
                        && house.entity().canTakeItemThroughFace(VerticalGreenhouseBlockEntity.REJECTS, ItemStack.EMPTY, Direction.DOWN),
                "Reject routes still expose only the rejects row");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void currentVerticalGreenhouseBreedingRetainsParentsAndExactWaterCost(GameTestHelper h) {
        var house = house(h);
        house.entity().toggle(0);
        house.entity().cycleBatchTarget(); // One accepted child.
        int water = house.entity().water();
        for (int second = 0; second < 40; second++) house.entity().runSecond(h.getLevel());
        h.assertTrue(house.entity().water() == water - VerticalGreenhouseBlockEntity.BREED_WATER
                        && house.entity().getItem(0).getCount() == 1 && house.entity().getItem(1).getCount() == 1
                        && house.entity().data.get(VerticalGreenhouseBlockEntity.BATCH_COUNT) == 1,
                "One completed child still costs exactly 500 mB and retains both planted parents");
        h.succeed();
    }
}
