package com.anthonyahellman.gluttony.network;

import com.anthonyahellman.gluttony.greed.AvariceAppraisals;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public record AvariceValuesPacket(Map<ResourceLocation, Double> values,
                                  Map<ResourceLocation, AvariceAppraisals.AppraisalSource> sources,
                                  Map<ResourceLocation, ResourceLocation> recipes) {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void encode(AvariceValuesPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.values.size());
        packet.values.forEach((id, value) -> {
            buffer.writeResourceLocation(id);
            buffer.writeDouble(value);
            AvariceAppraisals.AppraisalSource source = packet.sources.getOrDefault(id,
                    AvariceAppraisals.AppraisalSource.CONFIGURED);
            buffer.writeEnum(source);
            ResourceLocation recipe = packet.recipes.get(id);
            buffer.writeBoolean(recipe != null);
            if (recipe != null) buffer.writeResourceLocation(recipe);
        });
    }

    public static AvariceValuesPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        Map<ResourceLocation, Double> values = new HashMap<>();
        Map<ResourceLocation, AvariceAppraisals.AppraisalSource> sources = new HashMap<>();
        Map<ResourceLocation, ResourceLocation> recipes = new HashMap<>();
        for (int i = 0; i < size; i++) {
            ResourceLocation id = buffer.readResourceLocation();
            values.put(id, Math.max(0.0, buffer.readDouble()));
            sources.put(id, buffer.readEnum(AvariceAppraisals.AppraisalSource.class));
            if (buffer.readBoolean()) recipes.put(id, buffer.readResourceLocation());
        }
        return new AvariceValuesPacket(values, sources, recipes);
    }

    public static void handle(AvariceValuesPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> {
                    AvariceAppraisals.replaceClientValues(packet.values, packet.sources, packet.recipes);
                    ResourceLocation dirt = new ResourceLocation("minecraft", "dirt");
                    LOGGER.info("Roots of Sin packet received: {} appraisals; dirt={}",
                            packet.values.size(), packet.values.getOrDefault(dirt, 0.0));
                }));
        context.setPacketHandled(true);
    }
}
