package com.anthonyahellman.gluttony.client;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.anthonyahellman.gluttony.network.SinStatusPacket;
import com.anthonyahellman.gluttony.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class SinStatusScreen extends Screen {
    private static final ResourceLocation GLUTTONY_CREST =
            new ResourceLocation(GluttonyMod.MOD_ID, "textures/gui/gluttony_crest.png");
    private final SinStatusPacket data;

    private SinStatusScreen(SinStatusPacket data) {
        super(Component.literal("Sin Status"));
        this.data = data;
    }

    public static void open(SinStatusPacket packet) {
        Minecraft.getInstance().setScreen(new SinStatusScreen(packet));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        int panelWidth = Math.min(360, width - 28);
        int panelHeight = Math.min(230, height - 28);
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        int right = left + panelWidth;
        int bottom = top + panelHeight;
        int accent = data.sin() == 1 ? 0xFF8D1B27 : data.sin() == 2 ? 0xFFD4A62A : 0xFF666666;

        graphics.fill(left, top, right, bottom, 0xE5121012);
        graphics.fill(left, top, right, top + 2, accent);
        graphics.fill(left, bottom - 2, right, bottom, accent);
        graphics.fill(left, top, left + 2, bottom, accent);
        graphics.fill(right - 2, top, right, bottom, accent);

        int crestX = left + 18;
        int crestY = top + 18;
        renderCrest(graphics, crestX, crestY);

        int textX = left + 94;
        graphics.drawString(font, data.title(), textX, top + 22, accent, true);
        graphics.drawString(font, data.stage(), textX, top + 37, 0xFFF3E6D2, true);
        drawWrapped(graphics, data.description(), textX, top + 54, panelWidth - 112, 0xFFB8B1AD);

        int dividerY = top + 90;
        graphics.fill(left + 14, dividerY, right - 14, dividerY + 1, 0x665E5656);

        int columnGap = 18;
        int columnWidth = (panelWidth - 42 - columnGap) / 2;
        int leftColumnX = left + 18;
        int rightColumnX = leftColumnX + columnWidth + columnGap;
        int listY = dividerY + 12;

        graphics.drawString(font, "STATUS", leftColumnX, listY, accent, false);
        int y = listY + 14;
        for (String line : data.stats()) {
            graphics.drawString(font, line, leftColumnX, y, 0xFFE0D9D4, false);
            y += 12;
        }

        graphics.drawString(font, "PROGRESSION", rightColumnX, listY, accent, false);
        y = listY + 14;
        for (String line : data.progression()) {
            drawWrapped(graphics, line, rightColumnX, y, columnWidth, 0xFFD0C8C2);
            y += Math.max(12, font.wordWrapHeight(line, columnWidth) + 3);
        }

        int barLeft = left + 18;
        int barRight = right - 18;
        int barBottom = bottom - 16;
        int barTop = barBottom - 6;
        graphics.fill(barLeft, barTop, barRight, barBottom, 0xFF2A2527);
        int fill = (int) Math.round((barRight - barLeft) * Math.max(0.0, Math.min(1.0, data.progress())));
        graphics.fill(barLeft, barTop, barLeft + fill, barBottom, accent);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderCrest(GuiGraphics graphics, int x, int y) {
        if (data.sin() == 1) {
            graphics.blit(GLUTTONY_CREST, x, y, 0, 0, 64, 64, 128, 128);
        } else if (data.sin() == 2) {
            ItemStack pride = ModItems.PRIDE_SOL.get().getDefaultInstance();
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(3.0F, 3.0F, 1.0F);
            graphics.renderItem(pride, 2, 2);
            graphics.pose().popPose();
        }
    }

    private void drawWrapped(GuiGraphics graphics, String text, int x, int y, int maxWidth, int color) {
        int lineY = y;
        for (var line : font.split(Component.literal(text), maxWidth)) {
            graphics.drawString(font, line, x, lineY, color, false);
            lineY += 10;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
