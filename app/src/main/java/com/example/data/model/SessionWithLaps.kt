package com.example.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class SessionWithLaps(
    @Embedded val session: SessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val laps: List<LapRecordEntity>
)
