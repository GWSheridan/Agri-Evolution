package com.agrievolution.farming;

import com.agrievolution.content.AgriCropBlock;
import com.agrievolution.genetics.CropSpecies;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Broad gardening groups, with stable names for saves and optional datapack tags. */
public enum CropFamily {
    UNKNOWN("unknown", "Unknown"), GRAIN("grain", "Grains"), LEGUME("legume", "Legumes"),
    ROOT("root", "Roots and bulbs"), LEAFY("leafy", "Leafy crops"), FRUIT("fruit", "Fruiting crops"),
    HERB("herb", "Herbs"), FLOWER("flower", "Flowers"), RESOURCE("resource", "Resources");

    public final String id, label;
    private final TagKey<Block> tag;
    CropFamily(String id, String label) {
        this.id=id; this.label=label;
        tag=TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("agri_evolution", "rotation/"+id));
    }
    public static CropFamily fromId(String id) {
        for(var family:values()) if(family.id.equals(id)) return family;
        return UNKNOWN;
    }
    public static CropFamily of(BlockState state) {
        for(var family:values()) if(family!=UNKNOWN && state.is(family.tag)) return family;
        if(state.getBlock() instanceof AgriCropBlock crop) return of(crop.species());
        var block=state.getBlock();
        if(block==Blocks.WHEAT) return GRAIN;
        if(block==Blocks.CARROTS || block==Blocks.POTATOES || block==Blocks.BEETROOTS) return ROOT;
        if(block==Blocks.MELON_STEM || block==Blocks.PUMPKIN_STEM || block==Blocks.ATTACHED_MELON_STEM
                || block==Blocks.ATTACHED_PUMPKIN_STEM || block==Blocks.SWEET_BERRY_BUSH) return FRUIT;
        if(block==Blocks.TORCHFLOWER_CROP || block==Blocks.PITCHER_CROP) return FLOWER;
        return UNKNOWN;
    }
    public static CropFamily of(CropSpecies crop) {
        var registered=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath("agri_evolution",crop.blockId())).defaultBlockState();
        for(var family:values())if(family!=UNKNOWN&&registered.is(family.tag))return family;
        if(crop.isResource()) return RESOURCE;
        return switch(crop) {
            case WILD, RICE, CORN, BARLEY, BUCKWHEAT, OATS, SESAME -> GRAIN;
            case PEAS, BEANS, LENTIL, CHICKPEA, PEANUT -> LEGUME;
            case ONION, GARLIC, RADISH, SWEET_POTATO, GINGER, TURMERIC, SWEDE, CELERIAC,
                    TURNIP, PARSNIP, LEEK, SPRING_ONION -> ROOT;
            case LETTUCE, BROCCOLI, CABBAGE, KALE, PAK_CHOI, BRUSSELS_SPROUTS,
                    SPINACH, CAULIFLOWER, ARTICHOKE, CELERY, ASPARAGUS, RHUBARB -> LEAFY;
            case BASIL, MINT, ROSEMARY, THYME, PARSLEY, CORIANDER, FENNEL, DILL -> HERB;
            case CRIMSON_CLOVER, AZURE_FLAX, VIOLET_STARFLOWER, AMBER_MARIGOLD, SILVER_THISTLE,
                    SUNFIRE_LILY, MOON_ORCHID, ECLIPSE_LOTUS, TWILIGHT_SAGE -> FLOWER;
            default -> FRUIT;
        };
    }
}
