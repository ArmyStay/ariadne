package com.ariadne.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * The photographic plate.
 *
 * When a real bitmap exists the caller draws it here. When it does not — the demo
 * roll, a preview, or a capture whose photo is still being written — the plate
 * shows a single engraved line study of what was photographed. One subject, one
 * weight of line, generous margin: the same restraint a printed page uses for a
 * spot illustration. It is deliberately not a photo-effect and never pretends to
 * be one.
 */
public enum class Subject {
    CUP, SHOE, BAG, BOWL, TICKET, UNKNOWN;

    public companion object {
        /** Merchant text to subject, so the plate always matches the record. */
        public fun forMerchant(merchant: String?): Subject {
            val m = merchant?.lowercase() ?: return UNKNOWN
            return when {
                "chai" in m || "coffee" in m || "cafe" in m -> CUP
                "shoe" in m || "bata" in m || "foot" in m -> SHOE
                "kiosk" in m || "market" in m && "night" !in m -> BAG
                "stall" in m || "food" in m || "warung" in m -> BOWL
                "museum" in m || "ticket" in m || "gallery" in m -> TICKET
                else -> UNKNOWN
            }
        }
    }
}

@Composable
public fun Plate(
    subject: Subject,
    paper: Paper,
    modifier: Modifier = Modifier,
    caption: String? = null,
) {
    Box(modifier) {
        // Ruled ground, very faint: the paper the drawing sits on.
        Canvas(Modifier.fillMaxSize()) {
            var y = 26f
            while (y < size.height) {
                drawLine(
                    color = paper.edge.copy(alpha = 0.35f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 0.5f,
                )
                y += 26f
            }
            drawStudy(subject, paper.inkSoft, size.minDimension * 0.006f)
        }
        if (caption != null) {
            Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.BottomStart) {
                Label(caption, paper, color = paper.inkFaint)
            }
        }
    }
}

/**
 * One line study per subject, drawn in normalised units so it scales from a
 * 54dp receipt thumbnail to a full-page plate without redrawing.
 */
private fun DrawScope.drawStudy(subject: Subject, ink: Color, weight: Float) {
    val w = size.width
    val h = size.height
    val stroke = Stroke(width = weight.coerceAtLeast(1f))
    // Cap the study so a full-page plate holds a spot illustration rather than a
    // blown-up drawing: the line weight stays the same at every size.
    val u = minOf(w, h).coerceAtMost(300f)
    val cx = w / 2
    val cy = h / 2

    fun x(f: Float) = cx + (f - 0.5f) * u
    fun y(f: Float) = cy + (f - 0.5f) * u

    when (subject) {
        Subject.CUP -> {
            // A chai glass on a saucer, two steam curls.
            val body = Path().apply {
                moveTo(x(0.38f), y(0.44f)); lineTo(x(0.42f), y(0.72f))
                lineTo(x(0.58f), y(0.72f)); lineTo(x(0.62f), y(0.44f)); close()
            }
            drawPath(body, ink, style = stroke)
            drawLine(ink, Offset(x(0.34f), y(0.75f)), Offset(x(0.66f), y(0.75f)), strokeWidth = weight)
            drawPath(
                Path().apply {
                    moveTo(x(0.455f), y(0.38f))
                    cubicTo(x(0.42f), y(0.32f), x(0.50f), y(0.30f), x(0.465f), y(0.24f))
                },
                ink, style = stroke,
            )
            drawPath(
                Path().apply {
                    moveTo(x(0.545f), y(0.38f))
                    cubicTo(x(0.58f), y(0.31f), x(0.50f), y(0.30f), x(0.535f), y(0.23f))
                },
                ink, style = stroke,
            )
        }

        Subject.SHOE -> {
            val body = Path().apply {
                moveTo(x(0.20f), y(0.62f))
                cubicTo(x(0.24f), y(0.44f), x(0.40f), y(0.41f), x(0.51f), y(0.45f))
                cubicTo(x(0.63f), y(0.49f), x(0.75f), y(0.51f), x(0.81f), y(0.58f))
                lineTo(x(0.82f), y(0.63f)); close()
            }
            drawPath(body, ink, style = stroke)
            drawLine(ink, Offset(x(0.18f), y(0.65f)), Offset(x(0.84f), y(0.65f)), strokeWidth = weight)
            for (i in 0..2) {
                val lx = x(0.39f + i * 0.05f)
                drawLine(ink, Offset(lx, y(0.47f)), Offset(lx + u * 0.04f, y(0.51f)), strokeWidth = weight * 0.8f)
            }
        }

        Subject.BAG -> {
            drawRect(
                color = ink,
                topLeft = Offset(x(0.35f), y(0.40f)),
                size = Size(x(0.65f) - x(0.35f), y(0.72f) - y(0.40f)),
                style = stroke,
            )
            drawLine(ink, Offset(x(0.35f), y(0.48f)), Offset(x(0.65f), y(0.48f)), strokeWidth = weight * 0.8f)
            drawPath(
                Path().apply {
                    moveTo(x(0.42f), y(0.40f))
                    cubicTo(x(0.44f), y(0.30f), x(0.56f), y(0.30f), x(0.58f), y(0.40f))
                },
                ink, style = stroke,
            )
        }

        Subject.BOWL -> {
            drawPath(
                Path().apply {
                    moveTo(x(0.28f), y(0.50f))
                    cubicTo(x(0.32f), y(0.72f), x(0.68f), y(0.72f), x(0.72f), y(0.50f))
                    close()
                },
                ink, style = stroke,
            )
            drawLine(ink, Offset(x(0.26f), y(0.50f)), Offset(x(0.74f), y(0.50f)), strokeWidth = weight)
            drawLine(ink, Offset(x(0.36f), y(0.30f)), Offset(x(0.70f), y(0.45f)), strokeWidth = weight * 0.85f)
            drawLine(ink, Offset(x(0.32f), y(0.34f)), Offset(x(0.66f), y(0.49f)), strokeWidth = weight * 0.85f)
        }

        Subject.TICKET -> {
            drawRect(
                color = ink,
                topLeft = Offset(x(0.20f), y(0.42f)),
                size = Size(x(0.80f) - x(0.20f), y(0.62f) - y(0.42f)),
                style = stroke,
            )
            drawLine(ink, Offset(x(0.66f), y(0.42f)), Offset(x(0.66f), y(0.62f)), strokeWidth = weight * 0.8f)
            for (i in 0..2) {
                val ly = y(0.47f + i * 0.05f)
                drawLine(ink, Offset(x(0.25f), ly), Offset(x(0.46f), ly), strokeWidth = weight * 0.6f)
            }
            drawCircle(ink, radius = u * 0.012f, center = Offset(x(0.73f), y(0.52f)), style = stroke)
        }

        Subject.UNKNOWN -> {
            // A blank plate: the crosshair a camera registers on.
            drawLine(ink, Offset(cx - u * 0.03f, cy), Offset(cx + u * 0.03f, cy), strokeWidth = weight)
            drawLine(ink, Offset(cx, cy - u * 0.03f), Offset(cx, cy + u * 0.03f), strokeWidth = weight)
            drawRect(
                color = ink.copy(alpha = 0.4f),
                topLeft = Offset(cx - u * 0.18f, cy - u * 0.18f),
                size = Size(u * 0.36f, u * 0.36f),
                style = Stroke(width = weight * 0.7f),
            )
        }
    }
}
