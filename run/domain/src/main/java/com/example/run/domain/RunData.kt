package com.example.run.domain

import com.example.core.domain.location.LocationTimestamp
import kotlin.time.Duration

data class RunData(
    val distanceInMeters: Int = 0,
    val timePerKilometer: Duration = Duration.ZERO,
    val locations: List<List<LocationTimestamp>> = emptyList(),
    val heartRates: List<Int> = emptyList()
)
