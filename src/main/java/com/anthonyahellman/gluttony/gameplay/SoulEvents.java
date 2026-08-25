package com.anthonyahellman.gluttony.gameplay;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.anthonyahellman.gluttony.data.GluttonyData;
import com.anthonyahellman.gluttony.data.PrideData;
import com.anthonyahellman.gluttony.data.SinData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = GluttonyMod.MOD_ID)
public final class SoulEvents {
    private static final UUID HEALTH_ID = UUID.fromString("96d159ba-8eef-47e4-8357-090d8d75dde1");
    private static final UUID ATTACK_ID = UUID.fromString("9b0595c0-e7c8-45cb-ac37-1b999304586e");

    private SoulEvents() {}

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        LivingEntity victim = event.getEntity();
        if (victim instanceof ServerPlayer) return;
        if (SinData.selected(player) != SinData.NaturalSin.GLUTTONY) return;

        GluttonyData data = GluttonyData.of(player);
        if (!data.active()) return;

        boolean siphonKill = victim.getPersistentData().getBoolean(SoulSiphon.SIPHON_DAMAGE_TAG);
        int oldLevel = data.level();
        double soulsGained = 0.0;

        // Siphon/Devour award their own souls during the cast. Their killing blow
        // still stabilizes awakening and produces the soul trail, but does not
        // double-dip the normal kill reward or full kill-stat extraction.
        if (!siphonKill) {
            double maxHealth = attributeValueOrZero(victim, Attributes.MAX_HEALTH);
            double armor = attributeValueOrZero(victim, Attributes.ARMOR);
            double attack = attributeValueOrZero(victim, Attributes.ATTACK_DAMAGE);
            soulsGained = Math.max(1.0, maxHealth * 0.5 + armor * 0.75) * GluttonyExtraction.soulMultiplier(data.level());
            double fraction = GluttonyExtraction.statFraction(data.level());

            data.addSouls(soulsGained);
            data.addExtractedStats(maxHealth * fraction, Math.max(0, attack) * fraction);
            applyAttributes(player, data);
        }

        SoulSiphon.spawnSoulTrail(player.serverLevel(), victim, player);

        if (data.awakening()) {
            data.stabilize();
            player.getFoodData().setFoodLevel(Math.max(6, player.getFoodData().getFoodLevel()));
            player.displayClientMessage(Component.literal("Gluttony has tasted its first soul.").withStyle(ChatFormatting.DARK_RED), false);
            player.displayClientMessage(Component.literal("The hunger settles—but it will never leave.").withStyle(ChatFormatting.GRAY), false);
        } else if (!siphonKill && soulsGained > 0.0) {
            player.displayClientMessage(Component.literal(String.format("Consumed %s  +%.2f souls",
                    victim.getName().getString(), soulsGained)).withStyle(ChatFormatting.DARK_PURPLE), true);
        }

        int newLevel = data.level();
        if (newLevel > oldLevel) {
            player.displayClientMessage(Component.literal("GLUTTONY LEVEL " + newLevel).withStyle(ChatFormatting.GOLD), false);
            announceUnlocks(player, oldLevel, newLevel);
        }
    }

    private static void announceUnlocks(ServerPlayer player, int oldLevel, int newLevel) {
        if (oldLevel < SoulSiphon.UNLOCK_LEVEL && newLevel >= SoulSiphon.UNLOCK_LEVEL) {
            player.displayClientMessage(Component.literal("SOUL SIPHON AWAKENED").withStyle(ChatFormatting.DARK_PURPLE), false);
            player.displayClientMessage(Component.literal("Your hunger can now tear at a living soul directly.").withStyle(ChatFormatting.GRAY), false);
        }
        if (oldLevel < Devour.UNLOCK_LEVEL && newLevel >= Devour.UNLOCK_LEVEL) {
            player.displayClientMessage(Component.literal("SOUL SIPHON HAS BECOME DEVOUR").withStyle(ChatFormatting.DARK_RED), false);
            player.displayClientMessage(Component.literal("Flesh, soul, strength—Gluttony no longer distinguishes between them.").withStyle(ChatFormatting.GRAY), false);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!(event.getOriginal() instanceof ServerPlayer oldPlayer) || !(event.getEntity() instanceof ServerPlayer newPlayer)) return;
        event.getOriginal().reviveCaps();
        GluttonyData.copy(oldPlayer, newPlayer);
        PrideData.copy(oldPlayer, newPlayer);
        SinData.copy(oldPlayer, newPlayer);
        event.getOriginal().invalidateCaps();
        applyAttributes(newPlayer, GluttonyData.of(newPlayer));
        PrideProgression.applyAttributes(newPlayer);
        AbilityHudSync.send(newPlayer);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            applyAttributes(player, GluttonyData.of(player));
            PrideProgression.applyAttributes(player);
            AbilityHudSync.send(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            applyAttributes(player, GluttonyData.of(player));
            PrideProgression.applyAttributes(player);
            AbilityHudSync.send(player);
        }
    }

    public static void refreshAttributes(ServerPlayer player) {
        applyAttributes(player, GluttonyData.of(player));
        PrideProgression.applyAttributes(player);
    }

    private static void applyAttributes(ServerPlayer player, GluttonyData data) {
        boolean gluttony = SinData.selected(player) == SinData.NaturalSin.GLUTTONY;
        setModifier(player.getAttribute(Attributes.MAX_HEALTH), HEALTH_ID, "Gluttony consumed health",
                gluttony ? data.extractedHealth() : 0.0);
        setModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_ID, "Gluttony consumed attack",
                gluttony ? data.extractedAttack() : 0.0);
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static void setModifier(AttributeInstance attribute, UUID id, String name, double amount) {
        if (attribute == null) return;
        AttributeModifier old = attribute.getModifier(id);
        if (old != null) attribute.removeModifier(old);
        if (amount > 0) attribute.addPermanentModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.ADDITION));
    }

    private static double attributeValueOrZero(LivingEntity entity, net.minecraft.world.entity.ai.attributes.Attribute attribute) {
        AttributeInstance instance = entity.getAttribute(attribute);
        return instance == null ? 0.0 : instance.getValue();
    }
}
