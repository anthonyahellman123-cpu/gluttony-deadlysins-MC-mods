package com.anthonyahellman.gluttony.gameplay;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.anthonyahellman.gluttony.data.GluttonyData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = GluttonyMod.MOD_ID)
public final class SoulEvents {
    private static final UUID HEALTH_ID = UUID.fromString("96d159ba-8eef-47e4-8357-090d8d75dde1");
    private static final UUID ATTACK_ID = UUID.fromString("9b0595c0-e7c8-45cb-ac37-1b999304586e");

    private SoulEvents() {}

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        LivingEntity victim = event.getEntity();
        if (victim instanceof ServerPlayer) return;

        GluttonyData data = GluttonyData.of(player);
        if (!data.active()) return;

        int oldLevel = data.level();

        double maxHealth = victim.getAttributeValue(Attributes.MAX_HEALTH);
        double armor = victim.getAttributeValue(Attributes.ARMOR);
        double attack = victim.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double souls = Math.max(1.0, maxHealth * 0.5 + armor * 0.75) * GluttonyExtraction.soulMultiplier(data.level());
        double fraction = GluttonyExtraction.statFraction(data.level());

        data.addSouls(souls);
        data.addExtractedStats(maxHealth * fraction, Math.max(0, attack) * fraction);
        applyAttributes(player, data);

        if (data.awakening()) {
            data.stabilize();
            player.getFoodData().setFoodLevel(Math.max(6, player.getFoodData().getFoodLevel()));
            player.displayClientMessage(Component.literal("Gluttony has tasted its first soul.").withStyle(ChatFormatting.DARK_RED), false);
            player.displayClientMessage(Component.literal("The hunger settles—but it will never leave.").withStyle(ChatFormatting.GRAY), false);
        }

        if (data.level() > oldLevel) {
            player.displayClientMessage(Component.literal("GLUTTONY LEVEL " + data.level()).withStyle(ChatFormatting.GOLD), false);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!(event.getOriginal() instanceof ServerPlayer oldPlayer) || !(event.getEntity() instanceof ServerPlayer newPlayer)) return;
        event.getOriginal().reviveCaps();
        GluttonyData.copy(oldPlayer, newPlayer);
        event.getOriginal().invalidateCaps();
        applyAttributes(newPlayer, GluttonyData.of(newPlayer));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) applyAttributes(player, GluttonyData.of(player));
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) applyAttributes(player, GluttonyData.of(player));
    }

    private static void applyAttributes(ServerPlayer player, GluttonyData data) {
        setModifier(player.getAttribute(Attributes.MAX_HEALTH), HEALTH_ID, "Gluttony consumed health", data.extractedHealth());
        setModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_ID, "Gluttony consumed attack", data.extractedAttack());
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static void setModifier(AttributeInstance attribute, UUID id, String name, double amount) {
        if (attribute == null) return;
        AttributeModifier old = attribute.getModifier(id);
        if (old != null) attribute.removeModifier(old);
        if (amount > 0) attribute.addPermanentModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.ADDITION));
    }
}
