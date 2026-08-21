# TerminatorPlus release publishing

`master` is the source-of-truth branch for current TerminatorPlus development
and Paper/Minecraft 26.2 releases. Release changes must land through a scoped
branch and pull request before anything is published.

The retained compatibility branches are:

| Branch | Purpose | Toolchain |
| --- | --- | --- |
| `master` | Current development and release source | Paper 26.2, Java 25 |
| `mc-26.1.2` | Older compatibility/reference line | Paper 26.1.2, Java 25 |
| `mc-1.21.11` | Older compatibility/reference line | Paper 1.21.11, Java 21 |

Do not publish current releases from a feature branch or from one of the older
compatibility branches unless the release is explicitly scoped to that branch.

## Prerelease procedure

1. Merge the release changes and release notes into the intended target branch.
2. Check out that branch and synchronize it with `origin`.
3. Build the repository.
4. Run the publishing helper with the release version, artifact suffix, and
   target branch.

For the current line:

```bash
git checkout master
git pull --ff-only origin master
./gradlew build -q
bash publish/publish-releases.sh 6.1.5 mc26.2 master
```

The expected default artifact is:

```text
build/libs/TerminatorPlus-6.1.5-BETA-mc26.2.jar
```

The expected default notes file is:

```text
wiki/Release-Notes-6.1.5.md
```

The script creates or updates `v6.1.5-mc26.2` as a prerelease and uploads the
jar. It deliberately does **not** push source code. Before publishing, it
requires all of the following:

- `gh` is installed and authenticated for GitHub;
- the checkout is on the requested target branch;
- the working tree is clean;
- local `HEAD` exactly matches `origin/<target-branch>`;
- the requested jar and release-notes file exist.

This prevents a local feature branch, uncommitted work, or an unpushed commit
from becoming the release source accidentally.

## Overrides

The defaults can be overridden when a deliberately different artifact, tag,
title, notes file, owner, or repository is required:

```bash
PLUGIN_JAR=/path/to/plugin.jar \
NOTES_FILE=/path/to/notes.md \
TAG=v6.1.5-mc26.2 \
TITLE="TerminatorPlus 6.1.5 - mc26.2 (Prerelease)" \
bash publish/publish-releases.sh 6.1.5 mc26.2 master
```

`OWNER` and `REPO` are also accepted as environment variables. Overrides should
be used only for an explicitly reviewed release operation.
