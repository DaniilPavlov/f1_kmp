package com.example.f1_kmp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import f1_kmp.composeapp.generated.resources.Res
import f1_kmp.composeapp.generated.resources.helvetica_neue_cyr_bold
import f1_kmp.composeapp.generated.resources.inter_regular
import org.jetbrains.compose.resources.Font

/**
 * Брендовые цвета F1.
 *
 * В Compose берём Color напрямую — отдельный colors.xml / XML-тема не нужны.
 */
val F1Black = Color(0xFF333333)
val F1Red = Color(0xFFE1271E)
val F1Pink = Color(0xFFF3B2AE)
val F1White = Color(0xFFFFFFFF)
val F1GrayBg = Color(0xFFF6F6F6)
val F1StrokeGray = Color(0xFFD8D8D8)
val F1ShadowColor = Color(0xFFD7D7D7)

/**
 * FontFamily из composeResources.
 * В KMP шрифты лежат в commonMain/composeResources/font, не в res/font.
 * [Font] — composable, поэтому семейства собираем внутри @Composable.
 */
@Composable
private fun helveticaBoldFamily(): FontFamily =
    FontFamily(Font(Res.font.helvetica_neue_cyr_bold, weight = FontWeight.Bold))

@Composable
private fun interRegularFamily(): FontFamily =
    FontFamily(Font(Res.font.inter_regular, weight = FontWeight.Normal))

/**
 * Типографика приложения
 *
 * Экраны берут готовые стили (h1/h2/body…), чтобы не копировать fontSize/color.
 * Геттеры — @Composable, потому что [Font] требует composition context.
 */
object AppStyles {
    val h1: TextStyle
        @Composable get() = TextStyle(fontFamily = helveticaBoldFamily(), fontSize = 34.sp, color = Color.Black)

    val h2: TextStyle
        @Composable get() = TextStyle(fontFamily = helveticaBoldFamily(), fontSize = 30.sp, color = Color.Black)

    val h3: TextStyle
        @Composable get() = TextStyle(fontFamily = helveticaBoldFamily(), fontSize = 25.sp, color = Color.Black)

    val body: TextStyle
        @Composable get() = TextStyle(
            fontFamily = interRegularFamily(),
            fontSize = 16.sp,
            lineHeight = 20.sp,
            color = Color.Black,
        )

    val caption: TextStyle
        @Composable get() = TextStyle(
            fontFamily = interRegularFamily(),
            fontSize = 12.sp,
            lineHeight = 14.sp,
            color = Color.Black,
        )

    val navBar: TextStyle
        @Composable get() = TextStyle(
            fontFamily = interRegularFamily(),
            fontSize = 10.sp,
            lineHeight = 12.sp,
            color = Color.Black,
        )
}

/**
 * Экраны используют эти константы, чтобы поля слева/справа совпадали.
 */
object AppDimens {
    const val horizontalPadding = 12f
    const val verticalPadding = 20f
}
