package com.anthonyahellman.gluttony.network;

import com.anthonyahellman.gluttony.client.AbilityHudOverlay;
import com.anthonyahellman.gluttony.client.DevourVfxClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Authoritative Devour reservation state for the exact HUD and world-space maw. */
public record DevourChargePacket(int casterId, int targetId, boolean active,
                                 String targetName, double committedHealth,
                                 double currentHealth, double maximumHealth,
                                 double availableFlesh, double fleshPotential) {
    public static void encode(DevourChargePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.casterId);
        buffer.writeInt(packet.targetId);
        buffer.writeBoolean(packet.active);
        buffer.writeUtf(packet.targetName, 64);
        buffer.writeDouble(packet.committedHealth);
        buffer.writeDouble(packet.currentHealth);
        buffer.writeDouble(packet.maximumHealth);
        buffer.writeDouble(packet.availableFlesh);
        buffer.writeDouble(packet.fleshPotential);
    }

    public static DevourChargePacket decode(FriendlyByteBuf buffer) {
        return new DevourChargePacket(buffer.readVarInt(), buffer.readInt(), buffer.readBoolean(),
                buffer.readUtf(64), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble());
    }

    public static void handle(DevourChargePacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            AbilityHudOverlay.updateDevourCharge(packet);
            DevourVfxClient.updateCharge(packet);
        }));
        context.setPacketHandled(true);
    }
}
