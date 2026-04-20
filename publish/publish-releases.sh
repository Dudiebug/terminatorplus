#!/usr/bin/env bash
# Push mc-1.21.11 / mc-26.1 / mc-26.1.1 / mc-26.1.2 branches and create matching
# GitHub releases with the jars in this directory.
#
# Two ways to authenticate (either works):
#
#   (A) gh CLI:   gh auth login       # interactive once, then `bash publish/publish-releases.sh`
#   (B) PAT:      export GITHUB_TOKEN=ghp_...  &&  bash publish/publish-releases.sh
#
# gh is already installed at C:/Users/dudie/bin/gh.exe. PAT needs `repo` scope.

set -euo pipefail

OWNER="Dudiebug"
REPO="terminatorplus"
PUBLISH_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_DIR="$(cd "$PUBLISH_DIR/.." && pwd)"

# Resolve token: GITHUB_TOKEN env var wins, otherwise fall back to gh's stored creds.
if [[ -z "${GITHUB_TOKEN:-}" ]]; then
    if command -v gh >/dev/null 2>&1 && gh auth status >/dev/null 2>&1; then
        GITHUB_TOKEN="$(gh auth token)"
    else
        cat >&2 <<EOF
ERROR: no GitHub credentials found.
  - Either run:     gh auth login          (and re-run this script)
  - Or export:      GITHUB_TOKEN=ghp_...   (PAT with repo scope)
EOF
        exit 1
    fi
fi

API="https://api.github.com"
UPLOADS="https://uploads.github.com"
AUTH_HDR="Authorization: Bearer $GITHUB_TOKEN"
UA="User-Agent: tplus-publish"

BRANCHES=(mc-1.21.11 mc-26.1 mc-26.1.1 mc-26.1.2)
SUFFIXES=(mc1.21.11 mc26.1 mc26.1.1 mc26.1.2)
TITLES=(
    "4.5.2-BETA for Minecraft 1.21.11"
    "4.5.2-BETA for Minecraft 26.1"
    "4.5.2-BETA for Minecraft 26.1.1"
    "4.5.2-BETA for Minecraft 26.1.2"
)
TAGS=(v4.5.2-mc1.21.11 v4.5.2-mc26.1 v4.5.2-mc26.1.1 v4.5.2-mc26.1.2)

# One-shot credential helper so git push uses the PAT instead of Git Credential
# Manager's browser flow.
CRED_ARG=(-c "credential.helper=!f() { echo username=x-access-token; echo password=$GITHUB_TOKEN; }; f")

cd "$REPO_DIR"

jsonify() { python -c 'import json,sys; print(json.dumps(sys.stdin.read()))'; }
json_field() { python -c "import json,sys; print(json.loads(sys.stdin.read()).get('$1',''))"; }

for i in "${!BRANCHES[@]}"; do
    branch="${BRANCHES[$i]}"
    suffix="${SUFFIXES[$i]}"
    title="${TITLES[$i]}"
    tag="${TAGS[$i]}"
    plugin_jar="$PUBLISH_DIR/TerminatorPlus-4.5.2-BETA-${suffix}.jar"
    api_jar="$PUBLISH_DIR/TerminatorPlus-API-4.5.2-BETA-${suffix}.jar"

    echo "=== $branch ==="

    echo "Pushing $branch..."
    git "${CRED_ARG[@]}" push -u origin "$branch" 2>&1 | sed "s/$GITHUB_TOKEN/REDACTED/g"

    echo "Creating release $tag..."
    body="Built against Paper for Minecraft ${suffix#mc}.

Files:
- \`TerminatorPlus-4.5.2-BETA-${suffix}.jar\` — drop in \`plugins/\`.
- \`TerminatorPlus-API-4.5.2-BETA-${suffix}.jar\` — API module for plugin developers.

Branch: \`$branch\`."

    payload=$(printf '{"tag_name":"%s","name":"%s","target_commitish":"%s","body":%s,"prerelease":true}' \
        "$tag" "$title" "$branch" "$(printf '%s' "$body" | jsonify)")

    resp=$(curl -sS -X POST -H "$AUTH_HDR" -H "$UA" -H "Accept: application/vnd.github+json" \
        -d "$payload" "$API/repos/$OWNER/$REPO/releases")
    release_id=$(printf '%s' "$resp" | json_field id)
    if [[ -z "$release_id" ]]; then
        echo "Release create failed: $resp" >&2
        exit 1
    fi

    for jar in "$plugin_jar" "$api_jar"; do
        name=$(basename "$jar")
        echo "  uploading $name..."
        curl -sS -X POST -H "$AUTH_HDR" -H "$UA" -H "Content-Type: application/java-archive" \
            --data-binary @"$jar" \
            "$UPLOADS/repos/$OWNER/$REPO/releases/$release_id/assets?name=$name" >/dev/null
    done

    echo "  done: https://github.com/$OWNER/$REPO/releases/tag/$tag"
done

echo ""
echo "All four releases published on https://github.com/$OWNER/$REPO/releases"
