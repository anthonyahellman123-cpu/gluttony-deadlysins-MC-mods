package com.anthonyahellman.gluttony.menu;

import com.anthonyahellman.gluttony.block.entity.CofferOfAvariceBlockEntity;
import com.anthonyahellman.gluttony.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public final class CofferOfAvariceMenu extends AbstractContainerMenu {
    public static final int VISIBLE_ROWS = 6;
    public static final int VISIBLE_SLOTS = CofferOfAvariceBlockEntity.COLUMNS * VISIBLE_ROWS;
    public static final int MAX_SCROLL_ROW = CofferOfAvariceBlockEntity.ROWS - VISIBLE_ROWS;

    private final Container coffer;
    private final ContainerData data;
    private int scrollRow;

    public CofferOfAvariceMenu(int id, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(id, playerInventory, new SimpleContainer(CofferOfAvariceBlockEntity.SIZE),
                new SimpleContainerData(3));
    }

    public CofferOfAvariceMenu(int id, Inventory playerInventory, Container coffer, ContainerData data) {
        super(ModMenus.COFFER_OF_AVARICE.get(), id);
        checkContainerSize(coffer, CofferOfAvariceBlockEntity.SIZE);
        this.coffer = coffer;
        this.data = data;
        checkContainerDataCount(data, 3);
        addDataSlots(data);
        coffer.startOpen(playerInventory.player);

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            for (int column = 0; column < CofferOfAvariceBlockEntity.COLUMNS; column++) {
                int visibleIndex = row * CofferOfAvariceBlockEntity.COLUMNS + column;
                addSlot(new ScrollingSlot(coffer, visibleIndex, 8 + column * 18, 18 + row * 18));
            }
        }

        int playerX = 17;
        int playerY = 140;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9,
                        playerX + column * 18, playerY + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, playerX + column * 18, playerY + 58));
        }
    }

    public int scrollRow() {
        return scrollRow;
    }

    public double secondsRemaining() {
        return Math.max(0, CofferOfAvariceBlockEntity.PROCESS_TICKS - data.get(0)) / 20.0;
    }

    public double nextPayout() {
        return data.get(1) / 100.0;
    }

    public boolean blockedByUnappraisedItem() {
        return data.get(2) != 0;
    }

    public void scrollTo(int row) {
        scrollRow = Math.max(0, Math.min(MAX_SCROLL_ROW, row));
        broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id > MAX_SCROLL_ROW) return false;
        scrollTo(id);
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return coffer.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        coffer.stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int menuIndex) {
        Slot slot = slots.get(menuIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();

        if (menuIndex < VISIBLE_SLOTS) {
            if (!moveItemStackTo(original, VISIBLE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!insertIntoCoffer(original)) {
            return ItemStack.EMPTY;
        }

        if (original.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        if (original.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, original);
        return copy;
    }

    private boolean insertIntoCoffer(ItemStack stack) {
        boolean moved = false;
        for (int slot = 0; slot < coffer.getContainerSize() && !stack.isEmpty(); slot++) {
            ItemStack existing = coffer.getItem(slot);
            if (!existing.isEmpty() && ItemStack.isSameItemSameTags(existing, stack)) {
                int amount = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
                if (amount > 0) {
                    existing.grow(amount);
                    stack.shrink(amount);
                    coffer.setChanged();
                    moved = true;
                }
            }
        }
        for (int slot = 0; slot < coffer.getContainerSize() && !stack.isEmpty(); slot++) {
            if (coffer.getItem(slot).isEmpty() && coffer.canPlaceItem(slot, stack)) {
                int amount = Math.min(stack.getCount(), stack.getMaxStackSize());
                ItemStack inserted = stack.copy();
                inserted.setCount(amount);
                coffer.setItem(slot, inserted);
                stack.shrink(amount);
                moved = true;
            }
        }
        return moved;
    }

    private final class ScrollingSlot extends Slot {
        private final int visibleIndex;

        private ScrollingSlot(Container container, int visibleIndex, int x, int y) {
            super(container, 0, x, y);
            this.visibleIndex = visibleIndex;
        }

        private int backingIndex() {
            return scrollRow * CofferOfAvariceBlockEntity.COLUMNS + visibleIndex;
        }

        @Override public ItemStack getItem() { return coffer.getItem(backingIndex()); }
        @Override public boolean hasItem() { return !getItem().isEmpty(); }
        @Override public void set(ItemStack stack) { coffer.setItem(backingIndex(), stack); setChanged(); }
        @Override public void setChanged() { coffer.setChanged(); }
        @Override public ItemStack remove(int amount) { return coffer.removeItem(backingIndex(), amount); }
        @Override public boolean mayPlace(ItemStack stack) { return coffer.canPlaceItem(backingIndex(), stack); }
        @Override public boolean mayPickup(Player player) { return true; }
    }
}
