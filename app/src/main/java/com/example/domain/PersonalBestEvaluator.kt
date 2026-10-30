package com.example.domain

import com.example.data.model.LapRecordEntity
import com.example.data.model.SessionWithLaps
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

data class SwimmerSessionPerformance(
    val strokeType: String,
    val distanceMeters: Int,
    val bestTimeMillis: Long,
    val isRelayLeg: Boolean,
    val relayLegLabel: String? = null,
    val laps: List<LapItem> = emptyList()
)

object PersonalBestEvaluator {

    val LAGEN_STAFFEL_STROKES = listOf("Rücken", "Brust", "Schmetterling", "Freistil")

    fun isRelaySession(title: String, laps: List<LapRecordEntity>): Boolean {
        if (title.contains("Staffel", ignoreCase = true)) return true
        return laps.any { it.swimmerName.contains(" • ") }
    }

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
        val isRelayLane = laps.any { it.swimmerName.contains(" • ") }
        val totalSplitsNeeded = (targetDistanceMeters / splitDistanceMeters).coerceAtLeast(1)
        val result = mutableListOf<LapItem>()
        var currentSeriesIndex = 1
        var prevInSeriesIdx = 0
        var prevSplitMillis = -1L

        for (entity in laps.sortedBy { it.lapIndex }) {
            if (isRelayLane) {
                if (prevSplitMillis >= 0L && entity.splitTimeMillis < prevSplitMillis) {
                    currentSeriesIndex++
                    prevInSeriesIdx = 0
                }
                val lapIdxInSeries = prevInSeriesIdx + 1
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
            } else {
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
        // Relay Lane Handling: Evaluate only the specific leg(s) swum by this swimmer!
        if (lane.relayMembers.isNotEmpty()) {
            val memberLegs = lane.relayMembers.withIndex().filter { it.value.swimmerId == swimmer.id }
            if (memberLegs.isEmpty()) {
                return PbEvaluationResult(swimmer, false, emptyList())
            }
            var currentSwimmer = swimmer
            var anyImproved = false
            val allAchievements = mutableListOf<String>()
            val runs = lane.allLaps.groupBy { it.seriesIndex }

            for ((legIdx, member) in memberLegs) {
                val legDist = member.distanceMeters.coerceAtLeast(splitDistanceMeters)
                val splitsPerLeg = (legDist / splitDistanceMeters).coerceAtLeast(1)
                val startLapIdx = legIdx * splitsPerLeg
                val endLapIdx = startLapIdx + splitsPerLeg

                val memberSeriesLaps = mutableListOf<LapItem>()
                for ((seriesIdx, seriesLaps) in runs) {
                    val startSplitMs = if (startLapIdx == 0) {
                        0L
                    } else {
                        seriesLaps.find { it.lapIndex == startLapIdx }?.splitTimeMillis
                    }
                    val endSplitMs = seriesLaps.find { it.lapIndex == endLapIdx }?.splitTimeMillis

                    if (startSplitMs != null && endSplitMs != null && endSplitMs > startSplitMs) {
                        var allSubSplitsPresent = true
                        for (subIdx in 1..splitsPerLeg) {
                            val lapInLane = seriesLaps.find { it.lapIndex == startLapIdx + subIdx }
                            if (lapInLane == null) {
                                allSubSplitsPresent = false
                                break
                            }
                        }
                        if (allSubSplitsPresent) {
                            for (subIdx in 1..splitsPerLeg) {
                                val lapInLane = seriesLaps.first { it.lapIndex == startLapIdx + subIdx }
                                val relSplit = (lapInLane.splitTimeMillis - startSplitMs).coerceAtLeast(0L)
                                memberSeriesLaps.add(
                                    LapItem(
                                        lapIndex = subIdx,
                                        splitTimeMillis = relSplit,
                                        lapTimeMillis = lapInLane.lapTimeMillis,
                                        strokeRateSpm = lapInLane.strokeRateSpm,
                                        seriesIndex = seriesIdx
                                    )
                                )
                            }
                        }
                    }
                }

                if (memberSeriesLaps.isNotEmpty()) {
                    val syntheticLegLane = LaneState(
                        laneNumber = lane.laneNumber,
                        swimmerId = swimmer.id,
                        swimmerName = swimmer.name,
                        status = LaneStatus.FINISHED,
                        finishTimeMillis = memberSeriesLaps.last().splitTimeMillis,
                        completedSeriesLaps = memberSeriesLaps,
                        targetDistanceMeters = legDist,
                        splitDistanceMeters = splitDistanceMeters,
                        strokeType = member.stroke
                    )
                    val legEval = evaluateSingleSwimmerLane(
                        swimmer = currentSwimmer,
                        lane = syntheticLegLane,
                        targetDistanceMeters = legDist,
                        splitDistanceMeters = splitDistanceMeters,
                        strokeType = member.stroke
                    )
                    if (legEval.hasImproved) {
                        currentSwimmer = legEval.updatedSwimmer
                        anyImproved = true
                        allAchievements.addAll(legEval.achievements)
                    }
                }
            }
            return PbEvaluationResult(currentSwimmer, anyImproved, allAchievements)
        }

        return evaluateSingleSwimmerLane(
            swimmer = swimmer,
            lane = lane,
            targetDistanceMeters = targetDistanceMeters,
            splitDistanceMeters = splitDistanceMeters,
            strokeType = strokeType
        )
    }

    private fun evaluateSingleSwimmerLane(
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

        fun updateCustomPbIfFaster(strokeKey: String, distMeters: Int, timeMs: Long, announce: Boolean = false) {
            if (timeMs <= 0L) return
            val existing = customPbsMap[strokeKey to distMeters]
            if (existing == null || timeMs < existing) {
                customPbsMap[strokeKey to distMeters] = timeMs
                hasImproved = true
                if (announce) {
                    val useShort = distMeters <= 50
                    pbAchievements.add("${swimmer.name} ${distMeters}m $strokeKey: ${TimeFormatter.formatMillis(timeMs, shortFormat = useShort)}")
                }
            }
        }

        val expectedSplitsForFullDistance = (targetDistanceMeters / splitDistanceMeters).coerceAtLeast(1)
        val runs = lane.allLaps.groupBy { it.seriesIndex }.values

        runs.forEach { seriesLaps ->
            val hasTargetLap = seriesLaps.any { it.lapIndex == expectedSplitsForFullDistance }
            // A swimmer has completed the full distance ONLY if the target lap was recorded or
            // at least expectedSplitsForFullDistance splits were completed.
            // Early stops via "Ziel" / "Stopp Alle" before all splits were swum MUST NOT award
            // a PB for the full unswum target distance!
            val completedFullDistance = hasTargetLap || (seriesLaps.size >= expectedSplitsForFullDistance)

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

            // Check multi-stroke PBs in customPbsMap for target distance
            if (finishTime != null && finishTime > 0) {
                val isFavStroke = strokeType != "Lagen" && isSameStroke(strokeType, swimmer.favoriteStroke)
                val willAnnounceInLegacy = isFavStroke && targetDistanceMeters in listOf(25, 50, 100) ||
                        (strokeType == "Lagen" && targetDistanceMeters in listOf(100, 200, 400))
                updateCustomPbIfFaster(matchedStroke, targetDistanceMeters, finishTime, announce = !willAnnounceInLegacy)
            }

            // Non-Lagen: Check intermediate & final 25m, 50m, and 100m PBs
            if (strokeType != "Lagen") {
                val timeAt25m = if (targetDistanceMeters == 25) finishTime else getCumulativeSplitAtMeters(25)
                if (timeAt25m != null) {
                    val isFav = isSameStroke(strokeType, swimmer.favoriteStroke)
                    updateCustomPbIfFaster(matchedStroke, 25, timeAt25m, announce = !isFav && targetDistanceMeters != 25)
                    if (isFav && (updatedPb25 == null || timeAt25m < updatedPb25)) {
                        updatedPb25 = timeAt25m
                        hasImproved = true
                        pbAchievements.add("${swimmer.name} 25m ${swimmer.favoriteStroke}: ${TimeFormatter.formatMillis(timeAt25m, shortFormat = true)}")
                    }
                }

                val timeAt50m = if (targetDistanceMeters == 50) finishTime else getCumulativeSplitAtMeters(50)
                if (timeAt50m != null) {
                    val isFav = isSameStroke(strokeType, swimmer.favoriteStroke)
                    updateCustomPbIfFaster(matchedStroke, 50, timeAt50m, announce = !isFav && targetDistanceMeters != 50)
                    if (isFav && (updatedPb50 == null || timeAt50m < updatedPb50)) {
                        updatedPb50 = timeAt50m
                        hasImproved = true
                        pbAchievements.add("${swimmer.name} 50m ${swimmer.favoriteStroke}: ${TimeFormatter.formatMillis(timeAt50m, shortFormat = true)}")
                    }
                }

                val timeAt100m = if (targetDistanceMeters == 100) finishTime else getCumulativeSplitAtMeters(100)
                if (timeAt100m != null) {
                    val isFav = isSameStroke(strokeType, swimmer.favoriteStroke)
                    updateCustomPbIfFaster(matchedStroke, 100, timeAt100m, announce = !isFav && targetDistanceMeters != 100)
                    if (isFav && (updatedPb100 == null || timeAt100m < updatedPb100)) {
                        updatedPb100 = timeAt100m
                        hasImproved = true
                        pbAchievements.add("${swimmer.name} 100m ${swimmer.favoriteStroke}: ${TimeFormatter.formatMillis(timeAt100m)}")
                    }
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
                        val startLapIdx = legIndex * splitsPerLeg
                        val endLapIdx = startLapIdx + splitsPerLeg
                        val startSplitMs = if (startLapIdx == 0) {
                            0L
                        } else {
                            seriesLaps.find { it.lapIndex == startLapIdx }?.splitTimeMillis
                        }
                        val endSplitMs = seriesLaps.find { it.lapIndex == endLapIdx }?.splitTimeMillis
                        if (startSplitMs != null && endSplitMs != null && endSplitMs > startSplitMs) {
                            return endSplitMs - startSplitMs
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
                            updateCustomPbIfFaster("Schmetterling", 50, flyLegTime)
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
                            updateCustomPbIfFaster("Rücken", 50, backLegTime)
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
                            updateCustomPbIfFaster("Brust", 50, breastLegTime)
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
                            updateCustomPbIfFaster("Freistil", 50, freeLegTime)
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
                            updateCustomPbIfFaster("Schmetterling", 25, flyLegTime)
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
                            updateCustomPbIfFaster("Rücken", 25, backLegTime)
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
                            updateCustomPbIfFaster("Brust", 25, breastLegTime)
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
                            updateCustomPbIfFaster("Freistil", 25, freeLegTime)
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
                            updateCustomPbIfFaster("Schmetterling", 100, flyLegTime)
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
                            updateCustomPbIfFaster("Rücken", 100, backLegTime)
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
                            updateCustomPbIfFaster("Brust", 100, breastLegTime)
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
                            updateCustomPbIfFaster("Freistil", 100, freeLegTime)
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

    /**
     * Extracts a swimmer's individual performance(s) from a saved [SessionWithLaps],
     * accurately handling both normal individual swims and relay legs.
     */
    fun extractSwimmerSessionPerformances(
        sessionWithLaps: SessionWithLaps,
        swimmerId: Long
    ): List<SwimmerSessionPerformance> {
        val s = sessionWithLaps.session
        val relay = isRelaySession(s.title, sessionWithLaps.laps)

        if (!relay) {
            val swimmerLaps = sessionWithLaps.laps
                .asSequence()
                .filter { it.swimmerId == swimmerId }
                .sortedBy { it.lapIndex }
                .toList()
            if (swimmerLaps.isEmpty()) return emptyList()

            val seriesItems = splitSavedLapsIntoSeries(
                laps = swimmerLaps,
                targetDistanceMeters = s.targetDistanceMeters,
                splitDistanceMeters = s.splitDistanceMeters
            )
            val totalSplitsNeeded = (s.targetDistanceMeters / s.splitDistanceMeters).coerceAtLeast(1)
            val bestTime = seriesItems.groupBy { it.seriesIndex }.values.mapNotNull { runLaps ->
                runLaps.find { it.lapIndex == totalSplitsNeeded }?.splitTimeMillis
                    ?: runLaps.lastOrNull()?.splitTimeMillis
            }.filter { it > 0L }.minOrNull() ?: 0L

            return listOf(
                SwimmerSessionPerformance(
                    strokeType = s.strokeType,
                    distanceMeters = s.targetDistanceMeters,
                    bestTimeMillis = bestTime,
                    isRelayLeg = false,
                    relayLegLabel = null,
                    laps = seriesItems
                )
            )
        }

        // Relay session: group by lane first so cumulative splits on that lane are intact
        val performances = mutableListOf<SwimmerSessionPerformance>()
        val lanesWithSwimmer = sessionWithLaps.laps
            .groupBy { it.laneNumber }
            .filterValues { laneLaps -> laneLaps.any { it.swimmerId == swimmerId } }

        for ((_, laneLapsRaw) in lanesWithSwimmer) {
            val sortedLaneLaps = laneLapsRaw.sortedBy { it.lapIndex }
            val laneSeriesItems = splitSavedLapsIntoSeries(
                laps = sortedLaneLaps,
                targetDistanceMeters = s.targetDistanceMeters,
                splitDistanceMeters = s.splitDistanceMeters
            )
            val zipped = sortedLaneLaps.zip(laneSeriesItems)
            val bySeries = zipped.groupBy { it.second.seriesIndex }

            for ((seriesIdx, seriesPairs) in bySeries) {
                val swimmerPairs = seriesPairs.filter { it.first.swimmerId == swimmerId }
                if (swimmerPairs.isEmpty()) continue

                // Group into contiguous relay leg chunks (in case a swimmer swims multiple legs)
                val contiguousLegs = mutableListOf<MutableList<Pair<LapRecordEntity, LapItem>>>()
                for (pair in swimmerPairs) {
                    val lastGroup = contiguousLegs.lastOrNull()
                    if (lastGroup != null && pair.second.lapIndex == lastGroup.last().second.lapIndex + 1) {
                        lastGroup.add(pair)
                    } else {
                        contiguousLegs.add(mutableListOf(pair))
                    }
                }

                for (legPairs in contiguousLegs) {
                    val legDist = legPairs.size * s.splitDistanceMeters
                    val firstLapItem = legPairs.first().second
                    val rawName = legPairs.first().first.swimmerName
                    val parsedLegNumber = rawName.substringAfter(" • ", "")
                        .substringBefore(".")
                        .trim()
                        .toIntOrNull()
                    val legNumber = parsedLegNumber
                        ?: (((firstLapItem.lapIndex - 1) / legPairs.size.coerceAtLeast(1)) + 1)
                    val legStroke = if (isSameStroke(s.strokeType, "Lagen")) {
                        LAGEN_STAFFEL_STROKES.getOrElse((legNumber - 1).coerceAtLeast(0) % 4) { "Freistil" }
                    } else {
                        s.strokeType
                    }

                    // Verify that the preceding split (if legNumber > 1) was actually recorded
                    val startLapIdx = firstLapItem.lapIndex - 1
                    val startSplitMs = if (startLapIdx == 0) {
                        0L
                    } else {
                        seriesPairs.find { it.second.lapIndex == startLapIdx }?.second?.splitTimeMillis
                    }
                    val endSplitMs = legPairs.last().second.splitTimeMillis

                    val validLegTime = if (startSplitMs != null && endSplitMs > startSplitMs) {
                        endSplitMs - startSplitMs
                    } else {
                        legPairs.sumOf { it.second.lapTimeMillis }.takeIf { it > 0L } ?: 0L
                    }

                    if (validLegTime > 0L) {
                        var runningRelMs = 0L
                        val normalizedLaps = legPairs.mapIndexed { idx, (_, item) ->
                            val relSplit = if (startSplitMs != null && item.splitTimeMillis >= startSplitMs) {
                                (item.splitTimeMillis - startSplitMs).coerceAtLeast(0L)
                            } else {
                                runningRelMs += item.lapTimeMillis
                                runningRelMs
                            }
                            LapItem(
                                lapIndex = idx + 1,
                                splitTimeMillis = relSplit,
                                lapTimeMillis = item.lapTimeMillis,
                                strokeRateSpm = item.strokeRateSpm,
                                seriesIndex = seriesIdx
                            )
                        }
                        val teamTitle = rawName.substringBefore(" • ").trim()
                        val label = buildString {
                            if (teamTitle.isNotBlank()) append("$teamTitle • ")
                            append("$legNumber. Teilstrecke (${legDist}m $legStroke)")
                        }
                        performances.add(
                            SwimmerSessionPerformance(
                                strokeType = legStroke,
                                distanceMeters = legDist,
                                bestTimeMillis = validLegTime,
                                isRelayLeg = true,
                                relayLegLabel = label,
                                laps = normalizedLaps
                            )
                        )
                    }
                }
            }
        }
        return performances
    }

    fun evaluateSavedSessionForSwimmer(
        baseSwimmer: SwimmerEntity,
        sessionWithLaps: SessionWithLaps,
        swimmerId: Long
    ): SwimmerEntity {
        val s = sessionWithLaps.session
        val performances = extractSwimmerSessionPerformances(sessionWithLaps, swimmerId)
        if (performances.isEmpty()) return baseSwimmer

        var current = baseSwimmer
        for (perf in performances) {
            val splitDist = if (perf.isRelayLeg) {
                (perf.distanceMeters / perf.laps.size.coerceAtLeast(1)).coerceAtLeast(25)
            } else {
                s.splitDistanceMeters
            }
            val syntheticLane = LaneState(
                laneNumber = 1,
                swimmerId = swimmerId,
                swimmerName = current.name,
                status = LaneStatus.FINISHED,
                finishTimeMillis = perf.bestTimeMillis,
                completedSeriesLaps = perf.laps,
                targetDistanceMeters = perf.distanceMeters,
                splitDistanceMeters = splitDist,
                strokeType = perf.strokeType
            )
            current = evaluateSingleSwimmerLane(
                swimmer = current,
                lane = syntheticLane,
                targetDistanceMeters = perf.distanceMeters,
                splitDistanceMeters = splitDist,
                strokeType = perf.strokeType
            ).updatedSwimmer
        }
        return current
    }
}
