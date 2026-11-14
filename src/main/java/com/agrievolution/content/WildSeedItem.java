package com.agrievolution.content;

import com.agrievolution.genetics.CropSpecies;

/** Legacy Wild Seed item ID and gameplay retained for existing saves. */
public final class WildSeedItem extends SpeciesSeedItem {
    public WildSeedItem(Properties properties) {
        super(properties, CropSpecies.WILD, AEBlocks.AGRI_CROP::get);
    }
}
