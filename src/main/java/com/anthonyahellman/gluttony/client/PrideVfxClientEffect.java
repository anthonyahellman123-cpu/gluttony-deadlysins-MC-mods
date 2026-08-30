package com.anthonyahellman.gluttony.client;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.anthonyahellman.gluttony.network.PrideVfxTestPacket;
import com.anthonyahellman.gluttony.registry.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Client-only disposable Stage V visual ceiling for /pride_vfx_test. */
@Mod.EventBusSubscriber(modid = GluttonyMod.MOD_ID, value = Dist.CLIENT)
public final class PrideVfxClientEffect {
    private static final RandomSource RANDOM = RandomSource.create();
    private static final Map<Integer, Descent> DESCENTS = new HashMap<>();
    private static final Map<Integer, Impact> IMPACTS = new HashMap<>();

    private PrideVfxClientEffect() {}

    public static void accept(PrideVfxTestPacket packet) {
        if (packet.action() == PrideVfxTestPacket.START_DESCENT) {
            DESCENTS.put(packet.entityId(), new Descent(packet.entityId()));
        } else if (packet.action() == PrideVfxTestPacket.IMPACT) {
            DESCENTS.remove(packet.entityId());
            IMPACTS.put(packet.entityId(), new Impact(new Vec3(packet.x(), packet.y(), packet.z())));
            PrideShockwaveRenderer.spawnImpact(new Vec3(packet.x(), packet.y(), packet.z()), packet.radius());
        } else if (packet.action() == PrideVfxTestPacket.WAVE) {
            PrideShockwaveRenderer.spawnWave(new Vec3(packet.x(), packet.y(), packet.z()), packet.radius(),
                    packet.durationTicks(), packet.variant(), packet.delayTicks());
        }
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused()) return;
        tickDescents(level);
        tickImpacts(level);
        PrideShockwaveRenderer.tick(level);
    }

    private static void tickDescents(ClientLevel level) {
        Iterator<Descent> iterator = DESCENTS.values().iterator();
        while (iterator.hasNext()) {
            Descent descent = iterator.next();
            descent.age++;
            Entity actor = level.getEntity(descent.entityId);
            if (actor == null) {
                if (descent.age > 100) iterator.remove();
                continue;
            }
            double intensity = Mth.clamp(0.28 + descent.age / 36.0, 0.28, 1.0);
            Vec3 center = actor.position().add(0.0, 0.55, 0.0);

            int core = 4 + (int)(5 * intensity);
            for (int i = 0; i < core; i++) {
                add(level, mote(), center.x + spread(0.33), center.y + spread(0.34),
                        center.z + spread(0.33), 0.0, 0.035, 0.0);
            }
            int trail = 7 + (int)(9 * intensity);
            for (int i = 0; i < trail; i++) {
                double height = 0.7 + RANDOM.nextDouble() * (3.5 + intensity * 4.5);
                ParticleOptions sprite = RANDOM.nextFloat() < 0.46F ? mote() : streak();
                add(level, sprite, center.x + spread(0.42 + height * 0.025), center.y + height,
                        center.z + spread(0.42 + height * 0.025), 0.0, 0.045, 0.0);
            }

            // A restrained symmetrical suggestion of wings, kept as light streaks rather than geometry.
            for (int side : new int[]{-1, 1}) {
                for (int feather = 0; feather < 4; feather++) {
                    double rise = 0.65 + feather * 0.55;
                    double width = side * (0.55 + feather * 0.18);
                    add(level, feather < 2 ? mote() : streak(),
                            center.x + width, center.y + rise, center.z + spread(0.12),
                            side * 0.018, 0.035, 0.0);
                }
            }
            if (descent.age % 2 == 0) {
                add(level, streak(), center.x + spread(0.24), center.y + 0.2,
                        center.z + spread(0.24), 0.0, 0.08, 0.0);
            }
        }
    }

    private static void tickImpacts(ClientLevel level) {
        Iterator<Impact> iterator = IMPACTS.values().iterator();
        while (iterator.hasNext()) {
            Impact impact = iterator.next();
            int age = impact.age++;
            if (age == 0) impactFlash(level, impact.origin);
            if (age < 6) radiantColumn(level, impact.origin, age);
            if (age >= 6 && age < 40) {
                aftermath(level, impact.origin, age - 6);
            }
            if (age > 40) iterator.remove();
        }
    }

    private static void impactFlash(ClientLevel level, Vec3 origin) {
        add(level, mote(), origin.x, origin.y + 0.45, origin.z, 0.0, 0.0, 0.0);
        for (int i = 0; i < 72; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            double speed = 0.16 + RANDOM.nextDouble() * 0.62;
            double vertical = 0.05 + RANDOM.nextDouble() * 0.42;
            ParticleOptions sprite = i % 3 == 0 ? mote() : streak();
            add(level, sprite, origin.x + spread(0.35), origin.y + 0.25 + spread(0.18),
                    origin.z + spread(0.35), Math.cos(angle) * speed, vertical,
                    Math.sin(angle) * speed);
        }
    }

    private static void radiantColumn(ClientLevel level, Vec3 origin, int age) {
        int count = 24 - age * 3;
        for (int i = 0; i < count; i++) {
            double height = RANDOM.nextDouble() * (7.5 - age * 0.5);
            ParticleOptions sprite = RANDOM.nextBoolean() ? mote() : streak();
            add(level, sprite, origin.x + spread(0.48), origin.y + height,
                    origin.z + spread(0.48), 0.0, 0.08 + RANDOM.nextDouble() * 0.09, 0.0);
        }
    }

    private static void aftermath(ClientLevel level, Vec3 origin, int age) {
        int count = Math.max(1, 6 - Math.max(0, age) / 7);
        for (int i = 0; i < count; i++) {
            double radius = RANDOM.nextDouble() * 4.5;
            double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            ParticleOptions sprite = RANDOM.nextFloat() < 0.72F ? ember() : shard();
            add(level, sprite, origin.x + Math.cos(angle) * radius,
                    origin.y + 0.18 + RANDOM.nextDouble() * 1.6,
                    origin.z + Math.sin(angle) * radius,
                    0.0, 0.018 + RANDOM.nextDouble() * 0.035, 0.0);
        }
    }

    private static void add(ClientLevel level, ParticleOptions particle,
                            double x, double y, double z, double vx, double vy, double vz) {
        level.addParticle(particle, x, y, z, vx, vy, vz);
    }

    private static double spread(double radius) {
        return (RANDOM.nextDouble() * 2.0 - 1.0) * radius;
    }

    private static ParticleOptions mote() {
        return ModParticles.PRIDE_RADIANT_MOTE.get();
    }

    private static ParticleOptions streak() {
        return ModParticles.PRIDE_RADIANT_STREAK.get();
    }

    private static ParticleOptions shard() {
        return ModParticles.PRIDE_BLACKENED_GOLD_SHARD.get();
    }

    private static ParticleOptions ember() {
        return ModParticles.PRIDE_GOLDEN_EMBER.get();
    }

    private static final class Descent {
        private final int entityId;
        private int age;

        private Descent(int entityId) {
            this.entityId = entityId;
        }
    }

    private static final class Impact {
        private final Vec3 origin;
        private int age;

        private Impact(Vec3 origin) {
            this.origin = origin;
        }
    }
}
