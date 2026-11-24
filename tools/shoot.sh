#!/usr/bin/env bash
# Runs the preview under Xvfb and screenshots it. Used to eyeball the real UI
# without a display, and to produce stills for the README.
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ARGS="${*:1}"
OUT="${SHOT_OUT:-/tmp/shot.png}"
WAIT="${SHOT_WAIT:-14}"

pkill -f "Xvfb :99" 2>/dev/null; sleep 1
Xvfb :99 -screen 0 1400x1000x24 >/tmp/xvfb.log 2>&1 &
XPID=$!
sleep 2
DISPLAY=:99 "$ROOT/tools/run-preview.sh" $ARGS >/tmp/preview.log 2>&1 &
APP=$!
sleep "$WAIT"
DISPLAY=:99 import -window root -crop 1400x1000+0+0 "$OUT" 2>/dev/null
kill $APP 2>/dev/null; kill $XPID 2>/dev/null
echo "wrote $OUT ($(stat -c%s "$OUT" 2>/dev/null || echo 0) bytes)"
