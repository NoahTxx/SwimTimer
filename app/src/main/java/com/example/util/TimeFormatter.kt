package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object TimeFormatter {

    fun writeMillisToBuffer(millis: Long, buffer: CharArray): Int {
        val safeMillis = millis.coerceAtLeast(0L)
        val totalSeconds = (safeMillis / 1_000L).toInt()
        val minutes = (totalSeconds / 60).coerceAtMost(99)
        val seconds = totalSeconds % 60
        val hundredths = ((safeMillis % 1_000L) / 10L).toInt()

        buffer[0] = ('0'.code + (minutes / 10)).toChar()
        buffer[1] = ('0'.code + (minutes % 10)).toChar()
        buffer[2] = ':'
        buffer[3] = ('0'.code + (seconds / 10)).toChar()
        buffer[4] = ('0'.code + (seconds % 10)).toChar()
        buffer[5] = '.'
        buffer[6] = ('0'.code + (hundredths / 10)).toChar()
        buffer[7] = ('0'.code + (hundredths % 10)).toChar()
        return 8
    }

    @Suppress("unused")
    fun writeSignedMillisToBuffer(signedMillis: Long, buffer: CharArray): Int {
        val isCountdown = signedMillis < 0L
        val absMillis = abs(signedMillis)
        val totalSeconds = (absMillis / 1_000L).toInt()
        val minutes = (totalSeconds / 60).coerceAtMost(99)
        val seconds = totalSeconds % 60
        val hundredths = ((absMillis % 1_000L) / 10L).toInt()

        var idx = 0
        if (isCountdown) {
            buffer[idx++] = '-'
        }
        buffer[idx++] = ('0'.code + (minutes / 10)).toChar()
        buffer[idx++] = ('0'.code + (minutes % 10)).toChar()
        buffer[idx++] = ':'
        buffer[idx++] = ('0'.code + (seconds / 10)).toChar()
        buffer[idx++] = ('0'.code + (seconds % 10)).toChar()
        buffer[idx++] = '.'
        buffer[idx++] = ('0'.code + (hundredths / 10)).toChar()
        buffer[idx++] = ('0'.code + (hundredths % 10)).toChar()
        return idx
    }

    @Suppress("unused")
    fun formatMillisFast(millis: Long): String {
        val buf = CharArray(8)
        writeMillisToBuffer(millis, buf)
        return String(buf)
    }

    fun formatMillis(millis: Long, shortFormat: Boolean = false): String {
        val safeMillis = millis.coerceAtLeast(0L)
        val minutes = (safeMillis / 60_000L).toInt()
        val seconds = ((safeMillis % 60_000L) / 1_000L).toInt()
        val hundredths = ((safeMillis % 1_000L) / 10L).toInt()

        if (shortFormat && (minutes == 0)) {
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

    @Suppress("unused")
    fun formatDetailed(millis: Long): String = formatMillis(millis, shortFormat = false)

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

    fun formatForInput(millis: Long?): String {
        if ((millis == null) || (millis <= 0L)) return ""
        return formatMillis(millis, shortFormat = millis < 60_000L)
    }

    fun parseTimeInputToMillis(input: String): Long? {
        val cleaned = input.trim().replace(',', '.')
        if (cleaned.isEmpty() || cleaned.startsWith("-")) return null
        return try {
            if (cleaned.contains(":")) {
                val parts = cleaned.split(":")
                if (parts.size != 2) return null
                val min = parts[0].trim().toLongOrNull() ?: return null
                if (min < 0) return null
                val secDouble = parts[1].trim().toDoubleOrNull() ?: return null
                if (secDouble < 0.0 || secDouble >= 60.0) return null
                val totalMillis = (min * 60_000L) + (secDouble * 1000.0).roundToLong()
                if (totalMillis in 1L..3_600_000L) totalMillis else null
            } else {
                val secDouble = cleaned.toDoubleOrNull() ?: return null
                if (secDouble <= 0.0) return null
                val totalMillis = (secDouble * 1000.0).roundToLong()
                if (totalMillis in 1L..3_600_000L) totalMillis else null
            }
        } catch (_: Exception) {
            null
        }
    }

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

    @Suppress("unused")
    fun formatDateShort(timestamp: Long): String {
        return dateFormatShortThreadLocal.get()?.format(Date(timestamp)) ?: ""
    }

    fun formatPacePer100m(lapTimeMillis: Long, splitDistanceMeters: Int): String {
        if ((lapTimeMillis <= 0) || (splitDistanceMeters <= 0)) return "--:--"
        val pace100mMillis = ((lapTimeMillis.toDouble() / splitDistanceMeters) * 100.0).roundToLong()
        return "${formatMillis(pace100mMillis, shortFormat = false)}/100m"
    }
}

