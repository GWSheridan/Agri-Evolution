import com.agrievolution.genetics.CropGenome;
import com.agrievolution.genetics.GeneticResearch;

public class GeneticResearchSelfTest {
    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        CropGenome original = new CropGenome(25, 25, 25);
        require(GeneticResearch.enhance(original, 0, false).equals(new CropGenome(29,25,25)), "growth");
        require(GeneticResearch.enhance(original, 1, true).equals(new CropGenome(25,31,25)), "yield boost");
        require(GeneticResearch.enhance(original, 2, false).equals(new CropGenome(25,25,29)), "resilience");
        require(GeneticResearch.graft(original, new CropGenome(70,10,50), false)
            .equals(new CropGenome(70,25,50)), "donor maxima");
        require(GeneticResearch.graft(original, new CropGenome(70,10,50), true)
            .equals(new CropGenome(72,27,52)), "stabilization");
        require(GeneticResearch.enhance(new CropGenome(98,100,99), 0, true)
            .equals(new CropGenome(100,100,99)), "clamping");
        System.out.println("Genetics research tests passed (6 checks).");
    }
}
