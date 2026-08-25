package com.anthonyahellman.gluttony.network;

import com.anthonyahellman.gluttony.client.SinStatusScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record SinStatusPacket(int sin, String title, String stage, String description,
                              List<String> stats, List<String> progression, double progress) {
    public static void encode(SinStatusPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.sin);
        buffer.writeUtf(packet.title);
        buffer.writeUtf(packet.stage);
        buffer.writeUtf(packet.description);
        writeStrings(buffer, packet.stats);
        writeStrings(buffer, packet.progression);
        buffer.writeDouble(packet.progress);
    }

    public static SinStatusPacket decode(FriendlyByteBuf buffer) {
        return new SinStatusPacket(buffer.readVarInt(), buffer.readUtf(), buffer.readUtf(), buffer.readUtf(),
                readStrings(buffer), readStrings(buffer), buffer.readDouble());
    }

    public static void handle(SinStatusPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> SinStatusScreen.open(packet)));
        context.setPacketHandled(true);
    }

    private static void writeStrings(FriendlyByteBuf buffer, List<String> values) {
        buffer.writeVarInt(values.size());
        for (String value : values) buffer.writeUtf(value);
    }

    private static List<String> readStrings(FriendlyByteBuf buffer) {
        int size = Math.min(32, Math.max(0, buffer.readVarInt()));
        List<String> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) values.add(buffer.readUtf());
        return List.copyOf(values);
    }
}
