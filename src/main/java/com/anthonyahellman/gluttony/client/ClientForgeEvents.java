package com.anthonyahellman.gluttony.client;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.anthonyahellman.gluttony.network.ModNetwork;
import com.anthonyahellman.gluttony.network.SinStatusRequestPacket;
import com.anthonyahellman.gluttony.network.SoulSiphonPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GluttonyMod.MOD_ID, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private ClientForgeEvents() {}

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.isPaused()) return;
        while (ClientModEvents.SOUL_SIPHON.consumeClick()) {
            ModNetwork.CHANNEL.sendToServer(new SoulSiphonPacket());
        }
        while (ClientModEvents.SIN_STATUS.consumeClick()) {
            ModNetwork.CHANNEL.sendToServer(new SinStatusRequestPacket());
        }
    }
}
