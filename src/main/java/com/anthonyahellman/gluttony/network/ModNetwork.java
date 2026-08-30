package com.anthonyahellman.gluttony.network;

import com.anthonyahellman.gluttony.GluttonyMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String VERSION = "12";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(GluttonyMod.MOD_ID, "main"),
            () -> VERSION,
            VERSION::equals,
            VERSION::equals
    );

    private ModNetwork() {}

    public static void register() {
        CHANNEL.registerMessage(0, SinAbilityPacket.class,
                SinAbilityPacket::encode, SinAbilityPacket::decode, SinAbilityPacket::handle);
        CHANNEL.registerMessage(1, AbilityStatePacket.class,
                AbilityStatePacket::encode, AbilityStatePacket::decode, AbilityStatePacket::handle);
        CHANNEL.registerMessage(2, AvariceValuesPacket.class,
                AvariceValuesPacket::encode, AvariceValuesPacket::decode, AvariceValuesPacket::handle);
        CHANNEL.registerMessage(3, GreedStatePacket.class,
                GreedStatePacket::encode, GreedStatePacket::decode, GreedStatePacket::handle);
        CHANNEL.registerMessage(4, PrideStatePacket.class,
                PrideStatePacket::encode, PrideStatePacket::decode, PrideStatePacket::handle);
        CHANNEL.registerMessage(5, GluttonyAbilitySelectionPacket.class,
                GluttonyAbilitySelectionPacket::encode, GluttonyAbilitySelectionPacket::decode,
                GluttonyAbilitySelectionPacket::handle);
        CHANNEL.registerMessage(6, GluttonyTargetModePacket.class,
                GluttonyTargetModePacket::encode, GluttonyTargetModePacket::decode,
                GluttonyTargetModePacket::handle);
        CHANNEL.registerMessage(7, PrideVfxTestPacket.class,
                PrideVfxTestPacket::encode, PrideVfxTestPacket::decode,
                PrideVfxTestPacket::handle);
        CHANNEL.registerMessage(8, SoulSiphonVfxPacket.class,
                SoulSiphonVfxPacket::encode, SoulSiphonVfxPacket::decode,
                SoulSiphonVfxPacket::handle);
        CHANNEL.registerMessage(9, DevourVfxPacket.class,
                DevourVfxPacket::encode, DevourVfxPacket::decode,
                DevourVfxPacket::handle);
    }
}
