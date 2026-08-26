package com.anthonyahellman.gluttony.network;

import com.anthonyahellman.gluttony.client.GreedClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record GreedStatePacket(
        double avarice, double lifetimeEarned, double lifetimeSpent,
        long assetsDivested, double vaultIncome, double cofferIncome,
        long marketActivity, long contractClaims, int marketStockStacks,
        int coreHealth, int coreAttack, int coreArmor,
        int premiumMovement, int premiumAttackSpeed, int premiumLuck,
        int premiumKnockback, int premiumYield,
        int compoundInterest, int assetAppreciation, int contractLevel,
        int claimsInWindow, double currentClaimCost, long claimResetAt) {

    public static void encode(GreedStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.avarice);
        buffer.writeDouble(packet.lifetimeEarned);
        buffer.writeDouble(packet.lifetimeSpent);
        buffer.writeVarLong(packet.assetsDivested);
        buffer.writeDouble(packet.vaultIncome);
        buffer.writeDouble(packet.cofferIncome);
        buffer.writeVarLong(packet.marketActivity);
        buffer.writeVarLong(packet.contractClaims);
        buffer.writeVarInt(packet.marketStockStacks);
        buffer.writeVarInt(packet.coreHealth);
        buffer.writeVarInt(packet.coreAttack);
        buffer.writeVarInt(packet.coreArmor);
        buffer.writeVarInt(packet.premiumMovement);
        buffer.writeVarInt(packet.premiumAttackSpeed);
        buffer.writeVarInt(packet.premiumLuck);
        buffer.writeVarInt(packet.premiumKnockback);
        buffer.writeVarInt(packet.premiumYield);
        buffer.writeVarInt(packet.compoundInterest);
        buffer.writeVarInt(packet.assetAppreciation);
        buffer.writeVarInt(packet.contractLevel);
        buffer.writeVarInt(packet.claimsInWindow);
        buffer.writeDouble(packet.currentClaimCost);
        buffer.writeVarLong(packet.claimResetAt);
    }

    public static GreedStatePacket decode(FriendlyByteBuf buffer) {
        return new GreedStatePacket(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readVarLong(), buffer.readDouble(), buffer.readDouble(),
                buffer.readVarLong(), buffer.readVarLong(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readDouble(), buffer.readVarLong());
    }

    public static void handle(GreedStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> GreedClientState.update(packet)));
        context.setPacketHandled(true);
    }
}
