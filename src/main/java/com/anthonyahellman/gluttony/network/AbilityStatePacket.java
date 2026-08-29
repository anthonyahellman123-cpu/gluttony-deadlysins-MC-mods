package com.anthonyahellman.gluttony.network;

import com.anthonyahellman.gluttony.client.AbilityHudOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.function.Supplier;

public record AbilityStatePacket(int sin, boolean unlocked, boolean evolved, int cooldownTicks, int recastTicks,
                                 int level, double currentSouls, double lifetimeSouls,
                                 double extractedHealth, double extractedAttack,
                                 int nextLevelSouls, boolean auraActive, double avarice,
                                 int prideChargeTicks, int prideChargeStage, int gluttonyAbility,
                                 int siphonTargetMode, int devourTargetMode, int beelzebubTargetMode) {
    public static void encode(AbilityStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.sin);
        buffer.writeBoolean(packet.unlocked);
        buffer.writeBoolean(packet.evolved);
        buffer.writeVarInt(packet.cooldownTicks);
        buffer.writeVarInt(packet.recastTicks);
        buffer.writeVarInt(packet.level);
        buffer.writeDouble(packet.currentSouls);
        buffer.writeDouble(packet.lifetimeSouls);
        buffer.writeDouble(packet.extractedHealth);
        buffer.writeDouble(packet.extractedAttack);
        buffer.writeVarInt(packet.nextLevelSouls);
        buffer.writeBoolean(packet.auraActive);
        buffer.writeDouble(packet.avarice);
        buffer.writeVarInt(packet.prideChargeTicks);
        buffer.writeVarInt(packet.prideChargeStage);
        buffer.writeVarInt(packet.gluttonyAbility);
        buffer.writeVarInt(packet.siphonTargetMode);
        buffer.writeVarInt(packet.devourTargetMode);
        buffer.writeVarInt(packet.beelzebubTargetMode);
    }

    public static AbilityStatePacket decode(FriendlyByteBuf buffer) {
        return new AbilityStatePacket(buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readVarInt(),
                buffer.readBoolean(), buffer.readDouble(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(AbilityStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> AbilityHudOverlay.update(packet)));
        context.setPacketHandled(true);
    }
}
