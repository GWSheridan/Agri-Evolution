package com.agrievolution.content;

import com.agrievolution.farming.CompanionPlanting;
import com.agrievolution.genetics.CropSpecies;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class FarmsteadGameTests {
    @GameTest(template = "empty")
    public static void reserveProtectsRefillsWithoutStoppingBufferedSprays(GameTestHelper h) {
        var world = h.getLevel();
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        world.setBlock(pos, AEBlocks.IRRIGATION_CONTROLLER.get().defaultBlockState(), 3);
        var controller = (IrrigationControllerBlockEntity) world.getBlockEntity(pos);
        var junction = pos.north(2);
        var valve = namedValve(h, junction, "Buffered beds");
        for (var side : new Direction[]{Direction.EAST, Direction.WEST})
            world.setBlock(junction.relative(side), AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(), 3);
        for (var side : new Direction[]{Direction.NORTH, Direction.SOUTH})
            world.setBlock(junction.relative(side), AEBlocks.IRRIGATION_BASIN.get().defaultBlockState()
                    .setValue(IrrigationBasinBlock.FILLED, true), 3);
        var first = (IrrigationSprinklerBlockEntity) world.getBlockEntity(junction.east());
        var second = (IrrigationSprinklerBlockEntity) world.getBlockEntity(junction.west());
        long time = world.getDayTime();
        try {
            world.setDayTime(0);
            controller.link(valve);
            controller.action(0, 4);
            first.irrigateCycle();
            controller.checkSchedules();
            h.assertTrue(first.water() == 975 && controller.active() == 0 && first.hasCurrentSpray(),
                    "The schedule keeps watering with stored water after reaching the reserve");
            second.irrigateCycle();
            var route = IrrigationNetwork.trace(world, junction.west());
            h.assertTrue(second.water() == 0 && route.flow() == IrrigationNetwork.Flow.RESERVE_HELD
                    && SpannerItem.routeStatus(world, route).contains("1000 mB protected")
                    && second.inspectionStatus().contains("reserve"), "The second head reports a protected reserve without drawing it");
            world.setDayTime(500);
            controller.checkSchedules();
            h.assertTrue(controller.active() == -1 && !first.growthActive(), "The schedule still stops stored-water use at its end");
        } finally { world.setDayTime(time); }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void controllerReloadRejectsStaleOpenValve(GameTestHelper h) {
        var world = h.getLevel();
        var pos = h.absolutePos(new BlockPos(4, 225, 4));
        world.setBlock(pos, AEBlocks.IRRIGATION_CONTROLLER.get().defaultBlockState(), 3);
        var controller = (IrrigationControllerBlockEntity) world.getBlockEntity(pos);
        var valvePos = pos.north(2);
        var valve = namedValve(h, valvePos, "Reloaded beds");
        world.setBlock(valvePos.north(), AEBlocks.IRRIGATION_BASIN.get().defaultBlockState()
                .setValue(IrrigationBasinBlock.FILLED, true), 3);
        long time = world.getDayTime();
        try {
            world.setDayTime(0);
            controller.link(valve);
            h.assertTrue(IrrigationValveBlock.permitsFlow(world, valvePos, valve.getBlockState()), "Zone starts open");
            var saved = new CompoundTag();
            controller.saveAdditional(saved, world.registryAccess());
            world.removeBlockEntity(pos);
            var restored = new IrrigationControllerBlockEntity(pos, world.getBlockState(pos));
            restored.loadAdditional(saved, world.registryAccess());
            world.setBlockEntity(restored);
            h.assertFalse(IrrigationValveBlock.permitsFlow(world, valvePos, valve.getBlockState()),
                    "A valve's cached open state cannot bypass a restored controller before its first schedule check");
            restored.checkSchedules();
            h.assertTrue(IrrigationValveBlock.permitsFlow(world, valvePos, valve.getBlockState()), "A current schedule reopens the zone normally");
        } finally { world.setDayTime(time); }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void irrigationControllerReusesBrokenZoneSlotsAndEnforcesRange(GameTestHelper h) {
        var world = h.getLevel();
        var pos = h.absolutePos(new BlockPos(4, 200, 4));
        world.setBlockAndUpdate(pos, AEBlocks.IRRIGATION_CONTROLLER.get().defaultBlockState());
        var controller = (IrrigationControllerBlockEntity) world.getBlockEntity(pos);
        var oldPos = pos.north(2);
        var original = namedValve(h, oldPos, "Old orchard");
        controller.link(original);
        var viewer = h.makeMockPlayer(GameType.SURVIVAL);
        var menu = new IrrigationControllerMenu(1, viewer.getInventory(), controller);
        original.rename("North orchard");
        h.assertTrue(menu.label(0).equals("North orchard"), "The open controller uses the current valve name");
        world.removeBlock(oldPos, false);
        controller.checkSchedules();
        h.assertTrue(!controller.linked(0) && menu.label(0).equals("Unlinked"), "Breaking a valve clears its loaded slot");
        var replacement = namedValve(h, pos.above(32), "Replacement");
        replacement.setController(pos.above());
        controller.link(replacement);
        h.assertTrue(replacement.controlledBy(pos) && menu.label(0).equals("Replacement"), "The slot is reusable up to exactly 32 blocks");
        var tooFar = namedValve(h, pos.above(32).east(), "Too far");
        controller.link(tooFar);
        h.assertTrue(!controller.linked(1) && tooFar.controller() == null, "The diagonal beyond 32 blocks is rejected");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void irrigationSwitchCannotBorrowSupplyThroughItsPreviousZone(GameTestHelper h) {
        var world = h.getLevel();
        var pos = h.absolutePos(new BlockPos(4, 209, 4));
        world.setBlockAndUpdate(pos, AEBlocks.IRRIGATION_CONTROLLER.get().defaultBlockState());
        var controller = (IrrigationControllerBlockEntity) world.getBlockEntity(pos);
        var downstream = namedValve(h, pos.north(2), "Beds");
        var upstream = namedValve(h, pos.north(3), "Supply zone");
        world.setBlockAndUpdate(pos.north(4), AEBlocks.IRRIGATION_BASIN.get().defaultBlockState()
                .setValue(IrrigationBasinBlock.FILLED, true));
        controller.link(downstream);
        controller.link(upstream);
        long time = world.getDayTime();
        try {
            world.setDayTime(0);
            controller.action(1, 2);
            h.assertTrue(controller.active() == 1, "The supply zone opens on a manual run");
            controller.action(1, 2);
            h.assertTrue(controller.active() == -1, "The beds cannot use a supply through a sibling valve that is closing");
        } finally { world.setDayTime(time); }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void twoSprinklersCannotDrawBelowTheReserveBetweenChecks(GameTestHelper h) {
        var world = h.getLevel();
        var pos = h.absolutePos(new BlockPos(5, 239, 5));
        world.setBlockAndUpdate(pos, AEBlocks.IRRIGATION_CONTROLLER.get().defaultBlockState());
        var controller = (IrrigationControllerBlockEntity) world.getBlockEntity(pos);
        var junction = pos.north(2);
        var valve = namedValve(h, junction, "Orchard");
        for (var source : List.of(junction.north(), junction.south())) {
            world.setBlockAndUpdate(source, AEBlocks.IRRIGATION_BASIN.get().defaultBlockState()
                    .setValue(IrrigationBasinBlock.FILLED, true));
        }
        world.setBlockAndUpdate(junction.west(), AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState());
        world.setBlockAndUpdate(junction.east(), AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState());
        var first = (IrrigationSprinklerBlockEntity) world.getBlockEntity(junction.west());
        var second = (IrrigationSprinklerBlockEntity) world.getBlockEntity(junction.east());
        long time = world.getDayTime();
        try {
            world.setDayTime(0);
            controller.link(valve);
            controller.action(0, 4);
            h.assertTrue(controller.active() == 0, "Two full basins allow a draw with a one-bucket reserve");
            first.irrigateCycle();
            second.irrigateCycle();
            h.assertTrue(first.water() > 0 && second.water() == 0, "Only one sprinkler draws down to the reserve");
            controller.action(0, 2);
            second.irrigateCycle();
            h.assertTrue(second.water() > 0, "A manual run still overrides the reserve before the next check");
        } finally { world.setDayTime(time); }
        h.succeed();
    }

    private static IrrigationValveBlockEntity namedValve(GameTestHelper h, BlockPos pos, String name) {
        var world = h.getLevel();
        world.setBlockAndUpdate(pos, AEBlocks.IRRIGATION_VALVE.get().defaultBlockState());
        var valve = (IrrigationValveBlockEntity) world.getBlockEntity(pos);
        valve.rename(name);
        return valve;
    }

    @GameTest(template = "empty")
    public static void irrigationControllerSchedulesOneZoneAndRespectsReserve(GameTestHelper h) {
        var world = h.getLevel();
        var pos = h.absolutePos(new BlockPos(4, 173, 4));
        world.setBlock(pos, AEBlocks.IRRIGATION_CONTROLLER.get().defaultBlockState(), Block.UPDATE_ALL);
        var controller = (IrrigationControllerBlockEntity) world.getBlockEntity(pos);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        var tool = new ItemStack(AEItems.SPANNER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        int i = 0;
        for (Direction side : new Direction[]{Direction.NORTH, Direction.SOUTH}) {
            var valvePos = pos.relative(side, 3);
            var source = pos.relative(side, 4);
            world.setBlock(valvePos, AEBlocks.IRRIGATION_VALVE.get().defaultBlockState(), Block.UPDATE_ALL);
            world.setBlock(source, AEBlocks.IRRIGATION_BASIN.get().defaultBlockState().setValue(IrrigationBasinBlock.FILLED, true), Block.UPDATE_ALL);
            var valve = (IrrigationValveBlockEntity) world.getBlockEntity(valvePos);
            valve.rename("Zone " + ++i);
            var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            tool.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
            SpannerItem.onRightClick(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, valvePos,
                    new BlockHitResult(Vec3.atCenterOf(valvePos), Direction.UP, valvePos, false)));
            h.assertTrue(controller.contains(valvePos) && valve.controlledBy(pos), "The Spanner links a named valve");
        }
        BlockPos north = pos.north(3), south = pos.south(3);
        long time = world.getDayTime();
        try {
            world.setDayTime(0);
            controller.action(1, 6);
            controller.action(1, 6);
            controller.checkSchedules();
            h.assertTrue(controller.active() == 0 && IrrigationValveBlock.permitsFlow(world, north, world.getBlockState(north))
                    && !IrrigationValveBlock.permitsFlow(world, south, world.getBlockState(south)), "Only one overlapping zone opens");
            controller.action(0, 4);
            h.assertTrue(controller.active() < 0 && controller.reserveWater() == 1000, "Reserve pauses a source with only one bucket");
            controller.action(1, 2);
            h.assertTrue(controller.active() == 1 && !IrrigationValveBlock.permitsFlow(world, north, world.getBlockState(north)),
                    "Manual run selects the requested zone and bypasses the reserve");
            world.setBlock(south.east(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
            controller.checkSchedules();
            h.assertTrue(controller.active() < 0, "Physical redstone still blocks a manual run");
            world.removeBlock(south.east(), false);
            var tag = new CompoundTag();
            controller.saveAdditional(tag, world.registryAccess());
            controller.loadAdditional(tag, world.registryAccess());
            controller.checkSchedules();
            h.assertTrue(controller.linked(1) && controller.startHour(1) == 0 && controller.reserveWater() == 1000
                    && controller.active() < 0 && controller.manual() < 0, "Schedules and reserve persist while manual runs expire on reload");
            world.removeBlock(pos, false);
            h.assertTrue(IrrigationValveBlock.permitsFlow(world, north, world.getBlockState(north))
                    && IrrigationValveBlock.permitsFlow(world, south, world.getBlockState(south)), "Removing the controller releases both valves");
        } finally { world.setDayTime(time); }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void discoveredCompanionsUseLiveCropsAndSurviveJournalReload(GameTestHelper h) {
        var world = h.getLevel();
        var cabbage = h.absolutePos(new BlockPos(4, 178, 4));
        var flower = cabbage.east();
        world.setBlock(cabbage.below(), Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL);
        world.setBlock(flower.below(), Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL);
        world.setBlock(cabbage, AEBlocks.CABBAGE_CROP.get().defaultBlockState(), Block.UPDATE_ALL);
        world.setBlock(flower, AEBlocks.AMBER_MARIGOLD_CROP.get().defaultBlockState().setValue(AgriCropBlock.AGE, 3), Block.UPDATE_ALL);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var journal = new ItemStack(AEItems.BREEDING_JOURNAL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, journal);
        var context = new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(cabbage), Direction.UP, cabbage, false));
        journal.useOn(context);
        h.assertTrue(com.agrievolution.client.journal.JournalSnapshot.from(journal).companions().isEmpty(), "Young marigolds cannot be recorded");
        world.setBlock(flower, world.getBlockState(flower).setValue(AgriCropBlock.AGE, 4), Block.UPDATE_ALL);
        journal.useOn(context);
        var restored = ItemStack.parseOptional(world.registryAccess(), (CompoundTag) journal.save(world.registryAccess()));
        h.assertTrue(com.agrievolution.client.journal.JournalSnapshot.from(restored).companions().contains("marigold_cabbage")
                        && CompanionPlanting.weeds(world, cabbage, CropSpecies.CABBAGE, 80) == 160,
                "An established companion halves weeds and the illustrated journal discovery is saved");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void sharedOrdersAcceptPartialDeliveriesAndPayEveryContributor(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(4, 185, 4));
        level.setBlock(pos, AEBlocks.GARDEN_ORDER_BOARD.get().defaultBlockState(), Block.UPDATE_ALL);
        var first = h.makeMockPlayer(GameType.SURVIVAL);
        var second = h.makeMockPlayer(GameType.SURVIVAL);
        first.setPos(Vec3.atCenterOf(pos));
        second.setPos(Vec3.atCenterOf(pos));
        var access = ContainerLevelAccess.create(level, pos);
        var a = new GardenWorkMenu(1, first.getInventory(), access, false);
        var b = new GardenWorkMenu(2, second.getInventory(), access, false);
        a.clickMenuButton(first, a.pageAction(GardenWorkMenu.SHARED_PAGE));
        b.clickMenuButton(second, b.pageAction(GardenWorkMenu.SHARED_PAGE));
        first.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(AEItems.TOMATO.get(), 3));
        h.assertTrue(!a.canContribute(0) && !a.clickMenuButton(first, a.contributeAction(0)),
                "Offhand supplies neither light the button nor count toward delivery");
        first.getInventory().setItem(0, new ItemStack(AEItems.TOMATO.get(), 12));
        h.assertTrue(a.canContribute(0) && a.clickMenuButton(first, a.contributeAction(0))
                        && b.remaining(0, 0) == 20 && a.claimable(0) == 0 && a.contributed(0) == 12,
                "One delivery updates the shared count without paying early");
        second.getInventory().setItem(0, new ItemStack(AEItems.TOMATO.get(), 24));
        second.getInventory().setItem(1, new ItemStack(AEItems.LETTUCE.get(), 24));
        second.getInventory().setItem(2, new ItemStack(AEItems.CUCUMBER.get(), 24));
        int contribute = b.contributeAction(0);
        h.assertTrue(b.clickMenuButton(second, contribute) && second.getInventory().getItem(0).getCount() == 4,
                "Only goods still required are taken and the second delivery completes the round");
        h.assertTrue(a.claimable(0) == 1 && b.claimable(0) > 1 && b.remaining(0, 0) == 32, "All contributors can claim after the round resets");
        h.assertTrue(!b.clickMenuButton(second, contribute) && second.getInventory().getItem(0).getCount() == 4,
                "Repeating a delivery from the completed round cannot touch the next round");
        h.assertTrue(b.canContribute(0) && b.canClaim(0) && a.contributed(0) == 0,
                "A pending reward doesn't block the next round and contributions reset");
        int claim = a.claimAction(0);
        h.assertTrue(a.clickMenuButton(first, claim) && !a.clickMenuButton(first, claim)
                        && first.getInventory().countItem(Items.EMERALD) == 1, "A reward can be claimed once per finished round");
        h.assertTrue(b.clickMenuButton(second, b.claimAction(0)) && second.getInventory().countItem(Items.EMERALD) > 1,
                "The second contributor can claim independently");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void sharedOrderRewardsSurviveClosedInventoryAndReload(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        var player = h.makeMockPlayer(GameType.CREATIVE);
        var inventory = player.getInventory();
        var orders = new SharedMarketOrders();
        inventory.setItem(0, new ItemStack(AEItems.TOMATO.get(), 32));
        inventory.setItem(1, new ItemStack(AEItems.LETTUCE.get(), 24));
        h.assertTrue(orders.contribute(0, 0, player), "Partial delivery begins the saved round");
        orders = SharedMarketOrders.load(orders.save(new CompoundTag(), registries), registries);
        h.assertTrue(orders.contributed(0, player.getUUID()) == 56 && orders.remaining(0, 2) == 24,
                "The personal contribution and the unfinished item counts survive reload");
        inventory.setItem(0, new ItemStack(AEItems.CUCUMBER.get(), 24));
        h.assertTrue(orders.contribute(0, 0, player) && orders.reward(0, player.getUUID()) == 6, "Complete the original round");
        orders = SharedMarketOrders.load(orders.save(new CompoundTag(), registries), registries);
        h.assertTrue(orders.round(0) == 1 && orders.reward(0, player.getUUID()) == 6, "Round identity and reward survive reload");

        for (int slot = 0; slot < inventory.items.size(); slot++) inventory.setItem(slot, new ItemStack(Items.DIRT, 64));
        h.assertTrue(!SharedMarketOrders.hasRewardSpace(inventory) && !orders.claim(0, player)
                        && orders.reward(0, player.getUUID()) == 6,
                "A full creative inventory cannot erase the reward");
        inventory.setItem(0, new ItemStack(Items.EMERALD, 63));
        h.assertTrue(SharedMarketOrders.hasRewardSpace(inventory) && orders.claim(0, player)
                        && inventory.getItem(0).getCount() == 64 && orders.reward(0, player.getUUID()) == 5,
                "Only the emerald that fits is withdrawn");
        inventory.setItem(1, ItemStack.EMPTY);
        h.assertTrue(orders.claim(0, player) && inventory.getItem(1).getCount() == 5
                        && !orders.claim(0, player),
                "The remainder can be claimed exactly once later");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void batchCookingUsesRecipeAlternativesAndReturnsContainers(GameTestHelper h) {
        var world = h.getLevel();
        var pos = h.absolutePos(new BlockPos(4, 192, 4));
        world.setBlock(pos, AEBlocks.BATCH_COOKING_STATION.get().defaultBlockState(), Block.UPDATE_ALL);
        var kitchen = (BatchCookingBlockEntity) world.getBlockEntity(pos);
        int tart = 0;
        while (!CookingFoods.ALL.get(tart).id().equals("strawberry_tart")) tart++;
        h.assertTrue(kitchen.enqueue(tart, 2), "A batch can be queued");
        kitchen.setItem(0, new ItemStack(Items.WHEAT, 2));
        kitchen.setItem(1, new ItemStack(Items.EGG, 2));
        kitchen.setItem(2, new ItemStack(Items.SUGAR, 2));
        kitchen.setItem(3, new ItemStack(AEItems.BERRY_JAM.get(), 2));
        for (int slot = BatchCookingBlockEntity.RETURNS; slot < BatchCookingBlockEntity.SLOTS; slot++) {
            kitchen.setItem(slot, new ItemStack(Items.DIRT, 64));
        }
        kitchen.cookOne(world);
        h.assertTrue(kitchen.status() == BatchCookingBlockEntity.Status.CONTAINERS_FULL && kitchen.getItem(3).getCount() == 2
                        && kitchen.getItem(BatchCookingBlockEntity.FOOD).isEmpty(), "Full container output leaves all ingredients and meals untouched");
        kitchen.setItem(BatchCookingBlockEntity.RETURNS, ItemStack.EMPTY);
        kitchen.cookOne(world);
        var saved = new CompoundTag();
        kitchen.saveAdditional(saved, world.registryAccess());
        kitchen.loadAdditional(saved, world.registryAccess());
        kitchen.cookOne(world);
        h.assertTrue(kitchen.order(0) == null && kitchen.getItem(3).isEmpty()
                        && kitchen.getItem(BatchCookingBlockEntity.FOOD).is(CookingFoods.ALL.get(tart).item().get())
                        && kitchen.getItem(BatchCookingBlockEntity.FOOD).getCount() == 2
                        && kitchen.getItem(BatchCookingBlockEntity.RETURNS).is(Items.GLASS_BOTTLE)
                        && kitchen.getItem(BatchCookingBlockEntity.RETURNS).getCount() == 2,
                "Both tarts use the jam substitute and return exactly two bottles after a save/load");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void batchCookingUsesFreshAlternativesWithFullContainerSlots(GameTestHelper h) {
        var world = h.getLevel();
        var pos = h.absolutePos(new BlockPos(4, 252, 4));
        world.setBlockAndUpdate(pos, AEBlocks.BATCH_COOKING_STATION.get().defaultBlockState());
        var kitchen = (BatchCookingBlockEntity) world.getBlockEntity(pos);
        int tart = 0;
        while (!CookingFoods.ALL.get(tart).id().equals("strawberry_tart")) tart++;
        h.assertTrue(kitchen.enqueue(tart, 1), "Queue one tart");
        kitchen.setItem(0, new ItemStack(Items.WHEAT));
        kitchen.setItem(1, new ItemStack(Items.EGG));
        kitchen.setItem(2, new ItemStack(Items.SUGAR));
        kitchen.setItem(3, new ItemStack(AEItems.BERRY_JAM.get()));
        kitchen.setItem(4, new ItemStack(AEItems.STRAWBERRY.get()));
        for (int slot = BatchCookingBlockEntity.RETURNS; slot < BatchCookingBlockEntity.SLOTS; slot++) {
            kitchen.setItem(slot, new ItemStack(Items.DIRT, 64));
        }
        for (int slot = BatchCookingBlockEntity.FOOD; slot < BatchCookingBlockEntity.RETURNS; slot++) {
            kitchen.setItem(slot, new ItemStack(Items.DIRT, 64));
        }
        kitchen.cookOne(world);
        h.assertTrue(kitchen.status() == BatchCookingBlockEntity.Status.OUTPUT_FULL && kitchen.getItem(4).getCount() == 1
                        && kitchen.getItem(3).getCount() == 1, "Full meal slots are reported separately without charging ingredients");
        kitchen.setItem(BatchCookingBlockEntity.FOOD, ItemStack.EMPTY);
        kitchen.cookOne(world);
        h.assertTrue(kitchen.status() == BatchCookingBlockEntity.Status.IDLE && kitchen.order(0) == null
                && kitchen.getItem(4).isEmpty() && kitchen.getItem(3).is(AEItems.BERRY_JAM.get())
                && kitchen.getItem(BatchCookingBlockEntity.FOOD).is(CookingFoods.ALL.get(tart).item().get()),
                "A full container row uses the fresh alternative, keeps the jam and returns the empty queue to idle");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void batchCookingQueuesTheDisplayedRecipeAndCancelsExactJobs(GameTestHelper h) {
        var world = h.getLevel();
        var pos = h.absolutePos(new BlockPos(4, 258, 4));
        world.setBlockAndUpdate(pos, AEBlocks.BATCH_COOKING_STATION.get().defaultBlockState());
        var kitchen = (BatchCookingBlockEntity) world.getBlockEntity(pos);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(pos));
        var menu = new BatchCookingMenu(5, player.getInventory(), kitchen);
        int displayedDish = menu.selection();
        h.assertTrue(menu.clickMenuButton(player, BatchCookingMenu.SELECT_NEXT), "Server browses to the next dish");
        int displayedAction = BatchCookingMenu.queueAction(displayedDish, 1);
        h.assertTrue(menu.selection() != displayedDish && menu.clickMenuButton(player, displayedAction)
                && kitchen.order(0).dish() == displayedDish && kitchen.order(0).count() == 4,
                "The click carries the recipe the player saw and adds the right batch size");

        for (int i = 1; i < BatchCookingBlockEntity.QUEUE_LIMIT; i++) {
            h.assertTrue(menu.clickMenuButton(player, displayedAction), "The queue can hold eight identical jobs");
        }
        h.assertTrue(menu.queueFull() && !menu.clickMenuButton(player, displayedAction), "A ninth job is rejected");
        var first = menu.cancelAction(0);
        var second = menu.cancelAction(1);
        var third = menu.cancelAction(2);
        kitchen.setItem(0, new ItemStack(Items.BREAD));
        kitchen.setItem(1, new ItemStack(AEItems.LETTUCE.get()));
        kitchen.setItem(2, new ItemStack(AEItems.TOMATO.get()));
        kitchen.setItem(3, new ItemStack(AEItems.CUCUMBER.get()));
        kitchen.setItem(4, new ItemStack(AEItems.RADISH.get()));
        kitchen.cookOne(world);
        h.assertTrue(kitchen.order(0).count() == 3 && menu.cancelAction(0) == first,
                "A job keeps its identity as individual meals finish");
        h.assertTrue(menu.clickMenuButton(player, first), "Remove the first batch");
        h.assertTrue(!menu.clickMenuButton(player, first) && menu.cancelAction(0) == second,
                "Repeating a stale click cannot cancel an identical replacement row");
        h.assertTrue(menu.clickMenuButton(player, second) && menu.cancelAction(0) == third && !menu.queueFull(),
                "A queued batch can still be cancelled after it shifts rows");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void newStationRecipesHaveUniqueCraftingMatches(GameTestHelper h) {
        var level = h.getLevel();
        var copper = new ItemStack(Items.COPPER_INGOT);
        var comparator = new ItemStack(Items.COMPARATOR);
        var bricks = new ItemStack(Items.BRICKS);
        var furnace = new ItemStack(Items.FURNACE);
        var controller = CraftingInput.of(3, 3, List.of(copper, new ItemStack(Items.REPEATER), copper,
                comparator, new ItemStack(AEItems.IRRIGATION_VALVE.get()), comparator, copper, copper, copper));
        var kitchen = CraftingInput.of(3, 3, List.of(copper, copper, copper, furnace,
                new ItemStack(AEItems.PRESERVING_STATION.get()), furnace, bricks, bricks, bricks));
        for (var entry : java.util.Map.of("irrigation_controller", controller, "batch_cooking_station", kitchen).entrySet()) {
            var matches = level.getRecipeManager().getRecipesFor(RecipeType.CRAFTING, entry.getValue(), level);
            h.assertTrue(matches.size() == 1 && matches.getFirst().id().equals(ResourceLocation.fromNamespaceAndPath("agri_evolution", entry.getKey())),
                    "New station has a unique shaped recipe in Minecraft 1.21.1");
        }
        h.succeed();
    }
}
