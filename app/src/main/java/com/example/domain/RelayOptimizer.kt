package com.example.domain

import androidx.compose.runtime.Immutable
import com.example.data.model.SwimmerEntity

enum class RelayOptimizationMode(val label: String, val shortLabel: String) {
    MEDLEY_OPTIMAL("4× Lagen-Optimizer (Wettkampf)", "4× Lagen"),
    FASTEST_SINGLE("Top-Staffel (A/B-Teams)", "Top-Staffel"),
    BALANCED("Fair-Match (Ausgeglichen)", "Fair-Match"),
    RANDOM("Zufalls-Losung", "Zufall")
}

@Immutable
data class ResolvedLegTime(
    val millis: Long,
    val isEstimated: Boolean,
    val sourceLabel: String
)

@Immutable
data class RelayLegAssignment(
    val legPosition: Int,
    val swimmerId: Long?,
    val swimmerName: String,
    val stroke: String,
    val distanceMeters: Int,
    val timeMillis: Long?,
    val isEstimated: Boolean = false,
    val sourceLabel: String = ""
)

@Immutable
data class RelayTeamPlan(
    val teamIndex: Int,
    val teamTitle: String,
    val legs: List<RelayLegAssignment>,
    val totalTimeMillis: Long?,
    val averageLegMillis: Long?,
    val deltaToFastestMillis: Long?,
    val hasEstimatedTimes: Boolean
)

object RelayOptimizer {

    val MEDLEY_ORDER = listOf("Rücken", "Brust", "Schmetterling", "Freistil")
    val ALLOWED_LEG_DISTANCES = listOf(25, 50, 100)
    val SINGLE_STROKES = listOf("Freistil", "Brust", "Rücken", "Schmetterling")

    private fun roundToHundredths(millis: Double): Long =
        (((millis + 5.0) / 10.0).toLong()) * 10L

    private fun isSameStroke(a: String, b: String): Boolean {
        val normA = a.trim().lowercase()
        val normB = b.trim().lowercase()
        if (normA == normB) return true
        val freeSet = setOf("freistil", "kraul")
        val flySet = setOf("schmetterling", "delfin", "delphin", "schmett")
        if (normA in freeSet && normB in freeSet) return true
        if (normA in flySet && normB in flySet) return true
        return false
    }

