package com.anthonyahellman.gluttony.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public final class PrideSpriteParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final Vector3f startTint;
    private final Vector3f endTint;
    private final float maximumAlpha;

    private PrideSpriteParticle(ClientLevel level, double x, double y, double z,
                                double vx, double vy, double vz, SpriteSet sprites, Kind kind) {
        super(level, x, y, z, vx, vy, vz);
        this.sprites = sprites;
        this.hasPhysics = false;
        this.friction = kind.friction;
        this.gravity = kind.gravity;
        this.lifetime = kind.minimumLifetime + random.nextInt(kind.lifetimeVariance + 1);
        this.quadSize = kind.minimumSize + random.nextFloat() * kind.sizeVariance;
        this.alpha = kind.startAlpha;
        this.maximumAlpha = kind.startAlpha;
        this.startTint = kind.startTint(random.nextFloat());
        this.endTint = kind.endTint;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.roll = random.nextFloat() * ((float)Math.PI * 2.0F);
        this.oRoll = this.roll;
        setSpriteFromAge(sprites);
        applyTint(startTint);
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) return;
        float progress = lifetime <= 1 ? 1.0F : Math.min(1.0F, age / (float)lifetime);
        applyTint(PrideParticlePalette.mix(startTint, endTint, progress));
        alpha = Math.max(0.0F, 1.0F - progress * progress) * maximumAlpha;
        setSpriteFromAge(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    private void applyTint(Vector3f tint) {
        setColor(tint.x(), tint.y(), tint.z());
    }

    public enum Kind {
        MOTE(10, 8, 0.28F, 0.24F, 0.90F, 0.94F, -0.015F,
                PrideParticlePalette.BRILLIANT_GOLD),
        STREAK(9, 6, 0.52F, 0.34F, 0.92F, 0.96F, 0.0F,
                PrideParticlePalette.DEEP_GOLD),
        SHARD(14, 10, 0.34F, 0.30F, 0.84F, 0.91F, 0.055F,
                PrideParticlePalette.BLACKENED_GOLD),
        EMBER(22, 18, 0.20F, 0.20F, 0.82F, 0.89F, -0.025F,
                PrideParticlePalette.DEEP_GOLD);

        private final int minimumLifetime;
        private final int lifetimeVariance;
        private final float minimumSize;
        private final float sizeVariance;
        private final float friction;
        private final float startAlpha;
        private final float gravity;
        private final Vector3f endTint;

        Kind(int minimumLifetime, int lifetimeVariance, float minimumSize, float sizeVariance,
             float friction, float startAlpha, float gravity, Vector3f endTint) {
            this.minimumLifetime = minimumLifetime;
            this.lifetimeVariance = lifetimeVariance;
            this.minimumSize = minimumSize;
            this.sizeVariance = sizeVariance;
            this.friction = friction;
            this.startAlpha = startAlpha;
            this.gravity = gravity;
            this.endTint = endTint;
        }

        private Vector3f startTint(float randomMix) {
            return switch (this) {
                case MOTE -> PrideParticlePalette.mix(PrideParticlePalette.RADIANT_WHITE,
                        PrideParticlePalette.BRILLIANT_GOLD, randomMix * 0.18F);
                case STREAK -> PrideParticlePalette.mix(PrideParticlePalette.RADIANT_WHITE,
                        PrideParticlePalette.BRILLIANT_GOLD, 0.42F + randomMix * 0.34F);
                case SHARD -> PrideParticlePalette.mix(PrideParticlePalette.DEEP_GOLD,
                        PrideParticlePalette.BRILLIANT_GOLD, 0.24F + randomMix * 0.22F);
                case EMBER -> PrideParticlePalette.mix(PrideParticlePalette.DEEP_GOLD,
                        PrideParticlePalette.BRILLIANT_GOLD, 0.48F + randomMix * 0.42F);
            };
        }
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final Kind kind;

        public Provider(SpriteSet sprites, Kind kind) {
            this.sprites = sprites;
            this.kind = kind;
        }

        @Override
        public @Nullable Particle createParticle(SimpleParticleType type, ClientLevel level,
                                                 double x, double y, double z,
                                                 double vx, double vy, double vz) {
            return new PrideSpriteParticle(level, x, y, z, vx, vy, vz, sprites, kind);
        }
    }
}
