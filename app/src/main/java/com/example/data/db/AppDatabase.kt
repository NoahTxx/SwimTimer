package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.LapRecordEntity
import com.example.data.model.SessionEntity
import com.example.data.model.SwimmerEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SwimmerEntity::class,
        SessionEntity::class,
        LapRecordEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun swimmerDao(): SwimmerDao
    abstract fun sessionDao(): SessionDao
    abstract fun lapRecordDao(): LapRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN splitDistanceMeters INTEGER NOT NULL DEFAULT 50")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE swimmers ADD COLUMN pb100mLagenMillis INTEGER")
                db.execSQL("ALTER TABLE swimmers ADD COLUMN pb200mLagenMillis INTEGER")
                db.execSQL("ALTER TABLE swimmers ADD COLUMN lagenFlySplitMillis INTEGER")
                db.execSQL("ALTER TABLE swimmers ADD COLUMN lagenBackSplitMillis INTEGER")
                db.execSQL("ALTER TABLE swimmers ADD COLUMN lagenBreastSplitMillis INTEGER")
                db.execSQL("ALTER TABLE swimmers ADD COLUMN lagenFreeSplitMillis INTEGER")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE swimmers ADD COLUMN pb25mMillis INTEGER")
                db.execSQL("ALTER TABLE swimmers ADD COLUMN pb400mLagenMillis INTEGER")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE swimmers ADD COLUMN pb200mLagenFlySplitMillis INTEGER")
                db.execSQL("ALTER TABLE swimmers ADD COLUMN pb200mLagenBackSplitMillis INTEGER")
                db.execSQL("ALTER TABLE swimmers ADD COLUMN pb200mLagenBreastSplitMillis INTEGER")
                db.execSQL("ALTER TABLE swimmers ADD COLUMN pb200mLagenFreeSplitMillis INTEGER")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE swimmers ADD COLUMN pb400mLagenFlySplitMillis INTEGER")
                db.execSQL("ALTER TABLE swimmers ADD COLUMN pb400mLagenBackSplitMillis INTEGER")
                db.execSQL("ALTER TABLE swimmers ADD COLUMN pb400mLagenBreastSplitMillis INTEGER")
                db.execSQL("ALTER TABLE swimmers ADD COLUMN pb400mLagenFreeSplitMillis INTEGER")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "swim_timer_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Prepopulate with a realistic swim team roster with both standard and Lagen records
                            CoroutineScope(Dispatchers.IO).launch {
                                getInstance(context).swimmerDao().insertSwimmers(
                                    listOf(
                                        SwimmerEntity(
                                            name = "Lukas Weber",
                                            ageGroup = "AK 16",
                                            favoriteStroke = "Freistil",
                                            pb25mMillis = 11900L,
                                            pb50mMillis = 24800L,
                                            pb100mMillis = 54200L,
                                            pb100mLagenMillis = 62400L,
                                            pb200mLagenMillis = 138500L, // 2:18.50
                                            pb400mLagenMillis = 295000L,
                                            // 100m Lagen Splits (4x 25m)
                                            lagenFlySplitMillis = 13800L,
                                            lagenBackSplitMillis = 15900L,
                                            lagenBreastSplitMillis = 18500L,
                                            lagenFreeSplitMillis = 12900L,
                                            // 200m Lagen Splits (4x 50m)
                                            pb200mLagenFlySplitMillis = 29100L,
                                            pb200mLagenBackSplitMillis = 35800L,
                                            pb200mLagenBreastSplitMillis = 40200L,
                                            pb200mLagenFreeSplitMillis = 28400L,
                                            // 400m Lagen Splits (4x 100m)
                                            pb400mLagenFlySplitMillis = 64200L,
                                            pb400mLagenBackSplitMillis = 74800L,
                                            pb400mLagenBreastSplitMillis = 84500L,
                                            pb400mLagenFreeSplitMillis = 61500L,
                                            colorHex = 0xFF00E5FF
                                        ),
                                        SwimmerEntity(
                                            name = "Sophie Schmidt",
                                            ageGroup = "AK 15",
                                            favoriteStroke = "Schmetterling",
                                            pb25mMillis = 12700L,
                                            pb50mMillis = 27900L,
                                            pb100mMillis = 61400L,
                                            pb100mLagenMillis = 68200L,
                                            pb200mLagenMillis = 149300L,
                                            pb200mLagenFlySplitMillis = 31200L,
                                            pb200mLagenBackSplitMillis = 38500L,
                                            pb200mLagenBreastSplitMillis = 43100L,
                                            pb200mLagenFreeSplitMillis = 31500L,
                                            colorHex = 0xFFFF5252
                                        ),
                                        SwimmerEntity(
                                            name = "Felix Braun",
                                            ageGroup = "AK 17",
                                            favoriteStroke = "Rücken",
                                            pb25mMillis = 12300L,
                                            pb50mMillis = 26500L,
                                            pb100mMillis = 57800L,
                                            pb100mLagenMillis = 64800L,
                                            pb200mLagenMillis = 142100L,
                                            pb200mLagenFlySplitMillis = 30400L,
                                            pb200mLagenBackSplitMillis = 34200L,
                                            pb200mLagenBreastSplitMillis = 41500L,
                                            pb200mLagenFreeSplitMillis = 29800L,
                                            colorHex = 0xFFFFEB3B
                                        ),
                                        SwimmerEntity(
                                            name = "Emma Hoffmann",
                                            ageGroup = "AK 16",
                                            favoriteStroke = "Brust",
                                            pb25mMillis = 14800L,
                                            pb50mMillis = 32100L,
                                            pb100mMillis = 70500L,
                                            pb100mLagenMillis = 72100L,
                                            pb200mLagenMillis = 158400L,
                                            colorHex = 0xFF69F0AE
                                        ),
                                        SwimmerEntity(
                                            name = "Max Mayer",
                                            ageGroup = "Offen",
                                            favoriteStroke = "Freistil",
                                            pb25mMillis = 11200L,
                                            pb50mMillis = 23900L,
                                            pb100mMillis = 52300L,
                                            colorHex = 0xFFE040FB
                                        ),
                                        SwimmerEntity(
                                            name = "Lea Fischer",
                                            ageGroup = "AK 14",
                                            favoriteStroke = "Lagen",
                                            pb25mMillis = 13100L,
                                            pb50mMillis = 28400L,
                                            pb100mMillis = 63200L,
                                            pb100mLagenMillis = 69400L,
                                            pb200mLagenMillis = 152000L,
                                            colorHex = 0xFFFF9100
                                        ),
                                        SwimmerEntity(
                                            name = "Jonas Wagner",
                                            ageGroup = "AK 18",
                                            favoriteStroke = "Freistil",
                                            pb25mMillis = 11500L,
                                            pb50mMillis = 24200L,
                                            pb100mMillis = 53100L,
                                            colorHex = 0xFF40C4FF
                                        ),
                                        SwimmerEntity(
                                            name = "Mia Becker",
                                            ageGroup = "AK 15",
                                            favoriteStroke = "Brust",
                                            pb25mMillis = 15200L,
                                            pb50mMillis = 33400L,
                                            pb100mMillis = 72900L,
                                            colorHex = 0xFFB388FF
                                        )
                                    )
                                )
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