    /**
     * Resolves a swimmer's expected split time for a given [stroke] and [legDistanceMeters].
     * Uses exact PBs first, then Lagen split PBs, then adjacent distances in the same stroke,
     * and finally total Lagen PBs with stroke-specific weighting.
     */
    fun resolveSwimmerTime(
        swimmer: SwimmerEntity,
        stroke: String,
        legDistanceMeters: Int
    ): ResolvedLegTime? {
        val customMap = swimmer.parseCustomPbsMap()

        // 1. Exact PB for (stroke, legDistanceMeters)
        val exactCustom = customMap.entries
            .firstOrNull { (k, v) -> isSameStroke(k.first, stroke) && k.second == legDistanceMeters && v > 0L }
            ?.value

        val exactLegacy = if (isSameStroke(swimmer.favoriteStroke, stroke)) {
            when (legDistanceMeters) {
                25 -> swimmer.pb25mMillis?.takeIf { it > 0L }
                50 -> swimmer.pb50mMillis?.takeIf { it > 0L }
                100 -> swimmer.pb100mMillis?.takeIf { it > 0L }
                else -> null
            }
        } else null

        val bestExact = listOfNotNull(exactCustom, exactLegacy).minOrNull()
        if (bestExact != null) {
            return ResolvedLegTime(millis = bestExact, isEstimated = false, sourceLabel = "PB")
        }

        // 2. Exact Lagen split PB for this leg distance
        val lagenSplit = when (legDistanceMeters) {
            25 -> when {
                isSameStroke(stroke, "Schmetterling") -> swimmer.lagenFlySplitMillis
                isSameStroke(stroke, "Rücken") -> swimmer.lagenBackSplitMillis
                isSameStroke(stroke, "Brust") -> swimmer.lagenBreastSplitMillis
                isSameStroke(stroke, "Freistil") -> swimmer.lagenFreeSplitMillis
                else -> null
            }
            50 -> when {
                isSameStroke(stroke, "Schmetterling") -> swimmer.pb200mLagenFlySplitMillis
                isSameStroke(stroke, "Rücken") -> swimmer.pb200mLagenBackSplitMillis
                isSameStroke(stroke, "Brust") -> swimmer.pb200mLagenBreastSplitMillis
                isSameStroke(stroke, "Freistil") -> swimmer.pb200mLagenFreeSplitMillis
                else -> null
            }
            100 -> when {
                isSameStroke(stroke, "Schmetterling") -> swimmer.pb400mLagenFlySplitMillis
                isSameStroke(stroke, "Rücken") -> swimmer.pb400mLagenBackSplitMillis
                isSameStroke(stroke, "Brust") -> swimmer.pb400mLagenBreastSplitMillis
                isSameStroke(stroke, "Freistil") -> swimmer.pb400mLagenFreeSplitMillis
                else -> null
            }
            else -> null
        }?.takeIf { it > 0L }

        if (lagenSplit != null) {
            return ResolvedLegTime(millis = lagenSplit, isEstimated = false, sourceLabel = "Lagen-Split")
        }

        // 3. Adjacent distance PB in the same stroke
        fun findStrokePbAtDistance(dist: Int): Long? {
            val fromCustom = customMap.entries
                .firstOrNull { (k, v) -> isSameStroke(k.first, stroke) && k.second == dist && v > 0L }
                ?.value
            val fromLegacy = if (isSameStroke(swimmer.favoriteStroke, stroke)) {
                when (dist) {
                    25 -> swimmer.pb25mMillis?.takeIf { it > 0L }
                    50 -> swimmer.pb50mMillis?.takeIf { it > 0L }
                    100 -> swimmer.pb100mMillis?.takeIf { it > 0L }
                    else -> null
                }
            } else null
            return listOfNotNull(fromCustom, fromLegacy).minOrNull()
        }

        val conversionCandidates = when (legDistanceMeters) {
            25 -> listOf(50 to 0.47, 100 to 0.22, 200 to 0.105)
            50 -> listOf(100 to 0.47, 25 to 2.12, 200 to 0.225, 400 to 0.11)
            100 -> listOf(50 to 2.12, 200 to 0.475, 25 to 4.50, 400 to 0.23)
            else -> emptyList()
        }

        for ((srcDist, factor) in conversionCandidates) {
            val srcPb = findStrokePbAtDistance(srcDist)
            if (srcPb != null) {
                return ResolvedLegTime(
                    millis = roundToHundredths(srcPb * factor).coerceAtLeast(100L),
                    isEstimated = true,
                    sourceLabel = "~${srcDist}m"
                )
            }
        }

        // 4. Fallback from Lagen total PB
        val strokeWeight = when {
            isSameStroke(stroke, "Freistil") -> 0.89
            isSameStroke(stroke, "Schmetterling") -> 0.96
            isSameStroke(stroke, "Rücken") -> 1.02
            isSameStroke(stroke, "Brust") -> 1.13
            else -> 1.0
        }
        val matchingImDistance = legDistanceMeters * 4
        val imTotal = customMap["Lagen" to matchingImDistance]?.takeIf { it > 0L }
            ?: when (matchingImDistance) {
                100 -> swimmer.pb100mLagenMillis?.takeIf { it > 0L }
                200 -> swimmer.pb200mLagenMillis?.takeIf { it > 0L }
                400 -> swimmer.pb400mLagenMillis?.takeIf { it > 0L }
                else -> null
            }

        if (imTotal != null) {
            val estLeg = roundToHundredths((imTotal / 4.0) * strokeWeight).coerceAtLeast(100L)
            return ResolvedLegTime(millis = estLeg, isEstimated = true, sourceLabel = "~Lagen")
        }

        return null
    }

