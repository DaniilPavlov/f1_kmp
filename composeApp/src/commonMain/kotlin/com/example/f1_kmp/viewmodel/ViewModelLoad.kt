package com.example.f1_kmp.viewmodel

import com.example.f1_kmp.domain.AppError
import com.example.f1_kmp.domain.AsyncValue
import com.example.f1_kmp.domain.toAppError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Общий паттерн загрузки для всех ViewModel:
 * - отмена предыдущего [Job] при повторном вызове (не копятся параллельные запросы);
 * - не затирать уже показанные данные (кэш/peek) ошибкой сети — см. [applyUnlessCached].
 */
class LoadJobHolder {
    private var job: Job? = null

    /** Отменяет предыдущий [Job] и запускает новый блок загрузки. */
    fun launch(scope: CoroutineScope, block: suspend CoroutineScope.() -> Unit) {
        job?.cancel()
        job = scope.launch(block = block)
    }
}

/**
 * Применяет [Result]: успех всегда обновляет UI; ошибку — только если кэша ещё не было.
 *
 * @param hasCachedValue true, если peek уже отдал [AsyncValue.Value] на экран.
 */
inline fun <T> Result<T>.applyUnlessCached(
    hasCachedValue: Boolean,
    crossinline onSuccess: (T) -> Unit,
    crossinline onFailure: (AppError) -> Unit,
) {
    onSuccess { value -> onSuccess(value) }
    onFailure { e ->
        if (!hasCachedValue) {
            onFailure(e.toAppError())
        }
    }
}

/** Перегрузка: «есть кэш» = текущее состояние уже [AsyncValue.Value]. */
inline fun <T> Result<T>.applyUnlessCached(
    current: AsyncValue<*>,
    crossinline onSuccess: (T) -> Unit,
    crossinline onFailure: (AppError) -> Unit,
) = applyUnlessCached(
    hasCachedValue = current is AsyncValue.Value,
    onSuccess = onSuccess,
    onFailure = onFailure,
)
