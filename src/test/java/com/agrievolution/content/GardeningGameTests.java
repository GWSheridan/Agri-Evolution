package com.agrievolution.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class GardeningGameTests {
    private static UseOnContext context(Player player, BlockPos pos) {
        return new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }
    @GameTest(template = "empty")
    public static void cropDetection(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(1,1,1));
        for (var crop : new net.minecraft.world.level.block.Block[]{Blocks.WHEAT,Blocks.CARROTS,Blocks.POTATOES,Blocks.BEETROOTS,Blocks.NETHER_WART,Blocks.MELON_STEM,Blocks.COCOA,Blocks.SWEET_BERRY_BUSH}) {
            h.assertTrue(CropWeeds.growing(level,pos,crop.defaultBlockState()), "Missing crop: " + crop);
        }
        h.assertFalse(CropWeeds.growing(level,pos,Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7)), "Mature wheat must not acquire new weeds");
        h.assertFalse(CropWeeds.isHost(Blocks.OAK_SAPLING.defaultBlockState()), "Trees are not crops");
        h.assertFalse(CropWeeds.isHost(Blocks.DIRT.defaultBlockState()), "Dirt is not a crop");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void weedPersistenceAndRaking(GameTestHelper h) {
        var level=h.getLevel();
        var pos=h.absolutePos(new BlockPos(1,2,1));
        level.setBlock(pos.below(),Blocks.SOUL_SAND.defaultBlockState(),3);
        var state=Blocks.NETHER_WART.defaultBlockState();
        level.setBlock(pos,state,3);
        var weed=AEEntities.CROP_WEED.get().create(level);
        weed.attach(pos,state);
        CompoundTag saved=new CompoundTag();
        weed.saveWithoutId(saved);
        var loaded=AEEntities.CROP_WEED.get().create(level);
        loaded.load(saved);
        level.addFreshEntity(loaded);
        loaded.tick();
        h.assertTrue(CropWeeds.hasWeeds(level,pos),"Saved weeds must reload at the crop");
        Player player=h.makeMockPlayer(GameType.SURVIVAL);
        var rake=new ItemStack(AEItems.RAKE_WOOD.get());
        player.setItemInHand(InteractionHand.MAIN_HAND,rake);
        h.assertTrue(rake.getItem().onItemUseFirst(rake,context(player,pos)).consumesAction(),"Rake must clear weeds on non-farmland");
        h.assertFalse(CropWeeds.hasWeeds(level,pos),"Rake must remove weeds");
        h.assertTrue(level.getBlockState(pos).equals(state),"Rake must preserve crop age and block");
        h.assertTrue(rake.getDamageValue()==1,"One rake durability per use");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void ironRakeWeedsPatchAndStopsWhenBroken(GameTestHelper helper) {
        var level = helper.getLevel();
        var center = helper.absolutePos(new BlockPos(5, 25, 5));
        var corner = center.east().south();
        var outside = center.east(2);
        var raised = center.north().above();
        for (var plot : new BlockPos[]{center, corner, outside, raised}) {
            level.setBlock(plot.below(), Blocks.FARMLAND.defaultBlockState(), 2);
        }
        var weedyCrop = AEBlocks.AGRI_CROP.get().defaultBlockState().setValue(AgriCropBlock.WEEDY, true);
        for (var plot : new BlockPos[]{center, outside, raised}) level.setBlock(plot, weedyCrop, 2);
        var wheat = Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 3);
        level.setBlock(corner, wheat, 2);
        var weed = AEEntities.CROP_WEED.get().create(level);
        weed.attach(corner, wheat);
        level.addFreshEntity(weed);
        var crop = (AECropBlockEntity) level.getBlockEntity(center);
        int nutrition = crop.soilNutrition();

        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        var item = AEItems.RAKE_IRON.get();
        var rake = new ItemStack(item);
        player.setItemInHand(InteractionHand.MAIN_HAND, rake);
        helper.assertTrue(item.onItemUseFirst(rake, context(player, center.below())).consumesAction(), "A crouching soil click starts patch weeding");
        helper.assertTrue(!CropWeeds.hasWeeds(level, center) && !CropWeeds.hasWeeds(level, corner), "Both weed types in the patch are cleared");
        helper.assertTrue(CropWeeds.hasWeeds(level, outside) && CropWeeds.hasWeeds(level, raised), "Plots outside the square or on another level stay weedy");
        helper.assertTrue(level.getBlockState(corner).equals(wheat) && crop.soilNutrition() == nutrition
                && level.getBlockState(center.below()).getValue(net.minecraft.world.level.block.FarmBlock.MOISTURE) == 0,
                "Patch weeding preserves crop age, nutrition and soil moisture");
        helper.assertTrue(rake.getDamageValue() == 2, "Only cleared plots cost durability");
        player.getCooldowns().removeCooldown(item);
        item.onItemUseFirst(rake, context(player, center));
        helper.assertTrue(rake.getDamageValue() == 2, "A clean patch costs nothing");

        level.setBlock(center, weedyCrop, 2);
        level.setBlock(corner, weedyCrop, 2);
        rake.setDamageValue(rake.getMaxDamage() - 1);
        item.onItemUseFirst(rake, context(player, center));
        helper.assertTrue(rake.isEmpty() && !CropWeeds.hasWeeds(level, center) && CropWeeds.hasWeeds(level, corner),
                "The final durability point clears the center only");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void weedsCleanup(GameTestHelper h) {
        var level=h.getLevel();
        var pos=h.absolutePos(new BlockPos(1,2,1));
        level.setBlock(pos.below(),Blocks.FARMLAND.defaultBlockState(),3);
        var state=Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,3);
        level.setBlock(pos,state,3);
        var weed=AEEntities.CROP_WEED.get().create(level);
        weed.attach(pos,state);
        level.addFreshEntity(weed);
        level.setBlock(pos,state.setValue(CropBlock.AGE,0),3);
        weed.tick();
        h.assertTrue(weed.isRemoved(),"Harvest/regrowth resets weeds");
        var second=AEEntities.CROP_WEED.get().create(level);
        second.attach(pos,level.getBlockState(pos));
        level.addFreshEntity(second);
        level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
        second.tick();
        h.assertTrue(second.isRemoved(),"Broken crop cannot leave floating weeds");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void trowelPlantsOffhandSeed(GameTestHelper h) {
        var level=h.getLevel();
        var soil=h.absolutePos(new BlockPos(1,1,1));
        level.setBlock(soil,Blocks.FARMLAND.defaultBlockState(),3);
        Player player=h.makeMockPlayer(GameType.SURVIVAL);
        var tool=new ItemStack(AEItems.TROWEL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND,tool);
        player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.WHEAT_SEEDS,2));
        h.assertTrue(tool.getItem().onItemUseFirst(tool,context(player,soil)).consumesAction(),"Trowel must plant offhand seeds");
        h.assertTrue(level.getBlockState(soil.above()).is(Blocks.WHEAT),"Expected planted wheat");
        h.assertTrue(player.getOffhandItem().getCount()==1,"Plant exactly one seed");
        h.assertTrue(tool.getDamageValue()==1,"One trowel durability per planting");
        h.assertTrue(GardenToolItem.recentlyUsed(tool,level),"Use planted trowel appearance");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void trowelTillsSoil(GameTestHelper h) {
        var level=h.getLevel();
        var soil=h.absolutePos(new BlockPos(1,1,1));
        level.setBlock(soil,Blocks.DIRT.defaultBlockState(),3);
        Player player=h.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        var tool=new ItemStack(AEItems.TROWEL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND,tool);
        h.assertTrue(tool.useOn(context(player,soil)).consumesAction(),"Trowel must till dirt");
        h.assertTrue(level.getBlockState(soil).is(Blocks.FARMLAND),"Expected farmland");
        h.assertTrue(tool.getDamageValue()==1,"One durability per tilling");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void cancelledPlantingRefundsSeed(GameTestHelper h) {
        var level=h.getLevel();
        var soil=h.absolutePos(new BlockPos(1,1,1));
        level.setBlock(soil,Blocks.FARMLAND.defaultBlockState(),3);
        Player player=h.makeMockPlayer(GameType.SURVIVAL);
        var tool=new ItemStack(AEItems.TROWEL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND,tool);
        player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.WHEAT_SEEDS,2));
        java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent> cancel = event -> {
            if (event.getPos().equals(soil.above())) event.setCanceled(true);
        };
        var bus=net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener(cancel);
        try {
            tool.getItem().onItemUseFirst(tool,context(player,soil));
            h.assertTrue(level.isEmptyBlock(soil.above()),"Cancelled planting must restore air");
            h.assertTrue(player.getOffhandItem().getCount()==2,"Cancelled planting must refund the seed");
            h.assertTrue(tool.getDamageValue()==0,"Cancelled planting must not damage trowel");
        } finally { bus.unregister(cancel); }
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void naturalWeedGrowth(GameTestHelper h) {
        var level=h.getLevel();
        var pos=h.absolutePos(new BlockPos(1,2,1));
        level.setBlock(pos.below(),Blocks.FARMLAND.defaultBlockState(),3);
        var state=Blocks.WHEAT.defaultBlockState();
        level.setBlock(pos,state,3);
        level.random.setSeed(42035L);
        for(int i=0;i<400 && !CropWeeds.hasWeeds(level,pos);i++)
            CropWeeds.growth(new net.neoforged.neoforge.event.level.block.CropGrowEvent.Pre(level,pos,state));
        h.assertTrue(CropWeeds.hasWeeds(level,pos),"Crop growth events should generate weeds");
        h.assertTrue(level.getBlockState(pos).equals(state),"Weed generation must not replace crop");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void geneticSeedPlantingAndSingleBonus(GameTestHelper h) {
        var level=h.getLevel();
        var soil=h.absolutePos(new BlockPos(1,1,1));
        level.setBlock(soil,Blocks.FARMLAND.defaultBlockState(),3);
        Player player=h.makeMockPlayer(GameType.SURVIVAL);
        var tool=new ItemStack(AEItems.TROWEL.get());
        var seeds=new ItemStack(AEItems.WILD_SEED.get(),2);
        var genes=new com.agrievolution.genetics.CropGenome(72,63,54);
        com.agrievolution.genetics.GenomeStorage.putOnSeed(seeds,genes);
        player.setItemInHand(InteractionHand.MAIN_HAND,tool);
        player.setItemInHand(InteractionHand.OFF_HAND,seeds);
        tool.getItem().onItemUseFirst(tool,context(player,soil));
        h.assertTrue(level.getBlockEntity(soil.above()) instanceof AECropBlockEntity,"Expected Agri crop data");
        var plant=(AECropBlockEntity)level.getBlockEntity(soil.above());
        h.assertTrue(plant.genome().growth()==72 && plant.genome().yield()==63 && plant.genome().resilience()==54,"Planting must preserve seed genetics");
        h.assertTrue(plant.hasBeenTended() && plant.soilNutrition()==50,"One nutrition bonus on planting");
        h.assertTrue(level.getBlockState(soil.above()).getValue(AgriCropBlock.AGE)==1,"One initial growth stage");
        player.getCooldowns().removeCooldown(AEItems.TROWEL.get());
        tool.useOn(context(player,soil.above()));
        h.assertTrue(level.getBlockState(soil.above()).getValue(AgriCropBlock.AGE)==1 && plant.soilNutrition()==50,"Cannot repeat tending on the same planting");
        h.assertTrue(tool.getDamageValue()==1 && seeds.getCount()==1,"Repeated tending must not consume seeds or durability");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void trowelPatchPreservesGenesAndStopsWhenSeedsRunOut(GameTestHelper helper) {
        var level = helper.getLevel();
        var center = helper.absolutePos(new BlockPos(5, 35, 5));
        var open = java.util.List.of(center, center.east(), center.south().east());
        var occupied = center.north();
        var outside = center.west(2);
        for (var soil : java.util.List.of(center, center.east(), center.south().east(), occupied, outside)) {
            level.setBlock(soil, Blocks.FARMLAND.defaultBlockState(), 2);
        }
        var wheat = Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 3);
        level.setBlock(occupied.above(), wheat, 2);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        var item = AEItems.TROWEL.get();
        var tool = new ItemStack(item);
        var seeds = new ItemStack(AEItems.WILD_SEED.get(), 2);
        var genes = new com.agrievolution.genetics.CropGenome(81, 72, 63);
        com.agrievolution.genetics.GenomeStorage.putOnSeed(seeds, genes);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        player.setItemInHand(InteractionHand.OFF_HAND, seeds);

        helper.assertTrue(item.onItemUseFirst(tool, context(player, center)).consumesAction(), "Crouch-use starts patch planting");
        int planted = 0;
        for (var soil : open) {
            if (!(level.getBlockEntity(soil.above()) instanceof AECropBlockEntity crop)) continue;
            planted++;
            helper.assertTrue(crop.genome().equals(genes) && crop.hasBeenTended() && crop.soilNutrition() == 50
                    && level.getBlockState(soil.above()).getValue(AgriCropBlock.AGE) == 1, "Each crop keeps its genes and gets the normal bonus once");
        }
        helper.assertTrue(planted == 2 && !level.isEmptyBlock(center.above()) && seeds.isEmpty() && tool.getDamageValue() == 2,
                "The center is planted first, with one seed and one durability spent per crop");
        helper.assertTrue(level.getBlockState(occupied.above()).equals(wheat) && level.isEmptyBlock(outside.above()),
                "Occupied and out-of-range plots are left alone");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void trowelPatchRespectsCancelledPlantingAndToolBreakage(GameTestHelper helper) {
        var level = helper.getLevel();
        var center = helper.absolutePos(new BlockPos(5, 40, 5));
        var side = center.east();
        var corner = center.east().south();
        for (var soil : java.util.List.of(center, side, corner)) level.setBlock(soil, Blocks.FARMLAND.defaultBlockState(), 2);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        var item = AEItems.TROWEL.get();
        var tool = new ItemStack(item);
        tool.setDamageValue(tool.getMaxDamage() - 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.WHEAT_SEEDS, 2));
        java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent> cancel = event -> {
            if (event.getPos().equals(center.above())) event.setCanceled(true);
        };
        var bus = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener(cancel);
        try {
            item.onItemUseFirst(tool, context(player, center));
            helper.assertTrue(level.isEmptyBlock(center.above()) && player.getOffhandItem().getCount() == 1,
                    "The canceled plot refunds its seed");
            helper.assertTrue(level.getBlockState(side.above()).is(Blocks.WHEAT) && level.isEmptyBlock(corner.above()) && tool.isEmpty(),
                    "Planting continues to the first permitted plot and stops when the trowel breaks");
        } finally {
            bus.unregister(cancel);
        }
        helper.succeed();
    }
}
