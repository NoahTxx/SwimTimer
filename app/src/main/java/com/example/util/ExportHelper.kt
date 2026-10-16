package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.SessionWithLaps
import com.example.data.model.SwimmerEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object ExportHelper {

    fun generateTextSummary(sessionWithLaps: SessionWithLaps): String {
        val s = sessionWithLaps.session
        val sb = StringBuilder()
        sb.append("SWIMTIMER - TRAININGSBERICHT\n")
        sb.append("----------------------------------------\n")
        sb.append("Datum: ${TimeFormatter.formatDate(s.timestamp)}\n")
        sb.append("Titel: ${s.title}\n")
        sb.append("Disziplin: ${s.strokeType} | Becken: ${s.poolLengthMeters}m | Distanz: ${s.targetDistanceMeters}m\n")
        sb.append("Start-Modus: ${if (s.startMode == "INTERVAL") "Intervall (${s.intervalOffsetSeconds}s)" else "Gemeinsamer Start"}\n")
        if (s.notes.isNotBlank()) {
            sb.append("Notizen: ${s.notes}\n")
        }
        sb.append("\nERGEBNISSE & SPLITS:\n")

        val isLagen = s.strokeType.equals("Lagen", ignoreCase = true)
        val legDist = (s.targetDistanceMeters / 4).coerceAtLeast(1)
        val strokeNames = listOf("1. Schmett", "2. Rücken", "3. Brust", "4. Freistil")

        val grouped = sessionWithLaps.laps.groupBy { it.laneNumber }
        grouped.keys.sorted().forEach { lane ->
            val laps = grouped[lane] ?: emptyList()
            val swimmer = laps.firstOrNull()?.swimmerName ?: "Bahn $lane"
            val totalTime = laps.lastOrNull()?.splitTimeMillis ?: 0L
            sb.append("\nBahn $lane: $swimmer (Gesamt: ${TimeFormatter.formatMillis(totalTime)})\n")
            laps.forEach { lap ->
                val strokeTag = if (isLagen) {
                    val legIdx = (((lap.lapIndex - 1) * s.splitDistanceMeters) / legDist).coerceIn(0, 3)
                    " (${strokeNames[legIdx]})"
                } else ""
                val paceDelta = if (lap.deltaTargetMillis != null) {
                    " [Pace: ${TimeFormatter.formatDelta(lap.deltaTargetMillis)}]"
                } else ""
                val sr = if (lap.strokeRateSpm != null) {
                    " (Takt: %.1f SPM)".format(lap.strokeRateSpm)
                } else ""
                sb.append("  • Lap ${lap.lapIndex}$strokeTag: Split ${TimeFormatter.formatMillis(lap.splitTimeMillis)} | Runde: ${TimeFormatter.formatMillis(lap.lapTimeMillis)}$paceDelta$sr\n")
            }
        }
        sb.append("\n----------------------------------------\n")
        sb.append("Erfasst mit SwimTimer am Beckenrand")
        return sb.toString()
    }

    fun generateCsv(sessionWithLaps: SessionWithLaps): String {
        val s = sessionWithLaps.session
        val isLagen = s.strokeType.equals("Lagen", ignoreCase = true)
        val legDist = (s.targetDistanceMeters / 4).coerceAtLeast(1)
        val strokeNames = listOf("1. Schmett", "2. Rücken", "3. Brust", "4. Freistil")

        fun escapeCsv(value: String): String {
            return if (value.contains("\"") || value.contains(";") || value.contains("\n")) {
                "\"${value.replace("\"", "\"\"")}\""
            } else {
                value
            }
        }

        val sb = StringBuilder()
        sb.append("\uFEFF") // UTF-8 BOM
        sb.append("SessionID;Datum;Titel;Disziplin;Beckenlaenge_m;Zielstrecke_m;Startmodus;Intervall_s\n")
        sb.append("${s.id};${TimeFormatter.formatDate(s.timestamp)};${escapeCsv(s.title)};${escapeCsv(s.strokeType)};${s.poolLengthMeters};${s.targetDistanceMeters};${s.startMode};${s.intervalOffsetSeconds}\n\n")

        sb.append("Bahn;Schwimmer;Lap_Nr;Lage;Split_Zeit_ms;Split_Formatiert;Runden_Zeit_ms;Runde_Formatiert;Soll_Zeit_ms;Delta_Soll_ms;Schlagfrequenz_SPM\n")
        sessionWithLaps.laps.sortedWith(compareBy({ it.laneNumber }, { it.lapIndex })).forEach { lap ->
            val strokeName = if (isLagen) {
                val legIdx = (((lap.lapIndex - 1) * s.splitDistanceMeters) / legDist).coerceIn(0, 3)
                strokeNames[legIdx]
            } else s.strokeType
            sb.append("${lap.laneNumber};${escapeCsv(lap.swimmerName)};${lap.lapIndex};${escapeCsv(strokeName)};${lap.splitTimeMillis};${TimeFormatter.formatMillis(lap.splitTimeMillis)};${lap.lapTimeMillis};${TimeFormatter.formatMillis(lap.lapTimeMillis)};${lap.targetLapMillis ?: ""};${lap.deltaTargetMillis ?: ""};${lap.strokeRateSpm ?: ""}\n")
        }
        return sb.toString()
    }

    fun shareSession(context: Context, sessionWithLaps: SessionWithLaps) {
        val summaryText = generateTextSummary(sessionWithLaps)
        val csvContent = generateCsv(sessionWithLaps)

        try {
            val fileName = "SwimSession_${sessionWithLaps.session.id}_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { out ->
                out.write(csvContent.toByteArray(Charsets.UTF_8))
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Schwimmzeiten: ${sessionWithLaps.session.title}")
                putExtra(Intent.EXTRA_TEXT, summaryText)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Training exportieren & teilen"))
        } catch (e: Exception) {
            // Fallback to text sharing if file writing fails
            val textIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Schwimmzeiten: ${sessionWithLaps.session.title}")
                putExtra(Intent.EXTRA_TEXT, summaryText)
            }
            context.startActivity(Intent.createChooser(textIntent, "Training teilen (Text)"))
        }
    }

    fun exportSwimmersToJson(swimmers: List<SwimmerEntity>): String {
        val jsonArray = JSONArray()
        swimmers.forEach { swimmer ->
            val obj = JSONObject().apply {
                put("name", swimmer.name)
                put("ageGroup", swimmer.ageGroup)
                put("favoriteStroke", swimmer.favoriteStroke)
                swimmer.pb25mMillis?.let { put("pb25mMillis", it) }
                swimmer.pb50mMillis?.let { put("pb50mMillis", it) }
                swimmer.pb100mMillis?.let { put("pb100mMillis", it) }
                swimmer.pb100mLagenMillis?.let { put("pb100mLagenMillis", it) }
                swimmer.lagenFlySplitMillis?.let { put("lagenFlySplitMillis", it) }
                swimmer.lagenBackSplitMillis?.let { put("lagenBackSplitMillis", it) }
                swimmer.lagenBreastSplitMillis?.let { put("lagenBreastSplitMillis", it) }
                swimmer.lagenFreeSplitMillis?.let { put("lagenFreeSplitMillis", it) }
                swimmer.pb200mLagenMillis?.let { put("pb200mLagenMillis", it) }
                swimmer.pb400mLagenMillis?.let { put("pb400mLagenMillis", it) }
                swimmer.pb200mLagenFlySplitMillis?.let { put("pb200mLagenFlySplitMillis", it) }
                swimmer.pb200mLagenBackSplitMillis?.let { put("pb200mLagenBackSplitMillis", it) }
                swimmer.pb200mLagenBreastSplitMillis?.let { put("pb200mLagenBreastSplitMillis", it) }
                swimmer.pb200mLagenFreeSplitMillis?.let { put("pb200mLagenFreeSplitMillis", it) }
                swimmer.pb400mLagenFlySplitMillis?.let { put("pb400mLagenFlySplitMillis", it) }
                swimmer.pb400mLagenBackSplitMillis?.let { put("pb400mLagenBackSplitMillis", it) }
                swimmer.pb400mLagenBreastSplitMillis?.let { put("pb400mLagenBreastSplitMillis", it) }
                swimmer.pb400mLagenFreeSplitMillis?.let { put("pb400mLagenFreeSplitMillis", it) }
                put("colorHex", swimmer.colorHex)
                put("notes", swimmer.notes)
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString(2)
    }

    fun importSwimmersFromJson(jsonContent: String): List<SwimmerEntity> {
        val list = mutableListOf<SwimmerEntity>()
        val array = JSONArray(jsonContent)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            fun optLongNullable(key: String): Long? = if (obj.has(key) && !obj.isNull(key)) obj.getLong(key) else null

            list.add(
                SwimmerEntity(
                    id = 0,
                    name = obj.optString("name", "Unbekannt"),
                    ageGroup = obj.optString("ageGroup", "Offen"),
                    favoriteStroke = obj.optString("favoriteStroke", "Freistil"),
                    pb25mMillis = optLongNullable("pb25mMillis"),
                    pb50mMillis = optLongNullable("pb50mMillis"),
                    pb100mMillis = optLongNullable("pb100mMillis"),
                    pb100mLagenMillis = optLongNullable("pb100mLagenMillis"),
                    lagenFlySplitMillis = optLongNullable("lagenFlySplitMillis"),
                    lagenBackSplitMillis = optLongNullable("lagenBackSplitMillis"),
                    lagenBreastSplitMillis = optLongNullable("lagenBreastSplitMillis"),
                    lagenFreeSplitMillis = optLongNullable("lagenFreeSplitMillis"),
                    pb200mLagenMillis = optLongNullable("pb200mLagenMillis"),
                    pb400mLagenMillis = optLongNullable("pb400mLagenMillis"),
                    pb200mLagenFlySplitMillis = optLongNullable("pb200mLagenFlySplitMillis"),
                    pb200mLagenBackSplitMillis = optLongNullable("pb200mLagenBackSplitMillis"),
                    pb200mLagenBreastSplitMillis = optLongNullable("pb200mLagenBreastSplitMillis"),
                    pb200mLagenFreeSplitMillis = optLongNullable("pb200mLagenFreeSplitMillis"),
                    pb400mLagenFlySplitMillis = optLongNullable("pb400mLagenFlySplitMillis"),
                    pb400mLagenBackSplitMillis = optLongNullable("pb400mLagenBackSplitMillis"),
                    pb400mLagenBreastSplitMillis = optLongNullable("pb400mLagenBreastSplitMillis"),
                    pb400mLagenFreeSplitMillis = optLongNullable("pb400mLagenFreeSplitMillis"),
                    colorHex = obj.optLong("colorHex", 0xFF00E5FF),
                    notes = obj.optString("notes", "")
                )
            )
        }
        return list
    }

    fun shareSwimmersExport(context: Context, swimmers: List<SwimmerEntity>) {
        val json = exportSwimmersToJson(swimmers)
        try {
            val fileName = "SwimTimer_Athleten_Export_${System.currentTimeMillis()}.json"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_SUBJECT, "SwimTimer Athleten Export (${swimmers.size} Schwimmer)")
                putExtra(Intent.EXTRA_TEXT, "Anbei der Export deiner ${swimmers.size} Athleten für SwimTimer.")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Athleten exportieren"))
        } catch (e: Exception) {
            val textIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "SwimTimer Athleten Export")
                putExtra(Intent.EXTRA_TEXT, json)
            }
            context.startActivity(Intent.createChooser(textIntent, "Athleten exportieren (Text)"))
        }
    }
}
