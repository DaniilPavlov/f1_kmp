package com.example.f1_kmp.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.f1_kmp.MainActivity

/**
 * Explicit PendingIntents — destination classes are compile-time literals so CodeQL
 * can prove the Intent is not implicit (unlike Class&lt;*&gt; parameters).
 */
internal object ExplicitPendingIntents {
    fun raceReminder(
        context: Context,
        id: Int,
        title: String,
        body: String,
    ): PendingIntent {
        val intent = Intent(context, RaceReminderReceiver::class.java).apply {
            setPackage(context.packageName)
            putExtra(RaceReminderScheduler.EXTRA_ID, id)
            putExtra(RaceReminderScheduler.EXTRA_TITLE, title)
            putExtra(RaceReminderScheduler.EXTRA_BODY, body)
        }
        return PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun openMainActivity(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            setPackage(context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun schedule(context: Context, triggerAt: Long, pending: PendingIntent) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
        if (canExact) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun cancel(context: Context, pending: PendingIntent) {
        context.getSystemService(AlarmManager::class.java)?.cancel(pending)
    }
}
