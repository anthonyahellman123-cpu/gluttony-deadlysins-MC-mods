package com.anthonyahellman.gluttony.network;

import com.anthonyahellman.gluttony.gameplay.ShadowStep;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ShadowStepPacket {
    public static void encode(ShadowStepPacket packet, FriendlyByteBuf buffer) {}
    public static ShadowStepPacket decode(FriendlyByteBuf buffer) { return new ShadowStepPacket(); }

    public static void handle(ShadowStepPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) ShadowStep.tryCast(player);
        });
        context.setPacketHandled(true);
    }
}
