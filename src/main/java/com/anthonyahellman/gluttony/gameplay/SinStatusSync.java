package com.anthonyahellman.gluttony.gameplay;

import com.anthonyahellman.gluttony.data.GluttonyData;
import com.anthonyahellman.gluttony.data.PrideData;
import com.anthonyahellman.gluttony.data.SinData;
import com.anthonyahellman.gluttony.network.ModNetwork;
import com.anthonyahellman.gluttony.network.SinStatusPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

public final class SinStatusSync {
    private SinStatusSync() {}

    public static void send(ServerPlayer player) {
        SinData.NaturalSin sin = SinData.selected(player);
        SinStatusPacket packet = switch (sin) {
            case GLUTTONY -> gluttony(player);
            case PRIDE -> pride(player);
            default -> new SinStatusPacket(0, "NO SIN AWAKENED", "Dormant",
                    "Consume a Sin catalyst to awaken a Root of Sin.",
                    List.of(), List.of(), 0.0);
        };

        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    private static SinStatusPacket gluttony(ServerPlayer player) {
        GluttonyData data = GluttonyData.of(player);
        int level = data.level();
        String stage = level >= 50 ? "DEVOUR" : level >= SoulSiphon.UNLOCK_LEVEL ? "SOUL SIPHON" : "HUNGER";
        int nextLevel = level < SoulSiphon.UNLOCK_LEVEL ? SoulSiphon.UNLOCK_LEVEL : level < 50 ? 50 : 100;
        int currentFloor = level < SoulSiphon.UNLOCK_LEVEL ? 1 : level < 50 ? SoulSiphon.UNLOCK_LEVEL : 50;
        double progress = nextLevel <= currentFloor ? 1.0 : (level - currentFloor) / (double) (nextLevel - currentFloor);

        List<String> stats = List.of(
                "Level: " + level,
                String.format("Souls: %.2f", data.currentSouls()),
                String.format("Souls Consumed: %.2f", data.lifetimeSouls()),
                String.format("Consumed Max Health: +%.2f", data.extractedHealth()),
                String.format("Consumed Attack: +%.2f", data.extractedAttack()),
                String.format("Extraction: %.0f%%", GluttonyExtraction.statFraction(level) * 100.0)
        );

        List<String> progression;
        if (level < SoulSiphon.UNLOCK_LEVEL) {
            progression = List.of("Soul Siphon awakens at Level 10", "Kill and consume to grow stronger.");
        } else if (level < 50) {
            progression = List.of("Devour awakens at Level 50", "Soul Siphon evolves instead of becoming a separate power.");
        } else {
            progression = List.of("Devour awakened", "Beelzebub: future capstone", "Continue feeding Gluttony.");
        }

        return new SinStatusPacket(SinData.NaturalSin.GLUTTONY.ordinal(), "GLUTTONY", stage,
                "An endless hunger that converts what it consumes into permanent strength.",
                stats, progression, Math.max(0.0, Math.min(1.0, progress)));
    }

    private static SinStatusPacket pride(ServerPlayer player) {
        PrideData data = PrideData.of(player);
        List<String> stats = List.of(
                String.format("Max Health Bonus: +%.0f", data.maxHealthBonus()),
                String.format("Attack Bonus: +%.0f", data.attackDamageBonus()),
                String.format("Boss Damage Bonus: +%.0f%%", data.bossDamageBonus() * 100.0),
                "Trials Completed: " + data.completedTrials() + "/4"
        );
        List<String> progression = List.of(
                trialLine(data, PrideData.Trial.ENDER_DRAGON),
                trialLine(data, PrideData.Trial.WITHER),
                trialLine(data, PrideData.Trial.ELDER_GUARDIAN),
                trialLine(data, PrideData.Trial.WARDEN)
        );
        double progress = data.totalBossKills() / 26.0;
        String stage = data.fullyAwakened() ? "LUCIFER — AWAKENED" : "LUCIFER";
        return new SinStatusPacket(SinData.NaturalSin.PRIDE.ordinal(), "PRIDE", stage,
                "Dominance proven through impossible prey. Each conquered trial strengthens Pride.",
                stats, progression, Math.max(0.0, Math.min(1.0, progress)));
    }

    private static String trialLine(PrideData data, PrideData.Trial trial) {
        return trial.displayName() + ": " + data.count(trial) + "/" + trial.required();
    }
}
