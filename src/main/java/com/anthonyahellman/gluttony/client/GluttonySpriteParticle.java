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

public final class GluttonySpriteParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final Vector3f startTint;
    private final Vector3f endTint;
    private final float maximumAlpha;

    private GluttonySpriteParticle(ClientLevel level, double x, double y, double z,
                                   double vx, double vy, double vz, SpriteSet sprites, Kind kind) {
        super(level, x, y, z, vx, vy, vz);
        this.sprites = sprites;
        this.hasPhysics = false;
        this.friction = kind.friction;
        this.gravity = 0.0F;
        this.lifetime = kind.minimumLifetime + random.nextInt(kind.lifetimeVariance + 1);
        this.quadSize = kind.size + random.nextFloat() * kind.sizeVariance;
        this.maximumAlpha = kind.alpha;
        this.alpha = maximumAlpha;
        this.startTint = kind.startTint;
        this.endTint = kind.endTint;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.roll = random.nextFloat() * ((float)Math.PI * 2.0F);
        this.oRoll = roll;
        setSpriteFromAge(sprites);
        applyTint(startTint);
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) return;
        float progress = lifetime <= 1 ? 1.0F : Math.min(1.0F, age / (float)lifetime);
        applyTint(GluttonyParticlePalette.mix(startTint, endTint, progress));
        alpha = maximumAlpha * (1.0F - progress * progress);
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
        SOUL_CORE(5, 2, GluttonyVfxTuning.SOUL_CORE_SIZE, 0.035F, 0.98F, 0.96F,
                GluttonyParticlePalette.SOUL_WHITE, GluttonyParticlePalette.SOUL_CYAN),
        SOUL_WISP(7, 3, GluttonyVfxTuning.SOUL_WISP_SIZE, 0.025F, 0.91F, 0.90F,
                GluttonyParticlePalette.SOUL_WHITE, GluttonyParticlePalette.SOUL_CYAN),
        GLUTTONY_WISP(8, 4, GluttonyVfxTuning.GLUTTONY_WISP_SIZE, 0.022F, 0.88F, 0.86F,
                GluttonyParticlePalette.GLUTTONY_PURPLE, GluttonyParticlePalette.DEEP_PURPLE),
        HUNGER_FLICKER(5, 2, GluttonyVfxTuning.HUNGER_FLICKER_SIZE, 0.030F, 0.84F, 0.94F,
                GluttonyParticlePalette.HUNGER_RED, GluttonyParticlePalette.DARK_RED);

        private final int minimumLifetime;
        private final int lifetimeVariance;
        private final float size;
        private final float sizeVariance;
        private final float friction;
        private final float alpha;
        private final Vector3f startTint;
        private final Vector3f endTint;

        Kind(int minimumLifetime, int lifetimeVariance, float size, float sizeVariance,
             float friction, float alpha, Vector3f startTint, Vector3f endTint) {
            this.minimumLifetime = minimumLifetime;
            this.lifetimeVariance = lifetimeVariance;
            this.size = size;
            this.sizeVariance = sizeVariance;
            this.friction = friction;
            this.alpha = alpha;
            this.startTint = startTint;
            this.endTint = endTint;
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
            return new GluttonySpriteParticle(level, x, y, z, vx, vy, vz, sprites, kind);
        }
    }
}
