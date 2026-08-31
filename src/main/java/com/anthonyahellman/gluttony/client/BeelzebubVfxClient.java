package com.anthonyahellman.gluttony.client;

import com.anthonyahellman.gluttony.network.BeelzebubVfxPacket;
import com.anthonyahellman.gluttony.registry.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Client-only, bounded victim-to-caster feast streams for Beelzebub. */
public final class BeelzebubVfxClient {
    private static final RandomSource RANDOM = RandomSource.create();
    private static final List<FeastPulse> PULSES = new ArrayList<>();
    private static ClientLevel lastLevel;

    private BeelzebubVfxClient() {}

    public static void accept(BeelzebubVfxPacket packet) {
        if (Minecraft.getInstance().level == null || packet.targetIds().length == 0) return;
        while (PULSES.size() >= GluttonyVfxTuning.MAX_ACTIVE_FEAST_PULSES) PULSES.remove(0);
        int count = Math.min(packet.targetIds().length, GluttonyVfxTuning.MAX_BEELZEBUB_TARGETS);
        int[] targets = new int[count];
        System.arraycopy(packet.targetIds(), 0, targets, 0, count);
        PULSES.add(new FeastPulse(packet.casterId(), targets, Mth.clamp(packet.radius(), 1.0F, 512.0F)));
    }

    static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) { clear(); return; }
        if (level != lastLevel) { clear(); lastLevel = level; }
        if (minecraft.isPaused()) return;

        int budget = GluttonyVfxTuning.BEELZEBUB_PARTICLE_BUDGET_PER_TICK;
        Iterator<FeastPulse> iterator = PULSES.iterator();
        while (iterator.hasNext() && budget > 0) {
            FeastPulse pulse = iterator.next();
            Entity caster = level.getEntity(pulse.casterId);
            if (caster == null || !caster.isAlive()) { iterator.remove(); continue; }
            int age = pulse.age++;
            Vec3 destination = caster.getBoundingBox().getCenter().add(0.0, 0.22, 0.0);
            for (int i = 0; i < pulse.targetIds.length && budget >= 3; i++) {
                Entity target = level.getEntity(pulse.targetIds[i]);
                // Dying victims remain renderable during their death animation; only a missing
                // client entity prevents us from drawing the server-confirmed consumption stream.
                if (target == null) continue;
                Vec3 source = target.getBoundingBox().getCenter();
                if (age == 0) budget -= seize(level, source, i);
                if (age >= 1 && age <= 7 && budget >= 3) {
                    budget -= stream(level, pulse, source, destination, age, i);
                }
            }
            if (age == 8 && budget >= 8) budget -= consume(level, destination, pulse.targetIds.length);
            if (age >= GluttonyVfxTuning.BEELZEBUB_FEAST_TICKS) iterator.remove();
        }
    }

    private static int seize(ClientLevel level, Vec3 center, int index) {
        double angle = index * 2.399963229728653;
        Vec3 side = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
        for (int i = -1; i <= 1; i++) {
            Vec3 offset = side.scale(0.34 + Math.abs(i) * 0.11).add(0.0, i * 0.27, 0.0);
            add(level, gluttony(), center.add(offset), offset.scale(-0.16));
        }
        add(level, hunger(), center.add(randomOffset(0.18)), randomOffset(0.025));
        return 4;
    }

    private static int stream(ClientLevel level, FeastPulse pulse, Vec3 source,
                              Vec3 destination, int age, int index) {
        double progress = 0.06 + 0.94 * Math.pow(age / 7.0, 1.30);
        Vec3 direction = destination.subtract(source);
        Vec3 side = new Vec3(-direction.z, 0.0, direction.x);
        if (side.lengthSqr() < 0.001) side = new Vec3(1.0, 0.0, 0.0);
        double curvature = Math.min(1.10, 0.16 + direction.length() * 0.035);
        Vec3 control = source.lerp(destination, 0.52)
                .add(side.normalize().scale(((index & 1) == 0 ? 1.0 : -1.0) * curvature))
                .add(0.0, Math.min(0.65, pulse.radius * 0.018), 0.0);
        Vec3 point = quadratic(source, control, destination, progress);
        Vec3 next = quadratic(source, control, destination, Math.min(1.0, progress + 0.09));
        Vec3 velocity = next.subtract(point).scale(0.28);
        add(level, soulCore(), point, velocity);
        add(level, soulWisp(), point.add(randomOffset(0.075)), velocity.scale(0.42));
        add(level, gluttony(), point.lerp(source, 0.12).add(randomOffset(0.10)), velocity.scale(0.20));
        if (age == 5 && (index & 3) == 0) {
            add(level, hunger(), point.add(randomOffset(0.12)), velocity.scale(0.18));
            return 4;
        }
        return 3;
    }

    private static int consume(ClientLevel level, Vec3 center, int victims) {
        int count = Math.min(20, 8 + victims / 2);
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0 * i / count;
            Vec3 offset = new Vec3(Math.cos(angle) * 0.74, ((i % 3) - 1) * 0.20,
                    Math.sin(angle) * 0.74);
            add(level, (i & 1) == 0 ? hunger() : gluttony(), center.add(offset),
                    offset.scale(-0.38));
        }
        return count;
    }

    private static Vec3 quadratic(Vec3 from, Vec3 control, Vec3 to, double t) {
        double inverse = 1.0 - t;
        return from.scale(inverse * inverse).add(control.scale(2.0 * inverse * t))
                .add(to.scale(t * t));
    }

    private static Vec3 randomOffset(double radius) {
        return new Vec3(spread(radius), spread(radius), spread(radius));
    }

    private static double spread(double radius) {
        return (RANDOM.nextDouble() * 2.0 - 1.0) * radius;
    }

    private static void add(ClientLevel level, ParticleOptions particle, Vec3 point, Vec3 velocity) {
        level.addParticle(particle, point.x, point.y, point.z, velocity.x, velocity.y, velocity.z);
    }

    private static ParticleOptions soulCore() { return ModParticles.GLUTTONY_SOUL_CORE.get(); }
    private static ParticleOptions soulWisp() { return ModParticles.GLUTTONY_SOUL_WISP.get(); }
    private static ParticleOptions gluttony() { return ModParticles.GLUTTONY_WISP.get(); }
    private static ParticleOptions hunger() { return ModParticles.GLUTTONY_HUNGER_FLICKER.get(); }

    private static void clear() {
        PULSES.clear();
        lastLevel = null;
    }

    private static final class FeastPulse {
        private final int casterId;
        private final int[] targetIds;
        private final float radius;
        private int age;

        private FeastPulse(int casterId, int[] targetIds, float radius) {
            this.casterId = casterId;
            this.targetIds = targetIds;
            this.radius = radius;
        }
    }
}
