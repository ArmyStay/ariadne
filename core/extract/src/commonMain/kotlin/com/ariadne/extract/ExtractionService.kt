package com.ariadne.extract

import com.ariadne.model.CurrencyCode
import com.ariadne.model.LocalDay
import com.ariadne.queue.CaptureRepository
import com.ariadne.queue.Outbox
import com.ariadne.queue.QueueEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Best-effort extraction, off the capture path.
 *
 * The contract that matters: this class can fail in every possible way and the
 * capture still exists, still has its photo, and still reaches Nightfall. It
 * never throws to its caller and never blocks a shutter.
 */
public class ExtractionService(
    private val recognizer: TextRecognizer,
    private val repository: CaptureRepository,
    private val outbox: Outbox,
    private val parserFor: (CurrencyCode) -> ReceiptParser,
    private val baseCurrency: () -> CurrencyCode,
    private val extractionDispatcher: CoroutineDispatcher,
    private val retryDelayMillis: (attempt: Int) -> Long = { com.ariadne.queue.Backoff.delayMillis(it) },
) {
    private val scope = CoroutineScope(SupervisorJob() + extractionDispatcher)
    private val _lastOutcome = MutableStateFlow<ExtractionOutcome?>(null)
    public val lastOutcome: StateFlow<ExtractionOutcome?> = _lastOutcome

    /** Walks the queue once, oldest first. Safe to call on every app start. */
    /**
     * One drain at a time.
     *
     * Every capture used to start its own loop, so five photographs meant five
     * loops pulling from the same queue, racing on the same rows and each
     * re-triggering a refresh. On a device with no recogniser bound every one of
     * them also retried with backoff, which is how a shutter tap turned into a
     * stutter across the whole app. The flag makes drain idempotent: asking twice
     * costs nothing.
     */
    @kotlin.concurrent.Volatile
    private var draining = false

    public fun drain() {
        // Checked and set on the caller's thread. A lost race costs one extra loop
        // that finds an empty queue and exits; a mutex here would suspend the
        // shutter handler, which is the thing this is trying to avoid.
        if (draining) return
        draining = true
        scope.launch {
            try {
                while (true) {
                    val next = outbox.nextForExtraction() ?: break
                    extractOne(next.id, next.photoPath)
                }
            } finally {
                draining = false
            }
        }
    }

    /** Extracts a single capture by id. Never throws. */
    public suspend fun extractOne(id: String, photoPath: String) {
        outbox.apply(id, QueueEvent.ExtractionStarted)
        val outcome = runCatching { extract(photoPath) }
            .getOrElse { ExtractionOutcome.Failed(it.message ?: "recognizer threw") }
        _lastOutcome.value = outcome

        when (outcome) {
            is ExtractionOutcome.Failed -> {
                outbox.apply(id, QueueEvent.ExtractionFailed(outcome.reason))
                val failures = outbox.failuresFor(id)
                if (!outbox.exhaustedRetries(id)) {
                    delay(retryDelayMillis(failures))
                    outbox.apply(id, QueueEvent.Retried)
                }
            }

            is ExtractionOutcome.Nothing -> {
                // Text read fine, nothing usable: this is a human problem, not a
                // retry problem. Go straight to the stack with a blank field.
                outbox.apply(id, QueueEvent.ExtractionFoundAmount(confident = false))
            }

            is ExtractionOutcome.AmountOnly -> {
                repository.updateDetails(
                    id = id,
                    amountMinor = outcome.amountMinor,
                    currencyCode = outcome.currency.code,
                    merchant = null,
                    markedNoAmount = false,
                    confirmedAt = null,
                )
                outbox.apply(
                    id,
                    QueueEvent.ExtractionFoundAmount(
                        confident = outcome.confidence >= ReceiptParser.MERCHANT_MIN_CONFIDENCE,
                    ),
                )
            }

            is ExtractionOutcome.AmountAndMerchant -> {
                repository.updateDetails(
                    id = id,
                    amountMinor = outcome.amountMinor,
                    currencyCode = outcome.currency.code,
                    merchant = outcome.merchant,
                    markedNoAmount = false,
                    confirmedAt = null,
                )
                outbox.apply(id, QueueEvent.ExtractionFoundAmount(confident = true))
            }
        }
    }

    private suspend fun extract(photoPath: String): ExtractionOutcome = withContext(extractionDispatcher) {
        when (val result = recognizer.recognize(photoPath)) {
            is OcrResult.Failure -> ExtractionOutcome.Failed(result.reason)
            is OcrResult.Text -> parserFor(baseCurrency()).parse(result.lines)
        }
    }

    public companion object {
        /** No-op recognizer for Wasm and for platforms where OCR is not yet wired. */
        public fun unavailable(reason: String): TextRecognizer = object : TextRecognizer {
            override suspend fun recognize(imagePath: String): OcrResult = OcrResult.Failure(reason)
        }

        /** Date helper so callers do not have to reach into the model layer. */
        public fun dateOrNull(day: LocalDay?): LocalDay? = day
    }
}
