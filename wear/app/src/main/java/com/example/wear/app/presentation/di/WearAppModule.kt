package com.example.wear.app.presentation.di

import com.example.wear.app.presentation.BaseApplication
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val wearAppModule = module {
    single { (androidApplication() as BaseApplication).applicationScope }

}