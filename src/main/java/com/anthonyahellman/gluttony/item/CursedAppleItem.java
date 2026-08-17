package com.anthonyahellman.gluttony.item;

import com.anthonyahellman.gluttony.data.GluttonyData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class CursedAppleItem extends Item {
    public CursedAppleItem() {
        super(new Properties().food(new FoodProperties.Builder().nutrition(4).saturationMod(0.1F).alwaysEat().build()));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            GluttonyData data = GluttonyData.of(player);
            if (!data.active()) {
                data.activate();
                player.displayClientMessage(Component.literal("Something bottomless awakens inside you.").withStyle(ChatFormatting.DARK_RED), false);
            }
        }
        return result;
    }
}
