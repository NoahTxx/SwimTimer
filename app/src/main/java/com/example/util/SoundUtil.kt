package com.example.util

import android.media.AudioManager
import android.media.ToneGenerator

object SoundUtil {
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (_: Exception) {
            toneGenerator = null
        }
    }

    /**
     * Official swimming electronic start horn/whistle tone (loud 300ms beep)
     */
    fun playStartSignal() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 350)
        } catch (_: Exception) {
        }
    }

    /**
     * Countdown preparatory beep (100ms)
     */
    fun playCountdownBeep() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 120)
        } catch (_: Exception) {
        }
    }
}
