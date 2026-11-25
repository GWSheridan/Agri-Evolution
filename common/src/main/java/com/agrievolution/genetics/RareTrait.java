package com.agrievolution.genetics;

/** Inherited Tier-2 laboratory discoveries. None is the default for existing worlds. */
public enum RareTrait {
    NONE("No rare trait"),
    VIGOROUS("Vigorous - improved growth"),
    ABUNDANT("Abundant - extra harvest"),
    TENACIOUS("Tenacious - reduced nutrition loss");

    private final String description;
    RareTrait(String description) { this.description = description; }
    public String description() { return description; }
    public static RareTrait fromName(String name) {
        try { return valueOf(name); }
        catch (IllegalArgumentException | NullPointerException ignored) { return NONE; }
    }
    /** A rare trait can pass to a child; two traited parents increase the odds. */
    public static RareTrait inherit(RareTrait first, RareTrait second, java.util.Random random) {
        if (first == NONE && second == NONE) return NONE;
        if (first != NONE && second != NONE) {
            return random.nextInt(100) < 75 ? (random.nextBoolean() ? first : second) : NONE;
        }
        return random.nextBoolean() ? (first != NONE ? first : second) : NONE;
    }
    public static RareTrait discover(java.util.Random random) {
        return switch (random.nextInt(3)) {
            case 0 -> VIGOROUS;
            case 1 -> ABUNDANT;
            default -> TENACIOUS;
        };
    }
}
