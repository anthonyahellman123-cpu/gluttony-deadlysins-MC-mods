package com.anthonyahellman.gluttony.client;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.anthonyahellman.gluttony.network.AbilityStatePacket;
import com.anthonyahellman.gluttony.registry.ModItems;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = GluttonyMod.MOD_ID, value = Dist.CLIENT)
public final class AbilityHudOverlay {
    private static final int MAX_COOLDOWN = 60;
    private static final int MAX_RECAST = 20;

    private static int sin;
    private static boolean unlocked;
    private static boolean evolved;
    private static int cooldownTicks;
    private static int recastTicks;
    private static int level;
    private static double currentSouls;
    private static double lifetimeSouls;
    private static double extractedHealth;
    private static double extractedAttack;
    private static int nextLevelSouls;
    private static boolean auraActive;
    private static double avarice;
    private static int prideChargeTicks;
    private static int prideChargeStage;
    private static int gluttonyAbility;
    private static boolean statsVisible;

    private AbilityHudOverlay() {}

    public static void update(AbilityStatePacket packet) {
        sin = packet.sin();
        unlocked = packet.unlocked();
        evolved = packet.evolved();
        cooldownTicks = Math.max(0, packet.cooldownTicks());
        recastTicks = Math.max(0, packet.recastTicks());
        level = packet.level();
        currentSouls = packet.currentSouls();
        lifetimeSouls = packet.lifetimeSouls();
        extractedHealth = packet.extractedHealth();
        extractedAttack = packet.extractedAttack();
        nextLevelSouls = packet.nextLevelSouls();
        auraActive = packet.auraActive();
        avarice = packet.avarice();
        prideChargeTicks = packet.prideChargeTicks();
        prideChargeStage = packet.prideChargeStage();
        gluttonyAbility = packet.gluttonyAbility();
    }

    public static void toggleStats() {
        statsVisible = !statsVisible;
    }

    static int sinId() { return sin; }
    static boolean abilityUnlocked() { return unlocked; }
    static boolean fullyEvolved() { return evolved; }
    static int sinLevel() { return level; }
    static double souls() { return currentSouls; }
    static double lifetimeSouls() { return lifetimeSouls; }
    static double consumedHealth() { return extractedHealth; }
    static double consumedAttack() { return extractedAttack; }
    static boolean auraActive() { return auraActive; }
    static double avarice() { return avarice; }
    static int gluttonyAbility() { return gluttonyAbility; }

