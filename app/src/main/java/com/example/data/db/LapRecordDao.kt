package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.LapRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LapRecordDao {
    @Query("SELECT * FROM lap_records WHERE sessionId = :sessionId ORDER BY laneNumber ASC, lapIndex ASC")
    fun getLapsForSession(sessionId: Long): Flow<List<LapRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLap(lap: LapRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLaps(laps: List<LapRecordEntity>)

    @Query("DELETE FROM lap_records WHERE sessionId = :sessionId")
    suspend fun deleteLapsForSession(sessionId: Long)

    @Query("DELETE FROM lap_records WHERE id = :id")
    suspend fun deleteLapById(id: Long)
}
