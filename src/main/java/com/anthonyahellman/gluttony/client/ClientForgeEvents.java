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
    private static boolean abilityHeld;
    private static int abilityHeldTicks;
    private ClientForgeEvents() {}

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        // Keep Gluttony's short-lived VFX on the same proven client tick path as input/HUD.
        // This avoids relying on separate annotation-discovered subscribers for each effect.
        SoulSiphonVfxClient.tick();
        DevourVfxClient.tick();
        BeelzebubVfxClient.tick();
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
        boolean down = ClientModEvents.SIN_ABILITY.isDown();
        if (down && !abilityHeld) {
            abilityHeld = true;
            abilityHeldTicks = 0;
            if (minecraft.screen instanceof PouchOfMammonScreen) minecraft.player.closeContainer();
            else if (minecraft.screen == null) {
                ModNetwork.CHANNEL.sendToServer(new SinAbilityPacket(SinAbilityPacket.PRESS, 0));
            }
        } else if (down) {
            abilityHeldTicks++;
            if (abilityHeldTicks % 5 == 0) {
                ModNetwork.CHANNEL.sendToServer(new SinAbilityPacket(
                        SinAbilityPacket.HOLD, abilityHeldTicks));
            }
        } else if (abilityHeld) {
            if (minecraft.screen == null) {
                ModNetwork.CHANNEL.sendToServer(new SinAbilityPacket(
                        SinAbilityPacket.RELEASE, abilityHeldTicks));
            }
            abilityHeld = false;
            abilityHeldTicks = 0;
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
        AvariceAppraisals.Inspection appraisal = AvariceAppraisals.inspectClient(event.getItemStack());
        if (!appraisal.appraised()) {
            event.getToolTip().add(Component.literal("Appraisal: VALUE TBD")
                    .withStyle(ChatFormatting.DARK_GRAY));
            if (appraisal.resourceFamily() != null) {
                event.getToolTip().add(Component.literal("Resource Family: "
                                + appraisal.resourceFamily().displayName())
                        .withStyle(ChatFormatting.YELLOW));
            }
            if (appraisal.resourceFamilyPath() != null) {
                event.getToolTip().add(Component.literal("Family Path: "
                                + appraisal.resourceFamilyPath())
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            event.getToolTip().add(Component.literal("Reason: " + appraisal.unresolvedReason())
                    .withStyle(ChatFormatting.DARK_GRAY));
            event.getToolTip().add(Component.literal("Cannot Divest or Vault")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        double each = appraisal.value();
        double stack = AvariceAppraisals.clientStackValue(event.getItemStack());
        event.getToolTip().add(Component.literal(String.format("Appraisal: %.2f Ava each", each))
                .withStyle(ChatFormatting.GOLD));
        event.getToolTip().add(Component.literal("Quantity: " + event.getItemStack().getCount())
                .withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.literal(String.format("Total: %.2f Ava", stack))
                .withStyle(ChatFormatting.DARK_GREEN));
        event.getToolTip().add(Component.literal("Source: " + appraisal.source().displayName())
                .withStyle(ChatFormatting.GRAY));
        if (appraisal.resourceFamily() != null) {
            event.getToolTip().add(Component.literal("Resource Family: "
                            + appraisal.resourceFamily().displayName())
                    .withStyle(ChatFormatting.YELLOW));
        }
        if (appraisal.resourceFamilyPath() != null) {
            event.getToolTip().add(Component.literal("Family Path: "
                            + appraisal.resourceFamilyPath())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        if (appraisal.recipeId() != null) {
            event.getToolTip().add(Component.literal("Recipe: " + appraisal.recipeId())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        event.getToolTip().add(Component.literal("Asset Tier: " + appraisal.tier().displayName())
                .withStyle(ChatFormatting.DARK_GREEN));
    }
}
