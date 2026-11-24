import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.ariadne.model.Capture
import com.ariadne.model.Category
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

    println("detail + categories")
    // A capture is valid with no category and no note; both must stay optional.
    store.insert(
        Capture(
            id = "c-3",
            tripId = "trip-1",
            localDate = day,
            photoPath = "trip-1/2026-11-23/c-3.jpg",
            createdAt = 1_200L,
            state = CaptureState.CONFIRMED,
            amountMinor = 6_000L,
            currency = CurrencyCode.of("INR"),
            confirmedAt = 1_300L,
        ),
    )
    store.insert(
        Capture(
            id = "c-4",
            tripId = "trip-1",
            localDate = day,
            photoPath = "trip-1/2026-11-23/c-4.jpg",
            createdAt = 1_400L,
            state = CaptureState.CONFIRMED,
            amountMinor = 2_500L,
            currency = CurrencyCode.of("THB"),
            confirmedAt = 1_500L,
        ),
    )
    store.updateDetail(
        id = "c-3",
        amountMinor = 6_000L,
        currencyCode = "INR",
        merchant = "Kanchi Silks",
        category = "Shopping",
        note = "gift for amma",
    )
    val edited = store.findById("c-3")
    expect("category written", edited?.category, Category.Shopping)
    expect("note written", edited?.note, "gift for amma")
    store.updateDetail(
        id = "c-4",
        amountMinor = 2_500L,
        currencyCode = "THB",
        merchant = null,
        category = "Food",
        note = null,
    )
    val uncategorised = store.findById("c-1")
    expect("no category is a legal state", uncategorised?.category, null)

    // c-1 = 4000 INR-minor, c-3 = 6000 INR-minor, c-4 = 2500 THB-minor @ 0.25
    // INR per THB = 625 INR-minor. All figures are minor units, so 4000 is ₹40.00.
    val totals = store.categoryTotals("trip-1")
    expect("category rows", totals.size, 3)
    expect("largest first", totals.first().category, Category.Shopping)
    val food = totals.firstOrNull { it.category == Category.Food }
    expect("fx converted to base", food?.baseMinor, 625L)
    expect("food capture count", food?.captures, 1)
    val none = totals.firstOrNull { it.category == null }
    expect("uncategorised still totalled", none?.baseMinor, 4_000L)

    // A currency with no rate must surface its raw figure, never a zero that
    // reads as "you spent nothing".
    store.insert(
        Capture(
            id = "c-5",
            tripId = "trip-1",
            localDate = day,
            photoPath = "trip-1/2026-11-23/c-5.jpg",
            createdAt = 1_600L,
            // Deliberately left unfinished: it must not reach the ledger yet.
            state = CaptureState.NEEDS_CONFIRMATION,
            amountMinor = 9_900L,
            currency = CurrencyCode.of("EUR"),
            category = Category.Lodging,
        ),
    )
    // An unfinished capture is not in the ledger yet: it is a guess until the
    // human settles it, and the total above this line does not count it either.
    expect(
        "unfinished is not yet a figure",
        store.categoryTotals("trip-1").map { it.category }.contains(Category.Lodging),
        false,
    )
    expect("finished rows still there", store.categoryTotals("trip-1").size, 3)
    store.updateState("c-5", CaptureState.CONFIRMED)
    val orphan = store.categoryTotals("trip-1").firstOrNull { it.category == Category.Lodging }
    expect("no rate is not a zero", orphan?.baseMinor, 9_900L)

    println("persistence")
    expect("db file written", file.length() > 0, true)

    file.delete()
    if (failures > 0) {
        println("\n$failures CHECK(S) FAILED")
        kotlin.system.exitProcess(1)
    }
    println("\nALL STORE CHECKS PASSED")
}
