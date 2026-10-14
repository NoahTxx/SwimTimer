package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SwimmerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SwimmerDao {
    @Query("SELECT * FROM swimmers ORDER BY name ASC")
    fun getAllSwimmers(): Flow<List<SwimmerEntity>>

    @Query("SELECT * FROM swimmers WHERE id = :id")
    fun getSwimmerById(id: Long): Flow<SwimmerEntity?>

    @Query("SELECT * FROM swimmers WHERE id = :id")
    suspend fun getSwimmerByIdSync(id: Long): SwimmerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSwimmer(swimmer: SwimmerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSwimmers(swimmers: List<SwimmerEntity>)

    @Update
    suspend fun updateSwimmer(swimmer: SwimmerEntity)

    @Delete
    suspend fun deleteSwimmer(swimmer: SwimmerEntity)

    @Query("DELETE FROM swimmers WHERE id = :id")
    suspend fun deleteSwimmerById(id: Long)

    @Query("SELECT COUNT(*) FROM swimmers")
    suspend fun getSwimmerCount(): Int
}
