package com.anthonyahellman.gluttony;

import com.anthonyahellman.gluttony.registry.ModItems;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(GluttonyMod.MOD_ID)
public final class GluttonyMod {
    public static final String MOD_ID = "demonsbountygluttony";

    public GluttonyMod() {
        ModItems.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
