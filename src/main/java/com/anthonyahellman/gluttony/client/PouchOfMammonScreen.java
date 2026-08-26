package com.anthonyahellman.gluttony.client;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.anthonyahellman.gluttony.greed.AvariceAppraisals;
import com.anthonyahellman.gluttony.data.GreedData;
import com.anthonyahellman.gluttony.menu.PouchOfMammonMenu;
import com.anthonyahellman.gluttony.network.GreedStatePacket;
import com.anthonyahellman.gluttony.registry.ModItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public final class PouchOfMammonScreen extends AbstractContainerScreen<PouchOfMammonMenu> {
    private static final ResourceLocation SIGIL = new ResourceLocation(GluttonyMod.MOD_ID,
            "textures/gui/sigils/greed.png");
    private static final int GOLD = 0xFFD8B642;
    private static final int BRIGHT_GOLD = 0xFFFFE18A;
    private static final int EMERALD = 0xFF4ED486;
    private static final int RED = 0xFF9A3028;
    private static final int PANEL = 0xF3120E0B;
    private static final int LEFT_CONTENT_INSET = 14;

    private Button divestButton;
    private Button marketButton;

    public PouchOfMammonScreen(PouchOfMammonMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 530;
        imageHeight = 252;
        inventoryLabelY = -1000;
        titleLabelY = -1000;
    }

    @Override
    protected void init() {
        super.init();
        divestButton = addRenderableWidget(Button.builder(Component.literal("DIVEST ASSETS"), button -> {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                                PouchOfMammonMenu.DIVEST_BUTTON);
                    }
                }).bounds(leftPos + 244, topPos + 84, 92, 20).build());
        marketButton = addRenderableWidget(Button.builder(Component.literal("SELL ITEMS"), button -> {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                                PouchOfMammonMenu.MARKET_BUTTON);
                    }
                }).bounds(leftPos + 236, topPos + 221, 286, 20).build());
        addBuyButton(PouchOfMammonMenu.CORE_HEALTH_BUTTON, 59);
        addBuyButton(PouchOfMammonMenu.CORE_ATTACK_BUTTON, 76);
        addBuyButton(PouchOfMammonMenu.CORE_ARMOR_BUTTON, 93);
        addBuyButton(PouchOfMammonMenu.PREMIUM_MOVEMENT_BUTTON, 132);
        addBuyButton(PouchOfMammonMenu.PREMIUM_ATTACK_SPEED_BUTTON, 143);
        addBuyButton(PouchOfMammonMenu.PREMIUM_LUCK_BUTTON, 154);
        addBuyButton(PouchOfMammonMenu.PREMIUM_UNSHAKABLE_BUTTON, 165);
        addBuyButton(PouchOfMammonMenu.PREMIUM_YIELD_BUTTON, 176);
        addBuyButton(PouchOfMammonMenu.COMPOUND_INTEREST_BUTTON, 207);
        addBuyButton(PouchOfMammonMenu.ASSET_APPRECIATION_BUTTON, 218);
        addBuyButton(PouchOfMammonMenu.CONTRACT_BUTTON, 229);
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
        GreedStatePacket state = GreedClientState.get();

        graphics.fill(left, top, left + imageWidth, top + imageHeight, 0xF0060504);
        frame(graphics, left, top, imageWidth, imageHeight, GOLD);
        graphics.fill(left + 3, top + 3, left + imageWidth - 3, top + 20, 0xFF2B160D);
        graphics.blit(SIGIL, left + 5, top + 3, 0, 0, 16, 16, 128, 128);
        graphics.drawString(font, "POUCH OF MAMMON", left + 25 + LEFT_CONTENT_INSET, top + 8,
                BRIGHT_GOLD, false);
        graphics.drawString(font, "G or Esc to close", left + imageWidth - 86, top + 8,
                0xFF8D7961, false);

        // Left-side permanent investment ledger.
        panel(graphics, left + 7, top + 23, 220, 220, RED);
        int ledgerX = left + 15 + LEFT_CONTENT_INSET;
        graphics.drawString(font, "PERMANENT INVESTMENTS", ledgerX, top + 30, BRIGHT_GOLD, false);
        graphics.drawString(font, "CORE INVESTMENTS", ledgerX, top + 45, 0xFFFFC761, false);
        graphics.drawString(font, "STAT", ledgerX, top + 53, 0xFF766B5B, false);
        graphics.drawString(font, "BONUS", ledgerX + 74, top + 53, 0xFF766B5B, false);
        graphics.drawString(font, "OWNED", ledgerX + 109, top + 53, 0xFF766B5B, false);
        graphics.drawString(font, "COST", ledgerX + 144, top + 53, 0xFF766B5B, false);
        coreRow(graphics, ledgerX, top + 59, "MAX HEALTH", state.coreHealth(),
                coreCost(state.coreHealth()));
        coreRow(graphics, ledgerX, top + 76, "ATTACK DAMAGE", state.coreAttack(),
                coreCost(state.coreAttack()));
        coreRow(graphics, ledgerX, top + 93, "ARMOR", state.coreArmor(),
                coreCost(state.coreArmor()));

        line(graphics, left + 13 + LEFT_CONTENT_INSET, top + 113, left + 221, GOLD);
        graphics.drawString(font, "PREMIUM INVESTMENTS", ledgerX, top + 118,
                0xFFFFC761, false);
        premiumRow(graphics, ledgerX, top + 132, "MOVEMENT", state.premiumMovement());
        premiumRow(graphics, ledgerX, top + 143, "ATTACK SPEED", state.premiumAttackSpeed());
        premiumRow(graphics, ledgerX, top + 154, "LUCK", state.premiumLuck());
        premiumRow(graphics, ledgerX, top + 165, "KNOCKBACK RES.", state.premiumKnockback());
        premiumRow(graphics, ledgerX, top + 176, "AVARICE YIELD", state.premiumYield());

        line(graphics, left + 13 + LEFT_CONTENT_INSET, top + 190, left + 221, GOLD);
        graphics.drawString(font, "PINNACLE ASSETS", ledgerX, top + 195, 0xFFFFC761, false);
        pinnacleRow(graphics, ledgerX, top + 207, "COMPOUND INTEREST", state.compoundInterest(),
                state.compoundInterest() >= 5 ? "MAX" : compactPinnacleCost(state.compoundInterest()));
        pinnacleRow(graphics, ledgerX, top + 218, "ASSET APPRECIATION", state.assetAppreciation(),
                state.assetAppreciation() >= 5 ? "MAX" : compactPinnacleCost(state.assetAppreciation()));
        pinnacleRow(graphics, ledgerX, top + 229, "CONTRACT OF MAMMON", state.contractLevel(),
                state.contractLevel() >= 5 ? "MAX" : compactCost(GreedData.contractUpgradeCost(state.contractLevel())));

        // Pouch inventory and the player's inventory remain visible together.
        graphics.drawString(font, "DIVEST ASSETS", left + 248, top + 10, BRIGHT_GOLD, false);
        graphics.drawString(font, "YOUR INVENTORY", left + 347, top + 10, 0xFFB7A78F, false);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) drawSlot(graphics,
                    left + 253 + column * 18, top + 24 + row * 18, GOLD);
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) drawSlot(graphics,
                    left + 346 + column * 18, top + 24 + row * 18, 0xFF68573C);
        }
        for (int column = 0; column < 9; column++) drawSlot(graphics,
                left + 346 + column * 18, top + 86, 0xFF68573C);

        DivestmentPreview preview = previewDivestment();
        graphics.drawString(font, preview.text(), left + 342 - font.width(preview.text()) / 2,
                top + 108, preview.color(), false);

        panel(graphics, left + 236, top + 121, 286, 43, GOLD);
        graphics.renderItem(ModItems.COIN_OF_MAMMON.get().getDefaultInstance(), left + 245, top + 132);
        graphics.drawString(font, format(state.avarice()), left + 269, top + 130, BRIGHT_GOLD, false);
        graphics.drawString(font, "AVARICE", left + 270, top + 145, EMERALD, false);
        graphics.drawString(font, "+" + format(state.vaultIncome()) + " lifetime Vault",
                left + 405, top + 132, 0xFF9ACCA8, false);
        graphics.drawString(font, "+" + format(state.cofferIncome()) + " lifetime Coffer",
                left + 405, top + 146, 0xFFBFA86D, false);

        panel(graphics, left + 236, top + 169, 286, 45, RED);
        if (state.contractLevel() <= 0) {
            graphics.drawString(font, "CONTRACT OF MAMMON: NOT OWNED", left + 245, top + 178,
                    0xFFE9B4A9, false);
            graphics.drawString(font, "Acquisition cost: 100,000", left + 245, top + 193,
                    0xFF9E8C7E, false);
        } else {
            graphics.drawString(font, "CURRENT CLAIM: " + format(state.currentClaimCost()),
                    left + 245, top + 177, BRIGHT_GOLD, false);
            graphics.drawString(font, "CLAIMS THIS WINDOW: " + state.claimsInWindow(),
                    left + 245, top + 191, 0xFFE0C9A0, false);
            graphics.drawString(font, "RESET WINDOW: " + contractWindow(state.contractLevel()),
                    left + 390, top + 191, 0xFFE0C9A0, false);
        }
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) { }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_G || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private DivestmentPreview previewDivestment() {
        double total = 0.0;
        boolean any = false;
        for (int index = 0; index < PouchOfMammonMenu.POUCH_SLOTS; index++) {
            Slot slot = menu.getSlot(index);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;
            any = true;
            if (!AvariceAppraisals.inspectClient(stack).appraised()) {
                return new DivestmentPreview("UNAPPRAISED ASSET — DIVEST BLOCKED", 0xFFFF6868);
            }
            total += AvariceAppraisals.clientStackValue(stack);
        }
        return new DivestmentPreview(any ? "DIVESTMENT VALUE: +" + format(total) : "GRID IS PERSISTENT",
                any ? EMERALD : 0xFF817568);
    }

    private void coreRow(GuiGraphics graphics, int x, int y, String name, int owned, double nextCost) {
        graphics.drawString(font, name, x, y, 0xFFE8D8BD, false);
        double appreciation = 1.0 + GreedClientState.get().assetAppreciation() * 0.10;
        String bonus = name.equals("MAX HEALTH") ? "+" + format(owned * 2.0 * appreciation) + " HP"
                : name.equals("ATTACK DAMAGE") ? "+" + format(owned * appreciation)
                : "+" + format(owned * 0.5 * appreciation);
        graphics.drawString(font, bonus, x + 76, y, EMERALD, false);
        graphics.drawString(font, Integer.toString(owned), x + 116, y, 0xFFD7C29A, false);
        graphics.drawString(font, format(nextCost), x + 144, y, 0xFFBDAA82, false);
    }

    private void premiumRow(GuiGraphics graphics, int x, int y, String name, int level) {
        graphics.drawString(font, name, x, y, 0xFFCDBD9F, false);
        graphics.drawString(font, level + "/10", x + 111, y, 0xFFAA9A80, false);
        graphics.drawString(font, level >= 10 ? "MAX" : compactCost(premiumCost(level)), x + 143, y,
                0xFF9E8E74, false);
    }

    private void pinnacleRow(GuiGraphics graphics, int x, int y, String name, int level, String cost) {
        graphics.drawString(font, name, x, y, 0xFFD7C7AA, false);
        graphics.drawString(font, level + "/5", x + 122, y, level > 0 ? BRIGHT_GOLD : 0xFF8B7D69, false);
        graphics.drawString(font, cost, x + 153, y, 0xFF9E8E74, false);
    }

    private static double coreCost(int purchases) {
        return 100.0 * Math.pow(2.0, Math.max(0, purchases) / 10);
    }

    private static double premiumCost(int level) {
        return 5_000.0 * Math.pow(1.5, Math.max(0, level) / 2);
    }

    private static String compactPinnacleCost(int level) {
        double cost = 250_000.0 * Math.pow(2.0, Math.max(0, level));
        if (cost >= 1_000_000.0) return String.format("%.0fm", cost / 1_000_000.0);
        return String.format("%.0fk", cost / 1_000.0);
    }

    private static String contractWindow(int level) {
        if (level == 1) return "60:00";
        if (level == 2) return "52:30";
        if (level == 3) return "45:00";
        if (level == 4) return "37:30";
        if (level == 5) return "30:00";
        return "--";
    }

    private void addBuyButton(int id, int relativeY) {
        addRenderableWidget(Button.builder(Component.literal("BUY"), button -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
            }
        }).bounds(leftPos + 15 + LEFT_CONTENT_INSET + 184, topPos + relativeY - 1, 22, 10).build());
    }

    private static String compactCost(double cost) {
        if (cost >= 1_000_000.0) return String.format("%.1fm", cost / 1_000_000.0);
        if (cost >= 1_000.0) return String.format("%.1fk", cost / 1_000.0);
        return format(cost);
    }

    private static String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001) return String.format("%,.0f", value);
        return String.format("%,.2f", value);
    }

    private static void panel(GuiGraphics graphics, int x, int y, int width, int height, int accent) {
        graphics.fill(x, y, x + width, y + height, PANEL);
        frame(graphics, x, y, width, height, accent);
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y, int border) {
        graphics.fill(x, y, x + 18, y + 18, border);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF20160F);
    }

    private static void line(GuiGraphics graphics, int x1, int y, int x2, int color) {
        graphics.fill(x1, y, x2, y + 1, color);
    }

    private static void frame(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private record DivestmentPreview(String text, int color) {}
}
