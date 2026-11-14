package com.agrievolution.content;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("agri_evolution") @PrefixGameTestTemplate(false)
public final class SeedBagAutomationGameTests {
 @GameTest(template="empty") public static void depositSortPreservesComponentsAndOverflow(GameTestHelper h){
  var player=h.makeMockPlayer(GameType.SURVIVAL);var inv=player.getInventory();var bag=new ItemStack(AEItems.SEED_BAG.get());inv.setItem(0,bag);
  var a=new ItemStack(AEItems.WILD_SEED.get(),12);var b=a.copyWithCount(9);var genes=new net.minecraft.nbt.CompoundTag();genes.putInt("Growth",10);b.set(DataComponents.CUSTOM_DATA,CustomData.of(genes));
  inv.setItem(9,a.copy());inv.setItem(10,b.copy());inv.setItem(11,new ItemStack(Items.DIAMOND));
  var menu=new SeedBagMenu(1,inv,0);h.assertTrue(menu.clickMenuButton(player,0),"Deposit action accepted");menu.clickMenuButton(player,1);
  var stacks=bag.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).nonEmptyStream().toList();
  h.assertTrue(stacks.size()==2&&stacks.stream().anyMatch(s->s.getCount()==12&&ItemStack.isSameItemSameComponents(s,a))&&stacks.stream().anyMatch(s->s.getCount()==9&&ItemStack.isSameItemSameComponents(s,b)),"Different components must survive deposit and sorting unchanged");
  h.assertTrue(inv.getItem(11).is(Items.DIAMOND),"Nonseeds stay in inventory");
  for(int i=0;i<9;i++){var unique=new ItemStack(Items.WHEAT_SEEDS,64);unique.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("line "+i));menu.getSlot(i).set(unique);}
  inv.setItem(12,new ItemStack(Items.WHEAT_SEEDS,23));menu.clickMenuButton(player,0);h.assertTrue(inv.getItem(12).getCount()==23,"Full bag must leave overflow in inventory");
  inv.setItem(0,ItemStack.EMPTY);h.assertFalse(menu.clickMenuButton(player,0),"Stale menu cannot mutate moved bag");h.succeed();
 }
}
