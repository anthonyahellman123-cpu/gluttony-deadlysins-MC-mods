package com.anthonyahellman.gluttony.network;

import com.anthonyahellman.gluttony.data.GluttonyData;
import com.anthonyahellman.gluttony.data.SinData;
import com.anthonyahellman.gluttony.gameplay.Beelzebub;
import com.anthonyahellman.gluttony.gameplay.Devour;
import com.anthonyahellman.gluttony.gameplay.PrideAbility;
import com.anthonyahellman.gluttony.gameplay.SoulSiphon;
import com.anthonyahellman.gluttony.menu.PouchInventory;
import com.anthonyahellman.gluttony.menu.PouchOfMammonMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

public record SinAbilityPacket(int action, int chargeTicks) {
    public static final int PRESS = 0;
    public static final int RELEASE = 1;

    public static void encode(SinAbilityPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.action);
        buffer.writeVarInt(packet.chargeTicks);
    }
    public static SinAbilityPacket decode(FriendlyByteBuf buffer) {
        return new SinAbilityPacket(buffer.readVarInt(), buffer.readVarInt());
    }
    public static void handle(SinAbilityPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.isAlive() || player.isSpectator()) return;
            switch (SinData.selected(player)) {
                case GLUTTONY -> gluttony(player, packet);
                case PRIDE -> { if (packet.action == PRESS) PrideAbility.tryCast(player); }
                case GREED -> { if (packet.action == PRESS) openPouch(player); }
                case NONE -> { }
            }
        });
        context.setPacketHandled(true);
    }

    private static void gluttony(ServerPlayer player, SinAbilityPacket packet) {
        GluttonyData.Ability selected = GluttonyData.of(player).selectedAbility();
        if (packet.action == PRESS) {
            if (selected == GluttonyData.Ability.SOUL_SIPHON) SoulSiphon.arm(player);
            else if (selected == GluttonyData.Ability.BEELZEBUB) Beelzebub.toggle(player);
        } else if (packet.action == RELEASE && selected == GluttonyData.Ability.DEVOUR) {
            Devour.arm(player, packet.chargeTicks);
        }
    }

    private static void openPouch(ServerPlayer player) {
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (id, inventory, ignored) -> new PouchOfMammonMenu(id, inventory, new PouchInventory(player)),
                Component.literal("Pouch of Mammon")));
        com.anthonyahellman.gluttony.gameplay.AbilityHudSync.send(player);
    }
}
