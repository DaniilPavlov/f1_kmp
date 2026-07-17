package com.example.f1_kmp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.f1_kmp.ui.theme.AppStyles
import com.example.f1_kmp.ui.theme.F1Red
import com.example.f1_kmp.ui.theme.F1ShadowColor
import com.example.f1_kmp.ui.theme.F1White
import com.example.f1_kmp.util.DateUtils
import com.example.f1_kmp.util.YearMonth
import com.example.f1_kmp.viewmodel.DayLogo
import f1_kmp.composeapp.generated.resources.Res
import f1_kmp.composeapp.generated.resources.calendar_car
import f1_kmp.composeapp.generated.resources.calendar_finish
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.painterResource

private val weekdayLabels = listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс")

/**
 * Кастомный месячный календарь для расписания F1.
 *
 * Не используем Material DatePicker — нужны иконки на днях с сессиями/гонками.
 * Сетка строится вручную: считаем смещение первого дня месяца и заполняем ячейки.
 *
 * @param logoForDay колбэк из ViewModel: какую иконку показать на дате (или null)
 */
@Composable
fun F1Calendar(
    selectedDate: LocalDate,
    focusedMonth: YearMonth,
    userPickedDay: Boolean,
    logoForDay: (LocalDate) -> DayLogo?,
    onDaySelected: (LocalDate) -> Unit,
    onMonthChanged: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    val daysInMonth = focusedMonth.lengthOfMonth()
    val firstDayOfMonth = focusedMonth.atDay(1)
    // Смещение: календарь начинается с понедельника (ISO / kotlinx.datetime), не с воскресенья
    val startOffset = firstDayOfMonth.dayOfWeek.ordinal
    val todayDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(F1ShadowColor)
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onMonthChanged(focusedMonth.plusMonths(-1)) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
            }
            Text(
                text = "${DateUtils.monthName(focusedMonth.month)} ${focusedMonth.year}",
                style = AppStyles.body,
            )
            IconButton(onClick = { onMonthChanged(focusedMonth.plusMonths(1)) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            weekdayLabels.forEach { label ->
                Text(
                    text = label,
                    style = AppStyles.body,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        val totalCells = startOffset + daysInMonth
        val rows = (totalCells + 6) / 7
        var dayCounter = 1
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    if (cellIndex < startOffset || dayCounter > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val date = focusedMonth.atDay(dayCounter)
                        val today = date == todayDate
                        val selected = date == selectedDate && (!today || userPickedDay)
                        CalendarDay(
                            date = date,
                            selected = selected,
                            today = today,
                            logo = logoForDay(date),
                            onClick = { onDaySelected(date) },
                            modifier = Modifier.weight(1f),
                        )
                        dayCounter++
                    }
                }
            }
        }
    }
}

/**
 * Одна ячейка дня в календаре.
 * Сегодня — красный круг; выбранный день (в т.ч. сегодня после клика) — белый.
 */
@Composable
private fun CalendarDay(
    date: LocalDate,
    selected: Boolean,
    today: Boolean,
    logo: DayLogo?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = when {
        selected -> F1White
        today -> F1Red
        else -> Color.Transparent
    }
    val textStyle = when {
        selected -> AppStyles.body
        today -> AppStyles.body.copy(color = F1White)
        else -> AppStyles.body
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        when (logo) {
            DayLogo.Finish -> Image(
                painter = painterResource(Res.drawable.calendar_finish),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            DayLogo.Car -> Image(
                painter = painterResource(Res.drawable.calendar_car),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            null -> Text(text = date.dayOfMonth.toString(), style = textStyle)
        }
    }
}
