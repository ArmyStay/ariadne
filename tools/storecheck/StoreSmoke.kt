import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.ariadne.model.Capture
import com.ariadne.model.CaptureState
import com.ariadne.model.CurrencyCode
import com.ariadne.model.LocalDay
import com.ariadne.model.Rate
import com.ariadne.model.Trip
import com.ariadne.queue.Outbox
import com.ariadne.queue.QueueEvent
import com.ariadne.store.AriadneStore
import com.ariadne.store.db.AriadneDb
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Drives the real [AriadneStore] over a real SQLite file.
 *
 * Not a mock and not an in-memory fake: this creates the schema through the
 * generated `SqlSchema`, writes through the shipping store, and exercises the
 * paths that matter — the retry backoff that has to survive a restart, the
 * amount landing beside its currency code, the day closing.
 *
 * Run with `tools/store-check.sh`. Exits non-zero on any mismatch, so it works
 * in CI without a test framework.
 */
private var failures = 0

private fun expect(what: String, got: Any?, want: Any?) {
    if (got == want) {
        println("  ok   $what = $got")
    } else {
        println("  FAIL $what = $got (wanted $want)")
        failures++
    }
}

fun main() = runBlocking {
    val file = File(System.getProperty("java.io.tmpdir"), "ariadne-store-check.db")
    file.delete()
    val driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}")
    AriadneDb.Schema.create(driver)
    val store = AriadneStore(driver)

    val day = LocalDay.of(2026, 11, 23)

    println("schema + trips")
    store.createTrip(
        Trip(
            id = "trip-1",
            name = "Bangkok & Home",
            startDate = day,
            endDate = LocalDay.of(2026, 11, 29),
            baseCurrency = CurrencyCode.of("INR"),
            homeCurrency = CurrencyCode.of("INR"),
        ),
    )
    expect("trip name", store.trip("trip-1")?.name, "Bangkok & Home")
    expect("active trip", store.activeTrip()?.id, "trip-1")
    expect("day cards created", store.dayCards("trip-1").size, 7)

    println("captures + money")
    val id = "c-1"
    store.insert(
        Capture(
            id = id,
            tripId = "trip-1",
            localDate = day,
            photoPath = "trip-1/2026-11-23/c-1.jpg",
            createdAt = 1_000L,
            state = CaptureState.QUEUED,
        ),
    )
    expect("photo path round-trips", store.findById(id)?.photoPath, "trip-1/2026-11-23/c-1.jpg")
    // 40.00 INR as minor units: the only money representation in the product.
    store.updateDetails(id, 4_000L, "INR", "Shree Ganesh Chai", false, 2_000L)
    val outbox = Outbox(store) { 2_000L }
    outbox.apply(id, QueueEvent.AmountEntered)
    outbox.apply(id, QueueEvent.Confirmed)
    val done = store.findById(id)!!
    expect("amount is minor units", done.amountMinor, 4_000L)
    expect("currency kept beside it", done.currency?.code, "INR")
    expect("merchant kept", done.merchant, "Shree Ganesh Chai")
    expect("state after confirm", done.state, CaptureState.CONFIRMED)
    expect("confirmed for trip", store.confirmedForTrip("trip-1").size, 1)

    println("retry backoff")
    val retryId = "c-2"
    store.insert(
        Capture(
            id = retryId,
            tripId = "trip-1",
            localDate = day,
            photoPath = "trip-1/2026-11-23/c-2.jpg",
            createdAt = 1_100L,
            state = CaptureState.QUEUED,
        ),
    )
    store.recordRetry(retryId, failedAttempts = 1, notBefore = 9_000L)
    expect("hidden before its time", store.capturesReadyForExtraction(3_000L).size, 0)
    expect("visible after it", store.capturesReadyForExtraction(9_500L).size, 1)

    println("fx + days")
    store.upsertRate(
        Rate(
            base = CurrencyCode.of("THB"),
            quote = CurrencyCode.of("INR"),
            date = day,
            rateScaled = 250_000L,
        ),
    )
    expect("rate scaled integer", store.allRates().first().rateScaled, 250_000L)
    store.closeDay("trip-1", day, at = 50_000L)
    expect("day closed", store.dayCards("trip-1").count { it.isClosed }, 1)

    println("persistence")
    expect("db file written", file.length() > 0, true)

    file.delete()
    if (failures > 0) {
        println("\n$failures CHECK(S) FAILED")
        kotlin.system.exitProcess(1)
    }
    println("\nALL STORE CHECKS PASSED")
}
