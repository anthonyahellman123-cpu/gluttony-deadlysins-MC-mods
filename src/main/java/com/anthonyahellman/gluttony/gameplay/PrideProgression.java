package com.anthonyahellman.gluttony.gameplay;

import com.anthonyahellman.gluttony.data.PrideData;
import com.anthonyahellman.gluttony.data.SinData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public final class PrideProgression {
    private static final UUID HEALTH_ID = UUID.fromString("217a23e5-d6f8-4df8-b4b7-67091c9f9670");
    private static final UUID ATTACK_ID = UUID.fromString("852147a2-6fd5-4a9e-9257-420fd3ad857d");

    private PrideProgression() {}

    public static void applyAttributes(ServerPlayer player) {
        boolean pride = SinData.selected(player) == SinData.NaturalSin.PRIDE;
        PrideData data = PrideData.of(player);
        setModifier(player.getAttribute(Attributes.MAX_HEALTH), HEALTH_ID, "Pride conquered health",
                pride ? data.maxHealthBonus() : 0.0);
        setModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_ID, "Pride conquered attack",
                pride ? data.attackDamageBonus() : 0.0);
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static void setModifier(AttributeInstance attribute, UUID id, String name, double amount) {
        if (attribute == null) return;
        AttributeModifier old = attribute.getModifier(id);
        if (old != null) attribute.removeModifier(old);
        if (amount > 0.0) {
            attribute.addPermanentModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.ADDITION));
        }
    }
}
