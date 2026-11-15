package com.agrievolution.content;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("agri_evolution") @PrefixGameTestTemplate(false)
public final class ReservoirGameTests {
    private static final class InspectingPlayer extends Player {
        private String message = "";

        InspectingPlayer(ServerLevel level) {
            super(level, BlockPos.ZERO, 0, new GameProfile(UUID.randomUUID(), "irrigation-test"));
        }

        @Override public boolean isSpectator() { return false; }
        @Override public boolean isCreative() { return false; }
        @Override public void displayClientMessage(Component text, boolean actionBar) { message = text.getString(); }
    }

    static WaterReservoirBlockEntity tank(GameTestHelper h,BlockPos core){
        blueprint(h,core);h.assertTrue(ReservoirStructure.tryForm(h.getLevel(),core.north()),"Valid shell must form");
        return (WaterReservoirBlockEntity)h.getLevel().getBlockEntity(core);
    }
    static void blueprint(GameTestHelper h,BlockPos core){
        for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++)
            h.getLevel().setBlock(core.offset(x,y,z),(x==0&&y==0&&z==0?Blocks.WATER:Blocks.COPPER_BLOCK).defaultBlockState(),2);
    }
    @GameTest(template="empty") public static void formationStorageAndDismantle(GameTestHelper h){
        var level=h.getLevel();var core=h.absolutePos(new BlockPos(3,20,3));var tank=tank(h,core);
        h.assertTrue(tank.water()==1000,"Source must become exactly one stored bucket");
        for(int i=1;i<32;i++)h.assertTrue(tank.addBucket(),"Tank should accept remaining capacity");
        h.assertFalse(tank.addBucket(),"Full tank must reject bucket");
        var tag=new CompoundTag();tank.saveAdditional(tag,level.registryAccess());
        var restored=new WaterReservoirBlockEntity(core,level.getBlockState(core));restored.loadAdditional(tag,level.registryAccess());
        h.assertTrue(restored.water()==32000,"Tank must persist water");
        level.setBlock(core.north(),Blocks.AIR.defaultBlockState(),3);
        int copper=0;for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++)if(level.getBlockState(core.offset(x,y,z)).is(Blocks.COPPER_BLOCK))copper++;
        h.assertTrue(copper==25,"Breaking one shell must restore the other 25 copper blocks");
        h.assertTrue(level.getBlockState(core).is(Blocks.WATER),"Nonempty dismantled tank may return only its original source");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void spannerRespectsTheBoundaryAcrossFittingsAndReservoirParts(GameTestHelper h) {
        var level = h.getLevel();
        var core = h.absolutePos(new BlockPos(4, 95, 4));
        var clicked = core.west();
        blueprint(h, core);
        var pipe = core.east(3);
        var head = pipe.east();
        var valve = pipe.south();
        level.setBlock(pipe, AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(head, AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(valve, AEBlocks.IRRIGATION_VALVE.get().defaultBlockState(), Block.UPDATE_ALL);
        var sprinkler = (IrrigationSprinklerBlockEntity) level.getBlockEntity(head);
        var player = new InspectingPlayer(level);
        var tool = new ItemStack(AEItems.SPANNER.get());
        tool.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("Garden"));
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        var border = level.getWorldBorder();
        double oldX = border.getCenterX(), oldZ = border.getCenterZ(), oldSize = border.getSize();
        try {
            border.setCenter(core.getX(), core.getZ());
            border.setSize(2.0);
            h.assertTrue(level.mayInteract(player, clicked) && !level.mayInteract(player, core.east()), "The shell crosses the world border");
            var onShell = new BlockHitResult(Vec3.atCenterOf(clicked), Direction.WEST, clicked, false);
            tool.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, onShell));
            h.assertTrue(player.message.contains("permission") && level.getBlockState(core).is(Blocks.WATER),
                    "Formation reports a protected part and preserves the source");
            for (var part : BlockPos.betweenClosed(core.offset(-1, -1, -1), core.offset(1, 1, 1))) {
                if (!part.equals(core)) h.assertTrue(level.getBlockState(part).is(Blocks.COPPER_BLOCK), "The whole copper shell stays in place");
            }

            for (var target : List.of(pipe, head, valve)) {
                player.setShiftKeyDown(target.equals(valve));
                var hit = new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false);
                SpannerItem.onRightClick(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, target, hit));
            }
            var namedValve = (IrrigationValveBlockEntity) level.getBlockEntity(valve);
            h.assertTrue(IrrigationPipeBlock.enabled(level.getBlockState(pipe), Direction.UP)
                    && !sprinkler.smartWatering() && namedValve.zone().equals("Irrigation zone"),
                    "Protected fittings retain their port, watering mode, and name");
        } finally {
            border.setCenter(oldX, oldZ);
            border.setSize(oldSize);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void reservoirBucketAndUpgradeFeedbackMatchesTank(GameTestHelper h) {
        var level = h.getLevel();
        var core = h.absolutePos(new BlockPos(3, 105, 3));
        var tank = tank(h, core);
        var face = core.west();
        var state = level.getBlockState(face);
        var block = AEBlocks.WATER_RESERVOIR.get();
        var player = new InspectingPlayer(level);
        var hit = new BlockHitResult(Vec3.atCenterOf(face), Direction.WEST, face, false);
        tank.takeBucket();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET, 2));
        block.useItemOn(player.getMainHandItem(), state, level, face, player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(player.message.contains("less than 1000 mB") && player.getMainHandItem().getCount() == 2 && tank.water() == 0,
                "An empty tank explains the failed drain and preserves the bucket stack");

        tank.fill(new FluidStack(Fluids.WATER, WaterReservoirBlockEntity.CAPACITY), FluidAction.EXECUTE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        block.useItemOn(player.getMainHandItem(), state, level, face, player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(player.message.contains("needs space") && player.getMainHandItem().is(Items.WATER_BUCKET)
                && tank.water() == WaterReservoirBlockEntity.CAPACITY, "A full tank explains the failed fill and keeps the held water");

        block.useWithoutItem(state, level, face, player, hit);
        h.assertTrue(player.message.contains("Use a Conduit to upgrade"), "Mk I inspection explains the upgrade");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CONDUIT, 2));
        block.useItemOn(player.getMainHandItem(), state, level, face, player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(tank.renewable() && player.getMainHandItem().getCount() == 1, "Upgrading uses one Conduit");
        block.useItemOn(player.getMainHandItem(), state, level, face, player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(player.message.contains("already Mk II") && player.getMainHandItem().getCount() == 1,
                "An upgraded tank explains the second attempt and keeps the spare Conduit");
        block.useWithoutItem(state, level, face, player, hit);
        h.assertTrue(player.message.contains("1000 mB / 10 sec") && player.message.contains("buckets") && !player.message.contains("Use a Conduit"),
                "Mk II inspection shows operating controls and the generation rate");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void reservoirDefersDismantlingUntilTheShellIsLoaded(GameTestHelper h) {
        var level = h.getLevel();
        var chunk = new ChunkPos(h.absolutePos(new BlockPos(3328, 100, 4)));
        var core = new BlockPos(chunk.getMaxBlockX(), 100, chunk.getMiddleBlockZ());
        var outside = core.east();
        var broken = core.west();
        var remaining = core.north();
        level.getChunkAt(core);
        int quietUpdate = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        var block = AEBlocks.WATER_RESERVOIR.get();
        var state = block.defaultBlockState();
        level.setBlock(core, state, quietUpdate);
        level.setBlock(broken, state.setValue(WaterReservoirBlock.X, 0), quietUpdate);
        level.setBlock(remaining, state.setValue(WaterReservoirBlock.Z, 0), quietUpdate);
        var tank = (WaterReservoirBlockEntity) level.getBlockEntity(core);
        var saved = new CompoundTag();
        saved.putInt("Water", 2500);
        saved.putBoolean("Renewable", true);
        tank.loadAdditional(saved, level.registryAccess());
        h.assertFalse(level.hasChunkAt(outside), "The far side of the shell starts unloaded");

        ReservoirStructure.dismantle(level, core, broken);
        h.assertFalse(level.hasChunkAt(outside), "Dismantling must not load the far chunk");
        level.setBlock(broken, Blocks.AIR.defaultBlockState(), quietUpdate);
        h.assertTrue(level.getBlockEntity(core) == tank, "The same tank must stay in place while dismantling is deferred");
        h.assertTrue(tank.water() == 2500 && tank.renewable(), "The deferred tank must keep its water and upgrade");
        h.assertTrue(level.getBlockState(remaining).is(block), "The other loaded shell parts must stay in place");

        level.getChunkAt(outside);
        level.setBlock(outside, state.setValue(WaterReservoirBlock.X, 2), quietUpdate);
        block.tick(level.getBlockState(core), level, core, level.random);
        h.assertTrue(level.getBlockState(core).is(Blocks.WATER) && level.getBlockState(remaining).is(Blocks.COPPER_BLOCK)
                && level.getBlockState(outside).is(Blocks.COPPER_BLOCK), "The loaded shell can now restore copper and the original source");
        var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(core).inflate(3));
        h.assertTrue(drops.stream().filter(drop -> drop.getItem().is(Items.CONDUIT)).mapToInt(drop -> drop.getItem().getCount()).sum() == 1,
                "Deferred dismantling returns the Conduit once");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void reservoirDiagnosticsExplainUnavailableShells(GameTestHelper h) {
        var level = h.getLevel();
        var chunk = new ChunkPos(h.absolutePos(new BlockPos(2816, 100, 4)));
        var edge = new BlockPos(chunk.getMaxBlockX(), 100, chunk.getMiddleBlockZ());
        level.getChunkAt(edge);
        var reservoir = AEBlocks.WATER_RESERVOIR.get().defaultBlockState();
        var inlet = reservoir.setValue(WaterReservoirBlock.X, 0);
        int quietUpdate = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        var cases = List.of(ReservoirStructure.Status.UNLOADED, ReservoirStructure.Status.INCOMPLETE);
        var player = new InspectingPlayer(level);
        for (var expected : cases) {
            var core = expected == ReservoirStructure.Status.UNLOADED ? edge : edge.west(6);
            var face = core.west();
            var valve = core.west(2);
            level.setBlock(core, reservoir, quietUpdate);
            level.setBlock(face, inlet, quietUpdate);
            level.setBlock(valve, AEBlocks.IRRIGATION_VALVE.get().defaultBlockState(), quietUpdate);
            h.assertTrue(WaterReservoirBlock.inspectionStatus(level, face, inlet).equals(expected.message)
                    && FarmInspection.describe(level, face, true, false).contains(expected.message),
                    "Empty-hand inspection and the Spanner agree on reservoir availability");
            var hit = new BlockHitResult(Vec3.atCenterOf(face), Direction.WEST, face, false);
            for (var item : List.of(Items.WATER_BUCKET, Items.CONDUIT)) {
                var held = new ItemStack(item);
                AEBlocks.WATER_RESERVOIR.get().useItemOn(held, inlet, level, face, player, InteractionHand.MAIN_HAND, hit);
                h.assertTrue(player.message.equals(expected.message) && held.is(item), "Unavailable shells explain the failure and preserve the item");
            }
            var expectedFlow = expected == ReservoirStructure.Status.UNLOADED ? IrrigationNetwork.Flow.UNLOADED_CONNECTION
                    : IrrigationNetwork.Flow.INCOMPLETE_RESERVOIR;
            h.assertTrue(IrrigationNetwork.trace(level, valve).equals(new IrrigationNetwork.Route(List.of(valve, face), expectedFlow, null)),
                    "The trace points to the unavailable water inlet");
            var zone = IrrigationNetwork.inspectZone(level, valve);
            h.assertTrue(zone.supplies().isEmpty() && zone.unloadedConnection() == (expected == ReservoirStructure.Status.UNLOADED)
                    && zone.incompleteReservoir() == (expected == ReservoirStructure.Status.INCOMPLETE),
                    "The valve distinguishes unavailable sources from a disconnected pipe");
        }
        h.assertFalse(level.hasChunkAt(edge.east()), "Inspection must leave the unseen shell chunk unloaded");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void reservoirWaterChangesLeaveAdjacentChunkUnloaded(GameTestHelper h) {
        var level = h.getLevel();
        var chunk = new ChunkPos(h.absolutePos(new BlockPos(1792, 100, 4)));
        var edge = new BlockPos(chunk.getMaxBlockX(), 100, chunk.getMiddleBlockZ());
        var outside = edge.east();
        var core = edge.west(2);
        level.getChunkAt(core);
        var tank = tank(h, core);
        level.setBlock(edge, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        h.assertFalse(level.hasChunkAt(outside), "The chunk beyond the conductor must start unloaded");

        h.assertTrue(tank.addBucket() && tank.water() == 2000, "The formed tank stores another bucket");
        h.assertFalse(level.hasChunkAt(outside), "Refilling must not check for a comparator beyond the loaded conductor");
        h.assertTrue(tank.takeBucket() && tank.takeBucket() && tank.water() == 0, "Both buckets can be collected");
        h.assertTrue(level.getBlockState(core.east()).getValue(WaterReservoirBlock.FILL) == 0 && !level.hasChunkAt(outside),
                "The shell shows an empty tank without loading the next chunk");
        h.succeed();
    }

    @GameTest(template="empty") public static void invalidBlueprintAndEmptyDismantle(GameTestHelper h){
        var level=h.getLevel();var core=h.absolutePos(new BlockPos(3,30,3));blueprint(h,core);
        level.setBlock(core.above(),Blocks.AIR.defaultBlockState(),2);
        h.assertFalse(ReservoirStructure.tryForm(level,core.north()),"Incomplete shell must not form");
        h.assertTrue(level.getBlockState(core).is(Blocks.WATER),"Failed formation must preserve water");
        level.setBlock(core.above(),Blocks.COPPER_BLOCK.defaultBlockState(),2);
        h.assertTrue(ReservoirStructure.tryForm(level,core.north()),"Repaired shell forms");
        var tank=(WaterReservoirBlockEntity)level.getBlockEntity(core);h.assertTrue(tank.takeBucket(),"Drain original water");
        level.setBlock(core.north(),Blocks.AIR.defaultBlockState(),3);
        h.assertTrue(level.getBlockState(core).isAir(),"Empty tank must not recreate its original water source");h.succeed();
    }
    @GameTest(template="empty") public static void remoteValveAndPipeBreak(GameTestHelper h){
        var level=h.getLevel();var core=h.absolutePos(new BlockPos(3,40,3));var tank=tank(h,core);
        for(int i=2;i<=5;i++)level.setBlock(core.east(i),AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(),3);
        var valve=core.east(3);level.setBlock(valve,AEBlocks.IRRIGATION_VALVE.get().defaultBlockState().setValue(IrrigationValveBlock.FACING,Direction.EAST),3);
        var pos=core.east(6);level.setBlock(pos,AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(),3);
        var head=(IrrigationSprinklerBlockEntity)level.getBlockEntity(pos);head.irrigateCycle();
        h.assertTrue(tank.water()==0&&head.water()==975,"Remote transfer must debit one tank bucket");
        level.setBlock(valve,level.getBlockState(valve).setValue(IrrigationValveBlock.OPEN,false),3);head.checkFlow();
        h.assertFalse(head.growthActive(),"Remote closed valve must stop buffered sprinkler");
        level.setBlock(valve,level.getBlockState(valve).setValue(IrrigationValveBlock.OPEN,true),3);head.irrigateCycle();
        h.assertTrue(head.growthActive(),"Empty but intact source permits paid buffer use");
        level.setBlock(valve.above(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);head.checkFlow();
        h.assertFalse(head.growthActive(),"Remote powered valve must stop boost");
        level.setBlock(valve.above(),Blocks.AIR.defaultBlockState(),3);level.setBlock(core.east(4),Blocks.AIR.defaultBlockState(),3);head.checkFlow();
        head.irrigateCycle();h.assertTrue(head.growthActive(),"Disconnected supply cannot disable already-paid stored water");h.succeed();
    }
    @GameTest(template="empty") public static void branchesShareFiniteWater(GameTestHelper h){
        var level=h.getLevel();var core=h.absolutePos(new BlockPos(3,50,3));var tank=tank(h,core);
        var junction=core.east(2);level.setBlock(junction,AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(),3);
        for(var pos:new BlockPos[]{junction.north(),junction.south()})level.setBlock(pos,AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(),3);
        var a=(IrrigationSprinklerBlockEntity)level.getBlockEntity(junction.north());var b=(IrrigationSprinklerBlockEntity)level.getBlockEntity(junction.south());
        a.irrigateCycle();b.irrigateCycle();h.assertTrue(a.water()+b.water()==975&&tank.water()==0,"Branches must not duplicate one source bucket");
        tank.addBucket();b.irrigateCycle();h.assertTrue(b.water()==975&&tank.water()==0,"Refill supplies waiting branch");h.succeed();
    }
    @GameTest(template="empty") public static void bucketStacksAndDropConservation(GameTestHelper h){
        var level=h.getLevel();var core=h.absolutePos(new BlockPos(3,60,3));var tank=tank(h,core);
        var pos=core.north();var state=level.getBlockState(pos);var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BUCKET,2));
        AEBlocks.WATER_RESERVOIR.get().useItemOn(player.getMainHandItem(),state,level,pos,player,net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),Direction.NORTH,pos,false));
        h.assertTrue(tank.water()==0&&player.getMainHandItem().getCount()==1,"Draining must consume just one empty bucket");
        int filled=player.getInventory().countItem(net.minecraft.world.item.Items.WATER_BUCKET);
        h.assertTrue(filled==1,"Draining a stack must return exactly one filled bucket");
        var drops=net.minecraft.world.level.block.Block.getDrops(state,level,pos,null);
        h.assertTrue(drops.size()==1&&drops.getFirst().is(net.minecraft.world.item.Items.COPPER_BLOCK)&&drops.getFirst().getCount()==1,"Broken shell must drop one original copper block");
        var centerDrops=net.minecraft.world.level.block.Block.getDrops(level.getBlockState(core),level,core,tank);
        h.assertTrue(centerDrops.isEmpty(),"Water-source centre must never drop extra copper");h.succeed();
    }
    @GameTest(template="empty") public static void pipeCycleAndClosedAlternateBranch(GameTestHelper h){
        var level=h.getLevel();var core=h.absolutePos(new BlockPos(3,70,3));var tank=tank(h,core);
        // A four-pipe loop with one reachable reservoir and an unrelated closed branch.
        for(var p:new BlockPos[]{core.east(2),core.east(3),core.east(2).south(),core.east(3).south()})level.setBlock(p,AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(),3);
        level.setBlock(core.east(2).north(),AEBlocks.IRRIGATION_VALVE.get().defaultBlockState().setValue(IrrigationValveBlock.OPEN,false),3);
        var pos=core.east(4);level.setBlock(pos,AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(),3);
        var search=IrrigationNetwork.search(level,pos);
        h.assertTrue(!search.overflow()&&search.supplies().size()==1,"Loop terminates and deduplicates tank source");
        var head=(IrrigationSprinklerBlockEntity)level.getBlockEntity(pos);head.irrigateCycle();
        h.assertTrue(head.water()==975&&tank.water()==0,"Unrelated closed branch must not shut off an open supply path");h.succeed();
    }
    @GameTest(template = "empty")
    public static void savedReservoirSurvivesRotationAndMirroring(GameTestHelper h) {
        var level = h.getLevel();
        var core = h.absolutePos(new BlockPos(3, 150, 3));
        var tank = tank(h, core);
        for (int i = 0; i < 8; i++) h.assertTrue(tank.addBucket(), "Fill the tank above the first display level");
        h.assertTrue(tank.upgrade(), "The tank has the Mk II upgrade to save");
        level.setBlock(core.east(2), AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(core.east(3), AEBlocks.IRRIGATION_VALVE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(core.east(4), AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(), Block.UPDATE_ALL);

        var template = new StructureTemplate();
        template.fillFromWorld(level, core.offset(-1, -1, -1), new Vec3i(6, 3, 3), false, null);
        var placements = List.of(
                new StructurePlaceSettings().setRotation(Rotation.CLOCKWISE_90),
                new StructurePlaceSettings().setRotation(Rotation.CLOCKWISE_180).setMirror(Mirror.LEFT_RIGHT),
                new StructurePlaceSettings().setRotation(Rotation.COUNTERCLOCKWISE_90).setMirror(Mirror.FRONT_BACK));
        var copies = new ArrayList<WaterReservoirBlockEntity>();
        for (var placement : placements) {
            var origin = core.above(7 + copies.size() * 5);
            h.assertTrue(template.placeInWorld(level, origin, origin, placement.setKnownShape(true), level.random,
                    Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE), "Saved reservoir can be placed");
            var pastedCore = origin.offset(StructureTemplate.calculateRelativePosition(placement, new BlockPos(1, 1, 1)));
            var copy = (WaterReservoirBlockEntity) level.getBlockEntity(pastedCore);
            h.assertTrue(copy != null && copy.complete() && copy.water() == 9000 && copy.renewable()
                    && level.getBlockState(pastedCore).getValue(WaterReservoirBlock.FILL) == 2,
                    "Rotated shell, stored water, fill display and Mk II upgrade survive placement");
            copies.add(copy);

            var outlet = placement.getRotation().rotate(placement.getMirror().mirror(Direction.EAST));
            var sprinkler = (IrrigationSprinklerBlockEntity) level.getBlockEntity(pastedCore.relative(outlet, 4));
            sprinkler.irrigateCycle();
            h.assertTrue(sprinkler.water() == 975 && copy.water() == 8000 && tank.water() == 9000,
                    "The pasted sprinkler draws from its own tank through the transformed outlet");
        }
        h.runAfterDelay(21, () -> {
            h.assertTrue(copies.stream().allMatch(WaterReservoirBlockEntity::complete),
                    "Copied shells stay intact after the reservoir checks its structure");
            h.succeed();
        });
    }

    @GameTest(template="empty") public static void oversizedNetworkFailsClosed(GameTestHelper h){
        var level=h.getLevel();var origin=h.absolutePos(new BlockPos(1,90,1));
        for(int x=0;x<9;x++)for(int y=0;y<7;y++)for(int z=0;z<9;z++)
            level.setBlock(origin.offset(x,y,z),AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(),2);
        var head=origin.west();level.setBlock(head,AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(),3);
        var search=IrrigationNetwork.search(level,head);
        h.assertTrue(search.overflow()&&search.supplies().isEmpty(),"Oversized network must stop at the traversal limit without exposing partial supply");h.succeed();
    }
}
