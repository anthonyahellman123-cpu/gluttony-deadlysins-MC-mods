package com.anthonyahellman.gluttony.menu;

import com.anthonyahellman.gluttony.data.GreedData;
import com.anthonyahellman.gluttony.gameplay.AbilityHudSync;
import com.anthonyahellman.gluttony.gameplay.GreedProgression;
import com.anthonyahellman.gluttony.data.SinData;
import com.anthonyahellman.gluttony.greed.AvariceAppraisals;
import com.anthonyahellman.gluttony.registry.ModMenus;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class PouchOfMammonMenu extends AbstractContainerMenu {
    public static final int DIVEST_BUTTON = 0;
    public static final int MARKET_BUTTON = 1;
    public static final int CORE_HEALTH_BUTTON = 10;
    public static final int CORE_ATTACK_BUTTON = 11;
    public static final int CORE_ARMOR_BUTTON = 12;
    public static final int PREMIUM_MOVEMENT_BUTTON = 20;
    public static final int PREMIUM_ATTACK_SPEED_BUTTON = 21;
    public static final int PREMIUM_LUCK_BUTTON = 22;
    public static final int PREMIUM_UNSHAKABLE_BUTTON = 23;
    public static final int PREMIUM_YIELD_BUTTON = 24;
    public static final int COMPOUND_INTEREST_BUTTON = 30;
    public static final int ASSET_APPRECIATION_BUTTON = 31;
    public static final int CONTRACT_BUTTON = 32;
    public static final int POUCH_SLOTS = 9;

    private final Container pouch;

    public PouchOfMammonMenu(int id, Inventory inventory, FriendlyByteBuf ignored) {
        this(id, inventory, new SimpleContainer(POUCH_SLOTS));
    }

    public PouchOfMammonMenu(int id, Inventory inventory, Container pouch) {
        super(ModMenus.POUCH_OF_MAMMON.get(), id);
        checkContainerSize(pouch, POUCH_SLOTS);
        this.pouch = pouch;
        pouch.startOpen(inventory.player);

        int pouchX = 254;
        int pouchY = 25;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new Slot(pouch, column + row * 3, pouchX + column * 18, pouchY + row * 18));
            }
        }

        int playerX = 347;
        int playerY = 25;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        playerX + column * 18, playerY + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, playerX + column * 18, playerY + 62));
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == MARKET_BUTTON) {
            if (!player.level().isClientSide) {
                player.displayClientMessage(Component.literal(
                        "Market customer payments and interface are still TBD.")
                        .withStyle(ChatFormatting.GOLD), false);
            }
            return true;
        }
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(pouch instanceof PouchInventory inventory)) return false;
        if (SinData.selected(serverPlayer) != SinData.NaturalSin.GREED) return false;

        if (id >= CORE_HEALTH_BUTTON && id <= CONTRACT_BUTTON) {
            return purchase(serverPlayer, inventory.greed(), id);
        }
        if (id != DIVEST_BUTTON) return false;

        AvariceAppraisals.ensureDerived(serverPlayer.server);

        if (inventory.isEmpty()) {
            serverPlayer.displayClientMessage(Component.literal("The divestment grid is empty.")
                    .withStyle(ChatFormatting.GRAY), true);
            return true;
        }

        double total = 0.0;
        long count = 0L;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && AvariceAppraisals.value(stack) <= 0.0) {
                serverPlayer.displayClientMessage(Component.literal(
                        "Divestment blocked: remove every Unappraised asset.")
                        .withStyle(ChatFormatting.RED), true);
                return true;
            }
            total += AvariceAppraisals.stackValue(stack);
            count += stack.getCount();
        }

        GreedData greed = inventory.greed();
        inventory.clearContent();
        greed.recordDivestedAssets(count);
        greed.addAvarice(total, GreedData.IncomeSource.DIVESTMENT);
        AbilityHudSync.send(serverPlayer);
        serverPlayer.displayClientMessage(Component.literal(String.format(
                "DIVESTED %d ASSETS  +%.2f AVARICE", count, total))
                .withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    private boolean purchase(ServerPlayer player, GreedData greed, int id) {
        boolean purchased = switch (id) {
            case CORE_HEALTH_BUTTON -> greed.buyCoreHealth();
            case CORE_ATTACK_BUTTON -> greed.buyCoreAttack();
            case CORE_ARMOR_BUTTON -> greed.buyCoreArmor();
            case PREMIUM_MOVEMENT_BUTTON -> greed.buyPremiumMovement();
            case PREMIUM_ATTACK_SPEED_BUTTON -> greed.buyPremiumAttackSpeed();
            case PREMIUM_LUCK_BUTTON -> greed.buyPremiumLuck();
            case PREMIUM_UNSHAKABLE_BUTTON -> greed.buyPremiumKnockback();
            case PREMIUM_YIELD_BUTTON -> greed.buyPremiumYield();
            case COMPOUND_INTEREST_BUTTON -> greed.buyCompoundInterest();
            case ASSET_APPRECIATION_BUTTON -> greed.buyAssetAppreciation();
            case CONTRACT_BUTTON -> greed.buyContract();
            default -> false;
        };
        if (!purchased) {
            player.displayClientMessage(Component.literal("Purchase blocked: insufficient Avarice or maximum level reached.")
                    .withStyle(ChatFormatting.RED), true);
            return true;
        }
        GreedProgression.applyAttributes(player);
        AbilityHudSync.send(player);
        player.displayClientMessage(Component.literal("INVESTMENT ACQUIRED")
                .withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    @Override public boolean stillValid(Player player) { return pouch.stillValid(player); }

    @Override
    public void removed(Player player) {
        super.removed(player);
        pouch.stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (index < POUCH_SLOTS) {
            if (!moveItemStackTo(original, POUCH_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(original, 0, POUCH_SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (original.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        if (original.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, original);
        return copy;
    }
}
