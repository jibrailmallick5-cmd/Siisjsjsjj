package com.example

import com.example.audio.GameSoundManager
import com.example.audio.GunSoundProfile
import com.example.audio.SoundSynthesizer
import com.example.data.local.GameDao
import com.example.data.local.MissionProgressEntity
import com.example.data.local.PlayerSaveEntity
import com.example.data.repository.GameRepository
import com.example.game.GameMode
import com.example.game.GameViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

@OptIn(ExperimentalCoroutinesApi::class)
class SoundManagerTest {

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
    fun synthesizerGeneratesValidNonSilentWavPayloads() {
        val clips = listOf(
            SoundSynthesizer.synthesizeGunshot(GunSoundProfile.M4A1),
            SoundSynthesizer.synthesizeGunshot(GunSoundProfile.MP40),
            SoundSynthesizer.synthesizeGunshot(GunSoundProfile.AK47),
            SoundSynthesizer.synthesizeGunshot(GunSoundProfile.AWM),
            SoundSynthesizer.synthesizeFootstep(isSprint = false),
            SoundSynthesizer.synthesizeFootstep(isSprint = true),
            SoundSynthesizer.synthesizeJumpLand(),
            SoundSynthesizer.synthesizeZoneAlert(isInitial = true),
            SoundSynthesizer.synthesizeZoneAlert(isInitial = false),
            SoundSynthesizer.synthesizeHitmarker(isHeadshot = false),
            SoundSynthesizer.synthesizeHitmarker(isHeadshot = true),
            SoundSynthesizer.synthesizeReload()
        )

        for (wav in clips) {
            assertTrue(wav.size > 44)
            assertEquals("RIFF", String(wav.sliceArray(0..3)))
            assertEquals("WAVE", String(wav.sliceArray(8..11)))
            assertEquals("fmt ", String(wav.sliceArray(12..15)))
            assertEquals("data", String(wav.sliceArray(36..39)))

            var peak = 0
            for (i in 44 until wav.size - 1 step 2) {
                val sample = ((wav[i + 1].toInt() shl 8) or (wav[i].toInt() and 0xFF)).toShort().toInt()
                peak = maxOf(peak, abs(sample))
            }
            assertTrue("Expected audible waveform peak, got $peak", peak > 500)
        }
    }

    @Test
    fun viewModelTriggersWeaponFireFootstepsAndZoneDamageAlerts() {
        val fakeSound = RecordingSoundManager()
        val vm = GameViewModel(GameRepository(FakeGameDao()), fakeSound)

        assertTrue(vm.uiState.value.isLoaded)
        assertTrue(fakeSound.lastSoundEnabled == true)

        vm.startMatch(GameMode.OPEN_WORLD)

        // 1. Weapon fire triggers sound matching equipped weapon index
        vm.setWeaponIndex(2) // AK47
        vm.fireShot()
        assertEquals(listOf(2), fakeSound.firedWeapons)

        // 2. Cadence-synced footsteps play while moving on the ground
        vm.setJoystickVector(0f, -1f)
        repeat(25) {
            vm.gameLoop.onUpdate(0.05f)
        }
        assertTrue("Expected footstep sounds while running", fakeSound.footsteps.isNotEmpty())
        vm.setJoystickVector(0f, 0f)

        // 3. Stepping outside safe zone triggers initial warning + periodic damage alert
        vm.setJoystickVector(-0.75f, 0.75f)
        repeat(180) {
            vm.gameLoop.onUpdate(0.05f)
        }
        assertTrue("Expected player to cross outside safe zone", vm.uiState.value.isOutsideZone)
        assertTrue("Expected initial zone warning", fakeSound.zoneAlerts.contains(true))
        assertTrue("Expected periodic zone damage tick alert", fakeSound.zoneAlerts.contains(false))

        // 4. Audio settings propagate to SoundManager and state
        vm.setSoundVolume(0.42f)
        assertEquals(0.42f, vm.uiState.value.soundVolume, 0.001f)
        assertEquals(0.42f, fakeSound.lastMasterVolume ?: -1f, 0.001f)

        vm.setSoundEnabled(false)
        assertEquals(false, vm.uiState.value.soundEnabled)
        assertEquals(false, fakeSound.lastSoundEnabled)

        vm.gameLoop.stop()
    }

    private class RecordingSoundManager : GameSoundManager {
        val firedWeapons = mutableListOf<Int>()
        val footsteps = mutableListOf<Boolean>()
        val zoneAlerts = mutableListOf<Boolean>()
        var jumpLands = 0
        var reloads = 0
        var lastSoundEnabled: Boolean? = null
        var lastMasterVolume: Float? = null

        override fun playWeaponFire(weaponIndex: Int) {
            firedWeapons += weaponIndex
        }

        override fun playFootstep(isSprinting: Boolean) {
            footsteps += isSprinting
        }

        override fun playJumpLand() {
            jumpLands++
        }

        override fun playZoneDamageAlert(isInitialWarning: Boolean) {
            zoneAlerts += isInitialWarning
        }

        override fun playHitmarker(isHeadshot: Boolean) = Unit

        override fun playReload() {
            reloads++
        }

        override fun setSoundEnabled(enabled: Boolean) {
            lastSoundEnabled = enabled
        }

        override fun setMasterVolume(volume: Float) {
            lastMasterVolume = volume
        }

        override fun onPause() = Unit
        override fun onResume() = Unit
        override fun release() = Unit
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
