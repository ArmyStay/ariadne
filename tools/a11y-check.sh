#!/usr/bin/env bash
# Is every press in this app one a screen reader can make?
#
# The failure this exists for is quiet and invisible on a phone: a control drawn with
# `pointerInput { detectTapGestures { … } }` is focusable and describable, and cannot be
# activated. TalkBack reads the label, offers no action, and the person on the other side
# concludes the app is broken. Nothing about the frame shows it.
#
# So the rule is narrow and total: a press goes through `Modifier.tap` (ui/Tap.kt), which
# is `clickable` with no ripple, a role and the words for what it will do. Raw gesture
# detection is allowed only on the scrims that close a sheet — a dimmed page is not a
# control, and the sheet behind it carries a labelled one of its own.
#
#   ./tools/a11y-check.sh     # exits 0 when every press can be made by hand, or a name
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

ui="ui/app/src/commonMain/kotlin"
fail=0

say() {
  echo "a11y-check: $1" >&2
  if [ -n "${GITHUB_ACTIONS:-}" ]; then
    printf '::error::%s\n' "$1"
  fi
}

if [ ! -d "$ui" ]; then
  say "$ui is missing — has the UI moved?"
  exit 1
fi

# 1. A press is not a gesture. Scrims are the exception, and they are named here rather
#    than described, so adding a second one is a decision someone has to make.
while IFS= read -r hit; do
  file=${hit%%:*}
  case "$(basename "$file")" in
    ReminderSheet.kt) continue ;;
  esac
  say "raw tap handling at ${hit#./} — use Modifier.tap(\"what the press does\") { … }"
  fail=1
done < <(grep -rn "detectTapGestures\|pointerInput" "$ui" --include=*.kt | grep -v "ui/Tap.kt")

# 2. A press says what it does. "Tap", "Select" and "Activate" name the gesture, not the
#    consequence, and a person listening cannot tell them apart.
while IFS= read -r hit; do
  say "a press with a generic or empty label: ${hit#./}"
  fail=1
done < <(grep -rnE '\.tap\(\s*"(|-)?(Tap|Click|Activate|Select|Press)?\s*"' "$ui" --include=*.kt)

# 3. The label is a sentence a person can follow without seeing the control.
while IFS= read -r hit; do
  label=$(printf '%s' "$hit" | sed -E 's/.*\.tap\(\s*"([^"]*)".*/\1/')
  words=$(printf '%s' "$label" | wc -w)
  if [ "$words" -lt 2 ]; then
    say "a one-word press label (\"$label\") at ${hit%%:*} — say what it does"
    fail=1
  fi
done < <(grep -rn '\.tap("' "$ui" --include=*.kt | grep -v "ui/Tap.kt")

if [ "$fail" -ne 0 ]; then
  exit 1
fi

presses=$(grep -rn '\.tap(' "$ui" --include=*.kt | grep -v "ui/Tap.kt" | wc -l | tr -d ' ')
echo "a11y-check: every one of the $presses presses carries a label a screen reader can read"
