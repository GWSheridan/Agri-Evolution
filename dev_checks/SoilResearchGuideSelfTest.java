import com.agrievolution.client.journal.MachineGuidePages;
import java.util.Arrays;

/** Standalone Java 21 test for soil-guide completeness and page layout. */
public final class SoilResearchGuideSelfTest {
    private static String allText() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < MachineGuidePages.count(); i++) {
            var p = MachineGuidePages.get(i);
            if (p.left().length > 6 || p.right().length > 6)
                throw new AssertionError("Too many lines on page: " + p.title());
            for (String s : p.left()) {
                if (s.length() > 23) throw new AssertionError("Long left line: " + s);
                b.append(s).append('\n');
            }
            for (String s : p.right()) {
                if (s.length() > 23) throw new AssertionError("Long right line: " + s);
                b.append(s).append('\n');
            }
        }
        return b.toString();
    }
    private static void has(String text, String required) {
        if (!text.contains(required)) throw new AssertionError("Missing guide item: " + required);
    }
    public static void main(String[] args) {
        if (MachineGuidePages.count() != 36) throw new AssertionError("Wrong instruction page count");
        String book = allText();
        for (String source : new String[]{
                "1. Dirt", "2. Grass Block", "3. Coarse Dirt", "4. Rooted Dirt",
                "5. Podzol", "6. Vanilla Farmland", "7. Cultivated Farmland",
                "8. Nutrient Farmland", "9. Mythic Farmland", "10. Arcane Farmland",
                "11. Celestial Farmland"}) has(book, source);
        for (String threshold : new String[]{"8: Cultivated", "24: Nutrient", "48: Mythic", "72: Arcane", "96: Celestial"})
            has(book, threshold);
        has(book, "No one-of-each rule!");
        has(book, "Each location counts");
        has(book, "ANALYZE uses samples.");
        System.out.println("PASS: 36 journal pages fit; all 11 collectible soil kinds and research milestones documented.");
    }
}
