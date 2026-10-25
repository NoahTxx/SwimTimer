package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.model.SessionEntity
import com.example.data.model.SessionWithLaps
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Transaction
    @Query("SELECT * FROM sessions ORDER BY timestamp DESC")
    fun getAllSessionsWithLaps(): Flow<List<SessionWithLaps>>

    @Transaction
    @Query("SELECT * FROM sessions ORDER BY timestamp DESC")
    suspend fun getAllSessionsWithLapsSync(): List<SessionWithLaps>

    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    fun getSessionWithLaps(sessionId: Long): Flow<SessionWithLaps?>

    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    suspend fun getSessionWithLapsSync(sessionId: Long): SessionWithLaps?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    @Delete
    suspend fun deleteSession(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)

    @Query("""
        SELECT 
            COUNT(id) AS totalLapsSwum,
            MIN(lapTimeMillis) AS fastestLapMillis,
            AVG(lapTimeMillis) AS averageLapMillis
        FROM lap_records 
        WHERE swimmerId = :swimmerId
    """)
    fun getSwimmerStatsFlow(swimmerId: Long): Flow<SwimmerStatsSummary>
}

@androidx.compose.runtime.Immutable
data class SwimmerStatsSummary(
    val totalLapsSwum: Int,
    val fastestLapMillis: Long?,
    val averageLapMillis: Double?
)
