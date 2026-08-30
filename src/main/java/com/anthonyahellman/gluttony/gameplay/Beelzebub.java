package com.anthonyahellman.gluttony.gameplay;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.anthonyahellman.gluttony.data.GluttonyData;
import com.anthonyahellman.gluttony.data.SinData;
import com.anthonyahellman.gluttony.network.BeelzebubVfxPacket;
import com.anthonyahellman.gluttony.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

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
    private static final int MAX_VISUAL_TARGETS_PER_PULSE = 32;

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
        List<Integer> visualTargets = new ArrayList<>();

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
            boolean hurt;
            try {
                hurt = target.hurt(player.damageSources().indirectMagic(player, player), damage);
            } finally {
                target.getPersistentData().remove(SoulSiphon.SIPHON_DAMAGE_TAG);
            }
            if (!hurt) continue;
            if (firstBite) target.getPersistentData().putLong(firstBiteKey, activation);
            double dealt = Math.max(0.0, before - target.getHealth());
            data.addSouls(dealt * SOULS_PER_DAMAGE);
            Devour.extractUniqueStats(player, target, 0.05);
            if (visualTargets.size() < MAX_VISUAL_TARGETS_PER_PULSE) {
                visualTargets.add(target.getId());
            }
        }
        if (!visualTargets.isEmpty()) sendVfx(player, visualTargets, radius);
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

    private static void sendVfx(ServerPlayer player, List<Integer> targetIds, double radius) {
        int[] ids = targetIds.stream().mapToInt(Integer::intValue).toArray();
        ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new BeelzebubVfxPacket(player.getId(), (float)radius, ids));
    }
}
