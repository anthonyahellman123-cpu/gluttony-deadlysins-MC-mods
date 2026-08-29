package com.anthonyahellman.gluttony.network;

import com.anthonyahellman.gluttony.client.PrideVfxClientEffect;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record PrideVfxTestPacket(int action, int entityId, double x, double y, double z) {
    public static final int START_DESCENT = 0;
    public static final int IMPACT = 1;

    public static void encode(PrideVfxTestPacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.action);
        buffer.writeVarInt(packet.entityId);
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
    }

    public static PrideVfxTestPacket decode(FriendlyByteBuf buffer) {
        return new PrideVfxTestPacket(buffer.readUnsignedByte(), buffer.readVarInt(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public static void handle(PrideVfxTestPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> PrideVfxClientEffect.accept(packet)));
        context.setPacketHandled(true);
    }
}
