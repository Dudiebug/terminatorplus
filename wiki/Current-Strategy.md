# Current Strategy

TerminatorPlus is now a 1v1 PvP bot project.

The goal is one strong bot versus one skilled human PvPer in a controlled duel
arena. Work should improve actual duel behavior or protect the repo from
regressions.

Primary target branch: `mc-26.1.2`.

## Priority Order

1. Build stability
2. Correct bot lifecycle
3. 1v1 movement and spacing
4. Vanilla hit timing
5. Sword, axe, and shield fundamentals
6. Defensive recovery
7. Punish logic
8. Controlled advanced tools
9. Docs and release polish

## Non-Goals

- Do not optimize for large bot swarms first.
- Do not add flashy mechanics before fundamentals are reliable.
- Do not rewrite Paper/NMS internals without a version-specific runtime reason.
- Do not rewrite `LegacyAgent` broadly.
- Do not break neural-network training.
- Do not make movement code directly execute combat actions.

## Good Bot Behavior

A good bot holds useful melee range, strafes, backs up when too close, waits for
useful attack charge, pressures shields with axe, heals or retreats when low,
chases a low-HP player, and punishes eating, bowing, predictable shielding, and
overcommitment.

Advanced tools such as mace, trident, pearl, cobweb, crystal, anchor, and elytra
should be controlled tactical options, not substitutes for weak fundamentals.
