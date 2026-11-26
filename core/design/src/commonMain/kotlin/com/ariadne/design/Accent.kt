package com.ariadne.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember

/**
 * One blue action per surface — the rule the whole palette rests on — enforced
 * where it is drawn rather than remembered by the person writing the page.
 *
 * The rule cannot be a type in Compose without turning every page into a DSL:
 * `Action(accent = true)` is an ordinary composable call, so a second one is
 * perfectly legal Kotlin. What this file does instead is make it *observable*:
 * the shell hands every destination a budget, a blue action claims it once, and a
 * second claim from the same surface is recorded as a violation.
 *
 * [AccentAudit] keeps those violations. The stills harness renders every page and
 * fails if the list is not empty, so a page with two blues cannot ship quietly —
 * which is the property a type would have given us for free, obtained without
 * rewriting the twelve pages that already call [Action].
 *
 * The budget is keyed by route: during a page turn both pages are briefly
 * composed, and the outgoing page's blue must not be mistaken for a second blue
 * on the incoming one.
 */
public class AccentBudget internal constructor() {

    private val claimed = mutableMapOf<Any, String>()

    /** Which action holds the blue on a route, or null if the surface has none. */
    public fun holder(route: Any): String? = claimed[route]

    internal fun claim(route: Any, text: String) {
        val first = claimed[route]
        when {
            first == null -> claimed[route] = text
            first == text -> Unit // the same action recomposed, not a second one
            else -> AccentAudit.record(route, first, text)
        }
    }
}

/**
 * One budget per surface. The shell calls this per destination, keyed by route,
 * so leaving a page and coming back starts a clean count.
 */
@Composable
public fun rememberAccentBudget(key: Any? = Unit): AccentBudget = remember(key) { AccentBudget() }

/** Where the shell provides a budget, and which surface it is for. */
public val LocalAccentBudget: ProvidableCompositionLocal<AccentBudget?> =
    compositionLocalOf { null }

/** The identity of the surface being composed, used to key the budget. */
public val LocalSurfaceId: ProvidableCompositionLocal<Any?> =
    compositionLocalOf { null }

/**
 * The audit: every surface that claimed the blue twice, in the order it happened.
 *
 * Empty in a correct build. Read by the stills harness (and by anyone who wants
 * to check) rather than by the product: an app that stops working because two
 * buttons were blue would be a worse app, so this never throws.
 */
public object AccentAudit {

    private val entries = mutableListOf<String>()

    /** One line per violation: the surface, the action that held the blue, and the second. */
    public val violations: List<String> get() = entries.toList()

    public fun clear() {
        entries.clear()
    }

    internal fun record(route: Any, first: String, second: String) {
        entries += "$route: \"$first\" then \"$second\""
    }
}

/**
 * Claims the surface's blue for [text]. Called from [Action] when it is accented
 * and enabled; a disabled action is drawn on sunk paper and is not the blue one.
 *
 * `remember` on the text is deliberate: recomposition must not look like a second
 * claim, and a page that changes which action is blue is a page that has changed
 * its mind.
 */
@Composable
internal fun ClaimAccent(text: String) {
    val budget = LocalAccentBudget.current ?: return
    val surface = LocalSurfaceId.current ?: return
    // `remember` is what makes the claim idempotent: a recomposition is the same
    // action being drawn again, not a second action claiming the colour.
    remember(budget, surface, text) { budget.claim(surface, text) }
}
