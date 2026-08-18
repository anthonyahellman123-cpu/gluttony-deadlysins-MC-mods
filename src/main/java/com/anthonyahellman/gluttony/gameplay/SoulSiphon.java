package com.anthonyahellman.gluttony.gameplay;

import com.anthonyahellman.gluttony.data.GluttonyData;
import com.anthonyahellman.gluttony.data.SinData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

public final class SoulSiphon {
    public static final String SIPHON_DAMAGE_TAG = "DemonsBountySoulSiphonDamage";
    public static final int UNLOCK_LEVEL = 10;
    private static final double RANGE = 16.0;
    private static final float DAMAGE_PER_BURST = 4.0F;
    private static final double SOULS_PER_DAMAGE = 0.5;
    private static final double TARGET_CONE_DOT = 0.966; // roughly 15 degrees
    private static final double CLOSE_TARGET_CONE_DOT = 0.35;
    private static final double CLOSE_TARGET_RANGE = 3.0;
    private static final long TARGET_RESISTANCE_TICKS = 80L;
    private static final String RESISTANCE_UNTIL_TAG = "DemonsBountySiphonResistanceUntil";
    private static final String FEEDBACK_TICK_TAG = "DemonsBountySiphonFeedbackTick";

    private SoulSiphon() {}

    public static void tryCast(ServerPlayer player) {
        if (SinData.selected(player) != SinData.NaturalSin.GLUTTONY) {
            feedback(player, "Gluttony is dormant—consume the Cursed Apple first.", ChatFormatting.DARK_GRAY);
            return;
        }
        GluttonyData data = GluttonyData.of(player);
        if (!data.active()) {
            feedback(player, "Gluttony is dormant—consume the Cursed Apple first.", ChatFormatting.DARK_GRAY);
            return;
        }
        if (data.level() < UNLOCK_LEVEL) {
            feedback(player, "Soul Siphon requires Gluttony level 10.", ChatFormatting.RED);
            return;
        }
        if (!player.isAlive() || player.isSpectator()) return;

        LivingEntity target = findTarget(player);
        if (target == null) {
            feedback(player, "Aim at one living creature within 16 blocks.", ChatFormatting.GRAY);
            return;
        }

        if (data.level() >= Devour.UNLOCK_LEVEL) {
            Devour.tryConsume(player, target, false);
            return;
        }

        long gameTime = player.level().getGameTime();
        long resistantUntil = target.getPersistentData().getLong(RESISTANCE_UNTIL_TAG);
        if (gameTime < resistantUntil) {
            double seconds = (resistantUntil - gameTime) / 20.0;
            player.displayClientMessage(Component.literal(String.format("That soul resists for %.1fs", seconds))
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        float healthBefore = target.getHealth();
        target.getPersistentData().putBoolean(SIPHON_DAMAGE_TAG, true);
        boolean hurt = target.hurt(player.damageSources().indirectMagic(player, player), DAMAGE_PER_BURST);
        target.getPersistentData().remove(SIPHON_DAMAGE_TAG);
        if (!hurt) {
            feedback(player, "That soul resists the siphon.", ChatFormatting.RED);
            return;
        }

        double damageDealt = Math.max(0.0, healthBefore - target.getHealth());
        if (damageDealt > 0.0) data.addSouls(damageDealt * SOULS_PER_DAMAGE);
        target.getPersistentData().putLong(RESISTANCE_UNTIL_TAG, gameTime + TARGET_RESISTANCE_TICKS);
        spawnSoulTrail((ServerLevel) player.level(), target, player);
        player.displayClientMessage(Component.literal(String.format("Soul ripped from %s  +%.2f souls",
                target.getName().getString(), damageDealt * SOULS_PER_DAMAGE)).withStyle(ChatFormatting.DARK_PURPLE), true);
    }

    static LivingEntity findTarget(ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F).normalize();
        AABB search = player.getBoundingBox().expandTowards(look.scale(RANGE)).inflate(1.0);
        return player.level().getEntitiesOfClass(LivingEntity.class, search,
                        entity -> isValidTarget(player, entity) && isInsideAimCone(start, look, entity))
                .stream()
                .min(Comparator.comparingDouble(entity -> targetScore(start, look, entity)))
                .orElse(null);
    }

    private static boolean isValidTarget(ServerPlayer player, Entity entity) {
        if (!(entity instanceof LivingEntity living) || entity == player) return false;
        return living.isAlive() && !living.isSpectator() && player.hasLineOfSight(living);
    }

    private static boolean isInsideAimCone(Vec3 start, Vec3 look, LivingEntity entity) {
        Vec3 toward = entity.getBoundingBox().getCenter().subtract(start);
        double distance = toward.length();
        double requiredDot = distance <= CLOSE_TARGET_RANGE ? CLOSE_TARGET_CONE_DOT : TARGET_CONE_DOT;
        return distance <= RANGE && toward.normalize().dot(look) >= requiredDot;
    }

    private static double targetScore(Vec3 start, Vec3 look, LivingEntity entity) {
        Vec3 toward = entity.getBoundingBox().getCenter().subtract(start);
        double alignmentPenalty = 1.0 - toward.normalize().dot(look);
        return alignmentPenalty * 100.0 + toward.length() / RANGE;
    }

    public static void spawnSoulTrail(ServerLevel level, LivingEntity target, ServerPlayer player) {
        Vec3 from = target.getBoundingBox().getCenter();
        Vec3 to = player.getEyePosition().add(0.0, -0.35, 0.0);
        for (int i = 0; i <= 12; i++) {
            Vec3 point = from.lerp(to, i / 12.0);
            level.sendParticles(ParticleTypes.SOUL, point.x, point.y, point.z, 2, 0.04, 0.04, 0.04, 0.02);
        }
    }

    private static void feedback(ServerPlayer player, String message, ChatFormatting color) {
        int lastFeedback = player.getPersistentData().getInt(FEEDBACK_TICK_TAG);
        if (player.tickCount - lastFeedback >= 20) {
            player.getPersistentData().putInt(FEEDBACK_TICK_TAG, player.tickCount);
            player.displayClientMessage(Component.literal(message).withStyle(color), true);
        }
    }
}
