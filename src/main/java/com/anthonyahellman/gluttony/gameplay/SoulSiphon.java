package com.anthonyahellman.gluttony.gameplay;

import com.anthonyahellman.gluttony.data.GluttonyData;
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
    private static final float DAMAGE_PER_PULSE = 1.0F;
    private static final double SOULS_PER_DAMAGE = 0.5;
    private static final double TARGET_CONE_DOT = 0.966; // roughly 15 degrees
    private static final String FEEDBACK_TICK_TAG = "DemonsBountySiphonFeedbackTick";

    private SoulSiphon() {}

    public static void tryPulse(ServerPlayer player) {
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

        float healthBefore = target.getHealth();
        target.getPersistentData().putBoolean(SIPHON_DAMAGE_TAG, true);
        boolean hurt = target.hurt(player.damageSources().indirectMagic(player, player), DAMAGE_PER_PULSE);
        target.getPersistentData().remove(SIPHON_DAMAGE_TAG);
        if (!hurt) {
            feedback(player, "That soul resists the siphon.", ChatFormatting.RED);
            return;
        }

        double damageDealt = Math.max(0.0, healthBefore - target.getHealth());
        if (damageDealt > 0.0) data.addSouls(damageDealt * SOULS_PER_DAMAGE);
        spawnSoulTrail((ServerLevel) player.level(), target, player);
        player.displayClientMessage(Component.literal(String.format("Siphoning %s  +%.2f souls",
                target.getName().getString(), damageDealt * SOULS_PER_DAMAGE)).withStyle(ChatFormatting.DARK_PURPLE), true);
    }

    private static LivingEntity findTarget(ServerPlayer player) {
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
        return toward.lengthSqr() <= RANGE * RANGE && toward.normalize().dot(look) >= TARGET_CONE_DOT;
    }

    private static double targetScore(Vec3 start, Vec3 look, LivingEntity entity) {
        Vec3 toward = entity.getBoundingBox().getCenter().subtract(start);
        double alignmentPenalty = 1.0 - toward.normalize().dot(look);
        return alignmentPenalty * 100.0 + toward.length() / RANGE;
    }

    public static void spawnSoulTrail(ServerLevel level, LivingEntity target, ServerPlayer player) {
        Vec3 from = target.getBoundingBox().getCenter();
        Vec3 to = player.getEyePosition().add(0.0, -0.35, 0.0);
        for (int i = 0; i <= 6; i++) {
            Vec3 point = from.lerp(to, i / 6.0);
            level.sendParticles(ParticleTypes.SOUL, point.x, point.y, point.z, 1, 0.03, 0.03, 0.03, 0.01);
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
