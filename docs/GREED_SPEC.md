# Greed / Mammon Canon and Implementation Status

This file records locked Greed decisions. Missing or ambiguous behavior remains **TBD** and must not be invented.

## Controls and identity

- The Coin of Mammon awakens Greed and is also a crafting ingredient.
- A player may possess only one natural Sin.
- `H` opens the read-only page for the player's awakened Sin only.
- `G` invokes the awakened Sin's action. For Greed, it opens the Pouch of Mammon. With no Sin, `G` does nothing.
- Avarice is persistent, non-physical, and cannot be directly transferred.

## Core Investments

- Max Health: +1 heart / +2 HP per purchase.
- Attack Damage: +1 per purchase.
- Armor: +0.5 per purchase.
- Each stat tracks purchases independently and has no intended cap.
- Purchases 1–10 cost 100 each, 11–20 cost 200, 21–30 cost 400, and each later ten-purchase band doubles again.

## Premium Investments

Every Premium has a 10-level hard cap. Levels 1–2 cost 5,000 each; 3–4 cost 7,500; 5–6 cost 11,250;
7–8 cost 16,875; and 9–10 cost 25,312.5.

- Movement Speed: +2% additive per level, +20% at Level 10.
- Attack Speed: +0.5 additive per level, +5.0 at Level 10.
- Luck: +20% physical item-drop yield per level after normal loot calculation, including Fortune. Fractional items use
  probabilistic rounding, and Luck-created items cannot recursively trigger Luck.
- Unshakable: +10% knockback resistance and +10% damage-induced screen-shake reduction per level. It does not
  prevent damage or unrelated crowd control/teleports.
- Avarice Yield: +5% Avarice received per level. It never changes underlying item appraisal.

## Pinnacle Assets

All three Pinnacles are passive, capped at Level 5, and add no keybinds.

### Compound Interest

- Direct qualifying weapon hits build one stack per hit, capped by Pinnacle level (1–5).
- Every stack contributes +3 seconds duration and +25% of the stored weapon-hit damage per tick.
- Tick interval: 1.5 seconds.
- At Level 5 the maximum is five stacks, 15 seconds, and 125% stored weapon-hit damage per tick.
- Hits at maximum stacks refresh duration; Compound Interest damage cannot apply Compound Interest.
- Upgrade prices: 250,000; 500,000; 1,000,000; 2,000,000; 4,000,000.

### Asset Appreciation

- Levels 1–5 amplify Core Investment bonuses by +10%, +20%, +30%, +40%, and +50%.
- It applies only to Greed-bought Max HP, Attack Damage, and Armor—not equipment, enchantments, potions,
  Apotheosis/modded attributes, or any other external stat source.
- Upgrade prices: 250,000; 500,000; 1,000,000; 2,000,000; 4,000,000.

### Contract of Mammon

- An affordable claim cancels lethal damage, deducts the claim, and restores 75% maximum HP.
- An unaffordable claim does nothing: normal death, no debt, no negative Avarice, and no free revival.
- Base claim: 100,000. Further claims during the rolling window double: 200,000; 400,000; 800,000; etc.
- Reset windows by level: 60, 52.5, 45, 37.5, and 30 minutes.
- Purchase/upgrade costs: 100,000; 150,000; 225,000; 337,500; 506,250.

## Hybrid Appraisal and Asset Tiers

- Hand-set stone-tablet anchors take absolute precedence. The 2-Ava log anchor covers the complete Forge/Minecraft
  logs tag so wood, stripped wood, stems, and compatible tagged modded logs can safely seed related recipes.
- Reliable ordinary crafting recipes derive the cheapest known ingredient path divided by output count. Every
  alternative accepted by an ingredient must already have a positive appraisal; recipes with an unknown alternative
  remain unresolved instead of guessing. Derivation iterates through recipe chains and chooses the cheapest reliable
  path, which prevents compression/decompression and reversible crafting from manufacturing Avarice.
- Datapack files under `data/*/avarice_appraisals/` provide configured values for modded/weird cases when no reliable
  recipe derivation exists. Stone-tablet anchors still win, and reliable derivation takes priority over this fallback.
- Processing may create value only through an explicit anchor/override, such as Netherite Ingot at 750.
- Greed tooltips and `/greed appraise` show item name, Ava per item, quantity, total Ava, and Asset Tier before sale.
- Appraisal uses one service with separate authoritative server and synchronized client stores. Server divestment, Vault,
  and Coffer calls may never consult the client store; client previews may never mutate or replace the server store.
