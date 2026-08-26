package com.anthonyahellman.gluttony.greed;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = GluttonyMod.MOD_ID)
public final class AvariceAppraisals extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private static volatile Map<ResourceLocation, Double> values = Map.of();

    public AvariceAppraisals() {
        super(GSON, "avarice_appraisals");
    }

    public static double value(ItemStack stack) {
        if (stack.isEmpty()) return 0.0;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return Math.max(0.0, values.getOrDefault(id, 0.0));
    }

    public static double stackValue(ItemStack stack) {
        return value(stack) * stack.getCount();
    }

    public static Map<ResourceLocation, Double> snapshot() {
        return values;
    }

    public static void replaceClientValues(Map<ResourceLocation, Double> syncedValues) {
        values = Map.copyOf(syncedValues);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager manager,
                         ProfilerFiller profiler) {
        Map<ResourceLocation, Double> loaded = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            JsonObject json = entry.getValue().getAsJsonObject();
            if (!json.has("item") || !json.has("value")) continue;
            ResourceLocation itemId = ResourceLocation.tryParse(json.get("item").getAsString());
            double value = json.get("value").getAsDouble();
            if (itemId != null && BuiltInRegistries.ITEM.containsKey(itemId) && value >= 0.0) {
                loaded.put(itemId, value);
            }
        }
        values = Map.copyOf(loaded);
    }

    @SubscribeEvent
    public static void registerReloadListener(AddReloadListenerEvent event) {
        event.addListener(new AvariceAppraisals());
    }
}
