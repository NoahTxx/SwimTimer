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

    @Query("SELECT * FROM swimmers")
    suspend fun getAllSwimmersSync(): List<SwimmerEntity>

    @Query("UPDATE swimmers SET groupName = :newName WHERE LOWER(TRIM(groupName)) = LOWER(TRIM(:oldName))")
    suspend fun renameGroupForAllSwimmers(oldName: String, newName: String)

    @Query("UPDATE swimmers SET groupName = NULL WHERE LOWER(TRIM(groupName)) = LOWER(TRIM(:groupName))")
    suspend fun clearGroupForAllSwimmers(groupName: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSwimmer(swimmer: SwimmerEntity): Long

    @Update
    suspend fun updateSwimmer(swimmer: SwimmerEntity)

    @Delete
    suspend fun deleteSwimmer(swimmer: SwimmerEntity)

    @Query("SELECT COUNT(*) FROM swimmers")
    suspend fun getSwimmerCount(): Int
}
