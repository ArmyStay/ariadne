package com.ariadne.extract.bench

import com.ariadne.extract.ReceiptParser
import com.ariadne.model.CurrencyCode

/**
 * Cold-run the extraction domain: parse latency over the committed fixture set
 * (`fixtures` in `commonTest`, the same thirty shapes the parser tests run).
 *
 * Two runs: a warm-up of 50 rounds, then 200 timed rounds. Per-parse samples
 * are collected and reported as p50/p90/mean. The one number the README and the
 * CI gate track is p50 — "extraction p50 on the fixture set".
 */
fun main() {
    val parser = ReceiptParser(CurrencyCode.of("INR"))

    // Warm-up: class loading and any one-off allocation before timing begins.
    repeat(50) { fixtures.forEach { f -> parser.parse(f.lines) } }

    val samples = mutableListOf<Double>()
    repeat(200) {
        fixtures.forEach { f ->
            val t0 = System.nanoTime()
            parser.parse(f.lines)
            val t1 = System.nanoTime()
            samples += (t1 - t0) / 1_000_000.0
        }
    }
    samples.sort()
    val p50 = samples[(samples.size * 0.5).toInt()]
    val p90 = samples[(samples.size * 0.9).toInt()]
    val mean = samples.sum() / samples.size
    println("fixtures: ${fixtures.size}, rounds: 200, parses: ${samples.size}")
    println("extraction parse p50: ${p50} ms   p90: ${p90} ms   mean: ${mean} ms")
}
