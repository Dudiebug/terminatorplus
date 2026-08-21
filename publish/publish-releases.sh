#!/usr/bin/env bash
# Publish a TerminatorPlus prerelease from an already-pushed branch.

set -euo pipefail

OWNER="${OWNER:-Dudiebug}"
REPO="${REPO:-terminatorplus}"
VERSION="${1:-}"
SUFFIX="${2:-mc26.2}"
TARGET_BRANCH="${3:-master}"

usage() {
    cat >&2 <<'USAGE'
Usage: bash publish/publish-releases.sh <version> [suffix] [target-branch]

Examples:
  bash publish/publish-releases.sh 6.1.5
  bash publish/publish-releases.sh 6.1.5 mc26.2 master

The script publishes a prerelease. It never pushes source code. The current
checkout must be clean, checked out on the target branch, and exactly match the
remote target branch.
USAGE
}

if [[ -z "$VERSION" ]]; then
    usage
    exit 2
fi

PUBLISH_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_DIR="$(cd "$PUBLISH_DIR/.." && pwd)"
TAG="${TAG:-v${VERSION}-${SUFFIX}}"
TITLE="${TITLE:-TerminatorPlus ${VERSION} - ${SUFFIX} (Prerelease)}"
PLUGIN_JAR="${PLUGIN_JAR:-$REPO_DIR/build/libs/TerminatorPlus-${VERSION}-BETA-${SUFFIX}.jar}"
NOTES_FILE="${NOTES_FILE:-$REPO_DIR/wiki/Release-Notes-${VERSION}.md}"

cd "$REPO_DIR"

if ! command -v gh >/dev/null 2>&1; then
    echo "ERROR: gh CLI is required." >&2
    exit 1
fi

if ! gh auth status -h github.com >/dev/null 2>&1; then
    echo "ERROR: authenticate gh for github.com before publishing." >&2
    exit 1
fi

CURRENT_BRANCH="$(git branch --show-current)"
if [[ -z "$CURRENT_BRANCH" || "$CURRENT_BRANCH" != "$TARGET_BRANCH" ]]; then
    echo "ERROR: checkout target branch '$TARGET_BRANCH' before publishing; current branch is '${CURRENT_BRANCH:-detached HEAD}'." >&2
    exit 1
fi

if [[ -n "$(git status --porcelain)" ]]; then
    echo "ERROR: the working tree must be clean before publishing." >&2
    exit 1
fi

if ! git remote get-url origin >/dev/null 2>&1; then
    echo "ERROR: git remote 'origin' is not configured." >&2
    exit 1
fi

git fetch --quiet origin "$TARGET_BRANCH"
LOCAL_SHA="$(git rev-parse HEAD)"
REMOTE_SHA="$(git rev-parse "origin/$TARGET_BRANCH")"
if [[ "$LOCAL_SHA" != "$REMOTE_SHA" ]]; then
    echo "ERROR: local HEAD does not match origin/$TARGET_BRANCH." >&2
    echo "Local:  $LOCAL_SHA" >&2
    echo "Remote: $REMOTE_SHA" >&2
    exit 1
fi

if [[ ! -f "$PLUGIN_JAR" ]]; then
    echo "ERROR: missing release jar: $PLUGIN_JAR" >&2
    echo "Run ./gradlew build -q first and confirm the requested version/suffix." >&2
    exit 1
fi

if [[ ! -f "$NOTES_FILE" ]]; then
    echo "ERROR: missing release notes: $NOTES_FILE" >&2
    exit 1
fi

if gh release view "$TAG" -R "$OWNER/$REPO" >/dev/null 2>&1; then
    gh release edit "$TAG" \
        -R "$OWNER/$REPO" \
        --target "$TARGET_BRANCH" \
        --title "$TITLE" \
        --notes-file "$NOTES_FILE" \
        --prerelease
    gh release upload "$TAG" "$PLUGIN_JAR" -R "$OWNER/$REPO" --clobber
else
    gh release create "$TAG" \
        -R "$OWNER/$REPO" \
        --target "$TARGET_BRANCH" \
        --title "$TITLE" \
        --notes-file "$NOTES_FILE" \
        --prerelease \
        "$PLUGIN_JAR"
fi

echo "Published prerelease $TAG from $TARGET_BRANCH at $LOCAL_SHA"
