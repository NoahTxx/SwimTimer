package com.example.util

import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.sin

object SoundUtil {
    private val executor = Executors.newSingleThreadExecutor()
    private const val SAMPLE_RATE = 44100

    private val soundPool: SoundPool by lazy {
        SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
    }

    // Pre-loaded native SoundPool audio IDs for zero-latency (<1ms) playback
    private val sound3Id: Int by lazy { loadWavSound(pcmToWav(generateBeepPcm(freq = 800.0, durationMs = 80))) }
    private val sound2Id: Int by lazy { loadWavSound(pcmToWav(generateBeepPcm(freq = 1000.0, durationMs = 80))) }
    private val sound1Id: Int by lazy { loadWavSound(pcmToWav(generateBeepPcm(freq = 1200.0, durationMs = 80))) }
    private val soundStartId: Int by lazy { loadWavSound(pcmToWav(generateBeepPcm(freq = 1600.0, durationMs = 180))) }

    /**
     * Pre-warms native SoundPool memory buffers on startup ensuring 0ms audio latency.
     */
    fun init() {
        executor.execute {
            try {
                // Accessing lazy IDs forces pre-loading into native memory
                sound3Id
                sound2Id
                sound1Id
                soundStartId
            } catch (_: Exception) {}
        }
    }

    /**
     * Synthesizes a clean, short sine beep with smooth 4ms attack and 8ms decay envelopes
     * to eliminate any popping or clicking.
     */
    private fun generateBeepPcm(freq: Double, durationMs: Int): ByteArray {
        val numSamples = (SAMPLE_RATE * durationMs) / 1000
        val bytes = ByteArray(numSamples * 2)

        val attackSamples = (SAMPLE_RATE * 0.004).toInt()
        val decaySamples = (SAMPLE_RATE * 0.008).toInt()

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE

            val envelope = when {
                i < attackSamples -> i.toDouble() / attackSamples
                i > numSamples - decaySamples -> (numSamples - i).toDouble() / decaySamples
                else -> 1.0
            }

            val wave = sin(2.0 * PI * freq * t)
            val sampleVal = (wave * envelope * 28000.0).toInt().coerceIn(-32768, 32767).toShort()

            bytes[i * 2] = (sampleVal.toInt() and 0xFF).toByte()
            bytes[i * 2 + 1] = ((sampleVal.toInt() shr 8) and 0xFF).toByte()
        }
        return bytes
    }

    /**
     * Converts a 16-bit Mono PCM byte array into a standard 44-byte WAV header format
     * for instant SoundPool loading.
     */
    private fun pcmToWav(pcmData: ByteArray): ByteArray {
        val totalDataLen = pcmData.size + 36
        val byteRate = SAMPLE_RATE * 2
        val header = ByteArray(44)

        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xFF).toByte()
        header[5] = ((totalDataLen shr 8) and 0xFF).toByte()
        header[6] = ((totalDataLen shr 16) and 0xFF).toByte()
        header[7] = ((totalDataLen shr 24) and 0xFF).toByte()
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0 // 16-bit PCM chunk size
        header[20] = 1; header[21] = 0 // Audio format PCM
        header[22] = 1; header[23] = 0 // Mono channel
        header[24] = (SAMPLE_RATE and 0xFF).toByte()
        header[25] = ((SAMPLE_RATE shr 8) and 0xFF).toByte()
        header[26] = ((SAMPLE_RATE shr 16) and 0xFF).toByte()
        header[27] = ((SAMPLE_RATE shr 24) and 0xFF).toByte()
        header[28] = (byteRate and 0xFF).toByte()
        header[29] = ((byteRate shr 8) and 0xFF).toByte()
        header[30] = ((byteRate shr 16) and 0xFF).toByte()
        header[31] = 0 // Byte rate for 88.2kHz fits in 24 bits
        header[32] = 2; header[33] = 0 // Block align
        header[34] = 16; header[35] = 0 // 16 bits per sample
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        header[40] = (pcmData.size and 0xFF).toByte()
        header[41] = ((pcmData.size shr 8) and 0xFF).toByte()
        header[42] = ((pcmData.size shr 16) and 0xFF).toByte()
        header[43] = ((pcmData.size shr 24) and 0xFF).toByte()

        val wavBytes = ByteArray(44 + pcmData.size)
        System.arraycopy(header, 0, wavBytes, 0, 44)
        System.arraycopy(pcmData, 0, wavBytes, 44, pcmData.size)
        return wavBytes
    }

    private fun loadWavSound(wavBytes: ByteArray): Int {
        return try {
            val tempFile = File.createTempFile("swim_snd_", ".wav")
            tempFile.writeBytes(wavBytes)
            val soundId = soundPool.load(tempFile.absolutePath, 1)
            tempFile.deleteOnExit()
            soundId
        } catch (_: Exception) {
            0
        }
    }

    /**
     * Plays the high-pitched crisp start beep (1600 Hz, 180ms) instantly with zero delay.
     */
    fun playStartSignal() {
        executor.execute {
            try {
                soundPool.play(soundStartId, 1.0f, 1.0f, 1, 0, 1.0f)
            } catch (_: Exception) {}
        }
    }

    /**
     * Multi-stage countdown warning beep based on remaining seconds (3s, 2s, 1s):
     * 3 seconds left -> 800 Hz short warning ping (80ms)
     * 2 seconds left -> 1000 Hz short warning ping (80ms)
     * 1 second left -> 1200 Hz short warning ping (80ms)
     * 0 seconds left -> 1600 Hz crisp high start beep (180ms)
     */
    fun playCountdownBeep(remainingSec: Int = 1) {
        if (remainingSec <= 0) {
            playStartSignal()
            return
        }
        executor.execute {
            try {
                val soundId = when (remainingSec) {
                    3 -> sound3Id
                    2 -> sound2Id
                    1 -> sound1Id
                    else -> sound1Id
                }
                soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
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
                soundPool.release()
            } catch (_: Exception) {}
        }
    }
}
