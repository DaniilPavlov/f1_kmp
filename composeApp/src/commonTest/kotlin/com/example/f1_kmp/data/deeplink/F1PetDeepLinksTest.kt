package com.example.f1_kmp.data.deeplink

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class F1PetDeepLinksTest {

    @Test
    fun builders_useF1petScheme() {
        assertEquals("f1pet://driver/verstappen", F1PetDeepLinks.driver("verstappen"))
        assertEquals("f1pet://constructor/red_bull", F1PetDeepLinks.constructor("red_bull"))
        assertEquals("f1pet://circuit/monaco", F1PetDeepLinks.circuit("monaco"))
        assertEquals("f1pet://race/live", F1PetDeepLinks.raceLive())
        assertEquals("f1pet://race/2026/5", F1PetDeepLinks.race("2026", "5"))
    }

    @Test
    fun toDeepLinkTarget_parsesKnownHosts() {
        assertEquals(DeepLinkTarget.Driver("hamilton"), "f1pet://driver/hamilton".toDeepLinkTarget())
        assertEquals(
            DeepLinkTarget.Constructor("mercedes"),
            "f1pet://constructor/mercedes".toDeepLinkTarget(),
        )
        assertEquals(DeepLinkTarget.Circuit("spa"), "f1pet://circuit/spa".toDeepLinkTarget())
        assertEquals(DeepLinkTarget.RaceLive, "f1pet://race/live".toDeepLinkTarget())
        assertEquals(DeepLinkTarget.Race("2024", "12"), "f1pet://race/2024/12".toDeepLinkTarget())
    }

    @Test
    fun toDeepLinkTarget_rejectsInvalid() {
        assertNull("https://example.com".toDeepLinkTarget())
        assertNull("f1pet://unknown/x".toDeepLinkTarget())
        assertNull("f1pet://driver/".toDeepLinkTarget())
        assertNull("f1pet://race/only-season".toDeepLinkTarget())
        assertNull("".toDeepLinkTarget())
    }

    @Test
    fun toDeepLinkTarget_isCaseInsensitiveOnScheme() {
        val target = "F1PET://Driver/norris".toDeepLinkTarget()
        assertTrue(target is DeepLinkTarget.Driver)
        assertEquals("norris", (target as DeepLinkTarget.Driver).driverId)
    }
}
