package com.example.f1_kmp.platform

import android.content.Context

/**
 * Хранит Application [Context] для кода вне Activity
 * (файловый кэш, открытие URL). Инициализируется в [com.example.f1_kmp.F1Application].
 */
object AndroidContextHolder {
    lateinit var applicationContext: Context
        private set

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }
}
