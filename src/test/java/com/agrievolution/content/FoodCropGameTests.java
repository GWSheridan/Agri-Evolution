package com.agrievolution.content;

import com.agrievolution.genetics.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("agri_evolution") @PrefixGameTestTemplate(false)
public final class FoodCropGameTests {
    @GameTest(template="empty") public static void foodSeedsPlantAndHarvest(GameTestHelper h) {
        var level=h.getLevel();var player=h.makeMockPlayer(GameType.SURVIVAL);
        int index=0;
        for(var species:CropSpecies.values())if(species.isFood()) {
            var pos=h.absolutePos(new BlockPos(2+index%6,180,2+index++/6));
            // Keep this expanding crop fixture compact and above nearby irrigation tests.
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
            level.setBlock(pos.below(),Blocks.FARMLAND.defaultBlockState(),2);
            var seed=(SpeciesSeedItem)AEItems.seedFor(species);var stack=new ItemStack(seed,2);
            h.assertTrue(BuiltInRegistries.ITEM.getKey(seed).getPath().equals(species.seedId()),"Species seed ID matches registration: "+species);
            var genes=new CropGenome(48,37,62);GenomeStorage.putOnSeed(stack,genes);
            RareTraitStorage.putOnSeed(stack,RareTrait.VIGOROUS);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
            seed.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos.below()),Direction.UP,pos.below(),false)));
            h.assertTrue(level.getBlockState(pos).is(seed.cropBlock())&&stack.getCount()==1,"Correct planting and consumption: "+species);
            var crop=(AECropBlockEntity)level.getBlockEntity(pos);
            h.assertTrue(crop!=null&&crop.genome().equals(genes)&&crop.rareTrait()==RareTrait.VIGOROUS,"Genetics survive planting: "+species);
            h.assertFalse(species.isResource(),"Food must not become a resource crop");
            var youngDrops=Block.getDrops(level.getBlockState(pos),level,pos,crop);
            h.assertTrue(youngDrops.stream().anyMatch(s->s.is(seed)),"Immature plant returns a seed");
            var mature=seed.cropBlock().defaultBlockState().setValue(AgriCropBlock.AGE,7);
            level.setBlock(pos,mature,2);
            var food=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("agri_evolution",species.id()));
            var drops=Block.getDrops(mature,level,pos,crop);
            h.assertTrue(drops.stream().anyMatch(s->s.is(food)),"Mature harvest gives correct produce: "+species);
            h.assertTrue(new ItemStack(food).get(DataComponents.FOOD)!=null,"Produce is edible: "+species);
            h.assertFalse(CropWeeds.growing(level,pos,mature),"Ripe crops stop acquiring weeds");
        }
        h.assertTrue(index==60&&CropSpecies.NETHERITE.ordinal()==38&&CropSpecies.RICE.ordinal()==46&&CropSpecies.SWEET_POTATO.ordinal()==54,"Sixty food crops preserve old saved ordinals");h.succeed();
    }

    @GameTest(template="empty") public static void greenhouseHarvestsEveryFoodCrop(GameTestHelper h) throws Exception {
        var level=h.getLevel();var controller=h.absolutePos(new BlockPos(4,180,4));var facing=Direction.NORTH;
        level.setBlock(controller,AEBlocks.BOTANICAL_GREENHOUSE.get().defaultBlockState().setValue(BotanicalGreenhouseBlock.FACING,facing),2);
        var house=(BotanicalGreenhouseBlockEntity)level.getBlockEntity(controller);house.data.set(19,1);house.data.set(21,1);
        var pos=com.agrievolution.farming.BotanicalGreenhouseLayout.at(controller,facing,0,1,0);
        level.setBlock(pos.below(),Blocks.FARMLAND.defaultBlockState(),2);
        var method=BotanicalGreenhouseBlockEntity.class.getDeclaredMethod("harvestAndReplant",net.minecraft.server.level.ServerLevel.class);method.setAccessible(true);
        for(var species:CropSpecies.values())if(species.isFood()) {
            var block=((SpeciesSeedItem)AEItems.seedFor(species)).cropBlock();
            level.setBlock(pos,block.defaultBlockState().setValue(AgriCropBlock.AGE,7).setValue(AgriCropBlock.SUPPORTED,true).setValue(AgriCropBlock.SUPPORT_TIER,2),2);
            var crop=(AECropBlockEntity)level.getBlockEntity(pos);var genes=new CropGenome(32,42,52);crop.setGenome(genes);
            method.invoke(house,level);
            h.assertTrue(level.getBlockState(pos).is(block)&&level.getBlockState(pos).getValue(AgriCropBlock.AGE)==0,"Greenhouse replants correct food species: "+species);
            h.assertTrue(((AECropBlockEntity)level.getBlockEntity(pos)).genome().equals(genes)&&CropSupportItem.tier(level.getBlockState(pos))==2,"Automation retains genes and support");
            var produce=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("agri_evolution",species.id()));
            boolean stored=false;
            for(int slot=0;slot<house.getContainerSize();slot++)if(house.getItem(slot).is(produce))stored=true;
            h.assertTrue(stored,"Greenhouse stores correct food produce: "+species);
            // Collect each harvest so the next species tests replanting, not storage capacity.
            house.clearContent();
        }h.succeed();
    }
}

