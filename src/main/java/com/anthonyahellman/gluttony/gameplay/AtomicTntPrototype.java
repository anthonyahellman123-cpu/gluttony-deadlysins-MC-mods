package com.anthonyahellman.gluttony.gameplay;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Harmless visual benchmark used to establish the ceiling for Explosive Supremacy. */
@Mod.EventBusSubscriber(modid = GluttonyMod.MOD_ID)
public final class AtomicTntPrototype {
    private static final String TEST_TAG = "roots_of_sin_atomic_test";
    private static final List<Sequence> ACTIVE = new ArrayList<>();

    private AtomicTntPrototype() {}

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("atomic")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("tnt").executes(context -> summonTestTnt(context.getSource()))));
    }

    private static int summonTestTnt(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        Vec3 position = source.getPosition();
        Entity sourceEntity = source.getEntity();
        if (sourceEntity instanceof ServerPlayer player) {
            position = player.position().add(player.getLookAngle().scale(3.0)).add(0.0, 1.0, 0.0);
        }

        PrimedTnt tnt = new PrimedTnt(level, position.x, position.y, position.z, sourceEntity instanceof ServerPlayer p ? p : null);
        tnt.setFuse(60);
        tnt.addTag(TEST_TAG);
        level.addFreshEntity(tnt);
        source.sendSuccess(() -> Component.literal("Harmless atomic test armed. Three seconds."), false);
        return 1;
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();

        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof PrimedTnt tnt && tnt.getTags().contains(TEST_TAG) && tnt.getFuse() <= 1) {
                    Vec3 center = tnt.position().add(0.0, 0.25, 0.0);
                    tnt.discard(); // No vanilla explosion: zero damage, fire, or broken blocks.
                    ACTIVE.add(new Sequence(level, center));
                }
            }
        }

        Iterator<Sequence> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            Sequence sequence = iterator.next();
            if (sequence.tick()) iterator.remove();
        }
    }

    private static final class Sequence {
        private static final DustParticleOptions WHITE = dust(1.0F, 0.96F, 0.78F, 2.2F);
        private static final DustParticleOptions GOLD = dust(1.0F, 0.36F, 0.02F, 1.7F);
        private static final DustParticleOptions VIOLET = dust(0.45F, 0.05F, 1.0F, 1.8F);
        private final ServerLevel level;
        private final Vec3 center;
        private int age;

        private Sequence(ServerLevel level, Vec3 center) {
            this.level = level;
            this.center = center;
        }

        private boolean tick() {
            age++;
            if (age <= 24) implosion();
            if (age == 25) detonate();
            if (age >= 26 && age <= 48) expandingFront(age - 25);
            if (age >= 30 && age <= 115) cloud(age - 30);
            if (age >= 55 && age % 4 == 0) fallout();
            return age > 140;
        }

        private void implosion() {
            double radius = 8.0 * (1.0 - age / 25.0);
            int points = 28;
            for (int i = 0; i < points; i++) {
                double angle = Math.PI * 2.0 * i / points + age * 0.17;
                double y = center.y + 0.5 + Math.sin(angle * 3.0 + age * 0.2) * 2.0;
                send(VIOLET, center.x + Math.cos(angle) * radius, y,
                        center.z + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
            }
            if (age % 6 == 0) sound(SoundEvents.BEACON_AMBIENT, 1.2F, 0.45F + age * 0.018F);
        }

        private void detonate() {
            send(ParticleTypes.FLASH, center.x, center.y + 1.0, center.z, 12, 1.2, 1.2, 1.2, 0);
            send(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y + 1.0, center.z, 4, 0.8, 0.8, 0.8, 0);
            send(WHITE, center.x, center.y + 1.0, center.z, 180, 2.5, 2.5, 2.5, 0.18);
            // Dense vertical column: the eventual spell can replace this with a custom beam renderer.
            for (int y = 0; y < 46; y++) {
                double spread = 0.12 + y * 0.008;
                send(y % 3 == 0 ? GOLD : WHITE, center.x, center.y + y, center.z,
                        5, spread, 0.15, spread, 0.01);
            }
            sound(SoundEvents.GENERIC_EXPLODE, 4.0F, 0.55F);
            sound(SoundEvents.LIGHTNING_BOLT_THUNDER, 4.0F, 0.65F);
        }

        private void expandingFront(int phase) {
            double radius = phase * 0.75;
            ring(radius, 72, WHITE, 0.12);
            ring(radius * 0.92, 64, VIOLET, 0.08);
            if (phase % 4 == 0) sound(SoundEvents.WARDEN_SONIC_BOOM, 2.2F, 0.65F + phase * 0.012F);
        }

        private void cloud(int phase) {
            double rise = phase * 0.15;
            double waist = 1.4 + Math.min(phase, 32) * 0.075;
            send(ParticleTypes.CAMPFIRE_COSY_SMOKE, center.x, center.y + 1.0 + rise, center.z,
                    7, waist, 0.7, waist, 0.025);
            send(ParticleTypes.LARGE_SMOKE, center.x, center.y + 1.0 + rise, center.z,
                    5, waist * 0.75, 0.6, waist * 0.75, 0.015);
            if (phase < 35) {
                send(ParticleTypes.FLAME, center.x, center.y + 1.0 + rise * 0.6, center.z,
                        12, waist, 1.0, waist, 0.035);
                send(GOLD, center.x, center.y + 1.0 + rise * 0.65, center.z,
                        10, waist, 0.8, waist, 0.035);
            }
            if (phase > 34) {
                double capRadius = 3.5 + (phase - 34) * 0.10;
                ringAt(center.y + 1.0 + rise, capRadius, 54, ParticleTypes.CAMPFIRE_COSY_SMOKE);
            }
        }

        private void fallout() {
            send(ParticleTypes.ASH, center.x, center.y + 10.0, center.z, 35, 13.0, 6.0, 13.0, 0.025);
            send(ParticleTypes.PORTAL, center.x, center.y + 3.0, center.z, 15, 9.0, 3.0, 9.0, 0.04);
        }

        private void ring(double radius, int points, Object particle, double verticalSpread) {
            for (int i = 0; i < points; i++) {
                double angle = Math.PI * 2.0 * i / points;
                send(particle, center.x + Math.cos(angle) * radius, center.y + 0.15,
                        center.z + Math.sin(angle) * radius, 1, 0.08, verticalSpread, 0.08, 0);
            }
        }

        private void ringAt(double y, double radius, int points, Object particle) {
            for (int i = 0; i < points; i++) {
                double angle = Math.PI * 2.0 * i / points;
                send(particle, center.x + Math.cos(angle) * radius, y,
                        center.z + Math.sin(angle) * radius, 1, 0.35, 0.25, 0.35, 0.015);
            }
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private void send(Object particle, double x, double y, double z, int count,
                          double dx, double dy, double dz, double speed) {
            level.sendParticles((net.minecraft.core.particles.ParticleOptions) particle,
                    x, y, z, count, dx, dy, dz, speed);
        }

        private void sound(net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
            level.playSound(null, center.x, center.y, center.z, sound, SoundSource.AMBIENT, volume, pitch);
        }

        private static DustParticleOptions dust(float red, float green, float blue, float size) {
            return new DustParticleOptions(new Vector3f(red, green, blue), size);
        }
    }
}
