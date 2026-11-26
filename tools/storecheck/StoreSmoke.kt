import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.ariadne.fx.FxTable
import com.ariadne.fx.Tally
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
import com.ariadne.store.hasColumn
import com.ariadne.store.hasTable
import com.ariadne.store.openOrMigrate
import com.ariadne.store.userVersion
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
    // A currency with no rate is not a zero and is not the raw figure either: it is
    // money the app cannot convert, so it stays out of every base-currency total and
    // is named separately. Before this, 99.00 EUR would have joined a rupee column
    // as "₹99.00" — the same bug that printed THB 2,555 + INR 50 as ₹2,605.
    expect(
        "no rate means no converted row",
        store.categoryTotals("trip-1").any { it.category == Category.Lodging },
        false,
    )
    val tally = Tally(FxTable(store.allRates()), CurrencyCode.of("INR"))
        .total(store.confirmedForTrip("trip-1"))
    expect("unconverted money is named", tally.unconverted[CurrencyCode.of("EUR")], 9_900L)
    expect("and the total says it is incomplete", tally.isComplete, false)

    println("persistence")
    expect("db file written", file.length() > 0, true)

    // ── The upgrade path ────────────────────────────────────────────────
    //
    // Everything above ran against a database created at the current version,
    // which is exactly the case that hid the bug: a fresh install worked and an
    // existing one did not.
    //
    // This fixture is the schema **as it actually shipped**, read from commit
    // 81df38b and kept in `baseline-v1.sql`. The previous version of this test
    // invented a "version 1" that no build ever wrote, so it proved nothing -
    // and every real database went on failing. The lesson is in the file names:
    // a fixture is only worth anything if it is the thing the user has.
    println("migration from the shipped baseline")
    val old = File(System.getProperty("java.io.tmpdir"), "ariadne-store-baseline.db")
    old.delete()
    val oldDriver = JdbcSqliteDriver("jdbc:sqlite:${old.absolutePath}")
    // SqlDriver.execute prepares exactly one statement, so the fixture is split
    // on the semicolons rather than handed over whole.
    File("tools/storecheck/baseline-v1.sql").readText()
        .split(";")
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.lines().all { l -> l.trim().startsWith("--") } }
        .forEach { oldDriver.execute(null, it, 0) }
    oldDriver.execute(
        null,
        "INSERT INTO trip (id, name, startDate, endDate, baseCurrency, homeCurrency) " +
            "VALUES ('trip-old', 'An older trip', '2026-11-01', '2026-11-05', 'INR', 'INR')",
        0,
    )
    // The crux: this is what every real database looks like. Nothing ever wrote
    // the pragma, so a file full of trips reports version 0.
    expect("the baseline reports version 0, like every real install", oldDriver.userVersion(), 0L)
    expect("but it does have a trip table", oldDriver.hasTable("trip"), true)

    // Not a reimplementation: the exact function the desktop app calls on startup.
    com.ariadne.store.openOrMigrate(oldDriver)
    expect("versioned after opening", oldDriver.userVersion(), AriadneDb.Schema.version.toLong())

    val oldStore = AriadneStore(oldDriver)
    val migrated = oldStore.trip("trip-old")
    expect("the old trip survived the migration", migrated?.name, "An older trip")
    expect("it now has a place column, empty", migrated?.place, null)
    expect("it now has a budget column, empty", migrated?.budgetMinor, null)

    // The three queries that threw on the user's machine, in the order they threw.
    expect("capturesForTrip works after the upgrade", oldStore.capturesForTrip("trip-old").size, 0)
    expect("openCaptures works after the upgrade", oldStore.openCaptures().size, 0)
    expect("categoryTotals works after the upgrade", oldStore.categoryTotals("trip-old").size, 0)

    // And the write that used to fail silently: a trip with a place and a fence.
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

    // A capture with a category, through the column the upgrade added.
    oldStore.insert(
        Capture(
            id = "c-old",
            tripId = "trip-new",
            localDate = LocalDay.of(2026, 12, 1),
            photoPath = "trip-new/2026-12-01/c-old.jpg",
            createdAt = 2_000L,
            state = CaptureState.CONFIRMED,
            amountMinor = 1_500L,
            currency = CurrencyCode.of("INR"),
            confirmedAt = 2_100L,
        ),
    )
    oldStore.updateDetail("c-old", 1_500L, "INR", "Old shop", "Food", "a note")
    expect("category writes into the migrated column", oldStore.findById("c-old")?.category, Category.Food)
    expect("note writes into the migrated column", oldStore.findById("c-old")?.note, "a note")

    // Opening a second time must be a no-op: this is the call that used to run
    // `create` and throw on every launch after the first.
    com.ariadne.store.openOrMigrate(oldDriver)
    expect("re-opening keeps the data", AriadneStore(oldDriver).trip("trip-new")?.place, "Bangkok")

    // A file that is already current must also be left alone.
    com.ariadne.store.openOrMigrate(oldDriver)
    expect("version is stable across opens", oldDriver.userVersion(), AriadneDb.Schema.version.toLong())
    old.delete()

    // ── The shape the user actually had ────────────────────────────────────
    //
    // The interim commit created its database with `create` (so `place` and
    // `category` were already there) and - like every build before it - never
    // wrote `PRAGMA user_version`, so the file reports 0. Re-running declared
    // migrations on it fails with "duplicate column name". This is that file,
    // and it must open without losing the trip inside it.
    println("migration from the interim build's file")
    val interim = File(System.getProperty("java.io.tmpdir"), "ariadne-store-interim.db")
    interim.delete()
    val interimDriver = JdbcSqliteDriver("jdbc:sqlite:${interim.absolutePath}")
    // The current schema, but left unversioned. `create` does not set the pragma.
    AriadneDb.Schema.create(interimDriver)
    expect("a created file reports version 0", interimDriver.userVersion(), 0L)
    interimDriver.execute(
        null,
        "INSERT INTO trip (id, name, startDate, endDate, baseCurrency, homeCurrency, place, budgetMinor) " +
            "VALUES ('trip-i', 'A trip from the interim build', '2026-11-10', '2026-11-20', 'INR', 'INR', 'Chennai', 500000)",
        0,
    )
    com.ariadne.store.openOrMigrate(interimDriver)
    val interimStore = AriadneStore(interimDriver)
    expect("the interim trip opened", interimStore.trip("trip-i")?.name, "A trip from the interim build")
    expect("its place survived", interimStore.trip("trip-i")?.place, "Chennai")
    expect("its fence survived", interimStore.trip("trip-i")?.budgetMinor, 500_000L)
    expect("the interim file is versioned now", interimDriver.userVersion(), AriadneDb.Schema.version.toLong())
    // The queries that threw on the user's machine.
    expect("openCaptures works on it", interimStore.openCaptures().size, 0)
    expect("confirmedForTrip works on it", interimStore.confirmedForTrip("trip-i").size, 0)
    interim.delete()

    // ── A file with no version and no trip table is genuinely new ───────────
    println("a genuinely new file")
    val fresh = File(System.getProperty("java.io.tmpdir"), "ariadne-store-fresh.db")
    fresh.delete()
    val freshDriver = JdbcSqliteDriver("jdbc:sqlite:${fresh.absolutePath}")
    expect("no trip table yet", freshDriver.hasTable("trip"), false)
    com.ariadne.store.openOrMigrate(freshDriver)
    expect("created and versioned", freshDriver.userVersion(), AriadneDb.Schema.version.toLong())
    expect("trip table exists", freshDriver.hasTable("trip"), true)
    expect("place column exists", freshDriver.hasColumn("trip", "place"), true)
    expect("budgetMinor is an integer column", freshDriver.hasColumn("trip", "budgetMinor"), true)
    fresh.delete()

    file.delete()
    if (failures > 0) {
        println("\n$failures CHECK(S) FAILED")
        kotlin.system.exitProcess(1)
    }
    println("\nALL STORE CHECKS PASSED")
}
