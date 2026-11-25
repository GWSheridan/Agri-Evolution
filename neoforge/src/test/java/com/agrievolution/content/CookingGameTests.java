package com.agrievolution.content;

import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("agri_evolution") @PrefixGameTestTemplate(false)
public final class CookingGameTests {
    @GameTest(template="empty") public static void dishesCraftWithoutRecipeConflicts(GameTestHelper h) {
        var level=h.getLevel();var manager=level.getRecipeManager();
        for(var dish:CookingFoods.ALL) {
            var stacks=new ArrayList<ItemStack>();
            for(var id:dish.ingredients())stacks.add(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id))));
            while(stacks.size()<9)stacks.add(ItemStack.EMPTY);
            for(int rotation=0;rotation<9;rotation++) {
                Collections.rotate(stacks,1);
                var input=CraftingInput.of(3,3,stacks);
                var matches=manager.getAllRecipesFor(RecipeType.CRAFTING).stream().filter(r->r.value().matches(input,level)).toList();
                h.assertTrue(matches.size()==1,"Exactly one recipe, including vanilla and existing mod recipes: "+dish.id());
                var recipe=matches.getFirst();
                h.assertTrue(recipe.id().equals(ResourceLocation.fromNamespaceAndPath("agri_evolution",dish.id())),"Expected recipe ID");
                var result=recipe.value().assemble(input,level.registryAccess());
                h.assertTrue(result.is(dish.item().get())&&result.getCount()==1,"Correct edible result");
                var remainders=recipe.value().getRemainingItems(input);
                for(int i=0;i<input.size();i++)if(input.getItem(i).is(Items.MILK_BUCKET))
                    h.assertTrue(remainders.get(i).is(Items.BUCKET),"Milk bucket returned after crafting");
            }
            var holder=manager.byKey(ResourceLocation.fromNamespaceAndPath("agri_evolution",dish.id())).orElseThrow();
            var recipe=(CraftingRecipe)holder.value();
            for(int i=0;i<stacks.size();i++)if(!stacks.get(i).isEmpty()) {
                var incomplete=new ArrayList<>(stacks);incomplete.set(i,ItemStack.EMPTY);
                h.assertFalse(recipe.matches(CraftingInput.of(3,3,incomplete),level),"Missing ingredient rejected: "+dish.id());
            }
        }
        h.assertTrue(CookingFoods.ALL.size()==44,"44 dishes loaded");h.succeed();
    }

    @GameTest(template="empty") public static void eatingRestoresHungerAndReturnsBowls(GameTestHelper h) {
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        for(var dish:CookingFoods.ALL)for(int count:new int[]{1,2}) {
            player.getInventory().clearContent();player.getFoodData().setFoodLevel(0);player.getFoodData().setSaturation(0);
            var stack=new ItemStack(dish.item().get(),count);
            var food=stack.get(DataComponents.FOOD);
            h.assertTrue(food!=null&&food.nutrition()==dish.nutrition(),"Registered nutrition");
            h.assertTrue(stack.getMaxStackSize()==16,"Meals stack to 16");
            var result=stack.finishUsingItem(h.getLevel(),player);
            h.assertTrue(player.getFoodData().getFoodLevel()==dish.nutrition(),"Eating restores hunger: "+dish.id());
            h.assertTrue(player.getFoodData().getSaturationLevel()>0,"Eating restores saturation");
            if(count==2)h.assertTrue(result.is(dish.item().get())&&result.getCount()==1,"Only one serving consumed");
            else h.assertTrue(dish.bowl()?result.is(Items.BOWL):result.isEmpty(),"Last serving returns only its bowl");
            int bowls=result.is(Items.BOWL)?result.getCount():0;
            for(var inv:player.getInventory().items)if(inv.is(Items.BOWL))bowls+=inv.getCount();
            h.assertTrue(bowls==(dish.bowl()?1:0),"Exactly one bowl per bowl meal, including stacked consumption");
        }h.succeed();
    }

    @GameTest(template="empty") public static void everyFoodCropHasACookingUse(GameTestHelper h) {
        var used=new HashSet<String>();for(var dish:CookingFoods.ALL)used.addAll(Arrays.asList(dish.ingredients()));
        int covered=0;
        for(var species:com.agrievolution.genetics.CropSpecies.values())if(species.isFood()) {
            h.assertTrue(used.contains("agri_evolution:"+species.id()),"Cooking uses "+species.id());covered++;
        }
        h.assertTrue(covered==60,"All 60 food crops used");h.succeed();
    }
}

