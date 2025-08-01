package com.example.run.domain

import kotlinx.coroutines.flow.Flow
import com.example.core.domain.location.LocationWithAltitude

interface LocationObserver {

    fun observeLocation(interval: Long): Flow<LocationWithAltitude>
}