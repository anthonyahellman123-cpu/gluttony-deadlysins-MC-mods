package com.anthonyahellman.gluttony.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public final class GluttonyData {
    private static final String ROOT = "DemonsBountyGluttony";
    private static final String ACTIVE = "Active";
    private static final String AWAKENING = "Awakening";
    private static final String CURRENT_SOULS = "CurrentSouls";
    private static final String LIFETIME_SOULS = "LifetimeSouls";
    private static final String LEVEL = "Level";
    private static final String HEALTH = "ExtractedHealth";
    private static final String ATTACK = "ExtractedAttack";

    private final CompoundTag tag;

    private GluttonyData(CompoundTag tag) {
        this.tag = tag;
    }

    public static GluttonyData of(ServerPlayer player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT)) persistent.put(ROOT, new CompoundTag());
        return new GluttonyData(persistent.getCompound(ROOT));
    }

    public boolean active() { return tag.getBoolean(ACTIVE); }
    public boolean awakening() { return tag.getBoolean(AWAKENING); }
    public void beginAwakening() {
        tag.putBoolean(ACTIVE, true);
        tag.putBoolean(AWAKENING, true);
    }
    public void stabilize() { tag.putBoolean(AWAKENING, false); }
    public double currentSouls() { return tag.getDouble(CURRENT_SOULS); }
    public double lifetimeSouls() { return tag.getDouble(LIFETIME_SOULS); }
    public int level() { return Math.max(1, tag.getInt(LEVEL)); }
    public double extractedHealth() { return tag.getDouble(HEALTH); }
    public double extractedAttack() { return tag.getDouble(ATTACK); }

    public void addSouls(double amount) {
        if (amount <= 0) return;
        tag.putDouble(CURRENT_SOULS, currentSouls() + amount);
        tag.putDouble(LIFETIME_SOULS, lifetimeSouls() + amount);
        tag.putInt(LEVEL, levelFor(lifetimeSouls()));
    }

    public boolean spendSouls(double amount) {
        if (amount <= 0) return true;
        if (currentSouls() < amount) return false;
        tag.putDouble(CURRENT_SOULS, currentSouls() - amount);
        return true;
    }

    /** Returns spent souls without advancing lifetime progression. */
    public void refundSouls(double amount) {
        if (amount > 0) tag.putDouble(CURRENT_SOULS, currentSouls() + amount);
    }

    public void addExtractedStats(double health, double attack) {
        tag.putDouble(HEALTH, extractedHealth() + health);
        tag.putDouble(ATTACK, extractedAttack() + attack);
    }

    public static int levelFor(double lifetimeSouls) {
        return Math.min(100, 1 + (int) Math.floor(Math.sqrt(Math.max(0, lifetimeSouls) / 25.0)));
    }

    public static int soulsRequiredForLevel(int level) {
        int adjusted = Math.max(0, Math.min(99, level - 1));
        return 25 * adjusted * adjusted;
    }

    public static void copy(ServerPlayer oldPlayer, ServerPlayer newPlayer) {
        CompoundTag oldRoot = oldPlayer.getPersistentData().getCompound(ROOT);
        newPlayer.getPersistentData().put(ROOT, oldRoot.copy());
    }
}
