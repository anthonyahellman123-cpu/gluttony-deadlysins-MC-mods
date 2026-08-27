package com.anthonyahellman.gluttony.command;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.anthonyahellman.gluttony.data.GreedData;
import com.anthonyahellman.gluttony.greed.AvariceAppraisals;
import com.anthonyahellman.gluttony.gameplay.AbilityHudSync;
import com.anthonyahellman.gluttony.block.entity.GreedsVaultBlockEntity;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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
                                        DoubleArgumentType.getDouble(context, "amount")))))
                .then(Commands.literal("settle_vault")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> settleVault(context.getSource().getPlayerOrException()))));
    }

    private static int balance(ServerPlayer player) {
        player.displayClientMessage(Component.literal(String.format("Avarice: %.2f",
                GreedData.of(player).avarice())).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int appraise(ServerPlayer player) {
        AvariceAppraisals.ensureDerived(player.server);
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            player.displayClientMessage(Component.literal("Hold an item to appraise it.")
                    .withStyle(ChatFormatting.GRAY), false);
            return 0;
        }
        AvariceAppraisals.Inspection appraisal = AvariceAppraisals.inspectServer(stack);
        player.displayClientMessage(Component.literal("Item: " + appraisal.itemId())
                .withStyle(ChatFormatting.GRAY), false);
        if (!appraisal.appraised()) {
            player.displayClientMessage(Component.literal("Status: UNRESOLVED")
                    .withStyle(ChatFormatting.RED), false);
            if (appraisal.resourceFamily() != null) {
                player.displayClientMessage(Component.literal("Resource Family: "
                                + appraisal.resourceFamily().name())
                        .withStyle(ChatFormatting.YELLOW), false);
            }
            if (appraisal.resourceFamilyPath() != null) {
                player.displayClientMessage(Component.literal("Family Path: "
                                + appraisal.resourceFamilyPath())
                        .withStyle(ChatFormatting.DARK_GRAY), false);
            }
            player.displayClientMessage(Component.literal("Reason: " + appraisal.unresolvedReason())
                    .withStyle(ChatFormatting.GRAY), false);
            return 0;
        }
        player.displayClientMessage(Component.literal("Status: APPRAISED")
                .withStyle(ChatFormatting.GREEN), false);
        player.displayClientMessage(Component.literal(String.format("Value: %.2f Ava each", appraisal.value()))
                .withStyle(ChatFormatting.GOLD), false);
        player.displayClientMessage(Component.literal("Source: " + appraisal.source().name())
                .withStyle(ChatFormatting.YELLOW), false);
        if (appraisal.resourceFamily() != null) {
            player.displayClientMessage(Component.literal("Resource Family: "
                            + appraisal.resourceFamily().name())
                    .withStyle(ChatFormatting.YELLOW), false);
        }
        if (appraisal.resourceFamilyPath() != null) {
            player.displayClientMessage(Component.literal("Family Path: "
                            + appraisal.resourceFamilyPath())
                    .withStyle(ChatFormatting.DARK_GRAY), false);
        }
        if (appraisal.recipeId() != null) {
            player.displayClientMessage(Component.literal("Recipe: " + appraisal.recipeId())
                    .withStyle(ChatFormatting.DARK_GRAY), false);
        }
        player.displayClientMessage(Component.literal(String.format("Quantity: %d | Total: %.2f Ava | Tier: %s",
                        stack.getCount(), AvariceAppraisals.serverStackValue(stack), appraisal.tier().displayName()))
                .withStyle(ChatFormatting.GOLD), false);
        return 1;
    }

    private static int grant(ServerPlayer player, double amount) {
        GreedData.of(player).addAvarice(amount);
        AbilityHudSync.send(player);
        player.displayClientMessage(Component.literal(String.format("Granted %.2f test Avarice.", amount))
                .withStyle(ChatFormatting.GOLD), false);
        return 1;
    }

    private static int settleVault(ServerPlayer player) {
        HitResult hit = player.pick(6.0, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit)
                || !(player.level().getBlockEntity(blockHit.getBlockPos()) instanceof GreedsVaultBlockEntity vault)
                || !vault.forceProduction(player)) {
            player.displayClientMessage(Component.literal("Look directly at your Greed's Vault within six blocks.")
                    .withStyle(ChatFormatting.RED), false);
            return 0;
        }
        player.displayClientMessage(Component.literal("Forced one Vault settlement for testing.")
                .withStyle(ChatFormatting.GOLD), false);
        return 1;
    }
}
