package com.anthonyahellman.gluttony.client;

import com.anthonyahellman.gluttony.network.DevourVfxPacket;
import com.anthonyahellman.gluttony.network.DevourChargePacket;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Client-only bite, rip, and inward-consumption choreography for Devour. */
public final class DevourVfxClient {
    private static final RandomSource RANDOM = RandomSource.create();
    private static final List<Bite> BITES = new ArrayList<>();
    private static final Map<Integer, ChargingMaw> CHARGING_MAWS = new HashMap<>();
    private static ClientLevel lastLevel;

    private DevourVfxClient() {}

    public static void accept(DevourVfxPacket packet) {
        if (Minecraft.getInstance().level == null) return;
        if (BITES.size() >= GluttonyVfxTuning.MAX_ACTIVE_DEVOURS) BITES.remove(0);
        BITES.add(new Bite(packet.casterId(), packet.targetId(),
                new Vec3(packet.sourceX(), packet.sourceY(), packet.sourceZ()),
                new Vec3(packet.destinationX(), packet.destinationY(), packet.destinationZ()),
                packet.committedHealth()));
    }

    public static void updateCharge(DevourChargePacket packet) {
        if (!packet.active()) {
            CHARGING_MAWS.remove(packet.casterId());
            return;
        }
        ChargingMaw maw = CHARGING_MAWS.computeIfAbsent(packet.casterId(), ChargingMaw::new);
        maw.targetId = packet.targetId();
        maw.committedHealth = Math.max(0.0, packet.committedHealth());
        maw.staleTicks = 0;
    }

