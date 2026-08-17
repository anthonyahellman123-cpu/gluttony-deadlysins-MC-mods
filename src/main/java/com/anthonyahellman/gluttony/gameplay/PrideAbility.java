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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = GluttonyMod.MOD_ID)
public final class PrideAbility {
    public static final int UNLOCK_KILLS = 4;
    public static final String ABILITY_STRIKE_TAG = "RootsOfSinPrideAbilityStrike";
    public static final String FOLLOW_UP_STRIKE_TAG = "RootsOfSinPrideFollowUpStrike";

    private static final String DASH_TICKS = "RootsOfSinPrideDashTicks";
    private static final String DASH_DIRECTION_X = "RootsOfSinPrideDashX";
    private static final String DASH_DIRECTION_Y = "RootsOfSinPrideDashY";
    private static final String DASH_DIRECTION_Z = "RootsOfSinPrideDashZ";
    private static final String DASH_FOLLOW_UP = "RootsOfSinPrideDashFollowUp";
    private static final String FOLLOW_UP_TARGET = "RootsOfSinPrideFollowUpTarget";
    private static final String FOLLOW_UP_UNTIL = "RootsOfSinPrideFollowUpUntil";
    private static final String COOLDOWN_UNTIL = "RootsOfSinPrideAbilityCooldown";
    private static final String GROUNDED_UNTIL = "RootsOfSinPrideGroundedUntil";
    private static final String HEALING_SUPPRESSED_UNTIL = "RootsOfSinPrideHealingSuppressedUntil";

    private static final int DASH_DURATION = 8;
    private static final int RECAST_WINDOW = 20;
    private static final int RECOVERY_TICKS = 60;
    private static final double DASH_SPEED = 2.15;

    private PrideAbility() {}

    public static void tryCast(ServerPlayer player) {
        if (SinData.selected(player) != SinData.NaturalSin.PRIDE) {
            SoulSiphon.tryCast(player);
            return;
        }

        PrideData data = PrideData.of(player);
        if (data.totalBossKills() < UNLOCK_KILLS) {
            feedback(player, "Sovereign's Advance unlocks after four conquered bosses.", ChatFormatting.GOLD);
            AbilityHudSync.send(player);
            return;
        }
        if (!player.isAlive() || player.isSpectator()) return;

        long now = player.level().getGameTime();
        if (data.fullyAwakened() && now <= player.getPersistentData().getLong(FOLLOW_UP_UNTIL)
                && player.getPersistentData().hasUUID(FOLLOW_UP_TARGET)) {
            Entity entity = player.serverLevel().getEntity(player.getPersistentData().getUUID(FOLLOW_UP_TARGET));
            if (entity instanceof LivingEntity target && target.isAlive() && target.level() == player.level()) {
                beginDash(player, directionTo(player, target), true);
                player.getPersistentData().remove(FOLLOW_UP_UNTIL);
                AbilityHudSync.send(player);
                return;
            }
        }

        long cooldown = player.getPersistentData().getLong(COOLDOWN_UNTIL);
        if (now < cooldown) {
            feedback(player, String.format("Sovereign's Advance recovers in %.1fs", (cooldown - now) / 20.0),
                    ChatFormatting.GRAY);
            AbilityHudSync.send(player);
            return;
        }

        beginDash(player, player.getLookAngle().normalize(), false);
        player.getPersistentData().putLong(COOLDOWN_UNTIL, now + RECOVERY_TICKS);
        player.displayClientMessage(Component.literal(data.fullyAwakened()
                ? "ABSOLUTE DOMINATION" : "SOVEREIGN'S ADVANCE").withStyle(ChatFormatting.GOLD), true);
        AbilityHudSync.send(player);
    }

