package com.agrievolution.content;

import java.util.*;
import net.minecraft.world.item.*;
import net.minecraft.world.food.FoodProperties;
import net.neoforged.neoforge.registries.DeferredItem;

/** Prepared dishes use normal food components and vanilla consumption/remainder handling. */
public final class CookingFoods {
    public record Dish(String id,int nutrition,float saturation,boolean bowl,String[] ingredients,DeferredItem<Item> item) {}
    public static final List<Dish> ALL=List.of(
        dish("garden_sandwich",8,0.65f,false,new String[]{"minecraft:bread","agri_evolution:lettuce","agri_evolution:tomato","agri_evolution:cucumber","agri_evolution:radish"}),
        dish("chicken_club",11,0.65f,false,new String[]{"minecraft:bread","minecraft:cooked_chicken","agri_evolution:lettuce","agri_evolution:tomato","agri_evolution:basil"}),
        dish("pork_apple_sandwich",11,0.65f,false,new String[]{"minecraft:bread","minecraft:cooked_porkchop","minecraft:apple","agri_evolution:cabbage"}),
        dish("beef_onion_sandwich",12,0.65f,false,new String[]{"minecraft:bread","minecraft:cooked_beef","agri_evolution:onion","agri_evolution:pepper"}),
        dish("herbed_egg_sandwich",9,0.65f,false,new String[]{"minecraft:bread","minecraft:egg","agri_evolution:parsley","agri_evolution:coriander"}),
        dish("garden_vegetable_wrap",9,0.65f,false,new String[]{"minecraft:bread","agri_evolution:aubergine","agri_evolution:courgette","agri_evolution:garlic","agri_evolution:rosemary"}),
        dish("tomato_basil_soup",10,0.65f,true,new String[]{"minecraft:bowl","agri_evolution:tomato","agri_evolution:basil","agri_evolution:onion"}),
        dish("butternut_soup",10,0.65f,true,new String[]{"minecraft:bowl","agri_evolution:butternut_squash","agri_evolution:ginger","agri_evolution:garlic"}),
        dish("leek_potato_soup",11,0.65f,true,new String[]{"minecraft:bowl","agri_evolution:leek","minecraft:baked_potato","agri_evolution:thyme"}),
        dish("root_vegetable_stew",12,0.65f,true,new String[]{"minecraft:bowl","agri_evolution:turnip","agri_evolution:parsnip","minecraft:carrot","agri_evolution:celery"}),
        dish("chicken_rice_soup",13,0.65f,true,new String[]{"minecraft:bowl","minecraft:cooked_chicken","agri_evolution:rice","agri_evolution:peas","agri_evolution:coriander"}),
        dish("hearty_beef_stew",14,0.65f,true,new String[]{"minecraft:bowl","minecraft:cooked_beef","minecraft:baked_potato","agri_evolution:pepper","agri_evolution:rosemary"}),
        dish("strawberry_tart",8,0.35f,false,new String[]{"minecraft:wheat","minecraft:egg","minecraft:sugar","agri_evolution:strawberry"}),
        dish("blueberry_muffin",7,0.35f,false,new String[]{"minecraft:wheat","minecraft:egg","minecraft:sugar","agri_evolution:blueberry"}),
        dish("blackberry_crumble",8,0.35f,false,new String[]{"minecraft:wheat","minecraft:sugar","agri_evolution:blackberry","minecraft:apple"}),
        dish("raspberry_rice_pudding",9,0.35f,true,new String[]{"minecraft:bowl","agri_evolution:rice","minecraft:sugar","agri_evolution:raspberry","minecraft:milk_bucket"}),
        dish("cranberry_cookie",6,0.35f,false,new String[]{"minecraft:wheat","minecraft:sugar","agri_evolution:cranberry","minecraft:cocoa_beans"}),
        dish("pumpkin_spice_pie",9,0.35f,false,new String[]{"agri_evolution:pumpkin","minecraft:wheat","minecraft:egg","minecraft:sugar","agri_evolution:ginger"}),
        dish("roast_chicken_dinner",14,0.8f,false,new String[]{"minecraft:cooked_chicken","agri_evolution:sweet_potato","agri_evolution:broccoli","agri_evolution:thyme"}),
        dish("herb_lamb_platter",14,0.8f,false,new String[]{"minecraft:cooked_mutton","agri_evolution:peas","agri_evolution:parsnip","agri_evolution:rosemary"}),
        dish("pork_harvest_plate",14,0.8f,false,new String[]{"minecraft:cooked_porkchop","agri_evolution:corn","agri_evolution:cabbage","agri_evolution:turnip"}),
        dish("salmon_rice_bowl",13,0.8f,true,new String[]{"minecraft:bowl","minecraft:cooked_salmon","agri_evolution:rice","agri_evolution:asparagus","agri_evolution:garlic"}),
        dish("rabbit_garden_stew",14,0.8f,true,new String[]{"minecraft:bowl","minecraft:cooked_rabbit","minecraft:carrot","agri_evolution:beans","agri_evolution:celery"}),
        dish("spiced_vegetable_curry",12,0.8f,true,new String[]{"minecraft:bowl","agri_evolution:rice","agri_evolution:beans","agri_evolution:aubergine","agri_evolution:turmeric","agri_evolution:pepper","agri_evolution:mint"}),
        dish("green_garden_broth",10,0.65f,true,new String[]{"minecraft:bowl","agri_evolution:spinach","agri_evolution:spring_onion","agri_evolution:dill"}),
        dish("roasted_vegetable_bowl",12,0.8f,true,new String[]{"minecraft:bowl","agri_evolution:cauliflower","agri_evolution:artichoke","agri_evolution:fennel"}),
        dish("spicy_peanut_noodles",12,0.8f,true,new String[]{"minecraft:bowl","minecraft:wheat","agri_evolution:peanut","agri_evolution:chilli","agri_evolution:sesame"}),
        dish("berry_oat_crumble",9,0.35f,false,new String[]{"agri_evolution:oats","agri_evolution:gooseberry","agri_evolution:blackcurrant","minecraft:sugar"}),
        dish("braised_garden_greens",10,0.65f,true,new String[]{"minecraft:bowl","agri_evolution:kale","agri_evolution:pak_choi","agri_evolution:brussels_sprouts","agri_evolution:garlic"}),
        dish("beef_pulse_stew",14,0.8f,true,new String[]{"minecraft:bowl","minecraft:cooked_beef","agri_evolution:swede","agri_evolution:celeriac","agri_evolution:lentil"}),
        dish("chickpea_grain_bowl",12,0.8f,true,new String[]{"minecraft:bowl","agri_evolution:chickpea","agri_evolution:barley","agri_evolution:buckwheat","agri_evolution:parsley"}),
        dish("summer_fruit_compote",9,0.35f,true,new String[]{"minecraft:bowl","agri_evolution:rhubarb","agri_evolution:grape","agri_evolution:cantaloupe","minecraft:sugar"}),
        dish("salmon_dill_sandwich",11,0.65f,false,new String[]{"minecraft:bread","agri_evolution:dill","agri_evolution:cucumber","minecraft:cooked_salmon"}),
        dish("pulled_pork_slaw_roll",12,0.65f,false,new String[]{"minecraft:bread","minecraft:cooked_porkchop","agri_evolution:cabbage","agri_evolution:chilli","agri_evolution:onion"}),
        dish("chickpea_salad_wrap",10,0.65f,false,new String[]{"minecraft:bread","agri_evolution:chickpea","agri_evolution:cucumber","agri_evolution:tomato","agri_evolution:parsley"}),
        dish("mutton_barley_soup",14,0.8f,true,new String[]{"minecraft:bowl","minecraft:cooked_mutton","agri_evolution:barley","agri_evolution:turnip","agri_evolution:thyme"}),
        dish("creamy_cauliflower_soup",11,0.65f,true,new String[]{"minecraft:bowl","agri_evolution:cauliflower","agri_evolution:leek","minecraft:milk_bucket","agri_evolution:garlic"}),
        dish("spicy_lentil_soup",12,0.65f,true,new String[]{"minecraft:bowl","agri_evolution:lentil","agri_evolution:tomato","agri_evolution:chilli","agri_evolution:ginger"}),
        dish("rhubarb_custard_tart",9,0.35f,false,new String[]{"agri_evolution:rhubarb","minecraft:wheat","minecraft:egg","minecraft:milk_bucket","minecraft:sugar"}),
        dish("grape_oat_cookie",7,0.35f,false,new String[]{"agri_evolution:grape","agri_evolution:oats","minecraft:wheat","minecraft:sugar"}),
        dish("melon_berry_sundae",9,0.35f,true,new String[]{"minecraft:bowl","agri_evolution:cantaloupe","agri_evolution:strawberry","minecraft:milk_bucket","minecraft:sugar"}),
        dish("beef_garden_pie",14,0.8f,false,new String[]{"minecraft:cooked_beef","minecraft:wheat","agri_evolution:peas","minecraft:carrot","agri_evolution:onion"}),
        dish("garlic_cod_dinner",13,0.8f,false,new String[]{"minecraft:cooked_cod","minecraft:baked_potato","agri_evolution:asparagus","agri_evolution:garlic"}),
        dish("chicken_sesame_rice",14,0.8f,true,new String[]{"minecraft:bowl","minecraft:cooked_chicken","agri_evolution:sesame","agri_evolution:rice","agri_evolution:pak_choi","agri_evolution:spring_onion"})
    );
    private static Dish dish(String id,int nutrition,float saturation,boolean bowl,String[] ingredients) {
        var food=new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation);
        if(bowl)food.usingConvertsTo(Items.BOWL);
        var item=AEItems.ITEMS.registerItem(id,Item::new,new Item.Properties().stacksTo(16).food(food.build()));
        return new Dish(id,nutrition,saturation,bowl,ingredients,item);
    }
    public static void bootstrap() {}
    public static boolean contains(String id){return ALL.stream().anyMatch(d->d.id().equals(id));}
    private CookingFoods(){}
}
