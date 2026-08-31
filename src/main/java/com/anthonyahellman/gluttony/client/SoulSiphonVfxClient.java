package com.anthonyahellman.gluttony.client;

import com.anthonyahellman.gluttony.network.SoulSiphonVfxPacket;
import com.anthonyahellman.gluttony.registry.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Client-only choreography for server-confirmed Soul Siphon events. */
public final class SoulSiphonVfxClient {
    private static final RandomSource RANDOM = RandomSource.create();
    private static final Map<Integer, Primed> PRIMED = new HashMap<>();
    private static final List<Extraction> EXTRACTIONS = new ArrayList<>();
    private static ClientLevel lastLevel;

    private SoulSiphonVfxClient() {}

    public static void accept(SoulSiphonVfxPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return;
        if (packet.action() == SoulSiphonVfxPacket.PRIMED) {
            // Re-arming replaces the bounded state; repeated key presses cannot stack emitters.
            PRIMED.put(packet.casterId(), new Primed(packet.casterId(), packet.remainingTicks()));
            return;
        }
        if (packet.action() != SoulSiphonVfxPacket.EXTRACTION) return;

        if (packet.remainingCharges() <= 0 || packet.remainingTicks() <= 0) {
            PRIMED.remove(packet.casterId());
        } else {
            PRIMED.put(packet.casterId(), new Primed(packet.casterId(), packet.remainingTicks()));
        }
        if (EXTRACTIONS.size() >= GluttonyVfxTuning.MAX_ACTIVE_EXTRACTIONS) {
            EXTRACTIONS.remove(0);
        }
        EXTRACTIONS.add(new Extraction(packet.casterId(), packet.targetId(),
                new Vec3(packet.sourceX(), packet.sourceY(), packet.sourceZ()),
                new Vec3(packet.destinationX(), packet.destinationY(), packet.destinationZ())));
    }

