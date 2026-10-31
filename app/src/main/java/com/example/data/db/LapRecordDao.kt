package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.LapRecordEntity

@Dao
interface LapRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLaps(laps: List<LapRecordEntity>)

    @Query("DELETE FROM lap_records WHERE sessionId = :sessionId")
    suspend fun deleteLapsForSession(sessionId: Long)
}
