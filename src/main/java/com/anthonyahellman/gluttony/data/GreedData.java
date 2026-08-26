package com.anthonyahellman.gluttony.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
    private static final String MARKET_ACTIVITY = "MarketActivity";
    private static final String CONTRACT_CLAIMS = "ContractClaims";
    private static final String POUCH = "Pouch";
    private static final String MARKET_STOCK = "MarketStock";
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
        MARKET
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
        tag.putDouble(AVARICE, avarice() + amount);
        tag.putDouble(LIFETIME_EARNED, lifetimeEarned() + amount);
        if (source == IncomeSource.VAULT) tag.putDouble(VAULT_INCOME, vaultIncome() + amount);
        if (source == IncomeSource.COFFER) tag.putDouble(COFFER_INCOME, cofferIncome() + amount);
        if (source == IncomeSource.MARKET) tag.putLong(MARKET_ACTIVITY, marketActivity() + 1L);
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
    public long marketActivity() { return Math.max(0L, tag.getLong(MARKET_ACTIVITY)); }
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

    public void addMarketStock(ItemStack stack) {
        if (stack.isEmpty()) return;
        ListTag stock = tag.getList(MARKET_STOCK, Tag.TAG_COMPOUND);
        ItemStack remaining = stack.copy();
        for (int index = 0; index < stock.size() && !remaining.isEmpty(); index++) {
            CompoundTag entry = stock.getCompound(index);
            ItemStack existing = ItemStack.of(entry);
            if (!existing.isEmpty() && ItemStack.isSameItemSameTags(existing, remaining)) {
                int moved = Math.min(remaining.getCount(), existing.getMaxStackSize() - existing.getCount());
                if (moved > 0) {
                    existing.grow(moved);
                    remaining.shrink(moved);
                    stock.set(index, existing.save(new CompoundTag()));
                }
            }
        }
        while (!remaining.isEmpty()) {
            int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
            ItemStack stored = remaining.copy();
            stored.setCount(moved);
            stock.add(stored.save(new CompoundTag()));
            remaining.shrink(moved);
        }
        tag.put(MARKET_STOCK, stock);
    }

    public int marketStockStacks() {
        return tag.getList(MARKET_STOCK, Tag.TAG_COMPOUND).size();
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

    private int nonNegativeInt(String key) { return Math.max(0, tag.getInt(key)); }
    private int cappedLevel(String key, int cap) { return Math.min(cap, nonNegativeInt(key)); }
}
