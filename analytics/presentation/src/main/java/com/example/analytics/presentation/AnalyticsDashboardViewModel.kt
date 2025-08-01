package com.example.analytics.presentation

import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.analytics.domain.AnalyticsRepository
import kotlinx.coroutines.launch

class AnalyticsDashboardViewModel(
    private val analyticsRepository: AnalyticsRepository
): ViewModel() {

    var state by mutableStateOf<AnalyticsDashboardState?>(null)


    init {
        viewModelScope.launch {
            state = analyticsRepository.getAnalyticsValue().toAnalyticsDashboardState()
        }
    }
}