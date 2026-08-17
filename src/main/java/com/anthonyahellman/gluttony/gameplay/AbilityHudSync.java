package com.anthonyahellman.gluttony.gameplay;

import com.anthonyahellman.gluttony.data.GluttonyData;
import com.anthonyahellman.gluttony.data.PrideData;
import com.anthonyahellman.gluttony.data.SinData;
import com.anthonyahellman.gluttony.network.AbilityStatePacket;
import com.anthonyahellman.gluttony.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public final class AbilityHudSync {
    private AbilityHudSync() {}

    public static void send(ServerPlayer player) {
        SinData.NaturalSin sin = SinData.selected(player);
        boolean unlocked = false;
        boolean evolved = false;
        int cooldown = 0;
        int recast = 0;

        if (sin == SinData.NaturalSin.GLUTTONY) {
            unlocked = GluttonyData.of(player).level() >= SoulSiphon.UNLOCK_LEVEL;
        } else if (sin == SinData.NaturalSin.PRIDE) {
            PrideData pride = PrideData.of(player);
            unlocked = pride.totalBossKills() >= PrideAbility.UNLOCK_KILLS;
            evolved = pride.fullyAwakened();
            cooldown = PrideAbility.cooldownRemaining(player);
            recast = PrideAbility.recastRemaining(player);
        }

        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new AbilityStatePacket(sin.ordinal(), unlocked, evolved, cooldown, recast));
    }
}