    /**
     * Computes optimal 4x Medley Relays (Rücken -> Brust -> Schmetterling -> Freistil).
     * Uses exact combinatorial optimization to find the fastest 1. Mannschaft, then 2. Mannschaft, etc.
     */
    fun optimizeMedleyRelays(
        swimmers: List<SwimmerEntity>,
        legDistanceMeters: Int = 50,
        maxTeams: Int = 4
    ): List<RelayTeamPlan> {
        val remaining = swimmers.toMutableList()
        val teams = mutableListOf<RelayTeamPlan>()
        val safeMaxTeams = maxTeams.coerceIn(1, 6)

        var teamIdx = 0
        while (remaining.size >= 4 && teamIdx < safeMaxTeams) {
            val teamPlan = findSingleOptimalMedleyTeam(
                pool = remaining,
                legDistanceMeters = legDistanceMeters,
                teamIndex = teamIdx
            ) ?: break

            teams.add(teamPlan)
            val usedIds = teamPlan.legs.mapNotNull { it.swimmerId }.toSet()
            remaining.removeAll { it.id in usedIds }
            teamIdx++
        }

        return attachDeltas(teams)
    }

    private fun findSingleOptimalMedleyTeam(
        pool: List<SwimmerEntity>,
        legDistanceMeters: Int,
        teamIndex: Int
    ): RelayTeamPlan? {
        if (pool.size < 4) return null

        // Precompute resolved times for all swimmers and all 4 medley strokes
        val timeTable: Map<Long, Array<ResolvedLegTime?>> = pool.associate { s ->
            s.id to Array(4) { strokeIdx ->
                resolveSwimmerTime(s, MEDLEY_ORDER[strokeIdx], legDistanceMeters)
            }
        }

        // Compute fallback penalty time per stroke for swimmers without any PB so we can still form a team if needed
        val strokeMaxKnown = Array(4) { strokeIdx ->
            pool.mapNotNull { timeTable[it.id]?.get(strokeIdx)?.millis }.maxOrNull() ?: (legDistanceMeters * 1_000L)
        }
        val unratedPenaltyOffset = 180_000L

        fun effectiveCost(swimmer: SwimmerEntity, strokeIdx: Int): Long {
            val resolved = timeTable[swimmer.id]?.get(strokeIdx)
            if (resolved != null) return resolved.millis
            val favBonus = if (isSameStroke(swimmer.favoriteStroke, MEDLEY_ORDER[strokeIdx])) -5_000L else 0L
            return strokeMaxKnown[strokeIdx] + unratedPenaltyOffset + favBonus
        }

        // For each of the 4 strokes, only the top 6 candidates can ever be part of the optimal 4-person assignment
        val candidatesPerStroke = Array(4) { strokeIdx ->
            pool.sortedWith(
                compareBy<SwimmerEntity> { effectiveCost(it, strokeIdx) }
                    .thenBy { timeTable[it.id]?.get(strokeIdx)?.isEstimated == true }
                    .thenBy { it.name.lowercase() }
                    .thenBy { it.id }
            ).take(6)
        }

        var bestCost = Long.MAX_VALUE
        var bestEstimatedCount = Int.MAX_VALUE
        var bestQuad: List<SwimmerEntity>? = null

        for (s0 in candidatesPerStroke[0]) {
            val c0 = effectiveCost(s0, 0)
            val e0 = if (timeTable[s0.id]?.get(0)?.isEstimated != false) 1 else 0
            for (s1 in candidatesPerStroke[1]) {
                if (s1.id == s0.id) continue
                val c1 = c0 + effectiveCost(s1, 1)
                if (c1 > bestCost) continue
                val e1 = e0 + if (timeTable[s1.id]?.get(1)?.isEstimated != false) 1 else 0
                for (s2 in candidatesPerStroke[2]) {
                    if (s2.id == s0.id || s2.id == s1.id) continue
                    val c2 = c1 + effectiveCost(s2, 2)
                    if (c2 > bestCost) continue
                    val e2 = e1 + if (timeTable[s2.id]?.get(2)?.isEstimated != false) 1 else 0
                    for (s3 in candidatesPerStroke[3]) {
                        if (s3.id == s0.id || s3.id == s1.id || s3.id == s2.id) continue
                        val totalCost = c2 + effectiveCost(s3, 3)
                        val totalEst = e2 + if (timeTable[s3.id]?.get(3)?.isEstimated != false) 1 else 0
                        if (totalCost < bestCost || (totalCost == bestCost && totalEst < bestEstimatedCount)) {
                            bestCost = totalCost
                            bestEstimatedCount = totalEst
                            bestQuad = listOf(s0, s1, s2, s3)
                        }
                    }
                }
            }
        }

        val chosen = bestQuad ?: pool.take(4)
        val legs = chosen.mapIndexed { idx, swimmer ->
            val stroke = MEDLEY_ORDER[idx]
            val resolved = timeTable[swimmer.id]?.get(idx)
            RelayLegAssignment(
                legPosition = idx + 1,
                swimmerId = swimmer.id,
                swimmerName = swimmer.name,
                stroke = stroke,
                distanceMeters = legDistanceMeters,
                timeMillis = resolved?.millis,
                isEstimated = resolved?.isEstimated == true,
                sourceLabel = resolved?.sourceLabel ?: ""
            )
        }

        val allHaveTimes = legs.all { it.timeMillis != null }
        val totalMillis = if (allHaveTimes) legs.sumOf { it.timeMillis ?: 0L } else null
        val knownTimes = legs.mapNotNull { it.timeMillis }
        val avgMillis = if (knownTimes.isNotEmpty()) knownTimes.sum() / knownTimes.size else null

        val teamLabel = "${teamIndex + 1}. Mannschaft (${('A' + teamIndex)}-Staffel)"
        return RelayTeamPlan(
            teamIndex = teamIndex,
            teamTitle = teamLabel,
            legs = legs,
            totalTimeMillis = totalMillis,
            averageLegMillis = avgMillis,
            deltaToFastestMillis = null,
            hasEstimatedTimes = legs.any { it.isEstimated }
        )
    }

