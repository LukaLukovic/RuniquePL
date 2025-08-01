package com.example.wear.app.presentation

import android.app.Application
import com.example.core.connectivity.data.coreConnectivityDataModule
import com.example.wear.app.presentation.di.wearAppModule
import com.example.wear.run.data.di.wearRunDataModule
import com.example.wear.run.presentation.di.wearRunPresentationModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class BaseApplication: Application() {

    val applicationScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@BaseApplication)
            modules(
                wearAppModule,
                wearRunPresentationModule,
                wearRunDataModule,
                coreConnectivityDataModule,
            )
        }

    }
}
