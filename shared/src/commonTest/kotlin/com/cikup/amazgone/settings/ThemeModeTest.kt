package com.cikup.amazgone.settings

import com.cikup.amazgone.settings.domain.model.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeModeTest {
    @Test
    fun parsesStoredNames() {
        assertEquals(ThemeMode.DARK, ThemeMode.parse("DARK"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.parse("LIGHT"))
    }

    @Test
    fun unknownOrMissingFallsBackToSystem() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.parse(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.parse("sepia"))
    }
}
