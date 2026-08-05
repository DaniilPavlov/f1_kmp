package com.example.f1_kmp.notifications

import com.example.f1_kmp.domain.NotificationsPreference
import com.example.f1_kmp.domain.model.RaceSession
import com.example.f1_kmp.domain.model.Race
import com.example.f1_kmp.data.firebase.IRemoteConfigService
import com.example.f1_kmp.data.repository.IF1Repository
import com.example.f1_kmp.domain.SessionStrings
import com.example.f1_kmp.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

/**
 * Локальные напоминания за 30 минут до сессий (iOS).
 *
 * Держим только [MAX_SCHEDULED_REMINDERS] ближайших сессий (rolling window).
 * Sync: старт приложения / смена языка.
 *
 * Флаг Remote Config [IRemoteConfigService.localNotificationsEnabled] — kill-switch.
 */
class RaceReminderScheduler(
    private val repository: IF1Repository,
    private val remoteConfig: IRemoteConfigService,
    private val notificationsPreference: NotificationsPreference,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lastScheduledIds: Set<Int> = emptySet()
    private var permissionRequested = false

    /** Пересчитать rolling window напоминаний из актуального расписания. */
    fun sync() {
        ensureNotificationPermission()
        scope.launch {
            runCatching {
                if (!remoteConfig.localNotificationsEnabled || !notificationsPreference.effectivelyEnabled) {
                    cancelIds(lastScheduledIds)
                    lastScheduledIds = emptySet()
                    return@runCatching
                }
                val races = repository.getCurrentSchedule().getOrNull() ?: return@runCatching
                val includePractice = notificationsPreference.practiceRemindersEffectivelyEnabled
                val upcoming = sessions(races, includePractice).sortedBy { it.triggerAt }
                val window = upcoming.take(MAX_SCHEDULED_REMINDERS)

                cancelIds(lastScheduledIds + upcoming.map { it.id }.toSet())
                schedule(window)
                lastScheduledIds = window.map { it.id }.toSet()
            }
        }
    }

    /** Запрашивает разрешение на уведомления один раз при первом sync. */
    private fun ensureNotificationPermission() {
        if (permissionRequested) return
        permissionRequested = true
        UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
        ) { _, _ -> }
    }

    private fun sessions(races: List<Race>, includePractice: Boolean): List<Reminder> = buildList {
        races.forEach { race ->
            listOf(
                Triple("fp1", SessionStrings.firstPractice, race.firstPractice),
                Triple("fp2", SessionStrings.secondPractice, race.secondPractice),
                Triple("fp3", SessionStrings.thirdPractice, race.thirdPractice),
                Triple("sprint_qualifying", SessionStrings.sprintQualifying, race.sprintQualifying),
                Triple("sprint", SessionStrings.sprint, race.sprint),
                Triple("qualifying", SessionStrings.qualifying, race.qualifying),
                Triple("race", SessionStrings.race, RaceSession(race.date, race.time)),
            ).forEach { (key, title, date) ->
                if (!includePractice && key.startsWith("fp")) return@forEach
                val session = date ?: return@forEach
                val local = DateUtils.toLocalDateTime(session.date, session.time) ?: return@forEach
                val trigger = local.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds() - THIRTY_MINUTES
                if (trigger > Clock.System.now().toEpochMilliseconds()) {
                    add(
                        Reminder(
                            id = id(race.season, race.round, key),
                            triggerAt = trigger,
                            title = title,
                            body = "${race.raceName} · ${DateUtils.formatHourMinute(local)}",
                        ),
                    )
                }
            }
        }
    }

    private fun schedule(reminders: List<Reminder>) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        val nowMs = Clock.System.now().toEpochMilliseconds()
        reminders.forEach { reminder ->
            val delaySec = (reminder.triggerAt - nowMs) / 1000.0
            if (delaySec <= 0) return@forEach

            val content = UNMutableNotificationContent().apply {
                setTitle(reminder.title)
                setBody(reminder.body)
                setSound(UNNotificationSound.defaultSound)
            }
            val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(delaySec, repeats = false)
            val request = UNNotificationRequest.requestWithIdentifier(
                reminder.id.toString(),
                content,
                trigger,
            )
            center.addNotificationRequest(request) { }
        }
    }

    private fun cancelIds(ids: Set<Int>) {
        if (ids.isEmpty()) return
        UNUserNotificationCenter.currentNotificationCenter()
            .removePendingNotificationRequestsWithIdentifiers(ids.map { it.toString() })
    }

    private fun id(season: String, round: String, type: String) = "$season:$round:$type".hashCode() and 0x7fffffff

    private data class Reminder(
        val id: Int,
        val triggerAt: Long,
        val title: String,
        val body: String,
    )

    private companion object {
        private const val MAX_SCHEDULED_REMINDERS = 10
        private const val THIRTY_MINUTES = 30 * 60 * 1000L
    }
}
