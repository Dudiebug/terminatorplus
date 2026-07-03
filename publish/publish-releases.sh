#!/usr/bin/env bash
# Publish the current mc-26.2 compatibility release.

set -euo pipefail

OWNER="Dudiebug"
REPO="terminatorplus"
BRANCH="mc-26.2"
VERSION="6.1.1"
SUFFIX="mc26.2"
TAG="v${VERSION}-mc26.2"
TITLE="TerminatorPlus ${VERSION} - mc26.2"

PUBLISH_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_DIR="$(cd "$PUBLISH_DIR/.." && pwd)"
PLUGIN_JAR="$REPO_DIR/build/libs/TerminatorPlus-${VERSION}-BETA-${SUFFIX}.jar"
NOTES_FILE="$REPO_DIR/wiki/Release-Notes-${VERSION}.md"

cd "$REPO_DIR"

if ! command -v gh >/dev/null 2>&1; then
    echo "ERROR: gh CLI is required." >&2
    exit 1
fi

if ! gh auth status >/dev/null 2>&1; then
    echo "ERROR: run 'gh auth login' or set GITHUB_TOKEN before this script." >&2
    exit 1
fi

if [[ ! -f "$PLUGIN_JAR" ]]; then
    echo "ERROR: missing release jar: $PLUGIN_JAR" >&2
    echo "Run ./gradlew build -q first." >&2
    exit 1
fi

git push -u origin "$BRANCH"

if gh release view "$TAG" -R "$OWNER/$REPO" >/dev/null 2>&1; then
    gh release upload "$TAG" "$PLUGIN_JAR" -R "$OWNER/$REPO" --clobber
else
    gh release create "$TAG" \
        -R "$OWNER/$REPO" \
        --target "$BRANCH" \
        --title "$TITLE" \
        --notes-file "$NOTES_FILE" \
        "$PLUGIN_JAR"
fi

echo "Published https://github.com/$OWNER/$REPO/releases/tag/$TAG"
