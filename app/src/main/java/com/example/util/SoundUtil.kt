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
     * Clear, energetic high-pitched start signal (bright 350ms start beep)
     */
    fun playStartSignal() {
        executor.execute {
            try {
                val gen = getOrCreateGenerator() ?: return@execute
                try {
                    gen.startTone(ToneGenerator.TONE_DTMF_D, 350)
                } catch (_: Exception) {
                    try {
                        gen.startTone(ToneGenerator.TONE_DTMF_9, 350)
                    } catch (_: Exception) {
                        gen.startTone(ToneGenerator.TONE_DTMF_0, 350)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Multi-stage countdown warning beep based on remaining seconds (3s, 2s, 1s):
     * 3 seconds left -> Short low-mid pitch warning beep (100ms)
     * 2 seconds left -> Short mid pitch warning beep (100ms)
     * 1 second left -> Short mid-high pitch warning beep (100ms)
     * 0 seconds left -> High-pitched long start signal
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
                        3 -> ToneGenerator.TONE_DTMF_1
                        2 -> ToneGenerator.TONE_DTMF_5
                        1 -> ToneGenerator.TONE_DTMF_9
                        else -> ToneGenerator.TONE_DTMF_0
                    }
                    gen.startTone(tone, 100)
                } catch (_: Exception) {
                    gen.startTone(ToneGenerator.TONE_DTMF_0, 100)
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
