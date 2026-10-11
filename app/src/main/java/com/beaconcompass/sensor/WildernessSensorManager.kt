package com.beaconcompass.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * WildernessSensorManager handles high-performance, completely offline location and compass
 * hardware interactions.
 *
 * To comply with the user's objective of preserving battery, this manager supports explicit
 * startup & teardown mechanisms to ensure GPS and magnetometers are shut down immediately
 * when the app is paused or in background.
 */
class WildernessSensorManager(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    // Core Sensor instances
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    // Cached gravity and geomagnetic vectors for azimuth (heading) math
    private var gravityValues: FloatArray? = null
    private var geomagneticValues: FloatArray? = null

    // For exponential smoothing of heading to prevent jitter
    private var smoothedHeading = 0f
    private val smoothFactor = 0.12f // Lower value = smoother, higher value = more reactive

    // Tracking state
    private val _sensorState = MutableStateFlow(SensorState())
    val sensorState: StateFlow<SensorState> = _sensorState.asStateFlow()

    data class SensorState(
        val deviceHeading: Float = 0f,       // Heading in degrees (0..360, where 0 = North, 90 = East)
        val userLatitude: Double? = null,    // GPS Latitude
        val userLongitude: Double? = null,   // GPS Longitude
        val gpsAccuracyMeters: Float? = null, // Accuracy index for hiking trust
        val isGpsActive: Boolean = false,    // Is the receiver online
        val errorMsg: String? = null
    )

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            _sensorState.update { current ->
                current.copy(
                    userLatitude = location.latitude,
                    userLongitude = location.longitude,
                    gpsAccuracyMeters = location.accuracy,
                    isGpsActive = true
                )
            }
        }

        @Deprecated("Deprecated in Android 29")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {
            _sensorState.update { it.copy(isGpsActive = true) }
        }
        override fun onProviderDisabled(provider: String) {
            if (provider == LocationManager.GPS_PROVIDER) {
                _sensorState.update { it.copy(isGpsActive = false) }
            }
        }
    }

    /**
     * Start observing sensors. Call this inside onStart or onResume to start tracking.
     */
    @SuppressLint("MissingPermission")
    fun startTracking() {
        Log.d("WildernessSensor", "Resuming hardware sensors and GPS to start tracking.")
        
        // 1. Initialise Sensors
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        // 2. Fetch Cached Location (very fast offline cold start)
        try {
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            _sensorState.update { it.copy(isGpsActive = isGpsEnabled) }

            // Grab the last known location from standard provider for instant feedback
            val lastKnownGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastKnownNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val bestLocation = lastKnownGps ?: lastKnownNet

            bestLocation?.let { loc ->
                _sensorState.update { current ->
                    current.copy(
                        userLatitude = loc.latitude,
                        userLongitude = loc.longitude,
                        gpsAccuracyMeters = loc.accuracy
                    )
                }
            }

            // 3. Register for updates (no cellular connection needed, directly using satellite GPS hardware)
            if (isGpsEnabled) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000L,   // Update every 2 seconds for high performance balance
                    1f,      // Update if position changes by 1 meter
                    locationListener
                )
            }
            if (isNetworkEnabled) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    3000L,
                    1f,
                    locationListener
                )
            }
        } catch (e: SecurityException) {
            _sensorState.update { it.copy(errorMsg = "Location permissions required.") }
        } catch (e: Exception) {
            _sensorState.update { it.copy(errorMsg = "Hardware navigation unavailable: ${e.message}") }
        }
    }

    /**
     * Terminate sensor tracking immediately to conserve battery.
     * Call this inside onStop or onPause.
     */
    fun stopTracking() {
        Log.d("WildernessSensor", "Pausing hardware sensors and powering down GPS to preserve battery.")
        sensorManager.unregisterListener(this)
        try {
            locationManager.removeUpdates(locationListener)
        } catch (e: Exception) {
            Log.e("WildernessSensor", "Error destroying location listener: ${e.message}")
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> gravityValues = event.values.clone()
            Sensor.TYPE_MAGNETIC_FIELD -> geomagneticValues = event.values.clone()
        }

        val gravity = gravityValues
        val geomagnetic = geomagneticValues

        if (gravity != null && geomagnetic != null) {
            val rotationMatrix = FloatArray(9)
            val inclinationMatrix = FloatArray(9)
            val success = SensorManager.getRotationMatrix(
                rotationMatrix,
                inclinationMatrix,
                gravity,
                geomagnetic
            )

            if (success) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)

                // Azimuth is rotation around the vertical -Z axis.
                // It is in radians, ranging from -PI to PI
                var targetHeading = Math.toDegrees(orientation[0].toDouble()).toFloat()
                
                // Normalise heading: 0 is North, 90 is East, 180 is South, 270/(-90) is West.
                // Convert typical [-180..180] degrees to positive [0..360] degrees range
                if (targetHeading < 0) {
                    targetHeading += 360f
                }

                // Smooth rotation using trigonometric / modular interpolation to prevent "jittery" needle
                smoothedHeading = smoothAngle(smoothedHeading, targetHeading, smoothFactor)

                _sensorState.update { current ->
                    current.copy(deviceHeading = smoothedHeading)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Essential override, can ignore for simple metrics
    }

    /**
     * Circular angle interpolation to deal with modular 360-degree wrapping.
     * Keeps needle movement fluid even when shifting between 359° and 1°
     */
    private fun smoothAngle(current: Float, target: Float, alpha: Float): Float {
        var diff = target - current
        while (diff < -180f) diff += 360f
        while (diff > 180f) diff -= 360f
        return (current + alpha * diff + 360f) % 360f
    }
}
