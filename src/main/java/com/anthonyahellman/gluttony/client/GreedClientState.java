package com.anthonyahellman.gluttony.client;

import com.anthonyahellman.gluttony.network.GreedStatePacket;

public final class GreedClientState {
    private static GreedStatePacket state = new GreedStatePacket(
            0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 100_000, 0);

    private GreedClientState() {}

    public static void update(GreedStatePacket packet) { state = packet; }
    public static GreedStatePacket get() { return state; }
}
