package com.anthonyahellman.gluttony.gameplay;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.anthonyahellman.gluttony.data.GluttonyData;
import com.anthonyahellman.gluttony.data.SinData;
import com.anthonyahellman.gluttony.network.DevourChargePacket;
import com.anthonyahellman.gluttony.network.DevourVfxPacket;
import com.anthonyahellman.gluttony.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative, uncapped HP-reservation implementation of Devour. */
@Mod.EventBusSubscriber(modid = GluttonyMod.MOD_ID)
public final class Devour {
    public static final int UNLOCK_LEVEL = 50;
    public static final double HP_PER_TICK = 1.0;
    public static final double DAMAGE_PER_COMMITTED_HP = 1.0;
    public static final double HP_PER_TOOTH = 20.0;
    public static final double EXTRACTION_BONUS_PER_TOOTH = 0.10;
    public static final double CHARGING_DAMAGE_MULTIPLIER = 0.15;
    public static final double TARGET_ACQUISITION_RANGE = 24.0;
    public static final double LOCK_LEASH_RANGE = 128.0;
    private static final double SURVIVAL_FLOOR = 1.0;
    private static final int SYNC_INTERVAL_TICKS = 4;
    private static final UUID KNOCKBACK_ID = UUID.fromString("d36f6631-5ec4-444f-89f3-b41d914e397d");
    public static final String DEVOUR_DAMAGE_TAG = "RootsOfSinDevourDamage";
    private static final Map<UUID, Charge> CHARGES = new HashMap<>();

    private Devour() {}

    public static void toggle(ServerPlayer player) {
        if (charging(player)) release(player);
        else begin(player);
    }

    public static void begin(ServerPlayer player) {
        if (!canUse(player)) return;
        cancel(player);
        LivingEntity target = acquireTarget(player);
        if (target == null) {
            player.displayClientMessage(Component.literal("DEVOUR — NO VALID TARGET")
                    .withStyle(ChatFormatting.DARK_RED), true);
            return;
        }
        long now = player.level().getGameTime();
        double historicalHealth = GluttonyData.of(player).historicalMaxHealth();
        Charge charge = new Charge(now, target.getUUID(), player.level().dimension(),
                historicalHealth);
        CHARGES.put(player.getUUID(), charge);
        applyChargingAttributes(player, true);
        sync(player, charge, true);
        player.displayClientMessage(Component.literal("DEVOUR LOCKED — "
                + target.getDisplayName().getString()).withStyle(ChatFormatting.DARK_RED), true);
    }

    public static void release(ServerPlayer player) {
        Charge charge = CHARGES.remove(player.getUUID());
        applyChargingAttributes(player, false);
        if (charge == null) return;
        LivingEntity target = findLockedTarget(player, charge);
        if (target == null) {
            sync(player, charge, false);
            player.displayClientMessage(Component.literal("DEVOUR RELEASED — NO PREY")
                    .withStyle(ChatFormatting.DARK_RED), true);
            return;
        }
        consume(player, target, charge.committedHealth);
        sync(player, charge, false);
    }

    public static boolean charging(ServerPlayer player) {
        return CHARGES.containsKey(player.getUUID());
    }

    public static double committedHealth(ServerPlayer player) {
        Charge charge = CHARGES.get(player.getUUID());
        return charge == null ? 0.0 : charge.committedHealth;
    }

