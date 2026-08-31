package com.anthonyahellman.gluttony.network;

import com.anthonyahellman.gluttony.data.GluttonyData;
import com.anthonyahellman.gluttony.data.SinData;
import com.anthonyahellman.gluttony.gameplay.AbilityHudSync;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record GluttonyAbilitySelectionPacket(int ability) {
    public static void encode(GluttonyAbilitySelectionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.ability);
    }
    public static GluttonyAbilitySelectionPacket decode(FriendlyByteBuf buffer) {
        return new GluttonyAbilitySelectionPacket(buffer.readVarInt());
    }
    public static void handle(GluttonyAbilitySelectionPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || SinData.selected(player) != SinData.NaturalSin.GLUTTONY) return;
            GluttonyData.Ability[] abilities = GluttonyData.Ability.values();
            if (packet.ability >= 0 && packet.ability < abilities.length
                    && GluttonyData.of(player).selectAbility(abilities[packet.ability])) {
                AbilityHudSync.send(player);
            }
        });
        context.setPacketHandled(true);
    }
}
