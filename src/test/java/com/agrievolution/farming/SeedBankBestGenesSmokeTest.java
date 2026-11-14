package com.agrievolution.farming;
import java.util.List;

/** Run without Minecraft dependencies using javac, then java. */
public final class SeedBankBestGenesSmokeTest {
    public static void main(String[] args) {
        var seeds = List.of(
                new SeedBankSelection.Candidate(0, 2, 65),
                new SeedBankSelection.Candidate(1, 0, 96),
                new SeedBankSelection.Candidate(2, 2, 96),
                new SeedBankSelection.Candidate(3, 1, 96));
        if (SeedBankSelection.chooseStrongest(seeds, 0, 9) != 2)
            throw new AssertionError("Best genes must outrank higher slot priority");
        if (SeedBankSelection.chooseStrongest(seeds, 3, 9) != 2)
            throw new AssertionError("Equal genes must prefer high priority");
        if (SeedBankSelection.chooseStrongest(List.of(
                new SeedBankSelection.Candidate(0, 1, 90),
                new SeedBankSelection.Candidate(7, 1, 90)), 6, 9) != 7)
            throw new AssertionError("Equal genes and priority must rotate fairly");
        if (SeedBankSelection.chooseStrongest(List.of(), 0, 9) != -1)
            throw new AssertionError("Empty bank should return no seed");
        System.out.println("PASS: highest genetics, priority tie-break, rotation and empty bank");
    }
}
