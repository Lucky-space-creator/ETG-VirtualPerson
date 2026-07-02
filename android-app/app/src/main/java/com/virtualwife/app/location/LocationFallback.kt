package com.virtualwife.app.location

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LocationFallback {

    enum class LocationSource {
        GPS, BEACON, QRCODE, MANUAL, NONE
    }

    data class FallbackLocation(
        val latitude: Double,
        val longitude: Double,
        val source: LocationSource,
        val spotId: String? = null,
        val spotName: String? = null
    )

    private val _currentLocation = MutableStateFlow<FallbackLocation?>(null)
    val currentLocation: StateFlow<FallbackLocation?> = _currentLocation

    private val _currentSource = MutableStateFlow(LocationSource.NONE)
    val currentSource: StateFlow<LocationSource> = _currentSource

    fun updateFromGps(latitude: Double, longitude: Double) {
        _currentLocation.value = FallbackLocation(latitude, longitude, LocationSource.GPS)
        _currentSource.value = LocationSource.GPS
    }

    fun updateFromBeacon(spotId: String, latitude: Double, longitude: Double) {
        _currentLocation.value = FallbackLocation(
            latitude, longitude, LocationSource.BEACON, spotId
        )
        _currentSource.value = LocationSource.BEACON
    }

    fun updateFromQrCode(spotId: String, spotName: String, latitude: Double, longitude: Double) {
        _currentLocation.value = FallbackLocation(
            latitude, longitude, LocationSource.QRCODE, spotId, spotName
        )
        _currentSource.value = LocationSource.QRCODE
    }

    fun updateManual(spotId: String, spotName: String, latitude: Double, longitude: Double) {
        _currentLocation.value = FallbackLocation(
            latitude, longitude, LocationSource.MANUAL, spotId, spotName
        )
        _currentSource.value = LocationSource.MANUAL
    }

    fun clear() {
        _currentLocation.value = null
        _currentSource.value = LocationSource.NONE
    }
}
