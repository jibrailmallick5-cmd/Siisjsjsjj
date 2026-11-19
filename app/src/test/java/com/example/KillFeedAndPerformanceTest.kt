package com.example

import com.example.audio.NoOpSoundManager
import com.example.data.local.GameDao
import com.example.data.local.MissionProgressEntity
import com.example.data.local.PlayerSaveEntity
import com.example.data.repository.GameRepository
import com.example.game.GameMode
import com.example.game.GameViewModel
import com.example.game.KillFeedTeam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.atan2
import kotlin.math.hypot

@OptIn(ExperimentalCoroutinesApi::class)
class KillFeedAndPerformanceTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun rotateCameraUpdatesRenderStateImmediatelyWithoutUiRecompositionChurn() {
        val vm = GameViewModel(GameRepository(FakeGameDao()), NoOpSoundManager)
        vm.startMatch(GameMode.CLASH_SQUAD)

        val initialUiYaw = vm.uiState.value.cameraYaw
        vm.rotateCamera(deltaYaw = 45f, deltaPitch = 0f)

        val renderYaw = vm.currentRenderState().cameraYaw
        assertNotEquals("Render camera yaw should update immediately", initialUiYaw, renderYaw)
        assertEquals(
            "HUD StateFlow should not emit synchronously during camera drag",
            initialUiYaw,
            vm.uiState.value.cameraYaw,
            0.0001f
        )

        // Advancing simulation flushes HUD state smoothly
        repeat(4) { vm.gameLoop.onUpdate(0.016f) }
        assertEquals(renderYaw, vm.uiState.value.cameraYaw, 0.0001f)

        vm.gameLoop.stop()
    }

    @Test
    fun eliminatingEnemyInClashSquadAddsEntryToKillFeed() {
        val vm = GameViewModel(GameRepository(FakeGameDao()), NoOpSoundManager)
        vm.startMatch(GameMode.CLASH_SQUAD)
        assertTrue(vm.uiState.value.killFeed.isEmpty())

        // Fast-forward 15s spawn barrier countdown
        repeat(155) {
            vm.gameLoop.onUpdate(0.1f)
        }
        assertEquals(0f, vm.currentRenderState().roundCountdown, 0.001f)

        // Sprint north-west into open firing lane & verify live 4v4 fight or player AWM takedown fills Kill Feed
        vm.setJoystickVector(-0.35f, -0.94f)
        repeat(90) {
            vm.gameLoop.onUpdate(0.05f)
        }
        vm.setJoystickVector(0f, 0f)

        // Equip AWM, scope in for minimal shoulder offset & tight spread, and aim at nearest alive enemy
        vm.setWeaponIndex(3)
        if (!vm.currentRenderState().isScoped) vm.toggleScope()

        repeat(8) {
            val s = vm.currentRenderState()
            val target = s.enemies.filter { it.isAlive }
                .minByOrNull { hypot(it.x - s.playerX, it.y - s.playerY) }
            if (target != null && s.isPlayerAlive) {
                val desiredYaw = atan2(target.y - s.playerY, target.x - s.playerX)
                var yawErr = desiredYaw - s.cameraYaw
                while (yawErr > Math.PI) yawErr -= (2.0 * Math.PI).toFloat()
                while (yawErr < -Math.PI) yawErr += (2.0 * Math.PI).toFloat()
                val sens = s.scopeSensitivity
                val stepRate = 0.0042f * sens
                vm.rotateCamera(deltaYaw = yawErr / (stepRate * 0.78f), deltaPitch = 0f)

                val dist = hypot(target.x - s.playerX, target.y - s.playerY).coerceAtLeast(1f)
                val targetPitch = atan2((target.z + 24f) - (s.playerZ + 44f), dist)
                val pitchErr = targetPitch - s.cameraPitch
                val pitchRate = 0.0018f * sens * 0.72f
                vm.rotateCamera(deltaYaw = 0f, deltaPitch = -pitchErr / pitchRate)

                vm.fireShot()
                repeat(30) { vm.gameLoop.onUpdate(0.05f) }
            }
        }

        // Also advance simulation slightly if needed so squadmates/zone finish a combat exchange
        if (vm.uiState.value.killFeed.isEmpty()) {
            repeat(180) { vm.gameLoop.onUpdate(0.05f) }
        }

        val feed = vm.uiState.value.killFeed
        assertTrue("Expected kill feed entries during 4v4 clash squad combat", feed.isNotEmpty())
        val first = feed.first()
        assertTrue(first.killerName.isNotBlank())
        assertTrue(first.victimName.isNotBlank())
        assertTrue(first.weaponName.isNotBlank())
        assertTrue(first.killerTeam == KillFeedTeam.ALLY || first.killerTeam == KillFeedTeam.ENEMY || first.killerTeam == KillFeedTeam.ZONE)

        vm.gameLoop.stop()
    }

    private class FakeGameDao : GameDao {
        private val saveFlow = MutableStateFlow<PlayerSaveEntity?>(PlayerSaveEntity())
        private val missionsFlow = MutableStateFlow<List<MissionProgressEntity>>(emptyList())

        override fun observePlayerSave(): Flow<PlayerSaveEntity?> = saveFlow
        override suspend fun getPlayerSaveOnce(): PlayerSaveEntity? = saveFlow.value
        override suspend fun upsertPlayerSave(save: PlayerSaveEntity) {
            saveFlow.value = save
        }

        override fun observeMissions(): Flow<List<MissionProgressEntity>> = missionsFlow
        override suspend fun getMissionsOnce(): List<MissionProgressEntity> = missionsFlow.value
        override suspend fun upsertMission(mission: MissionProgressEntity) {
            missionsFlow.value = missionsFlow.value.filterNot { it.missionId == mission.missionId } + mission
        }

        override suspend fun upsertMissions(missions: List<MissionProgressEntity>) {
            missionsFlow.value = missions
        }
    }
}
