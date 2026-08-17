package com.anthonyahellman.gluttony.command;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.anthonyahellman.gluttony.data.PrideData;
import com.anthonyahellman.gluttony.data.SinData;
import com.anthonyahellman.gluttony.gameplay.PrideEvents;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GluttonyMod.MOD_ID)
public final class PrideCommands {
    private PrideCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("pride")
                .then(Commands.literal("stats").executes(context -> showStats(context.getSource()))));
    }

    private static int showStats(CommandSourceStack source) {
        final ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception ignored) {
            source.sendFailure(Component.literal("This command must be used by a player."));
            return 0;
        }

        PrideData data = PrideData.of(player);
        boolean active = SinData.selected(player) == SinData.NaturalSin.PRIDE;
        source.sendSuccess(() -> Component.literal("=== THE ROOTS OF SIN: PRIDE ===").withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.literal("State: " + (active ? "Awakened" : "Dormant")), false);
        for (PrideData.Trial trial : PrideData.Trial.values()) {
            source.sendSuccess(() -> Component.literal(String.format("%s: %d / %d%s",
                    trial.displayName(), data.count(trial), trial.required(), data.complete(trial) ? " — COMPLETE" : "")), false);
        }
        source.sendSuccess(() -> Component.literal(String.format("Damage: %.0f%% ordinary | %.0f%% bosses",
                PrideEvents.NON_BOSS_DAMAGE_MULTIPLIER * 100.0F,
                (PrideEvents.BOSS_DAMAGE_MULTIPLIER + data.bossDamageBonus()) * 100.0)), false);
        source.sendSuccess(() -> Component.literal(String.format("Conquered Stats: +%.0f max health | +%.0f attack",
                data.maxHealthBonus(), data.attackDamageBonus())), false);
        source.sendSuccess(() -> Component.literal("Trials completed: " + data.completedTrials() + " / 4"), false);
        return 1;
    }
}
