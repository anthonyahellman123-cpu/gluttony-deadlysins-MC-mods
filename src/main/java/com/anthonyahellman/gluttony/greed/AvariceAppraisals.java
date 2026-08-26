package com.anthonyahellman.gluttony.greed;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = GluttonyMod.MOD_ID)
public final class AvariceAppraisals extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Map<ResourceLocation, Double> ANCHORS = createAnchors();
    private static volatile Map<ResourceLocation, Double> fixedValues = ANCHORS;
    private static volatile Map<ResourceLocation, Double> values = ANCHORS;

    public enum AssetTier {
        UNAPPRAISED(0, "Unappraised"),
        COMMON(1, "T1 Common"),
        VALUABLE(2, "T2 Valuable"),
        PRECIOUS(3, "T3 Precious"),
        EXCEPTIONAL(4, "T4 Exceptional"),
        PINNACLE(5, "T5 Pinnacle");

        private final int level;
        private final String displayName;

        AssetTier(int level, String displayName) {
            this.level = level;
            this.displayName = displayName;
        }

        public int level() { return level; }
        public String displayName() { return displayName; }
    }

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

    public static AssetTier tier(ItemStack stack) {
        return tier(value(stack));
    }

    public static AssetTier tier(double value) {
        if (value <= 0.0) return AssetTier.UNAPPRAISED;
        if (value < 10.0) return AssetTier.COMMON;
        if (value < 25.0) return AssetTier.VALUABLE;
        if (value < 75.0) return AssetTier.PRECIOUS;
        if (value < 500.0) return AssetTier.EXCEPTIONAL;
        return AssetTier.PINNACLE;
    }

    public static Map<ResourceLocation, Double> snapshot() {
        return values;
    }

    public static void replaceClientValues(Map<ResourceLocation, Double> syncedValues) {
        values = Map.copyOf(syncedValues);
    }

    public static synchronized void deriveReliableCraftingValues(RecipeManager recipes,
                                                                  RegistryAccess registryAccess) {
        Map<ResourceLocation, Double> fixed = fixedValues;
        Map<ResourceLocation, Double> derived = new HashMap<>(fixed);
        boolean changed;
        int passes = 0;
        do {
            changed = false;
            Map<ResourceLocation, Double> candidates = new HashMap<>();
            for (Recipe<?> recipe : recipes.getRecipes()) {
                if (recipe.getType() != RecipeType.CRAFTING || recipe.isSpecial()) continue;
                ItemStack result = recipe.getResultItem(registryAccess);
                if (result.isEmpty()) continue;
                ResourceLocation resultId = BuiltInRegistries.ITEM.getKey(result.getItem());
                if (fixed.containsKey(resultId)) continue;
                double ingredientTotal = 0.0;
                boolean reliable = true;
                for (Ingredient ingredient : recipe.getIngredients()) {
                    if (ingredient.isEmpty()) continue;
                    ItemStack[] choices = ingredient.getItems();
                    if (choices.length == 0) { reliable = false; break; }
                    Double commonValue = null;
                    for (ItemStack choice : choices) {
                        ResourceLocation choiceId = BuiltInRegistries.ITEM.getKey(choice.getItem());
                        double choiceValue = derived.getOrDefault(choiceId, 0.0);
                        if (choiceValue <= 0.0 || (commonValue != null
                                && Math.abs(commonValue - choiceValue) > 0.0001)) {
                            reliable = false;
                            break;
                        }
                        commonValue = choiceValue;
                    }
                    if (!reliable || commonValue == null) break;
                    ingredientTotal += commonValue;
                }
                if (reliable && ingredientTotal > 0.0) {
                    candidates.merge(resultId, ingredientTotal / Math.max(1, result.getCount()), Math::min);
                }
            }
            for (Map.Entry<ResourceLocation, Double> candidate : candidates.entrySet()) {
                double old = derived.getOrDefault(candidate.getKey(), Double.POSITIVE_INFINITY);
                if (candidate.getValue() + 0.0001 < old) {
                    derived.put(candidate.getKey(), candidate.getValue());
                    changed = true;
                }
            }
            passes++;
        } while (changed && passes < 16);
        values = Map.copyOf(derived);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager manager,
                         ProfilerFiller profiler) {
        Map<ResourceLocation, Double> loaded = new HashMap<>(ANCHORS);
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            JsonObject json = entry.getValue().getAsJsonObject();
            if (json.has("values") && json.get("values").isJsonObject()) {
                for (Map.Entry<String, JsonElement> valueEntry : json.getAsJsonObject("values").entrySet()) {
                    putIfValid(loaded, ResourceLocation.tryParse(valueEntry.getKey()),
                            valueEntry.getValue().getAsDouble());
                }
            } else if (json.has("item") && json.has("value")) {
                putIfValid(loaded, ResourceLocation.tryParse(json.get("item").getAsString()),
                        json.get("value").getAsDouble());
            }
        }
        fixedValues = Map.copyOf(loaded);
        values = fixedValues;
    }

    private static void putIfValid(Map<ResourceLocation, Double> target, ResourceLocation itemId, double value) {
        if (itemId != null && BuiltInRegistries.ITEM.containsKey(itemId) && value >= 0.0) {
            target.put(itemId, value);
        }
    }

    private static Map<ResourceLocation, Double> createAnchors() {
        Map<ResourceLocation, Double> anchors = new LinkedHashMap<>();
        anchor(anchors, "dirt", 0.05);
        anchor(anchors, "cobblestone", 0.10);
        anchor(anchors, "stone", 0.10);
        anchor(anchors, "stick", 0.20);
        anchor(anchors, "rotten_flesh", 0.50);
        anchor(anchors, "coal", 1);
        anchor(anchors, "bone", 1);
        anchor(anchors, "string", 1);
        anchor(anchors, "spider_eye", 1);
        anchor(anchors, "copper_ingot", 2);
        for (String log : new String[]{"oak_log", "spruce_log", "birch_log", "jungle_log",
                "acacia_log", "dark_oak_log", "mangrove_log", "cherry_log", "crimson_stem",
                "warped_stem"}) anchor(anchors, log, 2);
        anchor(anchors, "gunpowder", 2);
        anchor(anchors, "iron_ingot", 5);
        anchor(anchors, "ender_pearl", 5);
        anchor(anchors, "redstone", 10);
        anchor(anchors, "lapis_lazuli", 10);
        anchor(anchors, "blaze_rod", 10);
        anchor(anchors, "ghast_tear", 20);
        anchor(anchors, "gold_ingot", 25);
        anchor(anchors, "echo_shard", 25);
        anchor(anchors, "shulker_shell", 25);
        anchor(anchors, "emerald", 35);
        anchor(anchors, "diamond", 50);
        anchor(anchors, "wither_skeleton_skull", 50);
        anchor(anchors, "ancient_debris", 75);
        anchor(anchors, "totem_of_undying", 100);
        anchor(anchors, "netherite_scrap", 125);
        anchor(anchors, "heart_of_the_sea", 150);
        anchor(anchors, "nether_star", 200);
        anchor(anchors, "elytra", 300);
        anchor(anchors, "beacon", 500);
        anchor(anchors, "enchanted_golden_apple", 500);
        anchor(anchors, "netherite_ingot", 750);
        anchor(anchors, "dragon_egg", 1_000);
        return Map.copyOf(anchors);
    }

    private static void anchor(Map<ResourceLocation, Double> target, String path, double value) {
        target.put(new ResourceLocation("minecraft", path), value);
    }

    @SubscribeEvent
    public static void registerReloadListener(AddReloadListenerEvent event) {
        event.addListener(new AvariceAppraisals());
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        deriveReliableCraftingValues(event.getPlayerList().getServer().getRecipeManager(),
                event.getPlayerList().getServer().registryAccess());
        if (event.getPlayer() != null) {
            com.anthonyahellman.gluttony.gameplay.AbilityHudSync.sendAppraisals(event.getPlayer());
        } else {
            for (net.minecraft.server.level.ServerPlayer player : event.getPlayerList().getPlayers()) {
                com.anthonyahellman.gluttony.gameplay.AbilityHudSync.sendAppraisals(player);
            }
        }
    }
}
