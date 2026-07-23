package com.example.f1_kmp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kmp.domain.model.RaceSession
import com.example.f1_kmp.domain.model.Race
import com.example.f1_kmp.data.repository.IF1Repository
import com.example.f1_kmp.domain.AppError
import com.example.f1_kmp.domain.AsyncValue
import com.example.f1_kmp.domain.SessionStrings
import com.example.f1_kmp.util.DateUtils
import com.example.f1_kmp.util.RaceDateTimeHelper
import com.example.f1_kmp.util.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

/**
 * Элемент списка «сессия на выбранный день» для экрана «Календарь».
 * [title] пустой у заголовка-разделителя с названием гонки.
 */
data class ScheduleSessionItem(
    val raceName: String,
    val title: String,
    val date: RaceSession,
)

/**
 * ViewModel вкладки «Календарь».
 *
 * Загружает расписание сезона, строит список сессий на выбранный день
 * и подсказывает иконки для дней (практика / гонка).
 */
class ScheduleViewModel(
    private val repository: IF1Repository,
) : ViewModel() {
    private val loadJob = LoadJobHolder()
    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    private val _races = MutableStateFlow<AsyncValue<List<Race>>>(AsyncValue.Loading)
    val races: StateFlow<AsyncValue<List<Race>>> = _races.asStateFlow()

    private val _selectedDate = MutableStateFlow(today)
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    /** true после первого клика по дню — чтобы «сегодня» оставался красным до тапа. */
    private val _userPickedDay = MutableStateFlow(false)
    val userPickedDay: StateFlow<Boolean> = _userPickedDay.asStateFlow()

    private val _focusedMonth = MutableStateFlow(YearMonth(today.year, today.monthNumber))
    val focusedMonth: StateFlow<YearMonth> = _focusedMonth.asStateFlow()

    private val _scheduleItems = MutableStateFlow<List<ScheduleSessionItem>>(emptyList())
    val scheduleItems: StateFlow<List<ScheduleSessionItem>> = _scheduleItems.asStateFlow()

    private val _upcomingRace = MutableStateFlow<Race?>(null)
    val upcomingRace: StateFlow<Race?> = _upcomingRace.asStateFlow()

    private val _error = MutableStateFlow<AppError?>(null)
    val error: StateFlow<AppError?> = _error.asStateFlow()

    init {
        loadAllData()
    }

    /** Peek → сеть. Ошибку сети не показываем, если на экране уже есть кэш. */
    fun loadAllData() {
        loadJob.launch(viewModelScope) {
            _error.value = null

            repository.peekScheduleCache()?.let {
                _races.value = AsyncValue.Value(it)
                refreshUpcoming()
                onSelectDay(today)
            } ?: run {
                _races.value = AsyncValue.Loading
            }

            repository.getCurrentSchedule().applyUnlessCached(
                current = _races.value,
                onSuccess = { races ->
                    _races.value = AsyncValue.Value(races)
                    refreshUpcoming()
                    onSelectDay(today)
                },
                onFailure = { ex ->
                    if (_races.value !is AsyncValue.Value) {
                        _races.value = AsyncValue.Error(ex.title, ex.subtitle)
                        _error.value = ex
                    }
                },
            )
        }
    }

    /** Смена месяца в календаре (прокрутка без смены выбранного дня). */
    fun onMonthChanged(month: YearMonth) {
        _focusedMonth.value = month
    }

    /** Обновляет [scheduleItems] при клике на день в календаре. */
    fun onSelectDay(date: LocalDate) {
        _userPickedDay.value = true
        _selectedDate.value = date
        buildScheduleForDate(date)
    }

    /** Перестраивает список сессий после смены языка. */
    fun refreshScheduleForCurrentDay() {
        buildScheduleForDate(_selectedDate.value)
    }

    private fun refreshUpcoming() {
        val races = _races.value.getOrNull() ?: run {
            _upcomingRace.value = null
            return
        }
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        _upcomingRace.value = races
            .filter { RaceDateTimeHelper.isUpcoming(it, now) }
            .minByOrNull { RaceDateTimeHelper.raceLocal(it) }
    }

    /** Иконка под днём: финиш — день гонки, машина — день сессии. */
    fun logoForDay(day: LocalDate): DayLogo? {
        val races = _races.value.getOrNull() ?: return null
        if (races.any { LocalDate.parse(it.date) == day }) return DayLogo.Finish
        if (races.any { hasSessionOnDay(it, day) }) return DayLogo.Car
        return null
    }

    private fun hasSessionOnDay(race: Race, day: LocalDate): Boolean =
        sessions(race).any { it?.date?.let { d -> LocalDate.parse(d) == day } == true }

    private fun sessions(race: Race) = listOf(
        race.firstPractice,
        race.secondPractice,
        race.thirdPractice,
        race.sprintQualifying,
        race.sprint,
        race.qualifying,
    )

    private fun buildScheduleForDate(date: LocalDate) {
        val races = _races.value.getOrNull() ?: return
        val items = mutableListOf<ScheduleSessionItem>()

        for (race in races) {
            val raceDate = LocalDate.parse(race.date)
            if (DateUtils.isSameDay(raceDate, date) || raceDate > date) {
                addSessionsForDay(race, date, items)
                if (DateUtils.isSameDay(raceDate, date)) {
                    items.add(
                        ScheduleSessionItem(
                            raceName = race.raceName,
                            title = SessionStrings.race,
                            date = RaceSession(date = race.date, time = race.time),
                        ),
                    )
                }
                if (items.isNotEmpty()) {
                    _scheduleItems.value = listOf(
                        ScheduleSessionItem(raceName = race.raceName, title = "", date = RaceSession("", null)),
                    ) + items
                    return
                }
            }
        }
        _scheduleItems.value = emptyList()
    }

    private fun addSessionsForDay(race: Race, day: LocalDate, items: MutableList<ScheduleSessionItem>) {
        val sessionPairs = listOf(
            race.firstPractice to SessionStrings.firstPractice,
            race.secondPractice to SessionStrings.secondPractice,
            race.thirdPractice to SessionStrings.thirdPractice,
            race.sprintQualifying to SessionStrings.sprintQualifying,
            race.sprint to SessionStrings.sprint,
            race.qualifying to SessionStrings.qualifying,
        )
        sessionPairs.forEach { (session, title) ->
            if (session != null && LocalDate.parse(session.date) == day) {
                items.add(ScheduleSessionItem(race.raceName, title, session))
            }
        }
    }
}
