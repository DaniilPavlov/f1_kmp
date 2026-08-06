package com.example.f1_kmp.viewmodel

import com.example.f1_kmp.data.model.H2hEntityCompareData
import com.example.f1_kmp.domain.AppError
import com.example.f1_kmp.domain.toAppError
import kotlinx.coroutines.CoroutineScope

/**
 * Общий скелет H2H-сравнения: A затем B последовательно (throttle Jolpica).
 *
 * GoF Behavioral Strategy — способ получения статистики выбирается на вызове:
 * [fetchA] / [fetchB] — взаимозаменяемые стратегии (драйверы / конструкторы / тесты).
 */
fun LoadJobHolder.launchH2hCompare(
    scope: CoroutineScope,
    canCompare: Boolean,
    fetchA: suspend () -> Result<H2hEntityCompareData>,
    fetchB: suspend () -> Result<H2hEntityCompareData>,
    onLoading: () -> Unit,
    onError: (AppError) -> Unit,
    onSuccess: suspend (H2hEntityCompareData, H2hEntityCompareData) -> Unit,
) {
    if (!canCompare) return
    launch(scope) {
        onLoading()
        val dataA = fetchA()
        dataA.exceptionOrNull()?.toAppError()?.let {
            onError(it)
            return@launch
        }
        val dataB = fetchB()
        dataB.exceptionOrNull()?.toAppError()?.let {
            onError(it)
            return@launch
        }
        onSuccess(dataA.getOrThrow(), dataB.getOrThrow())
    }
}
