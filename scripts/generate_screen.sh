#!/usr/bin/env bash
#
# Scaffold a new screen in Ariadne's Compose-Multimodule layout.
#
# Generates (roughly):
#   ui/app/src/commonMain/kotlin/com/ariadne/app/ui/pages/<Name>.kt
#       The screen composable with a minimal model state stub.
#   ui/app/src/commonMain/kotlin/com/ariadne/app/ui/pages/<Name>UiState.kt
#       Page state + page event — extend to your real feature.
#   ui/app/src/commonTest/kotlin/com/ariadne/app/ui/pages/<Name>UiStateTest.kt
#       A one-file test so every page starts with something green.
#
# Usage (from the repo root): ./scripts/generate_screen.sh Nightfall

set -e

SCREEN_NAME=$1

if [ -z "$SCREEN_NAME" ]; then
  echo "Usage: ./scripts/generate_screen.sh ScreenName"
  exit 1
fi

BASE="ui/app/src/commonMain/kotlin/com/ariadne/app/ui/pages"
BASE_TEST="ui/app/src/commonTest/kotlin/com/ariadne/app/ui/pages"

mkdir -p "$BASE" "$BASE_TEST"

SCREEN_BASE="$(printf '%s' "${SCREEN_NAME:0:1}" | tr '[:lower:]' '[:upper:]')${SCREEN_NAME:1}"
LOWER_NAME=$(echo "$SCREEN_BASE" | tr '[:upper:]' '[:lower:]')
FULL_NAME_LOWER=$(echo "$SCREEN_NAME" | tr '[:upper:]' '[:lower:]')

cat > "$BASE/$SCREEN_BASE.kt" <<'EOF'
package com.ariadne.app.ui.pages

import androidx.compose.runtime.Composable
import com.ariadne.design.Paper

@Composable
public fun ScreenPlaceholder() {
    Paper {
        // TODO: wire to ${FullNameLower}UiState
    }
}
EOF

cat > "$BASE/${SCREEN_BASE}UiState.kt" <<'EOF'
package com.ariadne.app.ui.pages

/** ${SCREEN_BASE} page state — extend with your feature's data. */
public data class ${SCREEN_BASE}UiState(
    public val loaded: Boolean = false,
)

/** User actions on ${SCREEN_BASE}. */
public sealed interface ${SCREEN_BASE}Event {
    public data object Retry : ${SCREEN_BASE}Event
}
EOF

cat > "$BASE_TEST/${SCREEN_BASE}UiStateTest.kt" <<EOF
package com.ariadne.app.ui.pages

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ${SCREEN_BASE}UiStateTest {

    @Test
    fun \`initial state is empty\`() {
        val state = ${SCREEN_BASE}UiState()
        assertFalse(state.loaded)
    }

    @Test
    fun \`placeholder composes without crashing\`() {
        assertEquals("Paper", Paper::class.simpleName)
    }
}
EOF

echo "Generated $BASE/${SCREEN_BASE}.kt (+ UiState, + test)"
