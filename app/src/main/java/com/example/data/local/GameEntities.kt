package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_save")
data class PlayerSaveEntity(
    @PrimaryKey val id: Int = 1,
    val playerName: String = "Ghost-01",
    val level: Int = 1,
    val xp: Int = 0,
    val gold: Int = 350,
    val aetherGems: Int = 25,
    val hp: Float = 200f,
    val maxHp: Float = 200f,
    val armor: Float = 100f,
    val maxArmor: Float = 100f,
    val medkits: Int = 3,
    val posX: Float = 1600f,
    val posY: Float = 2200f,
    val cloakIndex: Int = 0,
    val hatIndex: Int = 0,
    val weaponIndex: Int = 0,
    val auraIndex: Int = 0,
    val vitalityLevel: Int = 1,
    val mightLevel: Int = 1,
    val agilityLevel: Int = 1,
    val energyLevel: Int = 1,
    val graphicsQuality: Int = 2,
    val timeMode: Int = 0,
    val language: String = "bn",
    val generalSensitivity: Float = 1.0f,
    val scopeSensitivity: Float = 0.75f,
    val autoLootEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val soundVolume: Float = 0.85f,
    val trackedMissionId: String = "m1_patrol",
    val discoveredWaypointsCsv: String = "wp_clocktower",
    val openedChestsCsv: String = "",
    val activatedRunesCsv: String = "",
    val collectedShardsCsv: String = "",
    val talkedNpcsCsv: String = "",
    val enemiesDefeated: Int = 0,
    val headshots: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "mission_progress")
data class MissionProgressEntity(
    @PrimaryKey val missionId: String,
    val progress: Int = 0,
    val completed: Boolean = false,
    val rewardClaimed: Boolean = false
)
