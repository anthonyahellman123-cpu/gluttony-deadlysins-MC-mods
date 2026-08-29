package com.anthonyahellman.gluttony.gameplay;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.anthonyahellman.gluttony.data.PrideData;
import com.anthonyahellman.gluttony.data.SinData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = GluttonyMod.MOD_ID)
public final class PrideAbility {
    public static final int UNLOCK_KILLS = 4;
    public static final String ABILITY_STRIKE_TAG = "RootsOfSinPrideAbilityStrike";
    public static final String SLAM_DAMAGE_TAG = "RootsOfSinPrideSlamDamage";
    private static final String FALL_ACTIVE = "RootsOfSinLucifersFallActive";
    private static final String FALL_START_Y = "RootsOfSinLucifersFallStartY";
    private static final String CAST_STAGE = "RootsOfSinLucifersFallCastStage";
    private static final String CHARGE_TICKS = "RootsOfSinLucifersFallCharge";
    private static final String GROUNDED_UNTIL = "RootsOfSinPrideGroundedUntil";
    private static final String HEALING_SUPPRESSED_UNTIL = "RootsOfSinPrideHealingSuppressedUntil";
    private static final int TICKS_PER_STAGE = 1200;
    private static final int MAX_CHARGE_TICKS = 6000;
    private static final List<Wave> WAVES = new ArrayList<>();

    private PrideAbility() {}

    public static void tryCast(ServerPlayer player) {
        if (SinData.selected(player) != SinData.NaturalSin.PRIDE) {
            return;
        }
        if (PrideData.of(player).totalBossKills() < UNLOCK_KILLS) {
            message(player, "Lucifer's Fall unlocks after four conquered bosses.", ChatFormatting.GOLD);
            return;
        }
        if (player.onGround()) {
            message(player, "Lucifer's Fall can only begin while airborne.", ChatFormatting.GRAY);
            return;
        }
        if (player.getPersistentData().getBoolean(FALL_ACTIVE)) return;
        int stage = chargeStage(player);
        player.getPersistentData().putInt(CAST_STAGE, stage);
        player.getPersistentData().putInt(CHARGE_TICKS, 0);
        player.getPersistentData().putBoolean(FALL_ACTIVE, true);
        player.getPersistentData().putDouble(FALL_START_Y, player.getY());
        player.fallDistance = 0.0F;
        Vec3 initial = player.getDeltaMovement();
        player.setDeltaMovement(initial.x * 0.65, Math.min(-1.6, initial.y - 1.25), initial.z * 0.65);
        player.hurtMarked = true;
        player.displayClientMessage(Component.literal("LUCIFER'S FALL — STAGE " + roman(stage))
                .withStyle(ChatFormatting.GOLD), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS, 0.8F, 0.55F + stage * 0.05F);
        AbilityHudSync.send(player);
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 == 0) AbilityHudSync.send(player);
        if (SinData.selected(player) == SinData.NaturalSin.PRIDE
                && !player.getPersistentData().getBoolean(FALL_ACTIVE)) {
            int charge = chargeTicks(player);
            if (charge < MAX_CHARGE_TICKS) player.getPersistentData().putInt(CHARGE_TICKS, charge + 1);
        }
        if (!player.getPersistentData().getBoolean(FALL_ACTIVE)) return;

