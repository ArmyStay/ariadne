package com.ariadne.design

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * The book's palette, handed to Material 3.
 *
 * Everything on the page is drawn from [Paper] directly — a hairline is `paper.edge`,
 * a figure is `paper.ink` — so the design has never needed a theme. But the shell now
 * contains real Material components: the navigation bar, its items, the docked
 * shutter, the diagnostics screen. Each of those falls back to the stock Material
 * scheme wherever it is not told otherwise, and the stock scheme is purple. A ripple,
 * a focus ring, a disabled label, a tooltip — all of them would arrive in a colour
 * that exists nowhere in this app.
 *
 * So: one function that translates a [Paper] into a [ColorScheme], and one theme
 * composable that installs it. Pages still take `paper` as a parameter; this is only
 * for the components that ask the theme instead of being told.
 *
 * Two deliberate mappings:
 *
 *   - **error is blue.** This app has exactly one chromatic value on purpose, and a
 *     red that only appears in a Material default would be the loudest thing on a
 *     page about someone's holiday. Anything that needs to look wrong here looks
 *     *heavier*, not redder.
 *   - **surfaceTint is blue**, which in Material 3 means elevated surfaces tint
 *     towards blue instead of lavender.
 */
@Composable
public fun AriadneTheme(paper: Paper, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = paper.materialScheme(), content = content)
}

/** The same palette as a Material colour scheme, for the components that ask. */
public fun Paper.materialScheme(): ColorScheme {
    // Two branches rather than one: light and dark differ in the values Material
    // derives for the slots this app never sets directly.
    return if (isNight) {
        darkColorScheme(
            primary = blue,
            onPrimary = onBlue,
            primaryContainer = blueWash,
            onPrimaryContainer = ink,
            secondary = inkMid,
            onSecondary = bg,
            secondaryContainer = sunk,
            onSecondaryContainer = ink,
            tertiary = blue,
            onTertiary = onBlue,
            tertiaryContainer = blueWash,
            onTertiaryContainer = ink,
            background = bg,
            onBackground = ink,
            surface = raised,
            onSurface = ink,
            surfaceVariant = sunk,
            onSurfaceVariant = inkSoft,
            surfaceTint = blue,
            outline = edgeStrong,
            outlineVariant = edge,
            error = bluePressed,
            onError = onBlue,
            errorContainer = blueWash,
            onErrorContainer = ink,
            inverseSurface = ink,
            inverseOnSurface = bg,
            inversePrimary = blueEdge,
            scrim = scrim,
        )
    } else {
        lightColorScheme(
            primary = blue,
            onPrimary = onBlue,
            primaryContainer = blueWash,
            onPrimaryContainer = ink,
            secondary = inkMid,
            onSecondary = bg,
            secondaryContainer = sunk,
            onSecondaryContainer = ink,
            tertiary = blue,
            onTertiary = onBlue,
            tertiaryContainer = blueWash,
            onTertiaryContainer = ink,
            background = bg,
            onBackground = ink,
            surface = raised,
            onSurface = ink,
            surfaceVariant = sunk,
            onSurfaceVariant = inkSoft,
            surfaceTint = blue,
            outline = edgeStrong,
            outlineVariant = edge,
            error = bluePressed,
            onError = onBlue,
            errorContainer = blueWash,
            onErrorContainer = ink,
            inverseSurface = ink,
            inverseOnSurface = bg,
            inversePrimary = blueEdge,
            scrim = scrim,
        )
    }
}
