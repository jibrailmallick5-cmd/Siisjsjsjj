package com.example.data.repository

import com.example.data.local.GameDao
import com.example.data.local.MissionProgressEntity
import com.example.data.local.PlayerSaveEntity
import kotlinx.coroutines.flow.Flow

class GameRepository(private val gameDao: GameDao) {
    val playerSaveFlow: Flow<PlayerSaveEntity?> = gameDao.observePlayerSave()
    val missionsFlow: Flow<List<MissionProgressEntity>> = gameDao.observeMissions()

    suspend fun ensureInitialized(): Pair<PlayerSaveEntity, List<MissionProgressEntity>> {
        val existingSave = gameDao.getPlayerSaveOnce()
        val save = existingSave ?: PlayerSaveEntity().also {
            gameDao.upsertPlayerSave(it)
        }
        val existingMissions = gameDao.getMissionsOnce()
        val missions = if (existingMissions.isEmpty()) {
            defaultMissionProgress().also { gameDao.upsertMissions(it) }
        } else {
            existingMissions
        }
        return save to missions
    }

    suspend fun savePlayer(save: PlayerSaveEntity) {
        gameDao.upsertPlayerSave(save.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun saveAllMissions(missions: List<MissionProgressEntity>) {
        gameDao.upsertMissions(missions)
    }

    suspend fun resetAdventure(language: String): Pair<PlayerSaveEntity, List<MissionProgressEntity>> {
        val freshSave = PlayerSaveEntity(language = language)
        val freshMissions = defaultMissionProgress()
        gameDao.upsertPlayerSave(freshSave)
        gameDao.upsertMissions(freshMissions)
        return freshSave to freshMissions
    }

    private fun defaultMissionProgress(): List<MissionProgressEntity> = listOf(
        MissionProgressEntity("m1_patrol", progress = 0, completed = false, rewardClaimed = false),
        MissionProgressEntity("m2_airdrops", progress = 0, completed = false, rewardClaimed = false),
        MissionProgressEntity("m3_intel", progress = 0, completed = false, rewardClaimed = false),
        MissionProgressEntity("m4_commander", progress = 0, completed = false, rewardClaimed = false),
        MissionProgressEntity("m5_recon", progress = 1, completed = false, rewardClaimed = false)
    )
}
