package com.anthonyahellman.gluttony.gameplay;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.anthonyahellman.gluttony.data.GluttonyData;
import com.anthonyahellman.gluttony.data.SinData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GluttonyMod.MOD_ID)
public final class Beelzebub {
    public static final int UNLOCK_LEVEL = 100;
    private static final String ACTIVE = "RootsOfSinBeelzebubActive";
    private static final String ACTIVATION = "RootsOfSinBeelzebubActivation";
    private static final double BASE_RADIUS = 6.0;
    private static final double HEALTH_RADIUS_FACTOR = 1.25;
    private static final double TECHNICAL_RADIUS_LIMIT = 512.0;
    private static final double FIRST_BITE_MAX_HP_FRACTION = 0.10;
    private static final double ATTACK_DAMAGE_FACTOR = 0.25;
    private static final double EXTRACTED_HEALTH_DAMAGE_FACTOR = 0.35;
    private static final double SOUL_COST_PER_PULSE = 1.0;
    private static final double SOULS_PER_DAMAGE = 0.5;

    private Beelzebub() {}

    public static boolean active(ServerPlayer player) {
        return player.getPersistentData().getBoolean(ACTIVE);
    }

    public static void toggle(ServerPlayer player) {
        if (SinData.selected(player) != SinData.NaturalSin.GLUTTONY
                || !GluttonyData.of(player).active() || GluttonyData.of(player).level() < UNLOCK_LEVEL) return;
        boolean enabled = !active(player);
        player.getPersistentData().putBoolean(ACTIVE, enabled);
        if (enabled) player.getPersistentData().putLong(ACTIVATION,
                player.getPersistentData().getLong(ACTIVATION) + 1L);
        player.level().playSound(null, player.blockPosition(),
                enabled ? SoundEvents.WITHER_SPAWN : SoundEvents.WITHER_DEATH,
                SoundSource.PLAYERS, enabled ? 0.35F : 0.25F, enabled ? 0.65F : 1.35F);
        player.displayClientMessage(Component.literal(enabled
                ? "BEELZEBUB — THE CLOUD OF DESPAIR OPENS"
                : "Beelzebub recedes.").withStyle(enabled ? ChatFormatting.DARK_RED : ChatFormatting.GRAY), true);
        AbilityHudSync.send(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player) || !active(player)) return;

        GluttonyData data = GluttonyData.of(player);
        if (!player.isAlive() || player.isSpectator() || !data.active()
                || data.level() < UNLOCK_LEVEL || SinData.selected(player) != SinData.NaturalSin.GLUTTONY) {
            stop(player, "Beelzebub collapses.");
            return;
        }
        if (player.tickCount % 10 != 0) return;
        if (!data.spendSouls(SOUL_COST_PER_PULSE)) {
            stop(player, "Beelzebub starves—the soul reserve is empty.");
            return;
        }

        double radius = radius(player);

        player.serverLevel().sendParticles(ParticleTypes.SCULK_SOUL, player.getX(), player.getY() + 0.8,
                player.getZ(), 28, radius * 0.45, radius * 0.45, radius * 0.45, 0.02);
        player.serverLevel().sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 0.5,
                player.getZ(), 12, radius * 0.35, radius * 0.35, radius * 0.35, 0.01);

        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(radius), entity -> entity != player && entity.isAlive()
                        && !entity.isSpectator() && entity.distanceToSqr(player) <= radius * radius
                        && data.allowsTarget(GluttonyData.Ability.BEELZEBUB, entity))) {
            long activation = player.getPersistentData().getLong(ACTIVATION);
            String firstBiteKey = "RootsOfSinBeelzebubFirstBite_" + player.getStringUUID();
            boolean firstBite = target.getPersistentData().getLong(firstBiteKey) != activation;
            float before = target.getHealth();
            target.getPersistentData().putBoolean(SoulSiphon.SIPHON_DAMAGE_TAG, true);
            float damage = firstBite
                    ? target.getMaxHealth() * (float)FIRST_BITE_MAX_HP_FRACTION
                    : (float)(1.0 + player.getAttributeValue(Attributes.ATTACK_DAMAGE) * ATTACK_DAMAGE_FACTOR
                    + Math.sqrt(Math.max(0.0, data.extractedHealth())) * EXTRACTED_HEALTH_DAMAGE_FACTOR);
            boolean hurt = target.hurt(player.damageSources().indirectMagic(player, player), damage);
            target.getPersistentData().remove(SoulSiphon.SIPHON_DAMAGE_TAG);
            if (!hurt) continue;
            if (firstBite) target.getPersistentData().putLong(firstBiteKey, activation);
            double dealt = Math.max(0.0, before - target.getHealth());
            data.addSouls(dealt * SOULS_PER_DAMAGE);
            Devour.extractUniqueStats(player, target, 0.05);
            SoulSiphon.spawnSoulTrail(player.serverLevel(), target, player);
        }
        AbilityHudSync.send(player);
    }

    public static double radius(ServerPlayer player) {
        double growth = Math.max(0.0, player.getMaxHealth() - 20.0);
        double radius = BASE_RADIUS + HEALTH_RADIUS_FACTOR * Math.sqrt(growth);
        if (!Double.isFinite(radius)) return BASE_RADIUS;
        return Math.min(TECHNICAL_RADIUS_LIMIT, radius);
    }

    private static void stop(ServerPlayer player, String message) {
        player.getPersistentData().putBoolean(ACTIVE, false);
        player.displayClientMessage(Component.literal(message).withStyle(ChatFormatting.GRAY), true);
        AbilityHudSync.send(player);
    }
}
