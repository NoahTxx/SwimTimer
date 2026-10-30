package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.db.AppDatabase
import com.example.data.model.LapRecordEntity
import com.example.data.model.SessionEntity
import com.example.data.model.SessionWithLaps
import com.example.data.model.SwimmerEntity
import kotlinx.coroutines.flow.Flow

interface ISwimRepository {
    val allSwimmers: Flow<List<SwimmerEntity>>
    val allSessionsWithLaps: Flow<List<SessionWithLaps>>

    suspend fun allSessionsWithLapsSync(): List<SessionWithLaps>
    fun getSessionWithLaps(sessionId: Long): Flow<SessionWithLaps?>
    suspend fun getSessionWithLapsSync(sessionId: Long): SessionWithLaps?
    suspend fun getSwimmerById(id: Long): SwimmerEntity?
    suspend fun insertSwimmer(swimmer: SwimmerEntity): Long
    suspend fun updateSwimmer(swimmer: SwimmerEntity)
    suspend fun deleteSwimmer(swimmer: SwimmerEntity)
    suspend fun deleteSwimmerById(id: Long)
    suspend fun renameGroupForAllSwimmers(oldName: String, newName: String)
    suspend fun clearGroupForAllSwimmers(groupName: String)
    suspend fun saveSessionWithLaps(
        session: SessionEntity,
        laps: List<LapRecordEntity>,
    ): Long
    suspend fun importSessionsWithLaps(sessionsWithLaps: List<SessionWithLaps>)
    suspend fun deleteSessionById(sessionId: Long)
}

class SwimRepository(private val database: AppDatabase) : ISwimRepository {
    private val swimmerDao = database.swimmerDao()
    private val sessionDao = database.sessionDao()
    private val lapRecordDao = database.lapRecordDao()

    override val allSwimmers: Flow<List<SwimmerEntity>> = swimmerDao.getAllSwimmers()
    override val allSessionsWithLaps: Flow<List<SessionWithLaps>> = sessionDao.getAllSessionsWithLaps()

    override suspend fun allSessionsWithLapsSync(): List<SessionWithLaps> =
        sessionDao.getAllSessionsWithLapsSync()

    @Suppress("unused")
    override fun getSessionWithLaps(sessionId: Long): Flow<SessionWithLaps?> =
        sessionDao.getSessionWithLaps(sessionId)

    override suspend fun getSessionWithLapsSync(sessionId: Long): SessionWithLaps? =
        sessionDao.getSessionWithLapsSync(sessionId)

    override suspend fun getSwimmerById(id: Long): SwimmerEntity? =
        swimmerDao.getSwimmerByIdSync(id)

    override suspend fun insertSwimmer(swimmer: SwimmerEntity): Long =
        swimmerDao.insertSwimmer(swimmer)

    override suspend fun updateSwimmer(swimmer: SwimmerEntity) =
        swimmerDao.updateSwimmer(swimmer)

    override suspend fun deleteSwimmer(swimmer: SwimmerEntity) =
        swimmerDao.deleteSwimmer(swimmer)

    @Suppress("unused")
    override suspend fun deleteSwimmerById(id: Long) =
        swimmerDao.deleteSwimmerById(id)

    override suspend fun renameGroupForAllSwimmers(oldName: String, newName: String) =
        swimmerDao.renameGroupForAllSwimmers(oldName, newName)

    override suspend fun clearGroupForAllSwimmers(groupName: String) =
        swimmerDao.clearGroupForAllSwimmers(groupName)

    override suspend fun saveSessionWithLaps(
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

    override suspend fun importSessionsWithLaps(sessionsWithLaps: List<SessionWithLaps>) = database.withTransaction {
        val existingSwimmers = swimmerDao.getAllSwimmersSync()
        val swimmerMapByNormalizedName = existingSwimmers
            .associateBy { it.name.trim().lowercase() }
            .toMutableMap()

        sessionsWithLaps.forEach { (session, laps) ->
            val newSessionId = sessionDao.insertSession(session.copy(id = 0))

            val lapsWithNewIdAndResolvedSwimmers = laps.map { lap ->
                var resolvedSwimmerId = lap.swimmerId

                // Extract clean athlete name if formatted as relay leg (e.g. "Team • 1. Lukas" -> "Lukas")
                val rawSwimmerName = lap.swimmerName.trim()
                val extractedName = if (rawSwimmerName.contains(" • ")) {
                    rawSwimmerName.substringAfterLast(" • ")
                        .replaceFirst(Regex("""^\d+\.\s*"""), "")
                        .substringBefore(" (")
                        .trim()
                } else {
                    rawSwimmerName
                }

                // Check whether this is a placeholder or generic lane name
                val isPlaceholder = extractedName.isBlank() ||
                        extractedName.equals("Unbekannt", ignoreCase = true) ||
                        extractedName.matches(Regex("""^(Bahn|Lane)\s*\d+$""", RegexOption.IGNORE_CASE)) ||
                        extractedName.matches(Regex("""^Staffel\s+Bahn\s*\d+$""", RegexOption.IGNORE_CASE)) ||
                        extractedName.matches(Regex("""^Starter\s*\d+$""", RegexOption.IGNORE_CASE))

                if (!isPlaceholder) {
                    val normalizedName = extractedName.lowercase()
                    val matchedSwimmer = swimmerMapByNormalizedName[normalizedName]
                    if (matchedSwimmer != null) {
                        resolvedSwimmerId = matchedSwimmer.id
                    } else {
                        val newSwimmer = SwimmerEntity(id = 0, name = extractedName)
                        val newSwimmerId = swimmerDao.insertSwimmer(newSwimmer)
                        val createdEntity = newSwimmer.copy(id = newSwimmerId)
                        swimmerMapByNormalizedName[normalizedName] = createdEntity
                        resolvedSwimmerId = newSwimmerId
                    }
                } else if ((resolvedSwimmerId != null) && (resolvedSwimmerId > 0)) {
                    val swimmerExists = existingSwimmers.any { it.id == resolvedSwimmerId }
                    if (!swimmerExists) {
                        resolvedSwimmerId = null
                    }
                } else {
                    resolvedSwimmerId = null
                }

                val finalLap = if (resolvedSwimmerId != null) {
                    lap.copy(sessionId = newSessionId, swimmerId = resolvedSwimmerId)
                } else {
                    lap.copy(sessionId = newSessionId, swimmerId = null)
                }
                finalLap
            }

            if (lapsWithNewIdAndResolvedSwimmers.isNotEmpty()) {
                lapRecordDao.insertLaps(lapsWithNewIdAndResolvedSwimmers)
            }
        }
    }

    override suspend fun deleteSessionById(sessionId: Long) = database.withTransaction {
        lapRecordDao.deleteLapsForSession(sessionId)
        sessionDao.deleteSessionById(sessionId)
    }
}
