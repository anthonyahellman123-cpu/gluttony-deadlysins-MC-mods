package com.anthonyahellman.gluttony.network;

import com.anthonyahellman.gluttony.data.SinData;
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

public final class SinAbilityPacket {
    public static void encode(SinAbilityPacket packet, FriendlyByteBuf buffer) {}

    public static SinAbilityPacket decode(FriendlyByteBuf buffer) {
        return new SinAbilityPacket();
    }

    public static void handle(SinAbilityPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.isAlive() || player.isSpectator()) return;
            switch (SinData.selected(player)) {
                case GLUTTONY -> SoulSiphon.tryCast(player);
                case PRIDE -> PrideAbility.tryCast(player);
                case GREED -> openPouch(player);
                case NONE -> { }
            }
        });
        context.setPacketHandled(true);
    }

    private static void openPouch(ServerPlayer player) {
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (id, inventory, ignored) -> new PouchOfMammonMenu(id, inventory, new PouchInventory(player)),
                Component.literal("Pouch of Mammon")));
        com.anthonyahellman.gluttony.gameplay.AbilityHudSync.send(player);
    }
}
