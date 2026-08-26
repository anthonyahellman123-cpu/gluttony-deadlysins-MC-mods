# Greed Canon and Implementation Status

This file records confirmed Greed decisions. Values marked **TBD** must not be invented in code.

## Awakening

- The **Coin of Mammon** awakens Greed when used.
- The Coin is also a crafting ingredient for Greed machinery.
- A player may possess only one natural sin.
- Awakening Greed reveals item appraisal values in item tooltips.
- `H` opens the read-only contextual Sin dashboard.
- `G` is the universal Sin ability key. For Greed it opens the Pouch of Mammon.
- The dashboard and Pouch show Greed only; a player can never own multiple natural Sins.

Coin recipe:

| Gold Block | Nether Star | Gold Block |
| --- | --- | --- |
| Netherite Ingot | Apple | Netherite Ingot |
| Diamond Block | Netherite Ingot | Emerald Block |

## Avarice

- Avarice is Greed's currency.
- All Greed systems use the same per-item appraisal table.
- Exact appraisal values are **TBD**.
- The intended final evaluator considers scarcity, acquisition difficulty, renewability, recipe ingredients,
  processing complexity, rarity, and mob/boss or unique-item exclusivity.
- Automatic vanilla/modded recipe derivation, cheapest legitimate acquisition paths, circular-recipe protection,
  enchantments, durability, and other NBT adjustments were discussed but their exact rules remain **TBD**.
- Previously mentioned item numbers were examples, not canon, and are not installed as defaults.
- Appraisals are loaded from datapack JSON files under `data/*/avarice_appraisals/`.
- An item without an established entry is shown as **Unappraised**. It is never assigned an invented fallback value.

Example appraisal file:

```json
{
  "item": "minecraft:diamond",
  "value": 0.0
}
```

The zero is only a format example, not the canonical Diamond value.

## Greed's Vault

- Capacity ceiling: 54 slots.
- A new Vault begins with one unlocked slot.
- Slots expand individually toward 54.
- Slot-expansion pricing: **TBD**.
- Every 30 minutes, stored stacks generate 20% of their appraised value as passive Avarice.
- Stack fullness has eight stages: 12.5%, 25%, 37.5%, 50%, 62.5%, 75%, 87.5%, and 100%.
- Exact stage boundaries for items whose maximum stack size is not 64: **TBD**.
- Any additional row multiplier remains **TBD** and is not canonical.

Recipe:

| Gold Block | Chest | Diamond Block |
| --- | --- | --- |
| Chest | Coin of Mammon | Chest |
| Diamond Block | Chest | Gold Block |

## Coffer of Avarice

- **Coffer of Avarice** is the canonical name. “Capitalism Chest” was only the joke/workbench name.
- 10 columns by 20 rows: 200 slots total.
- The interface scrolls vertically through the 20 rows.
- Every five seconds, the front 10-slot row is consumed.
- Whole stacks in that row are permanently destroyed and cannot be recovered.
- The remaining rows advance toward the front after each processing cycle.
- The owner receives 10% of the destroyed items' total appraised value.
- A completely full Coffer takes 100 seconds to process.
- Hopper and pipe insertion is supported; automated extraction is blocked.
- Payout earned while the owner is offline is retained by the Coffer and credited when the owner returns.
- Development safety: an unappraised item in the front row jams processing instead of being destroyed for zero.
  This interlock becomes invisible once the canonical appraisal system prices all eligible items.

Recipe:

| Netherite Ingot | Chest | Netherite Ingot |
| --- | --- | --- |
| Chest | Coin of Mammon | Chest |
| Gold Block | Chest | Gold Block |

## Pouch of Mammon

- The Pouch is Greed's `G` ability interface.
- The left side is the investment shop; the right side contains a permanent 3×3 selling grid.
- Selling requires an explicit confirmation button; closing the menu does not sell anything.
- The confirmation label is **DIVEST ASSETS**.
- A confirmed manual sale pays 100% of appraisal.
- Sold items enter the Greed owner's Market stock.
- Current Avarice and the current Contract claim price must be highly visible.

## Core Investments

- Max Health, Attack Damage, and Armor track purchases independently.
- Each begins at 100 Avarice per purchase.
- Price doubles after every ten purchases: 100 for purchases 1–10, 200 for 11–20, 400 for 21–30, etc.
- There is no intended Core level cap.
- Exact attribute gain per purchase is **TBD**; purchasing must remain unavailable until those gains are confirmed.

## Premium Investments

- Premiums have a hard cap of 10 levels.
- Levels 1–2 cost 5,000 each; 3–4 cost 7,500; 5–6 cost 11,250; 7–8 cost 16,875;
  and 9–10 cost 25,312.5.
- Movement Speed, Attack Speed, Luck, Knockback Resistance, and Avarice Yield are candidates.
- The final roster and exact effects are **TBD**.
- Avarice Yield changes received Avarice, never underlying appraisal values.

## Pinnacle Assets

- Exactly three passive Pinnacles, each capped at Level 5. They add no keybinds.
- **Compound Interest:** every level adds one delayed echo dealing 75% of the preceding hit. Echoes cannot
  recursively trigger echoes. Prices: 250,000; 500,000; 1,000,000; 2,000,000; 4,000,000.
- **Asset Appreciation:** multiplies only stats bought from Core Investments. Candidate scaling is +10% per
  level through +50%. Prices: 250,000; 500,000; 1,000,000; 2,000,000; 4,000,000.
- **Contract of Mammon:** when affordable, lethal damage deducts the current claim price, prevents death,
  and restores 75% maximum health. There is no debt, negative Avarice, free revival, or activation when the
  claim is unaffordable. The first claim costs 100,000 and every further claim in the window doubles it.
  Level 1 has a 60-minute reset window and Level 5 has a 30-minute window. Intermediate windows and the
  Contract's acquisition prices are **TBD**.

## Market

- Manually divested items are retained in that Greed player's Market stock rather than destroyed.
- Non-Greed players may purchase Market stock.
- Greed players cannot purchase from their own or another Greed player's Market.
- Market prices begin at 50% of appraisal.
- Customers pay physical Minecraft resources, not Avarice; those payments become the Greed owner's assets.
- Payment resources and the customer interface remain **TBD**.

## Current Code Status

- Coin awakening, persistent Avarice, appraisal synchronization/tooltips, and Greed HUD: implemented.
- Coffer storage, scrolling menu, processing, ownership, pending payouts, and automation input: implemented.
- Appraisal values: data system implemented; canonical values TBD.
- Greed's Vault: specification recovered; implementation pending starting expansion cost/stage-boundary decisions.
- Pouch of Mammon persistent selling grid, safe explicit divestment, retained Market stock, economy history,
  and contextual `G` opening: implemented.
- Core, Premium, Pinnacle, Vault, and Market sections display their confirmed state without inventing TBD effects.
