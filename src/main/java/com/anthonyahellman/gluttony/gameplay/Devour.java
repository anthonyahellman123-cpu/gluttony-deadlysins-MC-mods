package com.anthonyahellman.gluttony.gameplay;

import com.anthonyahellman.gluttony.data.GluttonyData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class Devour {
    public static final int UNLOCK_LEVEL = 50;
    private static final float DAMAGE = 8.0F;
    private static final long TARGET_RESISTANCE_TICKS = 80L;

    private Devour() {}

    public static boolean tryConsume(ServerPlayer player, LivingEntity target, boolean ignoreResistance) {
        GluttonyData data = GluttonyData.of(player);
        if (data.level() < UNLOCK_LEVEL || target == null || !target.isAlive()) return false;

        long now = player.level().getGameTime();
        String resistanceKey = "RootsOfSinDevourResistance_" + player.getStringUUID();
        long resistantUntil = target.getPersistentData().getLong(resistanceKey);
        if (!ignoreResistance && now < resistantUntil) {
            player.displayClientMessage(Component.literal(String.format("That body resists Devour for %.1fs",
                    (resistantUntil - now) / 20.0)).withStyle(ChatFormatting.RED), true);
            return false;
        }

        float before = target.getHealth();
        target.getPersistentData().putBoolean(SoulSiphon.SIPHON_DAMAGE_TAG, true);
        boolean hurt = target.hurt(player.damageSources().indirectMagic(player, player), DAMAGE);
        target.getPersistentData().remove(SoulSiphon.SIPHON_DAMAGE_TAG);
        if (!hurt) return false;

        double dealt = Math.max(0.0, before - target.getHealth());
        data.addSouls(dealt);
        target.getPersistentData().putLong(resistanceKey, now + TARGET_RESISTANCE_TICKS);

        String consumedKey = "RootsOfSinDevouredBy_" + player.getStringUUID();
        if (!target.getPersistentData().getBoolean(consumedKey)) {
            double fraction = GluttonyExtraction.statFraction(data.level()) * 0.10;
            data.addExtractedStats(attribute(target, Attributes.MAX_HEALTH) * fraction,
                    Math.max(0.0, attribute(target, Attributes.ATTACK_DAMAGE)) * fraction);
            target.getPersistentData().putBoolean(consumedKey, true);
            SoulEvents.refreshAttributes(player);
        }

        SoulSiphon.spawnSoulTrail(player.serverLevel(), target, player);
        player.displayClientMessage(Component.literal(String.format("DEVOUR  %s  +%.2f souls",
                target.getName().getString(), dealt)).withStyle(ChatFormatting.DARK_RED), true);
        return true;
    }

    private static double attribute(LivingEntity entity, net.minecraft.world.entity.ai.attributes.Attribute attribute) {
        AttributeInstance instance = entity.getAttribute(attribute);
        return instance == null ? 0.0 : instance.getValue();
    }
}
