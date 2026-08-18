package com.anthonyahellman.gluttony.command;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.anthonyahellman.gluttony.data.SinData;
import com.anthonyahellman.gluttony.gameplay.AbilityHudSync;
import com.anthonyahellman.gluttony.gameplay.SoulEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GluttonyMod.MOD_ID)
public final class SinCommands {
    private SinCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("sin")
                .then(Commands.literal("clear")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> clear(context.getSource().getPlayerOrException()))));
    }

    private static int clear(ServerPlayer player) {
        SinData.NaturalSin old = SinData.selected(player);
        SinData.clear(player);
        SoulEvents.refreshAttributes(player);
        AbilityHudSync.send(player);
        player.displayClientMessage(Component.literal("Cleared natural sin: " + old.name()
                + ". Saved progression was preserved.").withStyle(ChatFormatting.GRAY), false);
        return 1;
    }
}
