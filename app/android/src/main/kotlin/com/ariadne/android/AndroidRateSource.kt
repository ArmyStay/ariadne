package com.ariadne.android

import com.ariadne.fx.RateSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

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
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
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
                connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000
    }
}
