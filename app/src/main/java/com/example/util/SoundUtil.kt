package com.example.util

import android.media.AudioManager
import android.media.ToneGenerator
import java.util.concurrent.Executors

object SoundUtil {
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile
    private var toneGenerator: ToneGenerator? = null

    private fun getOrCreateGenerator(): ToneGenerator? {
        if (toneGenerator == null) {
            toneGenerator = try {
                // Use STREAM_MUSIC so that sound volume obeys media volume controls
                ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            } catch (_: Exception) {
                null
            }
        }
        return toneGenerator
    }

    /**
     * Official swimming electronic start horn signal (loud high-pitch 400ms beep)
     */
    fun playStartSignal() {
        executor.execute {
            try {
                val gen = getOrCreateGenerator() ?: return@execute
                try {
                    gen.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 400)
                } catch (_: Exception) {
                    gen.startTone(ToneGenerator.TONE_PROP_BEEP, 400)
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Countdown preparatory beep (distinct short mid-pitch 100ms tone)
     */
    fun playCountdownBeep() {
        executor.execute {
            try {
                val gen = getOrCreateGenerator() ?: return@execute
                try {
                    gen.startTone(ToneGenerator.TONE_PROP_BEEP2, 100)
                } catch (_: Exception) {
                    gen.startTone(ToneGenerator.TONE_CDMA_PIP, 100)
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Safely releases native audio resources when app stops or destroys.
     */
    @Suppress("unused")
    fun release() {
        executor.execute {
            try {
                toneGenerator?.release()
            } catch (_: Exception) {}
            toneGenerator = null
        }
    }
}
