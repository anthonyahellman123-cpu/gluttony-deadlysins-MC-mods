package com.anthonyahellman.gluttony.gameplay;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.anthonyahellman.gluttony.data.GluttonyData;
import com.anthonyahellman.gluttony.data.SinData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = GluttonyMod.MOD_ID)
public final class Devour {
    public static final int UNLOCK_LEVEL = 50;
    public static final int EMPOWERED_ATTACKS = 3;
    public static final int MAX_CHARGE_TICKS = 60;
    public static final double MISSING_HP_FRACTION = 0.25;
    public static final double MAX_SACRIFICE_FRACTION = 0.10;
    public static final double SACRIFICED_HEALTH_DAMAGE = 1.0;
    public static final double SACRIFICED_ATTACK_DAMAGE = 4.0;
    public static final double KILL_HEALTH_CONVERSION = 0.10;
    public static final double KILL_ATTACK_CONVERSION = 0.025;
    private static final int BUFF_DURATION_TICKS = 200;
    private static final String DEVOUR_DAMAGE = "RootsOfSinDevourSecondaryDamage";
    private static final String CHARGES = "RootsOfSinDevourCharges";
    private static final String EXPIRES = "RootsOfSinDevourExpires";
    private static final String CHARGED_BONUS = "RootsOfSinDevourChargedBonus";
    private static final String LAST_PROC_TICK = "RootsOfSinDevourLastProcTick";
    private static final List<PendingBite> PENDING = new ArrayList<>();

    private Devour() {}

    public static void arm(ServerPlayer player, int chargeTicks) {
        GluttonyData data = GluttonyData.of(player);
        if (SinData.selected(player) != SinData.NaturalSin.GLUTTONY || !data.active()
                || data.level() < UNLOCK_LEVEL) return;
        double charge = Math.min(1.0, Math.max(0, chargeTicks) / (double)MAX_CHARGE_TICKS);
        double healthSpent = data.sacrificeExtractedHealth(data.extractedHealth()
                * MAX_SACRIFICE_FRACTION * charge);
        double attackSpent = data.sacrificeExtractedAttack(data.extractedAttack()
                * MAX_SACRIFICE_FRACTION * charge);
        double bonus = healthSpent * SACRIFICED_HEALTH_DAMAGE + attackSpent * SACRIFICED_ATTACK_DAMAGE;
        SoulEvents.refreshAttributes(player);
        var tag = player.getPersistentData();
        tag.putInt(CHARGES, EMPOWERED_ATTACKS);
        tag.putLong(EXPIRES, player.level().getGameTime() + BUFF_DURATION_TICKS);
        tag.putDouble(CHARGED_BONUS, bonus);
        player.displayClientMessage(Component.literal(String.format(
                "DEVOUR ARMED — 3 ATTACKS  |  COMMITTED %.2f HEALTH, %.2f ATTACK",
                healthSpent, attackSpent)).withStyle(ChatFormatting.DARK_RED), true);
        AbilityHudSync.send(player);
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (player.getPersistentData().getBoolean(DEVOUR_DAMAGE)) return;
        GluttonyData data = GluttonyData.of(player);
        if (SinData.selected(player) != SinData.NaturalSin.GLUTTONY
                || data.selectedAbility() != GluttonyData.Ability.DEVOUR) return;
        var tag = player.getPersistentData();
        long now = player.level().getGameTime();
        int charges = tag.getInt(CHARGES);
        if (charges <= 0 || now > tag.getLong(EXPIRES) || now < tag.getLong(LAST_PROC_TICK)
                || event.getAmount() <= 0.0F) return;
        tag.putLong(LAST_PROC_TICK, now + 3L);
        tag.putInt(CHARGES, charges - 1);
        double chargedBonus = tag.getDouble(CHARGED_BONUS);
        tag.putDouble(CHARGED_BONUS, 0.0);
        PENDING.add(new PendingBite(player.serverLevel(), player.getUUID(), event.getEntity().getUUID(),
                now + 1L, chargedBonus));
    }

    @SubscribeEvent
    public static void levelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        Iterator<PendingBite> iterator = PENDING.iterator();
        while (iterator.hasNext()) {
            PendingBite bite = iterator.next();
            if (bite.level != level || now < bite.executeAt) continue;
            iterator.remove();
            Entity source = level.getEntity(bite.playerId);
            Entity prey = level.getEntity(bite.targetId);
            if (!(source instanceof ServerPlayer player) || !(prey instanceof LivingEntity target)
                    || !target.isAlive()) continue;
            consume(player, target, bite.chargedBonus);
        }
    }

    private static void consume(ServerPlayer player, LivingEntity target, double chargedBonus) {
        float missing = Math.max(0.0F, target.getMaxHealth() - target.getHealth());
        float calculated = (float)(missing * MISSING_HP_FRACTION + chargedBonus);
        if (calculated <= 0.0F) return;
        float before = target.getHealth();
        player.getPersistentData().putBoolean(DEVOUR_DAMAGE, true);
        target.invulnerableTime = 0;
        boolean hurt = target.hurt(player.damageSources().indirectMagic(player, player), calculated);
        player.getPersistentData().remove(DEVOUR_DAMAGE);
        if (!hurt) return;

        double consumed = Math.max(0.0, before - Math.max(0.0F, target.getHealth()));
        GluttonyData data = GluttonyData.of(player);
        data.addSouls(consumed);
        extractUniqueStats(player, target, 0.10);
        if (!target.isAlive() && consumed > 0.0) {
            data.addExtractedStats(consumed * KILL_HEALTH_CONVERSION,
                    consumed * KILL_ATTACK_CONVERSION);
            SoulEvents.refreshAttributes(player);
        }
        SoulSiphon.spawnSoulTrail(player.serverLevel(), target, player);
        player.displayClientMessage(Component.literal(String.format(
                "DEVOUR BITE  %.2f HP CONSUMED%s", consumed,
                !target.isAlive() ? " — EXECUTED" : "")).withStyle(ChatFormatting.DARK_RED), true);
        AbilityHudSync.send(player);
    }

    public static boolean extractUniqueStats(ServerPlayer player, LivingEntity target, double scale) {
        String consumedKey = "RootsOfSinDevouredBy_" + player.getStringUUID();
        if (target.getPersistentData().getBoolean(consumedKey)) return false;
        GluttonyData data = GluttonyData.of(player);
        double fraction = GluttonyExtraction.statFraction(data.level()) * scale;
        data.addExtractedStats(attribute(target, Attributes.MAX_HEALTH) * fraction,
                Math.max(0.0, attribute(target, Attributes.ATTACK_DAMAGE)) * fraction);
        target.getPersistentData().putBoolean(consumedKey, true);
        SoulEvents.refreshAttributes(player);
        return true;
    }

    private static double attribute(LivingEntity entity, net.minecraft.world.entity.ai.attributes.Attribute attribute) {
        AttributeInstance instance = entity.getAttribute(attribute);
        return instance == null ? 0.0 : instance.getValue();
    }

    private record PendingBite(ServerLevel level, UUID playerId, UUID targetId,
                               long executeAt, double chargedBonus) {}
}
