#!/usr/bin/env bash
# Records a short video of the real preview UI at phone width — the "phone
# video" proof of the camera path. The preview is the shared Compose UI the
# four real targets compile; the camera is not exercised here, so this proves
# the pages, the tab bar and the phone layout, not a shutter tap.
#
#   tools/record-phone-video.sh [output]     # default: phone-video.mp4
#
# Needs Xvfb and ImageMagick (`import`). With ffmpeg on PATH the frames are
# assembled into an MP4; without it the frames are written to a directory next
# to the output name instead.
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="${1:-phone-video.mp4}"
WORK="$(mktemp -d /tmp/ariadne-phone-video.XXXXXX)"
FRAMES="$WORK/frames"
mkdir -p "$FRAMES"

if ! command -v import >/dev/null 2>&1; then
  echo "no ImageMagick `import` — cannot capture frames" >&2
  exit 1
fi

pkill -f "Xvfb :99" 2>/dev/null; sleep 1
Xvfb :99 -screen 0 420x860x24 >/tmp/ariadne-xvfb.log 2>&1 &
XPID=$!
sleep 2
DISPLAY=:99 "$ROOT/tools/run-preview.sh" --phone >/tmp/ariadne-preview.log 2>&1 &
PPID_=$!
sleep 12   # preview is a JVM window; this only matters when it is running

# Capture a few frames as it sits on the phone screen.
for i in 1 2 3 4 5; do
  DISPLAY=:99 import -window root "$FRAMES/frame-$(printf %02d "$i").png" 2>/dev/null
  sleep 2
done

kill $PPID_ 2>/dev/null; kill $XPID 2>/dev/null

if command -v ffmpeg >/dev/null 2>&1; then
  ffmpeg -y -framerate 1 -i "$FRAMES/frame-%02d.png" -vf "fps=4,scale=420:-2:flags=lanczos" "$OUT" >/dev/null 2>&1 \
    && echo "wrote $OUT" || echo "ffmpeg failed; frames in $FRAMES"
else
  echo "no ffmpeg — frames kept in $FRAMES"
  ls "$FRAMES"
fi
