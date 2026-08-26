package com.anthonyahellman.gluttony.gameplay;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.anthonyahellman.gluttony.data.GreedData;
import com.anthonyahellman.gluttony.data.SinData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = GluttonyMod.MOD_ID)
public final class GreedEvents {
    private static final String DOT = "RootsOfSinCompoundInterest";
    private static final String DOT_SOURCE = "RootsOfSinCompoundInterestDamage";

    private GreedEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)
                || SinData.selected(player) != SinData.NaturalSin.GREED) return;
        GreedData data = GreedData.of(player);
        int oldClaims = data.claimsInWindow();
        data.refreshClaimWindow(player.serverLevel().getGameTime());
        if (oldClaims != data.claimsInWindow()) AbilityHudSync.send(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || SinData.selected(player) != SinData.NaturalSin.GREED) return;
        GreedData data = GreedData.of(player);
        if (data.contractLevel() <= 0) return;
        long now = player.serverLevel().getGameTime();
        data.refreshClaimWindow(now);
        double claim = data.currentClaimCost();
        if (!data.spendAvarice(claim)) return;

        event.setCanceled(true);
        data.recordContractClaim(now);
        player.setHealth((float) (player.getMaxHealth() * 0.75));
        player.invulnerableTime = 40;
        player.clearFire();
        AbilityHudSync.send(player);
    }

    @SubscribeEvent
    public static void onWeaponDamage(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)
                || SinData.selected(player) != SinData.NaturalSin.GREED
                || player.getPersistentData().getBoolean(DOT_SOURCE)
                || player.getMainHandItem().isEmpty()) return;
        int level = GreedData.of(player).compoundInterestLevel();
        if (level <= 0 || event.getAmount() <= 0.0F) return;

        LivingEntity target = event.getEntity();
        CompoundTag dot = target.getPersistentData().getCompound(DOT);
        int stacks = dot.hasUUID("Owner") && dot.getUUID("Owner").equals(player.getUUID())
                ? Math.min(level, dot.getInt("Stacks") + 1) : 1;
        long now = target.level().getGameTime();
        dot.putUUID("Owner", player.getUUID());
        dot.putInt("Stacks", stacks);
        dot.putFloat("StoredDamage", event.getAmount());
        dot.putLong("Expires", now + stacks * 60L);
        if (!dot.contains("NextTick") || dot.getLong("NextTick") <= now) dot.putLong("NextTick", now + 30L);
        target.getPersistentData().put(DOT, dot);
    }

    @SubscribeEvent
    public static void onLivingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide || !target.getPersistentData().contains(DOT)) return;
        CompoundTag dot = target.getPersistentData().getCompound(DOT);
        long now = target.level().getGameTime();
        if (now > dot.getLong("Expires") || !dot.hasUUID("Owner")) {
            target.getPersistentData().remove(DOT);
            return;
        }
        if (now < dot.getLong("NextTick")) return;
        if (!(target.level() instanceof ServerLevel level)) return;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(dot.getUUID("Owner"));
        if (owner == null || SinData.selected(owner) != SinData.NaturalSin.GREED) {
            target.getPersistentData().remove(DOT);
            return;
        }
        int cap = GreedData.of(owner).compoundInterestLevel();
        int stacks = Math.min(cap, Math.max(0, dot.getInt("Stacks")));
        if (stacks <= 0) {
            target.getPersistentData().remove(DOT);
            return;
        }
        float damage = dot.getFloat("StoredDamage") * 0.25F * stacks;
        owner.getPersistentData().putBoolean(DOT_SOURCE, true);
        try {
            target.hurt(owner.damageSources().indirectMagic(owner, owner), damage);
        } finally {
            owner.getPersistentData().remove(DOT_SOURCE);
        }
        dot.putLong("NextTick", now + 30L);
        target.getPersistentData().put(DOT, dot);
    }

    @SubscribeEvent
    public static void onMobDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        int level = eligibleLuckLevel(player);
        if (level <= 0) return;
        Collection<ItemEntity> drops = event.getDrops();
        List<ItemEntity> bonuses = new ArrayList<>();
        for (ItemEntity drop : List.copyOf(drops)) {
            addBonusEntities(player, drop.getItem(), drop.getX(), drop.getY(), drop.getZ(), level, bonuses);
        }
        drops.addAll(bonuses);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof ServerLevel level)) return;
        int luck = eligibleLuckLevel(player);
        if (luck <= 0) return;
        BlockPos pos = event.getPos();
        List<ItemStack> normalDrops = Block.getDrops(event.getState(), level, pos,
                level.getBlockEntity(pos), player, player.getMainHandItem());
        List<ItemEntity> bonuses = new ArrayList<>();
        for (ItemStack stack : normalDrops) {
            addBonusEntities(player, stack, pos.getX() + 0.5, pos.getY() + 0.5,
                    pos.getZ() + 0.5, luck, bonuses);
        }
        bonuses.forEach(level::addFreshEntity);
    }

    private static int eligibleLuckLevel(ServerPlayer player) {
        return SinData.selected(player) == SinData.NaturalSin.GREED ? GreedData.of(player).premiumLuck() : 0;
    }

    private static void addBonusEntities(ServerPlayer player, ItemStack original, double x, double y, double z,
                                         int level, Collection<ItemEntity> output) {
        if (original.isEmpty()) return;
        double exact = original.getCount() * level * 0.20;
        int count = (int) Math.floor(exact);
        if (player.getRandom().nextDouble() < exact - count) count++;
        while (count > 0) {
            ItemStack bonus = original.copy();
            int amount = Math.min(count, bonus.getMaxStackSize());
            bonus.setCount(amount);
            output.add(new ItemEntity(player.level(), x, y, z, bonus));
            count -= amount;
        }
    }
}
