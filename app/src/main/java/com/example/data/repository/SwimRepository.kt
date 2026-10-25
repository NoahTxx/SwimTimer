package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.db.AppDatabase
import com.example.data.model.LapRecordEntity
import com.example.data.model.SessionEntity
import com.example.data.model.SessionWithLaps
import com.example.data.model.SwimmerEntity
import kotlinx.coroutines.flow.Flow

class SwimRepository(private val database: AppDatabase) {
    private val swimmerDao = database.swimmerDao()
    private val sessionDao = database.sessionDao()
    private val lapRecordDao = database.lapRecordDao()

    val allSwimmers: Flow<List<SwimmerEntity>> = swimmerDao.getAllSwimmers()
    val allSessionsWithLaps: Flow<List<SessionWithLaps>> = sessionDao.getAllSessionsWithLaps()

    suspend fun allSessionsWithLapsSync(): List<SessionWithLaps> =
        sessionDao.getAllSessionsWithLapsSync()

    @Suppress("unused")
    fun getSessionWithLaps(sessionId: Long): Flow<SessionWithLaps?> =
        sessionDao.getSessionWithLaps(sessionId)

    suspend fun getSessionWithLapsSync(sessionId: Long): SessionWithLaps? =
        sessionDao.getSessionWithLapsSync(sessionId)

    suspend fun getSwimmerById(id: Long): SwimmerEntity? =
        swimmerDao.getSwimmerByIdSync(id)

    suspend fun insertSwimmer(swimmer: SwimmerEntity): Long =
        swimmerDao.insertSwimmer(swimmer)

    suspend fun updateSwimmer(swimmer: SwimmerEntity) =
        swimmerDao.updateSwimmer(swimmer)

    suspend fun deleteSwimmer(swimmer: SwimmerEntity) =
        swimmerDao.deleteSwimmer(swimmer)

    @Suppress("unused")
    suspend fun deleteSwimmerById(id: Long) =
        swimmerDao.deleteSwimmerById(id)

    suspend fun saveSessionWithLaps(
        session: SessionEntity,
        laps: List<LapRecordEntity>,
    ): Long = database.withTransaction {
        val sessionId = sessionDao.insertSession(session)
        val lapsWithSessionId = laps.map { it.copy(sessionId = sessionId) }
        if (lapsWithSessionId.isNotEmpty()) {
            lapRecordDao.insertLaps(lapsWithSessionId)
        }
        sessionId
    }

    suspend fun importSessionsWithLaps(sessionsWithLaps: List<SessionWithLaps>) = database.withTransaction {
        sessionsWithLaps.forEach { (session, laps) ->
            // If id is 0, insert generates a new ID. If id > 0, REPLACE strategy upserts the session.
            val sessionToInsert = if (session.id > 0) session else session.copy(id = 0)
            val newSessionId = sessionDao.insertSession(sessionToInsert)
            val lapsWithNewId = laps.map { it.copy(id = if (session.id > 0) it.id else 0, sessionId = newSessionId) }
            if (lapsWithNewId.isNotEmpty()) {
                lapRecordDao.insertLaps(lapsWithNewId)
            }
        }
    }

    suspend fun deleteSessionById(sessionId: Long) = database.withTransaction {
        lapRecordDao.deleteLapsForSession(sessionId)
        sessionDao.deleteSessionById(sessionId)
    }
}
