package com.anthonyahellman.gluttony.gameplay;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.anthonyahellman.gluttony.data.PrideData;
import com.anthonyahellman.gluttony.data.SinData;
import com.anthonyahellman.gluttony.network.ModNetwork;
import com.anthonyahellman.gluttony.network.PrideVfxTestPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
import net.minecraftforge.network.PacketDistributor;

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
        sendNear(player.serverLevel(), player.getX(), player.getY(), player.getZ(), 96.0,
                PrideVfxTestPacket.descent(player.getId(), player.getX(), player.getY(), player.getZ()));
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
            if (charge < PrideFallTuning.MAX_CHARGE_TICKS) {
                player.getPersistentData().putInt(CHARGE_TICKS, charge + 1);
            }
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
        double impactRadius = Math.min(PrideFallTuning.MAX_IMPACT_RADIUS,
                6.0 + distance * 0.45 + stage * 4.0);
        double shockwaveRadius = Math.min(PrideFallTuning.MAX_SHOCKWAVE_RADIUS,
                impactRadius * PrideFallTuning.SHOCKWAVE_RADIUS_MULTIPLIER);
        ServerLevel level = player.serverLevel();
        Vec3 origin = player.position();
        sendNear(level, origin.x, origin.y, origin.z, shockwaveRadius + 48.0,
                PrideVfxTestPacket.impact(player.getId(), origin.x, origin.y, origin.z, impactRadius));
        level.playSound(null, player.blockPosition(), SoundEvents.RAVAGER_ROAR,
                SoundSource.PLAYERS, 1.7F, 0.48F);

        AABB impactArea = new AABB(origin, origin).inflate(impactRadius,
                PrideFallTuning.WAVE_TARGET_HEIGHT, impactRadius);
        double attackContribution = player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.50;
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, impactArea,
                e -> e != player && e.isAlive() && !e.isSpectator())) {
            double horizontal = Math.hypot(target.getX() - origin.x, target.getZ() - origin.z);
            if (horizontal > impactRadius) continue;
            // Component 1: 25% maximum HP plus half of Pride's live Attack Damage.
            hurt(player, target, (float)((target.getMaxHealth() * 0.25F + attackContribution) * scale));
            applyTrials(player, target);
        }
        if (stage >= 1) {
            // Component 2: the normal expanding aftershock — 25% of the HP
            // missing after the landing dome has resolved.
            addWave(player, shockwaveRadius, 4L, 0, scale);
            if (PrideData.of(player).complete(PrideData.Trial.WARDEN)) {
                // Components 3 and 4: Warden-unlocked missing-HP echoes.
                addWave(player, shockwaveRadius * 0.72, 12L, 1, scale);
                addWave(player, shockwaveRadius * 0.48, 20L, 2, scale);
            }
        }
        message(player, String.format("Lucifer's Fall: %.1f blocks — Stage %s", distance, roman(stage)),
                ChatFormatting.GOLD);
    }

    private static void addWave(ServerPlayer player, double radius, long delay, int index, double scale) {
        WAVES.add(new Wave(player.serverLevel(), player.getUUID(), player.position(), radius,
                player.level().getGameTime() + delay, PrideFallTuning.WAVE_DURATION_TICKS, index, scale));
    }

    @SubscribeEvent
    public static void levelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        Iterator<Wave> iterator = WAVES.iterator();
        while (iterator.hasNext()) {
            Wave wave = iterator.next();
            if (wave.level != level || now < wave.startTick) continue;
            if (!wave.visualSent) {
                wave.visualSent = true;
                sendNear(level, wave.origin.x, wave.origin.y, wave.origin.z, wave.maxRadius + 48.0,
                        PrideVfxTestPacket.wave(wave.origin.x, wave.origin.y, wave.origin.z,
                                wave.maxRadius, wave.durationTicks, wave.index, 0));
            }
            long elapsed = now - wave.startTick;
            wave.radius = PrideFallTuning.waveRadius(wave.maxRadius, elapsed + 1.0, wave.durationTicks);
            Entity source = level.getEntity(wave.playerId);
            if (source instanceof ServerPlayer player && now >= wave.nextDamageTick) {
                wave.nextDamageTick = now + PrideFallTuning.WAVE_DAMAGE_SAMPLE_TICKS;
                AABB area = new AABB(wave.origin, wave.origin).inflate(wave.radius,
                        PrideFallTuning.WAVE_TARGET_HEIGHT, wave.radius);
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                        e -> e != player && e.isAlive() && !e.isSpectator() && !wave.hit.contains(e.getUUID()))) {
                    double horizontal = Math.hypot(target.getX() - wave.origin.x, target.getZ() - wave.origin.z);
                    if (horizontal > wave.radius) continue;
                    wave.hit.add(target.getUUID());
                    float fraction = wave.index == 0 ? 0.25F : wave.index == 1 ? 0.10F : 0.05F;
                    float basis = Math.max(0.0F, target.getMaxHealth() - target.getHealth());
                    double attackContribution = wave.index == 0
                            ? player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.50 : 0.0;
                    hurt(player, target, (float)((basis * fraction + attackContribution) * wave.scale));
                    applyTrials(player, target);
                }
            }
            if (elapsed >= wave.durationTicks) iterator.remove();
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
        return Math.min(PrideFallTuning.MAX_CHARGE_TICKS,
                Math.max(0, player.getPersistentData().getInt(CHARGE_TICKS)));
    }
    public static int chargeStage(ServerPlayer player) {
        return Math.min(PrideFallTuning.MAX_STAGE,
                chargeTicks(player) / PrideFallTuning.CHARGE_STAGE_TICKS);
    }
    public static int cooldownRemaining(ServerPlayer player) { return 0; }
    public static int recastRemaining(ServerPlayer player) { return 0; }

    private static String roman(int stage) {
        return switch (stage) { case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V"; default -> "0"; };
    }
    private static void message(ServerPlayer player, String text, ChatFormatting color) {
        player.displayClientMessage(Component.literal(text).withStyle(color), true);
    }

    private static void sendNear(ServerLevel level, double x, double y, double z, double range,
                                 PrideVfxTestPacket packet) {
        double boundedRange = Mth.clamp(range, 64.0, 512.0);
        ModNetwork.CHANNEL.send(PacketDistributor.NEAR.with(() ->
                new PacketDistributor.TargetPoint(x, y, z, boundedRange, level.dimension())), packet);
    }

    private static final class Wave {
        final ServerLevel level; final UUID playerId; final Vec3 origin; final double maxRadius;
        final int durationTicks; final int index; final double scale; final Set<UUID> hit = new HashSet<>();
        double radius; final long startTick; long nextDamageTick; boolean visualSent;
        Wave(ServerLevel level, UUID playerId, Vec3 origin, double maxRadius, long startTick,
             int durationTicks, int index, double scale) {
            this.level = level; this.playerId = playerId; this.origin = origin; this.maxRadius = maxRadius;
            this.startTick = startTick; this.durationTicks = durationTicks; this.index = index; this.scale = scale;
            this.nextDamageTick = startTick;
        }
    }
}