    /**
     * Builds the fastest single-stroke relays (A-Staffel, B-Staffel, ...) and orders legs in
     * classic race order: 2nd fastest (Lead-off) -> middle legs -> 1st fastest (Anchor / Schlussschwimmer).
     */
    fun optimizeFastestSingleStrokeRelays(
        swimmers: List<SwimmerEntity>,
        stroke: String = "Freistil",
        legDistanceMeters: Int = 50,
        maxTeams: Int = 4,
        legsPerTeam: Int = 4
    ): List<RelayTeamPlan> {
        val teamSize = legsPerTeam.coerceIn(2, 8)
        if (swimmers.size < teamSize) return emptyList()

        val resolvedMap = swimmers.associate { s ->
            s.id to resolveSwimmerTime(s, stroke, legDistanceMeters)
        }

        val sorted = swimmers.sortedWith(
            compareBy<SwimmerEntity> { resolvedMap[it.id]?.millis ?: Long.MAX_VALUE }
                .thenBy { resolvedMap[it.id]?.isEstimated == true }
                .thenBy { it.name.lowercase() }
                .thenBy { it.id }
        )

        val safeMaxTeams = maxTeams.coerceIn(1, 6)
        val chunks = sorted.chunked(teamSize).filter { it.size == teamSize }.take(safeMaxTeams)

        val teams = chunks.mapIndexed { teamIdx, group ->
            // Group is sorted [fastest (0), 2nd (1), ..., slowest (teamSize - 1)]
            // Race order: Pos 1 = 2nd fastest, Middle = 3rd..slowest, Last Pos = 1st fastest (Anchor)
            val raceOrdered = if (group.size >= 2) {
                listOf(group[1]) + group.drop(2) + listOf(group[0])
            } else {
                group
            }
            val legs = raceOrdered.mapIndexed { idx, swimmer ->
                val resolved = resolvedMap[swimmer.id]
                RelayLegAssignment(
                    legPosition = idx + 1,
                    swimmerId = swimmer.id,
                    swimmerName = swimmer.name,
                    stroke = stroke,
                    distanceMeters = legDistanceMeters,
                    timeMillis = resolved?.millis,
                    isEstimated = resolved?.isEstimated == true,
                    sourceLabel = resolved?.sourceLabel ?: ""
                )
            }
            val allHaveTimes = legs.all { it.timeMillis != null }
            val totalMillis = if (allHaveTimes) legs.sumOf { it.timeMillis ?: 0L } else null
            val knownTimes = legs.mapNotNull { it.timeMillis }
            val avgMillis = if (knownTimes.isNotEmpty()) knownTimes.sum() / knownTimes.size else null

            RelayTeamPlan(
                teamIndex = teamIdx,
                teamTitle = "${teamIdx + 1}. Mannschaft (${('A' + teamIdx)}-Staffel)",
                legs = legs,
                totalTimeMillis = totalMillis,
                averageLegMillis = avgMillis,
                deltaToFastestMillis = null,
                hasEstimatedTimes = legs.any { it.isEstimated }
            )
        }

        return attachDeltas(teams)
    }

