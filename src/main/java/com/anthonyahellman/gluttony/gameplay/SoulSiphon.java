package com.anthonyahellman.gluttony.gameplay;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.anthonyahellman.gluttony.data.GluttonyData;
import com.anthonyahellman.gluttony.data.SinData;
import com.anthonyahellman.gluttony.network.ModNetwork;
import com.anthonyahellman.gluttony.network.SoulSiphonVfxPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = GluttonyMod.MOD_ID)
public final class SoulSiphon {
    public static final String SIPHON_DAMAGE_TAG = "DemonsBountySoulSiphonDamage";
    public static final int UNLOCK_LEVEL = 10;
    public static final int EMPOWERED_ATTACKS = 3;
    private static final double BONUS_SOULS_PER_DAMAGE = 1.5;
    private static final int BUFF_DURATION_TICKS = 200;
    private static final String CHARGES = "RootsOfSinSiphonCharges";
    private static final String EXPIRES = "RootsOfSinSiphonExpires";
    private static final String LAST_PROC_TICK = "RootsOfSinSiphonLastProcTick";

    private SoulSiphon() {}

    public static void arm(ServerPlayer player) {
        GluttonyData data = GluttonyData.of(player);
        if (SinData.selected(player) != SinData.NaturalSin.GLUTTONY || !data.active()
                || data.level() < UNLOCK_LEVEL) return;
        player.getPersistentData().putInt(CHARGES, EMPOWERED_ATTACKS);
        player.getPersistentData().putLong(EXPIRES, player.level().getGameTime() + BUFF_DURATION_TICKS);
        player.displayClientMessage(Component.literal("SOUL SIPHON — NEXT 3 ATTACKS EMPOWERED")
                .withStyle(ChatFormatting.DARK_PURPLE), true);
        sendVfx(player,
                SoulSiphonVfxPacket.primed(player.getId(), BUFF_DURATION_TICKS));
        AbilityHudSync.send(player);
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (SinData.selected(player) != SinData.NaturalSin.GLUTTONY
                || GluttonyData.of(player).selectedAbility() != GluttonyData.Ability.SOUL_SIPHON) return;
        if (!GluttonyData.of(player).allowsTarget(GluttonyData.Ability.SOUL_SIPHON,
                event.getEntity())) return;
        var tag = player.getPersistentData();
        long now = player.level().getGameTime();
        int charges = tag.getInt(CHARGES);
        if (charges <= 0 || now > tag.getLong(EXPIRES) || now < tag.getLong(LAST_PROC_TICK)) return;
        if (event.getAmount() <= 0.0F || !event.getEntity().isAlive()) return;

        tag.putLong(LAST_PROC_TICK, now + 3L);
        tag.putInt(CHARGES, charges - 1);
        double consumed = Math.min(event.getEntity().getHealth(), event.getAmount());
        double souls = consumed * BONUS_SOULS_PER_DAMAGE;
        GluttonyData.of(player).addSouls(souls);
        LivingEntity target = event.getEntity();
        Vec3 source = target.getBoundingBox().getCenter();
        Vec3 destination = player.getEyePosition().add(0.0, -0.35, 0.0);
        sendVfx(player,
                SoulSiphonVfxPacket.extraction(player.getId(), target.getId(), source,
                        destination, charges - 1, Math.max(0, (int)(tag.getLong(EXPIRES) - now))));
        player.displayClientMessage(Component.literal(String.format("SOUL SIPHON  +%.2f souls  (%d left)",
                souls, charges - 1)).withStyle(ChatFormatting.DARK_PURPLE), true);
        AbilityHudSync.send(player);
    }

    public static void spawnSoulTrail(ServerLevel level, LivingEntity target, ServerPlayer player) {
        Vec3 from = target.getBoundingBox().getCenter();
        Vec3 to = player.getEyePosition().add(0.0, -0.35, 0.0);
        for (int i = 0; i <= 12; i++) {
            Vec3 point = from.lerp(to, i / 12.0);
            level.sendParticles(ParticleTypes.SOUL, point.x, point.y, point.z,
                    2, 0.04, 0.04, 0.04, 0.02);
        }
    }

    private static void sendVfx(ServerPlayer player, SoulSiphonVfxPacket packet) {
        ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), packet);
    }
}
