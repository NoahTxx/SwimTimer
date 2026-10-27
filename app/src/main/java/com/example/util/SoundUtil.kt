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
     * Pleasant, clean start signal (clear 250ms start beep)
     */
    fun playStartSignal() {
        executor.execute {
            try {
                val gen = getOrCreateGenerator() ?: return@execute
                try {
                    gen.startTone(ToneGenerator.TONE_PROP_BEEP, 250)
                } catch (_: Exception) {
                    gen.startTone(ToneGenerator.TONE_DTMF_0, 250)
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Multi-stage countdown beep based on remaining seconds:
     * 3 seconds left -> Low pitch beep
     * 2 seconds left -> Medium pitch beep
     * 1 second left -> High-mid pitch beep
     * 0 seconds left -> Start signal
     */
    fun playCountdownBeep(remainingSec: Int = 1) {
        if (remainingSec <= 0) {
            playStartSignal()
            return
        }
        executor.execute {
            try {
                val gen = getOrCreateGenerator() ?: return@execute
                try {
                    val tone = when (remainingSec) {
                        3 -> ToneGenerator.TONE_PROP_BEEP
                        2 -> ToneGenerator.TONE_PROP_BEEP2
                        1 -> ToneGenerator.TONE_CDMA_PIP
                        else -> ToneGenerator.TONE_PROP_BEEP
                    }
                    gen.startTone(tone, 120)
                } catch (_: Exception) {
                    gen.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
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
