package com.cikup.amazgone.core.presentation.mvi

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private data class CounterState(val count: Int = 0) : UiState

private sealed interface CounterIntent : UiIntent {
    data object Increment : CounterIntent
    data object ReachLimit : CounterIntent
}

private sealed interface CounterEffect : UiEffect {
    data class LimitReached(val at: Int) : CounterEffect
}

private class CounterViewModel : MviViewModel<CounterState, CounterIntent, CounterEffect>(CounterState()) {
    override fun handleIntent(intent: CounterIntent) {
        when (intent) {
            CounterIntent.Increment -> setState { copy(count = count + 1) }
            CounterIntent.ReachLimit -> sendEffect(CounterEffect.LimitReached(currentState.count))
        }
    }
}

class MviViewModelTest {

    @Test
    fun exposesInitialState() {
        assertEquals(CounterState(0), CounterViewModel().state.value)
    }

    @Test
    fun intentsReduceStateImmutably() = runTest {
        val viewModel = CounterViewModel()
        val before = viewModel.state.value

        viewModel.onIntent(CounterIntent.Increment)
        viewModel.onIntent(CounterIntent.Increment)

        assertEquals(2, viewModel.state.value.count)
        assertEquals(0, before.count, "previous state instance must not be mutated")
    }

    @Test
    fun effectsAreDeliveredOnceInOrder() = runTest {
        val viewModel = CounterViewModel()

        viewModel.effects.test {
            viewModel.onIntent(CounterIntent.ReachLimit)
            viewModel.onIntent(CounterIntent.Increment)
            viewModel.onIntent(CounterIntent.ReachLimit)

            assertEquals(CounterEffect.LimitReached(0), awaitItem())
            assertEquals(CounterEffect.LimitReached(1), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun effectsSentBeforeCollectionAreBuffered() = runTest {
        val viewModel = CounterViewModel()
        viewModel.onIntent(CounterIntent.ReachLimit)

        viewModel.effects.test {
            assertEquals(CounterEffect.LimitReached(0), awaitItem())
        }
    }
}
