package com.anthonyahellman.gluttony.command;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.anthonyahellman.gluttony.data.GluttonyData;
import com.anthonyahellman.gluttony.gameplay.GluttonyExtraction;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GluttonyMod.MOD_ID)
public final class GluttonyCommands {
    private GluttonyCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("gluttony")
                .then(Commands.literal("stats").executes(context -> showStats(context.getSource())))
                .then(Commands.literal("grant")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("souls", DoubleArgumentType.doubleArg(0.0))
                                .executes(context -> grantSouls(
                                        context.getSource(), DoubleArgumentType.getDouble(context, "souls"))))));
    }

    private static int grantSouls(CommandSourceStack source, double souls) {
        final ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception ignored) {
            source.sendFailure(Component.literal("This command must be used by a player."));
            return 0;
        }
        GluttonyData data = GluttonyData.of(player);
        data.addSouls(souls);
        source.sendSuccess(() -> Component.literal(String.format(
                "Granted %.2f souls. Gluttony level: %d", souls, data.level())).withStyle(ChatFormatting.GOLD), false);
        return 1;
    }

    private static int showStats(CommandSourceStack source) {
        final ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception ignored) {
            source.sendFailure(Component.literal("This command must be used by a player."));
            return 0;
        }

        GluttonyData data = GluttonyData.of(player);
        source.sendSuccess(() -> Component.literal("=== THE ROOTS OF SIN: GLUTTONY ===").withStyle(ChatFormatting.DARK_RED), false);
        source.sendSuccess(() -> Component.literal("State: " + state(data)), false);
        source.sendSuccess(() -> Component.literal("Level: " + data.level()), false);
        source.sendSuccess(() -> Component.literal(String.format("Souls: %.2f", data.currentSouls())), false);
        source.sendSuccess(() -> Component.literal(String.format("Lifetime Souls: %.2f / %d", data.lifetimeSouls(), GluttonyData.soulsRequiredForLevel(Math.min(100, data.level() + 1)))), false);
        source.sendSuccess(() -> Component.literal(String.format("Extraction: %.0f%% stats | %.2fx souls", GluttonyExtraction.statFraction(data.level()) * 100.0, GluttonyExtraction.soulMultiplier(data.level()))), false);
        source.sendSuccess(() -> Component.literal(String.format("Consumed Stats: +%.2f health | +%.2f attack", data.extractedHealth(), data.extractedAttack())), false);
        source.sendSuccess(() -> Component.literal("Soul Siphon: " + (data.level() >= 10 ? "Unlocked" : "Locked (Level 10)"))
                .withStyle(data.level() >= 10 ? ChatFormatting.DARK_PURPLE : ChatFormatting.DARK_GRAY), false);
        source.sendSuccess(() -> Component.literal("Shadow Step: " + (data.level() >= 5 ? "Unlocked" : "Locked (Level 5)"))
                .withStyle(data.level() >= 5 ? ChatFormatting.DARK_PURPLE : ChatFormatting.DARK_GRAY), false);
        source.sendSuccess(() -> Component.literal("Devour: " + (data.level() >= 50 ? "Unlocked" : "Locked (Level 50)"))
                .withStyle(data.level() >= 50 ? ChatFormatting.DARK_RED : ChatFormatting.DARK_GRAY), false);
        source.sendSuccess(() -> Component.literal("Perfect Devour: " + (data.level() >= 100 ? "Unlocked" : "Locked (Level 100)"))
                .withStyle(data.level() >= 100 ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY), false);
        return 1;
    }

    private static String state(GluttonyData data) {
        if (!data.active()) return "Dormant";
        if (data.awakening()) return "Awakening — feed immediately";
        return "Awakened";
    }
}
