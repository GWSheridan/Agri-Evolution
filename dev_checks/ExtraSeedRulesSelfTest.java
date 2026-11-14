import com.agrievolution.farming.ExtraSeedRules;

/** Runnable without Minecraft classes: javac -d build/checks src/main/java/com/agrievolution/farming/ExtraSeedRules.java dev_checks/ExtraSeedRulesSelfTest.java */
public class ExtraSeedRulesSelfTest {
    public static void main(String[] args) {
        int[] genetics = {0, 25, 50, 75, 100};
        int[] percent = {20, 20, 30, 40, 50};
        for (int i = 0; i < genetics.length; i++) {
            if (ExtraSeedRules.chancePercent(genetics[i], false) != percent[i])
                throw new AssertionError("Yield " + genetics[i]);
        }
        if (ExtraSeedRules.chancePercent(100, true) != 60)
            throw new AssertionError("Abundant maximum");
        if (ExtraSeedRules.chancePercent(25, true) != 30)
            throw new AssertionError("Abundant baseline");
        System.out.println("PASS: Agri Evolution extra-seed chance rules");
    }
}
