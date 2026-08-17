package com.anthonyahellman.gluttony.item;

import com.anthonyahellman.gluttony.data.SinData;
import com.anthonyahellman.gluttony.gameplay.AbilityHudSync;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class PrideSolItem extends Item {
    public PrideSolItem() {
        super(new Properties().food(new FoodProperties.Builder().nutrition(4).saturationMod(1.0F).alwaysEat().build()));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            SinData.NaturalSin selected = SinData.selected(serverPlayer);
            if (selected != SinData.NaturalSin.NONE && selected != SinData.NaturalSin.PRIDE) {
                player.displayClientMessage(Component.literal("Pride refuses a throne already claimed by another sin.")
                        .withStyle(ChatFormatting.GOLD), false);
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }
            if (selected == SinData.NaturalSin.PRIDE) {
                player.displayClientMessage(Component.literal("Pride already recognizes no equal.")
                        .withStyle(ChatFormatting.GOLD), false);
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }
        }
        return super.use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide && entity instanceof ServerPlayer player
                && SinData.tryChoose(player, SinData.NaturalSin.PRIDE)) {
            player.displayClientMessage(Component.literal("The world remembers its rightful superior.")
                    .withStyle(ChatFormatting.GOLD), false);
            player.displayClientMessage(Component.literal("Only worthy victories can elevate Pride.")
                    .withStyle(ChatFormatting.YELLOW), false);
            AbilityHudSync.send(player);
        }
        return result;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
