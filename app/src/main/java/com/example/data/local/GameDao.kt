package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM player_save WHERE id = 1")
    fun observePlayerSave(): Flow<PlayerSaveEntity?>

    @Query("SELECT * FROM player_save WHERE id = 1")
    suspend fun getPlayerSaveOnce(): PlayerSaveEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlayerSave(save: PlayerSaveEntity)

    @Query("SELECT * FROM mission_progress")
    fun observeMissions(): Flow<List<MissionProgressEntity>>

    @Query("SELECT * FROM mission_progress")
    suspend fun getMissionsOnce(): List<MissionProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMission(mission: MissionProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMissions(missions: List<MissionProgressEntity>)
}
