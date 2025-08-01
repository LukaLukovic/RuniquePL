package com.example.runiquepl

data class MainState(
    val isLoggedIn: Boolean = false,
    val isCheckingAuth: Boolean = false,
    val shouldSHowAnalyticsInstallDialog: Boolean = false
)
