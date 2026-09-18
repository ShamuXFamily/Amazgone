package com.cikup.amazgone.presentation

import com.cikup.amazgone.settings.domain.model.ThemeMode
import com.cikup.amazgone.settings.domain.usecase.ObserveThemeModeUseCase
import com.cikup.amazgone.settings.presentation.ThemeIntent
import com.cikup.amazgone.settings.presentation.ThemeViewModel
import com.cikup.amazgone.testing.TestGraph
import com.cikup.amazgone.testing.eventually
import com.cikup.amazgone.testing.mainForViewModels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeSettingsTest {
    private lateinit var graph: TestGraph

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(mainForViewModels)
        graph = TestGraph()
    }

    @AfterTest
    fun tearDown() {
        graph.close()
        Dispatchers.resetMain()
    }

    @Test
    fun defaultsToSystemThenPersistsChoice() = runTest {
        val vm = graph.get<ThemeViewModel>()
        assertEquals(ThemeMode.SYSTEM, vm.state.eventually { it.isLoaded }.mode)

        vm.onIntent(ThemeIntent.Select(ThemeMode.DARK))
        vm.state.eventually { it.mode == ThemeMode.DARK }
        assertEquals(ThemeMode.DARK, graph.get<ObserveThemeModeUseCase>()().first { it == ThemeMode.DARK })
    }
}
