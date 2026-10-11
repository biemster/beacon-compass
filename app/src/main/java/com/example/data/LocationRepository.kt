package com.example.data

import kotlinx.coroutines.flow.Flow

class LocationRepository(private val locationDao: LocationDao) {
    val allLocations: Flow<List<TargetLocation>> = locationDao.getAllLocations()
    val activeLocation: Flow<TargetLocation?> = locationDao.getActiveLocation()

    suspend fun saveAndSelectLocation(location: TargetLocation) {
        locationDao.saveAndSelectLocation(location)
    }

    suspend fun selectActiveLocation(id: Int) {
        locationDao.selectActiveLocation(id)
    }

    suspend fun deleteLocation(location: TargetLocation) {
        locationDao.deleteLocation(location)
    }

    suspend fun insertLocation(location: TargetLocation) {
        locationDao.insertLocation(location)
    }
}