    public static double extractionBonus(double committedHealth) {
        return Math.max(0.0, committedHealth) / HP_PER_TOOTH * EXTRACTION_BONUS_PER_TOOTH;
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Iterator<Map.Entry<UUID, Charge>> iterator = CHARGES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Charge> entry = iterator.next();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            Charge charge = entry.getValue();
            if (player == null || !player.isAlive() || !canUse(player)) {
                if (player != null) {
                    applyChargingAttributes(player, false);
                    sync(player, charge, false);
                }
                iterator.remove();
                continue;
            }

            LivingEntity target = findLockedTarget(player, charge);
            if (target == null) {
                applyChargingAttributes(player, false);
                sync(player, charge, false);
                player.displayClientMessage(Component.literal("DEVOUR LOCK LOST — HP RELEASED")
                        .withStyle(ChatFormatting.GRAY), true);
                iterator.remove();
                continue;
            }

            charge.committedHealth = Math.min(charge.chargeCapacity,
                    charge.committedHealth + HP_PER_TICK);
            applyChargingAttributes(player, true);
            restrictMovement(player);
            if ((player.level().getGameTime() - charge.startedAt) % SYNC_INTERVAL_TICKS == 0) {
                sync(player, charge, true);
            }
        }
    }

    @SubscribeEvent
    public static void onChargingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !charging(player)) return;
        event.setAmount(Math.max(0.0F, event.getAmount() * (float) CHARGING_DAMAGE_MULTIPLIER));
        Charge charge = CHARGES.get(player.getUUID());
        if (charge != null) {
            charge.committedHealth = Math.max(0.0, charge.committedHealth - event.getAmount());
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) cancel(player);
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        CHARGES.clear();
    }

    private static boolean canUse(ServerPlayer player) {
        GluttonyData data = GluttonyData.of(player);
        return SinData.selected(player) == SinData.NaturalSin.GLUTTONY && data.active()
                && data.level() >= UNLOCK_LEVEL
                && data.selectedAbility() == GluttonyData.Ability.DEVOUR;
    }

    private static void cancel(ServerPlayer player) {
        Charge charge = CHARGES.remove(player.getUUID());
        applyChargingAttributes(player, false);
        if (charge != null) sync(player, charge, false);
    }

    private static LivingEntity acquireTarget(ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1.0F).scale(TARGET_ACQUISITION_RANGE));
        AABB search = player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.5);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, start, end, search,
                entity -> entity instanceof LivingEntity living && living.isAlive()
                        && GluttonyData.of(player).allowsTarget(GluttonyData.Ability.DEVOUR, living)
                        && player.hasLineOfSight(living) && !entity.isSpectator(),
                TARGET_ACQUISITION_RANGE * TARGET_ACQUISITION_RANGE);
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    private static LivingEntity findLockedTarget(ServerPlayer player, Charge charge) {
        if (!player.level().dimension().equals(charge.dimension)) return null;
        Entity entity = player.serverLevel().getEntity(charge.targetId);
        if (!(entity instanceof LivingEntity living) || !living.isAlive()
                || player.distanceToSqr(living) > LOCK_LEASH_RANGE * LOCK_LEASH_RANGE
                || !GluttonyData.of(player).allowsTarget(GluttonyData.Ability.DEVOUR, living)) return null;
        return living;
    }

    private static void consume(ServerPlayer player, LivingEntity target, double committedHealth) {
        double healthCost = Math.min(committedHealth, Math.max(0.0, player.getHealth() - SURVIVAL_FLOOR));
        double baseAttack = Math.max(0.0, player.getAttributeValue(Attributes.ATTACK_DAMAGE));
        float calculated = (float) Math.min(Float.MAX_VALUE,
                baseAttack + committedHealth * DAMAGE_PER_COMMITTED_HP);
        float before = target.getHealth();
        boolean hurt;
        player.getPersistentData().putBoolean(DEVOUR_DAMAGE_TAG, true);
        try {
            target.invulnerableTime = 0;
            hurt = target.hurt(player.damageSources().indirectMagic(player, player), calculated);
        } finally {
            player.getPersistentData().remove(DEVOUR_DAMAGE_TAG);
        }
        if (!hurt) {
            player.displayClientMessage(Component.literal("DEVOUR FAILED — HP RELEASED")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }

        player.setHealth(Math.max((float) SURVIVAL_FLOOR, player.getHealth() - (float) healthCost));
        double consumed = Math.max(0.0, before - Math.max(0.0F, target.getHealth()));
        double multiplier = 1.0 + extractionBonus(committedHealth);
        extractConsumedStats(player, target, consumed, multiplier);

        GluttonyData data = GluttonyData.of(player);
        data.addSouls(consumed);
        Vec3 source = target.getBoundingBox().getCenter();
        Vec3 destination = player.getEyePosition().add(0.0, -0.35, 0.0);
        ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new DevourVfxPacket(player.getId(), target.getId(), source.x, source.y, source.z,
                        destination.x, destination.y, destination.z, committedHealth));
        player.displayClientMessage(Component.literal(String.format(
                "DEVOUR — %.1f HP COMMITTED  |  %.2f HP CONSUMED  |  +%.1f%% EXTRACTION",
                committedHealth, consumed, extractionBonus(committedHealth) * 100.0))
                .withStyle(ChatFormatting.DARK_RED), true);
        AbilityHudSync.send(player);
    }

    private static void extractConsumedStats(ServerPlayer player, LivingEntity target,
                                             double consumed, double effectivenessMultiplier) {
        if (consumed <= 0.0) return;
        GluttonyData data = GluttonyData.of(player);
        double fraction = GluttonyExtraction.statFraction(data.level()) * effectivenessMultiplier;
        double targetMaxHealth = Math.max(1.0, attribute(target, Attributes.MAX_HEALTH));
        double consumedFraction = Math.min(1.0, consumed / targetMaxHealth);
        double healthGain = consumed * fraction;
        double attackGain = Math.max(0.0, attribute(target, Attributes.ATTACK_DAMAGE))
                * consumedFraction * fraction;
        data.addExtractedStats(healthGain, attackGain);
        SoulEvents.refreshAttributes(player);
    }

    /** Retained for Beelzebub's separate one-time target extraction behavior. */
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

    private static double attribute(LivingEntity entity,
                                    net.minecraft.world.entity.ai.attributes.Attribute attribute) {
        AttributeInstance instance = entity.getAttribute(attribute);
        return instance == null ? 0.0 : instance.getValue();
    }

    private static void applyChargingAttributes(ServerPlayer player, boolean active) {
        setTransient(player.getAttribute(Attributes.KNOCKBACK_RESISTANCE), KNOCKBACK_ID,
                "Devour standing firm", active ? 1.0 : 0.0, AttributeModifier.Operation.ADDITION);
    }

    private static void restrictMovement(ServerPlayer player) {
        // Do not modify MOVEMENT_SPEED: Minecraft uses that attribute for dynamic FOV.
        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(movement.x * 0.10, movement.y, movement.z * 0.10);
    }

    private static void setTransient(AttributeInstance attribute, UUID id, String name, double amount,
                                     AttributeModifier.Operation operation) {
        if (attribute == null) return;
        AttributeModifier old = attribute.getModifier(id);
        if (old != null) attribute.removeModifier(old);
        if (amount != 0.0) attribute.addTransientModifier(new AttributeModifier(id, name, amount, operation));
    }

    private static void sync(ServerPlayer player, Charge charge, boolean active) {
        LivingEntity target = findLockedTarget(player, charge);
        int targetId = target == null ? -1 : target.getId();
        String targetName = target == null ? "—" : target.getDisplayName().getString();
        if (targetName.length() > 64) targetName = targetName.substring(0, 64);
        double availableFlesh = Math.max(0.0, charge.chargeCapacity - charge.committedHealth);
        DevourChargePacket packet = new DevourChargePacket(player.getId(), targetId, active,
                targetName, charge.committedHealth, player.getHealth(), player.getMaxHealth(),
                availableFlesh, charge.historicalHealth);
        ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), packet);
    }

    private static final class Charge {
        private final long startedAt;
        private final UUID targetId;
        private final net.minecraft.resources.ResourceKey<Level> dimension;
        private final double historicalHealth;
        private final double chargeCapacity;
        private double committedHealth;

        private Charge(long startedAt, UUID targetId,
                       net.minecraft.resources.ResourceKey<Level> dimension,
                       double historicalHealth) {
            this.startedAt = startedAt;
            this.targetId = targetId;
            this.dimension = dimension;
            this.historicalHealth = historicalHealth;
            this.chargeCapacity = Math.max(0.0, historicalHealth - SURVIVAL_FLOOR);
        }
    }
}
