package com.anthonyahellman.gluttony.block.entity;

import com.anthonyahellman.gluttony.data.GreedData;
import com.anthonyahellman.gluttony.gameplay.AbilityHudSync;
import com.anthonyahellman.gluttony.greed.AvariceAppraisals;
import com.anthonyahellman.gluttony.menu.CofferOfAvariceMenu;
import com.anthonyahellman.gluttony.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Direction;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

import java.util.UUID;

public final class CofferOfAvariceBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int COLUMNS = 10;
    public static final int ROWS = 20;
    public static final int SIZE = COLUMNS * ROWS;
    public static final int PROCESS_TICKS = 5 * 20;
    public static final double PAYOUT_RATE = 0.10;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private UUID owner;
    private int processTicks;
    private double pendingAvarice;
    private LazyOptional<IItemHandler> inputHandler = createInputHandler();

    public CofferOfAvariceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COFFER_OF_AVARICE.get(), pos, state);
    }

    private LazyOptional<IItemHandler> createInputHandler() {
        return LazyOptional.of(() -> new InvWrapper(this) {
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }
        });
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public void claimIfUnowned(ServerPlayer player) {
        if (owner == null) setOwner(player.getUUID());
        flushPayout(player.serverLevel());
    }

    public int processTicks() {
        return processTicks;
    }

    public double pendingAvarice() {
        return pendingAvarice;
    }

    public double nextPayout() {
        double appraised = 0.0;
        for (int slot = 0; slot < COLUMNS; slot++) {
            appraised += AvariceAppraisals.stackValue(items.get(slot));
        }
        return appraised * PAYOUT_RATE;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  CofferOfAvariceBlockEntity coffer) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        coffer.flushPayout(serverLevel);
        if (coffer.isEmpty()) {
            if (coffer.processTicks != 0) {
                coffer.processTicks = 0;
                coffer.setChanged();
            }
            return;
        }

        coffer.processTicks++;
        if (coffer.processTicks >= PROCESS_TICKS) {
            coffer.processTicks = 0;
            coffer.consumeFrontRow();
            coffer.flushPayout(serverLevel);
        }
        coffer.setChanged();
    }

    private void consumeFrontRow() {
        double appraised = 0.0;
        for (int slot = 0; slot < COLUMNS; slot++) {
            appraised += AvariceAppraisals.stackValue(items.get(slot));
        }
        for (int slot = 0; slot < SIZE - COLUMNS; slot++) {
            items.set(slot, items.get(slot + COLUMNS));
        }
        for (int slot = SIZE - COLUMNS; slot < SIZE; slot++) {
            items.set(slot, ItemStack.EMPTY);
        }
        pendingAvarice += appraised * PAYOUT_RATE;
        setChanged();
    }

    private void flushPayout(ServerLevel level) {
        if (owner == null || pendingAvarice <= 0.0) return;
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
        if (player == null) return;
        GreedData.of(player).addAvarice(pendingAvarice);
        pendingAvarice = 0.0;
        AbilityHudSync.send(player);
        setChanged();
    }

    public void dropContents(Level level, BlockPos pos) {
        Containers.dropContents(level, pos, this);
        clearContent();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) return inputHandler.cast();
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        inputHandler.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        inputHandler = createInputHandler();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putInt("ProcessTicks", processTicks);
        tag.putDouble("PendingAvarice", pendingAvarice);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        processTicks = Math.max(0, Math.min(PROCESS_TICKS - 1, tag.getInt("ProcessTicks")));
        pendingAvarice = Math.max(0.0, tag.getDouble("PendingAvarice"));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.demonsbountygluttony.coffer_of_avarice");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new CofferOfAvariceMenu(id, inventory, this, new ContainerData() {
            @Override
            public int get(int index) {
                if (index == 0) return processTicks;
                if (index == 1) return (int) Math.min(Integer.MAX_VALUE, Math.round(nextPayout() * 100.0));
                return 0;
            }

            @Override public void set(int index, int value) {
                if (index == 0) processTicks = Math.max(0, Math.min(PROCESS_TICKS - 1, value));
            }
            @Override public int getCount() { return 2; }
        });
    }

    @Override public int getContainerSize() { return SIZE; }
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
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { items.clear(); setChanged(); }
}
