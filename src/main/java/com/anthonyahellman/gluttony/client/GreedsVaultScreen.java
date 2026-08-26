package com.anthonyahellman.gluttony.client;

import com.anthonyahellman.gluttony.block.entity.GreedsVaultBlockEntity;
import com.anthonyahellman.gluttony.greed.AvariceAppraisals;
import com.anthonyahellman.gluttony.menu.GreedsVaultMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

public final class GreedsVaultScreen extends AbstractContainerScreen<GreedsVaultMenu> {
    private static final int GOLD = 0xFFD8B642;
    private static final int EMERALD = 0xFF4ED486;
    private static final int PANEL = 0xF3120E0B;
    private Button unlockButton;

    public GreedsVaultScreen(GreedsVaultMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 430;
        imageHeight = 222;
        inventoryLabelY = -1000;
        titleLabelY = -1000;
    }

    @Override protected void init() {
        super.init();
        unlockButton = addRenderableWidget(Button.builder(Component.literal("UNLOCK NEXT SLOT"), button -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, GreedsVaultMenu.UNLOCK_BUTTON);
            }
        }).bounds(leftPos + 275, topPos + 185, 145, 20).build());
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;
        graphics.fill(left, top, left + imageWidth, top + imageHeight, 0xF0060504);
        frame(graphics, left, top, imageWidth, imageHeight, GOLD);
        graphics.fill(left + 3, top + 3, left + imageWidth - 3, top + 22, 0xFF2B160D);
        graphics.drawString(font, "GREED'S VAULT", left + 10, top + 8, 0xFFFFE18A, false);
        graphics.drawString(font, "Items remain invested and are never consumed", left + 103, top + 8,
                0xFF9ACCA8, false);

        for (int row = 0; row < GreedsVaultBlockEntity.ROWS; row++) {
            int y = top + 28 + row * 18;
            for (int column = 0; column < GreedsVaultBlockEntity.COLUMNS; column++) {
                int slot = column + row * GreedsVaultBlockEntity.COLUMNS;
                int x = left + 9 + column * 18;
                boolean unlocked = slot < menu.unlockedSlots();
                drawSlot(graphics, x, y, unlocked ? GOLD : 0xFF5C4033, unlocked);
                if (!unlocked) {
                    String price = compact(GreedsVaultBlockEntity.slotPrice(slot));
                    graphics.drawString(font, price, x + 9 - font.width(price) / 2, y + 5,
                            0xFFA97A56, false);
                }
            }
            int infoX = left + 182;
            double bonus = diversification(row);
            double effective = GreedsVaultBlockEntity.baseYield(row) + bonus;
            graphics.drawString(font, "ROW " + (row + 1) + "  T1-T" + (row + 1), infoX, y + 1,
                    0xFFFFD56A, false);
            graphics.drawString(font, pct(GreedsVaultBlockEntity.baseYield(row)) + " + " + pct(bonus)
                    + " = " + pct(effective), infoX + 78, y + 1, 0xFFB9A683, false);
            graphics.drawString(font, "+" + format(rowIncome(row)) + " Ava / 30m", infoX + 205, y + 1,
                    EMERALD, false);
        }

        graphics.drawString(font, "YOUR INVENTORY", left + 10, top + 130, 0xFFB7A78F, false);
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            drawSlot(graphics, left + 9 + column * 18, top + 142 + row * 18, 0xFF68573C, true);
        for (int column = 0; column < 9; column++)
            drawSlot(graphics, left + 9 + column * 18, top + 200, 0xFF68573C, true);

        double baseProjected = projectedIncome();
        double received = baseProjected * (1.0 + GreedClientState.get().premiumYield() * 0.05);
        graphics.drawString(font, "TOTAL APPRAISED ASSETS: " + format(totalAssets()), left + 182, top + 130,
                0xFFFFD56A, false);
        graphics.drawString(font, "PROJECTED: +" + format(received) + " Ava / 30 min", left + 182, top + 146,
                EMERALD, false);
        graphics.drawString(font, "NEXT PAYOUT: " + time(menu.secondsRemaining()), left + 182, top + 162,
                0xFFD8C7A2, false);
        String unlockText = menu.unlockedSlots() >= GreedsVaultBlockEntity.SIZE ? "ALL 45 SLOTS UNLOCKED"
                : "NEXT SLOT: " + format(menu.nextUnlockPrice()) + " Ava";
        graphics.drawString(font, unlockText, left + 182, top + 178, 0xFFFFC761, false);
        unlockButton.visible = menu.unlockedSlots() < GreedsVaultBlockEntity.SIZE;
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}

    private double diversification(int row) {
        Set<net.minecraft.world.item.Item> unique = new HashSet<>();
        int start = row * GreedsVaultBlockEntity.COLUMNS;
        for (int slot = start; slot < start + GreedsVaultBlockEntity.COLUMNS && slot < menu.unlockedSlots(); slot++) {
            ItemStack stack = menu.vault().getItem(slot);
            if (!stack.isEmpty()) unique.add(stack.getItem());
        }
        int count = unique.size();
        return count >= 9 ? 0.10 : count >= 7 ? 0.075 : count >= 5 ? 0.05 : count >= 3 ? 0.025 : 0.0;
    }

    private double rowIncome(int row) {
        double yield = GreedsVaultBlockEntity.baseYield(row) + diversification(row);
        double total = 0.0;
        int start = row * GreedsVaultBlockEntity.COLUMNS;
        for (int slot = start; slot < start + GreedsVaultBlockEntity.COLUMNS && slot < menu.unlockedSlots(); slot++) {
            ItemStack stack = menu.vault().getItem(slot);
            total += AvariceAppraisals.stackValue(stack) * GreedsVaultBlockEntity.stackEfficiency(stack) * yield;
        }
        return total;
    }

    private double projectedIncome() { double total = 0.0; for (int row = 0; row < 5; row++) total += rowIncome(row); return total; }
    private double totalAssets() { double total = 0.0; for (int i = 0; i < menu.unlockedSlots(); i++) total += AvariceAppraisals.stackValue(menu.vault().getItem(i)); return total; }
    private static String pct(double value) { return String.format("%.1f%%", value * 100.0); }
    private static String compact(double value) { return value >= 1_000 ? "1k" : String.format("%.0f", value); }
    private static String format(double value) { return Math.abs(value - Math.rint(value)) < 0.0001 ? String.format("%,.0f", value) : String.format("%,.2f", value); }
    private static String time(double seconds) { int value = Math.max(0, (int) Math.ceil(seconds)); return String.format("%02d:%02d", value / 60, value % 60); }

    private static void drawSlot(GuiGraphics graphics, int x, int y, int border, boolean unlocked) {
        graphics.fill(x, y, x + 18, y + 18, border);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, unlocked ? 0xFF20160F : 0xFF130D0A);
    }
    private static void frame(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color); graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color); graphics.fill(x + width - 1, y, x + width, y + height, color);
    }
}
