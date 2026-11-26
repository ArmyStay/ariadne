package com.ariadne.model

/**
 * What a photograph is called, and what a shop sign is not.
 *
 * The bug this file fixes shipped a card titled "ction View Go Run Terminal Help" —
 * the menu bar of a code editor, photographed by accident, run through OCR, and
 * accepted as the first line that "reads like a name". It does read like a name:
 * that is exactly why a blocklist of receipt words was not enough.
 *
 * Two rules, in one place because both the parser and the UI need them:
 *
 *  - [isUiChrome] recognises the vocabulary of *screens* — menus, window controls,
 *    status bars — as opposed to the vocabulary of shops. Two or more of those
 *    words on one line is chrome; no shop is called "View Go Run".
 *  - [receiptTitle] never invents a name. With a believable merchant it uses it;
 *    without one it says what it does know, which is that this is a receipt and
 *    when it was taken: "Receipt · 21:09".
 */
public object MerchantName {

    /**
     * Words from application menus and phone status bars.
     *
     * Deliberately does not include everyday words a shop might use ("Shop",
     * "Store", "Cafe", "Bar", "Home", "Open"). A single one of these on a receipt is
     * not evidence of anything; [isUiChrome] needs two.
     */
    private val CHROME_WORDS: Set<String> = setOf(
        "view", "go", "run", "terminal", "help", "edit", "file", "window", "debug",
        "insert", "selection", "format", "tools", "explorer", "navigate", "refactor",
        "build", "command", "palette", "extensions", "bookmarks", "output", "problems",
        "settings", "zoom", "undo", "redo", "toggle", "sidebar", "panel", "menu",
        "minimise", "minimize", "maximize", "fullscreen", "battery", "wifi",
        "bluetooth", "airplane", "notification", "notifications", "screenshot",
        "debugger", "breakpoint", "workspace", "preferences", "toolbar", "toolbar",
        "close", "save", "print", "share", "search", "back", "next", "refresh",
    )

    /** Words so generic they can only carry a title as part of a longer name. */
    private val EMPTY_NAMES: Set<String> = setOf(
        "receipt", "invoice", "tax invoice", "bill", "cash memo", "duplicate",
        "customer copy", "order", "total", "amount", "untitled", "image", "photo",
        "photograph", "scan", "document",
    )

    /**
     * True when [line] reads like a menu bar or a status bar rather than a shopfront.
     *
     * Counts distinct chrome words: one is a coincidence ("Food View Restaurant"),
     * two on one line is a screen.
     */
    public fun isUiChrome(line: String): Boolean {
        val words = wordsOf(line)
        val hits = words.count { it in CHROME_WORDS }
        if (hits >= 2) return true
        // A line of nothing but chrome words, however it is spaced.
        return words.isNotEmpty() && words.all { it in CHROME_WORDS }
    }

    /**
     * The merchant name in [raw], or null when the text cannot carry one.
     *
     * Returns null for chrome, for a line that is only a receipt word ("RECEIPT"),
     * for a line with no letters, and for a line with more digits than letters.
     */
    public fun clean(raw: String?): String? {
        val line = raw?.trim()?.replace(Regex("\\s+"), " ") ?: return null
        if (line.length < 3 || line.length > 48) return null
        if (line.none { it.isLetter() }) return null
        if (isUiChrome(line)) return null
        val trimmed = line.trimStart('-', '*', '#', '.', '|', '_').trim()
        if (trimmed.length < 3) return null
        val lower = trimmed.lowercase()
        if (EMPTY_NAMES.any { lower == it }) return null
        // "sh 45 12 9981" is a receipt number, not a shop.
        val digits = trimmed.count { it.isDigit() }
        val letters = trimmed.count { it.isLetter() }
        if (digits > letters) return null
        return trimmed.take(48)
    }

    /**
     * The title a card is filed under.
     *
     * A merchant that survives [clean], otherwise "Receipt · 21:09" — a true and
     * short statement — and never a raw OCR line, and never the word "Untitled".
     * [time] is formatted by the caller because only it knows the device's zone.
     */
    public fun receiptTitle(merchant: String?, time: String? = null): String {
        clean(merchant)?.let { return it }
        return if (time.isNullOrBlank()) "Receipt" else "Receipt · $time"
    }

    private fun wordsOf(line: String): List<String> {
        // Split on anything that is not a letter or a digit, without a regex
        // character class: this module is compiled for JVM, Android, iOS and Wasm,
        // and `\\p{L}` does not mean the same thing on all four.
        val words = mutableListOf<String>()
        val current = StringBuilder()
        for (character in line.lowercase()) {
            if (character.isLetterOrDigit()) {
                current.append(character)
            } else if (current.isNotEmpty()) {
                words += current.toString()
                current.clear()
            }
        }
        if (current.isNotEmpty()) words += current.toString()
        return words
    }
}
