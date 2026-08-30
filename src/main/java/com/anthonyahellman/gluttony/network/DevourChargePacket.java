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
                                 double committedHealth, double availableHealth,
                                 double maximumHealth) {
    public static void encode(DevourChargePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.casterId);
        buffer.writeInt(packet.targetId);
        buffer.writeBoolean(packet.active);
        buffer.writeDouble(packet.committedHealth);
        buffer.writeDouble(packet.availableHealth);
        buffer.writeDouble(packet.maximumHealth);
    }

    public static DevourChargePacket decode(FriendlyByteBuf buffer) {
        return new DevourChargePacket(buffer.readVarInt(), buffer.readInt(), buffer.readBoolean(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
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
