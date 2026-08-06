package com.example.f1_kmp.domain

/** Баннер «сохранённые данные»: есть контент и сейчас нет сети. */
fun shouldShowOfflineCachedBanner(
    hasCachedContent: Boolean,
    reachability: NetworkReachability,
): Boolean {
    if (!hasCachedContent) return false
    return reachability.isOffline()
}

/**
 * После resume: если баннер показан и сеть появилась — спрятать без перезагрузки.
 * @return новое значение showingCachedData (true = всё ещё офлайн).
 */
fun clearOfflineBannerIfOnline(
    currentlyShowing: Boolean,
    reachability: NetworkReachability,
): Boolean {
    if (!currentlyShowing) return false
    reachability.clearMemo()
    return reachability.isOffline()
}
