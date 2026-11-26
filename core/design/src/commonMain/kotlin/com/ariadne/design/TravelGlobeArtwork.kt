package com.ariadne.design

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import ariadne.core.design.generated.resources.Res
import ariadne.core.design.generated.resources.home_globe
import org.jetbrains.compose.resources.painterResource

@Composable
public fun TravelGlobeArtwork(
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    opacity: Float = 1f,
) {
    Image(
        painter = painterResource(Res.drawable.home_globe),
        contentDescription = null,
        modifier = modifier.alpha(opacity),
        contentScale = contentScale,
        alignment = alignment,
    )
}
