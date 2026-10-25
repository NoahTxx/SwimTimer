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

    suspend fun renameGroupForAllSwimmers(oldName: String, newName: String) =
        swimmerDao.renameGroupForAllSwimmers(oldName, newName)

    suspend fun clearGroupForAllSwimmers(groupName: String) =
        swimmerDao.clearGroupForAllSwimmers(groupName)

    suspend fun importSessionsWithLaps(sessionsWithLaps: List<SessionWithLaps>) = database.withTransaction {
        val existingSwimmers = swimmerDao.getAllSwimmersSync()
        val swimmerMapByNormalizedName = existingSwimmers
            .associateBy { it.name.trim().lowercase() }
            .toMutableMap()

        sessionsWithLaps.forEach { (session, laps) ->
            // Speichere jede zu importierende Session zwingend als neuen Eintrag mit id = 0
            val newSessionId = sessionDao.insertSession(session.copy(id = 0))

            val lapsWithNewIdAndResolvedSwimmers = laps.map { lap ->
                var resolvedSwimmerId = lap.swimmerId

                val swimmerName = lap.swimmerName.trim()
                if (swimmerName.isNotEmpty()) {
                    val normalizedName = swimmerName.lowercase()
                    val matchedSwimmer = swimmerMapByNormalizedName[normalizedName]
                    if (matchedSwimmer != null) {
                        resolvedSwimmerId = matchedSwimmer.id
                    } else {
                        // Schwimmer neu anlegen und dessen neue DB-ID nutzen
                        val newSwimmer = SwimmerEntity(id = 0, name = swimmerName)
                        val newSwimmerId = swimmerDao.insertSwimmer(newSwimmer)
                        val createdEntity = newSwimmer.copy(id = newSwimmerId)
                        swimmerMapByNormalizedName[normalizedName] = createdEntity
                        resolvedSwimmerId = newSwimmerId
                    }
                } else if (resolvedSwimmerId != null && resolvedSwimmerId > 0) {
                    val swimmerExists = existingSwimmers.any { it.id == resolvedSwimmerId }
                    if (!swimmerExists) {
                        resolvedSwimmerId = null
                    }
                }

                // Laps zwingend neue Session ID und id = 0 zuweisen
                lap.copy(
                    id = 0,
                    sessionId = newSessionId,
                    swimmerId = resolvedSwimmerId
                )
            }

            if (lapsWithNewIdAndResolvedSwimmers.isNotEmpty()) {
                lapRecordDao.insertLaps(lapsWithNewIdAndResolvedSwimmers)
            }
        }
    }

    suspend fun deleteSessionById(sessionId: Long) = database.withTransaction {
        lapRecordDao.deleteLapsForSession(sessionId)
        sessionDao.deleteSessionById(sessionId)
    }
}
