package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeFormatter {

    /**
     * Formats milliseconds into standard swimming digital format: MM:SS.cc
     * If under 1 minute and shortFormat is true: SS.cc
     */
    fun formatMillis(millis: Long, shortFormat: Boolean = false): String {
        if (millis < 0) return "00.00"
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val hundredths = (millis % 1000) / 10

        return if (minutes > 0 || !shortFormat) {
            String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, hundredths)
        } else {
            String.format(Locale.US, "%02d.%02d", seconds, hundredths)
        }
    }

    fun formatDetailed(millis: Long): String = formatMillis(millis, shortFormat = false)

    /**
     * Formats difference from target pace: e.g. -0.45s or +1.20s
     */
    fun formatDelta(deltaMillis: Long?): String {
        if (deltaMillis == null) return ""
        val sign = if (deltaMillis <= 0) "-" else "+"
        val absMillis = Math.abs(deltaMillis)
        val seconds = absMillis / 1000
        val hundredths = (absMillis % 1000) / 10
        return String.format(Locale.US, "%s%d.%02ds", sign, seconds, hundredths)
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY)
        return sdf.format(Date(timestamp))
    }

    fun formatDateShort(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd.MM.yy", Locale.GERMANY)
        return sdf.format(Date(timestamp))
    }

    /**
     * Calculates and formats pace per 100m: e.g. 01:14.50/100m
     */
    fun formatPacePer100m(lapTimeMillis: Long, splitDistanceMeters: Int): String {
        if (lapTimeMillis <= 0 || splitDistanceMeters <= 0) return "--:--"
        val pace100mMillis = (lapTimeMillis.toDouble() / splitDistanceMeters * 100).toLong()
        return "${formatMillis(pace100mMillis, shortFormat = false)}/100m"
    }
}
