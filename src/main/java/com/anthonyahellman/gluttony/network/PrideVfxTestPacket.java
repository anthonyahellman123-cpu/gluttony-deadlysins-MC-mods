package com.anthonyahellman.gluttony.network;

import com.anthonyahellman.gluttony.client.PrideVfxClientEffect;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record PrideVfxTestPacket(int action, int entityId, double x, double y, double z,
                                 double radius, int durationTicks, int variant, int delayTicks) {
    public static final int START_DESCENT = 0;
    public static final int IMPACT = 1;
    public static final int WAVE = 2;

    public static PrideVfxTestPacket descent(int entityId, double x, double y, double z) {
        return new PrideVfxTestPacket(START_DESCENT, entityId, x, y, z, 0.0, 0, 0, 0);
    }

    public static PrideVfxTestPacket impact(int entityId, double x, double y, double z, double impactRadius) {
        return new PrideVfxTestPacket(IMPACT, entityId, x, y, z, impactRadius, 0, 0, 0);
    }

    public static PrideVfxTestPacket wave(double x, double y, double z, double maxRadius,
                                          int durationTicks, int variant, int delayTicks) {
        return new PrideVfxTestPacket(WAVE, 0, x, y, z, maxRadius, durationTicks, variant, delayTicks);
    }

    public static void encode(PrideVfxTestPacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.action);
        buffer.writeVarInt(packet.entityId);
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeDouble(packet.radius);
        buffer.writeVarInt(packet.durationTicks);
        buffer.writeVarInt(packet.variant);
        buffer.writeVarInt(packet.delayTicks);
    }

    public static PrideVfxTestPacket decode(FriendlyByteBuf buffer) {
        return new PrideVfxTestPacket(buffer.readUnsignedByte(), buffer.readVarInt(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(PrideVfxTestPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> PrideVfxClientEffect.accept(packet)));
        context.setPacketHandled(true);
    }
}
