#!/usr/bin/env bash
# Re-generates app/src/main/assets/textmate from pinned npm packages.
set -euo pipefail
GRAMMARS_VERSION="${GRAMMARS_VERSION:-1.32.22}"
THEMES_VERSION="${THEMES_VERSION:-1.12.12}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
( cd "$WORK" && npm pack --silent "tm-grammars@${GRAMMARS_VERSION}" "tm-themes@${THEMES_VERSION}" >/dev/null )
mkdir -p "$WORK/g" "$WORK/t"
tar xzf "$WORK"/tm-grammars-*.tgz -C "$WORK/g"
tar xzf "$WORK"/tm-themes-*.tgz -C "$WORK/t"
node "$ROOT/tools/generate-textmate-assets.mjs" "$WORK/g/package" "$WORK/t/package"
