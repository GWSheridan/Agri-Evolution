package com.agrievolution.content;

import com.agrievolution.genetics.CropSpecies;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class VerticalGreenhouseGameTests {
    private static VerticalGreenhouseBlockEntity place(GameTestHelper helper, BlockPos base) {
        var level = helper.getLevel();
        level.setBlock(base.below(), Blocks.STONE.defaultBlockState(), 3);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var item = new ItemStack(AEItems.VERTICAL_GREENHOUSE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, item);
        var hit = new BlockHitResult(Vec3.atBottomCenterOf(base), Direction.UP, base.below(), false);
        helper.assertTrue(item.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(), "Column places from its item");
        var greenhouse = (VerticalGreenhouseBlockEntity) level.getBlockEntity(base);
        helper.assertTrue(item.isEmpty() && greenhouse != null && greenhouse.complete(), "One item creates a complete column");
        return greenhouse;
    }

    private static void seconds(GameTestHelper helper, VerticalGreenhouseBlockEntity greenhouse, int seconds) {
        for (int i = 0; i < seconds; i++) greenhouse.runSecond(helper.getLevel());
    }

    @GameTest(template = "empty")
    public static void greenhousePlacementAndDismantlingRespectChunkBorders(GameTestHelper helper) {
        var level = helper.getLevel();
        var chunk = new ChunkPos(helper.absolutePos(new BlockPos(5120, 100, 4)));
        var base = new BlockPos(chunk.getMaxBlockX(), 100, chunk.getMiddleBlockZ());
        var outside = base.east();
        var pipePos = base.west();
        var supply = pipePos.west();
        int quiet = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        level.getChunkAt(base);
        level.setBlock(base.below(), Blocks.STONE.defaultBlockState(), quiet);
        level.setBlock(supply, AEBlocks.IRRIGATION_BASIN.get().defaultBlockState().setValue(IrrigationBasinBlock.FILLED, true), quiet);
        level.setBlock(pipePos, AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(), quiet);
        helper.assertFalse(level.hasChunkAt(outside), "The next chunk starts unloaded");

        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var item = new ItemStack(AEItems.VERTICAL_GREENHOUSE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, item);
        var hit = new BlockHitResult(Vec3.atBottomCenterOf(base), Direction.UP, base.below(), false);
        helper.assertTrue(item.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction() && item.isEmpty(),
                "The normal item places all three sections");
        var greenhouse = (VerticalGreenhouseBlockEntity) level.getBlockEntity(base);
        var pipeArm = IrrigationSprinklerBlock.connection(Direction.EAST);
        helper.assertTrue(greenhouse != null && greenhouse.complete() && level.getBlockState(pipePos).getValue(pipeArm)
                        && !level.hasChunkAt(outside), "The intake connects without loading the chunk beyond the column");

        var invalidations = new AtomicInteger();
        var cache = BlockCapabilityCache.create(Capabilities.FluidHandler.BLOCK, level, base, Direction.NORTH,
                () -> true, invalidations::incrementAndGet);
        var port = cache.getCapability();
        greenhouse.cycleWaterIntake();
        seconds(helper, greenhouse, 1);
        helper.assertTrue(port != null && port.getFluidInTank(0).getAmount() == 1000
                        && !level.getBlockState(supply).getValue(IrrigationBasinBlock.FILLED),
                "The new connection draws one bucket into the real tank");
        helper.assertFalse(level.hasChunkAt(outside), "A pipe refill leaves the next chunk unloaded");

        greenhouse.setItem(0, new ItemStack(Items.CARROT));
        helper.assertFalse(level.hasChunkAt(outside), "Inserting a seed leaves the next chunk unloaded");
        level.setBlock(base.above(2), Blocks.AIR.defaultBlockState(), quiet);
        helper.assertFalse(level.hasChunkAt(outside), "Dismantling leaves the next chunk unloaded");
        helper.assertTrue(level.isEmptyBlock(base) && level.isEmptyBlock(base.above()) && level.isEmptyBlock(base.above(2))
                        && !level.getBlockState(pipePos).getValue(pipeArm), "Removing the crown clears the stack and its intake arm");
        helper.assertTrue(invalidations.get() > 0 && cache.getCapability() == null && port.getFluidInTank(0).isEmpty()
                        && port.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE) == 0,
                "External pumps lose the dismantled tank and old handles cannot use it");
        // The distant chunk is not tracking entities. The normal break test checks the dropped item.
        helper.assertTrue(greenhouse.getItem(0).isEmpty(), "Dismantling releases the tray item from the old inventory");

        var innerBase = base.west();
        var state = AEBlocks.VERTICAL_GREENHOUSE.get().defaultBlockState();
        level.setBlock(base, Blocks.STONE.defaultBlockState(), quiet);
        for (int section = 0; section <= 2; section++) {
            level.setBlock(innerBase.above(section), state.setValue(VerticalGreenhouseBlock.SECTION, section), quiet);
        }
        var innerGreenhouse = (VerticalGreenhouseBlockEntity) level.getBlockEntity(innerBase);
        helper.assertTrue(!level.hasChunkAt(outside) && innerGreenhouse.addBucket() && !level.hasChunkAt(outside),
                "Filling a tank does not scan through the stone into the next chunk");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void structureTransformsKeepTheThreeGreenhouseSectionsAligned(GameTestHelper helper) {
        var greenhouse = AEBlocks.VERTICAL_GREENHOUSE.get();
        for (int section = 0; section <= 2; section++) {
            var state = greenhouse.defaultBlockState().setValue(VerticalGreenhouseBlock.SECTION, section);
            var rotated = state.rotate(Rotation.CLOCKWISE_90);
            var mirrored = rotated.mirror(Mirror.FRONT_BACK);
            helper.assertTrue(VerticalGreenhouseBlock.matches(rotated, greenhouse, Direction.EAST, section)
                            && VerticalGreenhouseBlock.matches(mirrored, greenhouse, Direction.WEST, section),
                    "Rotation and mirroring turn section " + section + " while keeping its height in the column");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void flowerTraysProduceDyeAlongsideDedicatedResourceTrays(GameTestHelper helper) {
        var greenhouse = place(helper, helper.absolutePos(new BlockPos(4, 241, 4)));
        var species = List.of(CropSpecies.ECLIPSE_LOTUS, CropSpecies.MOON_ORCHID, CropSpecies.SUNFIRE_LILY,
                CropSpecies.TWILIGHT_SAGE, CropSpecies.ENDER_PEARL, CropSpecies.AMETHYST);
        var produce = List.of(Items.PINK_DYE, Items.LIGHT_BLUE_DYE, Items.YELLOW_DYE,
                Items.MAGENTA_DYE, Items.ENDER_PEARL, Items.AMETHYST_SHARD);
        for (int tray = 0; tray < species.size(); tray++) {
            greenhouse.setItem(tray, new ItemStack(AEItems.seedFor(species.get(tray))));
        }
        greenhouse.toggle(1); // Isolate tray harvests from the shared tank's breeding water use.
        helper.assertTrue(greenhouse.addBucket() && greenhouse.addBucket(), "Two buckets water all six trays");
        seconds(helper, greenhouse, 145);
        for (var item : produce) {
            int count = 0;
            for (int slot = VerticalGreenhouseBlockEntity.HARVEST; slot < VerticalGreenhouseBlockEntity.OFFSPRING; slot++) {
                var stack = greenhouse.getItem(slot);
                if (stack.is(item)) count += stack.getCount();
            }
            helper.assertTrue(count == 1, "Expected one " + item + ", got " + count);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void placementChecksClearanceAndBreaksOnce(GameTestHelper helper) {
        var level = helper.getLevel();
        var base = helper.absolutePos(new BlockPos(4, 190, 4));
        level.setBlock(base.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(base.above(2), Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var item = new ItemStack(AEItems.VERTICAL_GREENHOUSE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, item);
        var hit = new BlockHitResult(Vec3.atBottomCenterOf(base), Direction.UP, base.below(), false);
        helper.assertFalse(item.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(), "Obstructed column cannot be placed");
        helper.assertTrue(item.getCount() == 1 && level.isEmptyBlock(base) && level.getBlockState(base.above(2)).is(Blocks.DIAMOND_BLOCK),
                "Failed placement leaves the original blocks and item intact");
        level.removeBlock(base.above(2), false);
        var greenhouse = place(helper, base);
        greenhouse.setItem(0, new ItemStack(Items.CARROT));
        greenhouse.setItem(VerticalGreenhouseBlockEntity.HARVEST, new ItemStack(Items.WHEAT, 3));
        level.destroyBlock(base.above(2), true);
        helper.assertTrue(level.isEmptyBlock(base) && level.isEmptyBlock(base.above()) && level.isEmptyBlock(base.above(2)), "Breaking the crown dismantles the column");
        var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(base).inflate(4));
        helper.assertTrue(drops.stream().filter(drop -> drop.getItem().is(AEItems.VERTICAL_GREENHOUSE.get())).mapToInt(drop -> drop.getItem().getCount()).sum() == 1,
                "Dismantling yields one machine");
        helper.assertTrue(drops.stream().filter(drop -> drop.getItem().is(Items.CARROT)).mapToInt(drop -> drop.getItem().getCount()).sum() == 1
                && drops.stream().filter(drop -> drop.getItem().is(Items.WHEAT)).mapToInt(drop -> drop.getItem().getCount()).sum() == 3, "Tray and output items drop once");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void waterOnlyGrowAndBreedRetainsParents(GameTestHelper helper) {
        var greenhouse = place(helper, helper.absolutePos(new BlockPos(4, 195, 4)));
        var wild = new ItemStack(AEItems.WILD_SEED.get());
        var flax = new ItemStack(AEItems.AZURE_FLAX_SEED.get());
        var beet = new ItemStack(Items.BEETROOT_SEEDS);
        greenhouse.setItem(0, wild);
        greenhouse.setItem(1, flax);
        greenhouse.setItem(2, beet);
        greenhouse.setItem(3, beet.copy());
        greenhouse.setItem(4, new ItemStack(Items.BAMBOO));
        seconds(helper, greenhouse, 100);
        helper.assertTrue(greenhouse.data.get(VerticalGreenhouseBlockEntity.PROGRESS_START) == 0, "Dry trays do not advance");
        for (int i = 0; i < 8; i++) helper.assertTrue(greenhouse.addBucket(), "Water bucket fills the tank");
        seconds(helper, greenhouse, 190);
        helper.assertTrue(greenhouse.getItem(0).getCount() == 1 && greenhouse.getItem(1).getCount() == 1
                && greenhouse.getItem(2).getCount() == 1 && greenhouse.getItem(3).getCount() == 1, "Both jobs retain every planted parent");
        int wheat = 0, beetroot = 0, bamboo = 0, agriSeeds = 0, nativeSeeds = 0;
        for (int i = VerticalGreenhouseBlockEntity.HARVEST; i < VerticalGreenhouseBlockEntity.OFFSPRING; i++) {
            var stack = greenhouse.getItem(i);
            if (stack.is(Items.WHEAT)) wheat += stack.getCount();
            if (stack.is(Items.BEETROOT)) beetroot += stack.getCount();
            if (stack.is(Items.BAMBOO)) bamboo += stack.getCount();
        }
        for (int i = VerticalGreenhouseBlockEntity.OFFSPRING; i < VerticalGreenhouseBlockEntity.SLOTS; i++) {
            var stack = greenhouse.getItem(i);
            if (stack.getItem() instanceof SpeciesSeedItem) agriSeeds += stack.getCount();
            if (stack.is(Items.BEETROOT_SEEDS)) nativeSeeds += stack.getCount();
        }
        helper.assertTrue(wheat >= 2 && beetroot >= 4, "Agri and vanilla trays repeatedly harvest together");
        helper.assertTrue(bamboo == 4 && greenhouse.getItem(4).getCount() == 1, "A tagged datapack crop uses its configured harvest");
        helper.assertTrue(agriSeeds > 0 && nativeSeeds > 0 && greenhouse.water() < 8000, "Both kinds of breeding run and spend water");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pipeFillAndFullOutputPauseWithoutLoss(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(4, 200, 4));
        var greenhouse = place(helper, pos);
        var pipe = pos.east();
        var basin = pos.east(2);
        level.setBlock(pipe, AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(), 3);
        level.setBlock(basin, AEBlocks.IRRIGATION_BASIN.get().defaultBlockState().setValue(IrrigationBasinBlock.FILLED, true), 3);
        greenhouse.toggle(1);
        greenhouse.setItem(0, new ItemStack(Items.WHEAT_SEEDS));
        for (int i = VerticalGreenhouseBlockEntity.HARVEST; i < VerticalGreenhouseBlockEntity.OFFSPRING; i++) {
            greenhouse.setItem(i, new ItemStack(Items.STONE, 64));
        }
        seconds(helper, greenhouse, 100);
        helper.assertTrue(!level.getBlockState(basin).getValue(IrrigationBasinBlock.FILLED) && greenhouse.water() == 1000, "A pipe transfers exactly one bucket");
        helper.assertTrue(greenhouse.data.get(2) == VerticalGreenhouseBlockEntity.Status.OUTPUT_FULL.ordinal(), "Full output pauses a finished harvest");
        var saved = new CompoundTag();
        greenhouse.saveAdditional(saved, level.registryAccess());
        greenhouse.loadAdditional(saved, level.registryAccess());
        greenhouse.setItem(VerticalGreenhouseBlockEntity.HARVEST, ItemStack.EMPTY);
        seconds(helper, greenhouse, 1);
        helper.assertTrue(greenhouse.water() == 750 && greenhouse.getItem(VerticalGreenhouseBlockEntity.HARVEST).is(Items.WHEAT),
                "The saved harvest finishes once there is room and costs one watering cycle");
        helper.assertTrue(greenhouse.getSlotsForFace(Direction.DOWN).length == 18 && !greenhouse.canTakeItemThroughFace(0, greenhouse.getItem(0), Direction.DOWN),
                "Output automation cannot remove planted parents");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fixedPairBreedsOnlySelectedTrays(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(4, 205, 4));
        var greenhouse = place(helper, pos);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 1.5);
        var menu = new VerticalGreenhouseMenu(1, player.getInventory(), greenhouse);
        var beet = new ItemStack(Items.BEETROOT_SEEDS);
        greenhouse.setItem(0, new ItemStack(AEItems.WILD_SEED.get()));
        greenhouse.setItem(1, new ItemStack(AEItems.AZURE_FLAX_SEED.get()));
        greenhouse.setItem(2, beet);
        greenhouse.setItem(3, beet.copy());
        greenhouse.setItem(4, new ItemStack(Items.WHEAT_SEEDS));
        greenhouse.setItem(5, new ItemStack(Items.CARROT));
        for (int bucket = 0; bucket < 3; bucket++) greenhouse.addBucket();

        helper.assertFalse(menu.fixedPair(), "New machines start in auto pair mode");
        helper.assertFalse(menu.clickMenuButton(player, VerticalGreenhouseMenu.CHOOSE_A), "Auto mode ignores manual assignments");
        helper.assertTrue(menu.clickMenuButton(player, VerticalGreenhouseMenu.TOGGLE_PAIRING) && menu.fixedPair(), "Player can switch to fixed pair");
        menu.clickMenuButton(player, VerticalGreenhouseMenu.TOGGLE_GROW);
        menu.clickMenuButton(player, VerticalGreenhouseMenu.CHOOSE_A + 3);
        menu.clickMenuButton(player, VerticalGreenhouseMenu.CHOOSE_B + 3);
        helper.assertTrue(menu.selectedA() == 0 && menu.selectedB() == 4, "One tray cannot occupy both parent positions");
        menu.clickMenuButton(player, VerticalGreenhouseMenu.CHOOSE_B);
        menu.clickMenuButton(player, VerticalGreenhouseMenu.CHOOSE_A + 3);
        seconds(helper, greenhouse, 40);
        helper.assertTrue(menu.breedStatus() == VerticalGreenhouseBlockEntity.Status.NO_PAIR
                        && greenhouse.water() == 3000 && greenhouse.getItem(VerticalGreenhouseBlockEntity.OFFSPRING).isEmpty(),
                "Incompatible selections wait without falling back to other compatible trays or spending water");

        menu.clickMenuButton(player, VerticalGreenhouseMenu.CHOOSE_B + 2);
        menu.clickMenuButton(player, VerticalGreenhouseMenu.TOGGLE_GROW);
        seconds(helper, greenhouse, 12);
        for (int tray = 0; tray < VerticalGreenhouseBlockEntity.TRAYS; tray++) {
            helper.assertTrue(menu.trayProgress(tray) > 0, "Every planted tray grows independently of parent selection");
        }
        menu.clickMenuButton(player, VerticalGreenhouseMenu.TOGGLE_GROW);
        int progress = menu.breedProgress();
        var registries = helper.getLevel().registryAccess();
        var saved = new CompoundTag();
        greenhouse.saveAdditional(saved, registries);
        greenhouse.loadAdditional(saved, registries);
        helper.assertTrue(menu.fixedPair() && menu.selectedA() == 4 && menu.selectedB() == 3 && menu.breedProgress() == progress,
                "Mode, assigned tray order and active progress survive reload");
        seconds(helper, greenhouse, 58);
        var offspring = greenhouse.getItem(VerticalGreenhouseBlockEntity.OFFSPRING);
        helper.assertTrue(offspring.is(Items.BEETROOT_SEEDS) && offspring.getCount() == 2 && greenhouse.water() == 2000,
                "Fixed pair produces two cycles from the selected parents at 500 mB each");
        seconds(helper, greenhouse, 7);
        greenhouse.setItem(3, ItemStack.EMPTY);
        seconds(helper, greenhouse, 40);
        helper.assertTrue(menu.breedProgress() == 0 && menu.breedStatus() == VerticalGreenhouseBlockEntity.Status.NO_PAIR
                        && menu.selectedA() == 4 && offspring.getCount() == 2 && greenhouse.water() == 2000,
                "Removing a parent clears progress and waits for the selected tray to refill");
        menu.clickMenuButton(player, VerticalGreenhouseMenu.TOGGLE_PAIRING);
        seconds(helper, greenhouse, 1);
        helper.assertTrue(!menu.fixedPair() && menu.parentA() == 1 && menu.parentB() == 2, "Auto mode resumes rotation");
        menu.clickMenuButton(player, VerticalGreenhouseMenu.TOGGLE_PAIRING);
        helper.assertTrue(menu.selectedA() == 4 && menu.selectedB() == 3, "Switching modes remembers the fixed assignments");
        menu.clickMenuButton(player, VerticalGreenhouseMenu.CHOOSE_A + 3);
        helper.assertTrue(menu.selectedA() == 0 && menu.selectedB() == 3, "Clicking an assigned parent clears it");
        saved.remove("FixedPair");
        saved.remove("SelectedA");
        saved.remove("SelectedB");
        greenhouse.loadAdditional(saved, registries);
        helper.assertTrue(!menu.fixedPair() && menu.selectedA() == 0 && menu.selectedB() == 0,
                "Existing saves without pairing controls default to auto mode");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void waterIntakeModesRespectSupplyAndCapacity(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(4, 210, 4));
        var greenhouse = place(helper, pos);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 1.5);
        var menu = new VerticalGreenhouseMenu(1, player.getInventory(), greenhouse);
        var valvePos = pos.east();
        var basinPos = pos.east(3);
        var redstonePos = valvePos.above();
        var basin = AEBlocks.IRRIGATION_BASIN.get().defaultBlockState().setValue(IrrigationBasinBlock.FILLED, true);
        level.setBlock(valvePos, AEBlocks.IRRIGATION_VALVE.get().defaultBlockState(), 3);
        level.setBlock(pos.east(2), AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(), 3);
        level.setBlock(basinPos, basin, 3);

        seconds(helper, greenhouse, 1);
        helper.assertTrue(menu.waterIntake() == VerticalGreenhouseBlockEntity.WaterIntake.DEMAND
                        && menu.waterStatus() == VerticalGreenhouseBlockEntity.WaterStatus.WAITING && greenhouse.water() == 0,
                "The default mode keeps an idle greenhouse from taking water");
        helper.assertTrue(menu.clickMenuButton(player, VerticalGreenhouseMenu.CYCLE_WATER_INTAKE)
                        && menu.waterIntake() == VerticalGreenhouseBlockEntity.WaterIntake.KEEP_FULL,
                "Clicking the intake control enables idle refill");
        level.setBlock(redstonePos, Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        seconds(helper, greenhouse, 1);
        helper.assertTrue(menu.waterStatus() == VerticalGreenhouseBlockEntity.WaterStatus.BLOCKED_VALVE
                        && greenhouse.water() == 0 && level.getBlockState(basinPos).getValue(IrrigationBasinBlock.FILLED),
                "A redstone-powered valve stops the draw and reports the obstruction");
        level.removeBlock(redstonePos, false);
        seconds(helper, greenhouse, 1);
        helper.assertTrue(menu.waterStatus() == VerticalGreenhouseBlockEntity.WaterStatus.REFILLING
                        && greenhouse.water() == 1000 && !level.getBlockState(basinPos).getValue(IrrigationBasinBlock.FILLED),
                "Opening the route transfers one bucket from a finite source while idle");
        seconds(helper, greenhouse, 1);
        helper.assertTrue(menu.waterStatus() == VerticalGreenhouseBlockEntity.WaterStatus.EMPTY_SOURCE && greenhouse.water() == 1000,
                "An empty basin reports that the source needs water");
        level.removeBlock(basinPos, false);
        seconds(helper, greenhouse, 1);
        helper.assertTrue(menu.waterStatus() == VerticalGreenhouseBlockEntity.WaterStatus.NO_SOURCE, "A disconnected basin reports no source");
        level.setBlock(basinPos, basin, 3);

        var saved = new CompoundTag();
        greenhouse.saveAdditional(saved, level.registryAccess());
        greenhouse.loadAdditional(saved, level.registryAccess());
        helper.assertTrue(menu.waterIntake() == VerticalGreenhouseBlockEntity.WaterIntake.KEEP_FULL, "Keep full survives reload");
        menu.clickMenuButton(player, VerticalGreenhouseMenu.CYCLE_WATER_INTAKE);
        seconds(helper, greenhouse, 1);
        helper.assertTrue(menu.waterStatus() == VerticalGreenhouseBlockEntity.WaterStatus.OFF && greenhouse.water() == 1000
                        && level.getBlockState(basinPos).getValue(IrrigationBasinBlock.FILLED), "Off leaves water in the connected source");
        var pumpInput = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.WEST);
        helper.assertTrue(greenhouse.addBucket() && pumpInput != null
                        && pumpInput.fill(new FluidStack(Fluids.WATER, 12500), FluidAction.EXECUTE) == 12500 && greenhouse.water() == 14500,
                "Buckets and external pumps can fill the tank with automatic intake off");

        menu.clickMenuButton(player, VerticalGreenhouseMenu.CYCLE_WATER_INTAKE);
        helper.assertTrue(menu.waterIntake() == VerticalGreenhouseBlockEntity.WaterIntake.DEMAND, "Off cycles back to demand");
        menu.clickMenuButton(player, VerticalGreenhouseMenu.CYCLE_WATER_INTAKE);
        seconds(helper, greenhouse, 1);
        helper.assertTrue(greenhouse.water() == 15500, "Keep full draws a whole bucket when it fits");
        level.setBlock(basinPos, basin, 3);
        seconds(helper, greenhouse, 1);
        helper.assertTrue(menu.waterStatus() == VerticalGreenhouseBlockEntity.WaterStatus.READY
                        && greenhouse.water() == 15500 && level.getBlockState(basinPos).getValue(IrrigationBasinBlock.FILLED),
                "Less than one bucket of headroom leaves the connected supply intact");

        saved.remove("WaterIntake");
        greenhouse.loadAdditional(saved, level.registryAccess());
        helper.assertTrue(menu.waterIntake() == VerticalGreenhouseBlockEntity.WaterIntake.DEMAND, "Existing saves default to demand");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void greenhouseIntakeExplainsUnavailablePipeSupplies(GameTestHelper h) {
        var level = h.getLevel();
        var chunk = new ChunkPos(h.absolutePos(new BlockPos(3072, 100, 4)));
        var edge = new BlockPos(chunk.getMaxBlockX(), 100, chunk.getMiddleBlockZ());
        var base = edge.west(4);
        var greenhouse = place(h, base);
        greenhouse.cycleWaterIntake();
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var menu = new VerticalGreenhouseMenu(1, player.getInventory(), greenhouse);
        var pipe = AEBlocks.IRRIGATION_PIPE.get().defaultBlockState();
        var inlet = base.east();
        var supply = inlet.east();
        int quietUpdate = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        level.setBlock(inlet, pipe, quietUpdate);
        level.setBlock(supply, AEBlocks.IRRIGATION_BASIN.get().defaultBlockState()
                .setValue(IrrigationBasinBlock.FILLED, true), quietUpdate);
        IrrigationPipeBlock.toggle(level, inlet, Direction.EAST);
        seconds(h, greenhouse, 1);
        h.assertTrue(menu.waterStatus() == VerticalGreenhouseBlockEntity.WaterStatus.BLOCKED_PORT
                && level.getBlockState(supply).getValue(IrrigationBasinBlock.FILLED), "A disabled port leaves its basin water in place");
        var inspected = FarmInspection.describe(level, base, true, false);
        h.assertTrue(inspected.contains("Water: 0 / 16000 mB") && inspected.contains("Pipe supply: Disabled east pipe port")
                        && inspected.equals(FarmInspection.describe(level, base.above(2), true, false))
                        && level.getBlockState(supply).getValue(IrrigationBasinBlock.FILLED),
                "The Spanner can read the same tank and supply from any section without moving water");
        h.assertTrue(IrrigationNetwork.trace(level, base.above(2)).equals(IrrigationNetwork.trace(level, base)),
                "A trace from the roof starts at the actual intake and finds the same blocked port");
        var orphan = base.north().above(2);
        level.setBlock(orphan, level.getBlockState(base.above(2)), quietUpdate);
        var incomplete = IrrigationNetwork.trace(level, orphan);
        h.assertTrue(incomplete.flow() == IrrigationNetwork.Flow.INCOMPLETE_GREENHOUSE
                        && incomplete.blocks().getFirst().equals(orphan), "A missing controller is reported at the clicked section");

        IrrigationPipeBlock.toggle(level, inlet, Direction.EAST);
        level.setBlock(supply, pipe, quietUpdate);
        level.setBlock(edge.west(), pipe, quietUpdate);
        level.setBlock(edge, pipe.setValue(IrrigationSprinklerBlock.connection(Direction.EAST), true), quietUpdate);
        h.assertFalse(level.hasChunkAt(edge.east()), "The neighboring chunk must start unloaded");
        seconds(h, greenhouse, 1);
        h.assertTrue(menu.waterStatus() == VerticalGreenhouseBlockEntity.WaterStatus.UNLOADED_SUPPLY
                && !level.hasChunkAt(edge.east()), "An unloaded supply is reported without loading the other chunk");

        var tankPart = AEBlocks.WATER_RESERVOIR.get().defaultBlockState();
        level.setBlock(edge.west(), tankPart.setValue(WaterReservoirBlock.X, 0), quietUpdate);
        level.setBlock(edge, tankPart, quietUpdate);
        seconds(h, greenhouse, 1);
        h.assertTrue(menu.waterStatus() == VerticalGreenhouseBlockEntity.WaterStatus.UNLOADED_RESERVOIR
                && FarmInspection.describe(level, base, true, false).contains("Pipe supply: " + ReservoirStructure.Status.UNLOADED.message)
                && !level.hasChunkAt(edge.east()), "A partially unloaded reservoir gets its own intake status");

        level.setBlock(supply, AEBlocks.WATER_RESERVOIR.get().defaultBlockState().setValue(WaterReservoirBlock.X, 0), quietUpdate);
        seconds(h, greenhouse, 1);
        h.assertTrue(menu.waterStatus() == VerticalGreenhouseBlockEntity.WaterStatus.INCOMPLETE_RESERVOIR
                && greenhouse.water() == 0, "An incomplete shell is distinct from a disconnected pipe");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void targetBatchCountsOnlyRequestedSpeciesAndGenes(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(5, 220, 5));
        var greenhouse = place(helper, pos);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(pos.south()));
        var menu = new VerticalGreenhouseMenu(1, player.getInventory(), greenhouse);
        menu.clickMenuButton(player, VerticalGreenhouseMenu.TOGGLE_GROW);
        menu.clickMenuButton(player, VerticalGreenhouseMenu.CYCLE_BATCH);
        var tomato = new ItemStack(AEItems.TOMATO_SEED.get());
        menu.setCarried(tomato);
        helper.assertTrue(menu.clickMenuButton(player, VerticalGreenhouseMenu.TARGET_SEED) && menu.getCarried() == tomato,
                "A cursor seed sets the species without consuming the sample");
        for (int i = 0; i < 8; i++) menu.clickMenuButton(player, VerticalGreenhouseMenu.TARGET_GENE + 1);
        greenhouse.addBucket();
        greenhouse.addBucket();
        greenhouse.setItem(0, tomato.copy());
        greenhouse.setItem(1, tomato.copy());
        seconds(helper, greenhouse, 35);
        helper.assertTrue(menu.batchCount() == 0 && greenhouse.water() == 1500
                        && greenhouse.getItem(VerticalGreenhouseBlockEntity.REJECTS).is(AEItems.TOMATO_SEED.get()),
                "A seed below the minimum is paid for and routed to the reject buffer without completing the batch");
        var saved = new CompoundTag();
        greenhouse.saveAdditional(saved, level.registryAccess());
        greenhouse.loadAdditional(saved, level.registryAccess());
        helper.assertTrue(menu.targetSeed() == CropSpecies.TOMATO.ordinal() + 1 && menu.minimumGene(1) == 80,
                "Species and minimums survive reloading");
        com.agrievolution.genetics.GenomeStorage.putOnSeed(tomato, new com.agrievolution.genetics.CropGenome(100, 100, 100));
        greenhouse.setItem(0, tomato.copy());
        greenhouse.setItem(1, tomato.copy());
        seconds(helper, greenhouse, 80);
        helper.assertTrue(menu.batchCount() == 1 && greenhouse.water() == 1000
                        && greenhouse.getItem(VerticalGreenhouseBlockEntity.OFFSPRING).is(AEItems.TOMATO_SEED.get())
                        && menu.breedStatus() == VerticalGreenhouseBlockEntity.Status.BATCH_COMPLETE,
                "The first qualifying seed completes the batch and preserves the rejected seed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void routedBatchStopsAfterSuccessfulStorage(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(4, 215, 4));
        var greenhouse = place(helper, pos);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 1.5);
        var menu = new VerticalGreenhouseMenu(1, player.getInventory(), greenhouse);
        int east = VerticalGreenhouseMenu.CYCLE_ROUTE + 2;
        menu.clickMenuButton(player, VerticalGreenhouseMenu.CYCLE_ROUTE);
        menu.clickMenuButton(player, VerticalGreenhouseMenu.CYCLE_ROUTE);
        menu.clickMenuButton(player, east);
        menu.clickMenuButton(player, east);
        menu.clickMenuButton(player, VerticalGreenhouseMenu.CYCLE_BATCH);
        greenhouse.setItem(0, new ItemStack(Items.BEETROOT_SEEDS));
        greenhouse.setItem(1, new ItemStack(Items.BEETROOT_SEEDS));
        greenhouse.setItem(VerticalGreenhouseBlockEntity.HARVEST, new ItemStack(Items.WHEAT, 5));
        greenhouse.addBucket();
        greenhouse.addBucket();
        for (int slot = VerticalGreenhouseBlockEntity.OFFSPRING; slot < VerticalGreenhouseBlockEntity.SLOTS; slot++) {
            greenhouse.setItem(slot, new ItemStack(Items.DIRT, 64));
        }
        seconds(helper, greenhouse, 35);
        helper.assertTrue(menu.batchCount() == 0 && menu.breedStatus() == VerticalGreenhouseBlockEntity.Status.OUTPUT_FULL,
                "A blocked output cannot count or charge for offspring");
        for (int slot = VerticalGreenhouseBlockEntity.OFFSPRING; slot < VerticalGreenhouseBlockEntity.SLOTS; slot++) {
            greenhouse.setItem(slot, ItemStack.EMPTY);
        }
        level.setBlock(pos.east(), Blocks.CHEST.defaultBlockState(), 3);
        var chest = (net.minecraft.world.Container) level.getBlockEntity(pos.east());
        seconds(helper, greenhouse, 1);
        helper.assertTrue(menu.batchCount() == 1 && menu.breedStatus() == VerticalGreenhouseBlockEntity.Status.BATCH_COMPLETE
                        && greenhouse.water() == 1500 && chest.getItem(0).is(Items.BEETROOT_SEEDS),
                "Completed offspring counts once even when exported immediately");
        var down = greenhouse.getSlotsForFace(Direction.DOWN);
        helper.assertTrue(down.length == 9 && down[0] == VerticalGreenhouseBlockEntity.HARVEST
                        && !greenhouse.canTakeItemThroughFace(VerticalGreenhouseBlockEntity.OFFSPRING, chest.getItem(0), Direction.DOWN),
                "Bottom hopper can only access the harvest row");
        var saved = new CompoundTag();
        greenhouse.saveAdditional(saved, level.registryAccess());
        greenhouse.loadAdditional(saved, level.registryAccess());
        seconds(helper, greenhouse, 90);
        helper.assertTrue(menu.batchCount() == 1 && menu.batchTarget() == 1 && chest.getItem(0).getCount() == 1
                        && chest.getItem(1).isEmpty() && greenhouse.getItem(VerticalGreenhouseBlockEntity.HARVEST).is(Items.WHEAT)
                        && greenhouse.getItem(VerticalGreenhouseBlockEntity.HARVEST + 1).is(Items.BEETROOT),
                "Batch and routing survive reload and the trays keep producing crops");
        menu.clickMenuButton(player, VerticalGreenhouseMenu.RESET_BATCH);
        helper.assertTrue(menu.batchCount() == 0 && menu.batchTarget() == 1, "Reset starts another batch at the same target");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vaultRoutesOnlyMatchingOffspring(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(4, 227, 4));
        var greenhouse = place(helper, pos);
        level.setBlock(pos.east(), AEBlocks.SEED_VAULT.get().defaultBlockState(), 3);
        var vault = (SeedVaultBlockEntity) level.getBlockEntity(pos.east());
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        var menu = new VerticalGreenhouseMenu(1, player.getInventory(), greenhouse);
        int east = VerticalGreenhouseMenu.CYCLE_ROUTE + 2;
        for (int i = 0; i < 3; i++) menu.clickMenuButton(player, east);
        var crop = com.agrievolution.genetics.CropSpecies.WILD;
        var seed = new ItemStack(AEItems.seedFor(crop), 3);
        var wrong = new ItemStack(AEItems.seedFor(com.agrievolution.genetics.CropSpecies.LETTUCE));
        var vaultMenu = new SeedVaultMenu(2, player.getInventory(), vault);
        vaultMenu.setCarried(seed.copyWithCount(1));
        helper.assertTrue(vaultMenu.clickMenuButton(player, SeedVaultMenu.SET_SPECIES), "Filter can be set from the seed on the cursor");
        for (int i = 0; i < 3; i++) vaultMenu.clickMenuButton(player, SeedVaultMenu.CYCLE_MINIMUM);
        var tag = new CompoundTag();
        vault.saveAdditional(tag, level.registryAccess());
        vault.loadAdditional(tag, level.registryAccess());
        helper.assertTrue(vaultMenu.intakeSpecies() == crop && vaultMenu.minimum(0) == 30, "Intake filters persist");
        greenhouse.setItem(VerticalGreenhouseBlockEntity.HARVEST, new ItemStack(Items.WHEAT));
        greenhouse.setItem(VerticalGreenhouseBlockEntity.OFFSPRING, wrong);
        greenhouse.setItem(VerticalGreenhouseBlockEntity.OFFSPRING + 1, seed);
        seconds(helper, greenhouse, 1);
        helper.assertTrue(vault.isEmpty() && seed.getCount() == 3, "Low genetics and wrong species remain in the greenhouse");
        com.agrievolution.genetics.GenomeStorage.putOnSeed(seed, com.agrievolution.genetics.CropGenome.elite());
        vault.setItem(0, seed.copyWithCount(1));
        vault.toggleLock(0);
        seconds(helper, greenhouse, 2);
        helper.assertTrue(vault.getItem(0).getCount() == 1 && vault.getItem(1).getCount() == 2
                        && greenhouse.getItem(VerticalGreenhouseBlockEntity.HARVEST).is(Items.WHEAT)
                        && greenhouse.getItem(VerticalGreenhouseBlockEntity.OFFSPRING) == wrong && seed.getCount() == 1,
                "Vault merges matching seeds in unlocked slots and leaves produce and rejected seeds untouched");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void verticalGreenhouseCraftHasOneMatch(GameTestHelper helper) {
        var level = helper.getLevel();
        var input = CraftingInput.of(3, 3, List.of(new ItemStack(Items.NETHERITE_INGOT), new ItemStack(AEItems.GREENHOUSE_SMART_FARMING_UPGRADE.get()),
                new ItemStack(Items.NETHERITE_INGOT), new ItemStack(Items.TINTED_GLASS), new ItemStack(AEItems.BOTANICAL_GREENHOUSE.get()),
                new ItemStack(Items.TINTED_GLASS), new ItemStack(Items.COPPER_BLOCK), new ItemStack(AEItems.SEED_BREEDING_STATION.get()), new ItemStack(Items.COPPER_BLOCK)));
        var matches = level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream().filter(recipe -> recipe.value().matches(input, level)).toList();
        helper.assertTrue(matches.size() == 1 && matches.getFirst().value().assemble(input, level.registryAccess()).is(AEItems.VERTICAL_GREENHOUSE.get()),
                "The upgrade recipe has one match across vanilla and mod recipes");
        helper.succeed();
    }
}