    /**
     * Distributes swimmers (and optional guest names) into [teamCount] balanced teams so that
     * team sizes differ by at most 1 and predicted team times are maximally close (Fair-Match).
     */
    fun buildBalancedTeams(
        swimmers: List<SwimmerEntity>,
        guestNames: List<String> = emptyList(),
        teamCount: Int = 2,
        stroke: String = "Freistil",
        legDistanceMeters: Int = 50,
        legsPerTeam: Int? = null
    ): List<RelayTeamPlan> {
        data class Participant(
            val id: Long?,
            val name: String,
            val resolved: ResolvedLegTime?
        )

        val seenNames = mutableSetOf<String>()
        val participants = mutableListOf<Participant>()

        swimmers.forEach { s ->
            val clean = s.name.trim()
            if (clean.isNotEmpty() && seenNames.add(clean.lowercase())) {
                participants.add(
                    Participant(
                        id = s.id,
                        name = clean,
                        resolved = resolveSwimmerTime(s, stroke, legDistanceMeters)
                    )
                )
            }
        }
        guestNames.forEach { g ->
            val clean = g.trim()
            if (clean.isNotEmpty() && seenNames.add(clean.lowercase())) {
                participants.add(Participant(id = null, name = clean, resolved = null))
            }
        }

        if (participants.size < 2) return emptyList()
        val k = teamCount.coerceIn(2, participants.size.coerceAtMost(6))

        val knownTimes = participants.mapNotNull { it.resolved?.millis }.sorted()
        val medianMillis = if (knownTimes.isNotEmpty()) {
            knownTimes[knownTimes.size / 2]
        } else {
            legDistanceMeters * 700L
        }

        fun effectiveWeight(p: Participant): Long = p.resolved?.millis ?: medianMillis

        val sortedAll = participants.sortedWith(
            compareBy<Participant> { it.resolved == null }
                .thenBy { effectiveWeight(it) }
                .thenBy { it.name.lowercase() }
        )

        val sorted = if (legsPerTeam != null && legsPerTeam in 2..8) {
            sortedAll.take(k * legsPerTeam)
        } else {
            sortedAll
        }

        val buckets = List(k) { mutableListOf<Participant>() }
        val bucketSums = LongArray(k)

        // Assign in rounds of k so all teams stay within +/- 1 size while minimizing sum spread
        sorted.chunked(k).forEach { tier ->
            val availableTeamIndices = (0 until k).sortedBy { bucketSums[it] }.toMutableList()
            // Pair slowest in tier with lowest sum team, fastest in tier with highest sum team in this tier
            val tierDesc = tier.sortedByDescending { effectiveWeight(it) }
            tierDesc.forEachIndexed { idx, participant ->
                val targetTeam = availableTeamIndices[idx]
                buckets[targetTeam].add(participant)
                bucketSums[targetTeam] += effectiveWeight(participant)
            }
        }

        // Local 2-opt refinement between same-rank slots across teams to minimize spread of average team time
        fun currentSpread(): Double {
            val avgs = buckets.indices.map { i ->
                if (buckets[i].isEmpty()) 0.0 else bucketSums[i].toDouble() / buckets[i].size
            }
            return (avgs.maxOrNull() ?: 0.0) - (avgs.minOrNull() ?: 0.0)
        }

        var improved = true
        var iterations = 0
        while (improved && iterations < 50) {
            improved = false
            iterations++
            val baseSpread = currentSpread()
            outer@ for (t1 in 0 until k) {
                for (t2 in t1 + 1 until k) {
                    for (i1 in buckets[t1].indices) {
                        for (i2 in buckets[t2].indices) {
                            val p1 = buckets[t1][i1]
                            val p2 = buckets[t2][i2]
                            val w1 = effectiveWeight(p1)
                            val w2 = effectiveWeight(p2)
                            if (w1 == w2) continue

                            buckets[t1][i1] = p2
                            buckets[t2][i2] = p1
                            bucketSums[t1] = bucketSums[t1] - w1 + w2
                            bucketSums[t2] = bucketSums[t2] - w2 + w1

                            val newSpread = currentSpread()
                            if (newSpread + 1.0 < baseSpread) {
                                improved = true
                                break@outer
                            } else {
                                // Revert swap
                                buckets[t1][i1] = p1
                                buckets[t2][i2] = p2
                                bucketSums[t1] = bucketSums[t1] - w2 + w1
                                bucketSums[t2] = bucketSums[t2] - w1 + w2
                            }
                        }
                    }
                }
            }
        }

        val hasAnyKnownTime = knownTimes.isNotEmpty()
        val teams = buckets.filter { it.isNotEmpty() }.mapIndexed { idx, members ->
            // Put fastest swimmer last as anchor
            val orderedMembers = if (members.size >= 2 && hasAnyKnownTime) {
                val bySpeed = members.sortedBy { effectiveWeight(it) }
                bySpeed.drop(1) + bySpeed.first()
            } else {
                members
            }

            val legs = orderedMembers.mapIndexed { legIdx, p ->
                val resolved = p.resolved
                val timeToUse = resolved?.millis ?: if (hasAnyKnownTime) medianMillis else null
                val isEst = resolved?.isEstimated == true || (resolved == null && hasAnyKnownTime)
                val srcLabel = resolved?.sourceLabel ?: if (hasAnyKnownTime) "~Schnitt" else ""
                RelayLegAssignment(
                    legPosition = legIdx + 1,
                    swimmerId = p.id,
                    swimmerName = p.name,
                    stroke = stroke,
                    distanceMeters = legDistanceMeters,
                    timeMillis = timeToUse,
                    isEstimated = isEst,
                    sourceLabel = srcLabel
                )
            }

            val total = if (hasAnyKnownTime && legs.isNotEmpty()) legs.sumOf { it.timeMillis ?: 0L } else null
            val avg = if (total != null && legs.isNotEmpty()) total / legs.size else null

            RelayTeamPlan(
                teamIndex = idx,
                teamTitle = "Staffel-Team ${idx + 1}",
                legs = legs,
                totalTimeMillis = total,
                averageLegMillis = avg,
                deltaToFastestMillis = null,
                hasEstimatedTimes = legs.any { it.isEstimated }
            )
        }

        return attachDeltas(teams)
    }

