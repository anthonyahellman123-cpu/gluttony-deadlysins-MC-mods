package com.anthonyahellman.gluttony.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public final class GreedData {
    private static final String ROOT = "RootsOfSinGreed";
    private static final String AVARICE = "Avarice";

    private final CompoundTag tag;

    private GreedData(CompoundTag tag) {
        this.tag = tag;
    }

    public static GreedData of(ServerPlayer player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT)) persistent.put(ROOT, new CompoundTag());
        return new GreedData(persistent.getCompound(ROOT));
    }

    public static void copy(ServerPlayer oldPlayer, ServerPlayer newPlayer) {
        CompoundTag oldPersistent = oldPlayer.getPersistentData();
        if (oldPersistent.contains(ROOT)) {
            newPlayer.getPersistentData().put(ROOT, oldPersistent.getCompound(ROOT).copy());
        }
    }

    public double avarice() {
        return Math.max(0.0, tag.getDouble(AVARICE));
    }

    public void addAvarice(double amount) {
        if (amount > 0.0) tag.putDouble(AVARICE, avarice() + amount);
    }

    public boolean spendAvarice(double amount) {
        if (amount < 0.0 || avarice() < amount) return false;
        tag.putDouble(AVARICE, avarice() - amount);
        return true;
    }
}
