package com.anthonyahellman.gluttony.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.anthonyahellman.gluttony.GluttonyMod;
import com.anthonyahellman.gluttony.network.GreedStatePacket;
import com.anthonyahellman.gluttony.network.ModNetwork;
import com.anthonyahellman.gluttony.network.PrideStatePacket;
import com.anthonyahellman.gluttony.network.SinAbilityPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;
import com.anthonyahellman.gluttony.network.GluttonyAbilitySelectionPacket;
import com.anthonyahellman.gluttony.network.GluttonyTargetModePacket;

public final class SinMenuScreen extends Screen {
    private static final ResourceLocation GLUTTONY = sigil("gluttony");
    private static final ResourceLocation PRIDE = sigil("pride");
    private static final ResourceLocation GREED = sigil("greed");
    private final Button[] gluttonyAbilityButtons = new Button[3];
    private final Button[][] gluttonyTargetButtons = new Button[3][3];

    public SinMenuScreen() { super(Component.literal("The Roots of Sin")); }

    @Override
    protected void init() {
        super.init();
        if (AbilityHudOverlay.sinId() != 1) return;
        int panelWidth = Math.min(width - 24, 600);
        int panelHeight = Math.min(height - 24, 310);
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        String[] names = {"SOUL SIPHON", "DEVOUR", "BEELZEBUB"};
        int[] levels = {10, 50, 100};
        String[] modes = {"MOBS", "BOTH", "PLAYERS"};
        int[] modeWidths = {36, 36, 42};
        for (int index = 0; index < names.length; index++) {
            final int selected = index;
            Button button = Button.builder(Component.literal(names[index]), ignored ->
                    ModNetwork.CHANNEL.sendToServer(new GluttonyAbilitySelectionPacket(selected)))
                    .bounds(left + 92 + index * 128, top + 150, 118, 20).build();
            button.active = AbilityHudOverlay.sinLevel() >= levels[index];
            gluttonyAbilityButtons[index] = addRenderableWidget(button);
            int modeX = left + 92 + index * 128;
            for (int mode = 0; mode < modes.length; mode++) {
                final int selectedMode = mode;
                Button targetButton = Button.builder(Component.literal(modes[mode]), ignored ->
                        ModNetwork.CHANNEL.sendToServer(
                                new GluttonyTargetModePacket(selected, selectedMode)))
                        .bounds(modeX + (mode == 0 ? 0 : mode == 1 ? 38 : 76),
                                top + 173, modeWidths[mode], 16).build();
                targetButton.active = AbilityHudOverlay.sinLevel() >= levels[index];
                gluttonyTargetButtons[index][mode] = addRenderableWidget(targetButton);
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int panelWidth = Math.min(width - 24, 600);
        int panelHeight = Math.min(height - 24, 310);
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        int sin = AbilityHudOverlay.sinId();

        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xF20A0809);
        frame(graphics, left, top, panelWidth, panelHeight, accentFor(sin));
        graphics.fill(left + 3, top + 3, left + panelWidth - 3, top + 23, 0xFF1B1410);
        centered(graphics, "THE ROOTS OF SIN", width / 2, top + 9, 0xFFFFE7A3);
        renderSigilWatermark(graphics, left, top, panelWidth, panelHeight, sigilFor(sin));

        if (sin == 1) renderGluttony(graphics, left, top, panelWidth);
        else if (sin == 2) renderPride(graphics, left, top, panelWidth);
        else if (sin == 3) renderGreed(graphics, left, top, panelWidth);
        else renderDormant(graphics, left, top, panelWidth);

        centered(graphics, "H or Esc to close", width / 2, top + panelHeight - 13, 0xFF7F7468);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderGluttony(GuiGraphics graphics, int left, int top, int width) {
        drawIdentity(graphics, left, top, "GLUTTONY", "BEELZEBUB", 0xFF9B2335);
        int x = left + 92;
        int y = top + 39;
        graphics.drawString(font, "PROGRESSION", x, y, 0xFFFFA7AE, false);
        graphics.drawString(font, "Stage: " + gluttonyStage(), x, y + 17, 0xFFE093A0, false);
        graphics.drawString(font, "Level: " + AbilityHudOverlay.sinLevel(), x, y + 31, 0xFFF1D4DA, false);
        graphics.drawString(font, String.format("Souls: %.2f", AbilityHudOverlay.souls()), x, y + 45,
                0xFFD9B3FF, false);
        graphics.drawString(font, String.format("Lifetime Souls: %.2f", AbilityHudOverlay.lifetimeSouls()),
                x, y + 59, 0xFFC7A7D9, false);

        int second = left + width / 2 + 18;
        graphics.drawString(font, "CONSUMED POWER", second, y, 0xFFFFA7AE, false);
        graphics.drawString(font, String.format("Maximum Health: +%.2f", AbilityHudOverlay.consumedHealth()),
                second, y + 17, 0xFFE6C6C6, false);
        graphics.drawString(font, String.format("Attack Damage: +%.2f", AbilityHudOverlay.consumedAttack()),
                second, y + 31, 0xFFE6C6C6, false);
        graphics.drawString(font, "G — " + gluttonyAction(), second, y + 59, 0xFFFF7777, false);
        graphics.drawString(font, "EQUIP TO G", left + 92, top + 134, 0xFFFFA7AE, false);
        String[] names = {"SOUL SIPHON", "DEVOUR", "BEELZEBUB"};
        for (int index = 0; index < gluttonyAbilityButtons.length; index++) {
            Button button = gluttonyAbilityButtons[index];
            if (button == null) continue;
            boolean selected = AbilityHudOverlay.gluttonyAbility() == index;
            button.setMessage(Component.literal(selected ? "[" + names[index] + "]" : names[index]));
            String[] modes = {"MOBS", "BOTH", "PLAYERS"};
            int selectedMode = AbilityHudOverlay.gluttonyTargetMode(index);
            for (int mode = 0; mode < modes.length; mode++) {
                Button targetButton = gluttonyTargetButtons[index][mode];
                if (targetButton != null) targetButton.setMessage(Component.literal(
                        selectedMode == mode ? "[" + modes[mode] + "]" : modes[mode]));
            }
        }
    }

    private void renderPride(GuiGraphics graphics, int left, int top, int width) {
        drawIdentity(graphics, left, top, "PRIDE", "LUCIFER", 0xFFE3B12B);
        PrideStatePacket state = PrideClientState.get();
        int x = left + 92;
        int y = top + 39;
        graphics.drawString(font, "SUPREMACY TRIALS", x, y, 0xFFFFE09A, false);
        trial(graphics, x, y + 18, "Ender Dragons", state.dragons(), 12);
        trial(graphics, x, y + 33, "Withers", state.withers(), 8);
        trial(graphics, x, y + 48, "Elder Guardians", state.guardians(), 4);
        trial(graphics, x, y + 63, "Wardens", state.wardens(), 2);
        graphics.drawString(font, "Total conquests: " + state.totalConquests(), x, y + 83,
                0xFFFFD95A, false);

        int second = left + width / 2 + 18;
        graphics.drawString(font, "PRIDE-GRANTED POWER", second, y, 0xFFFFE09A, false);
        graphics.drawString(font, String.format("Maximum Health: +%.0f", state.maxHealthBonus()),
                second, y + 18, 0xFFE8D4A0, false);
        graphics.drawString(font, String.format("Attack Damage: +%.0f", state.attackBonus()),
                second, y + 33, 0xFFE8D4A0, false);
        graphics.drawString(font, String.format("Boss damage bonus: +%.1f%%", state.bossDamageBonus() * 100.0),
                second, y + 48, 0xFFE8D4A0, false);
        graphics.drawString(font, "Impact: 25% max HP + 50% Attack", second, y + 70, 0xFFFFC94D, false);
        graphics.drawString(font, "Echo: 25% missing HP", second, y + 84, 0xFFFFC94D, false);
        graphics.drawString(font, "Warden echoes: 10% then 5%", second, y + 98, 0xFFFFC94D, false);
        graphics.drawString(font, "G — LUCIFER'S FALL", second, y + 121, 0xFFFFE66D, false);
    }

    private void renderGreed(GuiGraphics graphics, int left, int top, int width) {
        drawIdentity(graphics, left, top, "GREED", "MAMMON", 0xFFD8B642);
        GreedStatePacket state = GreedClientState.get();
        int x = left + 92;
        int y = top + 39;
        graphics.drawString(font, "FINANCIAL POWER", x, y, 0xFFFFDD70, false);
        graphics.drawString(font, "Avarice: " + format(state.avarice()), x, y + 17, 0xFF72E29B, false);
        double appreciation = 1.0 + state.assetAppreciation() * 0.10;
        graphics.drawString(font, "Greed Max HP: +" + format(state.coreHealth() * 2.0 * appreciation)
                        + " (" + state.coreHealth() + " purchases)",
                x, y + 34, 0xFFD9C9A9, false);
        graphics.drawString(font, "Greed Attack: +" + format(state.coreAttack() * appreciation)
                        + " (" + state.coreAttack() + " purchases)",
                x, y + 48, 0xFFD9C9A9, false);
        graphics.drawString(font, "Greed Armor: +" + format(state.coreArmor() * 0.5 * appreciation)
                        + " (" + state.coreArmor() + " purchases)",
                x, y + 62, 0xFFD9C9A9, false);
        graphics.drawString(font, "Lifetime earned: " + format(state.lifetimeEarned()), x, y + 84,
                0xFFA9D6B5, false);
        graphics.drawString(font, "Lifetime spent: " + format(state.lifetimeSpent()), x, y + 98,
                0xFFC9A68D, false);
        graphics.drawString(font, "Assets divested: " + state.assetsDivested(), x, y + 112,
                0xFFD8C28D, false);

        int second = left + width / 2 + 20;
        graphics.drawString(font, "PORTFOLIO", second, y, 0xFFFFDD70, false);
        graphics.drawString(font, "Premiums: " + premiumTotal(state) + "/50", second, y + 17,
                0xFFD9C9A9, false);
        graphics.drawString(font, "Pinnacles: " + pinnacleTotal(state) + "/15", second, y + 31,
                0xFFD9C9A9, false);
        graphics.drawString(font, "Vault income: " + format(state.vaultIncome()), second, y + 50,
                0xFF92CE9D, false);
        graphics.drawString(font, "Coffer income: " + format(state.cofferIncome()), second, y + 64,
                0xFFD7BA67, false);
        graphics.drawString(font, "Contract claims: " + state.contractClaims(), second, y + 78,
                0xFFE5A79C, false);
        graphics.drawString(font, "Net worth: TBD", second, y + 98, 0xFF9A8D7A, false);
        graphics.drawString(font, "G — OPEN POUCH OF MAMMON", second, y + 120, 0xFFFFD95A, false);
    }

    private void renderDormant(GuiGraphics graphics, int left, int top, int width) {
        centered(graphics, "NO NATURAL SIN HAS AWAKENED", left + width / 2, top + 76, 0xFFB4AAA3);
        centered(graphics, "H remains dormant until a Sin claims your soul.", left + width / 2,
                top + 98, 0xFF81766F);
        centered(graphics, "G has no Sin ability to invoke.", left + width / 2, top + 114, 0xFF81766F);
    }

    private void renderSigilWatermark(GuiGraphics graphics, int left, int top, int panelWidth,
                                      int panelHeight, ResourceLocation sigil) {
        if (sigil == null) return;
        int bodyTop = top + 24;
        int bodyHeight = panelHeight - 44;
        int size = Math.min(220, Math.min(panelWidth - 48, bodyHeight - 24));
        if (size <= 0) return;
        int x = left + (panelWidth - size) / 2;
        int y = bodyTop + (bodyHeight - size) / 2;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.16F);
        graphics.blit(sigil, x, y, 0, 0, size, size, 128, 128);
        graphics.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private void drawIdentity(GuiGraphics graphics, int left, int top,
                              String sin, String demon, int accent) {
        int x = left + 15;
        int y = top + 36;
        graphics.fill(x - 4, y - 4, x + 68, y + 91, 0xD0120F10);
        frame(graphics, x - 4, y - 4, 72, 91, accent);
        centered(graphics, "SIN", x + 32, y + 14, accent);
        graphics.fill(x + 12, y + 29, x + 52, y + 30, accent);
        centered(graphics, sin, x + 32, y + 40, 0xFFFFFFFF);
        centered(graphics, demon, x + 32, y + 55, accent);
        graphics.fill(x + 20, y + 72, x + 44, y + 73, accent);
    }

    private void trial(GuiGraphics graphics, int x, int y, String name, int current, int required) {
        graphics.drawString(font, name + ": " + current + "/" + required, x, y,
                current >= required ? 0xFFFFD95A : 0xFFD8C79F, false);
    }

    private String gluttonyStage() {
        if (AbilityHudOverlay.sinLevel() >= 100) return "ALL ABILITIES UNLOCKED";
        if (AbilityHudOverlay.sinLevel() >= 50) return "DEVOUR UNLOCKED";
        if (AbilityHudOverlay.sinLevel() >= 10) return "SOUL SIPHON UNLOCKED";
        return "DORMANT ABILITY";
    }

    private String gluttonyAction() {
        return switch (AbilityHudOverlay.gluttonyAbility()) {
            case 1 -> "ARM / CHARGE DEVOUR";
            case 2 -> AbilityHudOverlay.auraActive() ? "CLOSE BEELZEBUB" : "OPEN BEELZEBUB";
            default -> "ARM SOUL SIPHON";
        };
    }

    private static int premiumTotal(GreedStatePacket state) {
        return state.premiumMovement() + state.premiumAttackSpeed() + state.premiumLuck()
                + state.premiumKnockback() + state.premiumYield();
    }

    private static int pinnacleTotal(GreedStatePacket state) {
        return state.compoundInterest() + state.assetAppreciation() + state.contractLevel();
    }

    private static String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001) return String.format("%,.0f", value);
        return String.format("%,.2f", value);
    }

    private static int accentFor(int sin) {
        return sin == 1 ? 0xFF9B2335 : sin == 2 ? 0xFFE3B12B : sin == 3 ? 0xFFD8B642 : 0xFF4B4542;
    }

    private static ResourceLocation sigilFor(int sin) {
        return sin == 1 ? GLUTTONY : sin == 2 ? PRIDE : sin == 3 ? GREED : null;
    }

    private static ResourceLocation sigil(String name) {
        return new ResourceLocation(GluttonyMod.MOD_ID, "textures/gui/sigils/" + name + ".png");
    }

    private void centered(GuiGraphics graphics, String text, int centerX, int y, int color) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
    }

    private static void frame(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_H || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_G && AbilityHudOverlay.sinId() > 0) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean isPauseScreen() { return false; }
}
