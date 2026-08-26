package com.anthonyahellman.gluttony.registry;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.anthonyahellman.gluttony.menu.CofferOfAvariceMenu;
import com.anthonyahellman.gluttony.menu.PouchOfMammonMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, GluttonyMod.MOD_ID);

    public static final RegistryObject<MenuType<CofferOfAvariceMenu>> COFFER_OF_AVARICE =
            MENUS.register("coffer_of_avarice", () -> IForgeMenuType.create(CofferOfAvariceMenu::new));

    public static final RegistryObject<MenuType<PouchOfMammonMenu>> POUCH_OF_MAMMON =
            MENUS.register("pouch_of_mammon", () -> IForgeMenuType.create(PouchOfMammonMenu::new));

    private ModMenus() {}
}
