package com.ariadne.app.ui.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.ariadne.design.Paper
import com.ariadne.design.Plate
import com.ariadne.design.Subject

public interface PhotoImageRenderer {
    @Composable
    public fun Render(photoPath: String, modifier: Modifier)
}

public val LocalPhotoImageRenderer = staticCompositionLocalOf<PhotoImageRenderer?> { null }

@Composable
public fun PhotoPreview(
    photoPath: String,
    caption: String,
    paper: Paper,
    modifier: Modifier = Modifier,
) {
    val renderer = LocalPhotoImageRenderer.current
    when {
        photoPath.isEmpty() -> PhotographPlaceholder(paper, "Manual expense · no photograph", modifier)
        photoPath.startsWith("demo:") -> Plate(
            subject = Subject.forMerchant(caption),
            paper = paper,
            modifier = modifier,
            caption = photoPath.removePrefix("demo:"),
        )
        renderer == null -> PhotographPlaceholder(paper, caption, modifier)
        else -> {
            PhotographPlaceholder(paper, caption, modifier)
            renderer.Render(photoPath, modifier)
        }
    }
}
