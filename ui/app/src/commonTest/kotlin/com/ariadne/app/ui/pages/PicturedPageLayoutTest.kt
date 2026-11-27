package com.ariadne.app.ui.pages

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PicturedPageLayoutTest {
    @Test fun controlsReflowForNarrowWindowsAndLargeText() {
        assertFalse(PicturedPageLayout.stack(320f, 250f, 1f))
        assertTrue(PicturedPageLayout.stack(220f, 250f, 1f))
        assertTrue(PicturedPageLayout.stack(320f, 250f, 2f))
        assertFalse(PicturedPageLayout.stack(600f, 250f, 2f))
    }

    @Test fun chartLabelsUseAvailableWidthAndFontScale() {
        assertEquals(1, PicturedPageLayout.chartLabelStride(7, 280f, 1f))
        assertEquals(2, PicturedPageLayout.chartLabelStride(7, 280f, 2f))
        assertEquals(26, PicturedPageLayout.chartLabelStride(255, 280f, 1f))
        assertEquals(255, PicturedPageLayout.chartLabelStride(255, 0f, 1f))
        assertEquals(1, PicturedPageLayout.chartLabelStride(0, 0f, 1f))
    }
}
