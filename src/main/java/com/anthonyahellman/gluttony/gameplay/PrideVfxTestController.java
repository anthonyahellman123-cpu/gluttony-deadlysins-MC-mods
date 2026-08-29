package com.anthonyahellman.gluttony.gameplay;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.anthonyahellman.gluttony.network.ModNetwork;
import com.anthonyahellman.gluttony.network.PrideVfxTestPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Disposable visual actor for /pride_vfx_test. It never calls PrideAbility. */
@Mod.EventBusSubscriber(modid = GluttonyMod.MOD_ID)
public final class PrideVfxTestController {
    private static final String TEST_ACTOR_TAG = "RootsOfSinPrideVfxTestActor";
    private static final double START_HEIGHT = 38.0;
    private static final double VIEW_DISTANCE = 112.0;
    private static final Map<UUID, ActiveTest> ACTIVE = new HashMap<>();

    private PrideVfxTestController() {}

    public static void start(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
        if (horizontal.lengthSqr() < 0.01) horizontal = new Vec3(0.0, 0.0, 1.0);
        horizontal = horizontal.normalize().scale(8.0);
        double x = player.getX() + horizontal.x;
        double z = player.getZ() + horizontal.z;
        int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mth.floor(x), Mth.floor(z));
        double startY = Math.max(groundY + START_HEIGHT, player.getY() + 24.0);

        PrimedTnt actor = new PrimedTnt(level, x, startY, z, player);
        actor.addTag(TEST_ACTOR_TAG);
        actor.setFuse(Integer.MAX_VALUE);
        actor.setNoGravity(true);
        actor.setDeltaMovement(0.0, -0.38, 0.0);
        level.addFreshEntity(actor);
        ACTIVE.put(actor.getUUID(), new ActiveTest(actor));
        sendNear(level, x, startY, z,
                new PrideVfxTestPacket(PrideVfxTestPacket.START_DESCENT,
                        actor.getId(), x, startY, z));
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Iterator<ActiveTest> iterator = ACTIVE.values().iterator();
        while (iterator.hasNext()) {
            ActiveTest test = iterator.next();
            PrimedTnt actor = test.actor;
            if (!actor.isAlive() || !(actor.level() instanceof ServerLevel level)) {
                iterator.remove();
                continue;
            }
            test.age++;
            actor.setFuse(Integer.MAX_VALUE);
            actor.fallDistance = 0.0F;
            double nextVelocity = Math.max(-2.65, actor.getDeltaMovement().y - 0.105);
            actor.setDeltaMovement(0.0, nextVelocity, 0.0);
            actor.hurtMarked = true;

            boolean supported = actor.onGround() || !level.noCollision(actor,
                    actor.getBoundingBox().move(0.0, -0.16, 0.0));
            if (supported || test.age >= 100) {
                double impactY = actor.getY() + 0.08;
                sendNear(level, actor.getX(), impactY, actor.getZ(),
                        new PrideVfxTestPacket(PrideVfxTestPacket.IMPACT,
                                actor.getId(), actor.getX(), impactY, actor.getZ()));
                actor.discard();
                iterator.remove();
            }
        }
    }

    private static void sendNear(ServerLevel level, double x, double y, double z,
                                 PrideVfxTestPacket packet) {
        ModNetwork.CHANNEL.send(PacketDistributor.NEAR.with(() ->
                new PacketDistributor.TargetPoint(x, y, z, VIEW_DISTANCE, level.dimension())), packet);
    }

    private static final class ActiveTest {
        private final PrimedTnt actor;
        private int age;

        private ActiveTest(PrimedTnt actor) {
            this.actor = actor;
        }
    }
}
