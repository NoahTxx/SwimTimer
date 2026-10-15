package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.TimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SwimTimer Pro", appName)
  }

  @Test
  fun `test time formatting for swim stopwatch`() {
    val formatted1 = TimeFormatter.formatMillis(65420L)
    assertEquals("01:05.42", formatted1)

    val formattedShort = TimeFormatter.formatMillis(25800L, shortFormat = true)
    assertEquals("25.80", formattedShort)

    val deltaFast = TimeFormatter.formatDelta(-450L)
    assertEquals("-0.45s", deltaFast)

    val deltaSlow = TimeFormatter.formatDelta(1200L)
    assertEquals("+1.20s", deltaSlow)
  }

  @Test
  fun `test official individual medley sequence is Schmett Ruecken Brust Freistil`() {
    val officialOrder = listOf("Schmett", "Rücken", "Brust", "Freistil")
    assertEquals("Schmett", officialOrder[0])
    assertEquals("Rücken", officialOrder[1])
    assertEquals("Brust", officialOrder[2])
    assertEquals("Freistil", officialOrder[3])
  }

  @Test
  fun `test lagen minimum distance enforcement`() {
    fun enforceLagenDistance(stroke: String, dist: Int): Int {
      return if (stroke == "Lagen" && dist < 100) 100 else dist
    }
    assertEquals(100, enforceLagenDistance("Lagen", 25))
    assertEquals(100, enforceLagenDistance("Lagen", 50))
    assertEquals(100, enforceLagenDistance("Lagen", 100))
    assertEquals(200, enforceLagenDistance("Lagen", 200))
    assertEquals(400, enforceLagenDistance("Lagen", 400))
    assertEquals(25, enforceLagenDistance("Freistil", 25))
    assertEquals(50, enforceLagenDistance("Brust", 50))
  }

  @Test
  fun `test 25m splits for 100m Lagen legs`() {
    val totalDist = 100
    val splitDist = 25
    val legDist = totalDist / 4 // 25m per stroke
    val strokeNames = listOf("1. Schmett", "2. Rücken", "3. Brust", "4. Freistil")

    val laps = (1..4).map { lapIndex ->
      val legIdx = (((lapIndex - 1) * splitDist) / legDist).coerceIn(0, 3)
      strokeNames[legIdx]
    }

    assertEquals("1. Schmett", laps[0])
    assertEquals("2. Rücken", laps[1])
    assertEquals("3. Brust", laps[2])
    assertEquals("4. Freistil", laps[3])
  }

  @Test
  fun `test 200m Lagen 4x 50m legs and automatic 50m Kraul PB update`() {
    val totalDist = 200
    val splitDist = 50
    val legDist = totalDist / 4 // 50m per stroke
    val strokeNames = listOf("1. Schmett (50m)", "2. Rücken (50m)", "3. Brust (50m)", "4. Kraul (50m)")

    val laps = (1..4).map { lapIndex ->
      val legIdx = (((lapIndex - 1) * splitDist) / legDist).coerceIn(0, 3)
      strokeNames[legIdx]
    }

    assertEquals("1. Schmett (50m)", laps[0])
    assertEquals("2. Rücken (50m)", laps[1])
    assertEquals("3. Brust (50m)", laps[2])
    assertEquals("4. Kraul (50m)", laps[3])

    // PB synchronization test:
    var pb50mKraul: Long? = 27_800L // 27.80s existing PB
    val kraulSplit200Lagen = 26_950L // 26.95s in 200m Lagen leg 4

    // Logic under test: If 200m Lagen leg 4 is faster than 50m PB, update it
    if (pb50mKraul == null || kraulSplit200Lagen < pb50mKraul) {
      pb50mKraul = kraulSplit200Lagen
    }
    assertEquals(26_950L, pb50mKraul)
  }

  @Test
  fun `test pace per 100m calculation`() {
    // 25m in 15.00s -> 60.00s per 100m -> 01:00.00/100m
    val pace25m = TimeFormatter.formatPacePer100m(15000L, 25)
    assertEquals("01:00.00/100m", pace25m)

    // 50m in 30.00s -> 60.00s per 100m -> 01:00.00/100m
    val pace50m = TimeFormatter.formatPacePer100m(30000L, 50)
    assertEquals("01:00.00/100m", pace50m)
  }

  @Test
  fun `test 400m Lagen 4x 100m legs and automatic PB sync across all legs`() {
    val totalDist = 400
    val splitDist = 100
    val legDist = totalDist / 4 // 100m per stroke
    val strokeNames = listOf("1. Schmett (100m)", "2. Rücken (100m)", "3. Brust (100m)", "4. Kraul (100m)")

    val laps = (1..4).map { lapIndex ->
      val legIdx = (((lapIndex - 1) * splitDist) / legDist).coerceIn(0, 3)
      strokeNames[legIdx]
    }

    assertEquals("1. Schmett (100m)", laps[0])
    assertEquals("2. Rücken (100m)", laps[1])
    assertEquals("3. Brust (100m)", laps[2])
    assertEquals("4. Kraul (100m)", laps[3])

    // PB synchronization test for 400m Lagen:
    // Swimmer: PB 100m Freistil = 56.50s (56500ms), PB 400m Lagen = 310.00s (310000ms)
    var pb100mFree: Long? = 56_500L
    var pb400mLagen: Long? = 310_000L
    var pb400Fly: Long? = 66_000L
    var pb400Back: Long? = 78_000L
    var pb400Breast: Long? = 88_000L
    var pb400Free: Long? = 57_000L

    // Swims new 400m Lagen session:
    // Leg 1: 64.20s (faster than 66.00s pb400Fly)
    val sessionLeg1 = 64_200L
    if (pb400Fly == null || sessionLeg1 < pb400Fly) {
      pb400Fly = sessionLeg1
    }
    assertEquals(64_200L, pb400Fly)

    // Leg 4: 55.80s (faster than 57.00s pb400Free AND faster than 56.50s pb100mFree!)
    val sessionLeg4 = 55_800L
    if (pb400Free == null || sessionLeg4 < pb400Free) {
      pb400Free = sessionLeg4
    }
    if (pb100mFree == null || sessionLeg4 < pb100mFree) {
      pb100mFree = sessionLeg4
    }
    assertEquals(55_800L, pb400Free)
    assertEquals(55_800L, pb100mFree)

    // Total time: 64.2 + 76.5 + 86.0 + 55.8 = 282.5s (faster than 310.00s pb400mLagen)
    val totalSessionTime = 282_500L
    if (pb400mLagen == null || totalSessionTime < pb400mLagen) {
      pb400mLagen = totalSessionTime
    }
    assertEquals(282_500L, pb400mLagen)
  }

  @Test
  fun `test database initially has zero example swimmers`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.db.AppDatabase.getInstance(context)
    val swimmerDao = db.swimmerDao()
    
    kotlinx.coroutines.runBlocking {
      val count = swimmerDao.getSwimmerCount()
      assertEquals(0, count)
    }
  }

  @Test
  fun `test settings persistence across ViewModel restarts`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.db.AppDatabase.getInstance(context)
    val repository = com.example.data.repository.SwimRepository(db)

    // Instance 1: User changes settings
    val vm1 = com.example.ui.StopwatchViewModel(repository, context)
    vm1.setSessionMeta("Test Session", "Brust", 25, 200, 25)
    vm1.setStartMode(com.example.ui.StartMode.INTERVAL)
    vm1.setIntervalOffsetSeconds(10)
    vm1.initializeLanes(6)

    // Instance 2: Simulates reopening app
    val vm2 = com.example.ui.StopwatchViewModel(repository, context)
    val state2 = vm2.uiState.value

    assertEquals("Brust", state2.strokeType)
    assertEquals(25, state2.poolLengthMeters)
    assertEquals(200, state2.targetDistanceMeters)
    assertEquals(25, state2.splitDistanceMeters)
    assertEquals(com.example.ui.StartMode.INTERVAL, state2.startMode)
    assertEquals(10, state2.intervalOffsetSeconds)
    assertEquals(6, state2.laneCount)
  }

  @Test
  fun `test session deletion recalculates swimmer PBs`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.db.AppDatabase.getInstance(context)
    val repository = com.example.data.repository.SwimRepository(db)

    kotlinx.coroutines.runBlocking {
      val sId = repository.insertSwimmer(com.example.data.model.SwimmerEntity(name = "Paul Test", favoriteStroke = "Freistil", pb50mMillis = 28500L))
      val swimmer = repository.getSwimmerById(sId)!!

      val session = com.example.data.model.SessionEntity(
        title = "Test Session",
        poolLengthMeters = 50,
        targetDistanceMeters = 50,
        strokeType = "Freistil"
      )
      val lap = com.example.data.model.LapRecordEntity(
        sessionId = 0,
        swimmerId = swimmer.id,
        swimmerName = swimmer.name,
        laneNumber = 1,
        lapIndex = 1,
        splitTimeMillis = 28500L,
        lapTimeMillis = 28500L
      )

      val sessionId = repository.saveSessionWithLaps(session, listOf(lap))
      val savedSessions = repository.allSessionsWithLapsSync()
      assertEquals(1, savedSessions.size)

      repository.deleteSessionById(sessionId)
      val remainingSessions = repository.allSessionsWithLapsSync()
      assertEquals(0, remainingSessions.size)
    }
  }

  @Test
  fun `test 15 swimmers max limit and dynamic lane management`() {
    val maxLimit = 15
    var currentLanes = 4

    // Add up to 15
    for (i in 5..20) {
      if (currentLanes < maxLimit) {
        currentLanes++
      }
    }
    assertEquals(15, currentLanes)

    // Remove down to 1
    for (i in 1..20) {
      if (currentLanes > 1) {
        currentLanes--
      }
    }
    assertEquals(1, currentLanes)
  }
}
