package com.anthonyahellman.gluttony.network;

import com.anthonyahellman.gluttony.gameplay.SinStatusSync;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class SinStatusRequestPacket {
    public static void encode(SinStatusRequestPacket packet, FriendlyByteBuf buffer) {}
    public static SinStatusRequestPacket decode(FriendlyByteBuf buffer) { return new SinStatusRequestPacket(); }

    public static void handle(SinStatusRequestPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) SinStatusSync.send(player);
        });
        context.setPacketHandled(true);
    }
}
