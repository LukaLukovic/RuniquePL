package com.example.runiquepl

import android.app.Application
import android.content.Context
import com.example.auth.data.di.authDataModule
import com.example.auth.presentation.di.authViewModelModule
import com.example.core.connectivity.data.coreConnectivityDataModule
import com.example.core.data.di.coreDataModule
import com.example.database.di.databaseModule
import com.example.run.data.di.runDataModule
import com.example.run.location.di.locationModule
import com.example.run.network.di.networkModule
import com.example.run.presentation.di.runPresentationModule
import com.example.runiquepl.di.appModule
import com.google.android.play.core.splitcompat.SplitCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import timber.log.Timber

class BaseApplication: Application() {

    val applicationScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {

        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        startKoin {
            androidLogger()
            androidContext(this@BaseApplication)
            workManagerFactory()
            modules(
                authDataModule,
                authViewModelModule,
                appModule,
                coreDataModule,
                runPresentationModule,
                locationModule,
                databaseModule,
                networkModule,
                runDataModule,
                coreConnectivityDataModule
            )
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        SplitCompat.install(this)
    }
}