package com.ariadne.app.ui.pages

import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TripCardPngTest {
    @Test
    fun encodesValidPngWithExpectedColorAndDimensions() {
        val decoded = ImageIO.read(
            ByteArrayInputStream(encodeRgbaPng(1, 1, intArrayOf(0xFFFF0000.toInt()))),
        )

        assertNotNull(decoded)
        assertEquals(1, decoded.width)
        assertEquals(1, decoded.height)
        assertEquals(0xFFFF0000.toInt(), decoded.getRGB(0, 0))
    }
}
