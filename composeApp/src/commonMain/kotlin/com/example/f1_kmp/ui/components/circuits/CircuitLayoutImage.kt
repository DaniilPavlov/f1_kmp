package com.example.f1_kmp.ui.components.circuits

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.f1_kmp.data.circuits.CircuitLayoutAssets
import com.example.f1_kmp.ui.theme.F1Black
import f1_kmp.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Локальная схема трассы; при отсутствии ассета — ничего не рисует. */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun CircuitLayoutImage(
    circuitId: String,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    tint: Color = F1Black,
    padding: Dp = 12.dp,
) {
    val path = CircuitLayoutAssets.assetPath(circuitId) ?: return
    val bitmap by produceState<ImageBitmap?>(initialValue = null, path) {
        value = runCatching {
            decodeCircuitLayoutBitmap(Res.readBytes(path))
        }.getOrNull()
    }

    val loaded = bitmap ?: return
    CircuitLayoutImageContent(loaded, modifier, height, tint, padding)
}

internal expect fun decodeCircuitLayoutBitmap(bytes: ByteArray): ImageBitmap

@Composable
internal fun CircuitLayoutImageContent(
    bitmap: ImageBitmap,
    modifier: Modifier,
    height: Dp,
    tint: Color,
    padding: Dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .padding(padding),
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(height),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(tint),
        )
    }
}
