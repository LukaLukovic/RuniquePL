package com.example.auth.presentation.login

sealed interface LoginAction {
    data object onTogglePasswordVisibility: LoginAction
    data object onLoginClick: LoginAction
    data object onRegisterClick: LoginAction
}