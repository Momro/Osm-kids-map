package com.momro.weltentdecker.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object LocationHelper {
    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    suspend fun awaitLocation(context: Context): Location? = suspendCancellableCoroutine { cont ->
        val lm = context.getSystemService(LocationManager::class.java)
        val provider = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .firstOrNull { lm.isProviderEnabled(it) }
        if (provider == null) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }
        var delivered = false
        val listener = object : LocationListener {
            fun deliver(location: Location) {
                if (delivered) return
                delivered = true
                lm.removeUpdates(this)
                if (cont.isActive) cont.resume(location)
            }

            override fun onLocationChanged(location: Location) = deliver(location)

            @Suppress("DEPRECATION")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

            override fun onProviderEnabled(provider: String) {}

            override fun onProviderDisabled(provider: String) {}
        }
        try {
            lm.getLastKnownLocation(provider)?.let { listener.deliver(it) }
            lm.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        } catch (e: Exception) {
            if (cont.isActive) cont.resume(null)
        }
        cont.invokeOnCancellation {
            try {
                lm.removeUpdates(listener)
            } catch (_: Exception) {
            }
        }
    }
}
