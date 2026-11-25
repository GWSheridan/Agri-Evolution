package com.agrievolution.content;

import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("agri_evolution") @PrefixGameTestTemplate(false)
public final class LunchBoxGameTests {
    private static ItemStack box(ItemStack food){
        var box=new ItemStack(AEItems.LUNCH_BOX.get());
        box.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(food)));
        return box;
    }
    @GameTest(template="empty") public static void foodsAndRemainders(GameTestHelper h){
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var foods=new ArrayList<ItemStack>(List.of(new ItemStack(Items.BREAD),new ItemStack(Items.MUSHROOM_STEW),new ItemStack(Items.HONEY_BOTTLE),new ItemStack(Items.GOLDEN_APPLE)));
        for(var dish:CookingFoods.ALL)foods.add(new ItemStack(dish.item().get()));
        for(var food:foods){
            player.getInventory().clearContent();player.getFoodData().setFoodLevel(0);player.getFoodData().setSaturation(0);
            var box=box(food.copyWithCount(2));player.getInventory().setItem(0,box);
            h.assertTrue(LunchBoxItem.accepts(food),"Food accepted");
            h.assertTrue(LunchBoxItem.feedOne(player),"Serving consumed");
            var remaining=box.get(DataComponents.CONTAINER).nonEmptyStream().toList();
            h.assertTrue(remaining.size()==1&&remaining.getFirst().getCount()==1,"Exactly one serving remains");
            h.assertTrue(player.getFoodData().getFoodLevel()==food.getFoodProperties(player).nutrition(),"Normal hunger gain");
            boolean bowl=food.is(Items.MUSHROOM_STEW)||CookingFoods.ALL.stream().anyMatch(d->food.is(d.item().get())&&d.bowl());
            int bowls=0,bottles=0;
            for(var stack:player.getInventory().items){if(stack.is(Items.BOWL))bowls+=stack.getCount();if(stack.is(Items.GLASS_BOTTLE))bottles+=stack.getCount();}
            h.assertTrue(bowls==(bowl?1:0)&&bottles==(food.is(Items.HONEY_BOTTLE)?1:0),"Remainders returned exactly once");
        }
        h.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.ABSORPTION),"Golden apple effect preserved");
        h.assertFalse(LunchBoxItem.accepts(new ItemStack(Items.DIRT)),"Nonfood rejected");
        h.assertFalse(LunchBoxItem.accepts(box(new ItemStack(Items.BREAD))),"Nested box rejected");h.succeed();
    }
    @GameTest(template="empty") public static void feedingGuardsAndOffhand(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var box=box(new ItemStack(Items.BREAD,3));p.getInventory().setItem(40,box);
        p.getFoodData().setFoodLevel(20);h.assertFalse(LunchBoxItem.feedOne(p),"Full hunger protected");
        p.getFoodData().setFoodLevel(0);LunchBoxItem.toggle(box);h.assertFalse(LunchBoxItem.feedOne(p),"Pause respected");
        LunchBoxItem.toggle(box);p.containerMenu=new LunchBoxMenu(1,p.getInventory(),40);
        h.assertFalse(LunchBoxItem.feedOne(p),"Open menu protected");p.containerMenu=p.inventoryMenu;
        p.getInventory().setItem(1,box(new ItemStack(Items.BREAD,4)));
        h.assertTrue(LunchBoxItem.feedOne(p),"Carried food eaten");
        h.assertTrue(p.getFoodData().getFoodLevel()==5,"Only one serving with multiple boxes");
        p.getInventory().setItem(1,ItemStack.EMPTY);h.assertTrue(LunchBoxItem.feedOne(p),"Offhand works");
        h.assertTrue(box.get(DataComponents.CONTAINER).nonEmptyStream().findFirst().orElseThrow().getCount()==2,"Offhand saved");
        var creative=h.makeMockPlayer(GameType.CREATIVE);creative.getFoodData().setFoodLevel(0);creative.getInventory().setItem(0,box(new ItemStack(Items.BREAD)));
        h.assertFalse(LunchBoxItem.feedOne(creative),"Creative protected");h.succeed();
    }
    @GameTest(template="empty") public static void menuPersistenceAndRecipe(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var box=box(ItemStack.EMPTY);p.getInventory().setItem(0,box);
        var menu=new LunchBoxMenu(1,p.getInventory(),0);
        h.assertFalse(menu.slots.get(36).mayPickup(p),"Open box locked");
        h.assertTrue(menu.quickMoveStack(p,36).isEmpty(),"Shift-click cannot move open box");
        h.assertFalse(menu.slots.get(0).mayPlace(new ItemStack(Items.DIRT)),"Nonfood slot rejected");
        menu.slots.get(0).set(new ItemStack(Items.BREAD,7));menu.removed(p);
        var reopened=new LunchBoxMenu(2,p.getInventory(),0);
        h.assertTrue(reopened.slots.get(0).getItem().getCount()==7,"Food survives closing/reopening");
        LunchBoxItem.toggle(box);
        var restored=ItemStack.parse(h.getLevel().registryAccess(),box.save(h.getLevel().registryAccess())).orElseThrow();
        h.assertFalse(LunchBoxItem.enabled(restored),"Pause survives serialization");
        h.assertTrue(restored.get(DataComponents.CONTAINER).nonEmptyStream().findFirst().orElseThrow().getCount()==7,"Food survives serialization");
        for(var plank:List.of(Items.OAK_PLANKS,Items.BIRCH_PLANKS,Items.BAMBOO_PLANKS,Items.CHERRY_PLANKS)){
            var input=CraftingInput.of(3,3,List.of(ItemStack.EMPTY,new ItemStack(Items.COPPER_INGOT),ItemStack.EMPTY,new ItemStack(plank),new ItemStack(Items.BOWL),new ItemStack(plank),new ItemStack(plank),new ItemStack(plank),new ItemStack(plank)));
            var matches=h.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream().filter(r->r.value().matches(input,h.getLevel())).toList();
            h.assertTrue(matches.size()==1&&matches.getFirst().value().assemble(input,h.getLevel().registryAccess()).is(AEItems.LUNCH_BOX.get()),"No vanilla/mod recipe conflict");
        }h.succeed();
    }
    @GameTest(template="empty") public static void prioritiesProtectionAndSaving(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);p.getFoodData().setFoodLevel(0);
        var box=box(ItemStack.EMPTY);p.getInventory().setItem(0,box);
        box.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(new ItemStack(Items.BREAD,3),new ItemStack(Items.APPLE,3),new ItemStack(Items.CARROT,3))));
        LunchBoxItem.cyclePriority(box,1);LunchBoxItem.cyclePriority(box,2);
        var foods=net.minecraft.core.NonNullList.withSize(9,ItemStack.EMPTY);box.get(DataComponents.CONTAINER).copyInto(foods);
        h.assertTrue(LunchBoxItem.selectFood(box,foods,p)==1,"High priority wins and ties choose leftmost");
        LunchBoxItem.toggleProtected(box,1);h.assertTrue(LunchBoxItem.selectFood(box,foods,p)==2,"Protected high priority skipped");
        h.assertTrue(LunchBoxItem.feedOne(p),"Unprotected food eaten");
        box.get(DataComponents.CONTAINER).copyInto(foods);
        h.assertTrue(foods.get(1).getCount()==3&&foods.get(2).getCount()==2,"Only selected serving consumed");
        LunchBoxItem.toggleProtected(box,0);LunchBoxItem.toggleProtected(box,2);
        h.assertTrue(LunchBoxItem.feedingStatus(box,p)==4&&!LunchBoxItem.feedOne(p),"All food protected status and guard agree");
        var restored=ItemStack.parse(h.getLevel().registryAccess(),box.save(h.getLevel().registryAccess())).orElseThrow();
        h.assertTrue(LunchBoxItem.priority(restored,1)==2&&LunchBoxItem.protectedSlot(restored,1),"Controls survive save round trip");
        h.assertTrue(LunchBoxItem.priority(box(ItemStack.EMPTY),5)==1,"Old boxes default to normal");h.succeed();
    }
    @GameTest(template="empty") public static void controlsSortAndStatus(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);p.getFoodData().setFoodLevel(0);
        var box=box(ItemStack.EMPTY);p.getInventory().setItem(0,box);var menu=new LunchBoxMenu(1,p.getInventory(),0);
        menu.slots.get(0).set(new ItemStack(Items.CARROT,3));menu.slots.get(1).set(new ItemStack(Items.BREAD,4));menu.slots.get(2).set(new ItemStack(Items.APPLE,5));
        h.assertTrue(menu.clickMenuButton(p,12)&&menu.clickMenuButton(p,4),"Server accepts protection and priority controls");
        menu.clickMenuButton(p,1);
        h.assertTrue(menu.slots.get(0).getItem().is(Items.CARROT)&&menu.slots.get(1).getItem().is(Items.BREAD),"Sort preserves protected slot and high-priority group");
        menu.clickMenuButton(p,2);h.assertTrue(LunchBoxItem.feedingStatus(box,p)==1,"Paused status");
        menu.clickMenuButton(p,2);p.getFoodData().setFoodLevel(20);h.assertTrue(LunchBoxItem.feedingStatus(box,p)==3,"Full status");
        p.getFoodData().setFoodLevel(0);h.assertTrue(LunchBoxItem.feedingStatus(box,p)==0,"Ready status");
        h.assertTrue(LunchBoxItem.feedingStatus(box(ItemStack.EMPTY),p)==5,"Empty status");
        h.assertFalse(menu.clickMenuButton(p,21),"Unknown action rejected");
        p.getInventory().setItem(0,ItemStack.EMPTY);h.assertFalse(menu.clickMenuButton(p,2),"Stale menu cannot change old box");h.succeed();
    }}
