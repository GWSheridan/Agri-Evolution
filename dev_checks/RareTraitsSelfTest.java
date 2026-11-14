import com.agrievolution.genetics.RareTrait;
import java.util.Random;

public final class RareTraitsSelfTest {
    private static void require(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
    }
    public static void main(String[] args) {
        require(RareTrait.fromName("") == RareTrait.NONE, "older crops default to NONE");
        require(RareTrait.fromName("VIGOROUS") == RareTrait.VIGOROUS, "rare trait restores from save");
        require(RareTrait.fromName("UNKNOWN") == RareTrait.NONE, "unknown trait degrades safely");
        Random rng = new Random(1616);
        int single = 0, doubleCount = 0;
        for (int i = 0; i < 10000; i++) {
            require(RareTrait.inherit(RareTrait.NONE, RareTrait.NONE, rng) == RareTrait.NONE,
                    "two untraited parents never invent traits");
            if (RareTrait.inherit(RareTrait.VIGOROUS, RareTrait.NONE, rng) != RareTrait.NONE) single++;
            if (RareTrait.inherit(RareTrait.VIGOROUS, RareTrait.ABUNDANT, rng) != RareTrait.NONE) doubleCount++;
            require(RareTrait.discover(rng) != RareTrait.NONE, "successful discovery has rare trait");
        }
        require(single > 4750 && single < 5250, "single parent chance ~50%");
        require(doubleCount > 7250 && doubleCount < 7750, "two parents chance ~75%");
        System.out.println("Rare trait pure-Java tests passed (7 assertions, 30,000 generations).");
    }
}
