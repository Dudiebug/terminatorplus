# Movement V2

Movement V2 is an optional combat pathing layer. It is off by default while
its movement and action timing are tested on a live Paper server.

## What it does

Each bot plans inside a 3×3 chunk window: its current chunk and the eight
chunks touching it. The bot cannot plan through an unloaded chunk or beyond
that window. A farther target is projected onto the edge of the current window;
as the bot crosses into the next chunk, the window recentres and it plans the
next local section. This also lets a locally validated placement or break make
progress before the distant target itself enters the window.

The route can contain these steps:

- normal walking, diagonal movement, one-block steps, and drops of at most
  three blocks;
- sprint parkour jumps over gaps;
- opening a wooden, copper, or other hand-opened door, gate, or trapdoor;
- placing a carried solid block to bridge a gap;
- jumping and placing a carried block under the bot to pillar upward;
- a planned water-bucket clutch outside the Nether, or a twisting-vines clutch
  in the Nether;
- breaking an obstructing block with the fastest suitable carried tool,
  including its Efficiency enchantment.

A normal route never treats a drop deeper than three blocks as safe. A clutch
route is separate and is allowed up to `max-clutch-drop`, which defaults to 48
blocks. It is available only when the bot actually carries the required clutch
item.

## Player-like action rule

The pathfinder only writes a plan. It does not change the world or inventory.
The action executor then checks the request against the live world and performs
it as a player action:

- the block must still be present and inside the bot's 3×3 window;
- the bot must be in reach and have a clear interaction ray when the action is
  performed;
- the action must be inside the world border and outside protected spawn unless
  the bot has an applicable bypass;
- Bukkit interaction, placement, and bucket events are fired so protection
  plugins can refuse the action;
- mining waits for the block's player break speed, uses `Player.breakBlock`,
  and refuses a block that would take longer than the bounded action window;
- a needed tool, block, or bucket is temporarily moved from storage to the
  hotbar, selected, consumed or damaged normally, then returned to its prior
  storage slot;
- combat cannot attack, consume, throw, or start another primary action while
  opening, mining, placing, pillaring, or clutching owns the bot's hands, nor
  on the same tick that traversal action finishes.

The planner always tries a route without breaking first. It may request a
break only after that complete search finds no route. Running out of time or
nodes is not proof that no route exists, so a search-budget failure never
enables breaking. If the best non-breaking progress ends at a placement,
pillar, door, or clutch, the bot performs that one checked action and replans
from the changed world before it considers mining. This is what permits a
multi-block bridge or pillar without guessing several world changes ahead.

## Thread and world safety

Live Bukkit block reads and every world change stay on the server thread.
Reading Bukkit blocks directly from a background thread is unsafe even if the
result is used for player-like movement. The current 3×3 context caches block
facts for one planning pass to avoid repeated reads. If route search is moved
to a worker later, the worker must receive an immutable snapshot captured on
the server thread; it must never receive a live Bukkit `World` or `Block`.

## Compatibility boundaries

- The feature does nothing unless `ai.movement.v2.enabled` is `true`.
- The old movement and obstacle code remains the fallback.
- Full-replacement neural-network mode is unchanged.
- Movement training bots are excluded so saved fitness values are not silently
  measured against a different movement system.
- The movement neural network still observes the real opponent and combat
  intent. A route waypoint changes only where its locomotion output steers.
- Existing combat policy still decides whether to approach, hold, commit, or
  use a combat item. Movement V2 does not attack or choose combat tactics.

## Status command

Use `/botenvironment movementV2Status` for all active bots, or
`/botenvironment movementV2Status <bot-name>` for one name. The command shows
the current route position, plan/replan/fallback counts, action failures, the
last reason, search phase, and expanded node count.

## Assumptions checked during implementation

The review found and fixed several easy-to-miss cases:

- both halves of a normal door are closed, so door opening must accept two
  openable blocks rather than requiring one half to look like air;
- a two-block-high wall may need two separate mining actions and a replan after
  each block;
- water, bubble columns, ladders, vines, scaffolding, and cobwebs are not air;
  V2 hands these states back to the existing swimming/climbing/survival logic;
- slow blocks cannot become an instant break merely because an action timeout
  expired;
- the parkour arc and the next three ordinary route steps are checked again
  against the live arena before movement;
- a finished partial route holds for one tick instead of falling through to an
  unvalidated direct step at the edge of its context window;
- bucket placement waits until the landing block is genuinely in reach;
- placement asks Paper whether the carried block data can exist at the target;
- creative-mode placement and bucket use do not consume the item;
- temporary hotbar promotion is reversed so pathing does not permanently
  scramble a loadout; when all nine slots are weapons, the selected slot can
  be borrowed for the action, the prior selected slot is restored exactly,
  and its visible held-item packet is refreshed;
- the older survival and passive-combat layers cannot replace the leased item
  while a traversal action owns the hands;
- a target disappearing does not cancel a bridge, pillar, mining action, or
  clutch that is already underway; no new route starts without a target;
- a running clutch remains in control after the bot touches its placed water so
  it can finish the landing and pick the source back up;
- water pickup waits for the source to settle, and cancellation makes a
  best-effort pickup without duplicating a bucket;
- pillar placement waits until the bot's feet clear the full block, and
  parkour never receives a stronger vertical launch than a normal bot jump.

## Known limits and live test list

These are intentionally not claimed as proven gameplay behavior:

- exact sprint-jump success under server movement drag and low TPS — **needs
  runtime test**;
- bucket and twisting-vines clutch timing at 4, 8, 16, 32, and 48 blocks —
  **needs runtime test**;
- repeated bridge placement and repeated pillaring as the bot replans after
  every placed block — **needs runtime test**;
- tool damage, drops, enchantments, potion effects, and protection-plugin
  cancellation during mining — **needs runtime test**;
- doors, fence gates, trapdoors, copper doors, and paired door halves — **needs
  runtime test**;
- slabs, stairs, fences, walls, snow layers, honey, slime, moving pistons, and
  other partial collision shapes. The current grid is deliberately
  conservative; **needs runtime test**;
- another player or bot stepping into a landing/placement space after planning.
  Live block state is rechecked, but crowd avoidance is not yet part of A*;
- modded materials. Bukkit's collision result is used, but custom hazards and
  unusual block actions need server-specific tests;
- world rules beyond the world border, spawn protection, and Bukkit event
  cancellation. A region plugin remains the final authority;
- boats, elytra, wind charges, pearls, swimming, and free-fall recovery that was
  not chosen as a route step remain owned by the existing survival/combat code.

Recommended arena runs should record route completion, fallbacks, action
failures, same-tick primary-action violations, inventory before/after, blocks
changed, damage taken, and server tick time. Until that evidence exists, the
feature gate should remain off.
