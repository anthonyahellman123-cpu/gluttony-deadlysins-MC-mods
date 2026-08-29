package com.anthonyahellman.gluttony.greed;

import com.anthonyahellman.gluttony.GluttonyMod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Classifies intrinsic item identity. It never inspects the event that produced an item. */
public final class AppraisalResourceFamilies {
    public record FamilyDerivation(Map<ResourceLocation, Double> values,
                                   Map<ResourceLocation, ResourceLocation> paths) {}
    public enum ResourceFamily {
        MOB_DROP("MOB DROP", 200),
        BOSS_DROP("BOSS DROP", 600),
        INGOT("INGOT / METAL", 500),
        ORE("ORE / RAW RESOURCE", 400),
        CROP("CROP / NATURAL RESOURCE", 100),
        WOOD("WOOD", 350);

        private final String displayName;
        private final int precedence;

        ResourceFamily(String displayName, int precedence) {
            this.displayName = displayName;
            this.precedence = precedence;
        }

        public String displayName() { return displayName; }
        int precedence() { return precedence; }
    }

    private static final Map<String, ResourceFamily> STANDARD_TAG_ROOTS = Map.ofEntries(
            Map.entry("ingots", ResourceFamily.INGOT),
            Map.entry("ores", ResourceFamily.ORE),
            Map.entry("raw_materials", ResourceFamily.ORE),
            Map.entry("gems", ResourceFamily.ORE),
            Map.entry("crops", ResourceFamily.CROP),
            Map.entry("seeds", ResourceFamily.CROP),
            Map.entry("raw_meats", ResourceFamily.MOB_DROP),
            Map.entry("boss_drops", ResourceFamily.BOSS_DROP),
            Map.entry("leather", ResourceFamily.MOB_DROP),
            Map.entry("feathers", ResourceFamily.MOB_DROP),
            Map.entry("bones", ResourceFamily.MOB_DROP),
            Map.entry("string", ResourceFamily.MOB_DROP),
            Map.entry("mob_drops", ResourceFamily.MOB_DROP),
            Map.entry("eggs", ResourceFamily.MOB_DROP),
            Map.entry("ender_pearls", ResourceFamily.MOB_DROP),
            Map.entry("gunpowder", ResourceFamily.MOB_DROP),
            Map.entry("slimeballs", ResourceFamily.MOB_DROP),
            Map.entry("logs", ResourceFamily.WOOD),
            Map.entry("woods", ResourceFamily.WOOD)
    );

    private AppraisalResourceFamilies() {}

