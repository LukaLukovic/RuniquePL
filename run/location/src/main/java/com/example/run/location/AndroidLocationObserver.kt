package com.example.run.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.content.getSystemService
import com.example.core.domain.location.LocationWithAltitude
import com.example.run.domain.LocationObserver
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AndroidLocationObserver(
    private val context: Context
) : LocationObserver {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    override fun observeLocation(interval: Long): Flow<LocationWithAltitude> {
        return callbackFlow {
            val locationManager = context.getSystemService<LocationManager>() ?: return@callbackFlow

            var isGpsEnabled = false
            var isNetWorkEnabled = false

            while (isGpsEnabled.not() && isNetWorkEnabled.not()) {
                isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                isNetWorkEnabled =
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                if (isGpsEnabled.not() && isNetWorkEnabled.not()) {
                    delay(3000L)
                }
            }

            if (
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                close()
            } else {

                client.lastLocation.addOnSuccessListener {
                    it?.let { location ->
                        trySend(location.toLocationWithAltitude())
                    }
                }

                val request =
                    LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval).build()

                val locationCallBack = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        super.onLocationResult(result)
                        result.locations.lastOrNull()?.let { location ->
                            trySend(location.toLocationWithAltitude())
                        }
                    }
                }

                client.requestLocationUpdates(request, locationCallBack, Looper.getMainLooper())
                awaitClose {
                    client.removeLocationUpdates(locationCallBack)
                }
            }
        }
    }
}