package com.ariadne.app.ui.pages

/** Width policies for the receipt, settings, and stats screens. Values are in dp. */
internal object PicturedPageLayout {
    /** Prefer wrapping controls to clipping text as either the window narrows or type grows. */
    fun stack(width: Float, minimumWidth: Float, fontScale: Float): Boolean =
        width < minimumWidth * fontScale.coerceAtLeast(1f)

    /** Number of day slots between labels, allowing room for scaled two-digit labels. */
    fun chartLabelStride(days: Int, width: Float, fontScale: Float): Int {
        if (days <= 0) return 1
        val labelWidth = 28f * fontScale.coerceAtLeast(1f)
        val capacity = (width.coerceAtLeast(0f) / labelWidth).toInt().coerceAtLeast(1)
        return ((days - 1) / capacity) + 1
    }
}
