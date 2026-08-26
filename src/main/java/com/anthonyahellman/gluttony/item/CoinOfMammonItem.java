package com.anthonyahellman.gluttony.item;

import com.anthonyahellman.gluttony.data.SinData;
import com.anthonyahellman.gluttony.gameplay.AbilityHudSync;
import com.anthonyahellman.gluttony.gameplay.SoulEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class CoinOfMammonItem extends Item {
    public CoinOfMammonItem() {
        super(new Properties().stacksTo(64));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        SinData.NaturalSin selected = SinData.selected(serverPlayer);
        if (selected != SinData.NaturalSin.NONE && selected != SinData.NaturalSin.GREED) {
            player.displayClientMessage(Component.literal("Mammon will not invest in a soul already claimed.")
                    .withStyle(ChatFormatting.GOLD), false);
            return InteractionResultHolder.fail(stack);
        }
        if (selected == SinData.NaturalSin.GREED) {
            player.displayClientMessage(Component.literal("Mammon's claim is already stamped upon your soul.")
                    .withStyle(ChatFormatting.GOLD), false);
            return InteractionResultHolder.fail(stack);
        }
        if (!SinData.tryChoose(serverPlayer, SinData.NaturalSin.GREED)) {
            return InteractionResultHolder.fail(stack);
        }

        if (!player.getAbilities().instabuild) stack.shrink(1);
        SoulEvents.refreshAttributes(serverPlayer);
        player.displayClientMessage(Component.literal("All things have a price...")
                .withStyle(ChatFormatting.GOLD), false);
        player.displayClientMessage(Component.literal("And Mammon has accepted yours.")
                .withStyle(ChatFormatting.DARK_GREEN), false);
        AbilityHudSync.send(serverPlayer);
        AbilityHudSync.sendAppraisals(serverPlayer);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
