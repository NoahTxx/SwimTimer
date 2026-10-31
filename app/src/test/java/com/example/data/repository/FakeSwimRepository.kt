package com.example.data.repository

import com.example.data.model.LapRecordEntity
import com.example.data.model.SessionEntity
import com.example.data.model.SessionWithLaps
import com.example.data.model.SwimmerEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

class FakeSwimRepository : ISwimRepository {

    private val swimmerIdGen = AtomicLong(1L)
    private val sessionIdGen = AtomicLong(1L)
    private val lapIdGen = AtomicLong(1L)

    private val _swimmers = MutableStateFlow<List<SwimmerEntity>>(emptyList())
    override val allSwimmers: Flow<List<SwimmerEntity>> = _swimmers.asStateFlow()

    private val _sessions = MutableStateFlow<List<SessionWithLaps>>(emptyList())
    override val allSessionsWithLaps: Flow<List<SessionWithLaps>> = _sessions.asStateFlow()

    override suspend fun allSessionsWithLapsSync(): List<SessionWithLaps> = _sessions.value

    override suspend fun getSessionWithLapsSync(sessionId: Long): SessionWithLaps? =
        _sessions.value.find { it.session.id == sessionId }

    override suspend fun getSwimmerById(id: Long): SwimmerEntity? =
        _swimmers.value.find { it.id == id }

    override suspend fun insertSwimmer(swimmer: SwimmerEntity): Long {
        val swimmerWithBase = if (swimmer.manualPbsJson == null) swimmer.withManualPbsSnapshot() else swimmer
        val assignedId = if (swimmerWithBase.id > 0) swimmerWithBase.id else swimmerIdGen.getAndIncrement()
        val newSwimmer = swimmerWithBase.copy(id = assignedId)
        _swimmers.value = _swimmers.value + newSwimmer
        return assignedId
    }

    override suspend fun updateSwimmer(swimmer: SwimmerEntity) {
        _swimmers.value = _swimmers.value.map { if (it.id == swimmer.id) swimmer else it }
    }

    override suspend fun deleteSwimmer(swimmer: SwimmerEntity) {
        _swimmers.value = _swimmers.value.filterNot { it.id == swimmer.id }
    }

    override suspend fun renameGroupForAllSwimmers(oldName: String, newName: String) {
        val target = oldName.trim()
        val replacement = newName.trim()
        _swimmers.value = _swimmers.value.map {
            if (it.groupName?.trim()?.equals(target, ignoreCase = true) == true) {
                it.copy(groupName = replacement)
            } else {
                it
            }
        }
    }

    override suspend fun clearGroupForAllSwimmers(groupName: String) {
        val target = groupName.trim()
        _swimmers.value = _swimmers.value.map {
            if (it.groupName?.trim()?.equals(target, ignoreCase = true) == true) {
                it.copy(groupName = null)
            } else {
                it
            }
        }
    }

    override suspend fun saveSessionWithLaps(
        session: SessionEntity,
        laps: List<LapRecordEntity>,
    ): Long {
        val assignedSessionId = if (session.id > 0) session.id else sessionIdGen.getAndIncrement()
        val savedSession = session.copy(id = assignedSessionId)
        val savedLaps = laps.map { lap ->
            val assignedLapId = if (lap.id > 0) lap.id else lapIdGen.getAndIncrement()
            lap.copy(id = assignedLapId, sessionId = assignedSessionId)
        }
        val sessionWithLaps = SessionWithLaps(session = savedSession, laps = savedLaps)
        _sessions.value = listOf(sessionWithLaps) + _sessions.value
        return assignedSessionId
    }

    override suspend fun importSessionsWithLaps(sessionsWithLaps: List<SessionWithLaps>) {
        sessionsWithLaps.forEach { (session, laps) ->
            saveSessionWithLaps(session, laps)
        }
    }

    override suspend fun deleteSessionById(sessionId: Long) {
        _sessions.value = _sessions.value.filterNot { it.session.id == sessionId }
    }

    // Helper for tests to pre-populate or reset
    fun setSwimmers(swimmers: List<SwimmerEntity>) {
        _swimmers.value = swimmers
    }
}
