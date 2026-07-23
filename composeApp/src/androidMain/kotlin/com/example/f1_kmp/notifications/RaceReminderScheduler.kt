package com.example.f1_kmp.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.f1_kmp.R
import com.example.f1_kmp.domain.model.RaceSession
import com.example.f1_kmp.domain.model.Race
import com.example.f1_kmp.data.repository.IF1Repository
import com.example.f1_kmp.domain.SessionStrings
import com.example.f1_kmp.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.atomic.AtomicReference

/**
 * Локальные напоминания за 30 минут до сессий (Android).
 *
 * В AlarmManager держим только [MAX_SCHEDULED_REMINDERS] ближайших сессий (rolling window).
 * Sync: старт приложения / resume / смена языка / boot / timezone.
 */
class RaceReminderScheduler(
    private val context: Context,
    private val repository: IF1Repository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lastScheduledIds = AtomicReference<Set<Int>>(emptySet())

    /** Пересчитать rolling window напоминаний из актуального расписания. */
    fun sync() {
        scope.launch {
            runCatching {
                val races = repository.getCurrentSchedule().getOrNull() ?: return@runCatching
                val upcoming = sessions(races).sortedBy { it.triggerAt }
                val window = upcoming.take(MAX_SCHEDULED_REMINDERS)

                cancelIds(lastScheduledIds.get() + upcoming.map { it.id })
                schedule(window)
                lastScheduledIds.set(window.map { it.id }.toSet())
            }
        }
    }

    private fun sessions(races: List<Race>): List<Reminder> = buildList {
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
        createChannel()
        reminders.forEach { reminder ->
            ExplicitPendingIntents.schedule(context, reminder.triggerAt, intent(reminder))
        }
    }

    private fun cancelIds(ids: Set<Int>) {
        if (ids.isEmpty()) return
        ids.forEach { id ->
            ExplicitPendingIntents.cancel(context, intent(Reminder(id, 0, "", "")))
        }
    }

    private fun intent(reminder: Reminder): PendingIntent =
        ExplicitPendingIntents.raceReminder(context, reminder.id, reminder.title, reminder.body)

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = if (com.example.f1_kmp.domain.LocaleController.language.value == "en") {
                "Race reminders"
            } else {
                "Напоминания о гонках"
            }
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH),
            )
        }
    }

    private fun id(season: String, round: String, type: String) = "$season:$round:$type".hashCode() and 0x7fffffff

    private data class Reminder(
        val id: Int,
        val triggerAt: Long,
        val title: String,
        val body: String,
    )

    companion object {
        const val CHANNEL_ID = "race_reminders"
        const val EXTRA_ID = "id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
        private const val MAX_SCHEDULED_REMINDERS = 10
        private const val THIRTY_MINUTES = 30 * 60 * 1000L
    }
}

/** Показывает notification по срабатыванию AlarmManager. */
class RaceReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val title = intent.getStringExtra(RaceReminderScheduler.EXTRA_TITLE).orEmpty()
        val body = intent.getStringExtra(RaceReminderScheduler.EXTRA_BODY).orEmpty()
        val notification = NotificationCompat.Builder(context, RaceReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context)
            .notify(intent.getIntExtra(RaceReminderScheduler.EXTRA_ID, 0), notification)
    }
}

/** После reboot / смены timezone — снова вызывает [RaceReminderScheduler.sync]. */
class BootCompletedReceiver : BroadcastReceiver(), KoinComponent {
    private val reminderScheduler: RaceReminderScheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_TIMEZONE_CHANGED) {
            return
        }
        reminderScheduler.sync()
    }
}
