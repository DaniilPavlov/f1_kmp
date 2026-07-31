package com.example.f1_kmp.data.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FinishStatusItemTest {

    @Test
    fun isHighlight_detectsRetiredAndDnf() {
        assertTrue(FinishStatusItem("1", "Retired", 3).isHighlight)
        assertTrue(FinishStatusItem("2", "Accident", 1).isHighlight)
        assertTrue(FinishStatusItem("3", "Collision", 1).isHighlight)
        assertTrue(FinishStatusItem("4", "Disqualified", 1).isHighlight)
        assertTrue(FinishStatusItem("5", "Did not start", 1).isHighlight)
        assertTrue(FinishStatusItem("6", "DNS", 1).isHighlight)
        assertTrue(FinishStatusItem("7", "DNF", 1).isHighlight)
        assertTrue(FinishStatusItem("8", "+1 Lap", 2).isHighlight)
        assertTrue(FinishStatusItem("9", "Lapped", 1).isHighlight)
        assertTrue(FinishStatusItem("10", "Not classified", 1).isHighlight)
        assertFalse(FinishStatusItem("11", "Finished", 20).isHighlight)
    }
}
