package com.agrievolution.content;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.RegisterEvent;

@GameTestHolder("agri_evolution") @PrefixGameTestTemplate(false)
public final class RemainingAuditGameTests {
    private static Runnable callback;
    private static int query, trigger;
    private static Runnable neighborCallback;
    private static BlockPos observedMachine;
    private static final ResourceLocation SEED = ResourceLocation.fromNamespaceAndPath("agri_evolution","test_remaining_audit_seed");
    private static final ResourceLocation OBSERVER = ResourceLocation.fromNamespaceAndPath("agri_evolution","test_seed_inventory_observer");
    @EventBusSubscriber(modid="agri_evolution",bus=EventBusSubscriber.Bus.MOD)
    public static final class Registry {
        @SubscribeEvent public static void register(RegisterEvent event) {
            event.register(Registries.BLOCK,OBSERVER,()->new Block(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()) {
                @Override public void onNeighborChange(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.LevelReader level,BlockPos pos,BlockPos neighbor) {
                    if(neighborCallback!=null&&neighbor.equals(observedMachine)){var action=neighborCallback;neighborCallback=null;action.run();}
                }
            });
            event.register(Registries.ITEM,SEED,()->new BlockItem(Blocks.WHEAT,new Item.Properties()) {
                @Override public Block getBlock() {
                    if (callback != null && ++query == trigger) { var action=callback;callback=null;action.run(); }
                    return super.getBlock();
                }
            });
        }
    }
    private static ItemStack seed(int count) { return new ItemStack(BuiltInRegistries.ITEM.get(SEED),count); }
    private static void hook(int at, Runnable action) { query=0;trigger=at;callback=action; }
    private static VerticalGreenhouseBlockEntity greenhouse(GameTestHelper h) {
        var base=h.absolutePos(new BlockPos(4,210,4));
        h.getLevel().setBlock(base.below(),Blocks.STONE.defaultBlockState(),2);
        for(int part=0;part<3;part++) h.getLevel().setBlock(base.above(part),AEBlocks.VERTICAL_GREENHOUSE.get().defaultBlockState().setValue(VerticalGreenhouseBlock.SECTION,part),2);
        var plant=(VerticalGreenhouseBlockEntity)h.getLevel().getBlockEntity(base);
        h.assertTrue(plant.complete(),"Audit column is complete");
        return plant;
    }
    @GameTest(template="empty") public static void verticalMenuPreservesReplacedInventorySource(GameTestHelper h) {
        var plant=greenhouse(h);var player=h.makeMockPlayer(GameType.SURVIVAL);player.setPos(plant.getBlockPos().getCenter());
        var menu=new VerticalGreenhouseMenu(1,player.getInventory(),plant);player.getInventory().setItem(9,seed(4));
        hook(1,()->player.getInventory().setItem(9,new ItemStack(Items.DIAMOND,7)));
        try { menu.quickMoveStack(player,VerticalGreenhouseBlockEntity.SLOTS); } finally { callback=null; }
        h.assertTrue(player.getInventory().getItem(9).is(Items.DIAMOND)&&player.getInventory().getItem(9).getCount()==7&&plant.getItem(0).isEmpty(),
                "A vertical greenhouse transfer must preserve a newer inventory source");h.succeed();
    }
    @GameTest(template="empty") public static void verticalMenuPreservesReplacedCursor(GameTestHelper h) {
        var plant=greenhouse(h);var player=h.makeMockPlayer(GameType.SURVIVAL);player.setPos(plant.getBlockPos().getCenter());
        var menu=new VerticalGreenhouseMenu(1,player.getInventory(),plant);menu.setCarried(seed(4));
        hook(1,()->menu.setCarried(new ItemStack(Items.DIAMOND,7)));
        try { menu.clicked(0,0,ClickType.PICKUP,player); } finally { callback=null; }
        h.assertTrue(menu.getCarried().is(Items.DIAMOND)&&menu.getCarried().getCount()==7&&plant.getItem(0).isEmpty(),
                "A vertical greenhouse click must preserve a newer cursor");h.succeed();
    }
    @GameTest(template="empty") public static void verticalBreedingRechecksChangedParents(GameTestHelper h) {
        var plant=greenhouse(h);plant.setItem(0,seed(1));plant.setItem(1,seed(1));plant.addBucket();
        for(int second=0;second<34;second++) plant.runSecond(h.getLevel());
        hook(1,()->plant.setItem(0,new ItemStack(Items.DIAMOND)));
        try { plant.runSecond(h.getLevel()); } finally { callback=null; }
        h.assertTrue(plant.getItem(VerticalGreenhouseBlockEntity.OFFSPRING).isEmpty(),"Changed native parents must not produce an unrelated item");h.succeed();
    }
    @GameTest(template="empty") public static void vaultIntakeRechecksNewLock(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(4,210,4));h.getLevel().setBlock(pos,AEBlocks.SEED_VAULT.get().defaultBlockState(),2);
        var vault=(SeedVaultBlockEntity)h.getLevel().getBlockEntity(pos);vault.setItem(0,seed(3));
        hook(1,()->vault.toggleLock(0));
        try { vault.receiveOffspring(seed(1)); } finally { callback=null; }
        h.assertTrue(query==1&&vault.getItem(0).getCount()==3,"Automatic offspring intake must classify once and preserve a newly locked stack");h.succeed();
    }
    @GameTest(template="empty") public static void vaultIntakeRejectsDetachedEntity(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(4,210,4));h.getLevel().setBlock(pos,AEBlocks.SEED_VAULT.get().defaultBlockState(),2);
        var vault=(SeedVaultBlockEntity)h.getLevel().getBlockEntity(pos);vault.setItem(0,seed(3));
        hook(1,()->{h.getLevel().removeBlockEntity(pos);h.getLevel().setBlockEntity(new SeedVaultBlockEntity(pos,h.getLevel().getBlockState(pos)));});
        boolean accepted;
        try { accepted=vault.receiveOffspring(seed(1)); } finally { callback=null; }
        h.assertTrue(!accepted&&vault.getItem(0).getCount()==3,"A detached vault must not report a successful delivery");h.succeed();
    }
    @GameTest(template="empty") public static void stationTickRechecksEntityAfterClassification(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(4,210,4));h.getLevel().setBlock(pos,AEBlocks.SEED_BREEDING_STATION.get().defaultBlockState(),2);
        var station=(SeedBreedingStationBlockEntity)h.getLevel().getBlockEntity(pos);station.setItem(0,seed(1));station.setItem(1,seed(1));station.setItem(2,new ItemStack(AEItems.NUTRIENT_COMPOST.get(),2));
        for(int tick=0;tick<139;tick++) SeedBreedingStationBlockEntity.serverTick(h.getLevel(),pos,station.getBlockState(),station);
        hook(1,()->{h.getLevel().removeBlockEntity(pos);h.getLevel().setBlockEntity(new SeedBreedingStationBlockEntity(pos,h.getLevel().getBlockState(pos)));});
        try { SeedBreedingStationBlockEntity.serverTick(h.getLevel(),pos,station.getBlockState(),station); } finally { callback=null; }
        h.assertTrue(station.getItem(4).isEmpty()&&station.getItem(2).getCount()==2,"A station detached during seed inspection must stop before consuming compost");h.succeed();
    }
    @GameTest(template="empty") public static void importedLunchBoxCanStillFeedVisibleFood(GameTestHelper h) {
        var player=h.makeMockPlayer(GameType.SURVIVAL);player.getFoodData().setFoodLevel(1);player.getFoodData().setSaturation(0);
        var contents=net.minecraft.core.NonNullList.withSize(10,ItemStack.EMPTY);contents.set(0,new ItemStack(Items.BREAD,3));contents.set(9,new ItemStack(Items.APPLE,7));
        var box=new ItemStack(AEItems.LUNCH_BOX.get());box.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(contents));player.getInventory().setItem(0,box);
        h.assertTrue(LunchBoxItem.feedOne(player)&&player.getFoodData().getFoodLevel()>1&&box.get(DataComponents.CONTAINER).getStackInSlot(9).getCount()==7,
                "An imported overflow slot must not silently disable visible food or be lost during feeding");h.succeed();
    }

    @GameTest(template="empty") public static void verticalMenuKeepsNormalPickupHotbarAndDrag(GameTestHelper h) {
        var plant=greenhouse(h);var player=h.makeMockPlayer(GameType.SURVIVAL);player.setPos(plant.getBlockPos().getCenter());
        var menu=new VerticalGreenhouseMenu(1,player.getInventory(),plant);
        menu.setCarried(seed(4));menu.clicked(0,0,ClickType.PICKUP,player);
        h.assertTrue(plant.getItem(0).getCount()==1&&menu.getCarried().getCount()==3,"Pickup fills one tray and retains the other seeds");
        player.getInventory().setItem(2,seed(5));menu.clicked(1,2,ClickType.SWAP,player);
        h.assertTrue(plant.getItem(1).getCount()==1&&player.getInventory().getItem(2).getCount()==4,"Hotbar insertion respects one seed per tray");
        menu.clicked(-999,0,ClickType.QUICK_CRAFT,player);
        menu.clicked(2,1,ClickType.QUICK_CRAFT,player);menu.clicked(3,1,ClickType.QUICK_CRAFT,player);
        menu.clicked(-999,2,ClickType.QUICK_CRAFT,player);
        h.assertTrue(plant.getItem(2).getCount()==1&&plant.getItem(3).getCount()==1&&menu.getCarried().getCount()==1,"Dragging preserves the seeds left after filling two trays");h.succeed();
    }
    @GameTest(template="empty") public static void verticalMenuPreservesEditedHotbar(GameTestHelper h) {
        var plant=greenhouse(h);var player=h.makeMockPlayer(GameType.SURVIVAL);player.setPos(plant.getBlockPos().getCenter());
        var menu=new VerticalGreenhouseMenu(1,player.getInventory(),plant);var held=seed(4);player.getInventory().setItem(2,held);
        hook(1,()->held.setCount(2));
        try {menu.clicked(0,2,ClickType.SWAP,player);}finally{callback=null;}
        h.assertTrue(held.getCount()==2&&plant.getItem(0).isEmpty(),"An in-place hotbar edit cancels the pending insertion");h.succeed();
    }
    @GameTest(template="empty") public static void verticalBreedingRejectsReentrantWork(GameTestHelper h) {
        var plant=greenhouse(h);plant.setItem(0,seed(1));plant.setItem(1,seed(1));plant.addBucket();
        for(int second=0;second<34;second++)plant.runSecond(h.getLevel());
        hook(1,()->plant.runSecond(h.getLevel()));
        try{plant.runSecond(h.getLevel());}finally{callback=null;}
        h.assertTrue(plant.getItem(VerticalGreenhouseBlockEntity.OFFSPRING).getCount()==1&&plant.water()==500,"A nested tick cannot finish a second cross or spend extra water");h.succeed();
    }
    @GameTest(template="empty") public static void verticalBreedingPreservesChangedSettings(GameTestHelper h) {
        var plant=greenhouse(h);plant.setItem(0,seed(1));plant.setItem(1,seed(1));plant.addBucket();
        for(int second=0;second<34;second++)plant.runSecond(h.getLevel());
        hook(1,()->plant.toggle(1));
        try{plant.runSecond(h.getLevel());}finally{callback=null;}
        h.assertTrue(plant.getItem(VerticalGreenhouseBlockEntity.OFFSPRING).isEmpty()&&plant.water()==1000&&(plant.data.get(1)&2)==0,"Disabling breeding during inspection keeps the new setting and water");h.succeed();
    }
    @GameTest(template="empty") public static void vaultIntakeClassifiesOnceAndRejectsReentry(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(4,210,4));h.getLevel().setBlock(pos,AEBlocks.SEED_VAULT.get().defaultBlockState(),2);
        var vault=(SeedVaultBlockEntity)h.getLevel().getBlockEntity(pos);vault.setItem(0,seed(3));boolean[] nested={true};
        hook(1,()->nested[0]=vault.receiveOffspring(seed(1)));
        boolean received;try{received=vault.receiveOffspring(seed(1));}finally{callback=null;}
        h.assertTrue(received&&!nested[0]&&query==1&&vault.getItem(0).getCount()==4,"One intake accepts one offspring with one classification");h.succeed();
    }
    @GameTest(template="empty") public static void vaultIntakePreservesChangedFilter(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(4,210,4));h.getLevel().setBlock(pos,AEBlocks.SEED_VAULT.get().defaultBlockState(),2);
        var vault=(SeedVaultBlockEntity)h.getLevel().getBlockEntity(pos);hook(1,()->vault.cycleMinimum(0));
        boolean received;try{received=vault.receiveOffspring(seed(1));}finally{callback=null;}
        h.assertTrue(!received&&vault.getItem(0).isEmpty()&&vault.minimum(0)==10,"A changed intake filter cancels delivery");h.succeed();
    }
    @GameTest(template="empty") public static void greenhouseRetainsDeliveryToReplacedVault(GameTestHelper h) {
        var plant=greenhouse(h);var pos=plant.getBlockPos().north();h.getLevel().setBlock(pos,AEBlocks.SEED_VAULT.get().defaultBlockState(),2);
        var oldVault=(SeedVaultBlockEntity)h.getLevel().getBlockEntity(pos);plant.cycleOutputRoute(1);plant.cycleOutputRoute(1);
        plant.setItem(VerticalGreenhouseBlockEntity.OFFSPRING,seed(1));
        hook(1,()->{h.getLevel().removeBlockEntity(pos);h.getLevel().setBlockEntity(new SeedVaultBlockEntity(pos,h.getLevel().getBlockState(pos)));});
        try{plant.runSecond(h.getLevel());}finally{callback=null;}
        h.assertTrue(query==1&&plant.getItem(VerticalGreenhouseBlockEntity.OFFSPRING).getCount()==1&&oldVault.getItem(0).isEmpty(),"A failed vault delivery restores the reserved greenhouse seed");h.succeed();
    }
    private static SeedBreedingStationBlockEntity readyStation(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(4,210,4));h.getLevel().setBlock(pos,AEBlocks.SEED_BREEDING_STATION.get().defaultBlockState(),2);
        var station=(SeedBreedingStationBlockEntity)h.getLevel().getBlockEntity(pos);station.setItem(0,seed(1));station.setItem(1,seed(1));station.setItem(2,new ItemStack(AEItems.NUTRIENT_COMPOST.get(),2));
        for(int tick=0;tick<139;tick++)SeedBreedingStationBlockEntity.serverTick(h.getLevel(),pos,station.getBlockState(),station);
        return station;
    }
    @GameTest(template="empty") public static void stationPreservesChangedParent(GameTestHelper h) {
        var station=readyStation(h);hook(1,()->station.setItem(0,new ItemStack(Items.DIAMOND,7)));
        try{SeedBreedingStationBlockEntity.serverTick(h.getLevel(),station.getBlockPos(),station.getBlockState(),station);}finally{callback=null;}
        h.assertTrue(station.getItem(4).isEmpty()&&station.getItem(0).getCount()==7&&station.getItem(2).getCount()==2,"Changed parents cancel completion without consuming compost");h.succeed();
    }
    @GameTest(template="empty") public static void stationRejectsReentrantCompletion(GameTestHelper h) {
        var station=readyStation(h);hook(1,()->SeedBreedingStationBlockEntity.serverTick(h.getLevel(),station.getBlockPos(),station.getBlockState(),station));
        try{SeedBreedingStationBlockEntity.serverTick(h.getLevel(),station.getBlockPos(),station.getBlockState(),station);}finally{callback=null;}
        h.assertTrue(station.getItem(4).getCount()==1&&station.getItem(2).getCount()==1,"Nested ticks complete only one offspring and spend one compost");h.succeed();
    }
    @GameTest(template="empty") public static void lunchBoxDoesNotEatHiddenOverflow(GameTestHelper h) {
        var player=h.makeMockPlayer(GameType.SURVIVAL);player.getFoodData().setFoodLevel(1);
        var contents=net.minecraft.core.NonNullList.withSize(10,ItemStack.EMPTY);contents.set(9,new ItemStack(Items.APPLE,7));
        var box=new ItemStack(AEItems.LUNCH_BOX.get());box.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(contents));player.getInventory().setItem(0,box);
        h.assertTrue(!LunchBoxItem.feedOne(player)&&box.get(DataComponents.CONTAINER).getStackInSlot(9).getCount()==7,"Hidden imported slots remain preserved and are never eaten");h.succeed();
    }
    @GameTest(template="empty") public static void hopperRetainsSeedWhenStationIsReplaced(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(4,210,4));h.getLevel().setBlock(pos,AEBlocks.SEED_BREEDING_STATION.get().defaultBlockState(),2);
        var station=(SeedBreedingStationBlockEntity)h.getLevel().getBlockEntity(pos);
        hook(1,()->{h.getLevel().removeBlockEntity(pos);h.getLevel().setBlockEntity(new SeedBreedingStationBlockEntity(pos,h.getLevel().getBlockState(pos)));});
        ItemStack remainder;try{remainder=net.minecraft.world.level.block.entity.HopperBlockEntity.addItem(null,station,seed(1),net.minecraft.core.Direction.UP);}finally{callback=null;}
        h.assertTrue(remainder.getCount()==1&&station.getItem(0).isEmpty(),"Hoppers must retain seeds rejected by a replaced station");h.succeed();
    }
    @GameTest(template="empty") public static void hopperPreservesStationDestinationChange(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(4,210,4));h.getLevel().setBlock(pos,AEBlocks.SEED_BREEDING_STATION.get().defaultBlockState(),2);
        var station=(SeedBreedingStationBlockEntity)h.getLevel().getBlockEntity(pos);hook(1,()->station.setItem(0,new ItemStack(Items.DIAMOND,7)));
        try{net.minecraft.world.level.block.entity.HopperBlockEntity.addItem(null,station,seed(1),net.minecraft.core.Direction.UP);}finally{callback=null;}
        h.assertTrue(station.getItem(0).is(Items.DIAMOND)&&station.getItem(0).getCount()==7,"A hopper insertion must not replace newer destination contents");h.succeed();
    }
    @GameTest(template="empty") public static void greenhouseDashboardPreviewRejectsMissingPlayer(GameTestHelper h) {
        new BotanicalGreenhouseMenu(1,new Inventory(null)).clicked(-999,0,ClickType.PICKUP,null);h.succeed();
    }
    @GameTest(template="empty") public static void harvestDashboardPreviewRejectsMissingPlayer(GameTestHelper h) {
        new BotanicalGreenhouseHarvestMenu(1,new Inventory(null)).clicked(-999,0,ClickType.PICKUP,null);h.succeed();
    }
    @GameTest(template="empty") public static void irrigationDashboardPreviewRejectsMissingPlayer(GameTestHelper h) {
        new IrrigationControllerMenu(1,new Inventory(null),(IrrigationControllerBlockEntity)null).clicked(-999,0,ClickType.PICKUP,null);h.succeed();
    }
    private static void neighborTransfer(GameTestHelper h,boolean vault,boolean cursor) {
        var pos=h.absolutePos(new BlockPos(4,210,4));h.getLevel().setBlock(pos,(vault?AEBlocks.SEED_VAULT.get():AEBlocks.SEED_BREEDING_STATION.get()).defaultBlockState(),2);
        h.getLevel().setBlock(pos.north(),BuiltInRegistries.BLOCK.get(OBSERVER).defaultBlockState(),2);
        var player=h.makeMockPlayer(GameType.SURVIVAL);player.setPos(pos.getCenter());
        AbstractContainerMenu menu=vault?new SeedVaultMenu(1,player.getInventory(),(SeedVaultBlockEntity)h.getLevel().getBlockEntity(pos)):new SeedBreedingStationMenu(1,player.getInventory(),(SeedBreedingStationBlockEntity)h.getLevel().getBlockEntity(pos));
        if(cursor)menu.setCarried(new ItemStack(Items.WHEAT_SEEDS,4));else player.getInventory().setItem(9,new ItemStack(Items.WHEAT_SEEDS,4));
        boolean[] notified={false};observedMachine=pos;neighborCallback=()->{notified[0]=true;if(cursor)menu.setCarried(new ItemStack(Items.DIAMOND,7));else player.getInventory().setItem(9,new ItemStack(Items.DIAMOND,7));};
        try{if(cursor)menu.clicked(0,0,ClickType.PICKUP,player);else menu.quickMoveStack(player,vault?27:5);}finally{neighborCallback=null;observedMachine=null;}
        var remaining=cursor?menu.getCarried():player.getInventory().getItem(9);
        h.assertTrue(notified[0]?remaining.is(Items.DIAMOND)&&remaining.getCount()==7:remaining.isEmpty()&&menu.getSlot(0).getItem().getCount()==4,"A storage notification must not overwrite a newer source or cursor");h.succeed();
    }
    @GameTest(template="empty") public static void vaultTransferPreservesSourceThroughNeighborCallback(GameTestHelper h){neighborTransfer(h,true,false);}
    @GameTest(template="empty") public static void stationTransferPreservesSourceThroughNeighborCallback(GameTestHelper h){neighborTransfer(h,false,false);}
    @GameTest(template="empty") public static void vaultClickPreservesCursorThroughNeighborCallback(GameTestHelper h){neighborTransfer(h,true,true);}
    @GameTest(template="empty") public static void stationClickPreservesCursorThroughNeighborCallback(GameTestHelper h){neighborTransfer(h,false,true);}
    private static WaterReservoirBlockEntity connectedWaterSource(GameTestHelper h,BlockPos receiver) {
        var core=receiver.east(4);var tank=ReservoirGameTests.tank(h,core);
        for(int offset=1;offset<=2;offset++)h.getLevel().setBlock(receiver.east(offset),AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(),2);
        h.getLevel().setBlock(core.above(2),BuiltInRegistries.BLOCK.get(OBSERVER).defaultBlockState(),2);observedMachine=core.above();return tank;
    }
    @GameTest(template="empty") public static void verticalBreedingRechecksParentsAfterWaterRefill(GameTestHelper h) {
        var plant=greenhouse(h);plant.setItem(0,new ItemStack(AEItems.TOMATO_SEED.get()));plant.setItem(1,new ItemStack(AEItems.TOMATO_SEED.get()));plant.addBucket();
        for(int second=0;second<34;second++)plant.runSecond(h.getLevel());var saved=plant.saveWithoutMetadata(h.getLevel().registryAccess());saved.putInt("Water",500);plant.loadAdditional(saved,h.getLevel().registryAccess());
        connectedWaterSource(h,plant.getBlockPos());neighborCallback=()->plant.setItem(0,new ItemStack(Items.DIAMOND,7));
        try{plant.runSecond(h.getLevel());}finally{neighborCallback=null;observedMachine=null;}
        h.assertTrue(plant.getItem(VerticalGreenhouseBlockEntity.OFFSPRING).isEmpty()&&plant.getItem(0).getCount()==7,"Water-source notifications must not breed a changed non-seed parent");h.succeed();
    }
    @GameTest(template="empty") public static void verticalRefillCannotOverflowFromManualFillCallback(GameTestHelper h) {
        var plant=greenhouse(h);plant.setItem(0,new ItemStack(AEItems.TOMATO_SEED.get()));var saved=plant.saveWithoutMetadata(h.getLevel().registryAccess());saved.putInt("Water",500);plant.loadAdditional(saved,h.getLevel().registryAccess());
        connectedWaterSource(h,plant.getBlockPos());neighborCallback=()->{for(int bucket=0;bucket<16;bucket++)plant.addBucket();};
        try{plant.runSecond(h.getLevel());}finally{neighborCallback=null;observedMachine=null;}
        h.assertTrue(plant.water()==1500,"A greenhouse refill rejects overlapping fills and retains its one paid network bucket");h.succeed();
    }
    private static IrrigationSprinklerBlockEntity suppliedSprinkler(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(4,210,4));h.getLevel().setBlock(pos.below(3),Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE,0),2);h.getLevel().setBlock(pos,AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(),2);
        return (IrrigationSprinklerBlockEntity)h.getLevel().getBlockEntity(pos);
    }
    @GameTest(template="empty") public static void replacedSprinklerCannotWaterAfterRefillNotification(GameTestHelper h) {
        var head=suppliedSprinkler(h);var pos=head.getBlockPos();connectedWaterSource(h,pos);
        neighborCallback=()->{h.getLevel().removeBlockEntity(pos);h.getLevel().setBlockEntity(new IrrigationSprinklerBlockEntity(pos,h.getLevel().getBlockState(pos)));};
        try{head.irrigateCycle();}finally{neighborCallback=null;observedMachine=null;}
        h.assertTrue(h.getLevel().getBlockState(pos.below(3)).getValue(FarmBlock.MOISTURE)==0,"A sprinkler removed by a source notification cannot irrigate from its detached tick");h.succeed();
    }
    @GameTest(template="empty") public static void sprinklerRefillCannotRunNestedSpray(GameTestHelper h) {
        var head=suppliedSprinkler(h);var source=connectedWaterSource(h,head.getBlockPos());source.addBucket();neighborCallback=head::irrigateCycle;
        try{head.irrigateCycle();}finally{neighborCallback=null;observedMachine=null;}
        h.assertTrue(source.water()==1000&&head.water()==975,"One watering cycle draws one bucket and pays one spray despite nested source notifications");h.succeed();
    }
    @GameTest(template="empty") public static void sprinklerRefillCannotOverflowFromManualFillCallback(GameTestHelper h) {
        var head=suppliedSprinkler(h);connectedWaterSource(h,head.getBlockPos());neighborCallback=()->{for(int bucket=0;bucket<4;bucket++)head.fillBucket();};
        try{head.irrigateCycle();}finally{neighborCallback=null;observedMachine=null;}
        h.assertTrue(head.water()<=IrrigationSprinklerBlockEntity.CAPACITY,"A network refill cannot exceed tank capacity when a source notification also tries to fill it");h.succeed();
    }
    private static void preview(GameTestHelper h,int kind) {
        var inv=new Inventory(null);
        AbstractContainerMenu menu=switch(kind) {
            case 0->new BatchCookingMenu(1,inv);case 1->new GardenWorkMenu(1,inv);case 2->new VerticalGreenhouseMenu(1,inv);
            case 3->new BotanicalGreenhouseStorageMenu(1,inv);case 4->new BotanicalGreenhouseSeedBankMenu(1,inv);
            case 5->new SoilResearchTableMenu(1,inv);default->new BotanicalGeneticsLaboratoryMenu(1,inv);
        };
        menu.clicked(0,0,ClickType.PICKUP,null);
        h.assertTrue(menu.quickMoveStack(null,0).isEmpty()&&!menu.clickMenuButton(null,0),"Preview menus must reject actions without a player");h.succeed();
    }
    @GameTest(template="empty") public static void cookingPreviewRejectsMissingPlayer(GameTestHelper h){preview(h,0);}
    @GameTest(template="empty") public static void ordersPreviewRejectsMissingPlayer(GameTestHelper h){preview(h,1);}
    @GameTest(template="empty") public static void verticalPreviewRejectsMissingPlayer(GameTestHelper h){preview(h,2);}
    @GameTest(template="empty") public static void storagePreviewRejectsMissingPlayer(GameTestHelper h){preview(h,3);}
    @GameTest(template="empty") public static void bankPreviewRejectsMissingPlayer(GameTestHelper h){preview(h,4);}
    @GameTest(template="empty") public static void soilPreviewRejectsMissingPlayer(GameTestHelper h){preview(h,5);}
    @GameTest(template="empty") public static void labPreviewRejectsMissingPlayer(GameTestHelper h){preview(h,6);}
}
