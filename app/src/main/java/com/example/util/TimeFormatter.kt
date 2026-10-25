package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object TimeFormatter {

    /**
     * Formats milliseconds into standard swimming digital format: MM:SS.cc
     * If under 1 minute and shortFormat is true: SS.cc
     */
    fun formatMillis(millis: Long, shortFormat: Boolean = false): String {
        val safeMillis = millis.coerceAtLeast(0L)
        val minutes = (safeMillis / 60_000L).toInt()
        val seconds = ((safeMillis % 60_000L) / 1_000L).toInt()
        val hundredths = ((safeMillis % 1_000L) / 10L).toInt()

        if (shortFormat && minutes == 0) {
            return buildString(6) {
                if (seconds < 10) append('0')
                append(seconds)
                append('.')
                if (hundredths < 10) append('0')
                append(hundredths)
            }
        }

        return buildString(9) {
            if (minutes < 10) append('0')
            append(minutes)
            append(':')
            if (seconds < 10) append('0')
            append(seconds)
            append('.')
            if (hundredths < 10) append('0')
            append(hundredths)
        }
    }

    fun formatDetailed(millis: Long): String = formatMillis(millis, shortFormat = false)

    /**
     * Formats whole seconds into standard MM:SS format (e.g. 00:05, 01:15)
     */
    fun formatSeconds(totalSeconds: Long): String {
        if (totalSeconds <= 0) return "00:00"
        val m = totalSeconds / 60
        val s = totalSeconds % 60
        return buildString(5) {
            if (m < 10) append('0')
            append(m)
            append(':')
            if (s < 10) append('0')
            append(s)
        }
    }

    /**
     * Formats milliseconds for text input fields (e.g. "24.80" or "01:04.50")
     */
    fun formatForInput(millis: Long?): String {
        if (millis == null || millis <= 0L) return ""
        return formatMillis(millis, shortFormat = millis < 60_000L)
    }

    /**
     * Parses both "MM:SS.cc" (e.g. "1:04.50") and seconds ("64.50" or "64,5") into milliseconds.
     */
    fun parseTimeInputToMillis(input: String): Long? {
        val cleaned = input.trim().replace(',', '.')
        if (cleaned.isEmpty()) return null
        return try {
            if (cleaned.contains(":")) {
                val parts = cleaned.split(":")
                if (parts.size != 2) return null
                val min = parts[0].trim().toLongOrNull() ?: return null
                val secDouble = parts[1].trim().toDoubleOrNull() ?: return null
                val totalMillis = (min * 60_000L) + (secDouble * 1000.0).roundToLong()
                if (totalMillis > 0L) totalMillis else null
            } else {
                val secDouble = cleaned.toDoubleOrNull() ?: return null
                val totalMillis = (secDouble * 1000.0).roundToLong()
                if (totalMillis > 0L) totalMillis else null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Formats difference from target pace: e.g. -0.45s or +1.20s
     */
    fun formatDelta(deltaMillis: Long?): String {
        if (deltaMillis == null) return ""
        val sign = if (deltaMillis <= 0) '-' else '+'
        val absMillis = abs(deltaMillis)
        val s = absMillis / 1000
        val h = (absMillis % 1000) / 10
        
        return buildString(8) {
            append(sign)
            append(s)
            append('.')
            if (h < 10) append('0')
            append(h)
            append('s')
        }
    }

    private val dateFormatThreadLocal = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue() = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY)
    }
    private val dateFormatShortThreadLocal = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue() = SimpleDateFormat("dd.MM.yy", Locale.GERMANY)
    }

    fun formatDate(timestamp: Long): String {
        return dateFormatThreadLocal.get()?.format(Date(timestamp)) ?: ""
    }

    fun formatDateShort(timestamp: Long): String {
        return dateFormatShortThreadLocal.get()?.format(Date(timestamp)) ?: ""
    }

    /**
     * Calculates and formats pace per 100m: e.g. 01:14.50/100m
     */
    fun formatPacePer100m(lapTimeMillis: Long, splitDistanceMeters: Int): String {
        if (lapTimeMillis <= 0 || splitDistanceMeters <= 0) return "--:--"
        val pace100mMillis = (lapTimeMillis.toDouble() / splitDistanceMeters * 100.0).roundToLong()
        return "${formatMillis(pace100mMillis, shortFormat = false)}/100m"
    }
}