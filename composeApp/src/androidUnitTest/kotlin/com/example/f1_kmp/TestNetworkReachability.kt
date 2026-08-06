package com.example.f1_kmp

import com.example.f1_kmp.domain.NetworkReachability

/** Online stub for ViewModel unit tests (no ConnectivityManager). */
fun onlineReachability(): NetworkReachability =
    NetworkReachability().apply { debugIsOfflineOverride = { false } }
