package com.example.f1_kmp.viewmodel

/**
 * Какая иконка нарисовать под ячейкой дня в календаре.
 *
 * [ScheduleViewModel.logoForDay] выбирает значение; экран мапит его на Compose Resource.
 * [Finish] — день самой гонки; [Car] — практика / квалификация / спринт в этот день.
 */
enum class DayLogo {
    Finish,
    Car,
}