    public static boolean greedAwakened() {
        return sin == 3;
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || Minecraft.getInstance().isPaused()) return;
        if (cooldownTicks > 0) cooldownTicks--;
        if (recastTicks > 0) recastTicks--;
    }

    @SubscribeEvent
    public static void render(RenderGuiOverlayEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) return;

        GuiGraphics graphics = event.getGuiGraphics();
        if (sin <= 0) return;
        int size = 24;
        int x = graphics.guiWidth() / 2 + 98;
        int y = graphics.guiHeight() - 28;
        int accent = sin == 1 ? 0xFF7A1622 : sin == 2 ? 0xFFE2AF25 : 0xFF2FAE63;
        int border = unlocked ? accent : 0xFF555555;

        graphics.fill(x - 2, y - 2, x + size + 2, y + size + 2, 0xB0000000);
        graphics.fill(x - 1, y - 1, x + size + 1, y, border);
        graphics.fill(x - 1, y + size, x + size + 1, y + size + 1, border);
        graphics.fill(x - 1, y, x, y + size, border);
        graphics.fill(x + size, y, x + size + 1, y + size, border);

        ItemStack icon = sin == 1 ? ModItems.CURSED_APPLE.get().getDefaultInstance()
                : sin == 2 ? ModItems.PRIDE_SOL.get().getDefaultInstance()
                : ModItems.COIN_OF_MAMMON.get().getDefaultInstance();
        graphics.renderItem(icon, x + 4, y + 4);

        // Item rendering uses a high GUI depth. Flush it, then move all state
        // feedback above the icon so neither the timer nor recast can hide.
        graphics.flush();
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 300.0F);
        renderState(graphics, minecraft, x, y, size);
        if (sin == 2 && unlocked) renderPrideCharge(graphics, minecraft, x, y, size);
        graphics.pose().popPose();

    }

    private static void renderPrideCharge(GuiGraphics graphics, Minecraft minecraft,
                                          int x, int y, int size) {
        double progress = prideChargeStage >= 5 ? 1.0
                : (prideChargeTicks % 1200) / 1200.0;
        if (progress > 0.0) drawRadial(graphics, x, y, size, progress, 0x45FFE58A);
        String stage = prideChargeStage <= 0 ? "STAGE 0" : "STAGE " + roman(prideChargeStage);
        drawCentered(graphics, minecraft, stage, x + size / 2, y - 10,
                prideChargeStage >= 5 ? 0xFFFFFFFF : 0xFFFFE58A);
    }

    private static String roman(int stage) {
        return switch (stage) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> "0";
        };
    }

    private static void renderState(GuiGraphics graphics, Minecraft minecraft, int x, int y, int size) {
        if (!unlocked) {
            graphics.fill(x, y, x + size, y + size, 0xB0000000);
            drawCentered(graphics, minecraft, "LOCK", x + size / 2, y + 8, 0xFFAAAAAA);
            return;
        }

        if (recastTicks > 0) {
            double fraction = recastTicks / (double) MAX_RECAST;
            drawRadial(graphics, x, y, size, 1.0 - fraction, 0x9AFFF1A8);
            graphics.fill(x - 2, y - 2, x + size + 2, y, 0xFFFFE66D);
            drawCentered(graphics, minecraft, "R", x + size / 2, y + 8, 0xFFFFFFFF);
            return;
        }

        if (cooldownTicks > 0) {
            double fraction = Math.min(1.0, cooldownTicks / (double) MAX_COOLDOWN);
            drawRadial(graphics, x, y, size, fraction, 0xB0000000);
            String time = String.format("%.1f", cooldownTicks / 20.0);
            drawCentered(graphics, minecraft, time, x + size / 2, y + 8, 0xFFFFFFFF);
        } else if (sin == 1 && auraActive) {
            graphics.fill(x, y, x + size, y + size, 0x70330044);
            drawCentered(graphics, minecraft, "ON", x + size / 2, y + 8, 0xFFFF7777);
        } else if (evolved) {
            graphics.fill(x + size - 5, y + 1, x + size - 1, y + 5, 0xFFFFF1A8);
        }
    }

    private static void renderGluttonyStats(GuiGraphics graphics, Minecraft minecraft) {
        int width = 190;
        int height = 100;
        int x = 10;
        int y = graphics.guiHeight() / 2 - height / 2;
        graphics.fill(x, y, x + width, y + height, 0xD0100714);
        graphics.fill(x, y, x + 3, y + height, 0xFF7A1622);
        graphics.fill(x, y, x + width, y + 2, 0xFF9B2335);

        graphics.drawString(minecraft.font, "THE ROOTS OF SIN — GLUTTONY", x + 9, y + 8, 0xFFFFB8B8, true);
        graphics.drawString(minecraft.font, "Level " + level + "  •  " + stageName(), x + 9, y + 22,
                auraActive ? 0xFFFF5555 : 0xFFE093A0, false);
        graphics.drawString(minecraft.font, String.format("Souls: %.2f", currentSouls), x + 9, y + 38,
                0xFFD9B3FF, false);
        String lifetime = level >= 100 ? String.format("Lifetime: %.2f  •  MAX", lifetimeSouls)
                : String.format("Lifetime: %.2f / %d", lifetimeSouls, nextLevelSouls);
        graphics.drawString(minecraft.font, lifetime, x + 9, y + 50, 0xFFC7A7D9, false);
        graphics.drawString(minecraft.font, String.format("Consumed Health: +%.2f", extractedHealth),
                x + 9, y + 66, 0xFFE6C6C6, false);
        graphics.drawString(minecraft.font, String.format("Consumed Attack: +%.2f", extractedAttack),
                x + 9, y + 78, 0xFFE6C6C6, false);
        graphics.drawString(minecraft.font, "H to close", x + width - 52, y + height - 11, 0xFF777777, false);
    }

    private static void renderDormantStats(GuiGraphics graphics, Minecraft minecraft) {
        int width = 190;
        int height = 62;
        int x = 10;
        int y = graphics.guiHeight() / 2 - height / 2;
        graphics.fill(x, y, x + width, y + height, 0xD0100714);
        graphics.fill(x, y, x + 3, y + height, 0xFF555555);
        graphics.drawString(minecraft.font, "THE ROOTS OF SIN", x + 9, y + 8, 0xFFBBBBBB, true);
        graphics.drawString(minecraft.font, "No natural sin has awakened.", x + 9, y + 25,
                0xFF999999, false);
        graphics.drawString(minecraft.font, "Consume the Cursed Apple for Gluttony.", x + 9, y + 38,
                0xFFB57A86, false);
        graphics.drawString(minecraft.font, "H to close", x + width - 52, y + height - 11, 0xFF777777, false);
    }

    private static void renderGreedStats(GuiGraphics graphics, Minecraft minecraft) {
        int width = 190;
        int height = 62;
        int x = 10;
        int y = graphics.guiHeight() / 2 - height / 2;
        graphics.fill(x, y, x + width, y + height, 0xD008130B);
        graphics.fill(x, y, x + 3, y + height, 0xFF2FAE63);
        graphics.fill(x, y, x + width, y + 2, 0xFFD8B642);
        graphics.drawString(minecraft.font, "THE ROOTS OF SIN — GREED", x + 9, y + 8, 0xFFFFE89A, true);
        graphics.drawString(minecraft.font, String.format("Avarice: %.2f", avarice), x + 9, y + 27,
                0xFF79E09A, false);
        graphics.drawString(minecraft.font, "H to close", x + width - 52, y + height - 11, 0xFF777777, false);
    }

    private static String stageName() {
        if (gluttonyAbility == 2) return auraActive ? "BEELZEBUB ACTIVE" : "BEELZEBUB";
        if (gluttonyAbility == 1) return "DEVOUR";
        if (level >= 10) return "SOUL SIPHON";
        return "DORMANT ABILITY";
    }

    private static void drawCentered(GuiGraphics graphics, Minecraft minecraft, String text,
                                     int centerX, int y, int color) {
        graphics.drawString(minecraft.font, text, centerX - minecraft.font.width(text) / 2, y, color, true);
    }

    private static void drawRadial(GuiGraphics graphics, int x, int y, int size, double fraction, int color) {
        if (fraction <= 0.0) return;
        graphics.enableScissor(x, y, x + size, y + size);
        double sweep = Math.PI * 2.0 * Math.min(1.0, fraction);
        int steps = Math.max(2, (int) Math.ceil(32 * fraction));
        float centerX = x + size / 2.0F;
        float centerY = y + size / 2.0F;
        float radius = size * 0.72F;
        float alpha = ((color >>> 24) & 255) / 255.0F;
        float red = ((color >>> 16) & 255) / 255.0F;
        float green = ((color >>> 8) & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;

        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, centerX, centerY, 0).color(red, green, blue, alpha).endVertex();
        for (int i = 0; i <= steps; i++) {
            double angle = -Math.PI / 2.0 + sweep * i / steps;
            buffer.vertex(matrix, centerX + (float) Math.cos(angle) * radius,
                    centerY + (float) Math.sin(angle) * radius, 0)
                    .color(red, green, blue, alpha).endVertex();
        }
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.disableBlend();
        graphics.disableScissor();
    }
}
