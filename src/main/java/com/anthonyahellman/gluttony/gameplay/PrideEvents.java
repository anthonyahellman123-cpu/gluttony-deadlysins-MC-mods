package com.anthonyahellman.gluttony.gameplay;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.anthonyahellman.gluttony.data.PrideData;
import com.anthonyahellman.gluttony.data.SinData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GluttonyMod.MOD_ID)
public final class PrideEvents {
    public static final float NON_BOSS_DAMAGE_MULTIPLIER = 0.75F;
    public static final float BOSS_DAMAGE_MULTIPLIER = 1.25F;

    private PrideEvents() {}

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (SinData.selected(player) != SinData.NaturalSin.PRIDE) return;
        event.setAmount(event.getAmount() * (BossClassifier.isBoss(event.getEntity())
                ? BOSS_DAMAGE_MULTIPLIER : NON_BOSS_DAMAGE_MULTIPLIER));
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (SinData.selected(player) != SinData.NaturalSin.PRIDE) return;

        PrideData.Trial trial = trialFor(event.getEntity());
        if (trial == null) return;

        PrideData data = PrideData.of(player);
        boolean newlyCompleted = data.increment(trial);
        player.displayClientMessage(Component.literal(String.format("PRIDE TRIAL — %s %d/%d",
                trial.displayName(), data.count(trial), trial.required())).withStyle(ChatFormatting.GOLD), false);
        if (newlyCompleted) {
            player.displayClientMessage(Component.literal(declaration(trial)).withStyle(ChatFormatting.YELLOW), false);
            if (data.fullyAwakened()) {
                player.displayClientMessage(Component.literal("PRIDE STANDS ABOVE ALL.").withStyle(ChatFormatting.GOLD), false);
            }
        }
    }

    private static PrideData.Trial trialFor(LivingEntity entity) {
        EntityType<?> type = entity.getType();
        if (type == EntityType.ENDER_DRAGON) return PrideData.Trial.ENDER_DRAGON;
        if (type == EntityType.WITHER) return PrideData.Trial.WITHER;
        if (type == EntityType.ELDER_GUARDIAN) return PrideData.Trial.ELDER_GUARDIAN;
        if (type == EntityType.WARDEN) return PrideData.Trial.WARDEN;
        return null;
    }

    private static String declaration(PrideData.Trial trial) {
        return switch (trial) {
            case ENDER_DRAGON -> "None shall live above me.";
            case WITHER -> "Death is for the weak.";
            case ELDER_GUARDIAN -> "The sea remains beneath me.";
            case WARDEN -> "Run? WHY WOULD I RUN?";
        };
    }
}
