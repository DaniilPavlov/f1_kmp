package com.example.f1_kmp.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class H2hPointsChartLabelStepTest {
    @Test
    fun labelStep_matchesFlutterRules() {
        assertEquals(1, labelStep(1))
        assertEquals(1, labelStep(8))
        assertEquals(2, labelStep(9))
        assertEquals(2, labelStep(16))
        assertEquals(3, labelStep(17))
        assertEquals(3, labelStep(30))
        assertEquals(5, labelStep(40)) // 40 / 8
    }
}
