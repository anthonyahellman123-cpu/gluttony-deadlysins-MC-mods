package com.anthonyahellman.gluttony.client;

import com.anthonyahellman.gluttony.network.DevourVfxPacket;
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

/** Client-only bite, rip, and inward-consumption choreography for Devour. */
public final class DevourVfxClient {
    private static final RandomSource RANDOM = RandomSource.create();
    private static final List<Bite> BITES = new ArrayList<>();
    private static ClientLevel lastLevel;

    private DevourVfxClient() {}

    public static void accept(DevourVfxPacket packet) {
        if (Minecraft.getInstance().level == null) return;
        if (BITES.size() >= GluttonyVfxTuning.MAX_ACTIVE_DEVOURS) BITES.remove(0);
        BITES.add(new Bite(packet.casterId(), packet.targetId(),
                new Vec3(packet.sourceX(), packet.sourceY(), packet.sourceZ()),
                new Vec3(packet.destinationX(), packet.destinationY(), packet.destinationZ()),
                Mth.clamp(packet.chargeStrength(), 0.0F, 1.0F)));
    }

    static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) { clear(); return; }
        if (level != lastLevel) { clear(); lastLevel = level; }
        if (minecraft.isPaused()) return;

        Iterator<Bite> iterator = BITES.iterator();
        while (iterator.hasNext()) {
            Bite bite = iterator.next();
            int age = bite.age++;
            Vec3 source = source(level, bite);
            Vec3 destination = destination(level, bite);
            if (age <= 2) closeJaws(level, bite, source, age);
            if (age == 3) contact(level, bite, source);
            if (age == 4) expose(level, bite, source);
            if (age >= 5 && age <= 10) rip(level, bite, source, destination, age);
            if (age == 11) consume(level, bite, destination);
            if (age >= GluttonyVfxTuning.DEVOUR_TICKS) iterator.remove();
        }
    }

    private static void closeJaws(ClientLevel level, Bite bite, Vec3 center, int age) {
        double progress = (age + 1) / 3.0;
        double scale = 0.85 + bite.charge * 0.55;
        double gap = (1.05 - progress * 0.82) * scale;
        double height = (0.72 - progress * 0.20) * scale;
        for (int side : new int[]{-1, 1}) {
            for (int i = 0; i < GluttonyVfxTuning.DEVOUR_JAW_POINTS; i++) {
                double t = i / (double)(GluttonyVfxTuning.DEVOUR_JAW_POINTS - 1);
                double y = (t - 0.5) * height * 1.65;
                double curve = Math.sin(t * Math.PI) * 0.30 * scale;
                Vec3 offset = bite.side.scale(side * (gap + curve)).add(0.0, y, 0.0);
                Vec3 velocity = bite.side.scale(-side * (0.10 + progress * 0.06));
                add(level, gluttony(), center.add(offset), velocity);
                if ((i + age) % 2 == 0) add(level, hunger(), center.add(offset.scale(0.92)),
                        velocity.scale(1.25));
            }
        }
    }

    private static void contact(ClientLevel level, Bite bite, Vec3 center) {
        int count = 8 + Math.round(bite.charge * 6.0F);
        for (int i = 0; i < count; i++) {
            Vec3 offset = bite.side.scale(spread(0.38)).add(0.0, spread(0.48), spread(0.30));
            add(level, hunger(), center.add(offset), offset.scale(-0.34));
        }
        for (int i = 0; i < 4; i++) add(level, gluttony(), center.add(randomOffset(0.42)),
                randomOffset(0.025));
    }

    private static void expose(ClientLevel level, Bite bite, Vec3 center) {
        add(level, soulCore(), center, Vec3.ZERO);
        int count = 5 + Math.round(bite.charge * 4.0F);
        for (int i = 0; i < count; i++) {
            Vec3 offset = randomOffset(0.16 + bite.charge * 0.08);
            add(level, soulWisp(), center.add(offset), offset.scale(-0.10));
        }
    }

    private static void rip(ClientLevel level, Bite bite, Vec3 source, Vec3 destination, int age) {
        double progress = (age - 4) / 6.0;
        double violent = 0.12 + 0.88 * Math.pow(progress, 0.72);
        Vec3 midpoint = source.lerp(destination, 0.5).add(bite.side.scale(0.34 * (1.0 - progress)));
        Vec3 point = quadratic(source, midpoint, destination, violent);
        Vec3 next = quadratic(source, midpoint, destination, Math.min(1.0, violent + 0.12));
        Vec3 velocity = next.subtract(point).scale(0.30);
        add(level, soulCore(), point, velocity);
        int trails = GluttonyVfxTuning.DEVOUR_TRAIL_POINTS + Math.round(bite.charge * 2.0F);
        for (int i = 0; i < trails; i++) {
            Vec3 trail = point.lerp(source, 0.06 + i * 0.045).add(randomOffset(0.055));
            add(level, soulWisp(), trail, velocity.scale(0.48));
        }
        add(level, gluttony(), point.add(randomOffset(0.09)), velocity.scale(0.26));
    }

    private static void consume(ClientLevel level, Bite bite, Vec3 center) {
        int count = GluttonyVfxTuning.DEVOUR_CONSUMPTION_FLICKERS
                + Math.round(bite.charge * 6.0F);
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0 * i / count;
            Vec3 offset = new Vec3(Math.cos(angle) * (0.58 + bite.charge * 0.20),
                    ((i % 3) - 1) * 0.18, Math.sin(angle) * (0.58 + bite.charge * 0.20));
            add(level, (i & 1) == 0 ? hunger() : gluttony(), center.add(offset),
                    offset.scale(-0.42));
        }
    }

    private static Vec3 source(ClientLevel level, Bite bite) {
        Entity target = level.getEntity(bite.targetId);
        return target != null ? target.getBoundingBox().getCenter() : bite.fallbackSource;
    }

    private static Vec3 destination(ClientLevel level, Bite bite) {
        Entity caster = level.getEntity(bite.casterId);
        return caster != null ? caster.getBoundingBox().getCenter().add(0.0, 0.22, 0.0)
                : bite.fallbackDestination;
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
        BITES.clear();
        lastLevel = null;
    }

    private static final class Bite {
        private final int casterId;
        private final int targetId;
        private final Vec3 fallbackSource;
        private final Vec3 fallbackDestination;
        private final float charge;
        private final Vec3 side;
        private int age;

        private Bite(int casterId, int targetId, Vec3 fallbackSource,
                     Vec3 fallbackDestination, float charge) {
            this.casterId = casterId;
            this.targetId = targetId;
            this.fallbackSource = fallbackSource;
            this.fallbackDestination = fallbackDestination;
            this.charge = charge;
            Vec3 direction = fallbackDestination.subtract(fallbackSource);
            Vec3 horizontal = new Vec3(-direction.z, 0.0, direction.x);
            this.side = horizontal.lengthSqr() < 0.001 ? new Vec3(1.0, 0.0, 0.0)
                    : horizontal.normalize();
        }
    }
}
