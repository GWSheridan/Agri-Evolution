package com.agrievolution.content;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
@GameTestHolder("agri_evolution") @PrefixGameTestTemplate(false)
public final class ReservoirAutomationGameTests {
 @GameTest(template="empty",timeoutTicks=240) public static void scheduledGenerationAndUpgradeRecovery(GameTestHelper h){
  var core=h.absolutePos(new BlockPos(3,55,3));var tank=ReservoirGameTests.tank(h,core);tank.upgrade();
  h.runAfterDelay(199,()->h.assertTrue(tank.water()==1000,"No bucket before ten actual seconds"));
  h.runAfterDelay(201,()->{
   h.assertTrue(tank.water()==2000,"Scheduled core ticks generate exactly one bucket");
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
