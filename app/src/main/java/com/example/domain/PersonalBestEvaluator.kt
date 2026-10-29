package com.example.domain

import com.example.data.model.LapRecordEntity
import com.example.data.model.SwimmerEntity
import com.example.ui.LaneState
import com.example.ui.LaneStatus
import com.example.ui.LapItem
import com.example.util.TimeFormatter

data class PbEvaluationResult(
    val updatedSwimmer: SwimmerEntity,
    val hasImproved: Boolean,
    val achievements: List<String>
)

object PersonalBestEvaluator {

    /**
     * Reconstructs individual series runs from a flat list of saved [LapRecordEntity] items.
     * In multi-series sessions, [LapRecordEntity.lapIndex] increments globally (1..N) while
     * [LapRecordEntity.splitTimeMillis] resets at the start of each new series.
     */
    fun splitSavedLapsIntoSeries(
        laps: List<LapRecordEntity>,
        targetDistanceMeters: Int,
        splitDistanceMeters: Int
    ): List<LapItem> {
        if (laps.isEmpty()) return emptyList()
        val totalSplitsNeeded = (targetDistanceMeters / splitDistanceMeters).coerceAtLeast(1)
        val result = mutableListOf<LapItem>()
        var currentSeriesIndex = 1
        var prevInSeriesIdx = 0
        var prevSplitMillis = -1L

        for (entity in laps.sortedBy { it.lapIndex }) {
            val rawInSeriesIdx = ((entity.lapIndex - 1).coerceAtLeast(0) % totalSplitsNeeded) + 1
            val rawSeriesIdx = ((entity.lapIndex - 1).coerceAtLeast(0) / totalSplitsNeeded) + 1

            if (rawSeriesIdx > currentSeriesIndex) {
                currentSeriesIndex = rawSeriesIdx
            } else if (prevSplitMillis >= 0L && (entity.splitTimeMillis < prevSplitMillis || rawInSeriesIdx <= prevInSeriesIdx)) {
                currentSeriesIndex++
            }

            val lapIdxInSeries = if (prevSplitMillis >= 0L && entity.splitTimeMillis < prevSplitMillis && rawInSeriesIdx > 1 && rawSeriesIdx == 1) {
                1
            } else {
                rawInSeriesIdx
            }

            prevInSeriesIdx = lapIdxInSeries
            prevSplitMillis = entity.splitTimeMillis
            result.add(
                LapItem(
                    lapIndex = lapIdxInSeries,
                    splitTimeMillis = entity.splitTimeMillis,
                    lapTimeMillis = entity.lapTimeMillis,
                    strokeRateSpm = entity.strokeRateSpm,
                    seriesIndex = currentSeriesIndex
                )
            )
        }
        return result
    }

    fun isSameStroke(strokeA: String?, strokeB: String?): Boolean {
        if (strokeA == null || strokeB == null) return false
        val a = strokeA.trim().lowercase()
        val b = strokeB.trim().lowercase()
        if (a == b) return true
        val isFreeA = a == "freistil" || a == "kraul" || a == "free"
        val isFreeB = b == "freistil" || b == "kraul" || b == "free"
        if (isFreeA && isFreeB) return true
        val isFlyA = a == "schmetterling" || a == "delfin" || a == "fly"
        val isFlyB = b == "schmetterling" || b == "delfin" || b == "fly"
        if (isFlyA && isFlyB) return true
        val isBackA = a == "rücken" || a == "ruecken" || a == "back"
        val isBackB = b == "rücken" || b == "ruecken" || b == "back"
        if (isBackA && isBackB) return true
        val isBreastA = a == "brust" || a == "breast"
        val isBreastB = b == "brust" || b == "breast"
        if (isBreastA && isBreastB) return true
        val isLagenA = a == "lagen" || a == "medley" || a == "im"
        val isLagenB = b == "lagen" || b == "medley" || b == "im"
        return isLagenA && isLagenB
    }

