package com.example.run.presentation.run_active.maps

import android.location.Location
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.example.core.domain.location.LocationTimestamp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object PolyLineCalculator {

    fun locationsToColor(location1: LocationTimestamp, location2: LocationTimestamp): Color {
        val distancMeters = location1.location.location.distanceTo(location2.location.location)
        val timeDiff = abs((location2.durationTimestamp - location1.durationTimestamp).inWholeSeconds)
        val speedKmh = (distancMeters / timeDiff) * 3.6

        return interpolateColor(
            speedKmh = speedKmh,
            minSpeed = 5.0,
            maxSpeed = 20.0,
            colorStart = Color.Green,
            colorMid = Color.Yellow,
            colorEnd = Color.Red
        )
    }

    private fun interpolateColor(
        speedKmh: Double,
        minSpeed: Double,
        maxSpeed: Double,
        colorStart: Color,
        colorMid: Color,
        colorEnd: Color
    ): Color {
        val ratio = ((speedKmh - minSpeed) / (maxSpeed - minSpeed)).coerceIn(0.0..1.0)

        val colorInt = if (ratio <= 0.5) {
            val midRatio = ratio * 2
            ColorUtils.blendARGB(colorStart.toArgb(), colorMid.toArgb(), midRatio.toFloat())
        } else {
            val midToEndRatio = (ratio - 0.5) * 2
            ColorUtils.blendARGB(colorMid.toArgb(), colorEnd.toArgb(), midToEndRatio.toFloat())
        }

        return Color(colorInt)
    }
}