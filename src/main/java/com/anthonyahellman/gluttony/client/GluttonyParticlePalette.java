package com.anthonyahellman.gluttony.client;

import org.joml.Vector3f;

/** Locked, high-separation Gluttony palette; tune all custom particle color here. */
public final class GluttonyParticlePalette {
    public static final Vector3f SOUL_WHITE = rgb(244, 255, 255);     // #F4FFFF
    public static final Vector3f SOUL_CYAN = rgb(80, 213, 255);       // #50D5FF
    public static final Vector3f GLUTTONY_PURPLE = rgb(104, 24, 190); // #6818BE
    public static final Vector3f DEEP_PURPLE = rgb(39, 4, 70);        // #270446
    public static final Vector3f HUNGER_RED = rgb(197, 24, 48);       // #C51830
    public static final Vector3f DARK_RED = rgb(86, 3, 17);           // #560311

    private GluttonyParticlePalette() {}

    public static Vector3f mix(Vector3f from, Vector3f to, float amount) {
        return new Vector3f(from).lerp(to, Math.max(0.0F, Math.min(1.0F, amount)));
    }

    private static Vector3f rgb(int red, int green, int blue) {
        return new Vector3f(red / 255.0F, green / 255.0F, blue / 255.0F);
    }
}
