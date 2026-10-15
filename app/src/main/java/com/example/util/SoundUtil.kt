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
            try {
                toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            } catch (_: Exception) {
                toneGenerator = null
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
