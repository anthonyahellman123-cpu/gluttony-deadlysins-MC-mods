package com.anthonyahellman.gluttony.client;

import org.joml.Vector3f;

/** Single tuning point for every custom Pride particle tint. */
public final class PrideParticlePalette {
    public static final Vector3f RADIANT_WHITE = rgb(255, 250, 225);   // #FFFAE1
    public static final Vector3f BRILLIANT_GOLD = rgb(255, 190, 40);  // #FFBE28
    public static final Vector3f DEEP_GOLD = rgb(180, 105, 15);       // #B4690F
    public static final Vector3f BLACKENED_GOLD = rgb(30, 20, 10);   // #1E140A
    public static final Vector3f SIN_RED = rgb(110, 18, 22);          // #6E1216

    private PrideParticlePalette() {}

    public static Vector3f mix(Vector3f from, Vector3f to, float amount) {
        float clamped = Math.max(0.0F, Math.min(1.0F, amount));
        return new Vector3f(from).lerp(to, clamped);
    }

    private static Vector3f rgb(int red, int green, int blue) {
        return new Vector3f(red / 255.0F, green / 255.0F, blue / 255.0F);
    }
}
