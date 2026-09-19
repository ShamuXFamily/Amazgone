package com.cikup.amazgone.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * Text for a field whose source of truth is ViewModel state. Keystrokes are applied locally in the
 * same frame (driving the field straight from the StateFlow drops characters when typing fast or
 * pasting), and each change is still sent as an intent. The field follows the ViewModel only when
 * the ViewModel clears it (e.g. a "clear" button or a form reset).
 */
@Composable
fun rememberFieldText(value: String, key: Any? = Unit): MutableState<String> {
    val text = remember(key) { mutableStateOf(value) }
    LaunchedEffect(value) { if (value.isEmpty() && text.value.isNotEmpty()) text.value = "" }
    return text
}
