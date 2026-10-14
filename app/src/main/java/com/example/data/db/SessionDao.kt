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
    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    fun getSessionWithLaps(sessionId: Long): Flow<SessionWithLaps?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    @Delete
    suspend fun deleteSession(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)
}