    private static void beginDash(ServerPlayer player, Vec3 direction, boolean followUp) {
        if (direction.lengthSqr() < 0.001) return;
        Vec3 normalized = direction.normalize();
        var tag = player.getPersistentData();
        tag.putInt(DASH_TICKS, DASH_DURATION);
        tag.putDouble(DASH_DIRECTION_X, normalized.x);
        tag.putDouble(DASH_DIRECTION_Y, normalized.y);
        tag.putDouble(DASH_DIRECTION_Z, normalized.z);
        tag.putBoolean(DASH_FOLLOW_UP, followUp);
        player.fallDistance = 0.0F;
        player.level().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1,
                SoundSource.PLAYERS, 0.9F, followUp ? 0.75F : 1.0F);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 == 0) AbilityHudSync.send(player);
        int ticks = player.getPersistentData().getInt(DASH_TICKS);
        if (ticks <= 0) return;

        PrideData data = PrideData.of(player);
        boolean followUp = player.getPersistentData().getBoolean(DASH_FOLLOW_UP);
        Vec3 direction = storedDirection(player);
        if (followUp && player.getPersistentData().hasUUID(FOLLOW_UP_TARGET)) {
            Entity entity = player.serverLevel().getEntity(player.getPersistentData().getUUID(FOLLOW_UP_TARGET));
            if (entity instanceof LivingEntity target && target.isAlive()) direction = directionTo(player, target);
        }

        double speed = DASH_SPEED;
        if (player.isInWater() && !data.complete(PrideData.Trial.ELDER_GUARDIAN)) speed *= 0.55;
        player.setDeltaMovement(direction.scale(speed));
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        if (data.complete(PrideData.Trial.WARDEN)) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 5, 1, true, false));
        }

        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(),
                5, 0.2, 0.25, 0.2, 0.02);
        LivingEntity hit = findContact(player);
        if (hit != null) {
            strike(player, hit, followUp, data);
            clearDash(player);
            return;
        }

        player.getPersistentData().putInt(DASH_TICKS, ticks - 1);
        if (ticks == 1) clearDash(player);
    }

    private static void strike(ServerPlayer player, LivingEntity target, boolean followUp, PrideData data) {
        var tag = player.getPersistentData();
        tag.putBoolean(ABILITY_STRIKE_TAG, true);
        tag.putBoolean(FOLLOW_UP_STRIKE_TAG, followUp);
        player.attack(target);
        tag.remove(ABILITY_STRIKE_TAG);
        tag.remove(FOLLOW_UP_STRIKE_TAG);

        if (data.complete(PrideData.Trial.ENDER_DRAGON)) ground(target, player.level().getGameTime() + 100);
        if (data.complete(PrideData.Trial.WITHER)) {
            target.getPersistentData().putLong(HEALING_SUPPRESSED_UNTIL, player.level().getGameTime() + 120);
            target.removeEffect(MobEffects.REGENERATION);
        }
        if (data.complete(PrideData.Trial.ELDER_GUARDIAN) && target.isInWater()) {
            Vec3 pull = player.position().subtract(target.position());
            if (pull.lengthSqr() > 0.01) target.setDeltaMovement(pull.normalize().scale(1.15));
            target.hurtMarked = true;
        }
        if (data.complete(PrideData.Trial.WARDEN)) impact(player, target);

        if (data.fullyAwakened()) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 1));
            if (!followUp && target.isAlive()) {
                tag.putUUID(FOLLOW_UP_TARGET, target.getUUID());
                tag.putLong(FOLLOW_UP_UNTIL, player.level().getGameTime() + RECAST_WINDOW);
                player.displayClientMessage(Component.literal("Press again — I WASN'T FINISHED.")
                        .withStyle(ChatFormatting.YELLOW), true);
                AbilityHudSync.send(player);
            }
        }
    }

    private static void impact(ServerPlayer player, LivingEntity directTarget) {
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.EXPLOSION, directTarget.getX(), directTarget.getY() + 0.5,
                directTarget.getZ(), 2, 0.25, 0.15, 0.25, 0.0);
        for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class,
                directTarget.getBoundingBox().inflate(2.5), entity -> entity != player && entity != directTarget)) {
            Vec3 away = nearby.position().subtract(directTarget.position());
            if (away.lengthSqr() > 0.01) nearby.push(away.x * 0.65, 0.35, away.z * 0.65);
        }
    }

    private static void ground(LivingEntity target, long until) {
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) return;
        target.getPersistentData().putLong(GROUNDED_UNTIL, until);
        target.setDeltaMovement(target.getDeltaMovement().x, -0.8, target.getDeltaMovement().z);
        target.hurtMarked = true;
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        if (entity.level().getGameTime() >= entity.getPersistentData().getLong(GROUNDED_UNTIL)) return;
        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x * 0.8, Math.min(-0.35, motion.y), motion.z * 0.8);
        entity.hurtMarked = true;
        entity.fallDistance = 0.0F;
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (event.getEntity().level().getGameTime()
                < event.getEntity().getPersistentData().getLong(HEALING_SUPPRESSED_UNTIL)) {
            event.setAmount(event.getAmount() * 0.25F);
        }
    }

    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.getPersistentData().getInt(DASH_TICKS) > 0
                && PrideData.of(player).complete(PrideData.Trial.WARDEN)) {
            event.setCanceled(true);
        }
    }

    private static LivingEntity findContact(ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(0.9);
        return player.level().getEntitiesOfClass(LivingEntity.class, area,
                        entity -> entity != player && entity.isAlive() && !entity.isSpectator())
                .stream().min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
    }

    private static Vec3 storedDirection(ServerPlayer player) {
        var tag = player.getPersistentData();
        return new Vec3(tag.getDouble(DASH_DIRECTION_X), tag.getDouble(DASH_DIRECTION_Y),
                tag.getDouble(DASH_DIRECTION_Z)).normalize();
    }

    private static Vec3 directionTo(ServerPlayer player, LivingEntity target) {
        return target.getBoundingBox().getCenter().subtract(player.getEyePosition()).normalize();
    }

    private static void clearDash(ServerPlayer player) {
        if (player.getPersistentData().getBoolean(DASH_FOLLOW_UP)) {
            player.getPersistentData().remove(FOLLOW_UP_TARGET);
        }
        player.getPersistentData().remove(DASH_TICKS);
        player.getPersistentData().remove(DASH_FOLLOW_UP);
    }

    private static void feedback(ServerPlayer player, String message, ChatFormatting color) {
        player.displayClientMessage(Component.literal(message).withStyle(color), true);
    }

    public static int cooldownRemaining(ServerPlayer player) {
        return (int) Math.max(0L, player.getPersistentData().getLong(COOLDOWN_UNTIL)
                - player.level().getGameTime());
    }

    public static int recastRemaining(ServerPlayer player) {
        if (!player.getPersistentData().hasUUID(FOLLOW_UP_TARGET)) return 0;
        return (int) Math.max(0L, player.getPersistentData().getLong(FOLLOW_UP_UNTIL)
                - player.level().getGameTime());
    }
}
