package com.anthonyahellman.gluttony.gameplay;

import com.anthonyahellman.gluttony.GluttonyMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

public final class BossClassifier {
    public static final TagKey<EntityType<?>> PRIDE_BOSSES = TagKey.create(
            Registries.ENTITY_TYPE, new ResourceLocation(GluttonyMod.MOD_ID, "pride_bosses"));

    private BossClassifier() {}

    public static boolean isBoss(LivingEntity entity) {
        return entity.getType().is(PRIDE_BOSSES)
                || entity.getPersistentData().getBoolean("apoth.boss")
                || entity.getPersistentData().getBoolean("apoth.miniboss");
    }
}
