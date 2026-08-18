package com.anthonyahellman.gluttony.command;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.anthonyahellman.gluttony.data.PrideData;
import com.anthonyahellman.gluttony.data.SinData;
import com.anthonyahellman.gluttony.gameplay.AbilityHudSync;
import com.anthonyahellman.gluttony.gameplay.PrideEvents;
import com.anthonyahellman.gluttony.gameplay.PrideProgression;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
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
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("pride");
        root.then(Commands.literal("stats").executes(context -> showStats(context.getSource())));

        LiteralArgumentBuilder<CommandSourceStack> grant = Commands.literal("grant")
                .requires(source -> source.hasPermission(2));
        LiteralArgumentBuilder<CommandSourceStack> complete = Commands.literal("complete")
                .requires(source -> source.hasPermission(2));
        for (PrideData.Trial trial : PrideData.Trial.values()) {
            grant.then(Commands.literal(trial.commandName())
                    .then(Commands.argument("count", IntegerArgumentType.integer(1, trial.required()))
                            .executes(context -> grant(context.getSource(), trial,
                                    IntegerArgumentType.getInteger(context, "count")))));
            complete.then(Commands.literal(trial.commandName())
                    .executes(context -> setTrial(context.getSource(), trial, trial.required())));
        }

        root.then(grant);
        root.then(complete);
        root.then(Commands.literal("complete_all").requires(source -> source.hasPermission(2))
                .executes(context -> completeAll(context.getSource())));
        root.then(Commands.literal("reset").requires(source -> source.hasPermission(2))
                .executes(context -> reset(context.getSource())));
        dispatcher.register(root);
    }

    private static int grant(CommandSourceStack source, PrideData.Trial trial, int amount) {
        ServerPlayer player = player(source);
        if (player == null) return 0;
        PrideData data = PrideData.of(player);
        return setTrial(source, trial, data.count(trial) + amount);
    }

    private static int setTrial(CommandSourceStack source, PrideData.Trial trial, int count) {
        ServerPlayer player = player(source);
        if (player == null) return 0;
        PrideData data = PrideData.of(player);
        double oldMaxHealth = player.getMaxHealth();
        data.setCount(trial, count);
        PrideProgression.applyAttributes(player);
        player.heal((float) Math.max(0.0, player.getMaxHealth() - oldMaxHealth));
        AbilityHudSync.send(player);
        source.sendSuccess(() -> Component.literal(String.format("Set %s to %d / %d.",
                trial.displayName(), data.count(trial), trial.required())).withStyle(ChatFormatting.GOLD), false);
        return 1;
    }

    private static int completeAll(CommandSourceStack source) {
        ServerPlayer player = player(source);
        if (player == null) return 0;
        PrideData data = PrideData.of(player);
        double oldMaxHealth = player.getMaxHealth();
        for (PrideData.Trial trial : PrideData.Trial.values()) data.setCount(trial, trial.required());
        PrideProgression.applyAttributes(player);
        player.heal((float) Math.max(0.0, player.getMaxHealth() - oldMaxHealth));
        AbilityHudSync.send(player);
        source.sendSuccess(() -> Component.literal("Completed every Pride trial for testing.")
                .withStyle(ChatFormatting.GOLD), false);
        return 1;
    }

    private static int reset(CommandSourceStack source) {
        ServerPlayer player = player(source);
        if (player == null) return 0;
        PrideData.of(player).reset();
        PrideProgression.applyAttributes(player);
        AbilityHudSync.send(player);
        source.sendSuccess(() -> Component.literal("Reset all Pride trial progress.")
                .withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int showStats(CommandSourceStack source) {
        ServerPlayer player = player(source);
        if (player == null) return 0;
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
        source.sendSuccess(() -> Component.literal("Sovereign's Advance: "
                + (data.totalBossKills() >= 4 ? "Unlocked" : "Locked (" + data.totalBossKills() + " / 4 bosses)")), false);
        if (data.fullyAwakened()) {
            source.sendSuccess(() -> Component.literal("Evolution: Absolute Domination").withStyle(ChatFormatting.GOLD), false);
        }
        return 1;
    }

    private static ServerPlayer player(CommandSourceStack source) {
        try {
            return source.getPlayerOrException();
        } catch (Exception ignored) {
            source.sendFailure(Component.literal("This command must be used by a player."));
            return null;
        }
    }
}
