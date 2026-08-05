package com.example.f1_kmp.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ConstructorColorsTest {
    @Test
    fun forConstructorId_knownTeams_returnExpectedColors() {
        assertEquals(Color(0xFFFF8700), ConstructorColors.forConstructorId("mclaren"))
        assertEquals(Color(0xFFA51010), ConstructorColors.forConstructorId("Ferrari"))
        assertEquals(Color(0xFF006F62), ConstructorColors.forConstructorId("mercedes"))
    }

    @Test
    fun forConstructorId_unknown_isStableAcrossCalls() {
        val a = ConstructorColors.forConstructorId("unknown_team_xyz")
        val b = ConstructorColors.forConstructorId("unknown_team_xyz")
        assertEquals(a, b)
        assertNotEquals(ConstructorColors.forConstructorId("other_unknown"), a)
    }
}
