package com.anthonyahellman.gluttony.registry;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.anthonyahellman.gluttony.item.CursedAppleItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GluttonyMod.MOD_ID);
    public static final RegistryObject<Item> CURSED_APPLE = ITEMS.register("cursed_apple", CursedAppleItem::new);
    private ModItems() {}
}
