package com.anthonyahellman.gluttony.gameplay;

public final class GluttonyExtraction {
    private GluttonyExtraction() {}

    public static double statFraction(int level) {
        if (level >= 95) return 1.00;
        if (level >= 85) return 0.90;
        if (level >= 75) return 0.80;
        if (level >= 65) return 0.70;
        if (level >= 55) return 0.60;
        if (level >= 45) return 0.50;
        if (level >= 35) return 0.40;
        if (level >= 25) return 0.30;
        if (level >= 15) return 0.20;
        return Math.max(1, Math.min(10, level)) / 100.0;
    }

    public static double soulMultiplier(int level) {
        if (level >= 95) return 3.50;
        if (level >= 85) return 3.25;
        if (level >= 75) return 3.00;
        if (level >= 65) return 2.75;
        if (level >= 55) return 2.50;
        if (level >= 45) return 2.25;
        if (level >= 35) return 2.00;
        if (level >= 25) return 1.75;
        if (level >= 15) return 1.50;
        if (level >= 5) return 1.25;
        return 1.00;
    }
}
