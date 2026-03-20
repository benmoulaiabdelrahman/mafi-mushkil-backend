package com.vardash.mafimushkil.auth

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class CodeSent(val verificationId: String) : AuthState()
    data class Success(val uid: String) : AuthState()
    data class Error(val message: String) : AuthState()
    object LoggedOut : AuthState()
}
