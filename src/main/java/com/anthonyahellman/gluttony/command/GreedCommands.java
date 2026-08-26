package com.anthonyahellman.gluttony.command;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.anthonyahellman.gluttony.data.GreedData;
import com.anthonyahellman.gluttony.greed.AvariceAppraisals;
import com.anthonyahellman.gluttony.gameplay.AbilityHudSync;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GluttonyMod.MOD_ID)
public final class GreedCommands {
    private GreedCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("greed")
                .then(Commands.literal("balance")
                        .executes(context -> balance(context.getSource().getPlayerOrException())))
                .then(Commands.literal("appraise")
                        .executes(context -> appraise(context.getSource().getPlayerOrException())))
                .then(Commands.literal("grant")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                                .executes(context -> grant(context.getSource().getPlayerOrException(),
                                        DoubleArgumentType.getDouble(context, "amount"))))));
    }

    private static int balance(ServerPlayer player) {
        player.displayClientMessage(Component.literal(String.format("Avarice: %.2f",
                GreedData.of(player).avarice())).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int appraise(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            player.displayClientMessage(Component.literal("Hold an item to appraise it.")
                    .withStyle(ChatFormatting.GRAY), false);
            return 0;
        }
        double each = AvariceAppraisals.value(stack);
        double total = AvariceAppraisals.stackValue(stack);
        player.displayClientMessage(Component.literal(each <= 0.0
                ? "Unappraised: " + stack.getHoverName().getString()
                : String.format("%s: %.2f each | %.2f for %d", stack.getHoverName().getString(), each,
                total, stack.getCount())).withStyle(each <= 0.0 ? ChatFormatting.GRAY : ChatFormatting.GOLD), false);
        return each <= 0.0 ? 0 : 1;
    }

    private static int grant(ServerPlayer player, double amount) {
        GreedData.of(player).addAvarice(amount);
        AbilityHudSync.send(player);
        player.displayClientMessage(Component.literal(String.format("Granted %.2f test Avarice.", amount))
                .withStyle(ChatFormatting.GOLD), false);
        return 1;
    }
}
