package com.anthonyahellman.gluttony.network;

import com.anthonyahellman.gluttony.client.AbilityHudOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.function.Supplier;

public record AbilityStatePacket(int sin, boolean unlocked, boolean evolved, int cooldownTicks, int recastTicks) {
    public static void encode(AbilityStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.sin);
        buffer.writeBoolean(packet.unlocked);
        buffer.writeBoolean(packet.evolved);
        buffer.writeVarInt(packet.cooldownTicks);
        buffer.writeVarInt(packet.recastTicks);
    }

    public static AbilityStatePacket decode(FriendlyByteBuf buffer) {
        return new AbilityStatePacket(buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean(),
                buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(AbilityStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> AbilityHudOverlay.update(packet)));
        context.setPacketHandled(true);
    }
}
