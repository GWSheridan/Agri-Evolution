package com.agrievolution.content;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import com.agrievolution.genetics.*;

@GameTestHolder("agri_evolution") @PrefixGameTestTemplate(false)
public final class SupportTierGameTests {
    @GameTest(template="empty") public static void illustratedRecipesMatchLoadedCrafting(GameTestHelper h) {
        for(var spec:com.agrievolution.client.journal.MachineRecipeCatalog.ALL) {
            var holder=h.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("agri_evolution",spec.id())).orElseThrow();
            var recipe=holder.value();var ingredients=recipe.getIngredients();var result=recipe.getResultItem(h.getLevel().registryAccess());
            h.assertTrue(ingredients.size()==spec.ingredients().length,"Illustrated ingredient count matches "+spec.id());
            h.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(result.getItem()).toString().equals(spec.result())&&result.getCount()==spec.count(),"Illustrated result matches "+spec.id());
            for(int i=0;i<spec.ingredients().length;i++) {
                var choices=spec.ingredients()[i];
                if(choices.length==0)h.assertTrue(ingredients.get(i).isEmpty(),"Blank crafting cells match");
                for(String id:choices) {
                    if(id.startsWith("#")) {
                        var tag=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,net.minecraft.resources.ResourceLocation.parse(id.substring(1)));
                        var variants=net.minecraft.world.item.crafting.Ingredient.of(tag).getItems();
                        h.assertTrue(variants.length>0,"Illustrated tag resolves: "+id);
                        for(var variant:variants)h.assertTrue(ingredients.get(i).test(variant),"Every illustrated tag alternative is accepted: "+spec.id());
                    } else h.assertTrue(ingredients.get(i).test(new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(id)))),"Every illustrated alternative is accepted: "+spec.id());
                }
            }
            h.assertTrue(spec.shaped()==(recipe instanceof net.minecraft.world.item.crafting.ShapedRecipe),"Crafting order matches the illustration: "+spec.id());
        }h.succeed();
    }
    @GameTest(template="empty") public static void upgradePreservesCropAndReturnsOldSupport(GameTestHelper h) {
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(3,20,3));
        level.setBlock(pos.below(),Blocks.FARMLAND.defaultBlockState(),2);
        level.setBlock(pos,AEBlocks.AGRI_CROP.get().defaultBlockState().setValue(AgriCropBlock.SUPPORTED,true),2);
        var plant=(AECropBlockEntity)level.getBlockEntity(pos);var genes=new CropGenome(40,30,20);plant.setGenome(genes);plant.setClipped(true);plant.setCrossed(true);
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(AEItems.IRON_CROP_SUPPORT.get(),2));
        var context=new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
        AEItems.IRON_CROP_SUPPORT.get().useOn(context);
        h.assertTrue(CropSupportItem.tier(level.getBlockState(pos))==2,"Iron tier installed");
        h.assertTrue(plant==level.getBlockEntity(pos)&&plant.genome().equals(genes)&&plant.hasBeenClipped()&&plant.hasCrossed(),"Upgrade must preserve full crop data");
        h.assertTrue(player.getInventory().countItem(AEItems.CROP_SUPPORT.get())==1&&player.getMainHandItem().getCount()==1,"Upgrade exchanges one support");
        AEItems.IRON_CROP_SUPPORT.get().useOn(context);
        h.assertTrue(player.getMainHandItem().getCount()==1,"Equal tier must not consume support");h.succeed();
    }
    @GameTest(template="empty") public static void exactTierRecoveredFromLoot(GameTestHelper h) {
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(3,20,3));
        level.setBlock(pos.below(),Blocks.FARMLAND.defaultBlockState(),2);
        for(int tier=0;tier<=3;tier++) {
            var state=AEBlocks.AGRI_CROP.get().defaultBlockState().setValue(AgriCropBlock.SUPPORTED,true).setValue(AgriCropBlock.SUPPORT_TIER,tier);
            level.setBlock(pos,state,2);
            var drops=Block.getDrops(state,level,pos,level.getBlockEntity(pos));
            final int expected=Math.max(1,tier);
            h.assertTrue(drops.stream().filter(i->i.is(CropSupportItem.itemFor(expected))).mapToInt(ItemStack::getCount).sum()==1,"Recover exactly one correct support, including legacy state "+tier);
        }h.succeed();
    }
    @GameTest(template="empty") public static void greenhouseKeepsSupportAcrossReplant(GameTestHelper h) throws Exception {
        var level=h.getLevel();
        for(int smart=0;smart<=1;smart++) {
            var controller=h.absolutePos(new BlockPos(4+smart*10,20,4));var facing=Direction.NORTH;
            level.setBlock(controller,AEBlocks.BOTANICAL_GREENHOUSE.get().defaultBlockState().setValue(BotanicalGreenhouseBlock.FACING,facing),2);
            var house=(BotanicalGreenhouseBlockEntity)level.getBlockEntity(controller);house.data.set(19,1);house.data.set(21,1);house.data.set(26,smart);
            var crop=com.agrievolution.farming.BotanicalGreenhouseLayout.at(controller,facing,0,1,0);
            level.setBlock(crop.below(),Blocks.FARMLAND.defaultBlockState(),2);
            level.setBlock(crop,AEBlocks.AGRI_CROP.get().defaultBlockState().setValue(AgriCropBlock.AGE,7).setValue(AgriCropBlock.SUPPORTED,true).setValue(AgriCropBlock.SUPPORT_TIER,3),2);
            var plant=(AECropBlockEntity)level.getBlockEntity(crop);var genes=new CropGenome(40,30,20);plant.setGenome(genes);plant.setRareTrait(RareTrait.VIGOROUS);
            var method=BotanicalGreenhouseBlockEntity.class.getDeclaredMethod("harvestAndReplant",net.minecraft.server.level.ServerLevel.class);method.setAccessible(true);method.invoke(house,level);
            h.assertTrue(CropSupportItem.tier(level.getBlockState(crop))==3,"Greenhouse replant retains obsidian support, smart="+smart);
            var replanted=(AECropBlockEntity)level.getBlockEntity(crop);
            h.assertTrue(replanted.genome().equals(genes)&&replanted.rareTrait()==RareTrait.VIGOROUS,"Replant retains genetics");
        }h.succeed();
    }
    @GameTest(template="empty") public static void greenhouseKeepsIronWearAcrossReplant(GameTestHelper h) throws Exception {
        var level=h.getLevel();
        for(int smart=0;smart<=1;smart++) {
            var controller=h.absolutePos(new BlockPos(4+smart*10,20,4));var facing=Direction.NORTH;
            level.setBlock(controller,AEBlocks.BOTANICAL_GREENHOUSE.get().defaultBlockState().setValue(BotanicalGreenhouseBlock.FACING,facing),2);
            var house=(BotanicalGreenhouseBlockEntity)level.getBlockEntity(controller);house.data.set(19,1);house.data.set(21,1);house.data.set(26,smart);
            var crop=com.agrievolution.farming.BotanicalGreenhouseLayout.at(controller,facing,0,1,0);
            level.setBlock(crop.below(),Blocks.FARMLAND.defaultBlockState(),2);
            level.setBlock(crop,AEBlocks.AGRI_CROP.get().defaultBlockState().setValue(AgriCropBlock.AGE,7).setValue(AgriCropBlock.SUPPORTED,true).setValue(AgriCropBlock.SUPPORT_TIER,2),2);
            var plant=(AECropBlockEntity)level.getBlockEntity(crop);var genes=new CropGenome(40,30,20);plant.setSupportTicks(1234);plant.setGenome(genes);plant.setRareTrait(RareTrait.VIGOROUS);
            var method=BotanicalGreenhouseBlockEntity.class.getDeclaredMethod("harvestAndReplant",net.minecraft.server.level.ServerLevel.class);method.setAccessible(true);method.invoke(house,level);
            h.assertTrue(CropSupportItem.tier(level.getBlockState(crop))==2,"Greenhouse replant retains iron support, smart="+smart);
            var replanted=(AECropBlockEntity)level.getBlockEntity(crop);h.assertTrue(replanted.supportTicks()==1234,"Replant must not reset wear");
            h.assertTrue(replanted.genome().equals(genes)&&replanted.rareTrait()==RareTrait.VIGOROUS,"Replant retains genetics");
        }h.succeed();
    }
    @GameTest(template="empty") public static void tierBonusesAndMachineSlots(GameTestHelper h) {
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var station=new SeedBreedingStationMenu(1,player.getInventory());var lab=new BotanicalGeneticsLaboratoryMenu(2,player.getInventory());
        for(int tier=1;tier<=3;tier++) {
            var stack=new ItemStack(CropSupportItem.itemFor(tier),2);
            h.assertTrue(station.getSlot(3).mayPlace(stack)&&lab.getSlot(3).mayPlace(stack),"Machines accept all support tiers");
            station.getSlot(3).set(stack);h.assertTrue(station.bonus()==CropSupportItem.fieldBonus(tier),"Station preview uses tier bonus");
            int chance=AdvancedHybridBreeder.recipe(CropSpecies.GOLD,CropSpecies.LAPIS).chance(CropSupportItem.labBonus(tier),false);
            h.assertTrue(chance==10+CropSupportItem.labBonus(tier),"Hybrid preview matches tier odds");
        }h.succeed();
    }
    @GameTest(template="empty") public static void diagnosticWarningsAreReadOnly(GameTestHelper h) {
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var menu=new BotanicalGreenhouseMenu(1,player.getInventory());
        var initial=GreenhouseStatus.indicators(menu);h.assertTrue(initial.get(0).severity()==2&&initial.get(1).state().equals("EMPTY"),"Missing shell and water visible");
        menu.setData(2,1);menu.setData(0,500);menu.setData(19,1);menu.setData(25,1);menu.setData(26,1);menu.setData(27,1);
        var warnings=GreenhouseStatus.indicators(menu);h.assertTrue(warnings.get(1).state().equals("LOW")&&warnings.get(2).state().equals("FULL")&&warnings.get(3).state().equals("EMPTY"),"Low water, full storage and seed shortage visible");
        h.assertTrue(menu.water()==500&&menu.smartPlantEnabled()&&menu.storageFull(),"Diagnostics never change settings or inventory");h.succeed();
    }
}
