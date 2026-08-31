package com.anthonyahellman.gluttony.client;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.anthonyahellman.gluttony.gameplay.PrideFallTuning;
import com.anthonyahellman.gluttony.registry.ModParticles;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Continuous world-space impact and shock-front geometry for Lucifer's Fall. */
@Mod.EventBusSubscriber(modid = GluttonyMod.MOD_ID, value = Dist.CLIENT)
public final class PrideShockwaveRenderer {
    public static final float GOLD_EDGE_OPACITY = 0.88F;
    public static final float BODY_OPACITY = 0.66F;
    public static final float RED_FISSURE_INTENSITY = 0.48F;
    public static final float WAVE_FADE_RATE = 0.22F;

    private static final double[] UNIT_X = new double[PrideFallTuning.WAVE_SEGMENTS + 1];
    private static final double[] UNIT_Z = new double[PrideFallTuning.WAVE_SEGMENTS + 1];
    private static final List<Front> FRONTS = new ArrayList<>();
    private static final List<ImpactBurst> BURSTS = new ArrayList<>();
    private static final RandomSource RANDOM = RandomSource.create();

    static {
        for (int i = 0; i <= PrideFallTuning.WAVE_SEGMENTS; i++) {
            double angle = Math.PI * 2.0 * i / PrideFallTuning.WAVE_SEGMENTS;
            UNIT_X[i] = Math.cos(angle);
            UNIT_Z[i] = Math.sin(angle);
        }
    }

    private PrideShockwaveRenderer() {}

    public static void spawnImpact(Vec3 origin, double radius) {
        BURSTS.add(new ImpactBurst(origin, Math.max(1.0, radius)));
    }

    public static void spawnWave(Vec3 origin, double maxRadius, int durationTicks, int variant, int delayTicks) {
        FRONTS.add(new Front(origin, Math.max(1.0, maxRadius), Math.max(1, durationTicks),
                Math.max(0, variant), Math.max(0, delayTicks)));
    }

    static void tick(ClientLevel level) {
        Iterator<ImpactBurst> bursts = BURSTS.iterator();
        while (bursts.hasNext()) {
            ImpactBurst burst = bursts.next();
            if (++burst.age > PrideFallTuning.IMPACT_BURST_TICKS + 4) bursts.remove();
        }

        Iterator<Front> fronts = FRONTS.iterator();
        while (fronts.hasNext()) {
            Front front = fronts.next();
            front.age++;
            if (front.age >= front.delayTicks && front.age <= front.delayTicks + front.durationTicks
                    && front.variant == 0 && front.age % 2 == 0) {
                spawnAccents(level, front);
            }
            if (front.age > front.delayTicks + front.durationTicks + 8) fronts.remove();
        }
    }

