package com.anthonyahellman.gluttony.gameplay;

import com.anthonyahellman.gluttony.data.GluttonyData;
import com.anthonyahellman.gluttony.data.GreedData;
import com.anthonyahellman.gluttony.data.PrideData;
import com.anthonyahellman.gluttony.data.SinData;
import com.anthonyahellman.gluttony.network.AbilityStatePacket;
import com.anthonyahellman.gluttony.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import com.anthonyahellman.gluttony.greed.AvariceAppraisals;
import com.anthonyahellman.gluttony.network.AvariceValuesPacket;
import com.anthonyahellman.gluttony.network.GreedStatePacket;
import com.anthonyahellman.gluttony.network.PrideStatePacket;

public final class AbilityHudSync {
    private AbilityHudSync() {}

    public static void send(ServerPlayer player) {
        SinData.NaturalSin sin = SinData.selected(player);
        boolean unlocked = false;
        boolean evolved = false;
        int cooldown = 0;
        int recast = 0;
        int level = 0;
        double currentSouls = 0.0;
        double lifetimeSouls = 0.0;
        double extractedHealth = 0.0;
        double extractedAttack = 0.0;
        int nextLevelSouls = 0;
        boolean auraActive = false;
        double avarice = 0.0;

        if (sin == SinData.NaturalSin.GLUTTONY) {
            GluttonyData gluttony = GluttonyData.of(player);
            level = gluttony.level();
            currentSouls = gluttony.currentSouls();
            lifetimeSouls = gluttony.lifetimeSouls();
            extractedHealth = gluttony.extractedHealth();
            extractedAttack = gluttony.extractedAttack();
            nextLevelSouls = GluttonyData.soulsRequiredForLevel(Math.min(100, level + 1));
            unlocked = level >= SoulSiphon.UNLOCK_LEVEL;
            evolved = level >= Devour.UNLOCK_LEVEL;
            auraActive = Beelzebub.active(player);
        } else if (sin == SinData.NaturalSin.PRIDE) {
            PrideData pride = PrideData.of(player);
            unlocked = pride.totalBossKills() >= PrideAbility.UNLOCK_KILLS;
            evolved = pride.fullyAwakened();
            cooldown = PrideAbility.cooldownRemaining(player);
            recast = PrideAbility.recastRemaining(player);
        } else if (sin == SinData.NaturalSin.GREED) {
            avarice = GreedData.of(player).avarice();
            unlocked = true;
        }

        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new AbilityStatePacket(sin.ordinal(), unlocked, evolved, cooldown, recast,
                        level, currentSouls, lifetimeSouls, extractedHealth, extractedAttack,
                        nextLevelSouls, auraActive, avarice));
        if (sin == SinData.NaturalSin.GREED) sendGreed(player);
        if (sin == SinData.NaturalSin.PRIDE) sendPride(player);
    }

    private static void sendGreed(ServerPlayer player) {
        GreedData greed = GreedData.of(player);
        greed.refreshClaimWindow(player.serverLevel().getGameTime());
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new GreedStatePacket(
                greed.avarice(), greed.lifetimeEarned(), greed.lifetimeSpent(),
                greed.assetsDivested(), greed.vaultIncome(), greed.cofferIncome(),
                greed.marketActivity(), greed.contractClaims(), greed.marketStockStacks(),
                greed.coreHealthPurchases(), greed.coreAttackPurchases(), greed.coreArmorPurchases(),
                greed.premiumMovement(), greed.premiumAttackSpeed(), greed.premiumLuck(),
                greed.premiumKnockbackResistance(), greed.premiumAvariceYield(),
                greed.compoundInterestLevel(), greed.assetAppreciationLevel(), greed.contractLevel(),
                greed.claimsInWindow(), greed.currentClaimCost(), greed.claimResetAt()));
    }

    private static void sendPride(ServerPlayer player) {
        PrideData pride = PrideData.of(player);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PrideStatePacket(
                pride.count(PrideData.Trial.ENDER_DRAGON), pride.count(PrideData.Trial.WITHER),
                pride.count(PrideData.Trial.ELDER_GUARDIAN), pride.count(PrideData.Trial.WARDEN),
                pride.maxHealthBonus(), pride.attackDamageBonus(), pride.bossDamageBonus()));
    }

    public static void sendAppraisals(ServerPlayer player) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new AvariceValuesPacket(AvariceAppraisals.snapshot()));
    }
}
