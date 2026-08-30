package com.anthonyahellman.gluttony.registry;

import com.anthonyahellman.gluttony.GluttonyMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, GluttonyMod.MOD_ID);

    public static final RegistryObject<SimpleParticleType> PRIDE_RADIANT_MOTE = register("pride_radiant_mote");
    public static final RegistryObject<SimpleParticleType> PRIDE_RADIANT_STREAK = register("pride_radiant_streak");
    public static final RegistryObject<SimpleParticleType> PRIDE_BLACKENED_GOLD_SHARD = register("pride_blackened_gold_shard");
    public static final RegistryObject<SimpleParticleType> PRIDE_GOLDEN_EMBER = register("pride_golden_ember");

    public static final RegistryObject<SimpleParticleType> GLUTTONY_SOUL_CORE = register("gluttony_soul_core");
    public static final RegistryObject<SimpleParticleType> GLUTTONY_SOUL_WISP = register("gluttony_soul_wisp");
    public static final RegistryObject<SimpleParticleType> GLUTTONY_WISP = register("gluttony_wisp");
    public static final RegistryObject<SimpleParticleType> GLUTTONY_HUNGER_FLICKER = register("gluttony_hunger_flicker");

    private ModParticles() {}

    private static RegistryObject<SimpleParticleType> register(String name) {
        return PARTICLES.register(name, () -> new SimpleParticleType(false));
    }
}
