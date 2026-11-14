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
public final class ResourceExpansionThirteenGameTests {
    @GameTest(template="empty") public static void resourceCatalystsMatchTheirMaterials(GameTestHelper h) {
        var species=new CropSpecies[]{CropSpecies.JUNGLE_LOG,CropSpecies.COARSE_DIRT,CropSpecies.ROOTED_DIRT,CropSpecies.RED_SANDSTONE,CropSpecies.PRISMARINE_BRICKS,CropSpecies.WARPED_WART_BLOCK,CropSpecies.RED_NETHER_BRICKS,CropSpecies.SCULK_SENSOR,CropSpecies.SCULK_SHRIEKER,CropSpecies.CONDUIT,CropSpecies.RESPAWN_ANCHOR};
        var blocks=new Block[]{Blocks.JUNGLE_LOG,Blocks.GRAVEL,Blocks.ROOTED_DIRT,Blocks.RED_SANDSTONE,Blocks.PRISMARINE,Blocks.WARPED_NYLIUM,Blocks.NETHER_BRICKS,Blocks.SCULK,Blocks.SCULK_CATALYST,Blocks.PRISMARINE_BRICKS,Blocks.CRYING_OBSIDIAN};
        float[] speeds={2,2,2,2,3,3,3,4,4,4,4};var pos=h.absolutePos(new BlockPos(3,20,3));
        for(int i=0;i<species.length;i++) {
            h.getLevel().setBlock(pos.below(2),Blocks.AIR.defaultBlockState(),2);
            h.assertTrue(com.agrievolution.farming.ResourceCropRules.growthMultiplier(species[i],h.getLevel(),pos)==1,"Optional catalyst: normal growth without it");
            h.getLevel().setBlock(pos.below(2),blocks[i].defaultBlockState(),2);
            h.assertTrue(com.agrievolution.farming.ResourceCropRules.growthMultiplier(species[i],h.getLevel(),pos)==speeds[i],"Matching catalyst speed: "+species[i]);
        }h.succeed();
    }
    @GameTest(template="empty") public static void resourceSeedsPlantAndHarvest(GameTestHelper h) {
        var level=h.getLevel();var player=h.makeMockPlayer(GameType.SURVIVAL);
        int index=0;
        for(var species:CropSpecies.values())if(species.ordinal()>=CropSpecies.JUNGLE_LOG.ordinal()&&species.ordinal()<=CropSpecies.RESPAWN_ANCHOR.ordinal()) {
            var pos=h.absolutePos(new BlockPos(2+index%6,180,2+index++/6));
            // Planting cells are above the template; clear any neighbouring fixture blocks.
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
            level.setBlock(pos.below(),Blocks.FARMLAND.defaultBlockState(),2);
            var seed=(SpeciesSeedItem)AEItems.seedFor(species);var stack=new ItemStack(seed,2);
            var genes=new CropGenome(48,37,62);GenomeStorage.putOnSeed(stack,genes);
            RareTraitStorage.putOnSeed(stack,RareTrait.VIGOROUS);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
            seed.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos.below()),Direction.UP,pos.below(),false)));
            h.assertTrue(level.getBlockState(pos).is(seed.cropBlock())&&stack.getCount()==1,"Correct planting and consumption: "+species);
            var crop=(AECropBlockEntity)level.getBlockEntity(pos);
            h.assertTrue(crop!=null&&crop.genome().equals(genes)&&crop.rareTrait()==RareTrait.VIGOROUS,"Genetics survive planting: "+species);
            h.assertTrue(species.isResource()&&!species.isFood(),"Resource classification");
            var youngDrops=Block.getDrops(level.getBlockState(pos),level,pos,crop);
            h.assertTrue(youngDrops.stream().anyMatch(s->s.is(seed)),"Immature plant returns a seed");
            var mature=seed.cropBlock().defaultBlockState().setValue(AgriCropBlock.AGE,7);
            level.setBlock(pos,mature,2);
            var food=BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(species.id()));
            var drops=Block.getDrops(mature,level,pos,crop);
            h.assertTrue(drops.stream().anyMatch(s->s.is(food)),"Mature harvest gives correct produce: "+species);
            if(species==CropSpecies.SCULK_SENSOR||species==CropSpecies.SCULK_SHRIEKER||species==CropSpecies.CONDUIT||species==CropSpecies.RESPAWN_ANCHOR)h.assertTrue(drops.stream().filter(d->d.is(food)).mapToInt(ItemStack::getCount).sum()==1,"Late resource base yield is one");
            h.assertFalse(CropWeeds.growing(level,pos,mature),"Ripe crops stop acquiring weeds");
        }
        h.assertTrue(index==11&&CropSpecies.NETHERITE.ordinal()==38&&CropSpecies.RICE.ordinal()==46&&CropSpecies.SWEET_POTATO.ordinal()==54,"Twelve resource crops preserve old saved ordinals");h.succeed();
    }

    @GameTest(template="empty") public static void greenhouseHarvestsNewResources(GameTestHelper h) throws Exception {
        var level=h.getLevel();var controller=h.absolutePos(new BlockPos(4,180,4));var facing=Direction.NORTH;
        level.setBlock(controller,AEBlocks.BOTANICAL_GREENHOUSE.get().defaultBlockState().setValue(BotanicalGreenhouseBlock.FACING,facing),2);
        var house=(BotanicalGreenhouseBlockEntity)level.getBlockEntity(controller);house.data.set(19,1);house.data.set(21,1);
        var pos=com.agrievolution.farming.BotanicalGreenhouseLayout.at(controller,facing,0,1,0);
        level.setBlock(pos.below(),Blocks.FARMLAND.defaultBlockState(),2);
        var method=BotanicalGreenhouseBlockEntity.class.getDeclaredMethod("harvestAndReplant",net.minecraft.server.level.ServerLevel.class);method.setAccessible(true);
        for(var species:CropSpecies.values())if(species.ordinal()>=CropSpecies.JUNGLE_LOG.ordinal()&&species.ordinal()<=CropSpecies.RESPAWN_ANCHOR.ordinal()) {
            var block=((SpeciesSeedItem)AEItems.seedFor(species)).cropBlock();
            level.setBlock(pos,block.defaultBlockState().setValue(AgriCropBlock.AGE,7).setValue(AgriCropBlock.SUPPORTED,true).setValue(AgriCropBlock.SUPPORT_TIER,2),2);
            var crop=(AECropBlockEntity)level.getBlockEntity(pos);var genes=new CropGenome(32,42,52);crop.setGenome(genes);
            method.invoke(house,level);
            var storageField=BotanicalGreenhouseBlockEntity.class.getDeclaredField("storage");storageField.setAccessible(true);
            var storage=(NonNullList<ItemStack>)storageField.get(house);
            h.assertTrue(storage.stream().anyMatch(d->d.is(com.agrievolution.farming.ResourceCropRules.produce(species))),"Greenhouse stores the material: "+species);
            h.assertTrue(level.getBlockState(pos).is(block)&&level.getBlockState(pos).getValue(AgriCropBlock.AGE)==0,"Greenhouse replants correct food species: "+species);
            h.assertTrue(((AECropBlockEntity)level.getBlockEntity(pos)).genome().equals(genes)&&CropSupportItem.tier(level.getBlockState(pos))==2,"Automation retains genes and support");
        }h.succeed();
    }
}
