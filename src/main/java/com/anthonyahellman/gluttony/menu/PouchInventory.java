package com.anthonyahellman.gluttony.menu;

import com.anthonyahellman.gluttony.data.GreedData;
import com.anthonyahellman.gluttony.data.SinData;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class PouchInventory implements Container {
    private final ServerPlayer owner;
    private final GreedData greed;
    private final NonNullList<ItemStack> items;

    public PouchInventory(ServerPlayer owner) {
        this.owner = owner;
        this.greed = GreedData.of(owner);
        this.items = greed.loadPouchItems();
    }

    public GreedData greed() {
        return greed;
    }

    @Override public int getContainerSize() { return GreedData.POUCH_SIZE; }

    @Override public boolean isEmpty() {
        for (ItemStack stack : items) if (!stack.isEmpty()) return false;
        return true;
    }

    @Override public ItemStack getItem(int slot) { return items.get(slot); }

    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = ContainerHelper.takeItem(items, slot);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
    }

    @Override public void setChanged() { greed.savePouchItems(items); }

    @Override public boolean stillValid(Player player) {
        return player == owner && owner.isAlive()
                && SinData.selected(owner) == SinData.NaturalSin.GREED;
    }

    @Override public void clearContent() {
        items.clear();
        setChanged();
    }
}
