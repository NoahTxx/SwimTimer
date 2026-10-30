package com.example.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "swimmers")
data class SwimmerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val ageGroup: String = "Offen",
    val favoriteStroke: String = "Freistil",
    val pb25mMillis: Long? = null,
    val pb50mMillis: Long? = null,
    val pb100mMillis: Long? = null,
    val pb100mLagenMillis: Long? = null,
    val pb200mLagenMillis: Long? = null,
    val pb400mLagenMillis: Long? = null,
    // 100m Lagen Teilstrecken-Splits (4x 25m)
    val lagenFlySplitMillis: Long? = null,
    val lagenBackSplitMillis: Long? = null,
    val lagenBreastSplitMillis: Long? = null,
    val lagenFreeSplitMillis: Long? = null,
    // 200m Lagen Teilstrecken-Splits (4x 50m)
    val pb200mLagenFlySplitMillis: Long? = null,
    val pb200mLagenBackSplitMillis: Long? = null,
    val pb200mLagenBreastSplitMillis: Long? = null,
    val pb200mLagenFreeSplitMillis: Long? = null,
    // 400m Lagen Teilstrecken-Splits (4x 100m)
    val pb400mLagenFlySplitMillis: Long? = null,
    val pb400mLagenBackSplitMillis: Long? = null,
    val pb400mLagenBreastSplitMillis: Long? = null,
    val pb400mLagenFreeSplitMillis: Long? = null,
    val colorHex: Long = 0xFF00E5FF,
    val notes: String = "",
    val groupName: String? = null,
    val customPbsJson: String? = null,
    val manualPbsJson: String? = null,
) {
    fun createManualPbsSnapshot(): String {
        val json = org.json.JSONObject()
        pb25mMillis?.let { json.put("pb25m", it) }
        pb50mMillis?.let { json.put("pb50m", it) }
        pb100mMillis?.let { json.put("pb100m", it) }
        pb100mLagenMillis?.let { json.put("pb100mLagen", it) }
        pb200mLagenMillis?.let { json.put("pb200mLagen", it) }
        pb400mLagenMillis?.let { json.put("pb400mLagen", it) }
        lagenFlySplitMillis?.let { json.put("lagenFlySplit", it) }
        lagenBackSplitMillis?.let { json.put("lagenBackSplit", it) }
        lagenBreastSplitMillis?.let { json.put("lagenBreastSplit", it) }
        lagenFreeSplitMillis?.let { json.put("lagenFreeSplit", it) }
        pb200mLagenFlySplitMillis?.let { json.put("pb200Fly", it) }
        pb200mLagenBackSplitMillis?.let { json.put("pb200Back", it) }
        pb200mLagenBreastSplitMillis?.let { json.put("pb200Breast", it) }
        pb200mLagenFreeSplitMillis?.let { json.put("pb200Free", it) }
        pb400mLagenFlySplitMillis?.let { json.put("pb400Fly", it) }
        pb400mLagenBackSplitMillis?.let { json.put("pb400Back", it) }
        pb400mLagenBreastSplitMillis?.let { json.put("pb400Breast", it) }
        pb400mLagenFreeSplitMillis?.let { json.put("pb400Free", it) }
        customPbsJson?.let { json.put("customPbsJson", it) }
        return json.toString()
    }

    fun withManualPbsSnapshot(): SwimmerEntity =
        this.copy(manualPbsJson = this.createManualPbsSnapshot())

    fun toBaseSwimmer(): SwimmerEntity {
        if (manualPbsJson.isNullOrBlank()) return this
        return try {
            val json = org.json.JSONObject(manualPbsJson)
            fun optLongOrNull(key: String): Long? = if (json.has(key) && !json.isNull(key)) json.getLong(key) else null
            this.copy(
                pb25mMillis = optLongOrNull("pb25m"),
                pb50mMillis = optLongOrNull("pb50m"),
                pb100mMillis = optLongOrNull("pb100m"),
                pb100mLagenMillis = optLongOrNull("pb100mLagen"),
                pb200mLagenMillis = optLongOrNull("pb200mLagen"),
                pb400mLagenMillis = optLongOrNull("pb400mLagen"),
                lagenFlySplitMillis = optLongOrNull("lagenFlySplit"),
                lagenBackSplitMillis = optLongOrNull("lagenBackSplit"),
                lagenBreastSplitMillis = optLongOrNull("lagenBreastSplit"),
                lagenFreeSplitMillis = optLongOrNull("lagenFreeSplit"),
                pb200mLagenFlySplitMillis = optLongOrNull("pb200Fly"),
                pb200mLagenBackSplitMillis = optLongOrNull("pb200Back"),
                pb200mLagenBreastSplitMillis = optLongOrNull("pb200Breast"),
                pb200mLagenFreeSplitMillis = optLongOrNull("pb200Free"),
                pb400mLagenFlySplitMillis = optLongOrNull("pb400Fly"),
                pb400mLagenBackSplitMillis = optLongOrNull("pb400Back"),
                pb400mLagenBreastSplitMillis = optLongOrNull("pb400Breast"),
                pb400mLagenFreeSplitMillis = optLongOrNull("pb400Free"),
                customPbsJson = if (json.has("customPbsJson") && !json.isNull("customPbsJson")) json.getString("customPbsJson") else null,
            )
        } catch (_: Exception) {
            this
        }
    }

    fun parseCustomPbsMap(): Map<Pair<String, Int>, Long> {
        if (customPbsJson.isNullOrBlank()) return emptyMap()
        val result = mutableMapOf<Pair<String, Int>, Long>()
        try {
            val json = org.json.JSONObject(customPbsJson)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val parts = key.split("_")
                if (parts.size == 2) {
                    val stroke = parts[0]
                    val dist = parts[1].toIntOrNull()
                    val millis = json.optLong(key, -1L)
                    if (dist != null && millis > 0L) {
                        result[stroke to dist] = millis
                    }
                }
            }
        } catch (_: Exception) {}
        return result
    }

    companion object {
        fun serializeCustomPbsMap(map: Map<Pair<String, Int>, Long>): String? {
            if (map.isEmpty()) return null
            val json = org.json.JSONObject()
            map.forEach { (strokeDist, millis) ->
                if (millis > 0L) {
                    val key = "${strokeDist.first}_${strokeDist.second}"
                    json.put(key, millis)
                }
            }
            return json.toString()
        }
    }
}
