package com.agrievolution.content;

import com.agrievolution.genetics.CropSpecies;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class FlowerHarvests {
    private FlowerHarvests() {}

    public static Item dye(CropSpecies species) {
        return switch (species) {
            case CRIMSON_CLOVER -> Items.RED_DYE;
            case AZURE_FLAX -> Items.BLUE_DYE;
            case VIOLET_STARFLOWER -> Items.PURPLE_DYE;
            case AMBER_MARIGOLD -> Items.ORANGE_DYE;
            case SILVER_THISTLE -> Items.LIGHT_GRAY_DYE;
            case SUNFIRE_LILY -> Items.YELLOW_DYE;
            case MOON_ORCHID -> Items.LIGHT_BLUE_DYE;
            case ECLIPSE_LOTUS -> Items.PINK_DYE;
            case TWILIGHT_SAGE -> Items.MAGENTA_DYE;
            default -> throw new IllegalArgumentException("No flower dye for " + species);
        };
    }
}
