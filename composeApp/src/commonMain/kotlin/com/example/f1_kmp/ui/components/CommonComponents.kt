package com.example.f1_kmp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.f1_kmp.ui.theme.AppDimens
import com.example.f1_kmp.ui.theme.AppStyles
import com.example.f1_kmp.ui.theme.ConstructorColors
import com.example.f1_kmp.ui.theme.F1Black
import com.example.f1_kmp.ui.theme.F1Chrome
import com.example.f1_kmp.ui.theme.F1OnChrome
import com.example.f1_kmp.ui.theme.F1Red
import com.example.f1_kmp.ui.theme.appColors
import com.example.f1_kmp.ui.theme.F1StrokeGray
import com.example.f1_kmp.ui.theme.F1White
import f1_kmp.composeapp.generated.resources.Res
import f1_kmp.composeapp.generated.resources.app_logo
import f1_kmp.composeapp.generated.resources.back
import f1_kmp.composeapp.generated.resources.error_car
import f1_kmp.composeapp.generated.resources.no_connection
import f1_kmp.composeapp.generated.resources.no_connection_subtitle
import f1_kmp.composeapp.generated.resources.refresh
import androidx.compose.ui.text.style.TextOverflow
import f1_kmp.composeapp.generated.resources.share
import org.jetbrains.compose.resources.painterResource
import com.example.f1_kmp.domain.stringResource

/**
 * Верхняя панель приложения.
 *
 * Без параметров — логотип (на вкладках). С [title] и [onBack] — экран деталей со стрелкой назад.
 */
@Composable
fun F1AppBar(
    title: String? = null,
    onBack: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(F1Chrome)
            .statusBarsPadding()
            .padding(horizontal = AppDimens.horizontalPadding.dp)
            .padding(top = 8.dp, bottom = 14.dp)
            .heightIn(min = 48.dp),
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable(onClick = onBack)
                    .align(Alignment.CenterStart),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.back),
                    tint = F1OnChrome,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        if (title != null) {
            Text(
                text = title,
                style = AppStyles.body.copy(color = F1OnChrome),
                modifier = Modifier.align(Alignment.Center),
            )
            if (onShare != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable(onClick = onShare)
                        .align(Alignment.CenterEnd),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = stringResource(Res.string.share),
                        tint = F1OnChrome,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        } else {
            Image(
                painter = painterResource(Res.drawable.app_logo),
                contentDescription = null,
                modifier = Modifier
                    .height(28.dp)
                    .align(Alignment.Center),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

/** Центрированный индикатор загрузки в фирменном красном. */
@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = F1Red)
    }
}

/**
 * Экран/блок ошибки: иллюстрация, текст, кнопка повтора.
 *
 * GoF Behavioral Command — действие повтора инкапсулировано в [onRetry]
 * (обычно `viewModel::refreshAll`); вид не знает, что именно перезагружать.
 */
@Composable
fun ErrorBody(
    title: String?,
    subtitle: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppDimens.verticalPadding.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.error_car),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(180.dp),
            contentScale = ContentScale.FillWidth,
        )
        Text(
            text = title ?: stringResource(Res.string.no_connection),
            style = AppStyles.h2,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        )
        Text(
            text = subtitle ?: stringResource(Res.string.no_connection_subtitle),
            style = AppStyles.h3,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
        BlackButton(
            text = stringResource(Res.string.refresh),
            onClick = onRetry,
            modifier = Modifier.padding(horizontal = 50.dp),
        )
    }
}

/** Основная чёрная кнопка действия. [enabled] = false блокирует клик и меняет фон. */
@Composable
fun BlackButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) F1Black else F1StrokeGray)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = AppStyles.body.copy(color = F1White))
    }
}

/**
 * Переключатель из двух вкладок (пилоты/конструкторы, карта/список).
 * [activeValue] — 0 или 1; подчёркивание красное у активной вкладки.
 */
@Composable
fun CustomSwitcher(
    firstTitle: String,
    secondTitle: String,
    activeValue: Int,
    onChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        SwitcherTab(firstTitle, activeValue == 0) { onChanged(0) }
        SwitcherTab(secondTitle, activeValue == 1) { onChanged(1) }
    }
}

@Composable
private fun RowScope.SwitcherTab(title: String, active: Boolean, onClick: () -> Unit) {
    val colors = appColors()
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = AppStyles.h3.copy(color = if (active) F1Red else colors.pink),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(15.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(if (active) F1Red else colors.pink),
        )
    }
}

/**
 * Красная шапка таблицы — чистый Compose Canvas.
 *
 * @param weights относительные ширины колонок; `null` — равные.
 */
