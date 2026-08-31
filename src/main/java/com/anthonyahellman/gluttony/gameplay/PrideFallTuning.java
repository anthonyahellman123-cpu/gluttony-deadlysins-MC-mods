package com.anthonyahellman.gluttony.gameplay;

import net.minecraft.util.Mth;

/** Shared, dedicated-server-safe tuning values for Lucifer's Fall. */
public final class PrideFallTuning {
    public static final int MAX_STAGE = 5;
    public static final int CHARGE_STAGE_TICKS = 20 * 30;
    public static final int MAX_CHARGE_TICKS = CHARGE_STAGE_TICKS * MAX_STAGE;

    public static final double SHOCKWAVE_RADIUS_MULTIPLIER = 1.5;
    public static final double MAX_IMPACT_RADIUS = 128.0;
    public static final double MAX_SHOCKWAVE_RADIUS = MAX_IMPACT_RADIUS * SHOCKWAVE_RADIUS_MULTIPLIER;
    public static final int WAVE_DURATION_TICKS = 48;
    public static final int WAVE_DAMAGE_SAMPLE_TICKS = 2;
    public static final double WAVE_TARGET_HEIGHT = 3.25;

    public static final int WAVE_SEGMENTS = 256;
    public static final double WAVE_HEIGHT = 2.35;
    public static final double WAVE_WIDTH = 2.10;
    public static final int IMPACT_BURST_TICKS = 8;

    public static final float PARTICLE_SCALE_MOTE = 0.075F;
    public static final float PARTICLE_SCALE_STREAK = 0.145F;
    public static final float PARTICLE_SCALE_SHARD = 0.090F;
    public static final float PARTICLE_SCALE_EMBER = 0.050F;

    private PrideFallTuning() {}

    public static double waveProgress(double elapsedTicks, int durationTicks) {
        double linear = Mth.clamp(elapsedTicks / Math.max(1.0, durationTicks), 0.0, 1.0);
        return 1.0 - Math.pow(1.0 - linear, 2.35);
    }

    public static double waveRadius(double maxRadius, double elapsedTicks, int durationTicks) {
        return Math.max(0.0, maxRadius) * waveProgress(elapsedTicks, durationTicks);
    }
}
