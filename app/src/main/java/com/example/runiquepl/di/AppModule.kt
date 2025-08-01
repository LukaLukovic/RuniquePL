package com.example.runiquepl.di

import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.auth.data.EmailPatternValidator
import com.example.domain.PatternValidator
import com.example.domain.UserDataValidator
import com.example.runiquepl.BaseApplication
import com.example.runiquepl.MainViewModel
import kotlinx.coroutines.CoroutineScope
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val appModule = module {

    single<PatternValidator> { EmailPatternValidator }
    singleOf(::UserDataValidator)

    single<SharedPreferences> {
        EncryptedSharedPreferences(
            androidApplication(),
            "auth_pref",
            MasterKey(androidApplication()),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    single<CoroutineScope> {
        (androidApplication() as BaseApplication).applicationScope
    }
    viewModelOf(::MainViewModel)

}