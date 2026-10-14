package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.SessionWithLaps
import java.io.File
import java.io.FileOutputStream

object ExportHelper {

    fun generateTextSummary(sessionWithLaps: SessionWithLaps): String {
        val s = sessionWithLaps.session
        val sb = StringBuilder()
        sb.append("SWIMTIMER PRO - TRAININGSBERICHT\n")
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
        sb.append("Erfasst mit SwimTimer Pro am Beckenrand")
        return sb.toString()
    }

    fun generateCsv(sessionWithLaps: SessionWithLaps): String {
        val s = sessionWithLaps.session
        val isLagen = s.strokeType.equals("Lagen", ignoreCase = true)
        val legDist = (s.targetDistanceMeters / 4).coerceAtLeast(1)
        val strokeNames = listOf("1. Schmett", "2. Rücken", "3. Brust", "4. Freistil")

        val sb = StringBuilder()
        sb.append("SessionID;Datum;Titel;Disziplin;Beckenlaenge_m;Zielstrecke_m;Startmodus;Intervall_s\n")
        sb.append("${s.id};${TimeFormatter.formatDate(s.timestamp)};\"${s.title}\";${s.strokeType};${s.poolLengthMeters};${s.targetDistanceMeters};${s.startMode};${s.intervalOffsetSeconds}\n\n")

        sb.append("Bahn;Schwimmer;Lap_Nr;Lage;Split_Zeit_ms;Split_Formatiert;Runden_Zeit_ms;Runde_Formatiert;Soll_Zeit_ms;Delta_Soll_ms;Schlagfrequenz_SPM\n")
        sessionWithLaps.laps.sortedWith(compareBy({ it.laneNumber }, { it.lapIndex })).forEach { lap ->
            val strokeName = if (isLagen) {
                val legIdx = (((lap.lapIndex - 1) * s.splitDistanceMeters) / legDist).coerceIn(0, 3)
                strokeNames[legIdx]
            } else s.strokeType
            sb.append("${lap.laneNumber};\"${lap.swimmerName}\";${lap.lapIndex};\"$strokeName\";${lap.splitTimeMillis};${TimeFormatter.formatMillis(lap.splitTimeMillis)};${lap.lapTimeMillis};${TimeFormatter.formatMillis(lap.lapTimeMillis)};${lap.targetLapMillis ?: ""};${lap.deltaTargetMillis ?: ""};${lap.strokeRateSpm ?: ""}\n")
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
}
