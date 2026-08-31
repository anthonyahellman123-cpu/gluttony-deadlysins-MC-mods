package com.anthonyahellman.gluttony.network;

import com.anthonyahellman.gluttony.client.SoulSiphonVfxClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** One bounded cosmetic event sent only after the server accepts Soul Siphon state/gameplay. */
public record SoulSiphonVfxPacket(int action, int casterId, int targetId,
                                  double sourceX, double sourceY, double sourceZ,
                                  double destinationX, double destinationY, double destinationZ,
                                  int remainingCharges, int remainingTicks) {
    public static final int PRIMED = 0;
    public static final int EXTRACTION = 1;

    public static SoulSiphonVfxPacket primed(int casterId, int remainingTicks) {
        return new SoulSiphonVfxPacket(PRIMED, casterId, 0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3, remainingTicks);
    }

    public static SoulSiphonVfxPacket extraction(int casterId, int targetId, Vec3 source,
                                                   Vec3 destination, int remainingCharges,
                                                   int remainingTicks) {
        return new SoulSiphonVfxPacket(EXTRACTION, casterId, targetId,
                source.x, source.y, source.z, destination.x, destination.y, destination.z,
                remainingCharges, remainingTicks);
    }

    public static void encode(SoulSiphonVfxPacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.action);
        buffer.writeVarInt(packet.casterId);
        buffer.writeVarInt(packet.targetId);
        buffer.writeDouble(packet.sourceX);
        buffer.writeDouble(packet.sourceY);
        buffer.writeDouble(packet.sourceZ);
        buffer.writeDouble(packet.destinationX);
        buffer.writeDouble(packet.destinationY);
        buffer.writeDouble(packet.destinationZ);
        buffer.writeVarInt(packet.remainingCharges);
        buffer.writeVarInt(packet.remainingTicks);
    }

    public static SoulSiphonVfxPacket decode(FriendlyByteBuf buffer) {
        return new SoulSiphonVfxPacket(buffer.readUnsignedByte(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readVarInt(),
                buffer.readVarInt());
    }

    public static void handle(SoulSiphonVfxPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> SoulSiphonVfxClient.accept(packet)));
        context.setPacketHandled(true);
    }
}
