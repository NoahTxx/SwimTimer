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
)
