package com.example.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import com.example.data.model.Municipality
import com.example.data.repository.MunicipalityData
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

class LocationHelper(private val context: Context) {

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentMunicipality(): Municipality? = withContext(Dispatchers.IO) {
        try {
            val location = getLastKnownLocation() ?: return@withContext null
            val geocoder = Geocoder(context, Locale.JAPAN)

            @Suppress("DEPRECATION")
            val addresses: List<Address>? = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            val address = addresses?.firstOrNull() ?: return@withContext null

            val locality = address.locality.orEmpty()
            val subAdmin = address.subAdminArea.orEmpty()
            val adminArea = address.adminArea.orEmpty()
            val subLocality = address.subLocality.orEmpty()

            val candidates = listOf(locality, subAdmin, subLocality).filter { it.isNotBlank() }

            // 1. Check if any candidate matches registered municipality name exactly or partially
            for (candidate in candidates) {
                val matched = MunicipalityData.ALL_MUNICIPALITIES.firstOrNull {
                    candidate.contains(it.name) || it.name.contains(candidate)
                }
                if (matched != null) {
                    return@withContext matched
                }
            }

            // 2. If locality contains "愛西" or "Aisai", return AISAI explicitly
            if (locality.contains("愛西") || locality.contains("Aisai", ignoreCase = true) ||
                subAdmin.contains("愛西") || address.getAddressLine(0).orEmpty().contains("愛西")) {
                return@withContext MunicipalityData.AISAI
            }

            // 3. Fallback to finding by name or creating dynamic municipality for the detected city
            if (locality.isNotBlank()) {
                return@withContext MunicipalityData.findByNameOrKeyword(locality)
            }

            // 4. If only prefecture is known, return prefecture's primary city
            if (adminArea.contains("愛知")) {
                return@withContext MunicipalityData.AISAI
            }

            MunicipalityData.findByNameOrKeyword(adminArea)
        } catch (e: Exception) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLastKnownLocation(): Location? = suspendCancellableCoroutine { continuation ->
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    continuation.resume(location)
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        } catch (e: Exception) {
            continuation.resume(null)
        }
    }
}
