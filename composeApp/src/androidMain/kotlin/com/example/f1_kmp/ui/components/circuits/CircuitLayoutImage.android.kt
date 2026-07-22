package com.example.f1_kmp.ui.components.circuits

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

internal actual fun decodeCircuitLayoutBitmap(bytes: ByteArray): ImageBitmap {
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        ?: error("Failed to decode circuit layout image")
    return bitmap.asImageBitmap()
}
