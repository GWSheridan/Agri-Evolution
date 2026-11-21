package com.agrievolution.content;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
@GameTestHolder("agri_evolution") @PrefixGameTestTemplate(false)
public final class ReservoirAutomationGameTests {
 private static final net.minecraft.server.level.TicketType<net.minecraft.world.level.ChunkPos> SCHEDULED_TICKET =
   net.minecraft.server.level.TicketType.create("agri_evolution_reservoir_test",java.util.Comparator.comparingLong(net.minecraft.world.level.ChunkPos::toLong),300);
    @GameTest(template = "empty")
    public static void fullReservoirRestartsMkTwoGenerationTimer(GameTestHelper h) {
        var tank = ReservoirGameTests.tank(h, h.absolutePos(new BlockPos(3, 160, 3)));
        h.assertTrue(tank.upgrade(), "Upgrade to Mk II");
        tank.fill(new FluidStack(Fluids.WATER, 30500), FluidAction.EXECUTE);
        for (int i = 0; i < 9; i++) tank.generationSecond();
        tank.fill(new FluidStack(Fluids.WATER, 500), FluidAction.EXECUTE);
        h.assertTrue(tank.takeBucket(), "Make room after topping up the tank");
        for (int i = 0; i < 9; i++) tank.generationSecond();
        h.assertTrue(tank.water() == 31000, "The seconds before reaching full do not count after draining");
        tank.generationSecond();
        h.assertTrue(tank.water() == 32000, "A fresh cycle fills the bucket on the tenth second");

        var saved = new CompoundTag();
        tank.saveAdditional(saved, h.getLevel().registryAccess());
        saved.putInt("GenerationSeconds", 9);
        tank.loadAdditional(saved, h.getLevel().registryAccess());
        h.assertTrue(tank.takeBucket(), "A full tank from an older save is drainable");
        tank.generationSecond();
        h.assertTrue(tank.water() == 31000, "Loading a full tank clears any saved partial cycle");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void reservoirGaugeRepairRefreshesComparatorOutput(GameTestHelper h) {
        var level = h.getLevel();
        var core = h.absolutePos(new BlockPos(3, 161, 3));
        var tank = ReservoirGameTests.tank(h, core);
        h.assertTrue(tank.takeBucket(), "Start with an empty tank before placing the comparator");
        var output = core.east(3);
        level.setBlock(core.east(2), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(output.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(output, Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.WEST), 3);
        var comparator = (ComparatorBlockEntity) level.getBlockEntity(output);
        h.assertTrue(tank.addBucket(), "Fill one bucket to notify the comparator");
        h.runAfterDelay(5, () -> {
            h.assertTrue(comparator.getOutputSignal() == 1, "The initial bucket powers the comparator through a solid block");
            tank.fill(new FluidStack(Fluids.WATER, 1), FluidAction.EXECUTE);
        });
        h.runAfterDelay(10, () -> {
            h.assertTrue(comparator.getOutputSignal() == 1, "The signal stays steady between thresholds");
            tank.fill(new FluidStack(Fluids.WATER, 1999), FluidAction.EXECUTE);
        });
        h.runAfterDelay(15, () -> {
            h.assertTrue(comparator.getOutputSignal() == 2, "Crossing the threshold changes the output even in the same gauge band");
            tank.drain(3000, FluidAction.EXECUTE);
        });
        h.runAfterDelay(20, () -> {
            h.assertTrue(comparator.getOutputSignal() == 0, "Draining the tank resets the comparator");
            var face = core.east();
            var block = AEBlocks.WATER_RESERVOIR.get();
            level.setBlock(face, level.getBlockState(face).setValue(WaterReservoirBlock.FILL, 4), Block.UPDATE_KNOWN_SHAPE);
            comparator.setOutputSignal(9);
            block.tick(level.getBlockState(face), level, face, level.random);
            h.assertTrue(level.getBlockState(face).getValue(WaterReservoirBlock.FILL) == 0, "The old gauge is repaired from tank storage");
        });
        h.runAfterDelay(25, () -> {
            h.assertTrue(comparator.getOutputSignal() == 0, "Gauge repair also refreshes the stale comparator behind the stone");
            h.succeed();
        });
    }

 @GameTest(template="empty",batch="reservoirScheduling",timeoutTicks=240) public static void scheduledGenerationAndUpgradeRecovery(GameTestHelper h){
  var core=h.absolutePos(new BlockPos(3,55,3));
  // Own the ticking tickets: other test batches can release their global
  // forced-chunk flags while this scheduled check is still running.
  var pinned=new java.util.HashSet<net.minecraft.world.level.ChunkPos>();
  for(var part:BlockPos.betweenClosed(core.offset(-1,0,-1),core.offset(1,0,1))){
   var chunk=new net.minecraft.world.level.ChunkPos(part);
   if(pinned.add(chunk))h.getLevel().getChunkSource().addRegionTicket(SCHEDULED_TICKET,chunk,2,chunk,true);
  }
  var tank=ReservoirGameTests.tank(h,core);h.assertTrue(tank.upgrade(),"A complete new reservoir accepts its renewable upgrade");
  h.runAfterDelay(199,()->h.assertTrue(tank.water()==1000,"No bucket before ten actual seconds"));
  h.runAfterDelay(201,()->{
   for(var chunk:pinned)h.getLevel().getChunkSource().removeRegionTicket(SCHEDULED_TICKET,chunk,2,chunk,true);
   h.assertTrue(tank.water()==2000,"Scheduled core ticks generate exactly one bucket; water="+tank.water()+", progress="+tank.saveWithoutMetadata(h.getLevel().registryAccess()).getInt("GenerationSeconds")+", complete="+tank.complete()+", renewable="+tank.renewable()+", queued="+h.getLevel().getBlockTicks().hasScheduledTick(core,AEBlocks.WATER_RESERVOIR.get())+", capture="+h.getLevel().captureBlockSnapshots);
   h.getLevel().setBlock(core.east(),Blocks.AIR.defaultBlockState(),3);
   int count=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(core).inflate(3)).stream().filter(e->e.getItem().is(net.minecraft.world.item.Items.CONDUIT)).mapToInt(e->e.getItem().getCount()).sum();
   h.assertTrue(count==1,"Dismantle returns exactly one upgrade");h.succeed();
  });
 }
 @GameTest(template="empty") public static void renewableTierAndCapacity(GameTestHelper h){
  var core=h.absolutePos(new BlockPos(3,20,3));var tank=ReservoirGameTests.tank(h,core);
  for(int i=0;i<10;i++)tank.generationSecond();h.assertTrue(tank.water()==1000,"Mk I cannot generate free water");
  h.assertTrue(tank.upgrade()&&!tank.upgrade(),"Upgrade once only");
  for(int i=0;i<9;i++)tank.generationSecond();h.assertTrue(tank.water()==1000,"No early generation");
  tank.generationSecond();h.assertTrue(tank.water()==2000,"One bucket after ten seconds");
  tank.fill(new FluidStack(Fluids.WATER,29999),FluidAction.EXECUTE);
  for(int i=0;i<10;i++)tank.generationSecond();h.assertTrue(tank.water()==32000,"Generation clamps to capacity");
  var tag=new net.minecraft.nbt.CompoundTag();tank.saveAdditional(tag,h.getLevel().registryAccess());
  var restored=new WaterReservoirBlockEntity(core,tank.getBlockState());restored.loadAdditional(tag,h.getLevel().registryAccess());
  h.assertTrue(restored.renewable()&&restored.water()==32000,"Tier and water persist");h.succeed();
 }
 @GameTest(template="empty") public static void capabilitySimulationAndStaleTank(GameTestHelper h){
  var core=h.absolutePos(new BlockPos(3,35,3));var tank=ReservoirGameTests.tank(h,core);var level=h.getLevel();
  var invalidations = new AtomicInteger();
  var cache = BlockCapabilityCache.create(Capabilities.FluidHandler.BLOCK, level, core.north(), Direction.NORTH,
          () -> true, invalidations::incrementAndGet);
  var port = cache.getCapability();
  h.assertTrue(port!=null,"External pumps can access outward port");
  h.assertTrue(level.getCapability(Capabilities.FluidHandler.BLOCK,core.north(),Direction.UP)==null,"Only outward face exposes tank");
  h.assertTrue(port.fill(new FluidStack(Fluids.LAVA,1000),FluidAction.EXECUTE)==0,"Reject nonwater");
  h.assertTrue(port.fill(new FluidStack(Fluids.WATER,500),FluidAction.SIMULATE)==500&&tank.water()==1000,"Simulated fill is read only");
  port.fill(new FluidStack(Fluids.WATER,500),FluidAction.EXECUTE);
  h.assertTrue(port.drain(250,FluidAction.SIMULATE).getAmount()==250&&tank.water()==1500,"Simulated drain is read only");
  h.assertTrue(port.drain(250,FluidAction.EXECUTE).getAmount()==250&&tank.water()==1250,"Exact fluid accounting");
  h.assertTrue(invalidations.get() == 0 && cache.getCapability() == port && port.getFluidInTank(0).getAmount() == 1250,
          "Small transfers change the stored amount without invalidating the pump connection");
  port.fill(new FluidStack(Fluids.WATER, 6750), FluidAction.EXECUTE);
  h.assertTrue(level.getBlockState(core.north()).getValue(WaterReservoirBlock.FILL) == 1, "The first gauge band includes 8000 mB");
  port.fill(new FluidStack(Fluids.WATER, 1), FluidAction.EXECUTE);
  h.assertTrue(level.getBlockState(core.north()).getValue(WaterReservoirBlock.FILL) == 2, "Crossing a gauge threshold updates the shell");
  h.assertTrue(cache.getCapability() == port, "Gauge changes leave the same tank available to pumps");
  level.setBlock(core.east(),Blocks.AIR.defaultBlockState(),3);
  h.assertTrue(invalidations.get() > 0 && cache.getCapability() == null
          && port.fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE)==0 && port.drain(1000,FluidAction.EXECUTE).isEmpty(),
          "Dismantling invalidates the connection and the old tank cannot operate");h.succeed();
 }
}
