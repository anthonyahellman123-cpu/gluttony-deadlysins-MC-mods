package com.anthonyahellman.gluttony.registry;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.anthonyahellman.gluttony.block.entity.CofferOfAvariceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, GluttonyMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<CofferOfAvariceBlockEntity>> COFFER_OF_AVARICE =
            BLOCK_ENTITIES.register("coffer_of_avarice",
                    () -> BlockEntityType.Builder.of(CofferOfAvariceBlockEntity::new,
                            ModBlocks.COFFER_OF_AVARICE.get()).build(null));

    private ModBlockEntities() {}
}
