# The Roots of Sin

A Forge 1.20.1 Minecraft mod built around accepting the sin of Gluttony, harvesting souls, and permanently extracting a portion of defeated enemies' strength.

## Prototype loop

1. Craft and eat the Cursed Apple to open yourself to Gluttony.
2. Kill non-player mobs to harvest souls.
3. Grow Gluttony levels from lifetime souls.
4. Permanently extract max health and attack damage according to the current level.

Eating the apple begins a dangerous awakening: saturation is stripped away and hunger rapidly drains until the player secures their first non-player kill. Use `/gluttony stats` to inspect awakening state, progression, extraction efficiency, and consumed attributes.

## Pride slice

Pride is awakened by eating Pride's Sol, crafted from an apple and four Nether Stars. Pride deals reduced damage to ordinary creatures and increased damage to bosses. Its progression consists of four independent supremacy trials: 12 Ender Dragons, 8 Withers, 4 Elder Guardians, and 2 Wardens. Every recorded boss grants +2 maximum health and +1 base attack damage; completing a trial grants +10 health and +5 attack. Difficulty-weighted boss damage reaches 200% after all four trials.

After four recorded bosses, the shared Sin Ability key unlocks Sovereign's Advance, a weapon-hit dash. Completed trials add Grounding, healing suppression, underwater control, and an unstoppable impact. Completing all trials evolves it into Absolute Domination: successful hits grant regeneration and open a one-second auto-targeted follow-up that deals maximum-health damage.

The reusable Sin Ability HUD slot changes its icon and color for the player's awakened sin. It displays the server-authoritative radial cooldown to tenths of a second and switches to a bright recast indicator during Absolute Domination's follow-up window.

Pride's Sol uses a dedicated solar apple texture with a white-gold radiant core and restrained amber-orange edges.

Developer commands include `/sin clear`, Pride trial grant/complete/reset controls, and a full completion shortcut. Clearing a natural sin preserves its progression while immediately removing its active attributes and HUD state.

Grounded pulls flying targets down until terrain or collision support is reached, then pins vertical movement without forcing them through blocks. Ender Dragons use heightmap-aware bounding-box correction to remain above the terrain surface.

Only one natural sin may be awakened per player. Optional mod integrations enhance classification but are never required for the base progression.
