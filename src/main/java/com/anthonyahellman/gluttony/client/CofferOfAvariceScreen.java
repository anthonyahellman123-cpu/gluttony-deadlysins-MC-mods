package com.anthonyahellman.gluttony.client;

import com.anthonyahellman.gluttony.block.entity.CofferOfAvariceBlockEntity;
import com.anthonyahellman.gluttony.menu.CofferOfAvariceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class CofferOfAvariceScreen extends AbstractContainerScreen<CofferOfAvariceMenu> {
    public CofferOfAvariceScreen(CofferOfAvariceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 198;
        imageHeight = 222;
        inventoryLabelY = 128;
        inventoryLabelX = 17;
        titleLabelX = 8;
        titleLabelY = 6;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;
        graphics.fill(left, top, left + imageWidth, top + imageHeight, 0xF00C0B08);
        graphics.fill(left + 3, top + 3, left + imageWidth - 3, top + imageHeight - 3, 0xFF1A1710);
        graphics.fill(left + 5, top + 5, left + imageWidth - 5, top + 16, 0xFF342711);
        if (menu.blockedByUnappraisedItem()) {
            graphics.drawString(font, "JAM", left + 168, top + 6, 0xFFFF5555, false);
            graphics.drawString(font, "Unappraised item in front row", left + 8, top + 119,
                    0xFFFF7777, false);
        } else {
            graphics.drawString(font, String.format("%.1fs", menu.secondsRemaining()),
                    left + 158, top + 6, 0xFFC7B98B, false);
            graphics.drawString(font, String.format("Next: +%.2f Avarice", menu.nextPayout()),
                    left + 8, top + 119, 0xFFD8B642, false);
        }

        for (int row = 0; row < CofferOfAvariceMenu.VISIBLE_ROWS; row++) {
            for (int column = 0; column < CofferOfAvariceBlockEntity.COLUMNS; column++) {
                int x = left + 7 + column * 18;
                int y = top + 17 + row * 18;
                graphics.fill(x, y, x + 18, y + 18, 0xFF75591F);
                graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF241E15);
            }
        }

        int inventoryLeft = left + 16;
        int inventoryTop = top + 139;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) drawSlot(graphics,
                    inventoryLeft + column * 18, inventoryTop + row * 18);
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(graphics, inventoryLeft + column * 18, inventoryTop + 58);
        }

        int trackX = left + 190;
        int trackTop = top + 18;
        int trackHeight = CofferOfAvariceMenu.VISIBLE_ROWS * 18;
        graphics.fill(trackX, trackTop, trackX + 4, trackTop + trackHeight, 0xFF090806);
        int knobTravel = trackHeight - 14;
        int knobY = trackTop + (menu.scrollRow() * knobTravel / CofferOfAvariceMenu.MAX_SCROLL_ROW);
        graphics.fill(trackX, knobY, trackX + 4, knobY + 14, 0xFFD8B642);
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF4B3A19);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF211D16);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int direction = delta > 0 ? -1 : delta < 0 ? 1 : 0;
        if (direction == 0) return false;
        int next = Math.max(0, Math.min(CofferOfAvariceMenu.MAX_SCROLL_ROW,
                menu.scrollRow() + direction));
        if (next == menu.scrollRow()) return true;
        menu.scrollTo(next);
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, next);
        }
        return true;
    }
}
