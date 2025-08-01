package com.example.domain

import com.example.core.domain.util.DataError
import com.example.core.domain.util.EmptyResult

interface AuthRepository {


    suspend fun login(email: String, password: String) : EmptyResult<DataError.Network>
    suspend fun register(email: String, password: String): EmptyResult<DataError.Network>
}