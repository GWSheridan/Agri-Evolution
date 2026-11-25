package com.agrievolution.farming;

/** A small, deterministic climate model for the sealed Botanical Greenhouse.
 *  Temperatures are gameplay estimates derived from the biome and weather;
 *  Minecraft does not expose a real-world temperature or humidity sensor.
 */
public final class GreenhouseClimate {
    public static final int HEAT_GAIN_C = 9;
    public static final int VENT_COOLING_C = 4;
    public static final int COAL_SECONDS = 600;
    public static final int MAX_FUEL_SECONDS = 7200;
    private GreenhouseClimate() { }

    public static int clamp(int n, int low, int high) {
        return Math.max(low, Math.min(high, n));
    }

    public static int ambientTemperature(float biomeTemperature, boolean raining, boolean night) {
        // 0.8 (plains) -> 20C; deserts and snowy biomes feel quite different.
        int result = Math.round(9f + biomeTemperature * 14f);
        if (raining) result -= 2;
        if (night) result -= 4;
        return clamp(result, -20, 55);
    }

    public static int temperature(int ambient, boolean heating, boolean venting) {
        return clamp(ambient + (heating ? HEAT_GAIN_C : 0) - (venting ? VENT_COOLING_C : 0), -20, 55);
    }

    public static int humidity(float biomeDownfall, boolean raining, int water,
                               int farmland, int hydrated, boolean heating, boolean venting) {
        int moistureBonus = farmland == 0 ? 0 : (15 * hydrated / farmland);
        int humid = Math.round(30f + biomeDownfall * 35f) + (raining ? 15 : 0)
                + (water > 0 ? 7 : 0) + moistureBonus;
        if (heating) humid -= 12;
        if (venting) humid -= 18;
        return clamp(humid, 0, 100);
    }

    /** Chance of greenhouse-specific extra growth every five seconds.
     *  Natural crop growth and fertilizer effects are never disabled by weather.
     */
    public static int extraGrowthChance(int tempC, int humidityPct) {
        if (tempC >= 19 && tempC <= 28 && humidityPct >= 45 && humidityPct <= 78) return 45;
        if (tempC < 5 || tempC > 40 || humidityPct < 20 || humidityPct > 94) return 7;
        if (tempC < 12 || tempC > 34 || humidityPct < 30 || humidityPct > 88) return 15;
        return 25; // same as 0.11.0's 1-in-4 greenhouse bonus
    }
}
