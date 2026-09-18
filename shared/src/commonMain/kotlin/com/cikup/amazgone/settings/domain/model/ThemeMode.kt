package com.cikup.amazgone.settings.domain.model

/** How the app picks light or dark colours. [SYSTEM] follows the device setting. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    companion object {
        /** Unknown or missing stored values fall back to [SYSTEM]. */
        fun parse(value: String?): ThemeMode = entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}
