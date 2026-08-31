package com.anthonyahellman.gluttony.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

public final class GreedData {
    private static final String ROOT = "RootsOfSinGreed";
    private static final String AVARICE = "Avarice";
    private static final String LIFETIME_EARNED = "LifetimeEarned";
    private static final String LIFETIME_SPENT = "LifetimeSpent";
    private static final String ASSETS_DIVESTED = "AssetsDivested";
    private static final String VAULT_INCOME = "VaultIncome";
    private static final String COFFER_INCOME = "CofferIncome";
    private static final String CONTRACT_CLAIMS = "ContractClaims";
    private static final String POUCH = "Pouch";
    private static final String CORE_HEALTH = "CoreHealthPurchases";
    private static final String CORE_ATTACK = "CoreAttackPurchases";
    private static final String CORE_ARMOR = "CoreArmorPurchases";
    private static final String PREMIUM_MOVEMENT = "PremiumMovement";
    private static final String PREMIUM_ATTACK_SPEED = "PremiumAttackSpeed";
    private static final String PREMIUM_LUCK = "PremiumLuck";
    private static final String PREMIUM_KNOCKBACK = "PremiumKnockbackResistance";
    private static final String PREMIUM_YIELD = "PremiumAvariceYield";
    private static final String PINNACLE_COMPOUND = "PinnacleCompoundInterest";
    private static final String PINNACLE_APPRECIATION = "PinnacleAssetAppreciation";
    private static final String PINNACLE_CONTRACT = "PinnacleContractOfMammon";
    private static final String CLAIMS_IN_WINDOW = "ClaimsInWindow";
    private static final String CLAIM_RESET_AT = "ClaimResetAt";

    public static final int POUCH_SIZE = 9;

