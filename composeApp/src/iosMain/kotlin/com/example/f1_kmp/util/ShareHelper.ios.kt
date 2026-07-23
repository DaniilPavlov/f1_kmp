package com.example.f1_kmp.util

import com.example.f1_kmp.domain.model.Race
import com.example.f1_kmp.domain.model.RaceResult
import com.example.f1_kmp.domain.LocaleController
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow

actual fun shareCareerCard(title: String, races: Int, wins: Int, podiums: Int, poles: Int) {
    val en = LocaleController.language.value == "en"
    val text = buildString {
        appendLine(title)
        appendLine(if (en) "Career" else "Карьера")
        appendLine()
        appendLine(
            buildString {
                append(if (en) "Races" else "Гонки")
                append(": $races")
                append(" · ")
                append(if (en) "Wins" else "Победы")
                append(": $wins")
            },
        )
        appendLine(
            buildString {
                append(if (en) "Podiums" else "Подиумы")
                append(": $podiums")
                append(" · ")
                append(if (en) "Poles" else "Поулы")
                append(": $poles")
            },
        )
        appendLine()
        append("F1 KMP")
    }
    presentShareSheet(text)
}

actual fun shareRaceResultsCard(race: Race) {
    presentShareSheet(formatRaceResultsText(race))
}

/** Формирует текстовую сводку результатов гонки для системного share sheet. */
internal fun formatRaceResultsText(race: Race, topN: Int = 10): String {
    val en = LocaleController.language.value == "en"
    val results = race.results.orEmpty()
    val rows = results.take(topN)
    return buildString {
        appendLine(race.raceName)
        appendLine(
            if (en) {
                "${race.season} · Round ${race.round}"
            } else {
                "${race.season} · Раунд ${race.round}"
            },
        )
        appendLine()
        if (rows.isEmpty()) {
            appendLine(if (en) "No race results yet" else "Результатов гонки пока нет")
        } else {
            rows.forEach { appendLine(formatResultLine(it)) }
            if (results.size > topN) {
                appendLine(
                    if (en) {
                        "…and ${results.size - topN} more"
                    } else {
                        "…и ещё ${results.size - topN}"
                    },
                )
            }
        }
        appendLine()
        append("F1 KMP")
    }
}

private fun formatResultLine(result: RaceResult): String {
    val classified = result.time != null || result.status.equals("Finished", ignoreCase = true)
    val timeOrStatus = result.time?.time ?: result.status
    return buildString {
        append("P${result.positionText} ")
        append(result.driver.fullName)
        append(" (${result.constructor.name})")
        append(" — ")
        append(timeOrStatus)
        if (!classified) append(" *")
    }
}

/** Открывает [UIActivityViewController] с верхнего UIViewController приложения. */
private fun presentShareSheet(text: String) {
    val presenter = topViewController() ?: return
    val activity = UIActivityViewController(
        activityItems = listOf(text),
        applicationActivities = null,
    )
    presenter.presentViewController(activity, animated = true, completion = null)
}

/** Ищет верхний контроллер: keyWindow или windows → root → presented. */
private fun topViewController(): UIViewController? {
    val window = keyWindow() ?: return null
    var controller = window.rootViewController ?: return null
    while (controller.presentedViewController != null) {
        controller = controller.presentedViewController!!
    }
    return controller
}

private fun keyWindow(): UIWindow? = UIApplication.sharedApplication.keyWindow
