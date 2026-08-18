package com.anthonyahellman.gluttony.registry;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.anthonyahellman.gluttony.item.CursedAppleItem;
import com.anthonyahellman.gluttony.item.PrideSolItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GluttonyMod.MOD_ID);
    public static final RegistryObject<Item> CURSED_APPLE = ITEMS.register("cursed_apple", CursedAppleItem::new);
    public static final RegistryObject<Item> PRIDE_SOL = ITEMS.register("pride_sol", PrideSolItem::new);
    public static final RegistryObject<Item> PRIDE_SPEAR = ITEMS.register("pride_spear",
            () -> new Item(new Item.Properties().stacksTo(1).fireResistant()));
    private ModItems() {}
}
