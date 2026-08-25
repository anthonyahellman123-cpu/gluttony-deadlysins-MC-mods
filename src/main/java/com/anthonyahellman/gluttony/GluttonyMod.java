package com.anthonyahellman.gluttony;

import com.anthonyahellman.gluttony.registry.ModItems;
import com.anthonyahellman.gluttony.network.ModNetwork;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(GluttonyMod.MOD_ID)
public final class GluttonyMod {
    public static final String MOD_ID = "demonsbountygluttony";

    public GluttonyMod() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(modBus);
        ModNetwork.register();
        modBus.addListener(this::addCreativeTabContents);
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS
                || event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.CURSED_APPLE);
            event.accept(ModItems.PRIDE_SOL);
        }
    }
}
