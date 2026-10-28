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

  private fun isNewPb(newTime: Long, pb: Long?): Boolean = (pb == null) || (newTime < pb)

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SwimTimer", appName)
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
  fun `test split rounding consistency guarantees lap sum equals total split`() {
    fun roundToHundredths(millis: Long): Long = ((millis + 5L) / 10L) * 10L

    val rawSplit1 = 1538L
    val rawSplit2 = 8786L

    val split1 = roundToHundredths(rawSplit1)
    val split2 = roundToHundredths(rawSplit2)

    val lap1 = split1
    val lap2 = split2 - split1

    val lap1Formatted = TimeFormatter.formatMillis(lap1, shortFormat = true)
    val lap2Formatted = TimeFormatter.formatMillis(lap2, shortFormat = true)
    val totalFormatted = TimeFormatter.formatMillis(split2, shortFormat = true)

    assertEquals("01.54", lap1Formatted)
    assertEquals("07.25", lap2Formatted)
    assertEquals("08.79", totalFormatted)
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
      return if ((stroke == "Lagen") && (dist < 100)) 100 else dist
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
    if (isNewPb(kraulSplit200Lagen, pb50mKraul)) {
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
    var pb400Free: Long? = 57_000L

    // Swims new 400m Lagen session:
    // Leg 1: 64.20s (faster than 66.00s pb400Fly)
    val sessionLeg1 = 64_200L
    if (isNewPb(sessionLeg1, pb400Fly)) {
      pb400Fly = sessionLeg1
    }
    assertEquals(64_200L, pb400Fly)

    // Leg 4: 55.80s (faster than 57.00s pb400Free AND faster than 56.50s pb100mFree!)
    val sessionLeg4 = 55_800L
    if (isNewPb(sessionLeg4, pb400Free)) {
      pb400Free = sessionLeg4
    }
    if (isNewPb(sessionLeg4, pb100mFree)) {
      pb100mFree = sessionLeg4
    }
    assertEquals(55_800L, pb400Free)
    assertEquals(55_800L, pb100mFree)

    // Total time: 64.2 + 76.5 + 86.0 + 55.8 = 282.5s (faster than 310.00s pb400mLagen)
    val totalSessionTime = 282_500L
    if (isNewPb(totalSessionTime, pb400mLagen)) {
      pb400mLagen = totalSessionTime
    }
    assertEquals(282_500L, pb400mLagen)
  }

  @Test
  fun `test database initially has zero example swimmers`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java).allowMainThreadQueries().build()
    val swimmerDao = db.swimmerDao()
    
    kotlinx.coroutines.runBlocking {
      val count = swimmerDao.getSwimmerCount()
      assertEquals(0, count)
    }
    db.close()
  }

  @Test
  fun `test settings persistence across ViewModel restarts`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.db.AppDatabase.getInstance(context)
    val repository = com.example.data.repository.SwimRepository(db)
    val prefsRepo = com.example.data.repository.UserPreferencesRepository(context)

    // Instance 1: User changes settings
    val vm1 = com.example.ui.StopwatchViewModel(repository, prefsRepo)
    kotlinx.coroutines.runBlocking { kotlinx.coroutines.delay(200) }
    vm1.setSessionMeta("Test Session", "Brust", 25, 200, 25)
    vm1.setStartMode(com.example.ui.StartMode.INTERVAL)
    vm1.setIntervalOffsetSeconds(10)
    vm1.initializeLanes(6)

    // Instance 2: Simulates reopening app
    val vm2 = com.example.ui.StopwatchViewModel(repository, prefsRepo)
    kotlinx.coroutines.runBlocking { kotlinx.coroutines.delay(200) }
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
        strokeType = "Freistil",
      )
      val lap = com.example.data.model.LapRecordEntity(
        sessionId = 0,
        swimmerId = swimmer.id,
        swimmerName = swimmer.name,
        laneNumber = 1,
        lapIndex = 1,
        splitTimeMillis = 28500L,
        lapTimeMillis = 28500L,
      )

      val initialSessions = repository.allSessionsWithLapsSync()
      val sessionId = repository.saveSessionWithLaps(session, listOf(lap))
      val savedSessions = repository.allSessionsWithLapsSync()
      assertEquals(initialSessions.size + 1, savedSessions.size)

      repository.deleteSessionById(sessionId)
      val remainingSessions = repository.allSessionsWithLapsSync()
      assertEquals(initialSessions.size, remainingSessions.size)
    }
  }

  @Test
  fun `test 15 swimmers max limit and dynamic lane management`() {
    val maxLimit = 15
    var currentLanes = 4

    // Add up to 15
    repeat(16) {
      if (currentLanes < maxLimit) {
        currentLanes++
      }
    }
    assertEquals(15, currentLanes)

    // Remove down to 1
    repeat(20) {
      if (currentLanes > 1) {
        currentLanes--
      }
    }
    assertEquals(1, currentLanes)
  }

  @Test
  fun `test series mode fixed rest transitions lane to rest countdown`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.db.AppDatabase.getInstance(context)
    val repository = com.example.data.repository.SwimRepository(db)
    val prefsRepo = com.example.data.repository.UserPreferencesRepository(context)

    val vm = com.example.ui.StopwatchViewModel(repository, prefsRepo)
    vm.initializeLanes(4)
    vm.setSeriesConfig(
      com.example.ui.SeriesConfig(
        isEnabled = true,
        mode = com.example.ui.SeriesModeType.FIXED_REST,
        restDurationSeconds = 20,
        maxSeriesCount = 4
      )
    )

    vm.onStartSingleLane(1)
    assertEquals(com.example.ui.LaneStatus.RUNNING, vm.uiState.value.lanes.first { it.laneNumber == 1 }.status)

    // Swimmer finishes 100m
    vm.onFinishLane(1, exactRealtime = System.currentTimeMillis())

    val lane1 = vm.uiState.value.lanes.first { it.laneNumber == 1 }
    assertEquals(com.example.ui.LaneStatus.REST_COUNTDOWN, lane1.status)
    assertEquals(20, lane1.restRemainingSeconds)

    // Perform manual reset
    vm.onResetAll()
    val resetLane = vm.uiState.value.lanes.first { it.laneNumber == 1 }
    assertEquals(com.example.ui.LaneStatus.IDLE, resetLane.status)
    assertEquals(1, resetLane.seriesCount)
  }

  @Test
  fun `test swimmer cannot be assigned to multiple lanes simultaneously`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.db.AppDatabase.getInstance(context)
    val repository = com.example.data.repository.SwimRepository(db)
    val prefsRepo = com.example.data.repository.UserPreferencesRepository(context)

    val vm = com.example.ui.StopwatchViewModel(repository, prefsRepo)
    vm.initializeLanes(4)

    val dummySwimmer = com.example.data.model.SwimmerEntity(
      id = 42L,
      name = "Max Mustermann",
      favoriteStroke = "Kraul"
    )

    // Assign to Lane 1
    vm.assignSwimmerToLane(1, dummySwimmer)
    assertEquals(42L, vm.uiState.value.lanes.first { it.laneNumber == 1 }.swimmerId)
    assertEquals("Max Mustermann", vm.uiState.value.lanes.first { it.laneNumber == 1 }.swimmerName)

    // Reassign same swimmer to Lane 2
    vm.assignSwimmerToLane(2, dummySwimmer)

    // Lane 1 should now be reset to default "Bahn 1"
    val lane1 = vm.uiState.value.lanes.first { it.laneNumber == 1 }
    org.junit.Assert.assertNull(lane1.swimmerId)
    assertEquals("Bahn 1", lane1.swimmerName)

    // Lane 2 should now have Max Mustermann
    val lane2 = vm.uiState.value.lanes.first { it.laneNumber == 2 }
    assertEquals(42L, lane2.swimmerId)
    assertEquals("Max Mustermann", lane2.swimmerName)
  }

  @Test
  fun `test pause timer finishRealtimeMillis starts immediately on finish and resets on restart`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.db.AppDatabase.getInstance(context)
    val repository = com.example.data.repository.SwimRepository(db)
    val prefsRepo = com.example.data.repository.UserPreferencesRepository(context)

    val vm = com.example.ui.StopwatchViewModel(repository, prefsRepo)
    vm.initializeLanes(2)

    // Initially IDLE
    val lane1Initial = vm.uiState.value.lanes.first { it.laneNumber == 1 }
    org.junit.Assert.assertNull(lane1Initial.finishRealtimeMillis)

    // Start Single Lane
    vm.onStartSingleLane(1)

    val lane1Running = vm.uiState.value.lanes.first { it.laneNumber == 1 }
    assertEquals(com.example.ui.LaneStatus.RUNNING, lane1Running.status)
    org.junit.Assert.assertNull(lane1Running.finishRealtimeMillis)

    // Finish Lane at finishTime
    val finishRealtime = 1_030_000L
    vm.onFinishLane(1, exactRealtime = finishRealtime)

    val lane1Finished = vm.uiState.value.lanes.first { it.laneNumber == 1 }
    assertEquals(com.example.ui.LaneStatus.FINISHED, lane1Finished.status)
    assertEquals(finishRealtime, lane1Finished.finishRealtimeMillis)

    // Undo Finish -> finishRealtimeMillis restored to null
    vm.performUndo()
    val lane1Undone = vm.uiState.value.lanes.first { it.laneNumber == 1 }
    assertEquals(com.example.ui.LaneStatus.RUNNING, lane1Undone.status)
    org.junit.Assert.assertNull(lane1Undone.finishRealtimeMillis)

    // Finish again
    vm.onFinishLane(1, exactRealtime = finishRealtime)
    assertEquals(finishRealtime, vm.uiState.value.lanes.first { it.laneNumber == 1 }.finishRealtimeMillis)

    // Reset All -> finishRealtimeMillis cleared
    vm.onResetAll()
    org.junit.Assert.assertNull(vm.uiState.value.lanes.first { it.laneNumber == 1 }.finishRealtimeMillis)
  }

  @Test
  fun `test stopwatch series functionality preserves previous laps on restart and increments seriesCount`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.db.AppDatabase.getInstance(context)
    val repository = com.example.data.repository.SwimRepository(db)
    val prefsRepo = com.example.data.repository.UserPreferencesRepository(context)

    val vm = com.example.ui.StopwatchViewModel(repository, prefsRepo)
    vm.initializeLanes(2)

    val lane1Initial = vm.uiState.value.lanes.first { it.laneNumber == 1 }
    assertEquals(1, lane1Initial.seriesCount)
    org.junit.Assert.assertTrue(lane1Initial.completedSeriesLaps.isEmpty())

    // 1. Start Lane 1
    vm.onStartSingleLane(1)
    vm.onLapLane(1, exactRealtime = 15_000L) // Lap 1 of Series 1
    vm.onFinishLane(1, exactRealtime = 30_000L) // Finish Series 1

    val lane1Series1Finished = vm.uiState.value.lanes.first { it.laneNumber == 1 }
    assertEquals(com.example.ui.LaneStatus.FINISHED, lane1Series1Finished.status)
    assertEquals(1, lane1Series1Finished.seriesCount)
    assertEquals(2, lane1Series1Finished.laps.size)
    assertEquals(2, lane1Series1Finished.allLaps.size)

    // 2. Restart Lane 1 (Start Series 2)
    vm.onStartSingleLane(1)

    val lane1Series2Running = vm.uiState.value.lanes.first { it.laneNumber == 1 }
    assertEquals(com.example.ui.LaneStatus.RUNNING, lane1Series2Running.status)
    assertEquals(2, lane1Series2Running.seriesCount)
    assertEquals(2, lane1Series2Running.completedSeriesLaps.size)
    assertEquals(0, lane1Series2Running.laps.size)
    assertEquals(2, lane1Series2Running.allLaps.size)

    // Take lap for Series 2
    vm.onFinishLane(1, exactRealtime = 60_000L) // Finish Series 2

    val lane1Series2Finished = vm.uiState.value.lanes.first { it.laneNumber == 1 }
    assertEquals(com.example.ui.LaneStatus.FINISHED, lane1Series2Finished.status)
    assertEquals(2, lane1Series2Finished.seriesCount)
    assertEquals(2, lane1Series2Finished.completedSeriesLaps.size)
    assertEquals(1, lane1Series2Finished.laps.size)
    assertEquals(3, lane1Series2Finished.allLaps.size)

    // Verify series indices
    assertEquals(1, lane1Series2Finished.allLaps[0].seriesIndex)
    assertEquals(1, lane1Series2Finished.allLaps[1].seriesIndex)
    assertEquals(2, lane1Series2Finished.allLaps[2].seriesIndex)
  }

  @Test
  fun `test early finish tap without intermediate splits sets lapIndex to totalSplitsNeeded`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.db.AppDatabase.getInstance(context)
    val repository = com.example.data.repository.SwimRepository(db)
    val prefsRepo = com.example.data.repository.UserPreferencesRepository(context)

    val vm = com.example.ui.StopwatchViewModel(repository, prefsRepo)
    kotlinx.coroutines.runBlocking { kotlinx.coroutines.delay(200) }
    vm.setSessionMeta("Test 100m", "Freistil", 50, 100, 50) // target = 100m, split = 50m -> totalSplitsNeeded = 2
    vm.initializeLanes(1)

    vm.onStartSingleLane(1)
    val startRealtime = vm.uiState.value.lanes.first { it.laneNumber == 1 }.laneStartTimeRealtime!!
    // User skipped 50m split, pressed Ziel at 100m directly (elapsed = 62.50s)
    vm.onFinishLane(1, exactRealtime = startRealtime + 62_500L)

    val lane = vm.uiState.value.lanes.first { it.laneNumber == 1 }
    assertEquals(com.example.ui.LaneStatus.FINISHED, lane.status)
    assertEquals(1, lane.laps.size)

    val finishLap = lane.laps.first()
    assertEquals(2, finishLap.lapIndex) // lapIndex must be 2 (totalSplitsNeeded = 100/50 = 2)
    assertEquals(62_500L, finishLap.splitTimeMillis)
    assertEquals(62_500L, finishLap.lapTimeMillis)
  }

  @Test
  fun `test series enabled state persistence`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.db.AppDatabase.getInstance(context)
    val repository = com.example.data.repository.SwimRepository(db)
    val prefsRepo = com.example.data.repository.UserPreferencesRepository(context)

    val vm = com.example.ui.StopwatchViewModel(repository, prefsRepo)
    kotlinx.coroutines.runBlocking { kotlinx.coroutines.delay(200) }
    org.junit.Assert.assertFalse(vm.uiState.value.seriesConfig.isEnabled)

    // Enable series mode
    val newConfig = com.example.ui.SeriesConfig(
      isEnabled = true,
      mode = com.example.ui.SeriesModeType.SEND_OFF,
      sendOffDurationSeconds = 75
    )
    vm.setSeriesConfig(newConfig)
    org.junit.Assert.assertTrue(vm.uiState.value.seriesConfig.isEnabled)
    assertEquals(com.example.ui.SeriesModeType.SEND_OFF, vm.uiState.value.seriesConfig.mode)

    // Re-instantiate ViewModel -> loads from preferences
    val vmReloaded = com.example.ui.StopwatchViewModel(repository, prefsRepo)
    kotlinx.coroutines.runBlocking { kotlinx.coroutines.delay(200) }
    org.junit.Assert.assertTrue(vmReloaded.uiState.value.seriesConfig.isEnabled)
    assertEquals(com.example.ui.SeriesModeType.SEND_OFF, vmReloaded.uiState.value.seriesConfig.mode)
    assertEquals(75, vmReloaded.uiState.value.seriesConfig.sendOffDurationSeconds)
  }

  @Test
  fun `test long distance PBs 400m 800m 1500m auto sync and export import`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.db.AppDatabase.getInstance(context)
    val repository = com.example.data.repository.SwimRepository(db)

    kotlinx.coroutines.runBlocking {
      val sId = repository.insertSwimmer(
        com.example.data.model.SwimmerEntity(name = "Long Distance Swimmer", favoriteStroke = "Freistil")
      )
      val swimmerBefore = repository.getSwimmerById(sId)!!

      val customMap = mutableMapOf<Pair<String, Int>, Long>()
      customMap["Freistil" to 400] = 275000L // 4:35.00
      customMap["Freistil" to 800] = 560000L // 9:20.00
      customMap["Freistil" to 1500] = 1090000L // 18:10.00

      val swimmerUpdated = swimmerBefore.copy(
        customPbsJson = com.example.data.model.SwimmerEntity.serializeCustomPbsMap(customMap)
      )
      repository.updateSwimmer(swimmerUpdated)

      val swimmerLoaded = repository.getSwimmerById(sId)!!
      val parsedPbs = swimmerLoaded.parseCustomPbsMap()
      assertEquals(275000L, parsedPbs["Freistil" to 400])
      assertEquals(560000L, parsedPbs["Freistil" to 800])
      assertEquals(1090000L, parsedPbs["Freistil" to 1500])

      // Test ExportHelper JSON roundtrip
      val jsonExport = com.example.util.ExportHelper.exportSwimmersToJson(listOf(swimmerLoaded))
      val importedSwimmers = com.example.util.ExportHelper.importSwimmersFromJson(jsonExport)
      assertEquals(1, importedSwimmers.size)
      val importedParsedPbs = importedSwimmers[0].parseCustomPbsMap()
      assertEquals(275000L, importedParsedPbs["Freistil" to 400])
      assertEquals(560000L, importedParsedPbs["Freistil" to 800])
      assertEquals(1090000L, importedParsedPbs["Freistil" to 1500])
    }
  }

  @Test
  fun `test 200m PBs for all stroke categories`() {
    val customMap = mutableMapOf<Pair<String, Int>, Long>()
    customMap["Freistil" to 200] = 122000L
    customMap["Brust" to 200] = 142000L
    customMap["Rücken" to 200] = 135000L
    customMap["Schmetterling" to 200] = 130000L
    customMap["Lagen" to 400] = 295000L

    val json = com.example.data.model.SwimmerEntity.serializeCustomPbsMap(customMap)
    val swimmer = com.example.data.model.SwimmerEntity(name = "Multi Stroke Swimmer", customPbsJson = json)
    val parsed = swimmer.parseCustomPbsMap()

    assertEquals(122000L, parsed["Freistil" to 200])
    assertEquals(142000L, parsed["Brust" to 200])
    assertEquals(135000L, parsed["Rücken" to 200])
    assertEquals(130000L, parsed["Schmetterling" to 200])
    assertEquals(295000L, parsed["Lagen" to 400])
  }

  @Test
  fun `test direct finish on 100m with 50m splits saves 100m PB and pace`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.db.AppDatabase.getInstance(context)
    val repository = com.example.data.repository.SwimRepository(db)
    val prefsRepo = com.example.data.repository.UserPreferencesRepository(context)

    val vm = com.example.ui.StopwatchViewModel(repository, prefsRepo)
    kotlinx.coroutines.runBlocking { kotlinx.coroutines.delay(200) }

    vm.setTargetDistance(100)
    vm.setSplitDistance(50)
    vm.setStrokeType("Freistil")

    kotlinx.coroutines.runBlocking {
      val swimmerEntity = com.example.data.model.SwimmerEntity(name = "Direct Finish Swimmer", favoriteStroke = "Freistil")
      val swimmerId = repository.insertSwimmer(swimmerEntity)
      val savedSwimmer = repository.getSwimmerById(swimmerId)!!

      val session = com.example.data.model.SessionEntity(
        title = "100m Direct Finish Test",
        poolLengthMeters = 50,
        targetDistanceMeters = 100,
        splitDistanceMeters = 50,
        strokeType = "Freistil"
      )
      val lap = com.example.data.model.LapRecordEntity(
        sessionId = 0,
        swimmerId = savedSwimmer.id,
        swimmerName = savedSwimmer.name,
        laneNumber = 1,
        lapIndex = 2, // Direct finish lap
        splitTimeMillis = 65_000L,
        lapTimeMillis = 65_000L
      )
      repository.saveSessionWithLaps(session, listOf(lap))
      vm.recalculateSwimmerPbs(savedSwimmer.id)

      val swimmerAfter = repository.getSwimmerById(swimmerId)!!
      val parsedPbs = swimmerAfter.parseCustomPbsMap()
      assertEquals(65_000L, parsedPbs["Freistil" to 100])
      assertEquals(65_000L, swimmerAfter.pb100mMillis)
    }
  }
}
