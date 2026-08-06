package com.example.f1_kmp.domain

/**
 * Быстрая проверка отсутствия сети (баннер «сохранённые данные»).
 *
 * GoF Structural Bridge — общая абстракция [NetworkReachability] отделена от
 * платформенных реализаций (ConnectivityManager / NWPathMonitor) через expect/actual.
 *
 * Без TCP-probe: смотрим только наличие интерфейса с INTERNET capability (Android)
 * или консервативный online при неуверенности (iOS).
 */
expect class NetworkReachability() {
    fun clearMemo()
    fun isOffline(): Boolean
}
