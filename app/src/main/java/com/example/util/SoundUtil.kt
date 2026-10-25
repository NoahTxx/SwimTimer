package com.example.util

import android.media.AudioManager
import android.media.ToneGenerator
import java.util.concurrent.Executors

object SoundUtil {
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile
    private var toneGenerator: ToneGenerator? = null

    init {
        executor.execute {
            toneGenerator = try {
                // Use STREAM_ALARM to ensure the start signal is loud and audible even if media is muted
                ToneGenerator(AudioManager.STREAM_ALARM, 100)
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Official swimming electronic start horn/whistle tone (loud 300ms beep)
     */
    fun playStartSignal() {
        executor.execute {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 350)
            } catch (_: Exception) {}
        }
    }

    /**
     * Countdown preparatory beep (100ms)
     */
    fun playCountdownBeep() {
        executor.execute {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 120)
            } catch (_: Exception) {}
        }
    }
}
