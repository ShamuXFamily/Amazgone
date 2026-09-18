package com.cikup.amazgone.core.presentation.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cikup.amazgone.core.common.AppLogger
import com.cikup.amazgone.core.common.PrintLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

/** Marker for a screen's single immutable render state. */
interface UiState

/** Marker for user actions sent from the UI. The only input a screen can produce. */
interface UiIntent

/** Marker for one-shot events: navigation, snackbar, haptics, animation triggers. */
interface UiEffect

/**
 * Base MVI store: UI → [onIntent] → [handleIntent] → [setState]/[sendEffect] → UI.
 *
 * Effects are buffered without limit so none are lost while the UI is not collecting
 * (e.g. during a configuration change); each effect is delivered to exactly one collector.
 */
abstract class MviViewModel<S : UiState, I : UiIntent, E : UiEffect>(
    initialState: S,
    private val logger: AppLogger = PrintLogger,
) : ViewModel() {

    private val mutableState = MutableStateFlow(initialState)
    val state: StateFlow<S> = mutableState.asStateFlow()

    private val effectChannel = Channel<E>(Channel.UNLIMITED)
    val effects: Flow<E> = effectChannel.receiveAsFlow()

    protected val currentState: S
        get() = mutableState.value

    /** Scope for sharing flows between several observers inside one ViewModel. */
    protected val vmScope: CoroutineScope
        get() = viewModelScope

    fun onIntent(intent: I) = handleIntent(intent)

    protected abstract fun handleIntent(intent: I)

    /** Atomically replaces the state with a reduced copy. Reducers must use `copy()`, never mutate. */
    protected fun setState(reduce: S.() -> S) = mutableState.update { it.reduce() }

    protected fun sendEffect(effect: E) {
        effectChannel.trySend(effect)
    }

    private val errorHandler = CoroutineExceptionHandler { _, throwable -> reportError(throwable) }

    /** Collects [this] for the ViewModel's lifetime; failures are logged and routed to [onError], never crash. */
    protected fun <T> Flow<T>.observe(action: suspend (T) -> Unit): Job =
        onEach(action).catch { reportError(it) }.launchIn(viewModelScope)

    /** Launches work whose failures are logged and routed to [onError] instead of crashing the app. */
    protected fun launchSafely(block: suspend CoroutineScope.() -> Unit): Job =
        viewModelScope.launch(errorHandler, block = block)

    /** Override to surface a friendly error state. Default: log only. */
    protected open fun onError(throwable: Throwable) = Unit

    private fun reportError(throwable: Throwable) {
        if (throwable is CancellationException) throw throwable
        logger.error(this::class.simpleName ?: "ViewModel", "Unhandled error", throwable)
        onError(throwable)
    }
}
