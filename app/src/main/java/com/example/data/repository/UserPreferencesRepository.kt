package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences

class UserPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("swim_timer_preferences", Context.MODE_PRIVATE)

    fun getPoolLengthMeters(default: Int = 50): Int = prefs.getInt("poolLengthMeters", default)
    fun getTargetDistanceMeters(default: Int = 100): Int = prefs.getInt("targetDistanceMeters", default)
    fun getSplitDistanceMeters(default: Int = 50): Int = prefs.getInt("splitDistanceMeters", default)
    fun getStrokeType(default: String = "Freistil"): String = prefs.getString("strokeType", default) ?: default
    fun getStartMode(default: String = "MASS"): String = prefs.getString("startMode", default) ?: default
    fun getIntervalOffsetSeconds(default: Int = 5): Int = prefs.getInt("intervalOffsetSeconds", default)
    fun getLaneCount(default: Int = 4): Int = prefs.getInt("laneCount", default)
    fun getCustomGroups(): Set<String> = prefs.getStringSet("customGroups", emptySet()) ?: emptySet()

    fun savePreferences(
        poolLengthMeters: Int,
        targetDistanceMeters: Int,
        splitDistanceMeters: Int,
        strokeType: String,
        startMode: String,
        intervalOffsetSeconds: Int,
        laneCount: Int
    ) {
        prefs.edit().apply {
            putInt("poolLengthMeters", poolLengthMeters)
            putInt("targetDistanceMeters", targetDistanceMeters)
            putInt("splitDistanceMeters", splitDistanceMeters)
            putString("strokeType", strokeType)
            putString("startMode", startMode)
            putInt("intervalOffsetSeconds", intervalOffsetSeconds)
            putInt("laneCount", laneCount)
            apply()
        }
    }

    fun saveCustomGroups(groups: Set<String>) {
        prefs.edit().putStringSet("customGroups", groups).apply()
    }
}
