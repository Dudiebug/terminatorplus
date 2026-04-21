#!/usr/bin/env bash
# Push mc-1.21.11 / mc-26.1.2 branches and create matching
# GitHub releases with the jars in this directory. Uses gh CLI; run `gh auth login` first.

set -euo pipefail

if ! command -v gh >/dev/null 2>&1; then
    export PATH="/c/Users/dudie/bin:$PATH"
fi
if ! gh auth status >/dev/null 2>&1; then
    echo "ERROR: run 'gh auth login' before this script." >&2
    exit 1
fi

OWNER="Dudiebug"
REPO="terminatorplus"
PUBLISH_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_DIR="$(cd "$PUBLISH_DIR/.." && pwd)"

BRANCHES=(mc-1.21.11 mc-26.1.2)
SUFFIXES=(mc1.21.11 mc26.1.2)
TITLES=(
    "5.0.2-BETA for Minecraft 1.21.11"
    "5.0.2-BETA for Minecraft 26.1.2"
)
TAGS=(v5.0.2-mc1.21.11 v5.0.2-mc26.1.2)

cd "$REPO_DIR"

for i in "${!BRANCHES[@]}"; do
    branch="${BRANCHES[$i]}"
    suffix="${SUFFIXES[$i]}"
    title="${TITLES[$i]}"
    tag="${TAGS[$i]}"
    # Root fat jar (includes plugin.yml + all classes); API jar is for developers only.
    plugin_jar="$REPO_DIR/build/libs/TerminatorPlus-5.0.2-BETA-${suffix}.jar"
    api_jar="$REPO_DIR/TerminatorPlus-API/build/libs/TerminatorPlus-API-5.0.2-BETA-${suffix}.jar"

    echo "=== $branch ==="

    if git ls-remote --exit-code --heads origin "$branch" >/dev/null 2>&1; then
        echo "branch already on origin, skipping push"
    else
        echo "pushing $branch..."
        git push -u origin "$branch"
    fi

    if gh release view "$tag" -R "$OWNER/$REPO" >/dev/null 2>&1; then
        echo "release $tag already exists, skipping"
        continue
    fi

    echo "creating release $tag..."
    notes_file=$(mktemp)
    cat >"$notes_file" <<EOF
