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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;

@Mod.EventBusSubscriber(modid = GluttonyMod.MOD_ID)
public final class ShadowStep {
    public static final int UNLOCK_LEVEL = 5;
    private static final String TICKS = "RootsOfSinShadowStepTicks";
    private static final String X = "RootsOfSinShadowStepX";
    private static final String Y = "RootsOfSinShadowStepY";
    private static final String Z = "RootsOfSinShadowStepZ";
    private static final String CAST_COST = "RootsOfSinShadowStepCost";
    private static final String LAST_CAST = "RootsOfSinShadowStepLastCast";
    private static final String CHAIN = "RootsOfSinShadowStepChain";
    private static final int DURATION = 7;
    private static final long ESCALATION_WINDOW = 60L;
    private static final double SPEED = 2.0;

    private ShadowStep() {}

    public static void tryCast(ServerPlayer player) {
        if (SinData.selected(player) != SinData.NaturalSin.GLUTTONY || !GluttonyData.of(player).active()) {
            feedback(player, "Gluttony is dormant.", ChatFormatting.DARK_GRAY);
            return;
        }
        GluttonyData data = GluttonyData.of(player);
        if (data.level() < UNLOCK_LEVEL) {
            feedback(player, "Shadow Step requires Gluttony level 5.", ChatFormatting.RED);
            return;
        }
        if (!player.isAlive() || player.isSpectator()) return;

        long now = player.level().getGameTime();
        var tag = player.getPersistentData();
        int chain = now - tag.getLong(LAST_CAST) <= ESCALATION_WINDOW ? tag.getInt(CHAIN) + 1 : 1;
        if (!data.spendSouls(chain)) {
            feedback(player, String.format("Shadow Step needs %d souls (you have %.2f)", chain, data.currentSouls()),
                    ChatFormatting.RED);
            return;
        }

        Vec3 direction = player.getLookAngle().normalize();
        tag.putLong(LAST_CAST, now);
        tag.putInt(CHAIN, chain);
        tag.putInt(CAST_COST, chain);
        tag.putInt(TICKS, DURATION);
        tag.putDouble(X, direction.x);
        tag.putDouble(Y, direction.y);
        tag.putDouble(Z, direction.z);
        player.fallDistance = 0.0F;
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 0.8F, 0.75F);
        player.displayClientMessage(Component.literal(String.format("SHADOW STEP  -%d souls  |  Next: %d",
                chain, chain + 1)).withStyle(ChatFormatting.DARK_PURPLE), true);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)) return;
        int ticks = player.getPersistentData().getInt(TICKS);
        if (ticks <= 0) return;

        var tag = player.getPersistentData();
        Vec3 direction = new Vec3(tag.getDouble(X), tag.getDouble(Y), tag.getDouble(Z)).normalize();
        player.setDeltaMovement(direction.scale(SPEED));
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        player.serverLevel().sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 0.8,
                player.getZ(), 4, 0.18, 0.35, 0.18, 0.02);

        LivingEntity target = findContact(player);
        if (target != null) {
            strike(player, target);
            clear(player);
            return;
        }
        tag.putInt(TICKS, ticks - 1);
        if (ticks == 1) clear(player);
    }

    private static void strike(ServerPlayer player, LivingEntity target) {
        GluttonyData data = GluttonyData.of(player);
        player.attack(target);
        if (data.level() >= Devour.UNLOCK_LEVEL && target.isAlive()) {
            Devour.tryConsume(player, target, data.level() >= 100);
        }
        if (!target.isAlive()) {
            int refund = player.getPersistentData().getInt(CAST_COST);
            data.refundSouls(refund);
            player.displayClientMessage(Component.literal("THE COST IS RETURNED  +" + refund + " souls")
                    .withStyle(ChatFormatting.GOLD), true);
        }
    }

    private static LivingEntity findContact(ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(0.9);
        return player.level().getEntitiesOfClass(LivingEntity.class, area,
                        entity -> entity != player && entity.isAlive() && !entity.isSpectator())
                .stream().min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
    }

    private static void clear(ServerPlayer player) {
        player.getPersistentData().remove(TICKS);
        player.getPersistentData().remove(CAST_COST);
    }

    private static void feedback(ServerPlayer player, String message, ChatFormatting color) {
        player.displayClientMessage(Component.literal(message).withStyle(color), true);
    }
}
