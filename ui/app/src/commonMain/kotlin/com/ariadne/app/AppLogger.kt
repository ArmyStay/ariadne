package com.ariadne.app

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlin.time.Clock

/** A log sink. Portable from the KMP contest starter kit's logging facade. */
public interface Logger {
    public fun initialize(isDebug: Boolean = true)
    public fun e(message: String, throwable: Throwable? = null, tag: String? = null)
    public fun d(message: String, throwable: Throwable? = null, tag: String? = null)
    public fun i(message: String, throwable: Throwable? = null, tag: String? = null)
}

/**
 * The console logger. Without Koin, the facade is a plain object; the
 * `Logger` interface is kept so a sink can be swapped in later.
 */
public object NapierLogger : Logger {

    override fun initialize(isDebug: Boolean) {
        if (isDebug) Napier.base(DebugAntilog())
    }

    override fun e(message: String, throwable: Throwable?, tag: String?) {
        Napier.e(message, throwable, tag)
    }

    override fun d(message: String, throwable: Throwable?, tag: String?) {
        Napier.d(message, throwable, tag)
    }

    override fun i(message: String, throwable: Throwable?, tag: String?) {
        Napier.i(message, throwable, tag)
    }
}

/** App-wide logging facade: currently just the Napier sink. */
public object AppLogger : Logger {
    public var sinks: List<Logger> = listOf(NapierLogger)
        private set

    override fun initialize(isDebug: Boolean) {
        sinks.forEach { it.initialize(isDebug) }
    }

    override fun e(message: String, throwable: Throwable?, tag: String?) {
        sinks.forEach { it.e(message, throwable, tag) }
    }

    override fun d(message: String, throwable: Throwable?, tag: String?) {
        sinks.forEach { it.d(message, throwable, tag) }
    }

    override fun i(message: String, throwable: Throwable?, tag: String?) {
        sinks.forEach { it.i(message, throwable, tag) }
    }
}

/** In-memory sink: recent log lines, first N, oldest-first, with level. For diagnostics screens. */
public class RingLogger(private val maxLines: Int = 256) : Logger {

    private val lock = Any()
    private val lines = ArrayDeque<String>()
    private var initialized = false

    override fun initialize(isDebug: Boolean) {
        synchronized(lock) {
            initialized = true
            log("I", "AppLogger initialized, isDebug=$isDebug", tag = null)
        }
    }

    override fun e(message: String, throwable: Throwable?, tag: String?) {
        log("E", message, throwable, tag)
    }

    override fun d(message: String, throwable: Throwable?, tag: String?) {
        log("D", message, throwable, tag)
    }

    override fun i(message: String, throwable: Throwable?, tag: String?) {
        log("I", message, throwable, tag)
    }

    private fun log(level: String, message: String, throwable: Throwable? = null, tag: String?) {
        val stamp = "${Clock.System.now()} $level ${tag ?: ""} ${message.take(200)}"
        synchronized(lock) {
            while (lines.size >= maxLines) lines.removeFirst()
            lines.addLast(stamp)
            throwable?.let {
                lines.addLast("    at ${it::class.simpleName}: ${it.message ?: ""}")
                lines.addLast("    at ${it::class.qualifiedName}")
            }
        }
    }

    public fun snapshot(): List<String> = synchronized(lock) { lines.toList() }

    public fun clear() = synchronized(lock) { lines.clear() }
}

// (RingLogger is registered as a sink — see AppDiagnostics.kt.)
