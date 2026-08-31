package com.anthonyahellman.gluttony.network;

import com.anthonyahellman.gluttony.client.PrideClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record PrideStatePacket(int dragons, int withers, int guardians, int wardens, long totalConquests,
                               double maxHealthBonus, double attackBonus, double bossDamageBonus) {
    public static void encode(PrideStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.dragons);
        buffer.writeVarInt(packet.withers);
        buffer.writeVarInt(packet.guardians);
        buffer.writeVarInt(packet.wardens);
        buffer.writeVarLong(packet.totalConquests);
        buffer.writeDouble(packet.maxHealthBonus);
        buffer.writeDouble(packet.attackBonus);
        buffer.writeDouble(packet.bossDamageBonus);
    }

    public static PrideStatePacket decode(FriendlyByteBuf buffer) {
        return new PrideStatePacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarLong(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public static void handle(PrideStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> PrideClientState.update(packet)));
        context.setPacketHandled(true);
    }
}
