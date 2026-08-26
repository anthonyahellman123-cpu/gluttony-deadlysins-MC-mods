package com.anthonyahellman.gluttony.block.entity;

import com.anthonyahellman.gluttony.data.GreedData;
import com.anthonyahellman.gluttony.gameplay.AbilityHudSync;
import com.anthonyahellman.gluttony.greed.AvariceAppraisals;
import com.anthonyahellman.gluttony.menu.GreedsVaultMenu;
import com.anthonyahellman.gluttony.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class GreedsVaultBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int COLUMNS = 9;
    public static final int ROWS = 5;
    public static final int SIZE = COLUMNS * ROWS;
    public static final int PRODUCTION_TICKS = 30 * 60 * 20;
    private static final double[] BASE_YIELDS = {0.20, 0.25, 0.30, 0.40, 0.50};

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private UUID owner;
    private int unlockedSlots = 1;
    private int productionTicks;
    private double pendingAvarice;

    public GreedsVaultBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GREEDS_VAULT.get(), pos, state);
    }

    public void setOwner(UUID owner) { this.owner = owner; setChanged(); }
    public void claimIfUnowned(ServerPlayer player) { if (owner == null) setOwner(player.getUUID()); }
    public boolean isOwner(ServerPlayer player) { return owner != null && owner.equals(player.getUUID()); }
    public int unlockedSlots() { return unlockedSlots; }
    public int productionTicks() { return productionTicks; }

    public static double slotPrice(int slot) {
        if (slot <= 0) return 0.0;
        int row = slot / COLUMNS;
        return switch (row) {
            case 0 -> 50.0;
            case 1 -> 100.0;
            case 2 -> 250.0;
            case 3 -> 500.0;
            default -> 1_000.0;
        };
    }

    public boolean unlockNext(ServerPlayer player) {
        if (!isOwner(player) || unlockedSlots >= SIZE) return false;
        if (!GreedData.of(player).spendAvarice(slotPrice(unlockedSlots))) return false;
        unlockedSlots++;
        setChanged();
        AbilityHudSync.send(player);
        return true;
    }

    public static double baseYield(int row) { return BASE_YIELDS[Math.max(0, Math.min(ROWS - 1, row))]; }

    public double diversificationBonus(int row) {
        Set<net.minecraft.resources.ResourceLocation> unique = new HashSet<>();
        int start = row * COLUMNS;
        for (int slot = start; slot < start + COLUMNS && slot < unlockedSlots; slot++) {
            ItemStack stack = items.get(slot);
            if (!stack.isEmpty()) unique.add(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        }
        int count = unique.size();
        if (count >= 9) return 0.10;
        if (count >= 7) return 0.075;
        if (count >= 5) return 0.05;
        if (count >= 3) return 0.025;
        return 0.0;
    }

    public double rowIncome(int row) {
        double effectiveYield = baseYield(row) + diversificationBonus(row);
        double income = 0.0;
        int start = row * COLUMNS;
        for (int slot = start; slot < start + COLUMNS && slot < unlockedSlots; slot++) {
            ItemStack stack = items.get(slot);
            income += AvariceAppraisals.stackValue(stack) * stackEfficiency(stack) * effectiveYield;
        }
        return income;
    }

    public double projectedIncome() {
        double total = 0.0;
        for (int row = 0; row < ROWS; row++) total += rowIncome(row);
        return total;
    }

    public double totalAppraisedAssets() {
        double total = 0.0;
        for (int slot = 0; slot < unlockedSlots; slot++) total += AvariceAppraisals.stackValue(items.get(slot));
        return total;
    }

    public static double stackEfficiency(ItemStack stack) {
        if (stack.isEmpty()) return 0.0;
        if (stack.getCount() >= stack.getMaxStackSize()) return 1.0;
        if (stack.getMaxStackSize() != 64) return 0.0;
        return Math.min(1.0, Math.ceil(stack.getCount() / 8.0) * 0.125);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GreedsVaultBlockEntity vault) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        vault.flushPayout(serverLevel);
        vault.productionTicks++;
        if (vault.productionTicks >= PRODUCTION_TICKS) {
            vault.productionTicks = 0;
            vault.pendingAvarice += vault.projectedIncome();
            vault.flushPayout(serverLevel);
        }
        if (vault.productionTicks % 20 == 0) vault.setChanged();
    }

    private void flushPayout(ServerLevel level) {
        if (owner == null || pendingAvarice <= 0.0) return;
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
        if (player == null) return;
        GreedData.of(player).addAvarice(pendingAvarice, GreedData.IncomeSource.VAULT);
        pendingAvarice = 0.0;
        AbilityHudSync.send(player);
        setChanged();
    }

    public boolean forceProduction(ServerPlayer player) {
        if (!isOwner(player)) return false;
        pendingAvarice += projectedIncome();
        productionTicks = 0;
        flushPayout(player.serverLevel());
        setChanged();
        return true;
    }

    public void dropContents(Level level, BlockPos pos) { Containers.dropContents(level, pos, this); clearContent(); }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putInt("UnlockedSlots", unlockedSlots);
        tag.putInt("ProductionTicks", productionTicks);
        tag.putDouble("PendingAvarice", pendingAvarice);
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        unlockedSlots = Math.max(1, Math.min(SIZE, tag.getInt("UnlockedSlots")));
        productionTicks = Math.max(0, Math.min(PRODUCTION_TICKS - 1, tag.getInt("ProductionTicks")));
        pendingAvarice = Math.max(0.0, tag.getDouble("PendingAvarice"));
    }

    @Override public Component getDisplayName() {
        return Component.translatable("container.demonsbountygluttony.greeds_vault");
    }

    @Override public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new GreedsVaultMenu(id, inventory, this, new ContainerData() {
            @Override public int get(int index) { return index == 0 ? unlockedSlots : productionTicks; }
            @Override public void set(int index, int value) {
                if (index == 0) unlockedSlots = Math.max(1, Math.min(SIZE, value));
                if (index == 1) productionTicks = Math.max(0, Math.min(PRODUCTION_TICKS - 1, value));
            }
            @Override public int getCount() { return 2; }
        });
    }

    @Override public int getContainerSize() { return SIZE; }
    @Override public boolean isEmpty() { for (ItemStack stack : items) if (!stack.isEmpty()) return false; return true; }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) { ItemStack result = ContainerHelper.removeItem(items, slot, amount); if (!result.isEmpty()) setChanged(); return result; }
    @Override public ItemStack removeItemNoUpdate(int slot) { ItemStack result = ContainerHelper.takeItem(items, slot); if (!result.isEmpty()) setChanged(); return result; }
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize()); setChanged(); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { items.clear(); setChanged(); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= unlockedSlots) return false;
        int tier = AvariceAppraisals.tier(stack).level();
        return tier > 0 && tier <= slot / COLUMNS + 1;
    }
}
