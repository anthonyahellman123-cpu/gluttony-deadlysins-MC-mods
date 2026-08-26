package com.anthonyahellman.gluttony.greed;

import com.anthonyahellman.gluttony.GluttonyMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.ItemTags;
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
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = GluttonyMod.MOD_ID)
public final class AvariceAppraisals extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Map<ResourceLocation, Double> ANCHORS = createAnchors();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile Map<ResourceLocation, Double> configuredValues = Map.of();
    private static volatile Map<ResourceLocation, Double> serverValues = ANCHORS;
    private static volatile Map<ResourceLocation, AppraisalSource> serverSources = anchorSources(ANCHORS);
    private static volatile Map<ResourceLocation, ResourceLocation> serverRecipes = Map.of();
    private static volatile Map<ResourceLocation, Double> clientValues = ANCHORS;
    private static volatile Map<ResourceLocation, AppraisalSource> clientSources = anchorSources(ANCHORS);
    private static volatile Map<ResourceLocation, ResourceLocation> clientRecipes = Map.of();
    private static volatile boolean derivationDirty = true;

    public enum AppraisalSource {
        ANCHOR("ANCHOR"),
        RECIPE_DERIVED("RECIPE DERIVED"),
        CONFIGURED("CONFIGURED");

        private final String displayName;

        AppraisalSource(String displayName) { this.displayName = displayName; }
        public String displayName() { return displayName; }
    }

    public record Inspection(ResourceLocation itemId, boolean appraised, double value,
                             AppraisalSource source, ResourceLocation recipeId, AssetTier tier,
                             String unresolvedReason) {}

    private record Candidate(double value, ResourceLocation recipeId) {}

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

    public static double serverValue(ItemStack stack) {
        return lookup(stack, serverValues);
    }

    public static double clientValue(ItemStack stack) {
        return lookup(stack, clientValues);
    }

    private static double lookup(ItemStack stack, Map<ResourceLocation, Double> lookup) {
        if (stack.isEmpty()) return 0.0;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return Math.max(0.0, lookup.getOrDefault(id, 0.0));
    }

    public static double serverStackValue(ItemStack stack) {
        return serverValue(stack) * stack.getCount();
    }

    public static double clientStackValue(ItemStack stack) {
        return clientValue(stack) * stack.getCount();
    }

    public static AssetTier serverTier(ItemStack stack) {
        return tier(serverValue(stack));
    }

    public static AssetTier clientTier(ItemStack stack) {
        return tier(clientValue(stack));
    }

    public static AssetTier tier(double value) {
        if (value <= 0.0) return AssetTier.UNAPPRAISED;
        if (value < 10.0) return AssetTier.COMMON;
        if (value < 25.0) return AssetTier.VALUABLE;
        if (value < 75.0) return AssetTier.PRECIOUS;
        if (value < 500.0) return AssetTier.EXCEPTIONAL;
        return AssetTier.PINNACLE;
    }

    public static Inspection inspectServer(ItemStack stack) {
        return inspect(stack, serverValues, serverSources, serverRecipes);
    }

    public static Inspection inspectClient(ItemStack stack) {
        return inspect(stack, clientValues, clientSources, clientRecipes);
    }

    private static Inspection inspect(ItemStack stack, Map<ResourceLocation, Double> lookup,
                                      Map<ResourceLocation, AppraisalSource> sources,
                                      Map<ResourceLocation, ResourceLocation> recipes) {
        ResourceLocation id = stack.isEmpty() ? new ResourceLocation("minecraft", "air")
                : BuiltInRegistries.ITEM.getKey(stack.getItem());
        double value = stack.isEmpty() ? 0.0 : Math.max(0.0, lookup.getOrDefault(id, 0.0));
        AppraisalSource source = sources.get(id);
        boolean appraised = value > 0.0 && source != null;
        return new Inspection(id, appraised, value, source, recipes.get(id), tier(value),
                appraised ? "" : "NO_SUPPORTED_VALUE_PATH");
    }

    public static Map<ResourceLocation, Double> serverSnapshot() { return serverValues; }
    public static Map<ResourceLocation, AppraisalSource> serverSourceSnapshot() { return serverSources; }
    public static Map<ResourceLocation, ResourceLocation> serverRecipeSnapshot() { return serverRecipes; }

    public static void replaceClientValues(Map<ResourceLocation, Double> syncedValues,
                                           Map<ResourceLocation, AppraisalSource> syncedSources,
                                           Map<ResourceLocation, ResourceLocation> syncedRecipes) {
        clientValues = Map.copyOf(syncedValues);
        clientSources = Map.copyOf(syncedSources);
        clientRecipes = Map.copyOf(syncedRecipes);
        ResourceLocation dirt = new ResourceLocation("minecraft", "dirt");
        LOGGER.info("Roots of Sin client appraisal sync: {} values; dirt={} source={}",
                clientValues.size(), clientValues.getOrDefault(dirt, 0.0), clientSources.get(dirt));
    }

    public static synchronized void ensureDerived(MinecraftServer server) {
        if (!derivationDirty) return;
        deriveReliableCraftingValues(server.getRecipeManager(), server.registryAccess());
    }

    public static synchronized void deriveReliableCraftingValues(RecipeManager recipes,
                                                                  RegistryAccess registryAccess) {
        Map<ResourceLocation, Double> anchors = effectiveAnchors();
        Map<ResourceLocation, Double> working = new HashMap<>(configuredValues);
        working.putAll(anchors);
        Map<ResourceLocation, Double> derived = new HashMap<>();
        Map<ResourceLocation, ResourceLocation> derivedRecipes = new HashMap<>();
        boolean changed;
        int passes = 0;
        do {
            changed = false;
            Map<ResourceLocation, Candidate> candidates = new HashMap<>();
            for (Recipe<?> recipe : recipes.getRecipes()) {
                if (recipe.getType() != RecipeType.CRAFTING || recipe.isSpecial()) continue;
                ItemStack result = recipe.getResultItem(registryAccess);
                if (result.isEmpty()) continue;
                ResourceLocation resultId = BuiltInRegistries.ITEM.getKey(result.getItem());
                if (anchors.containsKey(resultId)) continue;
                double ingredientTotal = 0.0;
                boolean reliable = true;
                for (Ingredient ingredient : recipe.getIngredients()) {
                    if (ingredient.isEmpty()) continue;
                    ItemStack[] choices = ingredient.getItems();
                    if (choices.length == 0) { reliable = false; break; }
                    double cheapestChoice = Double.POSITIVE_INFINITY;
                    for (ItemStack choice : choices) {
                        ResourceLocation choiceId = BuiltInRegistries.ITEM.getKey(choice.getItem());
                        double choiceValue = working.getOrDefault(choiceId, 0.0);
                        if (choiceValue <= 0.0) {
                            reliable = false;
                            break;
                        }
                        cheapestChoice = Math.min(cheapestChoice, choiceValue);
                    }
                    if (!reliable || !Double.isFinite(cheapestChoice)) break;
                    ingredientTotal += cheapestChoice;
                }
                if (reliable && ingredientTotal > 0.0) {
                    Candidate candidate = new Candidate(ingredientTotal / Math.max(1, result.getCount()),
                            recipe.getId());
                    candidates.merge(resultId, candidate,
                            (left, right) -> left.value() <= right.value() ? left : right);
                }
            }
            for (Map.Entry<ResourceLocation, Candidate> candidate : candidates.entrySet()) {
                double old = derived.getOrDefault(candidate.getKey(), Double.POSITIVE_INFINITY);
                if (candidate.getValue().value() + 0.0001 < old) {
                    working.put(candidate.getKey(), candidate.getValue().value());
                    derived.put(candidate.getKey(), candidate.getValue().value());
                    derivedRecipes.put(candidate.getKey(), candidate.getValue().recipeId());
                    changed = true;
                }
            }
            passes++;
        } while (changed && passes < 64);
        Map<ResourceLocation, Double> resolved = new HashMap<>(configuredValues);
        resolved.putAll(derived);
        resolved.putAll(anchors);
        Map<ResourceLocation, AppraisalSource> sources = new HashMap<>();
        configuredValues.keySet().forEach(id -> sources.put(id, AppraisalSource.CONFIGURED));
        derived.keySet().forEach(id -> sources.put(id, AppraisalSource.RECIPE_DERIVED));
        anchors.keySet().forEach(id -> sources.put(id, AppraisalSource.ANCHOR));
        serverValues = Map.copyOf(resolved);
        serverSources = Map.copyOf(sources);
        serverRecipes = Map.copyOf(derivedRecipes);
        derivationDirty = false;
        ResourceLocation dirt = new ResourceLocation("minecraft", "dirt");
        LOGGER.info("Roots of Sin appraisal ready: {} anchors, {} configured, {} recipe-derived, {} total values",
                anchors.size(), configuredValues.size(), derived.size(), serverValues.size());
        LOGGER.info("Roots of Sin server appraisal audit: dirt={} source={}",
                serverValues.getOrDefault(dirt, 0.0), serverSources.get(dirt));
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager manager,
                         ProfilerFiller profiler) {
        Map<ResourceLocation, Double> loaded = new HashMap<>();
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
        configuredValues = Map.copyOf(loaded);
        Map<ResourceLocation, Double> initial = new HashMap<>(configuredValues);
        initial.putAll(effectiveAnchors());
        serverValues = Map.copyOf(initial);
        Map<ResourceLocation, AppraisalSource> initialSources = new HashMap<>();
        configuredValues.keySet().forEach(id -> initialSources.put(id, AppraisalSource.CONFIGURED));
        effectiveAnchors().keySet().forEach(id -> initialSources.put(id, AppraisalSource.ANCHOR));
        serverSources = Map.copyOf(initialSources);
        serverRecipes = Map.of();
        derivationDirty = true;
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

    private static Map<ResourceLocation, Double> effectiveAnchors() {
        Map<ResourceLocation, Double> anchors = new HashMap<>(ANCHORS);
        BuiltInRegistries.ITEM.getTag(ItemTags.LOGS).ifPresent(logs -> logs.forEach(holder ->
                anchors.put(BuiltInRegistries.ITEM.getKey(holder.value()), 2.0)));
        return anchors;
    }

    private static Map<ResourceLocation, AppraisalSource> anchorSources(Map<ResourceLocation, Double> anchors) {
        Map<ResourceLocation, AppraisalSource> sources = new HashMap<>();
        anchors.keySet().forEach(id -> sources.put(id, AppraisalSource.ANCHOR));
        return Map.copyOf(sources);
    }

    @SubscribeEvent
    public static void registerReloadListener(AddReloadListenerEvent event) {
        event.addListener(new AvariceAppraisals());
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        ensureDerived(event.getPlayerList().getServer());
        if (event.getPlayer() != null) {
            com.anthonyahellman.gluttony.gameplay.AbilityHudSync.sendAppraisals(event.getPlayer());
        } else {
            for (net.minecraft.server.level.ServerPlayer player : event.getPlayerList().getPlayers()) {
                com.anthonyahellman.gluttony.gameplay.AbilityHudSync.sendAppraisals(player);
            }
        }
    }
}
