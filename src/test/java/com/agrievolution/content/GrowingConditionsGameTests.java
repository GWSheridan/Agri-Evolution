package com.agrievolution.content;

import com.agrievolution.farming.*;
import com.agrievolution.genetics.CropSpecies;
import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.gametest.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

@GameTestHolder("agri_evolution") @PrefixGameTestTemplate(false)
public final class GrowingConditionsGameTests {
    private static final ResourceLocation COMPAT=ResourceLocation.fromNamespaceAndPath("agri_evolution","test_rotation_crop");
    @EventBusSubscriber(modid="agri_evolution",bus=EventBusSubscriber.Bus.MOD)
    public static final class Registry {
        @SubscribeEvent public static void register(RegisterEvent event) {
            event.register(Registries.BLOCK,COMPAT,()->new CropBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().noCollission()) {
                @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {
                    super.createBlockStateDefinition(builder);builder.add(BlockStateProperties.HORIZONTAL_FACING);
                }
            });
        }
    }
    private static BlockPos soil(GameTestHelper h) {
        var p=h.absolutePos(new BlockPos(4,180,4));var l=h.getLevel();
        l.setBlock(p,Blocks.STONE.defaultBlockState(),2);SoilCareData.previous(l,p);
        l.setBlock(p,Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE,7),2);
        l.setBlock(p.above(),Blocks.AIR.defaultBlockState(),2);return p;
    }
    private static AgriCropBlock crop(ServerLevel l,BlockPos soil,CropSpecies species,int age) {
        var block=(AgriCropBlock)BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("agri_evolution",species.blockId()));
        l.setBlock(soil.above(),block.defaultBlockState().setValue(AgriCropBlock.AGE,age),2);return block;
    }
    private static void harvest(GameTestHelper h,BlockPos soil,CropSpecies species,int age) {
        var l=h.getLevel();var block=crop(l,soil,species,age);var state=l.getBlockState(soil.above());var be=l.getBlockEntity(soil.above());
        l.removeBlock(soil.above(),false);
        block.playerDestroy(l,h.makeMockPlayer(GameType.SURVIVAL),soil.above(),state,be,ItemStack.EMPTY);
    }
    @GameTest(template="empty") public static void rotationFollowsCompletedFamiliesRatherThanPlanting(GameTestHelper h) {
        var s=soil(h);var l=h.getLevel();crop(l,s,CropSpecies.IRON,1);
        h.assertTrue(SoilCareData.rotationBonus(l,s.above())==0,"A fresh plot has no invented harvest history");
        harvest(h,s,CropSpecies.PEAS,7);crop(l,s,CropSpecies.IRON,0);
        h.assertTrue(SoilCareData.rotationBonus(l,s.above())==10,"Resources after mature legumes get a bonus");
        harvest(h,s,CropSpecies.IRON,7);crop(l,s,CropSpecies.COPPER,0);
        h.assertTrue(SoilCareData.rotationBonus(l,s.above())==0,"Changing metal seeds alone is not rotation");
        crop(l,s,CropSpecies.PEAS,0);crop(l,s,CropSpecies.COPPER,0);
        h.assertTrue(SoilCareData.previous(l,s)==CropFamily.RESOURCE,"Merely planting another family never rewrites history");h.succeed();
    }
    @GameTest(template="empty") public static void unripeAndCreativeHarvestsDoNotAdvanceRotation(GameTestHelper h) {
        var s=soil(h);harvest(h,s,CropSpecies.PEAS,2);
        h.assertTrue(SoilCareData.previous(h.getLevel(),s)==CropFamily.UNKNOWN,"Unripe harvests do not qualify");
        var block=crop(h.getLevel(),s,CropSpecies.PEAS,7);var state=h.getLevel().getBlockState(s.above());
        h.getLevel().removeBlock(s.above(),false);
        var creative=h.makeMockPlayer(GameType.CREATIVE);GameType.CREATIVE.updatePlayerAbilities(creative.getAbilities());
        block.playerDestroy(h.getLevel(),creative,s.above(),state,null,ItemStack.EMPTY);
        h.assertTrue(SoilCareData.previous(h.getLevel(),s)==CropFamily.UNKNOWN,"Creative breaking does not qualify");h.succeed();
    }
    @GameTest(template="empty") public static void failedAutomaticRemovalCannotGrantRotation(GameTestHelper h) {
        var s=soil(h);crop(h.getLevel(),s,CropSpecies.LENTIL,7);
        CompanionPlanting.harvested(h.getLevel(),s.above(),CropSpecies.LENTIL);
        h.assertTrue(SoilCareData.previous(h.getLevel(),s)==CropFamily.UNKNOWN,"An unchanged mature crop is not a completed harvest");h.succeed();
    }
    @GameTest(template="empty") public static void familiesCoverFoodFlowersResourcesAndUnknownCrops(GameTestHelper h) {
        h.assertTrue(CropFamily.of(CropSpecies.OATS)==CropFamily.GRAIN&&CropFamily.of(CropSpecies.PEANUT)==CropFamily.LEGUME
            &&CropFamily.of(CropSpecies.PARSNIP)==CropFamily.ROOT&&CropFamily.of(CropSpecies.SPINACH)==CropFamily.LEAFY
            &&CropFamily.of(CropSpecies.DILL)==CropFamily.HERB&&CropFamily.of(CropSpecies.TOMATO)==CropFamily.FRUIT
            &&CropFamily.of(CropSpecies.MOON_ORCHID)==CropFamily.FLOWER&&CropFamily.of(CropSpecies.ENDER_PEARL)==CropFamily.RESOURCE,
            "Existing crops have broad, useful rotation families");
        h.assertTrue(CropFamily.of(Blocks.WHEAT.defaultBlockState())==CropFamily.GRAIN
            &&CropFamily.of(Blocks.CARROTS.defaultBlockState())==CropFamily.ROOT
            &&CropFamily.of(Blocks.ATTACHED_PUMPKIN_STEM.defaultBlockState())==CropFamily.FRUIT
            &&CropFamily.of(Blocks.STONE.defaultBlockState())==CropFamily.UNKNOWN,"Vanilla defaults and unknown blocks are safe");h.succeed();
    }
    @GameTest(template="empty") public static void sameFamilyKeepsNormalGrowthAndSoilUpgradesKeepHistory(GameTestHelper h) {
        var s=soil(h);var l=h.getLevel();harvest(h,s,CropSpecies.TOMATO,7);crop(l,s,CropSpecies.PEPPER,0);
        h.assertTrue(SoilCareData.rotationBonus(l,s.above())==0,"Repeated family has baseline growth");
        l.setBlock(s,AEBlocks.NUTRIENT_FARMLAND.get().defaultBlockState(),2);crop(l,s,CropSpecies.BEANS,0);
        h.assertTrue(SoilCareData.rotationBonus(l,s.above())==10,"Upgrading farmland retains the plot history");
        l.setBlock(s.above(),Blocks.PUMPKIN_STEM.defaultBlockState(),2);
        h.assertTrue(SoilCareData.rotationBonus(l,s.above())==0,"Unsupported native growth styles are not shown as receiving a bonus");
        l.setBlock(s,Blocks.STONE.defaultBlockState(),2);SoilCareData.previous(l,s);l.setBlock(s,Blocks.FARMLAND.defaultBlockState(),2);
        h.assertTrue(SoilCareData.previous(l,s)==CropFamily.UNKNOWN,"A plot observed without farmland loses old history");h.succeed();
    }
    private static BlockDropsEvent drops(GameTestHelper h,BlockPos s,int age) {
        var state=Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,age);h.getLevel().setBlock(s.above(),state,2);
        var list=new ArrayList<ItemEntity>();list.add(new ItemEntity(h.getLevel(),s.getX(),s.getY()+1,s.getZ(),new ItemStack(Items.WHEAT)));
        return new BlockDropsEvent(h.getLevel(),s.above(),state,null,list,null,ItemStack.EMPTY);
    }
    private static void nativeHarvest(GameTestHelper h,int mode) {
        var s=soil(h);var l=h.getLevel();var e=drops(h,s,mode==3?2:7);SoilCareData.nativeDrops(e);
        if(mode!=2)l.removeBlock(s.above(),false);
        if(mode==1)e.setCanceled(true);
        if(mode==4)e.getDrops().clear();
        if(mode==5)SoilCareData.recordHarvest(l,s,CropFamily.LEGUME);
        if(mode==6)SoilCareData.applyMulch(l,s);
        SoilCareData.get(l).finishHarvests(l);
        var expected=mode==0||mode==6?CropFamily.GRAIN:mode==5?CropFamily.LEGUME:CropFamily.UNKNOWN;
        h.assertTrue(SoilCareData.previous(l,s)==expected,"Only real mature drops update the plot; mode="+mode);h.succeed();
    }
    @GameTest(template="empty") public static void matureVanillaDropsRecordHistory(GameTestHelper h){nativeHarvest(h,0);}
    @GameTest(template="empty") public static void cancelledVanillaDropsPreserveHistory(GameTestHelper h){nativeHarvest(h,1);}
    @GameTest(template="empty") public static void simulatedDropsPreserveHistory(GameTestHelper h){nativeHarvest(h,2);}
    @GameTest(template="empty") public static void unripeVanillaDropsPreserveHistory(GameTestHelper h){nativeHarvest(h,3);}
    @GameTest(template="empty") public static void removedLootPreservesHistory(GameTestHelper h){nativeHarvest(h,4);}
    @GameTest(template="empty") public static void queuedDropsCannotOverwriteANewerHarvest(GameTestHelper h){nativeHarvest(h,5);}
    @GameTest(template="empty") public static void applyingMulchDoesNotCancelACompletedHarvest(GameTestHelper h){nativeHarvest(h,6);}
    @GameTest(template="empty") public static void realVanillaBreakUpdatesRotation(GameTestHelper h) {
        var s=soil(h);var l=h.getLevel();l.setBlock(s.above(),Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7),2);
        l.destroyBlock(s.above(),true);SoilCareData.get(l).finishHarvests(l);crop(l,s,CropSpecies.IRON,0);
        h.assertTrue(SoilCareData.rotationBonus(l,s.above())==10,"The subscribed drop event handles normal vanilla harvesting");h.succeed();
    }
    @GameTest(template="empty") public static void nativeGrowthBonusNeedsRotationAndNeverOvergrows(GameTestHelper h) {
        var s=soil(h);var l=h.getLevel();var old=Blocks.WHEAT.defaultBlockState();var grown=old.setValue(CropBlock.AGE,1);
        l.setBlock(s.above(),grown,2);SoilCareData.nativeGrowth(new CropGrowEvent.Post(l,s.above(),old,grown));
        h.assertTrue(l.getBlockState(s.above())==grown,"No history means normal native growth");
        SoilCareData.recordHarvest(l,s,CropFamily.LEGUME);int extras=0;
        for(int i=0;i<100;i++){l.setBlock(s.above(),grown,2);SoilCareData.nativeGrowth(new CropGrowEvent.Post(l,s.above(),old,grown));if(l.getBlockState(s.above()).getValue(CropBlock.AGE)==2)extras++;}
        h.assertTrue(extras>0&&extras<100,"Rotated native crops sometimes gain exactly one extra natural stage");
        l.setBlock(s.above(),old.setValue(CropBlock.AGE,7),2);SoilCareData.nativeGrowth(new CropGrowEvent.Post(l,s.above(),grown,l.getBlockState(s.above())));
        h.assertTrue(l.getBlockState(s.above()).getValue(CropBlock.AGE)==7,"Mature crops never exceed their maximum age");h.succeed();
    }
    @GameTest(template="empty") public static void taggedCompatibleCropsRetainTheirOtherProperties(GameTestHelper h) {
        var s=soil(h);var l=h.getLevel();var block=(CropBlock)BuiltInRegistries.BLOCK.get(COMPAT);
        var old=block.getStateForAge(0).setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.EAST);
        var grown=old.setValue(CropBlock.AGE,1);SoilCareData.recordHarvest(l,s,CropFamily.LEGUME);int extras=0;
        h.assertTrue(CropFamily.of(old)==CropFamily.GRAIN,"Datapack block tags opt compatible crops into rotation");
        for(int i=0;i<100;i++){
            l.setBlock(s.above(),grown,2);SoilCareData.nativeGrowth(new CropGrowEvent.Post(l,s.above(),old,grown));
            var result=l.getBlockState(s.above());if(result.getValue(CropBlock.AGE)==2)extras++;
            h.assertTrue(result.getValue(BlockStateProperties.HORIZONTAL_FACING)==Direction.EAST,"Bonus growth preserves the modded facing property");
        }
        h.assertTrue(extras>0,"Compatible crops receive the same natural-growth bonus");h.succeed();
    }
    @GameTest(template="empty") public static void taggedAgriFamiliesAgreeAtHarvestAndReplanting(GameTestHelper h) {
        var s=soil(h);harvest(h,s,CropSpecies.BARLEY,7);crop(h.getLevel(),s,CropSpecies.MINT,0);
        h.assertTrue(SoilCareData.previous(h.getLevel(),s)==CropFamily.HERB&&SoilCareData.rotationBonus(h.getLevel(),s.above())==0,
            "An optional family override is honoured both when recording a harvest and reading the next crop");h.succeed();
    }
    @GameTest(template="empty") public static void staleOrUnchangedGrowthEventsDoNotAddStages(GameTestHelper h) {
        var s=soil(h);var l=h.getLevel();SoilCareData.recordHarvest(l,s,CropFamily.LEGUME);
        var old=Blocks.WHEAT.defaultBlockState();var grown=old.setValue(CropBlock.AGE,1);var replacement=old.setValue(CropBlock.AGE,3);
        for(int i=0;i<100;i++) {
            l.setBlock(s.above(),replacement,2);SoilCareData.nativeGrowth(new CropGrowEvent.Post(l,s.above(),old,grown));
            h.assertTrue(l.getBlockState(s.above())==replacement,"A stale callback preserves a newer crop state");
            l.setBlock(s.above(),grown,2);SoilCareData.nativeGrowth(new CropGrowEvent.Post(l,s.above(),grown,grown));
            h.assertTrue(l.getBlockState(s.above())==grown,"A notification without growth cannot create growth");
        }h.succeed();
    }
    @GameTest(template="empty") public static void unloadedCareLookupsNeverGenerateChunks(GameTestHelper h) {
        var p=new BlockPos(25000000,80,25000000);var l=h.getLevel();
        h.assertTrue(!l.hasChunkAt(p)&&SoilCareData.previous(l,p)==CropFamily.UNKNOWN&&SoilCareData.rotationBonus(l,p.above())==0
            &&!SoilCareData.applyMulch(l,p)&&!SoilCareData.recordHarvest(l,p,CropFamily.GRAIN)&&!l.hasChunkAt(p),"Care lookups and mutations refuse unloaded soil");h.succeed();
    }
    @GameTest(template="empty") public static void careRecordsRoundTripWithoutChangingGenes(GameTestHelper h) {
        var s=soil(h);var l=h.getLevel();SoilCareData.recordHarvest(l,s,CropFamily.LEGUME);SoilCareData.applyMulch(l,s);
        var saved=SoilCareData.get(l).save(new CompoundTag(),l.registryAccess());var restored=SoilCareData.load(saved,l.registryAccess());
        h.assertTrue(restored.save(new CompoundTag(),l.registryAccess()).equals(saved),"Family IDs, expiry time and coordinates survive reload");
        crop(l,s,CropSpecies.IRON,1);var plant=(AECropBlockEntity)l.getBlockEntity(s.above());var genes=plant.genome();
        h.assertTrue(SoilCareData.rotationBonus(l,s.above())==10&&plant.genome().equals(genes),"Soil care changes no genetic stack");h.succeed();
    }
    private static final class AccessPlayer extends Player {
        BlockPos denied;Runnable callback;
        AccessPlayer(ServerLevel l,BlockPos pos){super(l,pos,0,new GameProfile(UUID.randomUUID(),"mulch-use"));getAbilities().mayBuild=true;setPos(Vec3.atCenterOf(pos));}
        @Override public boolean isCreative(){return false;}
        @Override public boolean isSpectator(){return false;}
        @Override public boolean mayUseItemAt(BlockPos pos,Direction side,ItemStack stack){if(callback!=null){var action=callback;callback=null;action.run();}return !pos.equals(denied);}
    }
    private static void use(Player player,BlockPos clicked){AEItems.COMPOST_MULCH.get().useOn(new UseOnContext(player,InteractionHand.OFF_HAND,new BlockHitResult(Vec3.atCenterOf(clicked),Direction.UP,clicked,false)));}
    @GameTest(template="empty") public static void offhandMulchUsesOneItemAndCannotStack(GameTestHelper h) {
        var s=soil(h);var p=h.makeMockPlayer(GameType.SURVIVAL);p.setPos(Vec3.atCenterOf(s));p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(AEItems.COMPOST_MULCH.get(),3));
        crop(h.getLevel(),s,CropSpecies.COAL,1);use(p,s.above());
        h.assertTrue(p.getOffhandItem().getCount()==2&&SoilCareData.mulchRemaining(h.getLevel(),s)==24000,"One item coats one plot from the crop target");
        use(p,s);h.assertTrue(p.getOffhandItem().getCount()==2,"An active layer cannot consume a second item");h.succeed();
    }
    @GameTest(template="empty") public static void creativeMulchIsFreeAndUnrelatedBlocksRejectIt(GameTestHelper h) {
        var s=soil(h);var p=h.makeMockPlayer(GameType.CREATIVE);GameType.CREATIVE.updatePlayerAbilities(p.getAbilities());p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(AEItems.COMPOST_MULCH.get(),3));
        h.getLevel().setBlock(s.above(),Blocks.STONE.defaultBlockState(),2);use(p,s.above());
        h.assertTrue(!SoilCareData.mulched(h.getLevel(),s),"Clicking a noncrop block above soil does not apply mulch");
        use(p,s);h.assertTrue(SoilCareData.mulched(h.getLevel(),s)&&p.getOffhandItem().getCount()==3,"Creative application is free");h.succeed();
    }
    private static void denied(GameTestHelper h,boolean cropPermission) {
        var s=soil(h);var p=new AccessPlayer(h.getLevel(),s);p.denied=cropPermission?s.above():s;
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(AEItems.COMPOST_MULCH.get(),2));use(p,s);
        h.assertTrue(!SoilCareData.mulched(h.getLevel(),s)&&p.getOffhandItem().getCount()==2,"Both crop and soil permissions are required");h.succeed();
    }
    @GameTest(template="empty") public static void mulchRespectsSoilPermission(GameTestHelper h){denied(h,false);}
    @GameTest(template="empty") public static void mulchRespectsCropPermission(GameTestHelper h){denied(h,true);}
    private static void interrupted(GameTestHelper h,int change) {
        var s=soil(h);var l=h.getLevel();crop(l,s,CropSpecies.IRON,1);var p=new AccessPlayer(l,s);
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(AEItems.COMPOST_MULCH.get(),2));var held=p.getOffhandItem();
        p.callback=()->{switch(change){case 0->p.setItemInHand(InteractionHand.OFF_HAND,held.copy());case 1->l.setBlock(s,Blocks.DIRT.defaultBlockState(),2);case 2->crop(l,s,CropSpecies.COAL,2);case 3->p.setHealth(0);case 4->held.grow(1);default->SoilCareData.applyMulch(l,s);}};
        use(p,s);h.assertTrue(held.getCount()==(change==4?3:2)&&SoilCareData.mulched(l,s)==(change==5),"Interrupted or nested mulch use never spends a stale item; change="+change);h.succeed();
    }
    @GameTest(template="empty") public static void replacedMulchStackCancelsUse(GameTestHelper h){interrupted(h,0);}
    @GameTest(template="empty") public static void replacedSoilCancelsMulch(GameTestHelper h){interrupted(h,1);}
    @GameTest(template="empty") public static void replacedCropCancelsMulch(GameTestHelper h){interrupted(h,2);}
    @GameTest(template="empty") public static void deadPlayerCannotFinishMulching(GameTestHelper h){interrupted(h,3);}
    @GameTest(template="empty") public static void changedMulchCountCancelsUse(GameTestHelper h){interrupted(h,4);}
    @GameTest(template="empty") public static void nestedApplicationDoesNotSpendTwice(GameTestHelper h){interrupted(h,5);}
    @GameTest(template="empty",timeoutTicks=30) public static void mulchExpiresAtSavedWorldTime(GameTestHelper h) {
        var s=soil(h);var l=h.getLevel();SoilCareData.applyMulch(l,s);
        var tag=SoilCareData.get(l).save(new CompoundTag(),l.registryAccess());var list=tag.getList("Plots",Tag.TAG_COMPOUND);
        for(int i=0;i<list.size();i++)if(list.getCompound(i).getLong("Soil")==s.asLong())list.getCompound(i).putLong("MulchedUntil",l.getGameTime()+2);
        var restored=SoilCareData.load(tag,l.registryAccess());
        h.runAfterDelay(3,()->{h.assertTrue(restored.remaining(l,s)==0,"Saved mulch expires rather than restarting its timer");h.succeed();});
    }
    @GameTest(template="empty") public static void mulchConservesAgriNutrition(GameTestHelper h) {
        var s=soil(h);var l=h.getLevel();var block=crop(l,s,CropSpecies.TOMATO,0);l.setBlock(s.above(2),Blocks.SEA_LANTERN.defaultBlockState(),2);
        var random=new LegacyRandomSource(3){@Override public int nextInt(int bound){return bound>50?1:0;}@Override public float nextFloat(){return .9f;}@Override public boolean nextBoolean(){return false;}};
        var plant=(AECropBlockEntity)l.getBlockEntity(s.above());int before=plant.soilNutrition();
        block.randomTick(l.getBlockState(s.above()),l,s.above(),random);int normalLoss=before-plant.soilNutrition();
        crop(l,s,CropSpecies.TOMATO,0);SoilCareData.applyMulch(l,s);before=plant.soilNutrition();
        block.randomTick(l.getBlockState(s.above()),l,s.above(),random);
        h.assertTrue(normalLoss>0&&plant.soilNutrition()==before,"A mulch conservation roll prevents nutrition loss without changing growth stages");h.succeed();
    }
    @GameTest(template="empty") public static void mulchDoublesUpgradedSoilDryingInterval(GameTestHelper h) {
        var s=soil(h);var l=h.getLevel();var block=(AdvancedFarmlandBlock)AEBlocks.CULTIVATED_FARMLAND.get();l.setBlock(s,block.defaultBlockState().setValue(FarmBlock.MOISTURE,7),2);
        int[] bound={0};var random=new LegacyRandomSource(1){@Override public int nextInt(int n){bound[0]=n;return 1;}};
        block.randomTick(l.getBlockState(s),l,s,random);int plain=bound[0];SoilCareData.applyMulch(l,s);block.randomTick(l.getBlockState(s),l,s,random);
        h.assertTrue(plain==2&&bound[0]==4&&l.getBlockState(s).getValue(FarmBlock.MOISTURE)==7,"Mulch halves the drying chance without adding water");h.succeed();
    }
    @GameTest(template="empty") public static void samplerAndLedgerExposeCareAndActualCatalysts(GameTestHelper h) {
        var s=soil(h);var l=h.getLevel();crop(l,s,CropSpecies.IRON,1);SoilCareData.recordHarvest(l,s,CropFamily.LEGUME);SoilCareData.applyMulch(l,s);
        l.setBlock(s.below(),Blocks.IRON_BLOCK.defaultBlockState(),2);var report=SoilSurveyData.survey(l,s,false);
        h.assertTrue(report.getInt("RotationBonus")==10&&report.getInt("MulchTicks")==24000&&report.getString("PreviousFamily").equals("Legumes")
            &&report.getString("CatalystHint").equals(ResourceCropRules.catalystHint(CropSpecies.IRON))&&report.getInt("CatalystX100")==400,"Saved guidance matches the real catalyst position and plot care");
        var f=new FarmLedgerData().create(UUID.randomUUID(),l.dimension().location().toString(),s,s);FarmLedgerService.sample(l,f,0);
        h.assertTrue((f.flags[0]&768)==768,"Ledger status reports both rotation and mulch");
        h.assertTrue(FarmLedgerData.readField(f.save()).flags[0]==f.flags[0],"New care flags survive field serialization");
        l.setBlock(s.below(),Blocks.DIAMOND_BLOCK.defaultBlockState(),2);h.assertTrue(SoilSurveyData.survey(l,s,false).getInt("CatalystX100")==100,"A different resource catalyst is not a match");h.succeed();
    }
    @GameTest(template="empty") public static void newCompanionsNeedMaturityAndSaveEveryDiscovery(GameTestHelper h) {
        var s=soil(h);var l=h.getLevel();var journal=new CompoundTag();
        for(var trial:CompanionPlanting.TRIALS.subList(6,12)) {
            l.setBlock(s.east(),Blocks.FARMLAND.defaultBlockState(),2);crop(l,s,trial.beneficiary(),0);crop(l,s.east(),trial.helper(),trial.readyAge()-1);
            h.assertTrue(CompanionPlanting.record(journal,l,s.above(),trial.beneficiary())==0,"Young helpers cannot be discovered");
            crop(l,s.east(),trial.helper(),trial.readyAge());h.assertTrue(CompanionPlanting.record(journal,l,s.above(),trial.beneficiary())==1,"Each established new trial records once");
            if(trial.weeds()>0)h.assertTrue(CompanionPlanting.weeds(l,s.above(),trial.beneficiary(),80)==160,"Established helper halves weeds");
            else {var plant=(AECropBlockEntity)l.getBlockEntity(s.above());int before=plant.soilNutrition();l.removeBlock(s.east().above(),false);CompanionPlanting.harvested(l,s.east().above(),trial.helper());h.assertTrue(plant.soilNutrition()==Math.min(100,before+8),"Mature legume harvest feeds its resource neighbour");}
        }
        h.assertTrue(CompanionPlanting.discovered(journal.copy()).size()==6&&CompanionPlanting.TRIALS.size()==12,"The six new trial IDs persist alongside the six original trials");h.succeed();
    }
    @GameTest(template="empty") public static void mulchRecipeCostsCompostAndTwoWeeds(GameTestHelper h) {
        var stacks=new ArrayList<ItemStack>();stacks.add(new ItemStack(AEItems.NUTRIENT_COMPOST.get()));stacks.add(new ItemStack(AEItems.PULLED_WEEDS.get()));stacks.add(new ItemStack(AEItems.PULLED_WEEDS.get()));while(stacks.size()<9)stacks.add(ItemStack.EMPTY);
        var recipe=h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,CraftingInput.of(3,3,stacks),h.getLevel()).orElseThrow();
        var output=recipe.value().assemble(CraftingInput.of(3,3,stacks),h.getLevel().registryAccess());
        h.assertTrue(output.is(AEItems.COMPOST_MULCH.get())&&output.getCount()==2,"The shapeless recipe makes two mulch items");
        stacks.set(2,ItemStack.EMPTY);h.assertTrue(h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,CraftingInput.of(3,3,stacks),h.getLevel()).isEmpty(),"One weed cannot pay the two-weed recipe");h.succeed();
    }
}