    @SubscribeEvent
    public static void renderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
                || (FRONTS.isEmpty() && BURSTS.isEmpty())) return;

        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (ImpactBurst burst : BURSTS) {
            double linear = Mth.clamp((burst.age + event.getPartialTick())
                    / PrideFallTuning.IMPACT_BURST_TICKS, 0.0, 1.0);
            double radius = burst.maxRadius * (1.0 - Math.pow(1.0 - linear, 2.8));
            float fade = (float)(1.0 - linear);
            appendFront(builder, poseStack, cameraPos, burst.origin, radius,
                    Math.max(0.45, radius * 0.18), 1.15 + radius * 0.035,
                    0.0F, fade, true, 0);
        }
        for (Front front : FRONTS) {
            double elapsed = front.age + event.getPartialTick() - front.delayTicks;
            if (elapsed < 0.0 || elapsed > front.durationTicks + 8.0) continue;
            double progress = PrideFallTuning.waveProgress(elapsed, front.durationTicks);
            double radius = front.maxRadius * progress;
            float tailFade = elapsed <= front.durationTicks ? 1.0F
                    : Math.max(0.0F, 1.0F - (float)(elapsed - front.durationTicks) / 8.0F);
            appendFront(builder, poseStack, cameraPos, front.origin, radius,
                    PrideFallTuning.WAVE_WIDTH, PrideFallTuning.WAVE_HEIGHT,
                    (float)progress, tailFade, false, front.variant);
        }

        BufferUploader.drawWithShader(builder.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void appendFront(BufferBuilder builder, PoseStack poseStack, Vec3 cameraPos,
                                    Vec3 origin, double radius, double width, double height,
                                    float progress, float fade, boolean impact, int variant) {
        if (radius <= 0.05 || fade <= 0.01F) return;
        double outer = radius;
        double inner = Math.max(0.0, radius - Math.max(0.15, width));
        double baseY = 0.06;
        poseStack.pushPose();
        poseStack.translate(origin.x - cameraPos.x, origin.y - cameraPos.y, origin.z - cameraPos.z);
        Matrix4f matrix = poseStack.last().pose();

        for (int i = 0; i < PrideFallTuning.WAVE_SEGMENTS; i++) {
            double x0 = UNIT_X[i];
            double z0 = UNIT_Z[i];
            double x1 = UNIT_X[i + 1];
            double z1 = UNIT_Z[i + 1];
            double crown0 = height * (0.90 + 0.10 * Math.sin(i * 0.71));
            double crown1 = height * (0.90 + 0.10 * Math.sin((i + 1) * 0.71));

            Color body = bodyColor(i, progress, impact, variant);
            Color edge = edgeColor(variant, impact);
            int bodyAlpha = alpha(BODY_OPACITY * fade * (impact ? 0.80F : 1.0F));
            int edgeAlpha = alpha(GOLD_EDGE_OPACITY * fade);
            int wakeAlpha = alpha(BODY_OPACITY * fade * WAVE_FADE_RATE);

            // Continuous upper annulus: brilliant leading rim to a fractured fading wake.
            vertex(builder, matrix, x0 * inner, crown0 * 0.72, z0 * inner, body, wakeAlpha);
            vertex(builder, matrix, x0 * outer, crown0, z0 * outer, body, bodyAlpha);
            vertex(builder, matrix, x1 * outer, crown1, z1 * outer, body, bodyAlpha);
            vertex(builder, matrix, x1 * inner, crown1 * 0.72, z1 * inner, body, wakeAlpha);

            // The vertical outer face makes the wave visible from ground level.
            vertex(builder, matrix, x0 * outer, baseY, z0 * outer, edge, edgeAlpha);
            vertex(builder, matrix, x0 * outer, crown0, z0 * outer, body, bodyAlpha);
            vertex(builder, matrix, x1 * outer, crown1, z1 * outer, body, bodyAlpha);
            vertex(builder, matrix, x1 * outer, baseY, z1 * outer, edge, edgeAlpha);

            // Inner face retains volume and a readable trailing boundary.
            vertex(builder, matrix, x1 * inner, baseY, z1 * inner, body, wakeAlpha);
            vertex(builder, matrix, x1 * inner, crown1 * 0.72, z1 * inner, body, wakeAlpha);
            vertex(builder, matrix, x0 * inner, crown0 * 0.72, z0 * inner, body, wakeAlpha);
            vertex(builder, matrix, x0 * inner, baseY, z0 * inner, body, wakeAlpha);
        }
        poseStack.popPose();
    }

    private static Color bodyColor(int segment, float progress, boolean impact, int variant) {
        if (variant > 0) {
            return segment % 13 == 0 ? new Color(155, 255, 242) : new Color(18, 104, 110);
        }
        if (impact) return segment % 11 == 0 ? color(PrideParticlePalette.RADIANT_WHITE)
                : color(PrideParticlePalette.BRILLIANT_GOLD);
        if (segment % 19 == 0 || segment % 23 == 0) {
            return mix(color(PrideParticlePalette.BLACKENED_GOLD),
                    color(PrideParticlePalette.SIN_RED), RED_FISSURE_INTENSITY);
        }
        if (segment % 11 == 0) return color(PrideParticlePalette.BRILLIANT_GOLD);
        float corruption = Mth.clamp(0.18F + progress * 0.74F, 0.0F, 0.92F);
        return mix(color(PrideParticlePalette.DEEP_GOLD),
                color(PrideParticlePalette.BLACKENED_GOLD), corruption);
    }

    private static Color edgeColor(int variant, boolean impact) {
        if (variant > 0) return new Color(92, 238, 230);
        return color(impact ? PrideParticlePalette.RADIANT_WHITE : PrideParticlePalette.BRILLIANT_GOLD);
    }

    private static Color color(Vector3f vector) {
        return new Color((int)(vector.x() * 255.0F), (int)(vector.y() * 255.0F), (int)(vector.z() * 255.0F));
    }

    private static Color mix(Color from, Color to, float amount) {
        float t = Mth.clamp(amount, 0.0F, 1.0F);
        return new Color((int)Mth.lerp(t, from.red, to.red),
                (int)Mth.lerp(t, from.green, to.green),
                (int)Mth.lerp(t, from.blue, to.blue));
    }

    private static void vertex(BufferBuilder builder, Matrix4f matrix, double x, double y, double z,
                               Color color, int alpha) {
        builder.vertex(matrix, (float)x, (float)y, (float)z)
                .color(color.red, color.green, color.blue, alpha).endVertex();
    }

    private static int alpha(float value) {
        return (int)(Mth.clamp(value, 0.0F, 1.0F) * 255.0F);
    }

    private static void spawnAccents(ClientLevel level, Front front) {
        double progress = PrideFallTuning.waveProgress(front.age - front.delayTicks, front.durationTicks);
        double radius = front.maxRadius * progress;
        for (int i = 0; i < 3; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            double wake = RANDOM.nextDouble() * PrideFallTuning.WAVE_WIDTH;
            double x = front.origin.x + Math.cos(angle) * Math.max(0.0, radius - wake);
            double z = front.origin.z + Math.sin(angle) * Math.max(0.0, radius - wake);
            level.addParticle(i == 0 ? ModParticles.PRIDE_BLACKENED_GOLD_SHARD.get()
                            : ModParticles.PRIDE_GOLDEN_EMBER.get(),
                    x, front.origin.y + 0.15 + RANDOM.nextDouble() * PrideFallTuning.WAVE_HEIGHT,
                    z, Math.cos(angle) * 0.025, 0.025 + RANDOM.nextDouble() * 0.035,
                    Math.sin(angle) * 0.025);
        }
    }

    private record Color(int red, int green, int blue) {}

    private static final class Front {
        private final Vec3 origin;
        private final double maxRadius;
        private final int durationTicks;
        private final int variant;
        private final int delayTicks;
        private int age;

        private Front(Vec3 origin, double maxRadius, int durationTicks, int variant, int delayTicks) {
            this.origin = origin;
            this.maxRadius = maxRadius;
            this.durationTicks = durationTicks;
            this.variant = variant;
            this.delayTicks = delayTicks;
        }
    }

    private static final class ImpactBurst {
        private final Vec3 origin;
        private final double maxRadius;
        private int age;

        private ImpactBurst(Vec3 origin, double maxRadius) {
            this.origin = origin;
            this.maxRadius = maxRadius;
        }
    }
}
