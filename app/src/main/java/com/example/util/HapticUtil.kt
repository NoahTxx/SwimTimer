package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.util.concurrent.Executors

object HapticUtil {
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile
    private var vibrator: Vibrator? = null

    private fun getVibrator(context: Context): Vibrator? {
        if (vibrator == null) {
            synchronized(this) {
                if (vibrator == null) {
                    vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val manager = context.applicationContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                        manager?.defaultVibrator
                    } else {
                        @Suppress("DEPRECATION")
                        context.applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    }
                }
            }
        }
        return vibrator
    }

    fun vibrateClick(context: Context) {
        executor.execute {
            try {
                val v = getVibrator(context) ?: return@execute
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(35)
                }
            } catch (_: Exception) {}
        }
    }

    fun vibrateHeavy(context: Context) {
        executor.execute {
            try {
                val v = getVibrator(context) ?: return@execute
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(80)
                }
            } catch (_: Exception) {}
        }
    }
}
