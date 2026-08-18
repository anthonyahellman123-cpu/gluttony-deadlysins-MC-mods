package com.anthonyahellman.gluttony.item;

import com.anthonyahellman.gluttony.data.GluttonyData;
import com.anthonyahellman.gluttony.data.SinData;
import com.anthonyahellman.gluttony.gameplay.AbilityHudSync;
import com.anthonyahellman.gluttony.gameplay.SoulEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class CursedAppleItem extends Item {
    public CursedAppleItem() {
        super(new Properties().food(new FoodProperties.Builder().nutrition(4).saturationMod(0.1F).alwaysEat().build()));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            SinData.NaturalSin selected = SinData.selected(serverPlayer);
            if (selected == SinData.NaturalSin.PRIDE) {
                player.displayClientMessage(Component.literal("Pride will not share its throne with Gluttony.")
                        .withStyle(ChatFormatting.GOLD), false);
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }
            if (selected == SinData.NaturalSin.GLUTTONY && GluttonyData.of(serverPlayer).active()) {
                player.displayClientMessage(Component.literal("Gluttony is already awake.")
                        .withStyle(ChatFormatting.DARK_RED), false);
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }
        }
        return super.use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            if (!SinData.tryChoose(player, SinData.NaturalSin.GLUTTONY)) return result;
            SoulEvents.refreshAttributes(player);
            GluttonyData data = GluttonyData.of(player);
            if (!data.active()) {
                data.beginAwakening();
                player.getFoodData().setSaturation(0.0F);
                player.displayClientMessage(Component.literal("Something bottomless awakens inside you.").withStyle(ChatFormatting.DARK_RED), false);
                player.displayClientMessage(Component.literal("Feed it before it consumes you.").withStyle(ChatFormatting.RED), false);
                AbilityHudSync.send(player);
            }
        }
        return result;
    }
}
