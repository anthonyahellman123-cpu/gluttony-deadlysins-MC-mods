package com.anthonyahellman.gluttony.gameplay;

import com.anthonyahellman.gluttony.data.GluttonyData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class SoulSiphon {
    public static final String SIPHON_DAMAGE_TAG = "DemonsBountySoulSiphonDamage";
    public static final int UNLOCK_LEVEL = 10;
    private static final double RANGE = 16.0;
    private static final float DAMAGE_PER_PULSE = 1.0F;
    private static final double SOULS_PER_DAMAGE = 0.5;

    private SoulSiphon() {}

    public static void tryPulse(ServerPlayer player) {
        GluttonyData data = GluttonyData.of(player);
        if (!data.active() || data.level() < UNLOCK_LEVEL || !player.isAlive() || player.isSpectator()) return;

        LivingEntity target = findTarget(player);
        if (target == null) return;

        float healthBefore = target.getHealth();
        target.getPersistentData().putBoolean(SIPHON_DAMAGE_TAG, true);
        boolean hurt = target.hurt(player.damageSources().indirectMagic(player, player), DAMAGE_PER_PULSE);
        target.getPersistentData().remove(SIPHON_DAMAGE_TAG);
        if (!hurt) return;

        double damageDealt = Math.max(0.0, healthBefore - target.getHealth());
        if (damageDealt > 0.0) data.addSouls(damageDealt * SOULS_PER_DAMAGE);
        spawnSoulTrail((ServerLevel) player.level(), target, player);
    }

    private static LivingEntity findTarget(ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = start.add(look.scale(RANGE));
        AABB search = player.getBoundingBox().expandTowards(look.scale(RANGE)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player.level(), player, start, end, search,
                entity -> isValidTarget(player, entity), (float) (RANGE * RANGE)
        );
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    private static boolean isValidTarget(ServerPlayer player, Entity entity) {
        if (!(entity instanceof LivingEntity living) || entity == player) return false;
        return living.isAlive() && !living.isSpectator() && player.hasLineOfSight(living);
    }

    public static void spawnSoulTrail(ServerLevel level, LivingEntity target, ServerPlayer player) {
        Vec3 from = target.getBoundingBox().getCenter();
        Vec3 to = player.getEyePosition().add(0.0, -0.35, 0.0);
        for (int i = 0; i <= 6; i++) {
            Vec3 point = from.lerp(to, i / 6.0);
            level.sendParticles(ParticleTypes.SOUL, point.x, point.y, point.z, 1, 0.03, 0.03, 0.03, 0.01);
        }
    }
}
