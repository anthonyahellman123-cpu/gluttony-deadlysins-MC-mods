package com.anthonyahellman.gluttony.gameplay;

import com.anthonyahellman.gluttony.data.GreedData;
import com.anthonyahellman.gluttony.data.SinData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public final class GreedProgression {
    private static final UUID HEALTH_ID = UUID.fromString("9339e31b-37b1-4678-b7aa-1c0aa09e4c81");
    private static final UUID ATTACK_ID = UUID.fromString("12dd56af-2cd0-4cec-8b2d-edf44f8cd211");
    private static final UUID ARMOR_ID = UUID.fromString("4a16ad35-30df-420e-85db-778363814b23");
    private static final UUID MOVEMENT_ID = UUID.fromString("b59b77b3-2ea8-4560-a914-cce4c93aa61a");
    private static final UUID ATTACK_SPEED_ID = UUID.fromString("314aa753-fcd7-4892-a2f1-b6666b812a76");
    private static final UUID KNOCKBACK_ID = UUID.fromString("af5aa631-a80e-4f94-bcf1-26978e9fa60c");

    private GreedProgression() {}

    public static void applyAttributes(ServerPlayer player) {
        boolean greed = SinData.selected(player) == SinData.NaturalSin.GREED;
        GreedData data = GreedData.of(player);
        double appreciation = 1.0 + data.assetAppreciationLevel() * 0.10;

        setModifier(player.getAttribute(Attributes.MAX_HEALTH), HEALTH_ID, "Greed purchased health",
                greed ? data.coreHealthPurchases() * 2.0 * appreciation : 0.0,
                AttributeModifier.Operation.ADDITION);
        setModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_ID, "Greed purchased attack",
                greed ? data.coreAttackPurchases() * appreciation : 0.0,
                AttributeModifier.Operation.ADDITION);
        setModifier(player.getAttribute(Attributes.ARMOR), ARMOR_ID, "Greed purchased armor",
                greed ? data.coreArmorPurchases() * 0.5 * appreciation : 0.0,
                AttributeModifier.Operation.ADDITION);
        setModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), MOVEMENT_ID, "Greed premium movement",
                greed ? data.premiumMovement() * 0.02 : 0.0,
                AttributeModifier.Operation.MULTIPLY_BASE);
        setModifier(player.getAttribute(Attributes.ATTACK_SPEED), ATTACK_SPEED_ID, "Greed premium attack speed",
                greed ? data.premiumAttackSpeed() * 0.5 : 0.0,
                AttributeModifier.Operation.ADDITION);
        setModifier(player.getAttribute(Attributes.KNOCKBACK_RESISTANCE), KNOCKBACK_ID,
                "Greed premium unshakable", greed ? data.premiumKnockbackResistance() * 0.10 : 0.0,
                AttributeModifier.Operation.ADDITION);

        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    public static double coreHealthBonus(GreedData data) {
        return data.coreHealthPurchases() * 2.0 * (1.0 + data.assetAppreciationLevel() * 0.10);
    }

    public static double coreAttackBonus(GreedData data) {
        return data.coreAttackPurchases() * (1.0 + data.assetAppreciationLevel() * 0.10);
    }

    public static double coreArmorBonus(GreedData data) {
        return data.coreArmorPurchases() * 0.5 * (1.0 + data.assetAppreciationLevel() * 0.10);
    }

    private static void setModifier(AttributeInstance attribute, UUID id, String name, double amount,
                                    AttributeModifier.Operation operation) {
        if (attribute == null) return;
        AttributeModifier old = attribute.getModifier(id);
        if (old != null) attribute.removeModifier(old);
        if (amount > 0.0) attribute.addPermanentModifier(new AttributeModifier(id, name, amount, operation));
    }
}
