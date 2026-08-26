package com.anthonyahellman.gluttony.client;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.anthonyahellman.gluttony.network.ModNetwork;
import com.anthonyahellman.gluttony.network.SoulSiphonPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import com.anthonyahellman.gluttony.greed.AvariceAppraisals;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GluttonyMod.MOD_ID, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private ClientForgeEvents() {}

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.isPaused()) return;
        while (ClientModEvents.SOUL_SIPHON.consumeClick()) {
            ModNetwork.CHANNEL.sendToServer(new SoulSiphonPacket());
        }
        while (ClientModEvents.SIN_STATS.consumeClick()) {
            AbilityHudOverlay.toggleStats();
        }
    }

    @SubscribeEvent
    public static void itemTooltip(ItemTooltipEvent event) {
        if (!AbilityHudOverlay.greedAwakened()) return;
        double each = AvariceAppraisals.value(event.getItemStack());
        if (each <= 0.0) {
            event.getToolTip().add(Component.literal("Unappraised").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        double stack = AvariceAppraisals.stackValue(event.getItemStack());
        event.getToolTip().add(Component.literal(String.format("Appraised: %.2f Avarice each", each))
                .withStyle(ChatFormatting.GOLD));
        if (event.getItemStack().getCount() > 1) {
            event.getToolTip().add(Component.literal(String.format("Stack value: %.2f Avarice", stack))
                    .withStyle(ChatFormatting.DARK_GREEN));
        }
    }
}
