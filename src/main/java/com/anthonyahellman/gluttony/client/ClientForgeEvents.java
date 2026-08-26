package com.anthonyahellman.gluttony.client;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.anthonyahellman.gluttony.network.ModNetwork;
import com.anthonyahellman.gluttony.network.SinAbilityPacket;
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
        if (AbilityHudOverlay.greedAwakened()) {
            int unshakable = GreedClientState.get().premiumKnockback();
            int maximumVisibleHurtTicks = Math.round(minecraft.player.hurtDuration
                    * Math.max(0.0F, 1.0F - unshakable * 0.10F));
            if (minecraft.player.hurtTime > maximumVisibleHurtTicks) {
                minecraft.player.hurtTime = maximumVisibleHurtTicks;
            }
        }
        while (ClientModEvents.SIN_ABILITY.consumeClick()) {
            if (minecraft.screen instanceof PouchOfMammonScreen) minecraft.player.closeContainer();
            else if (minecraft.screen == null) ModNetwork.CHANNEL.sendToServer(new SinAbilityPacket());
        }
        while (ClientModEvents.SIN_STATS.consumeClick()) {
            if (AbilityHudOverlay.sinId() <= 0) continue;
            if (minecraft.screen instanceof SinMenuScreen) minecraft.setScreen(null);
            else if (minecraft.screen == null) minecraft.setScreen(new SinMenuScreen());
        }
    }

    @SubscribeEvent
    public static void itemTooltip(ItemTooltipEvent event) {
        if (!AbilityHudOverlay.greedAwakened()) return;
        double each = AvariceAppraisals.value(event.getItemStack());
        if (each <= 0.0) {
            event.getToolTip().add(Component.literal("Appraisal: VALUE TBD")
                    .withStyle(ChatFormatting.DARK_GRAY));
            event.getToolTip().add(Component.literal("Cannot Divest or Vault")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        double stack = AvariceAppraisals.stackValue(event.getItemStack());
        event.getToolTip().add(Component.literal(String.format("Appraisal: %.2f Ava each", each))
                .withStyle(ChatFormatting.GOLD));
        event.getToolTip().add(Component.literal("Quantity: " + event.getItemStack().getCount())
                .withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.literal(String.format("Total: %.2f Ava", stack))
                .withStyle(ChatFormatting.DARK_GREEN));
        event.getToolTip().add(Component.literal("Asset Tier: "
                        + AvariceAppraisals.tier(event.getItemStack()).displayName())
                .withStyle(ChatFormatting.DARK_GREEN));
    }
}
