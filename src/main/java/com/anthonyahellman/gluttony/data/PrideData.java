package com.anthonyahellman.gluttony.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public final class PrideData {
    private static final String ROOT = "RootsOfSinPride";

    public enum Trial {
        ENDER_DRAGON("Ender Dragons", "EnderDragons", 12),
        WITHER("Withers", "Withers", 8),
        ELDER_GUARDIAN("Elder Guardians", "ElderGuardians", 4),
        WARDEN("Wardens", "Wardens", 2);

        private final String displayName;
        private final String key;
        private final int required;

        Trial(String displayName, String key, int required) {
            this.displayName = displayName;
            this.key = key;
            this.required = required;
        }

        public String displayName() { return displayName; }
        public int required() { return required; }
    }

    private final CompoundTag tag;

    private PrideData(CompoundTag tag) {
        this.tag = tag;
    }

    public static PrideData of(ServerPlayer player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT)) persistent.put(ROOT, new CompoundTag());
        return new PrideData(persistent.getCompound(ROOT));
    }

    public int count(Trial trial) {
        return Math.min(trial.required, Math.max(0, tag.getInt(trial.key)));
    }

    public boolean complete(Trial trial) {
        return count(trial) >= trial.required;
    }

    public boolean increment(Trial trial) {
        int old = count(trial);
        if (old >= trial.required) return false;
        tag.putInt(trial.key, old + 1);
        return old + 1 == trial.required;
    }

    public int completedTrials() {
        int completed = 0;
        for (Trial trial : Trial.values()) if (complete(trial)) completed++;
        return completed;
    }

    public int totalBossKills() {
        int total = 0;
        for (Trial trial : Trial.values()) total += count(trial);
        return total;
    }

    public double maxHealthBonus() {
        return totalBossKills() * 2.0 + completedTrials() * 10.0;
    }

    public double attackDamageBonus() {
        return totalBossKills() + completedTrials() * 5.0;
    }

    public double bossDamageBonus() {
        double bonus = count(Trial.ELDER_GUARDIAN) * 0.005;
        bonus += count(Trial.WITHER) * 0.01;
        bonus += count(Trial.ENDER_DRAGON) * 0.01;
        bonus += count(Trial.WARDEN) * 0.05;
        if (complete(Trial.ELDER_GUARDIAN)) bonus += 0.05;
        if (complete(Trial.WITHER)) bonus += 0.08;
        if (complete(Trial.ENDER_DRAGON)) bonus += 0.10;
        if (complete(Trial.WARDEN)) bonus += 0.20;
        return bonus;
    }

    public boolean fullyAwakened() {
        return completedTrials() == Trial.values().length;
    }

    public static void copy(ServerPlayer oldPlayer, ServerPlayer newPlayer) {
        newPlayer.getPersistentData().put(ROOT, oldPlayer.getPersistentData().getCompound(ROOT).copy());
    }
}
