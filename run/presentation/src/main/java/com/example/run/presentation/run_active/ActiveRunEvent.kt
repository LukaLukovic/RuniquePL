package com.example.run.presentation.run_active

import com.example.core.presentation.ui.UiText

sealed interface ActiveRunEvent {

    data class Error(val error: UiText): ActiveRunEvent

    data object RunSaved: ActiveRunEvent
}