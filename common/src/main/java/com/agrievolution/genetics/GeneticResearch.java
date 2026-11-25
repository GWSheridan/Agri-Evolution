package com.agrievolution.genetics;

/** Pure, deterministic research rules shared by the laboratory's server-side processes. */
public final class GeneticResearch {
    private GeneticResearch() { }

    /** A single selected gene gains +4, or +6 if a crop support is consumed. */
    public static CropGenome enhance(CropGenome before, int selectedTrait, boolean support) {
        int bonus = support ? 6 : 4;
        return new CropGenome(
                before.growth() + (selectedTrait == 0 ? bonus : 0),
                before.yield() + (selectedTrait == 1 ? bonus : 0),
                before.resilience() + (selectedTrait == 2 ? bonus : 0));
    }

    /** Stable gene grafting never reduces an existing trait or changes the seed species. */
    public static CropGenome graft(CropGenome original, CropGenome donor, boolean support) {
        int stability = support ? 2 : 0;
        return new CropGenome(
                Math.max(original.growth(), donor.growth()) + stability,
                Math.max(original.yield(), donor.yield()) + stability,
                Math.max(original.resilience(), donor.resilience()) + stability);
    }
}
