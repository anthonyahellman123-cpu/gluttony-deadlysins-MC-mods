package com.anthonyahellman.gluttony.client;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = GluttonyMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientModEvents {
    public static final KeyMapping SOUL_SIPHON = new KeyMapping(
            "key.demonsbountygluttony.sin_ability",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.demonsbountygluttony"
    );
    public static final KeyMapping SHADOW_STEP = new KeyMapping(
            "key.demonsbountygluttony.sin_movement",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.demonsbountygluttony"
    );

    private ClientModEvents() {}

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(SOUL_SIPHON);
        event.register(SHADOW_STEP);
    }
}
