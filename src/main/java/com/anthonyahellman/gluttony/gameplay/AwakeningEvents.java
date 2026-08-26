package com.anthonyahellman.gluttony.gameplay;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.anthonyahellman.gluttony.data.GluttonyData;
import com.anthonyahellman.gluttony.data.SinData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GluttonyMod.MOD_ID)
public final class AwakeningEvents {
    private AwakeningEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (SinData.selected(player) != SinData.NaturalSin.GLUTTONY) return;

        GluttonyData data = GluttonyData.of(player);
        if (!data.active() || !data.awakening()) return;

        FoodData food = player.getFoodData();
        food.setSaturation(0.0F);

        // One hunger point every two seconds: enough time to hunt, but not to hesitate.
        if (player.tickCount % 40 == 0 && food.getFoodLevel() > 0) {
            food.setFoodLevel(food.getFoodLevel() - 1);
        }
    }
}
