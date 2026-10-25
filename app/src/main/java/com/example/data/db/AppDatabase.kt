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

@Database(
    entities = [
        SwimmerEntity::class,
        SessionEntity::class,
        LapRecordEntity::class,
    ],
    version = 6,
    exportSchema = false,
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
                    "swim_timer_database",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
