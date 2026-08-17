package com.anthonyahellman.gluttony.network;

import com.anthonyahellman.gluttony.gameplay.PrideAbility;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class SoulSiphonPacket {
    public static void encode(SoulSiphonPacket packet, FriendlyByteBuf buffer) {}

    public static SoulSiphonPacket decode(FriendlyByteBuf buffer) {
        return new SoulSiphonPacket();
    }

    public static void handle(SoulSiphonPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) PrideAbility.tryCast(player);
        });
        context.setPacketHandled(true);
    }
}
