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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GluttonyMod.MOD_ID)
public final class Beelzebub {
    public static final int UNLOCK_LEVEL = 100;
    private static final String ACTIVE = "RootsOfSinBeelzebubActive";
    private static final double RADIUS = 8.0;
    private static final double SOUL_COST_PER_PULSE = 1.0;
    private static final float DAMAGE_PER_PULSE = 2.0F;
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

        player.serverLevel().sendParticles(ParticleTypes.SCULK_SOUL, player.getX(), player.getY() + 0.8,
                player.getZ(), 28, RADIUS * 0.45, 1.5, RADIUS * 0.45, 0.02);
        player.serverLevel().sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 0.5,
                player.getZ(), 12, RADIUS * 0.35, 1.0, RADIUS * 0.35, 0.01);

        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(RADIUS), entity -> entity != player && entity.isAlive()
                        && !entity.isSpectator() && player.hasLineOfSight(entity))) {
            float before = target.getHealth();
            target.getPersistentData().putBoolean(SoulSiphon.SIPHON_DAMAGE_TAG, true);
            boolean hurt = target.hurt(player.damageSources().indirectMagic(player, player), DAMAGE_PER_PULSE);
            target.getPersistentData().remove(SoulSiphon.SIPHON_DAMAGE_TAG);
            if (!hurt) continue;
            double dealt = Math.max(0.0, before - target.getHealth());
            data.addSouls(dealt * SOULS_PER_DAMAGE);
            Devour.extractUniqueStats(player, target, 0.05);
            SoulSiphon.spawnSoulTrail(player.serverLevel(), target, player);
        }
        AbilityHudSync.send(player);
    }

    private static void stop(ServerPlayer player, String message) {
        player.getPersistentData().putBoolean(ACTIVE, false);
        player.displayClientMessage(Component.literal(message).withStyle(ChatFormatting.GRAY), true);
        AbilityHudSync.send(player);
    }
}
