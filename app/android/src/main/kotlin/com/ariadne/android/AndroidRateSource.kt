package com.ariadne.android

import com.ariadne.fx.RateSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * The one place Ariadne touches the network.
 *
 * A read-only GET to frankfurter.app, five seconds in each direction, closed in a
 * `finally`. `HttpURLConnection` rather than an HTTP client library because this is
 * the whole of the requirement — one URL, one JSON body — and a new dependency for
 * it would need justifying line by line.
 *
 * Every failure returns null and no exception leaves this class. A missing rate is
 * a thing the app already knows how to live with: it keeps the last known rate,
 * converts at it, and prints its date. A trip in a dead zone must not lose its
 * totals because a fetch timed out.
 */
internal class AndroidRateSource : RateSource {

    override suspend fun get(url: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val parsed = URL(url)
            if (!isAllowed(parsed)) return@runCatching null
            val connection = (parsed.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MILLIS
                readTimeout = TIMEOUT_MILLIS
                // No cookies, no redirects off the host, nothing carried over.
                useCaches = false
                instanceFollowRedirects = false
                setRequestProperty("Accept", "application/json")
            }
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
                if (connection.contentLengthLong > MAX_RESPONSE_BYTES) return@runCatching null
                if (connection.contentType?.substringBefore(';')?.trim()?.lowercase() !in setOf(null, "application/json")) {
                    return@runCatching null
                }
                val bytes = connection.inputStream.use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    val output = java.io.ByteArrayOutputStream()
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > MAX_RESPONSE_BYTES) return@runCatching null
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                }
                bytes.toString(StandardCharsets.UTF_8)
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000
        const val MAX_RESPONSE_BYTES = 64L * 1024L
        val EXPECTED_PATH = Regex("/(?:latest|\\d{4}-\\d{2}-\\d{2})")
        val EXPECTED_QUERY = Regex("from=[A-Z]{3}&to=[A-Z]{3}(?:,[A-Z]{3})*")

        fun isAllowed(url: URL): Boolean =
            url.protocol == "https" &&
                url.host == "api.frankfurter.app" &&
                (url.port == -1 || url.port == 443) &&
                EXPECTED_PATH.matches(url.path) &&
                EXPECTED_QUERY.matches(url.query.orEmpty())
    }
}
