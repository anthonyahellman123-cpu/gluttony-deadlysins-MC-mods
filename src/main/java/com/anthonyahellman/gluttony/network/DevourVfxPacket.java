package com.anthonyahellman.gluttony.network;

import com.anthonyahellman.gluttony.client.DevourVfxClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** One cosmetic event emitted only after the authoritative Devour bite succeeds. */
public record DevourVfxPacket(int casterId, int targetId,
                              double sourceX, double sourceY, double sourceZ,
                              double destinationX, double destinationY, double destinationZ,
                              float chargeStrength) {
    public static void encode(DevourVfxPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.casterId);
        buffer.writeVarInt(packet.targetId);
        buffer.writeDouble(packet.sourceX);
        buffer.writeDouble(packet.sourceY);
        buffer.writeDouble(packet.sourceZ);
        buffer.writeDouble(packet.destinationX);
        buffer.writeDouble(packet.destinationY);
        buffer.writeDouble(packet.destinationZ);
        buffer.writeFloat(packet.chargeStrength);
    }

    public static DevourVfxPacket decode(FriendlyByteBuf buffer) {
        return new DevourVfxPacket(buffer.readVarInt(), buffer.readVarInt(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readFloat());
    }

    public static void handle(DevourVfxPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> DevourVfxClient.accept(packet)));
        context.setPacketHandled(true);
    }
}
