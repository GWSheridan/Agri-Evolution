package com.agrievolution.content;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("agri_evolution") @PrefixGameTestTemplate(false)
public final class InspectionGameTests {
    @GameTest(template="empty") public static void cropInspectionAndDiscoveryTracker(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(3,170,3));
        level.setBlock(pos.below(),Blocks.FARMLAND.defaultBlockState(),2);
        level.setBlock(pos,AEBlocks.AGRI_CROP.get().defaultBlockState().setValue(AgriCropBlock.SUPPORTED,true).setValue(AgriCropBlock.SUPPORT_TIER,2).setValue(AgriCropBlock.WEEDY,true),2);
        var plant=(AECropBlockEntity)level.getBlockEntity(pos);plant.setGenome(new com.agrievolution.genetics.CropGenome(61,42,33));
        String report=String.join(" ",FarmInspection.describe(level,pos,false,true));
        h.assertTrue(report.contains("61 / 42 / 33")&&report.contains("iron")&&report.contains("present - use a rake"),"Overlay reports server genes, weeds and tier");
        h.assertTrue(!plant.hasBeenTended()&&plant.soilNutrition()==40,"Inspection cannot tend or fertilize");
        var book=new net.minecraft.world.item.ItemStack(AEItems.BREEDING_JOURNAL.get());
        var recipe=com.agrievolution.client.journal.IllustratedBreedingRecipes.ALL.get(2);
        var empty=com.agrievolution.client.journal.JournalSnapshot.from(book);
        h.assertTrue(com.agrievolution.client.journal.JournalIndex.progress(empty,recipe).startsWith("Undiscovered parent"),"Missing parents named from journal records");
        var tag=new net.minecraft.nbt.CompoundTag();var found=new net.minecraft.nbt.ListTag();
        found.add(net.minecraft.nbt.StringTag.valueOf(recipe.first().id()));found.add(net.minecraft.nbt.StringTag.valueOf(recipe.second().id()));
        tag.put("AgriDiscoveredSpecies",found);book.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
        h.assertTrue(com.agrievolution.client.journal.JournalIndex.progress(com.agrievolution.client.journal.JournalSnapshot.from(book),recipe).startsWith("Parents discovered"),"Known parents suggest next cross");
        found.add(net.minecraft.nbt.StringTag.valueOf(recipe.result().id()));book.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
        h.assertTrue(com.agrievolution.client.journal.JournalIndex.progress(com.agrievolution.client.journal.JournalSnapshot.from(book),recipe).equals("Result discovered"),"Discovered result is not mislabeled as recorded cross success");h.succeed();
    }
    @GameTest(template="empty") public static void reservoirComparatorAllLevels(GameTestHelper h){
        var level=h.getLevel();var core=h.absolutePos(new BlockPos(3,130,3));var tank=ReservoirGameTests.tank(h,core);
        tank.takeBucket();
        for(int buckets=0;buckets<=32;buckets++){
            int expected=buckets==0?0:1+14*buckets/32;
            for(var side:Direction.values()){
                var pos=core.relative(side);var state=level.getBlockState(pos);
                h.assertTrue(state.hasAnalogOutputSignal()&&state.getAnalogOutputSignal(level,pos)==expected,"Every shell face must report exact fill at "+buckets);
            }
            if(buckets<32)tank.addBucket();
        }
        h.assertFalse(tank.addBucket(),"Inspection and redstone cannot increase capacity");h.succeed();
    }

    @GameTest(template="empty")
    public static void inspectAndRecoverInstalledSticks(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(3, 175, 3));
        level.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState(), 2);
        var crop = AEBlocks.AGRI_CROP.get().defaultBlockState().setValue(AgriCropBlock.AGE, 4)
                .setValue(AgriCropBlock.SUPPORTED, true).setValue(AgriCropBlock.SUPPORT_TIER, 2)
                .setValue(AgriCropBlock.WEEDY, true);
        level.setBlock(pos, crop, 2);
        var plant = (AECropBlockEntity) level.getBlockEntity(pos);
        var genes = new com.agrievolution.genetics.CropGenome(71, 52, 33);
        plant.setGenome(genes);
        plant.setSupportTicks(599);
        var report = FarmInspection.describe(level, pos, false, true);
        helper.assertTrue(report.contains("Stick life: 30s (replace soon)") && plant.supportTicks() == 599,
                "The sampler reports the countdown without changing it");

        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
        var spanner = AEItems.SPANNER.get();
        player.setItemInHand(hand, new net.minecraft.world.item.ItemStack(spanner));
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), Direction.UP, pos, false);
        var context = new net.minecraft.world.item.context.UseOnContext(player, hand, hit);
        helper.assertTrue(spanner.useOn(context) == net.minecraft.world.InteractionResult.PASS, "Crouching is required");
        player.setShiftKeyDown(true);
        helper.assertTrue(spanner.useOn(context).consumesAction(), "Spanner removes installed sticks");
        var after = level.getBlockState(pos);
        helper.assertTrue(after.is(crop.getBlock()) && after.getValue(AgriCropBlock.AGE) == 4
                && after.getValue(AgriCropBlock.WEEDY) && CropSupportItem.tier(after) == 0 && plant.genome().equals(genes),
                "Recovery preserves the crop, age, weeds and genetics");
        var recovered = player.getInventory().items.stream().filter(stack -> stack.is(AEItems.IRON_CROP_SUPPORT.get())).findFirst().orElseThrow();
        helper.assertTrue(recovered.getCount() == 1 && CropSupportItem.remaining(recovered) == 599, "Exactly one stick item keeps the remaining time");
        helper.assertTrue(spanner.useOn(context) == net.minecraft.world.InteractionResult.PASS, "The same sticks cannot be recovered twice");

        level.setBlock(pos, crop.setValue(AgriCropBlock.SUPPORT_TIER, 3).setValue(AgriCropBlock.WEEDY, false), 2);
        helper.assertTrue(FarmInspection.describe(level, pos, false, true).contains("Stick life: permanent"), "Obsidian has no countdown");
        helper.succeed();
    }
    @GameTest(template="empty") public static void comparatorUpdatesWithinVisualFillBand(GameTestHelper h){
        var level=h.getLevel();var core=h.absolutePos(new BlockPos(3,140,3));var tank=ReservoirGameTests.tank(h,core);
        var pos=core.north(2);level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);
        level.setBlock(pos,Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING,Direction.SOUTH),3);
        var through = core.east(3);
        level.setBlock(through.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(core.east(2), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(through, Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.WEST), Block.UPDATE_ALL);
        tank.addBucket();
        h.runAfterDelay(5,()->{
            int before=level.getBlockState(pos).getSignal(level,pos,Direction.SOUTH);
            h.assertTrue(before==1,"Comparator reads two buckets as 1 (was "+before+")");
            tank.addBucket();
            h.runAfterDelay(5,()->{
                h.assertTrue(level.getBlockState(core.north()).getValue(WaterReservoirBlock.FILL)==1,"Visual fill remains first band");
                h.assertTrue(level.getBlockState(pos).getSignal(level,pos,Direction.SOUTH)==2
                        && level.getBlockState(through).getSignal(level, through, Direction.WEST) == 2,
                        "Both comparators update inside the same visual band");
                level.removeBlock(core.south(), false);
                h.runAfterDelay(5, () -> {
                    h.assertTrue(level.getBlockState(pos).getSignal(level, pos, Direction.SOUTH) == 0
                            && level.getBlockState(through).getSignal(level, through, Direction.WEST) == 0,
                            "Dismantling clears direct and through-block comparators");
                    h.succeed();
                });
            });
        });
    }
    @GameTest(template="empty") public static void previewMatchesWateringAndObstructions(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(5,150,5));
        level.setBlock(pos,AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(),3);
        var open=pos.below(4);var blocked=open.east();var wet=open.west();
        for(var soil:new BlockPos[]{open,blocked,wet})level.setBlock(soil,Blocks.FARMLAND.defaultBlockState(),3);
        level.setBlock(blocked.above(2),Blocks.GLASS.defaultBlockState(),3);
        level.setBlock(wet.above(2),Blocks.WATER.defaultBlockState(),3);
        var head=(IrrigationSprinklerBlockEntity)level.getBlockEntity(pos);
        var cells=SprinklerCoverage.scan(level,pos);
        h.assertTrue(cells.size()==81,"Nine by nine columns");
        h.assertTrue(head.wateredSoils().contains(open)&&!head.wateredSoils().contains(blocked)&&!head.wateredSoils().contains(wet),"Solid glass and fluids block coverage");
        h.assertTrue(cells.stream().filter(SprinklerCoverage.Cell::watered).map(SprinklerCoverage.Cell::pos).collect(java.util.stream.Collectors.toSet()).equals(head.wateredSoils()),"Preview and real coverage identical");
        var floor = new BlockPos(pos.getX(), level.getMinBuildHeight(), pos.getZ());
        h.assertTrue(SprinklerCoverage.scan(level, floor).isEmpty(), "A head at the world floor has no room below it");
        var floorPreview = SprinklerCoverage.scan(level, floor.above());
        h.assertTrue(!floorPreview.isEmpty() && floorPreview.stream().allMatch(cell -> cell.pos().getY() == floor.getY()),
                "Near the floor the coverage preview stays within the world");
        h.assertTrue(head.water()==0&&!level.getBlockState(pos).getValue(IrrigationSprinklerBlock.ACTIVE),"Preview does not activate or refill");
        head.fillBucket();head.irrigateCycle();
        h.assertTrue(SprinklerGrowth.covers(level,open.above())&&!SprinklerGrowth.covers(level,blocked.above()),"Targeted overlay bonus matches real watered plots");h.succeed();
    }
    @GameTest(template="empty") public static void inspectionReadOnlyAndClosedValve(GameTestHelper h){
        var level=h.getLevel();var core=h.absolutePos(new BlockPos(3,160,3));var tank=ReservoirGameTests.tank(h,core);
        var valve=core.east(2);var pos=core.east(3);
        level.setBlock(valve,AEBlocks.IRRIGATION_VALVE.get().defaultBlockState(),3);
        level.setBlock(pos,AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(),3);
        var head=(IrrigationSprinklerBlockEntity)level.getBlockEntity(pos);
        String ready=String.join(" ",FarmInspection.describe(level,pos,true,false));
        h.assertTrue(ready.contains("1000 mB")&&ready.contains("Connected source ready to refill"),"Report source and ready status");
        h.assertTrue(tank.water()==1000&&head.water()==0,"Inspection cannot transfer water");
        level.setBlock(valve,level.getBlockState(valve).setValue(IrrigationValveBlock.OPEN,false),3);
        h.assertTrue(FarmInspection.describe(level,pos,true,false).contains("Adjacent valve closed"),"Explain closed valve");
        h.assertTrue(IrrigationNetwork.search(level,valve).supplies().isEmpty(),"Closed starting valve must not claim reachable source");h.succeed();
    }

    @GameTest(template="empty")
    public static void greenhouseStickMapTracksWearAndOrientation(GameTestHelper helper) {
        var level = helper.getLevel();
        var controller = helper.absolutePos(new BlockPos(6, 180, 6));
        var facing = Direction.EAST;
        var inward = facing.getOpposite();
        level.setBlock(controller, AEBlocks.BOTANICAL_GREENHOUSE.get().defaultBlockState().setValue(BotanicalGreenhouseBlock.FACING, facing), 3);
        var greenhouse = (BotanicalGreenhouseBlockEntity) level.getBlockEntity(controller);
        var crop = AEBlocks.AGRI_CROP.get().defaultBlockState();
        var frontLeft = controller.relative(inward).relative(inward.getCounterClockWise(), 2);
        var backRight = controller.relative(inward, 5).relative(inward.getClockWise(), 2);
        var center = controller.relative(inward, 3);
        var frontCenter = controller.relative(inward);
        for (var plot : new BlockPos[]{frontLeft, backRight, center, frontCenter}) {
            level.setBlock(plot.below(), Blocks.FARMLAND.defaultBlockState(), 2);
            level.setBlock(plot, crop, 2);
        }
        level.setBlock(frontLeft, crop.setValue(AgriCropBlock.SUPPORTED, true), 2);
        ((AECropBlockEntity) level.getBlockEntity(frontLeft)).setSupportTicks(1199);
        level.setBlock(backRight, crop.setValue(AgriCropBlock.SUPPORTED, true).setValue(AgriCropBlock.SUPPORT_TIER, 2), 2);
        level.setBlock(center, crop.setValue(AgriCropBlock.SUPPORTED, true).setValue(AgriCropBlock.SUPPORT_TIER, 3), 2);
        greenhouse.refresh();

        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var menu = new BotanicalGreenhouseMenu(1, player.getInventory(), greenhouse);
        int front = menu.stickStatus(20), back = menu.stickStatus(4);
        helper.assertTrue(GreenhouseStickStatus.tier(front) == 1 && GreenhouseStickStatus.seconds(front) == 60
                && GreenhouseStickStatus.dueSoon(front), "Front-left cell reports a legacy wooden support and its warning");
        helper.assertTrue(GreenhouseStickStatus.tier(back) == 2 && GreenhouseStickStatus.seconds(back) == 14400
                && !GreenhouseStickStatus.dueSoon(back) && back <= Short.MAX_VALUE, "Back-right full iron timer fits the menu protocol");
        helper.assertTrue(menu.stickStatus(12) == GreenhouseStickStatus.OBSIDIAN && menu.stickStatus(22) == GreenhouseStickStatus.UNSUPPORTED,
                "Permanent and unsupported crops have distinct states");
        level.setBlock(frontLeft, Blocks.AIR.defaultBlockState(), 2);
        greenhouse.refresh();
        helper.assertTrue(menu.stickStatus(20) == GreenhouseStickStatus.NO_CROP, "The next reading drops stale stick data");
        helper.succeed();
    }

    @GameTest(template="empty")
    public static void greenhouseWeedMapTracksBothWeedTypes(GameTestHelper helper) {
        var level = helper.getLevel();
        var controller = helper.absolutePos(new BlockPos(6, 185, 6));
        var facing = Direction.SOUTH;
        level.setBlock(controller, AEBlocks.BOTANICAL_GREENHOUSE.get().defaultBlockState().setValue(BotanicalGreenhouseBlock.FACING, facing), 3);
        var greenhouse = (BotanicalGreenhouseBlockEntity) level.getBlockEntity(controller);
        var agri = controller.north().west(2);
        var vanilla = controller.north().east(2);
        for (var plot : new BlockPos[]{agri, vanilla}) level.setBlock(plot.below(), Blocks.FARMLAND.defaultBlockState(), 2);
        level.setBlock(agri, AEBlocks.AGRI_CROP.get().defaultBlockState().setValue(AgriCropBlock.WEEDY, true), 2);
        level.setBlock(vanilla, Blocks.WHEAT.defaultBlockState(), 2);
        var weed = AEEntities.CROP_WEED.get().create(level);
        weed.attach(vanilla, level.getBlockState(vanilla));
        helper.assertTrue(level.addFreshEntity(weed), "Vanilla weed entity is attached");
        greenhouse.refresh();

        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var menu = new BotanicalGreenhouseMenu(1, player.getInventory(), greenhouse);
        helper.assertTrue(menu.weedStatus(20) == GreenhouseWeedStatus.WEEDY && menu.weedStatus(24) == GreenhouseWeedStatus.WEEDY,
                "Agri weeds and vanilla weed entities are reported in their own plots");
        helper.assertTrue(menu.weedStatus(21) == GreenhouseWeedStatus.NO_CROP, "Empty plots stay separate");
        helper.assertTrue(CropWeeds.hasWeeds(level, agri) && CropWeeds.hasWeeds(level, vanilla), "Reading the map leaves weeds in place");

        CropWeeds.clear(level, vanilla);
        greenhouse.refresh();
        helper.assertTrue(menu.weedStatus(24) == GreenhouseWeedStatus.CLEAR && menu.weedStatus(20) == GreenhouseWeedStatus.WEEDY,
                "Cleared plot updates without affecting another plot");
        level.setBlock(agri, Blocks.AIR.defaultBlockState(), 2);
        greenhouse.refresh();
        helper.assertTrue(menu.weedStatus(20) == GreenhouseWeedStatus.NO_CROP, "Removed crop leaves no stale weed flag");
        helper.succeed();
    }
}
