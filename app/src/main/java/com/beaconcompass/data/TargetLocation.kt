package com.beaconcompass.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "target_locations")
data class TargetLocation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val isCurrent: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
