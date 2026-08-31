package com.anthonyahellman.gluttony.client;

import com.anthonyahellman.gluttony.network.PrideStatePacket;

public final class PrideClientState {
    private static PrideStatePacket state = new PrideStatePacket(0, 0, 0, 0, 0, 0, 0, 0);

    private PrideClientState() {}

    public static void update(PrideStatePacket packet) { state = packet; }
    public static PrideStatePacket get() { return state; }
}
