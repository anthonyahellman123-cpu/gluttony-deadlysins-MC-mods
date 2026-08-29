package com.anthonyahellman.gluttony.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class GluttonyData {
    private static final String ROOT = "DemonsBountyGluttony";
    private static final String ACTIVE = "Active";
    private static final String AWAKENING = "Awakening";
    private static final String CURRENT_SOULS = "CurrentSouls";
    private static final String LIFETIME_SOULS = "LifetimeSouls";
    private static final String LEVEL = "Level";
    private static final String HEALTH = "ExtractedHealth";
    private static final String ATTACK = "ExtractedAttack";
    private static final String SELECTED_ABILITY = "SelectedAbility";
    private static final String SIPHON_TARGET_MODE = "SoulSiphonTargetMode";
    private static final String DEVOUR_TARGET_MODE = "DevourTargetMode";
    private static final String BEELZEBUB_TARGET_MODE = "BeelzebubTargetMode";

    public enum Ability {
        SOUL_SIPHON(10), DEVOUR(50), BEELZEBUB(100);
        private final int unlockLevel;
        Ability(int unlockLevel) { this.unlockLevel = unlockLevel; }
        public int unlockLevel() { return unlockLevel; }
    }

    public enum TargetMode {
        MOBS, BOTH, PLAYERS;

        public boolean allows(LivingEntity target) {
            boolean player = target instanceof Player;
            return this == BOTH || (this == PLAYERS ? player : !player);
        }
    }

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
    public Ability selectedAbility() {
        int stored = tag.getInt(SELECTED_ABILITY);
        Ability[] values = Ability.values();
        Ability selected = stored >= 0 && stored < values.length ? values[stored] : Ability.SOUL_SIPHON;
        return level() >= selected.unlockLevel ? selected : Ability.SOUL_SIPHON;
    }
    public boolean selectAbility(Ability ability) {
        if (ability == null || level() < ability.unlockLevel) return false;
        tag.putInt(SELECTED_ABILITY, ability.ordinal());
        return true;
    }
    public TargetMode targetMode(Ability ability) {
        String key = targetModeKey(ability);
        int stored = tag.getInt(key);
        TargetMode[] values = TargetMode.values();
        return stored >= 0 && stored < values.length ? values[stored] : TargetMode.MOBS;
    }
    public void setTargetMode(Ability ability, TargetMode mode) {
        if (ability != null && mode != null) tag.putInt(targetModeKey(ability), mode.ordinal());
    }
    public boolean allowsTarget(Ability ability, LivingEntity target) {
        return target != null && targetMode(ability).allows(target);
    }
    private static String targetModeKey(Ability ability) {
        return switch (ability) {
            case SOUL_SIPHON -> SIPHON_TARGET_MODE;
            case DEVOUR -> DEVOUR_TARGET_MODE;
            case BEELZEBUB -> BEELZEBUB_TARGET_MODE;
        };
    }

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

    public double sacrificeExtractedHealth(double amount) {
        double spent = Math.min(extractedHealth(), Math.max(0.0, amount));
        tag.putDouble(HEALTH, extractedHealth() - spent);
        return spent;
    }

    public double sacrificeExtractedAttack(double amount) {
        double spent = Math.min(extractedAttack(), Math.max(0.0, amount));
        tag.putDouble(ATTACK, extractedAttack() - spent);
        return spent;
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
