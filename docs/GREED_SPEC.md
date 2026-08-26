# Greed Canon and Implementation Status

This file records confirmed Greed decisions. Values marked **TBD** must not be invented in code.

## Awakening

- The **Coin of Mammon** awakens Greed when used.
- The Coin is also a crafting ingredient for Greed machinery.
- A player may possess only one natural sin.
- Awakening Greed reveals item appraisal values in item tooltips.

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
- Each subsequent slot costs 2.5 times the preceding slot.
- Starting slot-expansion cost: **TBD**.
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

Recipe:

| Gold Block | Chest | Netherite Ingot |
| --- | --- | --- |
| Chest | Coin of Mammon | Chest |
| Netherite Ingot | Chest | Gold Block |

## Manual Sale and Market

- Manual selling uses **Mammon's Purse**, with a 3×3 selling grid.
- Selling requires an explicit confirmation button; closing the menu does not sell anything.
- A confirmed manual sale pays 100% of appraisal.
- Sold items enter the Greed owner's Market stock.
- Market prices begin at 50% of appraisal.
- Further Market pricing, demand, and buyer rules are **TBD**.

## Current Code Status

- Coin awakening, persistent Avarice, appraisal synchronization/tooltips, and Greed HUD: implemented.
- Coffer storage, scrolling menu, processing, ownership, pending payouts, and automation input: implemented.
- Appraisal values: data system implemented; canonical values TBD.
- Greed's Vault: specification recovered; implementation pending starting expansion cost/stage-boundary decisions.
- Mammon's Purse and Market: specification recovered; implementation pending remaining Market rules.
