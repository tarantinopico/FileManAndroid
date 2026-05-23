package com.example.ui.state

sealed interface UiEvent {
    data class ShowToast(val message: String) : UiEvent
    data class Error(val message: String) : UiEvent
}
