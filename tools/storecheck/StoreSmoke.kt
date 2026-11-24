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
import com.ariadne.store.openOrMigrate
import com.ariadne.store.schemaVersion
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

    println("trip place and budget")
    expect("no place by default", store.trip("trip-1")?.place, null)
    expect("no budget by default", store.trip("trip-1")?.budgetMinor, null)
    val withFence = store.trip("trip-1")!!.copy(place = "Bangkok", budgetMinor = 2_500_050L)
    store.updateTrip(withFence)
    expect("place survives the round trip", store.trip("trip-1")?.place, "Bangkok")
    expect("budget survives the round trip", store.trip("trip-1")?.budgetMinor, 2_500_050L)
    expect("remaining is arithmetic, not a column", store.trip("trip-1")?.remainingAgainstBudget(1_695_00L), 2_330_550L)
    // Clearing the fence is null, which is not the same as a budget of zero.
    store.updateTrip(withFence.copy(budgetMinor = null))
    expect("clearing the budget stores null", store.trip("trip-1")?.budgetMinor, null)

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

    // ── The upgrade path ────────────────────────────────────────────────────
    //
    // Everything above ran against a database created at the current version,
    // which is exactly the case that hid the "Open the trip" bug: a fresh
    // install worked and an existing one did not. So build a *v1* database by
    // hand — the schema as it shipped — put a trip in it, then migrate it and
    // prove the trip is still readable and that a new one can be written.
    println("migration from v1")
    val old = File(System.getProperty("java.io.tmpdir"), "ariadne-store-v1.db")
    old.delete()
    val oldDriver = JdbcSqliteDriver("jdbc:sqlite:${old.absolutePath}")
    // SqlDriver.execute prepares exactly one statement - a multi-statement
    // string silently runs only its first line. Hence one call per line.
    // The schema exactly as it shipped at version 1: no place, no budgetMinor on
    // trip, and everything else as Ariadne.sq has it today.
    val v1Schema = listOf(
        "CREATE TABLE trip (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, " +
            "startDate TEXT NOT NULL, endDate TEXT NOT NULL, baseCurrency TEXT NOT NULL, " +
            "homeCurrency TEXT NOT NULL, closedAt INTEGER)",
        "CREATE INDEX trip_start ON trip(startDate)",
        "CREATE TABLE capture (id TEXT NOT NULL PRIMARY KEY, " +
            "tripId TEXT NOT NULL REFERENCES trip(id) ON DELETE CASCADE, " +
            "localDate TEXT NOT NULL, photoPath TEXT NOT NULL, createdAt INTEGER NOT NULL, " +
            "state TEXT NOT NULL, amountMinor INTEGER, currency TEXT, merchant TEXT, " +
            "extractionConfidence REAL, confirmedAt INTEGER, " +
            "markedNoAmount INTEGER NOT NULL DEFAULT 0, category TEXT, note TEXT, " +
            "failedAttempts INTEGER NOT NULL DEFAULT 0, retryNotBefore INTEGER)",
        "CREATE INDEX capture_trip_day ON capture(tripId, localDate, createdAt)",
        "CREATE INDEX capture_state ON capture(state)",
        "CREATE INDEX capture_merchant ON capture(merchant)",
        "CREATE TABLE dayCard (id TEXT NOT NULL PRIMARY KEY, " +
            "tripId TEXT NOT NULL REFERENCES trip(id) ON DELETE CASCADE, " +
            "localDate TEXT NOT NULL, closedAt INTEGER, UNIQUE(tripId, localDate))",
        "CREATE TABLE rate (base TEXT NOT NULL, quote TEXT NOT NULL, " +
            "rateDate TEXT NOT NULL, rateScaled INTEGER NOT NULL, " +
            "PRIMARY KEY (base, quote, rateDate))",
        "CREATE TABLE settings (id INTEGER NOT NULL PRIMARY KEY CHECK (id = 1), " +
            "baseCurrency TEXT NOT NULL, homeCurrency TEXT NOT NULL, " +
            "theme TEXT NOT NULL, ocrProvider TEXT NOT NULL)",
        "PRAGMA user_version = 1",
    )
    v1Schema.forEach { oldDriver.execute(null, it, 0) }
    oldDriver.execute(
        null,
        "INSERT INTO trip (id, name, startDate, endDate, baseCurrency, homeCurrency) " +
            "VALUES ('trip-old', 'An older trip', '2026-11-01', '2026-11-05', 'INR', 'INR')",
        0,
    )
    expect("v1 database reports version 1", oldDriver.schemaVersion(), 1L)

    // Not a reimplementation: the exact function Main.kt calls on startup.
    com.ariadne.store.openOrMigrate(oldDriver)
    expect("migrated to the current version", oldDriver.schemaVersion(), AriadneDb.Schema.version.toLong())

    val oldStore = AriadneStore(oldDriver)
    val migrated = oldStore.trip("trip-old")
    expect("the old trip survived the migration", migrated?.name, "An older trip")
    expect("it starts without a place", migrated?.place, null)
    expect("it starts without a budget", migrated?.budgetMinor, null)

    // The bug itself: writing a trip with place and budget into a migrated
    // database. Before 1.sqm this threw and the button did nothing.
    oldStore.createTrip(
        Trip(
            id = "trip-new",
            name = "The one that would not open",
            startDate = LocalDay.of(2026, 12, 1),
            endDate = LocalDay.of(2026, 12, 6),
            baseCurrency = CurrencyCode.of("INR"),
            homeCurrency = CurrencyCode.of("INR"),
            place = "Bangkok",
            budgetMinor = 3_000_000L,
        ),
    )
    expect("a new trip writes into a migrated database", oldStore.trip("trip-new")?.name, "The one that would not open")
    expect("its place is there", oldStore.trip("trip-new")?.place, "Bangkok")
    expect("its fence is there", oldStore.trip("trip-new")?.budgetMinor, 3_000_000L)
    expect("it becomes the active trip", oldStore.activeTrip()?.id, "trip-new")
    // Opening again must be a no-op: this is the call that used to run `create`
    // and throw on every launch after the first.
    com.ariadne.store.openOrMigrate(oldDriver)
    expect("re-opening keeps the data", AriadneStore(oldDriver).trip("trip-new")?.place, "Bangkok")
    old.delete()

    file.delete()
    if (failures > 0) {
        println("\n$failures CHECK(S) FAILED")
        kotlin.system.exitProcess(1)
    }
    println("\nALL STORE CHECKS PASSED")
}
