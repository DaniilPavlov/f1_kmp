package com.example.f1_kmp.viewmodel

import com.example.f1_kmp.domain.model.Constructor
import com.example.f1_kmp.domain.model.Driver
import com.example.f1_kmp.data.model.H2hStats

/** Результат сравнения двух пилотов на экране H2H. */
data class H2hDriverCompareResult(
    val driverA: Driver,
    val driverB: Driver,
    val statsA: H2hStats,
    val statsB: H2hStats,
    val season: String?,
)

/** Результат сравнения двух конструкторов на экране H2H. */
data class H2hConstructorCompareResult(
    val constructorA: Constructor,
    val constructorB: Constructor,
    val statsA: H2hStats,
    val statsB: H2hStats,
    val season: String?,
)
