package com.anthonyahellman.gluttony.client;

/** Single size/count/timing surface for Gluttony's reusable visual language. */
public final class GluttonyVfxTuning {
    public static final float SOUL_CORE_SIZE = 0.115F;
    public static final float SOUL_WISP_SIZE = 0.072F;
    public static final float GLUTTONY_WISP_SIZE = 0.060F;
    public static final float HUNGER_FLICKER_SIZE = 0.075F;

    public static final int EXTRACTION_TICKS = 17;
    public static final int MAX_ACTIVE_EXTRACTIONS = 24;
    public static final int PRIMED_EMIT_INTERVAL = 3;
    public static final int CATCH_SEGMENTS = 9;
    public static final int CONSUMPTION_FLICKERS = 12;

    public static final int DEVOUR_TICKS = 15;
    public static final int MAX_ACTIVE_DEVOURS = 24;
    public static final int DEVOUR_JAW_POINTS = 9;
    public static final int DEVOUR_TRAIL_POINTS = 4;
    public static final int DEVOUR_CONSUMPTION_FLICKERS = 14;

    public static final int BEELZEBUB_FEAST_TICKS = 10;
    public static final int MAX_ACTIVE_FEAST_PULSES = 4;
    public static final int MAX_BEELZEBUB_TARGETS = 32;
    public static final int BEELZEBUB_PARTICLE_BUDGET_PER_TICK = 128;

    private GluttonyVfxTuning() {}
}
