package com.example.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(
    tableName = "lap_records",
    indices = [
        Index("sessionId"),
        Index("swimmerId"),
    ]
)
data class LapRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val swimmerId: Long? = null,
    val swimmerName: String,
    val laneNumber: Int,
    val lapIndex: Int,
    val splitTimeMillis: Long,
    val lapTimeMillis: Long,
    val targetLapMillis: Long? = null,
    val deltaTargetMillis: Long? = null,
    val strokeRateSpm: Double? = null,
    val timestamp: Long = System.currentTimeMillis(),
)
