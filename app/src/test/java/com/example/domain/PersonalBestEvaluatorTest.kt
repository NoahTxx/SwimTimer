package com.example.domain

import com.example.data.model.SwimmerEntity
import com.example.ui.LaneState
import com.example.ui.LaneStatus
import com.example.ui.LapItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
