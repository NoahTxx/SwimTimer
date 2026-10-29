package com.example.domain

import com.example.data.model.SwimmerEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RelayOptimizerTest {

    private fun swimmerWithCustomPbs(
        id: Long,
        name: String,
        favoriteStroke: String = "Freistil",
        pbs: Map<Pair<String, Int>, Long>
    ): SwimmerEntity {
        return SwimmerEntity(
            id = id,
            name = name,
            favoriteStroke = favoriteStroke,
            pb25mMillis = pbs[favoriteStroke to 25],
            pb50mMillis = pbs[favoriteStroke to 50],
            pb100mMillis = pbs[favoriteStroke to 100],
            customPbsJson = SwimmerEntity.serializeCustomPbsMap(pbs)
        )
    }

    @Test
    fun optimizeMedleyRelays_findsGlobalOptimumInsteadOfGreedySuboptimum() {
        // Classic assignment trap where greedy assignment on stroke 1 (Rücken) would pick Swimmer A,
        // even though Swimmer A is the ONLY strong Brust swimmer:
        // Swimmer 1 (Allrounder): Rücken 30.0s, Brust 32.0s, Schmett 40.0s, Freistil 35.0s
        // Swimmer 2 (Back specialist): Rücken 30.5s, Brust 45.0s, Schmett 42.0s, Freistil 36.0s
        // Swimmer 3 (Fly specialist): Rücken 38.0s, Brust 44.0s, Schmett 28.0s, Freistil 34.0s
        // Swimmer 4 (Free specialist): Rücken 37.0s, Brust 43.0s, Schmett 39.0s, Freistil 25.0s
        // Greedy would put Swimmer 1 on Rücken (30.0s) and be forced to use Swimmer 2/3/4 on Brust (43-45s) -> ~126s
        // Exact optimum puts Swimmer 2 on Rücken (30.5s) and Swimmer 1 on Brust (32.0s), Swimmer 3 on Schmett (28.0s), Swimmer 4 on Freistil (25.0s) = 115.5s!
        val s1 = swimmerWithCustomPbs(
            1L, "Allrounder",
            pbs = mapOf(
                ("Rücken" to 50) to 30_000L,
                ("Brust" to 50) to 32_000L,
                ("Schmetterling" to 50) to 40_000L,
                ("Freistil" to 50) to 35_000L
            )
        )
        val s2 = swimmerWithCustomPbs(
            2L, "BackSpec",
            pbs = mapOf(
                ("Rücken" to 50) to 30_500L,
                ("Brust" to 50) to 45_000L,
                ("Schmetterling" to 50) to 42_000L,
                ("Freistil" to 50) to 36_000L
            )
        )
        val s3 = swimmerWithCustomPbs(
            3L, "FlySpec",
            pbs = mapOf(
                ("Rücken" to 50) to 38_000L,
                ("Brust" to 50) to 44_000L,
                ("Schmetterling" to 50) to 28_000L,
                ("Freistil" to 50) to 34_000L
            )
        )
        val s4 = swimmerWithCustomPbs(
            4L, "FreeSpec",
            pbs = mapOf(
                ("Rücken" to 50) to 37_000L,
                ("Brust" to 50) to 43_000L,
                ("Schmetterling" to 50) to 39_000L,
                ("Freistil" to 50) to 25_000L
            )
        )

        val teams = RelayOptimizer.optimizeMedleyRelays(
            swimmers = listOf(s1, s2, s3, s4),
            legDistanceMeters = 50
        )

        assertEquals(1, teams.size)
        val teamA = teams.first()
        assertEquals(115_500L, teamA.totalTimeMillis)
        assertEquals("BackSpec", teamA.legs[0].swimmerName)
        assertEquals("Rücken", teamA.legs[0].stroke)
        assertEquals("Allrounder", teamA.legs[1].swimmerName)
        assertEquals("Brust", teamA.legs[1].stroke)
        assertEquals("FlySpec", teamA.legs[2].swimmerName)
        assertEquals("Schmetterling", teamA.legs[2].stroke)
        assertEquals("FreeSpec", teamA.legs[3].swimmerName)
        assertEquals("Freistil", teamA.legs[3].stroke)
    }

    @Test
    fun optimizeFastestSingleStrokeRelays_placesFastestSwimmerAsAnchorAndSecondFastestAsLeadOff() {
        val swimmers = listOf(
            swimmerWithCustomPbs(1L, "Slowest", pbs = mapOf(("Freistil" to 50) to 30_000L)),
            swimmerWithCustomPbs(2L, "Fastest", pbs = mapOf(("Freistil" to 50) to 24_000L)),
            swimmerWithCustomPbs(3L, "Third", pbs = mapOf(("Freistil" to 50) to 28_000L)),
            swimmerWithCustomPbs(4L, "Second", pbs = mapOf(("Freistil" to 50) to 26_000L))
        )

        val teams = RelayOptimizer.optimizeFastestSingleStrokeRelays(
            swimmers = swimmers,
            stroke = "Freistil",
            legDistanceMeters = 50
        )

        assertEquals(1, teams.size)
        val legs = teams[0].legs
        assertEquals("Second", legs[0].swimmerName)   // Pos 1: 2nd fastest
        assertEquals("Third", legs[1].swimmerName)    // Pos 2: 3rd fastest
        assertEquals("Slowest", legs[2].swimmerName)  // Pos 3: 4th fastest
        assertEquals("Fastest", legs[3].swimmerName)  // Pos 4: 1st fastest (Anchor)
        assertEquals(108_000L, teams[0].totalTimeMillis)
    }

    @Test
    fun buildBalancedTeams_minimizesTimeDifferenceBetweenTeams() {
        val swimmers = listOf(
            swimmerWithCustomPbs(1L, "S1", pbs = mapOf(("Freistil" to 50) to 24_000L)),
            swimmerWithCustomPbs(2L, "S2", pbs = mapOf(("Freistil" to 50) to 25_000L)),
            swimmerWithCustomPbs(3L, "S3", pbs = mapOf(("Freistil" to 50) to 26_000L)),
            swimmerWithCustomPbs(4L, "S4", pbs = mapOf(("Freistil" to 50) to 27_000L)),
            swimmerWithCustomPbs(5L, "S5", pbs = mapOf(("Freistil" to 50) to 28_000L)),
            swimmerWithCustomPbs(6L, "S6", pbs = mapOf(("Freistil" to 50) to 29_000L)),
            swimmerWithCustomPbs(7L, "S7", pbs = mapOf(("Freistil" to 50) to 30_000L)),
            swimmerWithCustomPbs(8L, "S8", pbs = mapOf(("Freistil" to 50) to 31_000L))
        )

        val teams = RelayOptimizer.buildBalancedTeams(
            swimmers = swimmers,
            teamCount = 2,
            stroke = "Freistil",
            legDistanceMeters = 50
        )

        assertEquals(2, teams.size)
        assertEquals(4, teams[0].legs.size)
        assertEquals(4, teams[1].legs.size)
        val t1 = teams[0].totalTimeMillis!!
        val t2 = teams[1].totalTimeMillis!!
        // Total sum is 220_000ms -> perfectly balanced teams have 110_000ms each!
        assertTrue("Expected difference <= 1000ms, got ${abs(t1 - t2)}", abs(t1 - t2) <= 1_000L)
    }

    @Test
    fun resolveSwimmerTime_usesLagenSplitsAndAdjacentDistanceEstimation() {
        val swimmer = SwimmerEntity(
            id = 10L,
            name = "Medley Kid",
            favoriteStroke = "Rücken",
            pb100mMillis = 64_000L, // 100m Rücken PB -> should estimate 50m Rücken
            pb200mLagenBreastSplitMillis = 36_500L // 50m Brust Lagen-Split
        )

        val breast50 = RelayOptimizer.resolveSwimmerTime(swimmer, "Brust", 50)
        assertNotNull(breast50)
        assertEquals(36_500L, breast50!!.millis)
        assertEquals(false, breast50.isEstimated)
        assertEquals("Lagen-Split", breast50.sourceLabel)

        val back50 = RelayOptimizer.resolveSwimmerTime(swimmer, "Rücken", 50)
        assertNotNull(back50)
        assertEquals(true, back50!!.isEstimated)
        assertEquals("~100m", back50.sourceLabel)
    }
}
