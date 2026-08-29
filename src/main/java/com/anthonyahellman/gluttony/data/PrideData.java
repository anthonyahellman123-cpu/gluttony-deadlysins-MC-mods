package com.anthonyahellman.gluttony.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public final class PrideData {
    private static final String ROOT = "RootsOfSinPride";
    private static final String TOTAL_CONQUESTS = "TotalConquests";

    public enum Trial {
        ENDER_DRAGON("dragon", "Ender Dragons", "EnderDragons", 12),
        WITHER("wither", "Withers", "Withers", 8),
        ELDER_GUARDIAN("elder_guardian", "Elder Guardians", "ElderGuardians", 4),
        WARDEN("warden", "Wardens", "Wardens", 2);

        private final String commandName;
        private final String displayName;
        private final String key;
        private final int required;

        Trial(String commandName, String displayName, String key, int required) {
            this.commandName = commandName;
            this.displayName = displayName;
            this.key = key;
            this.required = required;
        }

        public String commandName() { return commandName; }
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
        tag.putLong(TOTAL_CONQUESTS, totalConquests() + 1L);
        if (old >= trial.required) return false;
        tag.putInt(trial.key, old + 1);
        return old + 1 == trial.required;
    }

    public void setCount(Trial trial, int count) {
        int old = count(trial);
        int updated = Math.min(trial.required, Math.max(0, count));
        long total = totalConquests();
        tag.putInt(trial.key, updated);
        tag.putLong(TOTAL_CONQUESTS, Math.max(0L, total + updated - old));
    }

    public void reset() {
        for (Trial trial : Trial.values()) tag.remove(trial.key);
        tag.remove(TOTAL_CONQUESTS);
    }

    public int completedTrials() {
        int completed = 0;
        for (Trial trial : Trial.values()) if (complete(trial)) completed++;
        return completed;
    }

    public int totalBossKills() {
        return (int)Math.min(Integer.MAX_VALUE, totalConquests());
    }

    public long totalConquests() {
        if (!tag.contains(TOTAL_CONQUESTS)) {
            long migrated = 0L;
            for (Trial trial : Trial.values()) migrated += count(trial);
            tag.putLong(TOTAL_CONQUESTS, migrated);
        }
        return Math.max(0L, tag.getLong(TOTAL_CONQUESTS));
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
