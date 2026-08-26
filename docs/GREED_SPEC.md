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

- Hand-set anchors/overrides take precedence.
- Reliable ordinary crafting recipes may derive a value only when every ingredient choice already has the same
  established value. Derived output equals ingredient value divided by output count, preventing value creation.
- Datapack files under `data/*/avarice_appraisals/` override anchors and handle modded/weird cases.
- Processing may create value only through an explicit anchor/override, such as Netherite Ingot at 750.
- Unappraised items remain visibly Unappraised and safely jam destructive liquidation.

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
- Normal 64-stack items use eight fullness stages at each eight-item boundary. A full stack is 100% efficient.
- Partial-stack scaling for naturally non-64-stack items remains TBD; only a full natural stack receives income.

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
- Closing never sells. `DIVEST ASSETS` explicitly confirms, pays full appraisal, and retains items as Market stock.
- The final lower button label is `SELL ITEMS`.
- Non-Greed customers may eventually buy stock at 50% appraisal using physical Minecraft resources.
  Greed players cannot buy from any Greed Market. Exact payment resources and customer UI remain TBD.

## Implementation status

- Awakening, shared H/G routing, Avarice, Pouch, Coffer, appraisal sync/tooltips, and Market-stock retention: implemented.
- Core, Premium, Contract, Asset Appreciation, and Compound Interest mechanics: implemented in 0.11.0.
- Locked anchors, conservative recipe derivation, and five Asset Tiers: implemented in 0.11.0.
- Vault block, owner persistence, slot purchasing, tier rules, payouts, diversification, and UI: implemented in 0.11.0.
- Customer Market transactions and visual/sound polish remain later work because their exact rules are still TBD.
