package com.example.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.LapRecordEntity
import com.example.data.model.SessionEntity
import com.example.data.model.SwimmerEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppDatabaseTest {

    private lateinit var database: AppDatabase
    private lateinit var swimmerDao: SwimmerDao
    private lateinit var sessionDao: SessionDao
    private lateinit var lapRecordDao: LapRecordDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        swimmerDao = database.swimmerDao()
        sessionDao = database.sessionDao()
        lapRecordDao = database.lapRecordDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndGetSwimmer() = runBlocking {
        val swimmer = SwimmerEntity(name = "Caeleb Dressel", groupName = "Sprint", pb50mMillis = 21040L)
        val id = swimmerDao.insertSwimmer(swimmer)
        assertTrue(id > 0)

        val retrieved = swimmerDao.getSwimmerByIdSync(id)
        assertNotNull(retrieved)
        assertEquals("Caeleb Dressel", retrieved?.name)
        assertEquals("Sprint", retrieved?.groupName)
        assertEquals(21040L, retrieved?.pb50mMillis)
    }

    @Test
    fun updateSwimmer() = runBlocking {
        val swimmer = SwimmerEntity(name = "Leon Marchand", groupName = "IM")
        val id = swimmerDao.insertSwimmer(swimmer)

        val updated = swimmer.copy(id = id, pb100mMillis = 47800L)
        swimmerDao.updateSwimmer(updated)

        val retrieved = swimmerDao.getSwimmerByIdSync(id)
        assertEquals(47800L, retrieved?.pb100mMillis)
    }

    @Test
    fun deleteSwimmer() = runBlocking {
        val swimmer = SwimmerEntity(name = "Temp Swimmer")
        val id = swimmerDao.insertSwimmer(swimmer)
        assertNotNull(swimmerDao.getSwimmerByIdSync(id))

        swimmerDao.deleteSwimmer(swimmer.copy(id = id))
        assertNull(swimmerDao.getSwimmerByIdSync(id))
    }

    @Test
    fun renameGroupForAllSwimmers() = runBlocking {
        swimmerDao.insertSwimmer(SwimmerEntity(name = "Swimmer A", groupName = "Juniors"))
        swimmerDao.insertSwimmer(SwimmerEntity(name = "Swimmer B", groupName = "Juniors"))
        swimmerDao.insertSwimmer(SwimmerEntity(name = "Swimmer C", groupName = "Seniors"))

        swimmerDao.renameGroupForAllSwimmers(oldName = "Juniors", newName = "Team Red")

        val all = swimmerDao.getAllSwimmersSync()
        val redGroup = all.filter { it.groupName == "Team Red" }
        assertEquals(2, redGroup.size)
    }

    @Test
    fun insertSessionWithLapsAndQueryRelations() = runBlocking {
        val swimmerId = swimmerDao.insertSwimmer(SwimmerEntity(name = "Adam Peaty", groupName = "Breaststroke"))
        val session = SessionEntity(title = "Morning Test", timestamp = 1000L, poolLengthMeters = 50)
        val sessionId = sessionDao.insertSession(session)

        val lap1 = LapRecordEntity(
            sessionId = sessionId,
            swimmerId = swimmerId,
            swimmerName = "Adam Peaty",
            laneNumber = 1,
            lapIndex = 1,
            lapTimeMillis = 26500L,
            splitTimeMillis = 26500L
        )
        val lap2 = LapRecordEntity(
            sessionId = sessionId,
            swimmerId = swimmerId,
            swimmerName = "Adam Peaty",
            laneNumber = 1,
            lapIndex = 2,
            lapTimeMillis = 31200L,
            splitTimeMillis = 57700L
        )
        lapRecordDao.insertLaps(listOf(lap1, lap2))

        val sessionWithLaps = sessionDao.getSessionWithLapsSync(sessionId)
        assertNotNull(sessionWithLaps)
        assertEquals("Morning Test", sessionWithLaps?.session?.title)
        assertEquals(2, sessionWithLaps?.laps?.size)
        assertEquals(57700L, sessionWithLaps?.laps?.get(1)?.splitTimeMillis)
    }

    @Test
    fun deleteSessionAndLaps() = runBlocking {
        val sessionId = sessionDao.insertSession(SessionEntity(title = "To Delete", timestamp = 2000L))
        lapRecordDao.insertLaps(listOf(
            LapRecordEntity(
                sessionId = sessionId,
                swimmerName = "Unknown",
                laneNumber = 1,
                lapIndex = 1,
                lapTimeMillis = 10000L,
                splitTimeMillis = 10000L
            )
        ))

        lapRecordDao.deleteLapsForSession(sessionId)
        sessionDao.deleteSessionById(sessionId)

        assertNull(sessionDao.getSessionWithLapsSync(sessionId))
    }
}
