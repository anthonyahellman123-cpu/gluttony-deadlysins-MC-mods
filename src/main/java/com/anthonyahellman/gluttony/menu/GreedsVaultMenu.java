package com.anthonyahellman.gluttony.menu;

import com.anthonyahellman.gluttony.block.entity.GreedsVaultBlockEntity;
import com.anthonyahellman.gluttony.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class GreedsVaultMenu extends AbstractContainerMenu {
    public static final int UNLOCK_BUTTON = 0;
    private final Container vault;
    private final ContainerData data;

    public GreedsVaultMenu(int id, Inventory inventory, FriendlyByteBuf ignored) {
        this(id, inventory, new SimpleContainer(GreedsVaultBlockEntity.SIZE), new SimpleContainerData(2));
    }

    public GreedsVaultMenu(int id, Inventory inventory, Container vault, ContainerData data) {
        super(ModMenus.GREEDS_VAULT.get(), id);
        checkContainerSize(vault, GreedsVaultBlockEntity.SIZE);
        checkContainerDataCount(data, 2);
        this.vault = vault;
        this.data = data;
        addDataSlots(data);
        vault.startOpen(inventory.player);

        for (int row = 0; row < GreedsVaultBlockEntity.ROWS; row++) {
            for (int column = 0; column < GreedsVaultBlockEntity.COLUMNS; column++) {
                int index = column + row * GreedsVaultBlockEntity.COLUMNS;
                addSlot(new VaultSlot(vault, index, 10 + column * 18, 29 + row * 18));
            }
        }

        int playerY = 143;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 10 + column * 18, playerY + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 10 + column * 18, playerY + 58));
    }

    public int unlockedSlots() { return data.get(0); }
    public int productionTicks() { return data.get(1); }
    public double secondsRemaining() {
        return Math.max(0, GreedsVaultBlockEntity.PRODUCTION_TICKS - productionTicks()) / 20.0;
    }
    public double nextUnlockPrice() {
        return unlockedSlots() >= GreedsVaultBlockEntity.SIZE ? 0.0 : GreedsVaultBlockEntity.slotPrice(unlockedSlots());
    }
    public Container vault() { return vault; }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (id != UNLOCK_BUTTON || !(player instanceof ServerPlayer serverPlayer)
                || !(vault instanceof GreedsVaultBlockEntity blockEntity)) return false;
        if (!blockEntity.unlockNext(serverPlayer)) {
            serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "Vault expansion blocked: insufficient Avarice or every slot is already unlocked."), true);
        }
        return true;
    }

    @Override public boolean stillValid(Player player) { return vault.stillValid(player); }
    @Override public void removed(Player player) { super.removed(player); vault.stopOpen(player); }

    @Override public ItemStack quickMoveStack(Player player, int menuIndex) {
        Slot slot = slots.get(menuIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (menuIndex < GreedsVaultBlockEntity.SIZE) {
            if (!moveItemStackTo(original, GreedsVaultBlockEntity.SIZE, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveIntoVault(original)) return ItemStack.EMPTY;
        if (original.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        if (original.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, original);
        return copy;
    }

    private boolean moveIntoVault(ItemStack stack) {
        boolean moved = false;
        for (int slot = 0; slot < unlockedSlots() && !stack.isEmpty(); slot++) {
            ItemStack existing = vault.getItem(slot);
            if (!existing.isEmpty() && ItemStack.isSameItemSameTags(existing, stack)
                    && vault.canPlaceItem(slot, stack)) {
                int amount = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
                if (amount > 0) { existing.grow(amount); stack.shrink(amount); vault.setChanged(); moved = true; }
            }
        }
        for (int slot = 0; slot < unlockedSlots() && !stack.isEmpty(); slot++) {
            if (vault.getItem(slot).isEmpty() && vault.canPlaceItem(slot, stack)) {
                int amount = Math.min(stack.getCount(), stack.getMaxStackSize());
                ItemStack inserted = stack.copy(); inserted.setCount(amount);
                vault.setItem(slot, inserted); stack.shrink(amount); moved = true;
            }
        }
        return moved;
    }

    private final class VaultSlot extends Slot {
        private VaultSlot(Container container, int index, int x, int y) { super(container, index, x, y); }
        @Override public boolean mayPlace(ItemStack stack) {
            return getContainerSlot() < unlockedSlots() && vault.canPlaceItem(getContainerSlot(), stack);
        }
        @Override public boolean mayPickup(Player player) { return getContainerSlot() < unlockedSlots(); }
    }
}
