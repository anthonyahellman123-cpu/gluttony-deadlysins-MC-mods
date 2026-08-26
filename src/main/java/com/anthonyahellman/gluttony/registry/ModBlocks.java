package com.anthonyahellman.gluttony.registry;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.anthonyahellman.gluttony.block.CofferOfAvariceBlock;
import com.anthonyahellman.gluttony.block.GreedsVaultBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, GluttonyMod.MOD_ID);

    public static final RegistryObject<Block> COFFER_OF_AVARICE = BLOCKS.register("coffer_of_avarice",
            () -> new CofferOfAvariceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(12.0F, 1200.0F)
                    .sound(SoundType.NETHERITE_BLOCK)));

    public static final RegistryObject<Item> COFFER_OF_AVARICE_ITEM = ModItems.ITEMS.register("coffer_of_avarice",
            () -> new BlockItem(COFFER_OF_AVARICE.get(), new Item.Properties().fireResistant()));

    public static final RegistryObject<Block> GREEDS_VAULT = BLOCKS.register("greeds_vault",
            () -> new GreedsVaultBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK).strength(8.0F, 1200.0F)
                    .sound(SoundType.NETHERITE_BLOCK)));

    public static final RegistryObject<Item> GREEDS_VAULT_ITEM = ModItems.ITEMS.register("greeds_vault",
            () -> new BlockItem(GREEDS_VAULT.get(), new Item.Properties().fireResistant()));

    private ModBlocks() {}
}