    fun evaluate(
        swimmer: SwimmerEntity,
        lane: LaneState,
        targetDistanceMeters: Int,
        splitDistanceMeters: Int,
        strokeType: String
    ): PbEvaluationResult {
        var updatedPb25 = swimmer.pb25mMillis
        var updatedPb50 = swimmer.pb50mMillis
        var updatedPb100 = swimmer.pb100mMillis
        var updatedPb100Lagen = swimmer.pb100mLagenMillis
        var updatedPb200Lagen = swimmer.pb200mLagenMillis
        var updatedPb400Lagen = swimmer.pb400mLagenMillis

        // 100m Lagen Splits (4x 25m)
        var updatedFly = swimmer.lagenFlySplitMillis
        var updatedBack = swimmer.lagenBackSplitMillis
        var updatedBreast = swimmer.lagenBreastSplitMillis
        var updatedFree = swimmer.lagenFreeSplitMillis

        // 200m Lagen Splits (4x 50m)
        var updatedPb200Fly = swimmer.pb200mLagenFlySplitMillis
        var updatedPb200Back = swimmer.pb200mLagenBackSplitMillis
        var updatedPb200Breast = swimmer.pb200mLagenBreastSplitMillis
        var updatedPb200Free = swimmer.pb200mLagenFreeSplitMillis

        // 400m Lagen Splits (4x 100m)
        var updatedPb400Fly = swimmer.pb400mLagenFlySplitMillis
        var updatedPb400Back = swimmer.pb400mLagenBackSplitMillis
        var updatedPb400Breast = swimmer.pb400mLagenBreastSplitMillis
        var updatedPb400Free = swimmer.pb400mLagenFreeSplitMillis

        val strokeOptions = listOf("Freistil", "Brust", "Rücken", "Schmetterling", "Lagen")
        val matchedStroke = strokeOptions.find { isSameStroke(it, strokeType) } ?: strokeType
        val customPbsMap = swimmer.parseCustomPbsMap().toMutableMap()

        var hasImproved = false
        val pbAchievements = mutableListOf<String>()

        val expectedSplitsForFullDistance = (targetDistanceMeters / splitDistanceMeters).coerceAtLeast(1)
        val runs = lane.allLaps.groupBy { it.seriesIndex }.values

        runs.forEach { seriesLaps ->
            val hasTargetLap = seriesLaps.any { it.lapIndex == expectedSplitsForFullDistance }
            val completedFullDistance = hasTargetLap ||
                    (seriesLaps.size >= expectedSplitsForFullDistance) ||
                    (seriesLaps.isNotEmpty() && (lane.status == LaneStatus.FINISHED || lane.status == LaneStatus.REST_COUNTDOWN || lane.finishTimeMillis != null))

            val finishTime = if (completedFullDistance) {
                seriesLaps.find { it.lapIndex == expectedSplitsForFullDistance }?.splitTimeMillis
                    ?: seriesLaps.lastOrNull()?.splitTimeMillis
            } else null

            fun getCumulativeSplitAtMeters(meters: Int): Long? {
                if (meters == targetDistanceMeters && finishTime != null && finishTime > 0) return finishTime
                if (meters % splitDistanceMeters != 0) return null
                val expectedLapIndex = meters / splitDistanceMeters
                val lap = seriesLaps.find { it.lapIndex == expectedLapIndex }
                return lap?.splitTimeMillis?.takeIf { it > 0L }
            }

            // Check multi-stroke PBs in customPbsMap for any stroke
            if (finishTime != null && finishTime > 0) {
                val currentCustomPb = customPbsMap[matchedStroke to targetDistanceMeters]
                if (currentCustomPb == null || finishTime < currentCustomPb) {
                    customPbsMap[matchedStroke to targetDistanceMeters] = finishTime
                    hasImproved = true
                    pbAchievements.add("${swimmer.name} ${targetDistanceMeters}m $matchedStroke: ${TimeFormatter.formatMillis(finishTime)}")
                }
            }

            // Non-Lagen: Check 25m, 50m, and 100m PBs for favorite stroke
            if (strokeType != "Lagen" && isSameStroke(strokeType, swimmer.favoriteStroke)) {
                val timeAt25m = if (targetDistanceMeters == 25) finishTime else getCumulativeSplitAtMeters(25)
                if (timeAt25m != null && (updatedPb25 == null || timeAt25m < updatedPb25)) {
                    updatedPb25 = timeAt25m
                    hasImproved = true
                    pbAchievements.add("${swimmer.name} 25m ${swimmer.favoriteStroke}: ${TimeFormatter.formatMillis(timeAt25m, shortFormat = true)}")
                }

                val timeAt50m = if (targetDistanceMeters == 50) finishTime else getCumulativeSplitAtMeters(50)
                if (timeAt50m != null && (updatedPb50 == null || timeAt50m < updatedPb50)) {
                    updatedPb50 = timeAt50m
                    hasImproved = true
                    pbAchievements.add("${swimmer.name} 50m ${swimmer.favoriteStroke}: ${TimeFormatter.formatMillis(timeAt50m, shortFormat = true)}")
                }

                val timeAt100m = if (targetDistanceMeters == 100) finishTime else getCumulativeSplitAtMeters(100)
                if (timeAt100m != null && (updatedPb100 == null || timeAt100m < updatedPb100)) {
                    updatedPb100 = timeAt100m
                    hasImproved = true
                    pbAchievements.add("${swimmer.name} 100m ${swimmer.favoriteStroke}: ${TimeFormatter.formatMillis(timeAt100m)}")
                }
            }

            // Lagen (Official Einzel-Reihenfolge: 1. Schmett, 2. Rücken, 3. Brust, 4. Freistil)
            if (strokeType == "Lagen") {
                if (targetDistanceMeters == 100 && finishTime != null && finishTime > 0) {
                    if (updatedPb100Lagen == null || finishTime < updatedPb100Lagen) {
                        updatedPb100Lagen = finishTime
                        hasImproved = true
                        pbAchievements.add("${swimmer.name} 100m Lagen: ${TimeFormatter.formatMillis(finishTime)}")
                    }
                } else if (targetDistanceMeters == 200 && finishTime != null && finishTime > 0) {
                    if (updatedPb200Lagen == null || finishTime < updatedPb200Lagen) {
                        updatedPb200Lagen = finishTime
                        hasImproved = true
                        pbAchievements.add("${swimmer.name} 200m Lagen: ${TimeFormatter.formatMillis(finishTime)}")
                    }
                } else if (targetDistanceMeters == 400 && finishTime != null && finishTime > 0) {
                    if (updatedPb400Lagen == null || finishTime < updatedPb400Lagen) {
                        updatedPb400Lagen = finishTime
                        hasImproved = true
                        pbAchievements.add("${swimmer.name} 400m Lagen: ${TimeFormatter.formatMillis(finishTime)}")
                    }
                }

                val legDist = (targetDistanceMeters / 4).coerceAtLeast(1)
                if (legDist % splitDistanceMeters == 0) {
                    val splitsPerLeg = (legDist / splitDistanceMeters).coerceAtLeast(1)

                    fun getLegTime(legIndex: Int): Long? {
                        val startIdx = legIndex * splitsPerLeg
                        val endIdx = startIdx + splitsPerLeg
                        if (seriesLaps.size >= endIdx) {
                            var sum = 0L
                            for (i in startIdx until endIdx) {
                                sum += seriesLaps[i].lapTimeMillis
                            }
                            return if (sum > 0) sum else null
                        }
                        return null
                    }

                    val flyLegTime = getLegTime(0)
                    val backLegTime = getLegTime(1)
                    val breastLegTime = getLegTime(2)
                    val freeLegTime = getLegTime(3)

                    if (targetDistanceMeters == 200) {
                        if (flyLegTime != null) {
                            if (updatedPb200Fly == null || flyLegTime < updatedPb200Fly) {
                                updatedPb200Fly = flyLegTime
                                hasImproved = true
                                pbAchievements.add("${swimmer.name} (200L 1. Schmett (50m): ${TimeFormatter.formatMillis(flyLegTime, shortFormat = true)})")
                            }
                            if (isSameStroke(swimmer.favoriteStroke, "Schmetterling") && (updatedPb50 == null || flyLegTime < updatedPb50)) {
                                updatedPb50 = flyLegTime
                                hasImproved = true
                                pbAchievements.add("${swimmer.name} (50m Schmett PB via 200L: ${TimeFormatter.formatMillis(flyLegTime, shortFormat = true)})")
                            }
                        }
                        if (backLegTime != null) {
                            if (updatedPb200Back == null || backLegTime < updatedPb200Back) {
                                updatedPb200Back = backLegTime
                                hasImproved = true
                                pbAchievements.add("${swimmer.name} (200L 2. Rücken (50m): ${TimeFormatter.formatMillis(backLegTime, shortFormat = true)})")
                            }
                            if (isSameStroke(swimmer.favoriteStroke, "Rücken") && (updatedPb50 == null || backLegTime < updatedPb50)) {
                                updatedPb50 = backLegTime
                                hasImproved = true
                                pbAchievements.add("${swimmer.name} (50m Rücken PB via 200L: ${TimeFormatter.formatMillis(backLegTime, shortFormat = true)})")
                            }
                        }
                        if (breastLegTime != null) {
                            if (updatedPb200Breast == null || breastLegTime < updatedPb200Breast) {
                                updatedPb200Breast = breastLegTime
                                hasImproved = true
                                pbAchievements.add("${swimmer.name} (200L 3. Brust (50m): ${TimeFormatter.formatMillis(breastLegTime, shortFormat = true)})")
                            }
                            if (isSameStroke(swimmer.favoriteStroke, "Brust") && (updatedPb50 == null || breastLegTime < updatedPb50)) {
                                updatedPb50 = breastLegTime
                                hasImproved = true
                                pbAchievements.add("${swimmer.name} (50m Brust PB via 200L: ${TimeFormatter.formatMillis(breastLegTime, shortFormat = true)})")
                            }
                        }
                        if (freeLegTime != null) {
                            if (updatedPb200Free == null || freeLegTime < updatedPb200Free) {
                                updatedPb200Free = freeLegTime
                                hasImproved = true
                                pbAchievements.add("${swimmer.name} (200L 4. Kraul (50m): ${TimeFormatter.formatMillis(freeLegTime, shortFormat = true)})")
                            }
                            if (isSameStroke(swimmer.favoriteStroke, "Freistil") && (updatedPb50 == null || freeLegTime < updatedPb50)) {
                                updatedPb50 = freeLegTime
                                hasImproved = true
                                pbAchievements.add("${swimmer.name} (50m Freistil PB via 200L: ${TimeFormatter.formatMillis(freeLegTime, shortFormat = true)})")
                            }
                        }
                    } else if (targetDistanceMeters == 100) {
                        if (flyLegTime != null) {
                            if (updatedFly == null || flyLegTime < updatedFly) {
                                updatedFly = flyLegTime
                                hasImproved = true
                                pbAchievements.add("${swimmer.name} (100L 1. Schmett (25m): ${TimeFormatter.formatMillis(flyLegTime, shortFormat = true)})")
                            }
                            if (isSameStroke(swimmer.favoriteStroke, "Schmetterling") && (updatedPb25 == null || flyLegTime < updatedPb25)) {
                                updatedPb25 = flyLegTime
                                hasImproved = true
                                pbAchievements.add("${swimmer.name} (25m Schmett PB via 100L: ${TimeFormatter.formatMillis(flyLegTime, shortFormat = true)})")
                            }
                        }
                        if (backLegTime != null) {
                            if (updatedBack == null || backLegTime < updatedBack) {
                                updatedBack = backLegTime
                                hasImproved = true
                                pbAchievements.add("${swimmer.name} (100L 2. Rücken (25m): ${TimeFormatter.formatMillis(backLegTime, shortFormat = true)})")
                            }
                            if (isSameStroke(swimmer.favoriteStroke, "Rücken") && (updatedPb25 == null || backLegTime < updatedPb25)) {
                                updatedPb25 = backLegTime
                                hasImproved = true
                                pbAchievements.add("${swimmer.name} (25m Rücken PB via 100L: ${TimeFormatter.formatMillis(backLegTime, shortFormat = true)})")
                            }
                        }
                        if (breastLegTime != null) {
                            if (updatedBreast == null || breastLegTime < updatedBreast) {
                                updatedBreast = breastLegTime
                                hasImproved = true
                                pbAchievements.add("${swimmer.name} (100L 3. Brust (25m): ${TimeFormatter.formatMillis(breastLegTime, shortFormat = true)})")
                            }
                            if (isSameStroke(swimmer.favoriteStroke, "Brust") && (updatedPb25 == null || breastLegTime < updatedPb25)) {
                                updatedPb25 = breastLegTime
                                hasImproved = true
                                pbAchievements.add("${swimmer.name} (25m Brust PB via 100L: ${TimeFormatter.formatMillis(breastLegTime, shortFormat = true)})")
                            }
                        }
                        if (freeLegTime != null) {
                            if (updatedFree == null || freeLegTime < updatedFree) {
                                updatedFree = freeLegTime
                                hasImproved = true
                                pbAchievements.add("${swimmer.name} (100L 4. Freistil (25m): ${TimeFormatter.formatMillis(freeLegTime, shortFormat = true)})")
                            }
                            if (isSameStroke(swimmer.favoriteStroke, "Freistil") && (updatedPb25 == null || freeLegTime < updatedPb25)) {
                                updatedPb25 = freeLegTime
                                hasImproved = true
                                pbAchievements.add("${swimmer.name} (25m Freistil PB via 100L: ${TimeFormatter.formatMillis(freeLegTime, shortFormat = true)})")
                            }
                        }
                    } else if (targetDistanceMeters == 400) {
                        if (flyLegTime != null) {
                            if (updatedPb400Fly == null || flyLegTime < updatedPb400Fly) {
                                updatedPb400Fly = flyLegTime
                                hasImproved = true
                                pbAchievements.add("${swimmer.name} (400L 1. Schmett (100m): ${TimeFormatter.formatMillis(flyLegTime)})")
                            }
                            if (isSameStroke(swimmer.favoriteStroke, "Schmetterling") && (updatedPb100 == null || flyLegTime < updatedPb100)) {
                                updatedPb100 = flyLegTime
                                hasImproved = true
                                pbAchievements.add("${swimmer.name} (100m Schmett PB via 400L: ${TimeFormatter.formatMillis(flyLegTime)})")
                            }
                        }
                        if (backLegTime != null) {
                            if (updatedPb400Back == null || backLegTime < updatedPb400Back) {
                                updatedPb400Back = backLegTime
                                hasImproved = true
                                pbAchievements.add("${swimmer.name} (400L 2. Rücken (100m): ${TimeFormatter.formatMillis(backLegTime)})")
                            }
                            if (isSameStroke(swimmer.favoriteStroke, "Rücken") && (updatedPb100 == null || backLegTime < updatedPb100)) {
                                updatedPb100 = backLegTime
                                hasImproved = true
                                pbAchievements.add("${swimmer.name} (100m Rücken PB via 400L: ${TimeFormatter.formatMillis(backLegTime)})")
                            }
                        }
                        if (breastLegTime != null) {
                            if (updatedPb400Breast == null || breastLegTime < updatedPb400Breast) {
                                updatedPb400Breast = breastLegTime
                                hasImproved = true
                                pbAchievements.add("${swimmer.name} (400L 3. Brust (100m): ${TimeFormatter.formatMillis(breastLegTime)})")
                            }
                            if (isSameStroke(swimmer.favoriteStroke, "Brust") && (updatedPb100 == null || breastLegTime < updatedPb100)) {
                                updatedPb100 = breastLegTime
                                hasImproved = true
                                pbAchievements.add("${swimmer.name} (100m Brust PB via 400L: ${TimeFormatter.formatMillis(breastLegTime)})")
                            }
                        }
                        if (freeLegTime != null) {
                            if (updatedPb400Free == null || freeLegTime < updatedPb400Free) {
                                updatedPb400Free = freeLegTime
                                hasImproved = true
                                pbAchievements.add("${swimmer.name} (400L 4. Freistil (100m): ${TimeFormatter.formatMillis(freeLegTime)})")
                            }
                            if (isSameStroke(swimmer.favoriteStroke, "Freistil") && (updatedPb100 == null || freeLegTime < updatedPb100)) {
                                updatedPb100 = freeLegTime
                                hasImproved = true
                                pbAchievements.add("${swimmer.name} (100m Freistil PB via 400L: ${TimeFormatter.formatMillis(freeLegTime)})")
                            }
                        }
                    }
                }
            }
        }

        val updatedSwimmer = if (hasImproved) {
            swimmer.copy(
                pb25mMillis = updatedPb25,
                pb50mMillis = updatedPb50,
                pb100mMillis = updatedPb100,
                pb100mLagenMillis = updatedPb100Lagen,
                pb200mLagenMillis = updatedPb200Lagen,
                pb400mLagenMillis = updatedPb400Lagen,
                lagenFlySplitMillis = updatedFly,
                lagenBackSplitMillis = updatedBack,
                lagenBreastSplitMillis = updatedBreast,
                lagenFreeSplitMillis = updatedFree,
                pb200mLagenFlySplitMillis = updatedPb200Fly,
                pb200mLagenBackSplitMillis = updatedPb200Back,
                pb200mLagenBreastSplitMillis = updatedPb200Breast,
                pb200mLagenFreeSplitMillis = updatedPb200Free,
                pb400mLagenFlySplitMillis = updatedPb400Fly,
                pb400mLagenBackSplitMillis = updatedPb400Back,
                pb400mLagenBreastSplitMillis = updatedPb400Breast,
                pb400mLagenFreeSplitMillis = updatedPb400Free,
                customPbsJson = SwimmerEntity.serializeCustomPbsMap(customPbsMap)
            )
        } else {
            swimmer
        }

        return PbEvaluationResult(
            updatedSwimmer = updatedSwimmer,
            hasImproved = hasImproved,
            achievements = pbAchievements
        )
    }
}
