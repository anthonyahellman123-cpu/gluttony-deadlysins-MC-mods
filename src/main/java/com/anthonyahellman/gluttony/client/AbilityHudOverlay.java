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

    private AbilityHudOverlay() {}

    public static void update(AbilityStatePacket packet) {
        sin = packet.sin();
        unlocked = packet.unlocked();
        evolved = packet.evolved();
        cooldownTicks = Math.max(0, packet.cooldownTicks());
        recastTicks = Math.max(0, packet.recastTicks());
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
        if (minecraft.player == null || minecraft.options.hideGui || sin <= 0) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int size = 24;
        int x = graphics.guiWidth() / 2 + 98;
        int y = graphics.guiHeight() - 28;
        int accent = sin == 1 ? 0xFF7A1622 : 0xFFE2AF25;
        int border = unlocked ? accent : 0xFF555555;

        graphics.fill(x - 2, y - 2, x + size + 2, y + size + 2, 0xB0000000);
        graphics.fill(x - 1, y - 1, x + size + 1, y, border);
        graphics.fill(x - 1, y + size, x + size + 1, y + size + 1, border);
        graphics.fill(x - 1, y, x, y + size, border);
        graphics.fill(x + size, y, x + size + 1, y + size, border);

        ItemStack icon = sin == 1 ? ModItems.CURSED_APPLE.get().getDefaultInstance()
                : ModItems.PRIDE_SOL.get().getDefaultInstance();
        graphics.renderItem(icon, x + 4, y + 4);

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
        } else if (evolved) {
            graphics.fill(x + size - 5, y + 1, x + size - 1, y + 5, 0xFFFFF1A8);
        }
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
