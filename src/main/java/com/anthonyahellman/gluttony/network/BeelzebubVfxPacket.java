package com.anthonyahellman.gluttony.network;

import com.anthonyahellman.gluttony.client.BeelzebubVfxClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** One compact cosmetic pulse containing only targets affected by authoritative gameplay. */
public record BeelzebubVfxPacket(int casterId, float radius, int[] targetIds) {
    private static final int MAX_TARGETS = 32;

    public BeelzebubVfxPacket {
        targetIds = targetIds.length <= MAX_TARGETS ? targetIds.clone()
                : java.util.Arrays.copyOf(targetIds, MAX_TARGETS);
    }

    public static void encode(BeelzebubVfxPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.casterId);
        buffer.writeFloat(packet.radius);
        buffer.writeVarInt(packet.targetIds.length);
        for (int targetId : packet.targetIds) buffer.writeVarInt(targetId);
    }

    public static BeelzebubVfxPacket decode(FriendlyByteBuf buffer) {
        int casterId = buffer.readVarInt();
        float radius = buffer.readFloat();
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_TARGETS) {
            throw new IllegalArgumentException("Invalid Beelzebub VFX target count: " + count);
        }
        int[] targetIds = new int[count];
        for (int i = 0; i < count; i++) targetIds[i] = buffer.readVarInt();
        return new BeelzebubVfxPacket(casterId, radius, targetIds);
    }

    public static void handle(BeelzebubVfxPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> BeelzebubVfxClient.accept(packet)));
        context.setPacketHandled(true);
    }
}