    static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) { clear(); return; }
        if (level != lastLevel) { clear(); lastLevel = level; }
        if (minecraft.isPaused()) return;

        Iterator<ChargingMaw> maws = CHARGING_MAWS.values().iterator();
        while (maws.hasNext()) {
            ChargingMaw maw = maws.next();
            if (++maw.staleTicks > 16) {
                maws.remove();
                continue;
            }
            if ((maw.age++ & 1) == 0) renderChargingMaw(level, maw);
        }

        Iterator<Bite> iterator = BITES.iterator();
        while (iterator.hasNext()) {
            Bite bite = iterator.next();
            int age = bite.age++;
            Vec3 source = source(level, bite);
            Vec3 destination = destination(level, bite);
            if (age <= 3) closeJaws(level, bite, source, age);
            if (age == 4) contact(level, bite, source);
            if (age == 5) expose(level, bite, source);
            if (age >= 6 && age <= 12) rip(level, bite, source, destination, age);
            if (age == 13) consume(level, bite, destination);
            if (age >= GluttonyVfxTuning.DEVOUR_TICKS) iterator.remove();
        }
    }

    private static void renderChargingMaw(ClientLevel level, ChargingMaw maw) {
        Entity caster = level.getEntity(maw.casterId);
        Entity target = level.getEntity(maw.targetId);
        if (caster == null || target == null) return;
        Vec3 center = target.getBoundingBox().getCenter();
        Vec3 towardCaster = caster.getBoundingBox().getCenter().subtract(center);
        Vec3 forward = towardCaster.lengthSqr() < 0.001 ? new Vec3(0.0, 0.0, 1.0)
                : towardCaster.normalize();
        Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
        if (side.lengthSqr() < 0.001) side = new Vec3(1.0, 0.0, 0.0);
        else side = side.normalize();
        Vec3 up = side.cross(forward).normalize();
        double growth = Math.log1p(maw.committedHealth / 20.0);
        double radius = Math.min(3.8, 0.72 + growth * 0.38);
        int ringPoints = 18;
        double pulse = 1.0 + Math.sin(maw.age * 0.26) * 0.045;

        for (int i = 0; i < ringPoints; i++) {
            double angle = Math.PI * 2.0 * i / ringPoints;
            Vec3 offset = side.scale(Math.cos(angle) * radius * pulse)
                    .add(up.scale(Math.sin(angle) * radius * 0.82 * pulse));
            add(level, gluttony(), center.add(offset), offset.scale(-0.018));
        }
        for (int i = 0; i < 5; i++) {
            Vec3 dark = side.scale(spread(radius * 0.48)).add(up.scale(spread(radius * 0.38)));
            add(level, hunger(), center.add(dark), dark.scale(-0.035));
        }

        int totalTeeth = (int) Math.floor(maw.committedHealth / 20.0);
        int visibleTeeth = Math.min(120, totalTeeth);
        for (int i = 0; i < visibleTeeth; i++) {
            int layer = i / 24;
            int slot = i % 24;
            double irregular = ((i * 37) % 11 - 5) * 0.012;
            double angle = Math.PI * 2.0 * (slot + 0.33 * layer) / 24.0 + irregular;
            double toothRadius = Math.max(0.20, radius - 0.16 - layer * 0.13);
            Vec3 tooth = side.scale(Math.cos(angle) * toothRadius)
                    .add(up.scale(Math.sin(angle) * toothRadius * 0.82));
            Vec3 inward = tooth.normalize().scale(-0.055);
            add(level, soulWisp(), center.add(tooth), inward);
        }
    }

    private static void closeJaws(ClientLevel level, Bite bite, Vec3 center, int age) {
        double progress = (age + 1) / 4.0;
        double scale = 0.95 + bite.charge * 0.70;
        double gap = (1.28 - progress * 1.10) * scale;
        double height = (0.78 - progress * 0.16) * scale;
        for (int side : new int[]{-1, 1}) {
            for (int i = 0; i < GluttonyVfxTuning.DEVOUR_JAW_POINTS; i++) {
                double t = i / (double)(GluttonyVfxTuning.DEVOUR_JAW_POINTS - 1);
                double y = (t - 0.5) * height * 1.65;
                double curve = Math.sin(t * Math.PI) * 0.38 * scale;
                Vec3 offset = bite.side.scale(side * (gap + curve)).add(0.0, y, 0.0);
                Vec3 velocity = bite.side.scale(-side * (0.13 + progress * 0.09));
                add(level, gluttony(), center.add(offset), velocity);
                if ((i + age) % 2 == 0) {
                    Vec3 inner = bite.side.scale(side * Math.max(0.08, gap * 0.62))
                            .add(0.0, y * 0.78, 0.0);
                    add(level, hunger(), center.add(inner), velocity.scale(1.35));
                }
            }
        }
        int teeth = Math.min(120, (int) Math.floor(bite.committedHealth / 20.0));
        for (int i = 0; i < teeth; i++) {
            int row = i / 24;
            double angle = Math.PI * 2.0 * ((i % 24) + row * 0.31) / 24.0;
            double radius = Math.max(0.18, (0.72 + bite.charge * 0.52) - row * 0.11);
            double vertical = Math.sin(angle) * radius;
            double horizontal = Math.cos(angle) * radius * Math.max(0.12, 1.0 - progress * 0.86);
            Vec3 tooth = bite.side.scale(horizontal).add(0.0, vertical, 0.0);
            add(level, soulWisp(), center.add(tooth), tooth.normalize().scale(-0.075));
        }
    }

    private static void contact(ClientLevel level, Bite bite, Vec3 center) {
        int count = 14 + Math.round(bite.charge * 10.0F);
        for (int i = 0; i < count; i++) {
            Vec3 offset = bite.side.scale(spread(0.50)).add(0.0, spread(0.54), spread(0.34));
            add(level, hunger(), center.add(offset), offset.scale(-0.46));
        }
        for (int i = 0; i < 7; i++) add(level, gluttony(), center.add(randomOffset(0.46)),
                randomOffset(0.032));
    }

    private static void expose(ClientLevel level, Bite bite, Vec3 center) {
        add(level, soulCore(), center, Vec3.ZERO);
        int count = 7 + Math.round(bite.charge * 6.0F);
        for (int i = 0; i < count; i++) {
            Vec3 offset = randomOffset(0.16 + bite.charge * 0.08);
            add(level, soulWisp(), center.add(offset), offset.scale(-0.10));
        }
    }

    private static void rip(ClientLevel level, Bite bite, Vec3 source, Vec3 destination, int age) {
        double progress = (age - 5) / 7.0;
        double violent = 0.10 + 0.90 * Math.pow(progress, 0.62);
        Vec3 midpoint = source.lerp(destination, 0.5).add(bite.side.scale(0.34 * (1.0 - progress)));
        Vec3 point = quadratic(source, midpoint, destination, violent);
        Vec3 next = quadratic(source, midpoint, destination, Math.min(1.0, violent + 0.12));
        Vec3 velocity = next.subtract(point).scale(0.38);
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
        CHARGING_MAWS.clear();
        lastLevel = null;
    }

    private static final class Bite {
        private final int casterId;
        private final int targetId;
        private final Vec3 fallbackSource;
        private final Vec3 fallbackDestination;
        private final float charge;
        private final double committedHealth;
        private final Vec3 side;
        private int age;

        private Bite(int casterId, int targetId, Vec3 fallbackSource,
                     Vec3 fallbackDestination, double committedHealth) {
            this.casterId = casterId;
            this.targetId = targetId;
            this.fallbackSource = fallbackSource;
            this.fallbackDestination = fallbackDestination;
            this.committedHealth = committedHealth;
            this.charge = (float) Mth.clamp(Math.log1p(committedHealth / 20.0) / 3.0, 0.0, 1.0);
            Vec3 direction = fallbackDestination.subtract(fallbackSource);
            Vec3 horizontal = new Vec3(-direction.z, 0.0, direction.x);
            this.side = horizontal.lengthSqr() < 0.001 ? new Vec3(1.0, 0.0, 0.0)
                    : horizontal.normalize();
        }
    }

    private static final class ChargingMaw {
        private final int casterId;
        private int targetId = -1;
        private double committedHealth;
        private int staleTicks;
        private int age;

        private ChargingMaw(int casterId) {
            this.casterId = casterId;
        }
    }
}
