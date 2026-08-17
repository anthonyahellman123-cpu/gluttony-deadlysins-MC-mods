package com.anthonyahellman.gluttony.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public final class SinData {
    private static final String ROOT = "RootsOfSinCore";
    private static final String NATURAL_SIN = "NaturalSin";

    public enum NaturalSin {
        NONE,
        GLUTTONY,
        PRIDE
    }

    private SinData() {}

    public static NaturalSin selected(ServerPlayer player) {
        CompoundTag root = root(player);
        if (!root.contains(NATURAL_SIN) && GluttonyData.of(player).active()) {
            root.putString(NATURAL_SIN, NaturalSin.GLUTTONY.name());
        }
        try {
            return NaturalSin.valueOf(root.getString(NATURAL_SIN));
        } catch (IllegalArgumentException ignored) {
            return NaturalSin.NONE;
        }
    }

    public static boolean tryChoose(ServerPlayer player, NaturalSin sin) {
        NaturalSin current = selected(player);
        if (current != NaturalSin.NONE && current != sin) return false;
        root(player).putString(NATURAL_SIN, sin.name());
        return true;
    }

    public static void copy(ServerPlayer oldPlayer, ServerPlayer newPlayer) {
        newPlayer.getPersistentData().put(ROOT, oldPlayer.getPersistentData().getCompound(ROOT).copy());
    }

    private static CompoundTag root(ServerPlayer player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT)) persistent.put(ROOT, new CompoundTag());
        return persistent.getCompound(ROOT);
    }
}