        player.fallDistance = 0.0F;
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x * 0.78, Math.max(-8.0, motion.y - 0.55), motion.z * 0.78);
        player.hurtMarked = true;
        if (player.onGround() || !player.level().noCollision(player,
                player.getBoundingBox().move(0.0, -0.18, 0.0))) impact(player);
    }

    private static void impact(ServerPlayer player) {
        var tag = player.getPersistentData();
        int stage = Math.min(5, tag.getInt(CAST_STAGE));
        double distance = Math.max(0.0, tag.getDouble(FALL_START_Y) - player.getY());
        tag.remove(FALL_ACTIVE);
        tag.remove(FALL_START_Y);
        tag.remove(CAST_STAGE);
        player.fallDistance = 0.0F;

        // Height and stored charge are independent multipliers applied after each
        // percentage basis has been calculated from the target's live health state.
        double heightScale = 1.0 + Math.min(2.0, distance / 60.0);
        double chargeScale = 1.0 + stage * 0.16;
        double scale = heightScale * chargeScale;
        double radius = Math.min(192.0, 6.0 + distance * 0.45 + stage * 4.0);
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 0.2, player.getZ(),
                4 + stage, 0.7, 0.12, 0.7, 0.0);
        level.playSound(null, player.blockPosition(), SoundEvents.RAVAGER_ROAR,
                SoundSource.PLAYERS, 1.7F, 0.48F);

        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(2.5), e -> e != player && e.isAlive() && !e.isSpectator())) {
            // Component 1: 25% maximum HP plus half of Pride's live Attack Damage.
            double attackContribution = player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.50;
            hurt(player, target, (float)((target.getMaxHealth() * 0.25F + attackContribution) * scale));
            applyTrials(player, target);
        }
        if (stage >= 1) {
            // Component 2: the normal expanding aftershock — 25% of the HP
            // missing after the landing dome has resolved.
            addWave(player, radius, 4L, 0, scale);
            if (PrideData.of(player).complete(PrideData.Trial.WARDEN)) {
                // Components 3 and 4: Warden-unlocked missing-HP echoes.
                addWave(player, radius * 0.72, 12L, 1, scale);
                addWave(player, radius * 0.48, 20L, 2, scale);
            }
        }
        message(player, String.format("Lucifer's Fall: %.1f blocks — Stage %s", distance, roman(stage)),
                ChatFormatting.GOLD);
    }

    private static void addWave(ServerPlayer player, double radius, long delay, int index, double scale) {
        WAVES.add(new Wave(player.serverLevel(), player.getUUID(), player.position(), radius,
                player.level().getGameTime() + delay, index, scale));
    }

    @SubscribeEvent
    public static void levelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        Iterator<Wave> iterator = WAVES.iterator();
        while (iterator.hasNext()) {
            Wave wave = iterator.next();
            if (wave.level != level || now < wave.nextTick) continue;
            wave.radius = Math.min(wave.maxRadius, wave.radius + Math.max(1.4, wave.maxRadius / 12.0));
            wave.nextTick = now + 4L;
            renderHalo(level, wave);
            Entity source = level.getEntity(wave.playerId);
            if (source instanceof ServerPlayer player) {
                AABB area = new AABB(wave.origin, wave.origin).inflate(wave.radius, 2.5, wave.radius);
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                        e -> e != player && e.isAlive() && !e.isSpectator() && !wave.hit.contains(e.getUUID()))) {
                    double horizontal = Math.hypot(target.getX() - wave.origin.x, target.getZ() - wave.origin.z);
                    if (horizontal > wave.radius) continue;
                    wave.hit.add(target.getUUID());
                    float fraction = wave.index == 0 ? 0.25F : wave.index == 1 ? 0.10F : 0.05F;
                    float basis = Math.max(0.0F, target.getMaxHealth() - target.getHealth());
                    hurt(player, target, (float)(basis * fraction * wave.scale));
                    applyTrials(player, target);
                }
            }
            if (wave.radius >= wave.maxRadius) iterator.remove();
        }
    }

    private static void renderHalo(ServerLevel level, Wave wave) {
        int points = Math.min(192, Math.max(24, (int)Math.ceil(wave.radius * 4.0)));
        for (int point = 0; point < points; point++) {
            double angle = Math.PI * 2.0 * point / points;
            double x = wave.origin.x + Math.cos(angle) * wave.radius;
            double z = wave.origin.z + Math.sin(angle) * wave.radius;
            if (wave.index == 0) {
                level.sendParticles(point % 3 == 0 ? ParticleTypes.WAX_ON : ParticleTypes.END_ROD,
                        x, wave.origin.y + 0.22, z, 1, 0.015, 0.025, 0.015, 0.0);
            } else {
                level.sendParticles(point % 3 == 0 ? ParticleTypes.SCULK_SOUL : ParticleTypes.SOUL_FIRE_FLAME,
                        x, wave.origin.y + 0.22, z, 1, 0.015, 0.025, 0.015, 0.0);
            }
        }
    }

    private static void hurt(ServerPlayer player, LivingEntity target, float damage) {
        if (damage <= 0.0F) return;
        player.getPersistentData().putBoolean(ABILITY_STRIKE_TAG, true);
        player.getPersistentData().putBoolean(SLAM_DAMAGE_TAG, true);
        target.invulnerableTime = 0;
        target.hurt(player.damageSources().playerAttack(player), damage);
        player.getPersistentData().remove(ABILITY_STRIKE_TAG);
        player.getPersistentData().remove(SLAM_DAMAGE_TAG);
    }

    private static void applyTrials(ServerPlayer player, LivingEntity target) {
        PrideData data = PrideData.of(player);
        if (data.complete(PrideData.Trial.ENDER_DRAGON)) {
            target.getPersistentData().putLong(GROUNDED_UNTIL, player.level().getGameTime() + 100L);
            target.setDeltaMovement(target.getDeltaMovement().x, -0.8, target.getDeltaMovement().z);
            target.hurtMarked = true;
        }
        if (data.complete(PrideData.Trial.WITHER)) {
            target.getPersistentData().putLong(HEALING_SUPPRESSED_UNTIL, player.level().getGameTime() + 120L);
            target.removeEffect(net.minecraft.world.effect.MobEffects.REGENERATION);
        }
        if (data.complete(PrideData.Trial.ELDER_GUARDIAN) && target.isInWater()) {
            Vec3 pull = player.position().subtract(target.position());
            if (pull.lengthSqr() > 0.01) target.setDeltaMovement(pull.normalize().scale(1.15));
            target.hurtMarked = true;
        }
    }

    @SubscribeEvent
    public static void heal(LivingHealEvent event) {
        if (event.getEntity().level().getGameTime()
                < event.getEntity().getPersistentData().getLong(HEALING_SUPPRESSED_UNTIL)) {
            event.setAmount(event.getAmount() * 0.25F);
        }
    }

    @SubscribeEvent
    public static void livingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || entity.level().getGameTime()
                >= entity.getPersistentData().getLong(GROUNDED_UNTIL)) return;
        if (entity instanceof Player p && (p.isCreative() || p.isSpectator())) return;
        Vec3 motion = entity.getDeltaMovement();
        boolean supported = entity.onGround()
                || !entity.level().noCollision(entity, entity.getBoundingBox().move(0.0, -0.45, 0.0));
        if (entity.getType() == EntityType.ENDER_DRAGON) {
            int surface = entity.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    entity.getBlockX(), entity.getBlockZ());
            double safeBottom = surface + 0.5;
            if (entity.getBoundingBox().minY <= safeBottom) {
                entity.setPos(entity.getX(), entity.getY() + Math.max(0.0, safeBottom - entity.getBoundingBox().minY), entity.getZ());
                supported = true;
            }
        }
        double drag = supported ? 0.55 : 0.8;
        entity.setDeltaMovement(motion.x * drag, supported ? 0.0 : Math.min(-0.35, motion.y), motion.z * drag);
        entity.hurtMarked = true;
        entity.fallDistance = 0.0F;
    }

    public static int chargeTicks(ServerPlayer player) {
        return Math.min(MAX_CHARGE_TICKS, Math.max(0, player.getPersistentData().getInt(CHARGE_TICKS)));
    }
    public static int chargeStage(ServerPlayer player) { return chargeTicks(player) / TICKS_PER_STAGE; }
    public static int cooldownRemaining(ServerPlayer player) { return 0; }
    public static int recastRemaining(ServerPlayer player) { return 0; }

    private static String roman(int stage) {
        return switch (stage) { case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V"; default -> "0"; };
    }
    private static void message(ServerPlayer player, String text, ChatFormatting color) {
        player.displayClientMessage(Component.literal(text).withStyle(color), true);
    }

    private static final class Wave {
        final ServerLevel level; final UUID playerId; final Vec3 origin; final double maxRadius;
        final int index; final double scale; final Set<UUID> hit = new HashSet<>();
        double radius; long nextTick;
        Wave(ServerLevel level, UUID playerId, Vec3 origin, double maxRadius, long nextTick, int index, double scale) {
            this.level = level; this.playerId = playerId; this.origin = origin; this.maxRadius = maxRadius;
            this.nextTick = nextTick; this.index = index; this.scale = scale;
        }
    }
}
