package com.anthonyahellman.gluttony.gameplay;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.anthonyahellman.gluttony.data.GluttonyData;
import com.anthonyahellman.gluttony.data.GreedData;
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

        // Devour awards only what its bite actually consumed; bypass the normal
        // max-health kill extraction path so overkill cannot create phantom gains.
        if (player.getPersistentData().getBoolean(Devour.DEVOUR_DAMAGE_TAG)) return;

        boolean siphonKill = victim.getPersistentData().getBoolean(SoulSiphon.SIPHON_DAMAGE_TAG);
        int oldLevel = data.level();

        // Siphon already awards its bonus souls per damage pulse. Its killing blow
        // stabilizes awakening but deliberately grants no normal souls or stats.
        if (!siphonKill) {
            double maxHealth = attributeValueOrZero(victim, Attributes.MAX_HEALTH);
            double armor = attributeValueOrZero(victim, Attributes.ARMOR);
            double attack = attributeValueOrZero(victim, Attributes.ATTACK_DAMAGE);
            double souls = Math.max(1.0, maxHealth * 0.5 + armor * 0.75) * GluttonyExtraction.soulMultiplier(data.level());
            double fraction = GluttonyExtraction.statFraction(data.level());

            data.addSouls(souls);
            data.addExtractedStats(maxHealth * fraction, Math.max(0, attack) * fraction);
            applyAttributes(player, data);
        }

        if (data.awakening()) {
            data.stabilize();
            player.getFoodData().setFoodLevel(Math.max(6, player.getFoodData().getFoodLevel()));
            player.displayClientMessage(Component.literal("Gluttony has tasted its first soul.").withStyle(ChatFormatting.DARK_RED), false);
            player.displayClientMessage(Component.literal("The hunger settles—but it will never leave.").withStyle(ChatFormatting.GRAY), false);
        }

        if (data.level() > oldLevel) {
            player.displayClientMessage(Component.literal("GLUTTONY LEVEL " + data.level()).withStyle(ChatFormatting.GOLD), false);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!(event.getOriginal() instanceof ServerPlayer oldPlayer) || !(event.getEntity() instanceof ServerPlayer newPlayer)) return;
        event.getOriginal().reviveCaps();
        GluttonyData.copy(oldPlayer, newPlayer);
        GreedData.copy(oldPlayer, newPlayer);
        PrideData.copy(oldPlayer, newPlayer);
        SinData.copy(oldPlayer, newPlayer);
        event.getOriginal().invalidateCaps();
        applyAttributes(newPlayer, GluttonyData.of(newPlayer));
        PrideProgression.applyAttributes(newPlayer);
        GreedProgression.applyAttributes(newPlayer);
        AbilityHudSync.send(newPlayer);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            applyAttributes(player, GluttonyData.of(player));
            PrideProgression.applyAttributes(player);
            GreedProgression.applyAttributes(player);
            AbilityHudSync.send(player);
            AbilityHudSync.sendAppraisals(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            applyAttributes(player, GluttonyData.of(player));
            PrideProgression.applyAttributes(player);
            GreedProgression.applyAttributes(player);
            AbilityHudSync.send(player);
        }
    }

    public static void refreshAttributes(ServerPlayer player) {
        applyAttributes(player, GluttonyData.of(player));
        PrideProgression.applyAttributes(player);
        GreedProgression.applyAttributes(player);
    }

    private static void applyAttributes(ServerPlayer player, GluttonyData data) {
        boolean gluttony = SinData.selected(player) == SinData.NaturalSin.GLUTTONY;
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        setModifier(maxHealth, HEALTH_ID, "Gluttony consumed health",
                gluttony ? data.extractedHealth() : 0.0);
        if (gluttony && maxHealth != null) {
            data.recordHistoricalMaxHealth(maxHealth.getBaseValue() + data.extractedHealth());
        }
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
