package com.anthonyahellman.gluttony.network;

import com.anthonyahellman.gluttony.greed.AvariceAppraisals;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public record AvariceValuesPacket(Map<ResourceLocation, Double> values) {
    public static void encode(AvariceValuesPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.values.size());
        packet.values.forEach((id, value) -> {
            buffer.writeResourceLocation(id);
            buffer.writeDouble(value);
        });
    }

    public static AvariceValuesPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        Map<ResourceLocation, Double> values = new HashMap<>();
        for (int i = 0; i < size; i++) {
            values.put(buffer.readResourceLocation(), Math.max(0.0, buffer.readDouble()));
        }
        return new AvariceValuesPacket(values);
    }

    public static void handle(AvariceValuesPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> AvariceAppraisals.replaceClientValues(packet.values)));
        context.setPacketHandled(true);
    }
}
