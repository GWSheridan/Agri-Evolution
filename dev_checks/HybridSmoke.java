import com.agrievolution.genetics.*;

public class HybridSmoke {
    public static void main(String[] args) {
        if (AdvancedHybridBreeder.recipe(CropSpecies.SUNFIRE_LILY, CropSpecies.MOON_ORCHID).offspring() != CropSpecies.ECLIPSE_LOTUS) throw new AssertionError("lotus");
        if (AdvancedHybridBreeder.recipe(CropSpecies.SILVER_THISTLE, CropSpecies.AMBER_MARIGOLD).offspring() != CropSpecies.TWILIGHT_SAGE) throw new AssertionError("sage");
        if (AdvancedHybridBreeder.recipe(CropSpecies.AZURE_FLAX, CropSpecies.CRIMSON_CLOVER) != null) throw new AssertionError("old recipe unexpectedly modified");
        var lotus = AdvancedHybridBreeder.recipe(CropSpecies.SUNFIRE_LILY, CropSpecies.MOON_ORCHID);
        if (lotus.chance(false,false) !=12 || lotus.chance(true,false)!=20 || lotus.chance(true,true)!=25) throw new AssertionError("chances");
        var out = AdvancedHybridBreeder.crossGenes(new CropGenome(50,60,70),new CropGenome(70,80,90),17L,true);
        if (out.growth()<59||out.growth()>69||out.yield()<69||out.yield()>89||out.resilience()<79||out.resilience()>99) throw new AssertionError("genes "+out);
        int count=0; for (int i=0;i<100_000;i++) {
            if (AdvancedHybridBreeder.crossSpecies(CropSpecies.SUNFIRE_LILY,CropSpecies.MOON_ORCHID,i,false,false)==CropSpecies.ECLIPSE_LOTUS) count++;
        }
        if (count<10_000||count>14_000) throw new AssertionError("12% frequency" + count);
        try { AdvancedHybridBreeder.crossSpecies(CropSpecies.SUNFIRE_LILY,CropSpecies.SUNFIRE_LILY,0,false,false);throw new AssertionError("same species accepted");} catch(IllegalArgumentException success) { }
        System.out.println("Hybrid breeding Java smoke tests PASS; Eclipse Lotus discovery: " + count + " / 100000");
    }
}
