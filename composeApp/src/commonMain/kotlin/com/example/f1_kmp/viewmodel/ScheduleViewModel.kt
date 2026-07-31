package com.example.f1_kmp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kmp.domain.model.RaceSession
import com.example.f1_kmp.domain.model.Race
import com.example.f1_kmp.data.repository.IF1Repository
import com.example.f1_kmp.domain.AppDataRefresh
import com.example.f1_kmp.domain.AppError
import com.example.f1_kmp.domain.AsyncValue
import com.example.f1_kmp.domain.SessionStrings
import com.example.f1_kmp.util.DateUtils
import com.example.f1_kmp.util.RaceDateTimeHelper
import com.example.f1_kmp.util.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Clock
import kotlinx.datetime.number
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

data class ScheduleUiState(
    val races: AsyncValue<List<Race>> = AsyncValue.Loading,
    val selectedDate: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    /** true после первого клика по дню — чтобы «сегодня» оставался красным до тапа. */
    val userPickedDay: Boolean = false,
    val focusedMonth: YearMonth = YearMonth(
        Clock.System.todayIn(TimeZone.currentSystemDefault()).year,
        Clock.System.todayIn(TimeZone.currentSystemDefault()).month.number,
    ),
    val scheduleItems: List<ScheduleSessionItem> = emptyList(),
    val upcomingRace: Race? = null,
    val error: AppError? = null,
    val isRefreshing: Boolean = false,
)

/**
 * ViewModel вкладки «Календарь».
 *
 * Загружает расписание сезона, строит список сессий на выбранный день
 * и подсказывает иконки для дней (практика / гонка).
 * [refreshAll] чистит кэши через [AppDataRefresh] и грузит заново (ErrorBody / pull-to-refresh).
 */
class ScheduleViewModel(
    private val repository: IF1Repository,
    private val appDataRefresh: AppDataRefresh,
) : ViewModel() {
    private val loadJob = LoadJobHolder()
    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        loadAllData()
    }

    /** Peek → сеть. Ошибку сети не показываем, если на экране уже есть кэш. */
    fun loadAllData() {
        loadJob.launch(viewModelScope) {
            loadInternal(clearCaches = false)
        }
    }

    /** ErrorBody / pull-to-refresh: сброс кэшей, затем сеть. */
    fun refreshAll() {
        loadJob.launch(viewModelScope) {
            loadInternal(clearCaches = true)
        }
    }

    private suspend fun loadInternal(clearCaches: Boolean) {
        try {
            if (clearCaches) {
                appDataRefresh.clearAll()
                _uiState.update {
                    it.copy(
                        isRefreshing = true,
                        error = null,
                        races = if (it.races is AsyncValue.Value) it.races else AsyncValue.Loading,
                    )
                }
            } else {
                _uiState.update { it.copy(error = null) }

                repository.peekScheduleCache()?.let {
                    _uiState.update { state -> state.copy(races = AsyncValue.Value(it)) }
                    refreshUpcoming()
                    onSelectDay(today)
                } ?: run {
                    _uiState.update { it.copy(races = AsyncValue.Loading) }
                }
            }

            repository.getCurrentSchedule().applyUnlessCached(
                current = _uiState.value.races,
                onSuccess = { races ->
                    _uiState.update { it.copy(races = AsyncValue.Value(races)) }
                    refreshUpcoming()
                    onSelectDay(today)
                },
                onFailure = { err ->
                    if (_uiState.value.races !is AsyncValue.Value) {
                        _uiState.update {
                            it.copy(
                                races = err.toAsyncError(),
                                error = err,
                            )
                        }
                    }
                },
            )
        } finally {
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    /** Смена месяца в календаре (прокрутка без смены выбранного дня). */
    fun onMonthChanged(month: YearMonth) {
        _uiState.update { it.copy(focusedMonth = month) }
    }

    /** Обновляет [ScheduleUiState.scheduleItems] при клике на день в календаре. */
    fun onSelectDay(date: LocalDate) {
        _uiState.update { it.copy(userPickedDay = true, selectedDate = date) }
        buildScheduleForDate(date)
    }

    /** Перестраивает список сессий после смены языка. */
    fun refreshScheduleForCurrentDay() {
        buildScheduleForDate(_uiState.value.selectedDate)
    }

    private fun refreshUpcoming() {
        val races = _uiState.value.races.getOrNull() ?: run {
            _uiState.update { it.copy(upcomingRace = null) }
            return
        }
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        _uiState.update {
            it.copy(
                upcomingRace = races
                    .filter { race -> RaceDateTimeHelper.isUpcoming(race, now) }
                    .minByOrNull { race -> RaceDateTimeHelper.raceLocal(race) },
            )
        }
    }

    /** Иконка под днём: финиш — день гонки, машина — день сессии. */
    fun logoForDay(day: LocalDate): DayLogo? {
        val races = _uiState.value.races.getOrNull() ?: return null
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
        val races = _uiState.value.races.getOrNull() ?: return
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
                    _uiState.update {
                        it.copy(
                            scheduleItems = listOf(
                                ScheduleSessionItem(
                                    raceName = race.raceName,
                                    title = "",
                                    date = RaceSession("", null),
                                ),
                            ) + items,
                        )
                    }
                    return
                }
            }
        }
        _uiState.update { it.copy(scheduleItems = emptyList()) }
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