    static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            clear();
            return;
        }
        if (level != lastLevel) {
            clear();
            lastLevel = level;
        }
        if (minecraft.isPaused()) return;
        tickPrimed(level);
        tickExtractions(level);
    }

    private static void tickPrimed(ClientLevel level) {
        Iterator<Primed> iterator = PRIMED.values().iterator();
        while (iterator.hasNext()) {
            Primed primed = iterator.next();
            Entity caster = level.getEntity(primed.casterId);
            if (--primed.remainingTicks < 0 || caster == null || !caster.isAlive()) {
                iterator.remove();
                continue;
            }
            primed.age++;
            if (primed.age % GluttonyVfxTuning.PRIMED_EMIT_INTERVAL != 0) continue;

            double angle = primed.age * 0.52 + (primed.casterId & 7);
            double radius = 0.30 + RANDOM.nextDouble() * 0.12;
            double y = caster.getY() + 0.72 + RANDOM.nextDouble() * 0.65;
            add(level, gluttonyWisp(), caster.getX() + Math.cos(angle) * radius, y,
                    caster.getZ() + Math.sin(angle) * radius,
                    -Math.cos(angle) * 0.012, 0.018, -Math.sin(angle) * 0.012);
            if (primed.age % 12 == 0) {
                add(level, hungerFlicker(), caster.getX() - Math.cos(angle) * 0.24,
                        caster.getY() + 0.95, caster.getZ() - Math.sin(angle) * 0.24,
                        Math.cos(angle) * 0.010, 0.006, Math.sin(angle) * 0.010);
            }
        }
    }

    private static void tickExtractions(ClientLevel level) {
        Iterator<Extraction> iterator = EXTRACTIONS.iterator();
        while (iterator.hasNext()) {
            Extraction extraction = iterator.next();
            int age = extraction.age++;
            Vec3 source = source(level, extraction);
            Vec3 destination = destination(level, extraction);

            if (age == 0) {
                catchTarget(level, extraction, destination, source);
                revealSoul(level, source);
            }
            if (age >= 1 && age <= 14) {
                pullSoul(level, extraction, source, destination, age);
            }
            if (age == 15) consume(level, destination);
            if (age >= GluttonyVfxTuning.EXTRACTION_TICKS) iterator.remove();
        }
    }

    private static void catchTarget(ClientLevel level, Extraction extraction, Vec3 from, Vec3 to) {
        Vec3 control = curvedControl(from, to, extraction.curveSign, 0.30);
        for (int i = 1; i <= GluttonyVfxTuning.CATCH_SEGMENTS; i++) {
            double t = i / (double)GluttonyVfxTuning.CATCH_SEGMENTS;
            Vec3 point = quadratic(from, control, to, t);
            Vec3 next = quadratic(from, control, to, Math.min(1.0, t + 0.08));
            Vec3 velocity = next.subtract(point).scale(0.12);
            add(level, gluttonyWisp(), point.x, point.y, point.z,
                    velocity.x, velocity.y, velocity.z);
        }
    }

    private static void revealSoul(ClientLevel level, Vec3 source) {
        add(level, soulCore(), source.x, source.y, source.z, 0.0, 0.015, 0.0);
        for (int i = 0; i < 5; i++) {
            double angle = Math.PI * 2.0 * i / 5.0;
            add(level, soulWisp(), source.x + Math.cos(angle) * 0.16,
                    source.y + spread(0.11), source.z + Math.sin(angle) * 0.16,
                    Math.cos(angle) * 0.015, 0.012, Math.sin(angle) * 0.015);
        }
    }

    private static void pullSoul(ClientLevel level, Extraction extraction, Vec3 source,
                                 Vec3 destination, int age) {
        double progress = Mth.clamp((age - 1) / 13.0, 0.0, 1.0);
        double accelerated = 0.04 + 0.96 * Math.pow(progress, 1.72);
        Vec3 control = curvedControl(source, destination, extraction.curveSign,
                0.42 * (1.0 - progress));
        Vec3 point = quadratic(source, control, destination, accelerated);
        Vec3 next = quadratic(source, control, destination, Math.min(1.0, accelerated + 0.055));
        Vec3 velocity = next.subtract(point).scale(0.18);

        add(level, soulCore(), point.x, point.y, point.z, velocity.x, velocity.y, velocity.z);
        for (int i = 0; i < 2; i++) {
            add(level, soulWisp(), point.x + spread(0.075), point.y + spread(0.075),
                    point.z + spread(0.075), velocity.x * 0.35, velocity.y * 0.35,
                    velocity.z * 0.35);
        }
        if ((age & 1) == 0) {
            add(level, gluttonyWisp(), point.x + spread(0.10), point.y + spread(0.10),
                    point.z + spread(0.10), velocity.x * 0.22, velocity.y * 0.22,
                    velocity.z * 0.22);
        }
    }

    private static void consume(ClientLevel level, Vec3 center) {
        for (int i = 0; i < GluttonyVfxTuning.CONSUMPTION_FLICKERS; i++) {
            double angle = Math.PI * 2.0 * i / GluttonyVfxTuning.CONSUMPTION_FLICKERS;
            double yOffset = ((i % 3) - 1) * 0.15;
            Vec3 offset = new Vec3(Math.cos(angle) * 0.68, yOffset,
                    Math.sin(angle) * 0.68);
            Vec3 velocity = offset.scale(-0.23);
            add(level, hungerFlicker(), center.x + offset.x, center.y + offset.y,
                    center.z + offset.z, velocity.x, velocity.y, velocity.z);
        }
        for (int i = 0; i < 4; i++) {
            Vec3 offset = new Vec3(spread(0.38), spread(0.32), spread(0.38));
            Vec3 velocity = offset.scale(-0.16);
            add(level, gluttonyWisp(), center.x + offset.x, center.y + offset.y,
                    center.z + offset.z, velocity.x, velocity.y, velocity.z);
        }
    }

    private static Vec3 source(ClientLevel level, Extraction extraction) {
        Entity target = level.getEntity(extraction.targetId);
        return target != null ? target.getBoundingBox().getCenter() : extraction.fallbackSource;
    }

    private static Vec3 destination(ClientLevel level, Extraction extraction) {
        Entity caster = level.getEntity(extraction.casterId);
        return caster != null ? caster.getBoundingBox().getCenter().add(0.0, 0.22, 0.0)
                : extraction.fallbackDestination;
    }

    private static Vec3 curvedControl(Vec3 from, Vec3 to, double sign, double curvature) {
        Vec3 direction = to.subtract(from);
        Vec3 horizontal = new Vec3(-direction.z, 0.0, direction.x);
        if (horizontal.lengthSqr() < 0.001) horizontal = new Vec3(1.0, 0.0, 0.0);
        double offset = Math.min(1.35, Math.max(0.22, direction.length() * curvature));
        return from.lerp(to, 0.5).add(horizontal.normalize().scale(offset * sign))
                .add(0.0, Math.min(0.75, direction.length() * 0.07), 0.0);
    }

    private static Vec3 quadratic(Vec3 from, Vec3 control, Vec3 to, double t) {
        double inverse = 1.0 - t;
        return from.scale(inverse * inverse).add(control.scale(2.0 * inverse * t))
                .add(to.scale(t * t));
    }

    private static void add(ClientLevel level, ParticleOptions particle, double x, double y,
                            double z, double vx, double vy, double vz) {
        level.addParticle(particle, x, y, z, vx, vy, vz);
    }

    private static double spread(double radius) {
        return (RANDOM.nextDouble() * 2.0 - 1.0) * radius;
    }

    private static ParticleOptions soulCore() { return ModParticles.GLUTTONY_SOUL_CORE.get(); }
    private static ParticleOptions soulWisp() { return ModParticles.GLUTTONY_SOUL_WISP.get(); }
    private static ParticleOptions gluttonyWisp() { return ModParticles.GLUTTONY_WISP.get(); }
    private static ParticleOptions hungerFlicker() { return ModParticles.GLUTTONY_HUNGER_FLICKER.get(); }

    private static void clear() {
        PRIMED.clear();
        EXTRACTIONS.clear();
        lastLevel = null;
    }

    private static final class Primed {
        private final int casterId;
        private int remainingTicks;
        private int age;

        private Primed(int casterId, int remainingTicks) {
            this.casterId = casterId;
            this.remainingTicks = remainingTicks;
        }
    }

    private static final class Extraction {
        private final int casterId;
        private final int targetId;
        private final Vec3 fallbackSource;
        private final Vec3 fallbackDestination;
        private final double curveSign;
        private int age;

        private Extraction(int casterId, int targetId, Vec3 fallbackSource,
                           Vec3 fallbackDestination) {
            this.casterId = casterId;
            this.targetId = targetId;
            this.fallbackSource = fallbackSource;
            this.fallbackDestination = fallbackDestination;
            this.curveSign = ((casterId ^ targetId) & 1) == 0 ? 1.0 : -1.0;
        }
    }
}
