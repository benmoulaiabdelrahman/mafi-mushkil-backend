package com.vardash.mafimushkil.auth

sealed class ApplicationState {
    object Idle : ApplicationState()
    object Loading : ApplicationState()
    object Success : ApplicationState()
    data class Error(val message: String) : ApplicationState()
}
