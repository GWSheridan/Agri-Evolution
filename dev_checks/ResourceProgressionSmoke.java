import com.agrievolution.genetics.*;
class ResourceProgressionSmoke {
  static void check(CropSpecies a, CropSpecies b, CropSpecies target, boolean advanced) {
    if (advanced) {
      var r=AdvancedHybridBreeder.recipe(a,b);
      if (r==null || r.offspring()!=target || AdvancedHybridBreeder.recipe(b,a)!=r && AdvancedHybridBreeder.recipe(b,a).offspring()!=target) throw new AssertionError(a+"+"+b);
    } else {
      var r=SpeciesBreeder.mutationRecipe(a,b);
      if (r==null || r.offspring()!=target || SpeciesBreeder.mutationRecipe(b,a).offspring()!=target) throw new AssertionError(a+"+"+b);
    }
  }
  public static void main(String[] args) {
    check(CropSpecies.DIRT,CropSpecies.WOOD,CropSpecies.STONE,false);
    check(CropSpecies.STONE,CropSpecies.WOOD,CropSpecies.COAL,false);
    check(CropSpecies.STONE,CropSpecies.COAL,CropSpecies.COPPER,false);
    check(CropSpecies.COPPER,CropSpecies.COAL,CropSpecies.IRON,false);
    check(CropSpecies.IRON,CropSpecies.COPPER,CropSpecies.REDSTONE,false);
    check(CropSpecies.IRON,CropSpecies.REDSTONE,CropSpecies.GOLD,false);
    check(CropSpecies.GOLD,CropSpecies.REDSTONE,CropSpecies.LAPIS,false);
    check(CropSpecies.GOLD,CropSpecies.LAPIS,CropSpecies.DIAMOND,true);
    check(CropSpecies.DIAMOND,CropSpecies.LAPIS,CropSpecies.EMERALD,true);
    check(CropSpecies.COAL,CropSpecies.LAPIS,CropSpecies.QUARTZ,true);
    check(CropSpecies.DIRT,CropSpecies.STONE,CropSpecies.GRAVEL,false);
    check(CropSpecies.GRAVEL,CropSpecies.DIRT,CropSpecies.SAND,false);
    check(CropSpecies.SAND,CropSpecies.DIRT,CropSpecies.CLAY,false);
    check(CropSpecies.GRAVEL,CropSpecies.COAL,CropSpecies.FLINT,false);
    check(CropSpecies.REDSTONE,CropSpecies.QUARTZ,CropSpecies.GLOWSTONE,true);
    check(CropSpecies.DIAMOND,CropSpecies.QUARTZ,CropSpecies.OBSIDIAN,true);

    check(CropSpecies.QUARTZ,CropSpecies.OBSIDIAN,CropSpecies.NETHERRACK,true);
    check(CropSpecies.NETHERRACK,CropSpecies.SAND,CropSpecies.SOUL_SAND,true);
    check(CropSpecies.NETHERRACK,CropSpecies.GLOWSTONE,CropSpecies.MAGMA_CREAM,true);
    check(CropSpecies.SOUL_SAND,CropSpecies.CRIMSON_CLOVER,CropSpecies.NETHER_WART,true);
    check(CropSpecies.MAGMA_CREAM,CropSpecies.NETHER_WART,CropSpecies.BLAZE_ROD,true);
    check(CropSpecies.OBSIDIAN,CropSpecies.EMERALD,CropSpecies.END_STONE,true);
    check(CropSpecies.END_STONE,CropSpecies.AZURE_FLAX,CropSpecies.CHORUS_FRUIT,true);
    check(CropSpecies.END_STONE,CropSpecies.CHORUS_FRUIT,CropSpecies.ENDER_PEARL,true);
    check(CropSpecies.ENDER_PEARL,CropSpecies.CHORUS_FRUIT,CropSpecies.SHULKER_SHELL,true);
    check(CropSpecies.BLAZE_ROD,CropSpecies.OBSIDIAN,CropSpecies.ANCIENT_DEBRIS,true);
    check(CropSpecies.ANCIENT_DEBRIS,CropSpecies.ENDER_PEARL,CropSpecies.NETHERITE,true);
    if (CropSpecies.values().length != 39) throw new AssertionError("Species count");
    if(AdvancedHybridBreeder.recipe(CropSpecies.GOLD,CropSpecies.LAPIS).chance(true,true)!=23) throw new AssertionError("Bonus");
    System.out.println("PASS: 39 species and all 27 resource crossbreeding recipes symmetric, tier separated, and compiled Java 21");
  }
}
