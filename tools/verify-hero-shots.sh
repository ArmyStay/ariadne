#!/usr/bin/env bash
# Re-render the six hero frames with `tools/run-preview.sh --shots` and compare
# them against the golden snapshots in docs/shots/golden/. Frames are matched
# by hash: a missing or changed file means a design change, so the script
# prints an OK/STALE table and exits non-zero when anything is stale.
#
# Run it after any visual change: intended changes update docs/shots/golden/
# (the six hero frames — shutter, Nightfall, record, card, timeline, stats).
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TMP="$(mktemp -d)"
GOLDEN="$ROOT/docs/shots/golden"

# The six hero frames, in README order.
SHOTS=(04-capture 05-nightfall 06-record 07-trip-card 02b-timeline 02c-stats)

if ! command -v sha256sum >/dev/null 2>&1; then
  echo "no sha256sum available" >&2
  exit 1
fi

"$ROOT/tools/run-preview.sh" --shots "$TMP" >/dev/null 2>&1 || {
  echo "render failed (needs a JVM for the preview harness)" >&2
  exit 1
}

rc=0
for name in "${SHOTS[@]}"; do
  g="$GOLDEN/$name.png"
  r="$TMP/$name.png"
  if [ ! -f "$g" ]; then
    echo "MISSING golden: $name"
    rc=1
    continue
  fi
  if [ ! -f "$r" ]; then
    echo "MISSING render: $name (the page no longer exists)"
    rc=1
    continue
  fi
  gs=$(sha256sum "$g" | cut -d' ' -f1)
  rs=$(sha256sum "$r" | cut -d' ' -f1)
  if [ "$gs" = "$rs" ]; then
    echo "OK     $name"
  else
    echo "STALE  $name — review and re-commit the golden snapshot"
    rc=1
  fi
done
rm -rf "$TMP"
exit $rc