Built against Paper for Minecraft ${suffix#mc}.

**What's new in 5.0.2-BETA**
- **Wind-charge spam bug fixed** (the headline bug). Bots were firing wind charges at any target 4+ blocks away as their default ranged play — the standalone wind charge case in \`CombatDirector\` treated it as a generic ranged weapon. Wind charges are now removed from the standard pipeline entirely; they only fire via deliberate \`OpportunityScanner\` plays (knockup-crystal setup, block-place interrupt, aerial strike) and \`ComboBehavior\` engage/escape launches. Net result: bots use the right weapon for the range and the wind charge becomes a finisher, not a spam gimmick.
- **Opportunity-based combat scanner (37 plays, 4 tiers)**: the new \`OpportunityScanner\` evaluates every tick, in priority order, a bank of wiki-sourced PvP plays — S-tier (crystal clutch, anchor bomb, mace airdrop, pearl stuff, totem reset), A-tier (trident elytra-pierce, splash-harming combo, cobweb-stuff, crystal knockup + wind launch), B-tier (bow snipe over void, potion deny, axe shield-break, sword crit jump), C/D-tier (utility fallbacks, trap placement). First matching opportunity fires; strict inventory-kit gating + per-play cooldowns stop any one trick from dominating a fight.
- **Wind + pearl velocity-stacked combos** via new \`ComboBehavior\`: \`WIND_PEARL_ENGAGE\` spawns a wind charge behind the bot and throws a pearl forward 2 ticks later — the blast wave stacks onto the pearl's velocity and closes a 20+ block gap in one launch. \`WIND_PEARL_ESCAPE\` does the mirror: wind charge between bot and target, pearl thrown over the shoulder, resulting in a long-range disengage the target cannot chase.
- **Shared battlefield snapshot** (\`CombatSnapshot\`) — per-tick reading of target airborne/rising/over-void, target blocking/eating/drinking/using-bow, target-near-wall, target-in-water, bot-in-lava-area, bot-on-fire, target-throwing-pearl, open sky above bot. Populated once per tick, read by the scanner and by the standard pipeline, so every opportunity check is O(1) instead of recomputing geometry.
- **Mid-attack phase commitment**: once a bot commits to a mace airdrop (\`AIRBORNE\` phase) or a trident charge-up (\`CHARGING\` phase), the director stays on that weapon until the phase completes — no more mid-animation weapon swaps dropping damage.
- **Aerial mace dive-through**: bots in free-fall with a mace now steer toward any target within 10 blocks horizontally and 2 blocks vertically, committing to the airdrop automatically instead of cancelling the smash to swap to a sword on the way down.

**What's new in 5.0.1-BETA**
- **Attack cooldown fixed**. Melee bots previously swung every 3 ticks regardless of weapon, producing ~25% of full damage per hit and never crossing vanilla's 0.848 crit/sweep/sprint-knockback threshold. \`MeleeBehavior\` + \`MaceBehavior\` fallback + \`LegacyAgent\` + \`BotAgent\` all now gate their swings on attack-strength charge >= 0.95 AND target i-frame count <= 10. Net result: a sword bot swings every 13t (full charge) at 100% damage and produces crits/sweeps normally — roughly 4× the previous effective DPS on sword, ~5× on mace.
- **Swing-gate helper** \`BotCombatTiming\` exposes \`canSwing(bot, target)\` / \`chargeReady(bot)\` / \`targetHasIFrames(target)\`; API consumers can use \`Terminator.canSwingAttack(target)\`.
- **Automatic movement kit**: every bot now always carries ender pearls and wind charges. The stacks top up every 2s, so no preset or \`/bot give\` is required to unlock gap-closes and wind-charge boosts.
- **Ender pearls for long-range travel**: bots throw a pearl whenever the target is 28+ blocks away (was 14–35). Pearls no longer decrement the stack — the 3s cooldown paces them.
- **Wind-charge self-propulsion — direction-aware and deliberate**: bots plan a wind-charge throw based on target geometry — below them if the target is on a ledge (launches UP), above them if the target is below (launches DOWN), behind them for flat-ground traversal (launches FORWARD). There's a 4-tick windup so the throw reads as a build, and a 6-second cooldown between attempts. Only fires in the 12–28 block approach window, never during combat or while mid-mace-jump / mid-trident-charge.
- **Five new PvP-kit loadouts** matching the Minecraft Wiki's kit taxonomy: \`vanilla\`, \`axe\`, \`smp\`, \`pot\`, \`spear\`. Apply with \`/bot loadout <kit> [bot-name]\`. Cart and UHC kits intentionally skipped (TNT-minecart aiming + natural-regen-disabled rulesets aren't on the bot yet).
- **Reactive consumable use** — bots eat golden apples + enchanted apples, drink healing / fire-resistance / strength potions, throw splash-healing at their feet to self-heal, and throw splash harming / poison / slowness at enemies in range. \`ConsumableBehavior\` runs every combat tick with a priority stack: fire-res → critical-HP heal → low-HP heal → buff-up → offensive splash. Each path has its own cooldown so the bot doesn't drain its inventory in one tick.
- \`BotInventory.armorTier(Material)\`, \`getEquippedArmorTier()\`, \`ensureMovementKit()\`, \`findHealingPotion()\`, \`findSplashHealing()\`, \`findStrengthPotion()\`, \`findFireResPotion()\`, \`findSplashHarming()\`, \`hasAnyConsumable()\` are now public for API consumers.

**Carried over from 5.0.0-BETA**
- Weapon-aware combat AI: mace smash, trident momentum throw, wind charges, ender pearls, crystal PvP, anchor bomb (Nether), cobweb utility, elytra glide + firework boost
- Passive behaviors: elytra↔chestplate auto-swap, totem of undying auto-equip
- Full per-bot inventory editor: \`/bot inventory <name>\` opens a 54-slot chest GUI
- Built-in loadouts: \`sword\`, \`mace\`, \`trident\`, \`windcharge\`, \`skydiver\`, \`hybrid\`, \`crystalpvp\`, \`anchorbomb\`, \`pvp\`, \`vanilla\`, \`axe\`, \`smp\`, \`pot\`, \`spear\`, \`clear\`
- YAML preset system: save/apply/list/delete bot loadouts + behavior settings with full NBT preservation
- \`/bot weapons [bot]\`: shows which behaviors each bot's inventory unlocks
- \`terminatorplus.admin\` permission node for destructive commands

**Files**
- \`$(basename "$plugin_jar")\` — drop in \`plugins/\`.
- \`$(basename "$api_jar")\` — API module for plugin developers.

**Branch:** \`$branch\`

---

Fork of [HorseNuggets/TerminatorPlus](https://github.com/HorseNuggets/TerminatorPlus). Licensed under the Eclipse Public License 2.0 — see [LICENSE](https://github.com/$OWNER/$REPO/blob/$branch/LICENSE). This build modifies the upstream sources to compile and run on the target Paper runtime; the diff on branch \`$branch\` documents the changes.
EOF

    gh release create "$tag" \
        -R "$OWNER/$REPO" \
        --target "$branch" \
        --title "$title" \
        --notes-file "$notes_file" \
        --prerelease \
        "$plugin_jar" "$api_jar"

    rm -f "$notes_file"
    echo "  done: https://github.com/$OWNER/$REPO/releases/tag/$tag"
done

echo ""
echo "Both releases published on https://github.com/$OWNER/$REPO/releases"
