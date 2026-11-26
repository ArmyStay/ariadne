package com.ariadne.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RingLoggerTest {

    @Test
    fun `snapshot is oldest-first`() {
        val ring = RingLogger(maxLines = 4)
        repeat(4) { i -> ring.i("line $i", tag = "T") }
        val expected = listOf("I T line 0", "I T line 1", "I T line 2", "I T line 3")
        val actual = ring.snapshot()
        assertEquals(expected.size, actual.size)
        expected.zip(actual).forEach { (line, entry) -> assertTrue(entry.endsWith(line)) }
    }

    @Test
    fun `oldest are dropped beyond the ring size`() {
        val ring = RingLogger(maxLines = 3)
        repeat(5) { i -> ring.d("line $i", tag = "T") }
        val expected = listOf("D T line 2", "D T line 3", "D T line 4")
        val actual = ring.snapshot()
        assertEquals(expected.size, actual.size)
        expected.zip(actual).forEach { (line, entry) -> assertTrue(entry.endsWith(line)) }
    }

    @Test
    fun `app error goes in and clear empties it`() {
        val ring = RingLogger()
        ring.e("boom", null, "AppGraph")
        assertTrue(ring.snapshot().any { it.endsWith("E AppGraph boom") })
        ring.clear()
        assertTrue(ring.snapshot().isEmpty())
    }

    @Test
    fun `clear from AppDiagnostics empties the sink`() {
        val ring = RingLogger()
        val before = AppLogger.sinks
        AppLogger.sinks = listOf(ring)
        try {
            AppLogger.i("a line", tag = "T")
            assertTrue(AppDiagnostics.current().isNotEmpty())
            AppDiagnostics.clearAll()
            assertTrue(AppDiagnostics.current().isEmpty())
        } finally {
            AppLogger.sinks = before
        }
    }
}
