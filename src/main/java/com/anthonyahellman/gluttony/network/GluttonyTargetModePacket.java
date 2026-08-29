package com.anthonyahellman.gluttony.network;

import com.anthonyahellman.gluttony.data.GluttonyData;
import com.anthonyahellman.gluttony.data.SinData;
import com.anthonyahellman.gluttony.gameplay.AbilityHudSync;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record GluttonyTargetModePacket(int ability, int mode) {
    public static void encode(GluttonyTargetModePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.ability);
        buffer.writeVarInt(packet.mode);
    }

    public static GluttonyTargetModePacket decode(FriendlyByteBuf buffer) {
        return new GluttonyTargetModePacket(buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(GluttonyTargetModePacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || SinData.selected(player) != SinData.NaturalSin.GLUTTONY) return;
            GluttonyData.Ability[] abilities = GluttonyData.Ability.values();
            GluttonyData.TargetMode[] modes = GluttonyData.TargetMode.values();
            if (packet.ability < 0 || packet.ability >= abilities.length
                    || packet.mode < 0 || packet.mode >= modes.length) return;
            GluttonyData data = GluttonyData.of(player);
            GluttonyData.Ability ability = abilities[packet.ability];
            if (data.level() < ability.unlockLevel()) return;
            data.setTargetMode(ability, modes[packet.mode]);
            AbilityHudSync.send(player);
        });
        context.setPacketHandled(true);
    }
}
