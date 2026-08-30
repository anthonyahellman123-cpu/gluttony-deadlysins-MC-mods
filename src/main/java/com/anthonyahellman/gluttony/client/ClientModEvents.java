package com.anthonyahellman.gluttony.client;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import com.anthonyahellman.gluttony.registry.ModParticles;
import com.anthonyahellman.gluttony.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = GluttonyMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientModEvents {
    public static final KeyMapping SIN_ABILITY = new KeyMapping(
            "key.demonsbountygluttony.sin_ability",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.demonsbountygluttony"
    );
    public static final KeyMapping SIN_STATS = new KeyMapping(
            "key.demonsbountygluttony.sin_stats",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "key.categories.demonsbountygluttony"
    );

    private ClientModEvents() {}

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(SIN_ABILITY);
        event.register(SIN_STATS);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.PRIDE_RADIANT_MOTE.get(),
                sprites -> new PrideSpriteParticle.Provider(sprites, PrideSpriteParticle.Kind.MOTE));
        event.registerSpriteSet(ModParticles.PRIDE_RADIANT_STREAK.get(),
                sprites -> new PrideSpriteParticle.Provider(sprites, PrideSpriteParticle.Kind.STREAK));
        event.registerSpriteSet(ModParticles.PRIDE_BLACKENED_GOLD_SHARD.get(),
                sprites -> new PrideSpriteParticle.Provider(sprites, PrideSpriteParticle.Kind.SHARD));
        event.registerSpriteSet(ModParticles.PRIDE_GOLDEN_EMBER.get(),
                sprites -> new PrideSpriteParticle.Provider(sprites, PrideSpriteParticle.Kind.EMBER));
        event.registerSpriteSet(ModParticles.GLUTTONY_SOUL_CORE.get(),
                sprites -> new GluttonySpriteParticle.Provider(sprites, GluttonySpriteParticle.Kind.SOUL_CORE));
        event.registerSpriteSet(ModParticles.GLUTTONY_SOUL_WISP.get(),
                sprites -> new GluttonySpriteParticle.Provider(sprites, GluttonySpriteParticle.Kind.SOUL_WISP));
        event.registerSpriteSet(ModParticles.GLUTTONY_WISP.get(),
                sprites -> new GluttonySpriteParticle.Provider(sprites, GluttonySpriteParticle.Kind.GLUTTONY_WISP));
        event.registerSpriteSet(ModParticles.GLUTTONY_HUNGER_FLICKER.get(),
                sprites -> new GluttonySpriteParticle.Provider(sprites, GluttonySpriteParticle.Kind.HUNGER_FLICKER));
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ModMenus.COFFER_OF_AVARICE.get(),
                CofferOfAvariceScreen::new));
        event.enqueueWork(() -> MenuScreens.register(ModMenus.POUCH_OF_MAMMON.get(),
                PouchOfMammonScreen::new));
        event.enqueueWork(() -> MenuScreens.register(ModMenus.GREEDS_VAULT.get(),
                GreedsVaultScreen::new));
    }
}
