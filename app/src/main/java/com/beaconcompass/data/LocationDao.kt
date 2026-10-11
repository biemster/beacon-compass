package com.beaconcompass.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Query("SELECT * FROM target_locations ORDER BY timestamp DESC")
    fun getAllLocations(): Flow<List<TargetLocation>>

    @Query("SELECT * FROM target_locations WHERE isCurrent = 1 LIMIT 1")
    fun getActiveLocation(): Flow<TargetLocation?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: TargetLocation): Long

    @Delete
    suspend fun deleteLocation(location: TargetLocation)

    @Query("UPDATE target_locations SET isCurrent = 0")
    suspend fun clearActiveFlag()

    @Query("UPDATE target_locations SET isCurrent = 1 WHERE id = :id")
    suspend fun rawSetActive(id: Int)

    @Transaction
    suspend fun selectActiveLocation(id: Int) {
        clearActiveFlag()
        rawSetActive(id)
    }

    @Transaction
    suspend fun saveAndSelectLocation(location: TargetLocation) {
        clearActiveFlag()
        val newId = insertLocation(location.copy(isCurrent = true))
        // Already activated via copy, but if we updated an existing one we make sure
    }
}