- Appraisal synchronization preserves decimal values as doubles and includes source plus deriving recipe metadata.
- `/greed appraise` reports item ID, status, exact decimal value, source, recipe when applicable, tier, or the explicit
  unresolved reason `NO_SUPPORTED_VALUE_PATH`.
- Unresolved items show `VALUE TBD`, cannot be Divested or Vaulted, and safely jam destructive liquidation.
- Beef and Leather remain intentionally unresolved until a deterministic source or locked override is specified:
  Beef has no supported value-producing crafting path, while Leather's vanilla Rabbit Hide recipe begins from another
  unresolved item. No placeholder values are invented for either item.

Tiers are appraisal-based: T1 0–9.99, T2 10–24.99, T3 25–74.99, T4 75–499.99, and T5 500+ Avarice.
The full locked vanilla anchor table is installed in `AvariceAppraisals`.

## Greed's Vault

- 9 columns × 5 rows = 45 investment slots. Items are never consumed.
- Slot 1 is free. The remaining unlocks cost: Row 1 8×50; Row 2 9×100; Row 3 9×250;
  Row 4 9×500; Row 5 9×1,000. Total: 17,050 Avarice.
- Row 1 accepts T1 and yields 20%; Row 2 accepts T1–T2 and yields 25%; Row 3 accepts T1–T3 and yields 30%;
  Row 4 accepts T1–T4 and yields 40%; Row 5 accepts T1–T5 and yields 50%, all per 30 minutes.
- Higher rows may hold lower-tier items.
- Different item IDs in a row add diversification: 3 unique +2.5 points, 5 +5, 7 +7.5, 9 +10.
  NBT, names, and enchantments do not make duplicate IDs unique.
- Every Vault position has a custom 64-unit investment capacity regardless of the item's normal Minecraft stack size.
  This rule is Vault-only and never changes normal inventories.
- Fullness and yield scale linearly from invested quantity: 1 item is 1/64, 16 is 25%, 32 is 50%, 48 is 75%,
  and 64 is 100%. A Vault "full stack" always means 64 invested units.
- The Vault appraises only the top-level item. Shulker contents, backpacks, bundles, stored fluids/energy, nested
  inventories, and similar contained resources contribute no additional value.
- Withdrawals and block breaking split invested quantities back into legal vanilla-sized stacks before they leave
  the Vault.

Recipe:

| Gold Block | Chest | Diamond Block |
| --- | --- | --- |
| Chest | Coin of Mammon | Chest |
| Diamond Block | Chest | Gold Block |

## Coffer of Avarice

- 10×20 scrolling inventory, 200 slots.
- Every five seconds it destroys one full 10-slot front row, advances all later rows, and pays 10% appraisal.
- Hoppers/pipes may insert; automated extraction is blocked. Offline earnings are buffered.
- An unappraised front-row item jams processing instead of being deleted for zero.

Recipe:

| Netherite Ingot | Chest | Netherite Ingot |
| --- | --- | --- |
| Chest | Coin of Mammon | Chest |
| Gold Block | Chest | Gold Block |

## Pouch and Market

- The Pouch is Greed's `G` interface and preserves the right-shifted ledger layout introduced in 0.10.1.
- It contains Core, Premium, and Pinnacle buying; a persistent 3×3 manual-divest grid; player inventory; Avarice;
  and Contract status.
- Closing never sells. `DIVEST ASSETS` explicitly confirms, destroys the submitted items, and pays their full appraisal.
- The final lower button label is `SELL ITEMS`.
- Non-Greed customers may eventually buy stock at 50% appraisal using physical Minecraft resources.
  Greed players cannot buy from any Greed Market. Exact payment resources and customer UI remain TBD.

## Implementation status

- Awakening, shared H/G routing, Avarice, Pouch, Coffer, and pre-sale appraisal feedback: implemented.
- Core, Premium, Contract, Asset Appreciation, and Compound Interest mechanics: implemented in 0.11.0.
- Locked anchors, hybrid recipe derivation, arbitrage-safe cheapest paths, five Asset Tiers, split server/client
  authorities, and source-aware diagnostics: implemented in 0.11.2.
- Vault block, owner persistence, slot purchasing, tier rules, payouts, diversification, and UI: implemented in 0.11.0.
- Customer Market transactions and visual/sound polish remain later work because their exact rules are still TBD.
