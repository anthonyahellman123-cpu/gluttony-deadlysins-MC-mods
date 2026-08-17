package com.anthonyahellman.gluttony.network;

import com.anthonyahellman.gluttony.GluttonyMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(GluttonyMod.MOD_ID, "main"),
            () -> VERSION,
            VERSION::equals,
            VERSION::equals
    );

    private ModNetwork() {}

    public static void register() {
        CHANNEL.registerMessage(0, SoulSiphonPacket.class,
                SoulSiphonPacket::encode, SoulSiphonPacket::decode, SoulSiphonPacket::handle);
    }
}
