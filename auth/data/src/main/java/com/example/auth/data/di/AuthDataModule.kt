package com.example.auth.data.di

import com.example.auth.data.AuthRepositoryImpl
import com.example.auth.data.EmailPatternValidator
import com.example.domain.AuthRepository
import com.example.domain.PatternValidator
import com.example.domain.UserDataValidator
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val authDataModule = module {

    single<PatternValidator> { EmailPatternValidator }
    singleOf(::UserDataValidator)
    single<AuthRepository> { AuthRepositoryImpl(get(), get())}
}