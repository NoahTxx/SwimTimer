package com.example.domain

import com.example.data.model.SwimmerEntity
import com.example.ui.LaneState
import com.example.ui.LaneStatus
import com.example.ui.LapItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PersonalBestEvaluatorTest {

    @Test
    fun isSameStroke_recognizesEquivalences() {
        assertTrue(PersonalBestEvaluator.isSameStroke("Freistil", "Kraul"))
        assertTrue(PersonalBestEvaluator.isSameStroke("kraul", "free"))
        assertTrue(PersonalBestEvaluator.isSameStroke("Schmetterling", "Delfin"))
        assertTrue(PersonalBestEvaluator.isSameStroke("Rücken", "ruecken"))
        assertTrue(PersonalBestEvaluator.isSameStroke("Lagen", "IM"))
        assertFalse(PersonalBestEvaluator.isSameStroke("Brust", "Freistil"))
    }

    @Test
    fun evaluate_freestylePb_updatesSwimmerPb() {
        val swimmer = SwimmerEntity(id = 1, name = "Alex", favoriteStroke = "Freistil", pb50mMillis = 26000L)
        val lane = LaneState(
            laneNumber = 1,
            swimmerId = 1,
            status = LaneStatus.FINISHED,
            finishTimeMillis = 24800L,
            laps = listOf(
                LapItem(lapIndex = 1, splitTimeMillis = 24800L, lapTimeMillis = 24800L)
            )
        )

        val result = PersonalBestEvaluator.evaluate(
            swimmer = swimmer,
            lane = lane,
            targetDistanceMeters = 50,
            splitDistanceMeters = 50,
            strokeType = "Kraul"
        )

        assertTrue(result.hasImproved)
        assertEquals(24800L, result.updatedSwimmer.pb50mMillis)
        assertTrue(result.achievements.any { it.contains("24.80") })
    }

    @Test
    fun evaluate_noImprovement_keepsCurrentPbs() {
        val customJson = SwimmerEntity.serializeCustomPbsMap(mapOf(("Freistil" to 50) to 24000L))
        val swimmer = SwimmerEntity(
            id = 1,
            name = "Alex",
            favoriteStroke = "Freistil",
            pb50mMillis = 24000L,
            customPbsJson = customJson
        )
        val lane = LaneState(
            laneNumber = 1,
            swimmerId = 1,
            status = LaneStatus.FINISHED,
            finishTimeMillis = 25500L,
            laps = listOf(
                LapItem(lapIndex = 1, splitTimeMillis = 25500L, lapTimeMillis = 25500L)
            )
        )

        val result = PersonalBestEvaluator.evaluate(
            swimmer = swimmer,
            lane = lane,
            targetDistanceMeters = 50,
            splitDistanceMeters = 50,
            strokeType = "Freistil"
        )

        assertFalse(result.hasImproved)
        assertEquals(24000L, result.updatedSwimmer.pb50mMillis)
        assertTrue(result.achievements.isEmpty())
    }

    @Test
    fun evaluate_200mLagen_updatesLegSplitsAndTotalLagenPb() {
        val swimmer = SwimmerEntity(id = 2, name = "Sarah", favoriteStroke = "Brust")
        val laps = listOf(
            LapItem(lapIndex = 1, splitTimeMillis = 29000L, lapTimeMillis = 29000L),  // Fly 50m
            LapItem(lapIndex = 2, splitTimeMillis = 62000L, lapTimeMillis = 33000L),  // Back 50m
            LapItem(lapIndex = 3, splitTimeMillis = 98000L, lapTimeMillis = 36000L),  // Breast 50m
            LapItem(lapIndex = 4, splitTimeMillis = 130000L, lapTimeMillis = 32000L)  // Free 50m
        )
        val lane = LaneState(
            laneNumber = 2,
            swimmerId = 2,
            status = LaneStatus.FINISHED,
            finishTimeMillis = 130000L,
            laps = laps
        )

        val result = PersonalBestEvaluator.evaluate(
            swimmer = swimmer,
            lane = lane,
            targetDistanceMeters = 200,
            splitDistanceMeters = 50,
            strokeType = "Lagen"
        )

        assertTrue(result.hasImproved)
        assertEquals(130000L, result.updatedSwimmer.pb200mLagenMillis)
        assertEquals(29000L, result.updatedSwimmer.pb200mLagenFlySplitMillis)
        assertEquals(33000L, result.updatedSwimmer.pb200mLagenBackSplitMillis)
        assertEquals(36000L, result.updatedSwimmer.pb200mLagenBreastSplitMillis)
        assertEquals(32000L, result.updatedSwimmer.pb200mLagenFreeSplitMillis)
        // Since favorite stroke is Brust, 50m Brust PB should also be updated from breast split
        assertEquals(36000L, result.updatedSwimmer.pb50mMillis)
    }

    @Test
    fun splitSavedLapsIntoSeries_reconstructsMultiSeriesRuns() {
        val savedLaps = listOf(
            com.example.data.model.LapRecordEntity(sessionId = 1, swimmerId = 1, swimmerName = "Alex", laneNumber = 1, lapIndex = 1, splitTimeMillis = 30000L, lapTimeMillis = 30000L),
            com.example.data.model.LapRecordEntity(sessionId = 1, swimmerId = 1, swimmerName = "Alex", laneNumber = 1, lapIndex = 2, splitTimeMillis = 62000L, lapTimeMillis = 32000L),
            com.example.data.model.LapRecordEntity(sessionId = 1, swimmerId = 1, swimmerName = "Alex", laneNumber = 1, lapIndex = 3, splitTimeMillis = 29000L, lapTimeMillis = 29000L),
            com.example.data.model.LapRecordEntity(sessionId = 1, swimmerId = 1, swimmerName = "Alex", laneNumber = 1, lapIndex = 4, splitTimeMillis = 59500L, lapTimeMillis = 30500L)
        )

        val reconstructed = PersonalBestEvaluator.splitSavedLapsIntoSeries(
            laps = savedLaps,
            targetDistanceMeters = 100,
            splitDistanceMeters = 50
        )

        assertEquals(4, reconstructed.size)
        assertEquals(1, reconstructed[0].seriesIndex)
        assertEquals(1, reconstructed[0].lapIndex)
        assertEquals(1, reconstructed[1].seriesIndex)
        assertEquals(2, reconstructed[1].lapIndex)
        assertEquals(2, reconstructed[2].seriesIndex)
        assertEquals(1, reconstructed[2].lapIndex)
        assertEquals(2, reconstructed[3].seriesIndex)
        assertEquals(2, reconstructed[3].lapIndex)
    }

    @Test
    fun extractSwimmerSessionPerformances_5SwimmerRelayWith100mSessionTarget_evaluates5thSwimmerLegCorrectly() {
        val savedLaps = listOf(
            com.example.data.model.LapRecordEntity(sessionId = 1, swimmerId = 1L, swimmerName = "Lukas • 1. Starter (Freistil)", laneNumber = 2, lapIndex = 1, splitTimeMillis = 15000L, lapTimeMillis = 15000L),
            com.example.data.model.LapRecordEntity(sessionId = 1, swimmerId = 2L, swimmerName = "Sophie • 2. Starter (Freistil)", laneNumber = 2, lapIndex = 2, splitTimeMillis = 31000L, lapTimeMillis = 16000L),
            com.example.data.model.LapRecordEntity(sessionId = 1, swimmerId = 3L, swimmerName = "Leon • 3. Starter (Freistil)", laneNumber = 2, lapIndex = 3, splitTimeMillis = 46500L, lapTimeMillis = 15500L),
            com.example.data.model.LapRecordEntity(sessionId = 1, swimmerId = 4L, swimmerName = "Emma • 4. Starter (Freistil)", laneNumber = 2, lapIndex = 4, splitTimeMillis = 61000L, lapTimeMillis = 14500L),
            com.example.data.model.LapRecordEntity(sessionId = 1, swimmerId = 5L, swimmerName = "Noah • 5. Starter (Freistil)", laneNumber = 2, lapIndex = 5, splitTimeMillis = 76200L, lapTimeMillis = 15200L)
        )

        val sessionWithLaps = com.example.data.model.SessionWithLaps(
            session = com.example.data.model.SessionEntity(
                id = 1L,
                title = "Staffel-Wettkampf (Freistil)",
                strokeType = "Freistil",
                poolLengthMeters = 25,
                targetDistanceMeters = 100, // Session target is 100m (4x25m), while lane 2 swam 5x25m (125m)
                splitDistanceMeters = 25
            ),
            laps = savedLaps
        )

        val fifthSwimmerPerfs = PersonalBestEvaluator.extractSwimmerSessionPerformances(
            swimmerId = 5L,
            sessionWithLaps = sessionWithLaps
        )

        assertEquals(1, fifthSwimmerPerfs.size)
        assertEquals(25, fifthSwimmerPerfs[0].distanceMeters)
        assertEquals("Freistil", fifthSwimmerPerfs[0].strokeType)
        assertEquals(15200L, fifthSwimmerPerfs[0].bestTimeMillis)
        assertEquals(15200L, fifthSwimmerPerfs[0].laps.single().lapTimeMillis)
    }

    @Test
    fun evaluateSingleSwimmerLane_earlyFinishDoesNotAwardFullDistancePb() {
        val swimmer = SwimmerEntity(
            id = 1,
            name = "Lukas",
            favoriteStroke = "Freistil",
            pb25mMillis = 15000L,
            pb50mMillis = null,
            pb100mMillis = null
        )

        // Swimmer stopped early after 1 split (25m swum in 14.5s) on a 100m race without completing all 4 splits
        val earlyLane = LaneState(
            laneNumber = 1,
            swimmerId = 1,
            swimmerName = "Lukas",
            status = LaneStatus.PAUSED, // stopped early without full distance
            finishTimeMillis = null,
            targetDistanceMeters = 100,
            splitDistanceMeters = 25,
            strokeType = "Freistil",
            laps = listOf(
                LapItem(lapIndex = 1, splitTimeMillis = 14500L, lapTimeMillis = 14500L, seriesIndex = 1)
            )
        )

        val result = PersonalBestEvaluator.evaluate(
            swimmer = swimmer,
            lane = earlyLane,
            targetDistanceMeters = 100,
            splitDistanceMeters = 25,
            strokeType = "Freistil"
        )

        // Swimmer improved 25m Freistil (14.5s < 15.0s), BUT 100m PB MUST NOT be awarded!
        assertEquals(14500L, result.updatedSwimmer.pb25mMillis)
        assertNull(result.updatedSwimmer.pb100mMillis)
        assertNull(result.updatedSwimmer.parseCustomPbsMap()["Freistil" to 100])
    }

    @Test
    fun evaluate_preservesLegacyPbsWhenUpdatingCustomPbs() {
        // Swimmer has legacy pb50m = 30.0s (30000ms) and no customPbsJson initially
        val swimmer = SwimmerEntity(
            id = 1,
            name = "Max",
            favoriteStroke = "Freistil",
            pb50mMillis = 30000L,
            customPbsJson = null
        )

        // Swimmer swims 100m Brust in 75s (75000ms)
        val lane = LaneState(
            laneNumber = 1,
            swimmerId = 1,
            swimmerName = "Max",
            status = LaneStatus.FINISHED,
            finishTimeMillis = 75000L,
            targetDistanceMeters = 100,
            splitDistanceMeters = 50,
            strokeType = "Brust",
            laps = listOf(
                LapItem(lapIndex = 1, splitTimeMillis = 36000L, lapTimeMillis = 36000L, seriesIndex = 1),
                LapItem(lapIndex = 2, splitTimeMillis = 75000L, lapTimeMillis = 39000L, seriesIndex = 1)
            )
        )

        val result = PersonalBestEvaluator.evaluate(
            swimmer = swimmer,
            lane = lane,
            targetDistanceMeters = 100,
            splitDistanceMeters = 50,
            strokeType = "Brust"
        )

        assertTrue(result.hasImproved)
        val customMap = result.updatedSwimmer.parseCustomPbsMap()
        // Legacy 50m Freistil PB MUST be preserved in customMap and pb50mMillis!
        assertEquals(30000L, result.updatedSwimmer.pb50mMillis)
        assertEquals(30000L, customMap["Freistil" to 50])
        // New 100m Brust PB is recorded
        assertEquals(75000L, customMap["Brust" to 100])
    }
}

