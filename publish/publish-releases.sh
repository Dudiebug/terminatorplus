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
    "5.0.0-BETA for Minecraft 1.21.11"
    "5.0.0-BETA for Minecraft 26.1.2"
)
TAGS=(v5.0.0-mc1.21.11 v5.0.0-mc26.1.2)

cd "$REPO_DIR"

for i in "${!BRANCHES[@]}"; do
    branch="${BRANCHES[$i]}"
    suffix="${SUFFIXES[$i]}"
    title="${TITLES[$i]}"
    tag="${TAGS[$i]}"
    plugin_jar="$PUBLISH_DIR/TerminatorPlus-5.0.0-BETA-${suffix}.jar"
    api_jar="$PUBLISH_DIR/TerminatorPlus-API-5.0.0-BETA-${suffix}.jar"

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

**What's new in 5.0.0-BETA**
- Weapon-aware combat AI: mace smash, trident momentum throw, wind charges, ender pearls, crystal PvP, anchor bomb (Nether), cobweb utility, elytra glide + firework boost
- Passive behaviors: elytra↔chestplate auto-swap, totem of undying auto-equip
- Full per-bot inventory editor: \`/bot inventory <name>\` opens a 54-slot chest GUI
- Built-in loadouts: \`sword\`, \`mace\`, \`trident\`, \`windcharge\`, \`skydiver\`, \`hybrid\`, \`crystalpvp\`, \`anchorbomb\`, \`pvp\`, \`clear\`
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
