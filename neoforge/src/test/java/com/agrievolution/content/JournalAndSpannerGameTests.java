package com.agrievolution.content;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@GameTestHolder("agri_evolution") @PrefixGameTestTemplate(false)
public final class JournalAndSpannerGameTests {
    @GameTest(template="empty") public static void bothBooksOnceAndAcrossClone(GameTestHelper h){
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        StarterProgressionEvents.giveBooks(player);StarterProgressionEvents.giveBooks(player);
        h.assertTrue(player.getInventory().countItem(AEItems.JOURNAL.get())==1,"Give one building journal");
        h.assertTrue(player.getInventory().countItem(AEItems.BREEDING_JOURNAL.get())==1,"Give one breeding journal");
        var respawn=h.makeMockPlayer(GameType.SURVIVAL);
        StarterProgressionEvents.onClone(player,respawn);StarterProgressionEvents.giveBooks(respawn);
        h.assertTrue(respawn.getInventory().countItem(AEItems.JOURNAL.get())==0&&respawn.getInventory().countItem(AEItems.BREEDING_JOURNAL.get())==0,"Death must not generate new starter books");h.succeed();
    }
    @GameTest(template="empty") public static void existingPlayerReceivesOnlyNewGuide(GameTestHelper h){
        var player=h.makeMockPlayer(GameType.SURVIVAL);player.getPersistentData().putBoolean("AgriEvolutionStarterJournalV1",true);
        var journal=new ItemStack(AEItems.BREEDING_JOURNAL.get());var data=new net.minecraft.nbt.CompoundTag();data.putString("PreserveDiscovery","existing");
        journal.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(data));player.getInventory().add(journal);
        StarterProgressionEvents.giveBooks(player);
        h.assertTrue(player.getInventory().countItem(AEItems.JOURNAL.get())==1&&player.getInventory().countItem(AEItems.BREEDING_JOURNAL.get())==1,"Migrate without duplicating breeding journal");
        var saved=player.getInventory().getItem(0).get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        h.assertTrue(saved!=null&&saved.copyTag().getString("PreserveDiscovery").equals("existing"),"Existing item data must remain unchanged");h.succeed();
    }
    @GameTest(template="empty") public static void spannerFormsButSamplerDoesNot(GameTestHelper h){
        var level=h.getLevel();var core=h.absolutePos(new BlockPos(3,100,3));ReservoirGameTests.blueprint(h,core);
        var player=h.makeMockPlayer(GameType.SURVIVAL);var pos=core.north();var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.NORTH,pos,false);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(AEItems.SOIL_SAMPLE.get()));
        player.getMainHandItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
        h.assertFalse(ReservoirStructure.complete(level,core),"Soil Sampler must no longer form reservoirs");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(AEItems.SPANNER.get()));
        player.getMainHandItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
        h.assertTrue(ReservoirStructure.complete(level,core),"Spanner must form valid reservoir");
        h.assertTrue(player.getMainHandItem().getCount()==1,"Assembly must preserve spanner");h.succeed();
    }
}
