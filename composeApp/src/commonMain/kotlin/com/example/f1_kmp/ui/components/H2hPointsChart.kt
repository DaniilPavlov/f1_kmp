package com.example.f1_kmp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.f1_kmp.ui.theme.appColors
import com.example.f1_kmp.viewmodel.H2hPointsTimeline
import com.example.f1_kmp.viewmodel.H2hTimelinePoint
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.pow

/** Canvas-график очков H2H по раундам (две серии + ось). */
@Composable
fun H2hPointsChart(
    timeline: H2hPointsTimeline,
    colorA: Color,
    colorB: Color,
    modifier: Modifier = Modifier,
    height: Float = 220f,
) {
    val colors = appColors()
    val axisColor = colors.textGray
    val gridColor = colors.strokeGray.copy(alpha = 0.5f)
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, color = colors.textGray)
    val xLabelStyle = TextStyle(fontSize = 9.sp, color = colors.textGray)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp),
    ) {
        val points = timeline.points
        if (points.isEmpty()) return@Canvas

        val leftPad = 36.dp.toPx()
        val rightPad = 8.dp.toPx()
        val topPad = 12.dp.toPx()
        val bottomPad = 28.dp.toPx()
        val chartLeft = leftPad
        val chartTop = topPad
        val chartRight = size.width - rightPad
        val chartBottom = size.height - bottomPad
        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop
        if (chartWidth <= 0f || chartHeight <= 0f) return@Canvas

        val niceMax = niceCeil(max(timeline.maxCumulative, 1.0)).toFloat()
        val yTicks = 4

        for (i in 0..yTicks) {
            val t = i / yTicks.toFloat()
            val y = chartBottom - chartHeight * t
            drawLine(gridColor, Offset(chartLeft, y), Offset(chartRight, y), strokeWidth = 1f)
            val label = formatPoints(niceMax * t)
            val measured = textMeasurer.measure(label, style = labelStyle)
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    chartLeft - measured.size.width - 4.dp.toPx(),
                    y - measured.size.height / 2f,
                ),
            )
        }
        drawLine(axisColor, Offset(chartLeft, chartBottom), Offset(chartRight, chartBottom), strokeWidth = 1f)

        fun pointAt(index: Int, cumulative: Double): Offset {
            val x = if (points.size == 1) {
                chartLeft + chartWidth / 2f
            } else {
                chartLeft + chartWidth * (index / (points.size - 1).toFloat())
            }
            val y = chartBottom - chartHeight * (cumulative.toFloat() / niceMax)
            return Offset(x, y)
        }

        val stroke = Stroke(
            width = 2.5.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        val dotRadius = if (points.size > 30) 1.5.dp.toPx() else 3.dp.toPx()

        fun drawSeries(color: Color, values: (H2hTimelinePoint) -> Double) {
            val path = Path()
            points.forEachIndexed { index, p ->
                val pt = pointAt(index, values(p))
                if (index == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }
            drawPath(path, color, style = stroke)
            points.forEachIndexed { index, p ->
                drawCircle(color, radius = dotRadius, center = pointAt(index, values(p)))
            }
        }

        drawSeries(colorA) { it.cumulativeA }
        drawSeries(colorB) { it.cumulativeB }

        val careerYears = points.first().label == points.first().season
        val labelIndices = if (careerYears) {
            val seasonStarts = points.mapIndexedNotNull { index, p ->
                if (index == 0 || points[index - 1].season != p.season) index else null
            }
            val step = labelStep(seasonStarts.size)
            seasonStarts.filterIndexed { i, _ -> i % step == 0 }
        } else {
            val step = labelStep(points.size)
            (0 until points.size step step).toList()
        }

        labelIndices.forEach { index ->
            val x = pointAt(index, 0.0).x
            val measured = textMeasurer.measure(points[index].label, style = xLabelStyle)
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    x - measured.size.width / 2f,
                    size.height - measured.size.height - 2.dp.toPx(),
                ),
            )
        }
    }
}

/** Same spacing rules as Flutter `_H2hPointsChartPainter._labelStep`. */
internal fun labelStep(count: Int): Int = when {
    count <= 8 -> 1
    count <= 16 -> 2
    count <= 30 -> 3
    else -> max(1, count / 8)
}

private fun niceCeil(value: Double): Double {
    if (value <= 0) return 1.0
    val exp = kotlin.math.floor(kotlin.math.log10(value))
    val base = 10.0.pow(exp)
    val fraction = value / base
    val nice = when {
        fraction <= 1.0 -> 1.0
        fraction <= 2.0 -> 2.0
        fraction <= 5.0 -> 5.0
        else -> 10.0
    }
    return nice * base
}

private fun formatPoints(value: Float): String =
    if (value >= 100) {
        ceil(value.toDouble()).toInt().toString()
    } else if (value == value.toInt().toFloat()) {
        value.toInt().toString()
    } else {
        value.toInt().toString()
    }
