package com.example.analytics_feature

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.analytics.data.di.analyticsModule
import com.example.analytics.presentation.AnalyticsDashboardScreenRoot
import com.example.analytics.presentation.di.analyticsPresentationModule
import com.example.core.presentation.designsystem.RuniquePLTheme
import com.google.android.play.core.splitcompat.SplitCompat
import org.koin.core.context.loadKoinModules

class AnalyticsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadKoinModules(
            listOf(analyticsModule, analyticsPresentationModule)
        )
        SplitCompat.installActivity(this)

        setContent {
            RuniquePLTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "analytics_dashboard") {
                    composable("analytics_dashboard") {
                        AnalyticsDashboardScreenRoot(onBackClick = { finish() })
                    }
                }
            }
        }
    }
}