    /**
     * Random shuffle into [teamCount] teams while still attaching known PBs for transparency.
     */
    fun buildRandomTeams(
        swimmers: List<SwimmerEntity>,
        guestNames: List<String> = emptyList(),
        teamCount: Int = 2,
        stroke: String = "Freistil",
        legDistanceMeters: Int = 50,
        legsPerTeam: Int? = null,
        random: kotlin.random.Random = kotlin.random.Random.Default
    ): List<RelayTeamPlan> {
        data class Participant(
            val id: Long?,
            val name: String,
            val resolved: ResolvedLegTime?
        )

        val seenNames = mutableSetOf<String>()
        val participants = mutableListOf<Participant>()

        swimmers.forEach { s ->
            val clean = s.name.trim()
            if (clean.isNotEmpty() && seenNames.add(clean.lowercase())) {
                participants.add(
                    Participant(
                        id = s.id,
                        name = clean,
                        resolved = resolveSwimmerTime(s, stroke, legDistanceMeters)
                    )
                )
            }
        }
        guestNames.forEach { g ->
            val clean = g.trim()
            if (clean.isNotEmpty() && seenNames.add(clean.lowercase())) {
                participants.add(Participant(id = null, name = clean, resolved = null))
            }
        }

        if (participants.size < 2) return emptyList()
        val k = teamCount.coerceIn(2, participants.size.coerceAtMost(6))
        val shuffledAll = participants.shuffled(random)
        val shuffled = if (legsPerTeam != null && legsPerTeam in 2..8) {
            shuffledAll.take(k * legsPerTeam)
        } else {
            shuffledAll
        }
        val buckets = List(k) { mutableListOf<Participant>() }
        shuffled.forEachIndexed { idx, p ->
            buckets[idx % k].add(p)
        }

        val teams = buckets.filter { it.isNotEmpty() }.mapIndexed { idx, members ->
            val legs = members.mapIndexed { legIdx, p ->
                RelayLegAssignment(
                    legPosition = legIdx + 1,
                    swimmerId = p.id,
                    swimmerName = p.name,
                    stroke = stroke,
                    distanceMeters = legDistanceMeters,
                    timeMillis = p.resolved?.millis,
                    isEstimated = p.resolved?.isEstimated == true,
                    sourceLabel = p.resolved?.sourceLabel ?: ""
                )
            }
            val allHaveTimes = legs.isNotEmpty() && legs.all { it.timeMillis != null }
            val total = if (allHaveTimes) legs.sumOf { it.timeMillis ?: 0L } else null
            val known = legs.mapNotNull { it.timeMillis }
            val avg = if (known.isNotEmpty()) known.sum() / known.size else null

            RelayTeamPlan(
                teamIndex = idx,
                teamTitle = "Staffel-Team ${idx + 1}",
                legs = legs,
                totalTimeMillis = total,
                averageLegMillis = avg,
                deltaToFastestMillis = null,
                hasEstimatedTimes = legs.any { it.isEstimated }
            )
        }

        return attachDeltas(teams)
    }

    private fun attachDeltas(teams: List<RelayTeamPlan>): List<RelayTeamPlan> {
        val sameSize = teams.map { it.legs.size }.distinct().size <= 1
        if (sameSize) {
            val fastestTotal = teams.mapNotNull { it.totalTimeMillis }.minOrNull() ?: return teams
            return teams.map { team ->
                val delta = team.totalTimeMillis?.let { (it - fastestTotal).coerceAtLeast(0L) }
                team.copy(deltaToFastestMillis = delta)
            }
        } else {
            val fastestAvg = teams.mapNotNull { it.averageLegMillis }.minOrNull() ?: return teams
            return teams.map { team ->
                val delta = team.averageLegMillis?.let { (it - fastestAvg).coerceAtLeast(0L) }
                team.copy(deltaToFastestMillis = delta)
            }
        }
    }
}