@Composable
fun TableHeaderRow(
    cells: List<String>,
    modifier: Modifier = Modifier,
    weights: List<Float>? = null,
) {
    val textMeasurer = rememberTextMeasurer()
    val captionStyle = AppStyles.caption.copy(color = F1White)
    val density = LocalDensity.current
    val heightDp = with(density) { (12.sp.toPx() * 2.2f).toDp() }
    // 4.dp как в F1TableHeaderView — иначе узкие колонки (W) съедаются padding и дают «…»
    val cellPaddingPx = with(density) { 4.dp.toPx() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp),
    ) {
        drawRect(color = F1Red)
        if (cells.isEmpty()) return@Canvas

        val columnWeights = weights ?: List(cells.size) { 1f }
        val totalWeight = columnWeights.sum().coerceAtLeast(0.01f)
        var xOffset = 0f
        cells.forEachIndexed { index, title ->
            val weight = columnWeights.getOrElse(index) { 1f }
            val columnWidth = size.width * (weight / totalWeight)
            val maxWidth = (columnWidth - cellPaddingPx * 2).toInt().coerceAtLeast(0)
            val layout = textMeasurer.measure(
                text = title,
                style = captionStyle,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
                constraints = Constraints(maxWidth = maxWidth),
            )
            // Центрируем как в ячейках TableDataRow (и как в оригинальном F1TableHeaderView).
            val textX = (xOffset + (columnWidth - layout.size.width) / 2f)
                .coerceAtLeast(xOffset + cellPaddingPx)
            val textY = (size.height - layout.size.height) / 2f
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(textX, textY),
            )
            xOffset += columnWidth
        }
    }
}

/** Ячейка строки таблицы. */
sealed interface TableCell {
    data class Text(val value: String, val color: Color? = null) : TableCell
    data class Flag(val countryOrNationality: String) : TableCell
    /** Место слева + имя (часто с `\n`) по центру колонки. */
    data class PlaceAndName(
        val place: String,
        val name: String,
        val placeColor: Color? = null,
    ) : TableCell
}

/**
 * Строка данных таблицы с зеброй.
 * [weights] — относительные ширины; `null` — равные колонки.
 * [constructorId] — опциональный акцент 3dp слева цветом команды.
 */
@Composable
fun TableDataRow(
    cells: List<TableCell>,
    index: Int,
    modifier: Modifier = Modifier,
    weights: List<Float>? = null,
    constructorId: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = appColors()
    val accent = constructorId?.takeIf { it.isNotBlank() }?.let(ConstructorColors::forConstructorId)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (index % 2 == 1) colors.grayBg else Color.Transparent)
            .drawBehind {
                if (accent != null) {
                    drawRect(
                        color = accent,
                        topLeft = Offset.Zero,
                        size = Size(3.dp.toPx(), size.height),
                    )
                }
            }
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        cells.forEachIndexed { cellIndex, cell ->
            val weight = weights?.getOrNull(cellIndex) ?: 1f
            Box(
                modifier = Modifier.weight(weight),
                contentAlignment = Alignment.Center,
            ) {
                when (cell) {
                    is TableCell.Text -> Text(
                        text = cell.value,
                        style = if (cell.color != null) {
                            AppStyles.caption.copy(color = cell.color)
                        } else {
                            AppStyles.caption
                        },
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    is TableCell.Flag -> CountryFlag(countryOrNationality = cell.countryOrNationality)
                    is TableCell.PlaceAndName -> PlaceAndNameContent(cell)
                }
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(F1StrokeGray),
    )
}

@Composable
private fun PlaceAndNameContent(cell: TableCell.PlaceAndName) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = cell.place,
            style = if (cell.placeColor != null) {
                AppStyles.caption.copy(color = cell.placeColor)
            } else {
                AppStyles.caption
            },
        )
        Text(
            text = cell.name,
            style = AppStyles.caption,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/** Удобный overload: все ячейки — строки; [flagCellIndices] → флаги. */
@Composable
fun TableDataRow(
    cells: List<String>,
    index: Int,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    flagCellIndices: Set<Int> = emptySet(),
    weights: List<Float>? = null,
    constructorId: String? = null,
    onClick: (() -> Unit)? = null,
) {
    TableDataRow(
        cells = cells.mapIndexed { i, value ->
            when {
                i in flagCellIndices -> TableCell.Flag(value)
                highlight && i > 0 -> TableCell.Text(value, color = F1Red)
                else -> TableCell.Text(value)
            }
        },
        index = index,
        modifier = modifier,
        weights = weights,
        constructorId = constructorId,
        onClick = onClick,
    )
}

/** Кликабельный текст со подчёркиванием (ссылка на Wikipedia). */
@Composable
fun LinkText(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = AppStyles.body.copy(textDecoration = TextDecoration.Underline),
        modifier = Modifier.clickable(onClick = onClick),
    )
}