    public enum IncomeSource {
        OTHER,
        DIVESTMENT,
        VAULT,
        COFFER,
    }

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
        addAvarice(amount, IncomeSource.OTHER);
    }

    public void addAvarice(double amount, IncomeSource source) {
        if (amount <= 0.0) return;
        double received = source == IncomeSource.OTHER
                ? amount : amount * (1.0 + premiumAvariceYield() * 0.05);
        tag.putDouble(AVARICE, avarice() + received);
        tag.putDouble(LIFETIME_EARNED, lifetimeEarned() + received);
        if (source == IncomeSource.VAULT) tag.putDouble(VAULT_INCOME, vaultIncome() + received);
        if (source == IncomeSource.COFFER) tag.putDouble(COFFER_INCOME, cofferIncome() + received);
    }

    public boolean spendAvarice(double amount) {
        if (amount < 0.0 || avarice() < amount) return false;
        tag.putDouble(AVARICE, avarice() - amount);
        if (amount > 0.0) tag.putDouble(LIFETIME_SPENT, lifetimeSpent() + amount);
        return true;
    }

    public double lifetimeEarned() { return Math.max(0.0, tag.getDouble(LIFETIME_EARNED)); }
    public double lifetimeSpent() { return Math.max(0.0, tag.getDouble(LIFETIME_SPENT)); }
    public long assetsDivested() { return Math.max(0L, tag.getLong(ASSETS_DIVESTED)); }
    public double vaultIncome() { return Math.max(0.0, tag.getDouble(VAULT_INCOME)); }
    public double cofferIncome() { return Math.max(0.0, tag.getDouble(COFFER_INCOME)); }
    public long contractClaims() { return Math.max(0L, tag.getLong(CONTRACT_CLAIMS)); }

    public void recordDivestedAssets(long count) {
        if (count > 0L) tag.putLong(ASSETS_DIVESTED, assetsDivested() + count);
    }

    public NonNullList<ItemStack> loadPouchItems() {
        NonNullList<ItemStack> items = NonNullList.withSize(POUCH_SIZE, ItemStack.EMPTY);
        if (tag.contains(POUCH)) ContainerHelper.loadAllItems(tag.getCompound(POUCH), items);
        return items;
    }

    public void savePouchItems(NonNullList<ItemStack> items) {
        CompoundTag pouch = new CompoundTag();
        ContainerHelper.saveAllItems(pouch, items);
        tag.put(POUCH, pouch);
    }

    public int coreHealthPurchases() { return nonNegativeInt(CORE_HEALTH); }
    public int coreAttackPurchases() { return nonNegativeInt(CORE_ATTACK); }
    public int coreArmorPurchases() { return nonNegativeInt(CORE_ARMOR); }
    public int premiumMovement() { return cappedLevel(PREMIUM_MOVEMENT, 10); }
    public int premiumAttackSpeed() { return cappedLevel(PREMIUM_ATTACK_SPEED, 10); }
    public int premiumLuck() { return cappedLevel(PREMIUM_LUCK, 10); }
    public int premiumKnockbackResistance() { return cappedLevel(PREMIUM_KNOCKBACK, 10); }
    public int premiumAvariceYield() { return cappedLevel(PREMIUM_YIELD, 10); }
    public int compoundInterestLevel() { return cappedLevel(PINNACLE_COMPOUND, 5); }
    public int assetAppreciationLevel() { return cappedLevel(PINNACLE_APPRECIATION, 5); }
    public int contractLevel() { return cappedLevel(PINNACLE_CONTRACT, 5); }
    public int claimsInWindow() { return nonNegativeInt(CLAIMS_IN_WINDOW); }
    public long claimResetAt() { return Math.max(0L, tag.getLong(CLAIM_RESET_AT)); }

    public double coreNextCost(int purchases) {
        int completedBands = Math.max(0, purchases) / 10;
        return 100.0 * Math.pow(2.0, completedBands);
    }

    public double currentClaimCost() {
        return 100_000.0 * Math.pow(2.0, Math.max(0, claimsInWindow()));
    }

    public boolean buyCoreHealth() { return buyCore(CORE_HEALTH, coreHealthPurchases()); }
    public boolean buyCoreAttack() { return buyCore(CORE_ATTACK, coreAttackPurchases()); }
    public boolean buyCoreArmor() { return buyCore(CORE_ARMOR, coreArmorPurchases()); }

    public boolean buyPremiumMovement() { return buyPremium(PREMIUM_MOVEMENT, premiumMovement()); }
    public boolean buyPremiumAttackSpeed() { return buyPremium(PREMIUM_ATTACK_SPEED, premiumAttackSpeed()); }
    public boolean buyPremiumLuck() { return buyPremium(PREMIUM_LUCK, premiumLuck()); }
    public boolean buyPremiumKnockback() { return buyPremium(PREMIUM_KNOCKBACK, premiumKnockbackResistance()); }
    public boolean buyPremiumYield() { return buyPremium(PREMIUM_YIELD, premiumAvariceYield()); }

    public boolean buyCompoundInterest() {
        return buyPinnacle(PINNACLE_COMPOUND, compoundInterestLevel(), pinnacleCost(compoundInterestLevel()));
    }

    public boolean buyAssetAppreciation() {
        return buyPinnacle(PINNACLE_APPRECIATION, assetAppreciationLevel(),
                pinnacleCost(assetAppreciationLevel()));
    }

    public boolean buyContract() {
        int level = contractLevel();
        if (level >= 5) return false;
        double cost = contractUpgradeCost(level);
        if (!spendAvarice(cost)) return false;
        tag.putInt(PINNACLE_CONTRACT, level + 1);
        return true;
    }

    public static double premiumCost(int currentLevel) {
        return 5_000.0 * Math.pow(1.5, Math.max(0, currentLevel) / 2);
    }

    public static double pinnacleCost(int currentLevel) {
        return 250_000.0 * Math.pow(2.0, Math.max(0, currentLevel));
    }

    public static double contractUpgradeCost(int currentLevel) {
        return 100_000.0 * Math.pow(1.5, Math.max(0, currentLevel));
    }

    public long contractWindowTicks() {
        return switch (contractLevel()) {
            case 1 -> 72_000L;
            case 2 -> 63_000L;
            case 3 -> 54_000L;
            case 4 -> 45_000L;
            case 5 -> 36_000L;
            default -> 0L;
        };
    }

    public void refreshClaimWindow(long gameTime) {
        if (claimsInWindow() > 0 && claimResetAt() > 0L && gameTime >= claimResetAt()) {
            tag.putInt(CLAIMS_IN_WINDOW, 0);
            tag.putLong(CLAIM_RESET_AT, 0L);
        }
    }

    public void recordContractClaim(long gameTime) {
        tag.putInt(CLAIMS_IN_WINDOW, claimsInWindow() + 1);
        tag.putLong(CONTRACT_CLAIMS, contractClaims() + 1L);
        tag.putLong(CLAIM_RESET_AT, gameTime + contractWindowTicks());
    }

    private boolean buyCore(String key, int purchases) {
        if (!spendAvarice(coreNextCost(purchases))) return false;
        tag.putInt(key, purchases + 1);
        return true;
    }

    private boolean buyPremium(String key, int level) {
        if (level >= 10 || !spendAvarice(premiumCost(level))) return false;
        tag.putInt(key, level + 1);
        return true;
    }

    private boolean buyPinnacle(String key, int level, double cost) {
        if (level >= 5 || !spendAvarice(cost)) return false;
        tag.putInt(key, level + 1);
        return true;
    }

    private int nonNegativeInt(String key) { return Math.max(0, tag.getInt(key)); }
    private int cappedLevel(String key, int cap) { return Math.min(cap, nonNegativeInt(key)); }
}
