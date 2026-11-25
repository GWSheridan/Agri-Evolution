package com.agrievolution.genetics;
/** Stable IDs for the 0.69 duplicate-resource cleanup; never reorder the legacy table. */
public final class RetiredResourceCrops {
    private RetiredResourceCrops() {}
    public static final java.util.Map<String,String> ALIASES = java.util.Map.ofEntries(
        java.util.Map.entry("snow_block","snowball"),
        java.util.Map.entry("honey_block","honey_bottle"),
        java.util.Map.entry("magma_block","magma_cream"),
        java.util.Map.entry("bone_block","bone"),
        java.util.Map.entry("slime_block","slime_ball"),
        java.util.Map.entry("honeycomb_block","honeycomb"),
        java.util.Map.entry("quartz_block","quartz"),
        java.util.Map.entry("copper_block","copper"),
        java.util.Map.entry("amethyst_block","amethyst"),
        java.util.Map.entry("nether_wart_block","nether_wart"));
    private static final String[] LEGACY_068 = {"wild","crimson_clover","azure_flax","violet_starflower","amber_marigold","silver_thistle","sunfire_lily","moon_orchid","eclipse_lotus","twilight_sage","dirt","wood","stone","coal","copper","iron","redstone","gold","lapis","diamond","emerald","quartz","gravel","sand","clay","flint","glowstone","obsidian","netherrack","soul_sand","magma_cream","nether_wart","blaze_rod","end_stone","chorus_fruit","ender_pearl","shulker_shell","ancient_debris","netherite","tomato","lettuce","cucumber","onion","garlic","pepper","strawberry","rice","corn","peas","beans","broccoli","cabbage","radish","aubergine","sweet_potato","basil","mint","rosemary","thyme","parsley","coriander","ginger","turmeric","string","wool","leather","bone","amethyst","prismarine","ghast_tear","breeze_rod","feather","ink_sac","honeycomb","slime_ball","gunpowder","prismarine_crystals","phantom_membrane","echo_shard","snowball","moss","calcite","terracotta","spider_eye","glow_ink_sac","crying_obsidian","nautilus_shell","paper","kelp","bamboo","blackstone","basalt","purpur","turtle_scute","sponge","granite","diorite","andesite","mud","tuff","dripstone","deepslate","soul_soil","sea_lantern","shroomlight","end_rod","sculk","red_sand","sandstone","cactus","vine","packed_mud","brick","ice","packed_ice","blue_ice","magma_block","nether_brick","ender_eye","charcoal","glass","brown_mushroom","red_mushroom","snow_block","honey_block","mossy_cobblestone","lily_pad","dark_prismarine","gilded_blackstone","sculk_catalyst","ochre_froglight","spruce_log","birch_log","acacia_log","dark_oak_log","mangrove_log","cherry_log","sea_pickle","glow_lichen","crimson_stem","warped_stem","verdant_froglight","pearlescent_froglight","sugar","cocoa_beans","egg","rotten_flesh","rabbit_hide","rabbit_foot","armadillo_scute","fermented_spider_eye","wither_rose","chorus_flower","crimson_fungus","warped_fungus","green_dye","red_dye","yellow_dye","brown_dye","honey_bottle","glow_berries","blue_dye","cyan_dye","fire_charge","wind_charge","dragon_breath","experience_bottle","white_dye","black_dye","orange_dye","pink_dye","purple_dye","magenta_dye","light_blue_dye","lime_dye","heart_of_the_sea","nether_star","dragon_head","heavy_core","stick","dried_kelp","gray_dye","light_gray_dye","bone_block","slime_block","honeycomb_block","quartz_block","skeleton_skull","wither_skeleton_skull","creeper_head","zombie_head","pumpkin","butternut_squash","courgette","turnip","parsnip","leek","celery","asparagus","blueberry","raspberry","blackberry","cranberry","spinach","cauliflower","artichoke","chilli","spring_onion","fennel","dill","peanut","sesame","oats","gooseberry","blackcurrant","kale","pak_choi","brussels_sprouts","swede","celeriac","lentil","chickpea","barley","buckwheat","rhubarb","grape","cantaloupe","jungle_log","coarse_dirt","rooted_dirt","red_sandstone","prismarine_bricks","nether_wart_block","warped_wart_block","red_nether_bricks","sculk_sensor","sculk_shrieker","conduit","respawn_anchor","grass_block","podzol","mycelium","dead_bush","copper_block","amethyst_block","mud_bricks","deepslate_bricks","calibrated_sculk_sensor","end_crystal","totem_of_undying","enchanted_golden_apple"};
    public static String canonical(String id) { return ALIASES.getOrDefault(id,id); }
    public static CropSpecies species(String id) {
        String current=canonical(id);
        for(var species:CropSpecies.values())if(species.id().equals(current))return species;
        return null;
    }
    public static int legacyMonitorValue(int value) {
        if(value<=0||value>LEGACY_068.length)return 0;
        var species=species(LEGACY_068[value-1]);
        return species==null?0:species.ordinal()+1;
    }
    public static String monitorId(int value) { return value>0&&value<=CropSpecies.values().length?CropSpecies.values()[value-1].id():""; }
    public static int monitorValue(String id) { var species=species(id);return species==null?0:species.ordinal()+1; }
}
