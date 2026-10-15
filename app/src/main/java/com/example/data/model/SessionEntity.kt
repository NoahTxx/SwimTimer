package com.example.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val title: String,
    val poolLengthMeters: Int = 50,
    val targetDistanceMeters: Int = 100,
    val strokeType: String = "Freistil",
    val startMode: String = "MASS", // "MASS" or "INTERVAL"
    val intervalOffsetSeconds: Int = 5,
    val laneCount: Int = 4,
    val splitDistanceMeters: Int = 50,
    val notes: String = ""
)
