package com.anthonyahellman.gluttony.client;

import com.anthonyahellman.gluttony.GluttonyMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

public final class SinMenuScreen extends Screen {
    private static final ResourceLocation GLUTTONY = sigil("gluttony");
    private static final ResourceLocation PRIDE = sigil("pride");
    private static final ResourceLocation GREED = sigil("greed");

    public SinMenuScreen() {
        super(Component.literal("The Roots of Sin"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int panelWidth = Math.min(width - 24, 620);
        int panelHeight = Math.min(height - 24, 286);
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;

        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xF20A0809);
        frame(graphics, left, top, panelWidth, panelHeight, 0xFF6B5421);
        graphics.fill(left + 3, top + 3, left + panelWidth - 3, top + 22, 0xFF1B1410);
        centered(graphics, "THE ROOTS OF SIN", width / 2, top + 8, 0xFFFFE7A3);

        int gap = 7;
        int cardsLeft = left + 9;
        int cardWidth = (panelWidth - 18 - gap * 2) / 3;
        int cardTop = top + 29;
        int cardHeight = Math.min(126, panelHeight - 91);
        drawCard(graphics, cardsLeft, cardTop, cardWidth, cardHeight, 1,
                "GLUTTONY", "BEELZEBUB", GLUTTONY, 0xFF9B2335);
        drawCard(graphics, cardsLeft + cardWidth + gap, cardTop, cardWidth, cardHeight, 2,
                "PRIDE", "LUCIFER", PRIDE, 0xFFE3B12B);
        drawCard(graphics, cardsLeft + (cardWidth + gap) * 2, cardTop, cardWidth, cardHeight, 3,
                "GREED", "MAMMON", GREED, 0xFF35B86D);

        int detailsTop = cardTop + cardHeight + 7;
        int detailsHeight = top + panelHeight - detailsTop - 17;
        graphics.fill(left + 9, detailsTop, left + panelWidth - 9, detailsTop + detailsHeight, 0xD0120F10);
        frame(graphics, left + 9, detailsTop, panelWidth - 18, detailsHeight, accentFor(AbilityHudOverlay.sinId()));
        drawDetails(graphics, left + 17, detailsTop + 7);

        centered(graphics, "H or Esc to close", width / 2, top + panelHeight - 12, 0xFF7F7468);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawCard(GuiGraphics graphics, int x, int y, int width, int height, int sin,
                          String name, String sovereign, ResourceLocation icon, int accent) {
        boolean active = AbilityHudOverlay.sinId() == sin;
        int border = active ? accent : 0xFF3A3432;
        graphics.fill(x, y, x + width, y + height, active ? 0xE01B1515 : 0xD00E0C0D);
        frame(graphics, x, y, width, height, border);
        centered(graphics, name, x + width / 2, y + 7, active ? 0xFFFFFFFF : 0xFF8B8582);

        int iconSize = Math.max(34, Math.min(64, height - 52));
        int iconX = x + (width - iconSize) / 2;
        int iconY = y + 21;
        graphics.setColor(active ? 1.0F : 0.42F, active ? 1.0F : 0.42F,
                active ? 1.0F : 0.42F, 1.0F);
        graphics.blit(icon, iconX, iconY, 0, 0, iconSize, iconSize, 128, 128);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        centered(graphics, sovereign, x + width / 2, y + height - 22,
                active ? accent : 0xFF665F5B);
        String state = active ? (AbilityHudOverlay.abilityUnlocked() ? "AWAKENED" : "ABILITY LOCKED") : "UNCLAIMED";
        centered(graphics, state, x + width / 2, y + height - 11,
                active ? 0xFFD8CFBF : 0xFF554F4C);
    }

    private void drawDetails(GuiGraphics graphics, int x, int y) {
        switch (AbilityHudOverlay.sinId()) {
            case 1 -> {
                graphics.drawString(font, "GLUTTONY — " + gluttonyStage(), x, y, 0xFFFFA7AE, false);
                graphics.drawString(font, String.format("Level %d   Souls %.2f   Lifetime %.2f",
                        AbilityHudOverlay.sinLevel(), AbilityHudOverlay.souls(), AbilityHudOverlay.lifetimeSouls()),
                        x, y + 12, 0xFFD9B3FF, false);
                graphics.drawString(font, String.format("Consumed: +%.2f health   +%.2f attack",
                        AbilityHudOverlay.consumedHealth(), AbilityHudOverlay.consumedAttack()),
                        x, y + 24, 0xFFE3C8C8, false);
            }
            case 2 -> {
                graphics.drawString(font, "PRIDE — SOVEREIGN'S SLAM", x, y, 0xFFFFE09A, false);
                graphics.drawString(font, "Impact: 25% max HP  >  Echo: 25% missing HP", x, y + 12,
                        0xFFE8D4A0, false);
                graphics.drawString(font, "After Warden: 10%  >  5% missing HP",
                        x, y + 24, 0xFFFFC94D, false);
            }
            case 3 -> {
                graphics.drawString(font, "GREED — MAMMON'S MARK", x, y, 0xFFFFDD70, false);
                graphics.drawString(font, String.format("Avarice: %.2f", AbilityHudOverlay.avarice()),
                        x, y + 12, 0xFF72E29B, false);
                graphics.drawString(font, "The Coffer liquidates appraised offerings into Avarice.",
                        x, y + 24, 0xFFC9B77B, false);
            }
            default -> {
                graphics.drawString(font, "No natural sin has claimed your soul.", x, y, 0xFFB4AAA3, false);
                graphics.drawString(font, "Awakening relics: Cursed Apple, Pride's Sol, Coin of Mammon.",
                        x, y + 14, 0xFF81766F, false);
            }
        }
    }

    private String gluttonyStage() {
        if (AbilityHudOverlay.sinLevel() >= 100) return AbilityHudOverlay.auraActive() ? "BEELZEBUB ACTIVE" : "BEELZEBUB";
        if (AbilityHudOverlay.sinLevel() >= 50) return "DEVOUR";
        if (AbilityHudOverlay.sinLevel() >= 10) return "SOUL SIPHON";
        return "DORMANT ABILITY";
    }

    private static int accentFor(int sin) {
        return sin == 1 ? 0xFF9B2335 : sin == 2 ? 0xFFE3B12B : sin == 3 ? 0xFF35B86D : 0xFF4B4542;
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
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