    public static Map<ResourceLocation, ResourceFamily> classifyAll() {
        Map<ResourceLocation, ResourceFamily> result = new HashMap<>();

        BuiltInRegistries.ITEM.getTags().forEach(pair -> {
            ResourceLocation tagId = pair.getFirst().location();
            ResourceFamily family = standardFamily(tagId);
            if (family != null) pair.getSecond().forEach(holder -> putBest(result, holder, family));
        });

        // Block tags provide reliable crop/flower coverage when no equivalent item tag exists.
        classifyBlockTag(result, new ResourceLocation("minecraft", "crops"), ResourceFamily.CROP);
        classifyBlockTag(result, new ResourceLocation("minecraft", "flowers"), ResourceFamily.CROP);
        classifyBlockTag(result, new ResourceLocation("minecraft", "small_flowers"), ResourceFamily.CROP);
        classifyBlockTag(result, new ResourceLocation("minecraft", "tall_flowers"), ResourceFamily.CROP);

        // Vanilla lacks one complete ordinary-mob-drop tag. These are classifications only, never prices.
        classifyVanilla(result, ResourceFamily.MOB_DROP, "beef", "leather", "feather", "chicken",
                "porkchop", "mutton", "rabbit", "rabbit_hide", "rabbit_foot", "ink_sac",
                "glow_ink_sac", "slime_ball", "honey_bottle", "phantom_membrane", "scute",
                "rotten_flesh", "bone", "string", "spider_eye", "gunpowder", "ender_pearl",
                "blaze_rod", "ghast_tear", "shulker_shell");
        classifyVanilla(result, ResourceFamily.BOSS_DROP, "nether_star", "dragon_egg");
        classifyVanilla(result, ResourceFamily.ORE, "coal", "raw_iron", "raw_copper",
                "raw_gold", "redstone", "lapis_lazuli", "diamond", "emerald", "ancient_debris");
        classifyVanilla(result, ResourceFamily.CROP, "wheat", "carrot", "potato", "beetroot",
                "sugar_cane", "cactus", "wheat_seeds", "beetroot_seeds", "melon_seeds",
                "pumpkin_seeds", "cocoa_beans", "sweet_berries", "glow_berries", "nether_wart");

        // Explicit extension tags are applied last. Boss precedence defeats all ordinary families.
        for (ResourceFamily family : ResourceFamily.values()) {
            ResourceLocation tagId = new ResourceLocation(GluttonyMod.MOD_ID,
                    "appraisal/" + family.name().toLowerCase());
            BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, tagId)).ifPresent(items ->
                    items.forEach(holder -> putBest(result, holder, family)));
        }
        return Map.copyOf(result);
    }

    /**
     * Propagates a known value only through a specific material tag whose already-valued members
     * unanimously agree. Generic umbrella tags are deliberately excluded.
     */
    public static FamilyDerivation deriveEquivalentTagValues(
            Map<ResourceLocation, Double> known,
            Map<ResourceLocation, ResourceFamily> classifications) {
        Map<ResourceLocation, Double> candidates = new HashMap<>();
        Map<ResourceLocation, ResourceLocation> paths = new HashMap<>();
        Set<ResourceLocation> conflicts = new HashSet<>();

        BuiltInRegistries.ITEM.getTags().forEach(pair -> {
            ResourceLocation tagId = pair.getFirst().location();
            ResourceFamily family = standardFamily(tagId);
            if (family == null || (!tagId.getPath().contains("/")
                    && !(tagId.getNamespace().equals("minecraft") && tagId.getPath().equals("logs")))) return;

            Double unanimous = null;
            boolean disagrees = false;
            for (Holder<Item> holder : pair.getSecond()) {
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(holder.value());
                double value = known.getOrDefault(itemId, 0.0);
                if (value <= 0.0) continue;
                if (unanimous == null) unanimous = value;
                else if (Math.abs(unanimous - value) > 0.0001) {
                    disagrees = true;
                    break;
                }
            }
            if (unanimous == null || disagrees) return;

            for (Holder<Item> holder : pair.getSecond()) {
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(holder.value());
                if (known.getOrDefault(itemId, 0.0) > 0.0
                        || classifications.get(itemId) != family || conflicts.contains(itemId)) continue;
                Double previous = candidates.putIfAbsent(itemId, unanimous);
                if (previous != null && Math.abs(previous - unanimous) > 0.0001) {
                    candidates.remove(itemId);
                    paths.remove(itemId);
                    conflicts.add(itemId);
                } else {
                    paths.putIfAbsent(itemId, tagId);
                }
            }
        });
        conflicts.forEach(candidates::remove);
        conflicts.forEach(paths::remove);
        return new FamilyDerivation(Map.copyOf(candidates), Map.copyOf(paths));
    }

    private static ResourceFamily standardFamily(ResourceLocation tagId) {
        String path = tagId.getPath();
        if (tagId.getNamespace().equals("minecraft")
                && (path.equals("logs") || path.equals("logs_that_burn"))) return ResourceFamily.WOOD;
        if (!tagId.getNamespace().equals("forge") && !tagId.getNamespace().equals("c")) return null;
        if (path.equals("foods/raw_meat") || path.equals("foods/raw_meats")) {
            return ResourceFamily.MOB_DROP;
        }
        String root = path.indexOf('/') < 0 ? path : path.substring(0, path.indexOf('/'));
        return STANDARD_TAG_ROOTS.get(root);
    }

    private static void classifyBlockTag(Map<ResourceLocation, ResourceFamily> result,
                                         ResourceLocation tagId, ResourceFamily family) {
        BuiltInRegistries.BLOCK.getTag(TagKey.create(Registries.BLOCK, tagId)).ifPresent(blocks ->
                blocks.forEach(holder -> {
                    Item item = holder.value().asItem();
                    if (item != net.minecraft.world.item.Items.AIR) {
                        putBest(result, BuiltInRegistries.ITEM.getKey(item), family);
                    }
                }));
    }

    private static void classifyVanilla(Map<ResourceLocation, ResourceFamily> result,
                                        ResourceFamily family, String... paths) {
        for (String path : paths) {
            ResourceLocation id = new ResourceLocation("minecraft", path);
            if (BuiltInRegistries.ITEM.containsKey(id)) putBest(result, id, family);
        }
    }

    private static void putBest(Map<ResourceLocation, ResourceFamily> result, Holder<Item> holder,
                                ResourceFamily family) {
        putBest(result, BuiltInRegistries.ITEM.getKey(holder.value()), family);
    }

    private static void putBest(Map<ResourceLocation, ResourceFamily> result, ResourceLocation itemId,
                                ResourceFamily family) {
        result.merge(itemId, family,
                (existing, candidate) -> candidate.precedence() > existing.precedence() ? candidate : existing);
    }
}
