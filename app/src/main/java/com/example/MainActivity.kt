package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.SoundManager
import com.example.data.local.AppDatabase
import com.example.data.repository.GameRepository
import com.example.game.ActiveOverlay
import com.example.game.AppLanguage
import com.example.game.GameViewModel
import com.example.ui.canvas.WorldCanvas
import com.example.ui.components.GameHud
import com.example.ui.screens.CustomizationScreen
import com.example.ui.screens.MissionsSheet
import com.example.ui.screens.SettingsOverlay
import com.example.ui.screens.TitleOverlay
import com.example.ui.screens.WorldMapOverlay
import com.example.ui.theme.AetherCyan
import com.example.ui.theme.AetherisTheme
import com.example.ui.theme.ObsidianBg

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getInstance(applicationContext)
        val repository = GameRepository(db.gameDao())

        setContent {
            AetherisTheme {
                val gameViewModel: GameViewModel = viewModel(
                    factory = GameViewModel.factory(repository) {
                        SoundManager(applicationContext)
                    }
                )
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner, gameViewModel) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_PAUSE -> gameViewModel.onAppPause()
                            Lifecycle.Event.ON_RESUME -> gameViewModel.onAppResume()
                            else -> Unit
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }
                AetherisApp(viewModel = gameViewModel)
            }
        }
    }
}

@Composable
fun AetherisApp(viewModel: GameViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ObsidianBg
    ) {
        if (!state.isLoaded) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AetherCyan)
            }
            return@Surface
        }

        val waypointCoords = viewModel.activeWaypointCoordinates(state)
        val renderStateProvider = remember(viewModel) { viewModel::currentRenderState }
        val renderWaypointProvider = remember(viewModel) { viewModel::activeWaypointCoordinates }
        val attachDrawHandler = remember(viewModel) { viewModel::attachDrawHandler }

        if (state.activeDialogue != null && state.activeOverlay == ActiveOverlay.NONE) {
            BackHandler { viewModel.dismissDialogue() }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Real-Time HD 3D Battleground Engine (zero Compose recomposition per frame)
            WorldCanvas(
                stateProvider = renderStateProvider,
                waypointProvider = renderWaypointProvider,
                gameLoop = viewModel.gameLoop,
                onAttachDrawHandler = attachDrawHandler,
                modifier = Modifier.fillMaxSize()
            )

            // 2. Interactive Dual-Hand Tactical HUD (when playing)
            if (!state.showTitleOverlay && state.activeOverlay == ActiveOverlay.NONE) {
                GameHud(
                    state = state,
                    waypointCoords = waypointCoords,
                    onJoystickMove = viewModel::setJoystickVector,
                    onRotateCamera = viewModel::rotateCamera,
                    onSetFiring = viewModel::setFiring,
                    onJump = viewModel::performJump,
                    onToggleSprint = viewModel::toggleSprintLock,
                    onToggleScope = viewModel::toggleScope,
                    onReload = viewModel::startReload,
                    onCycleWeapon = viewModel::cycleWeapon,
                    onUseMedkit = viewModel::useMedkit,
                    onPickupLoot = viewModel::pickupNearbyLoot,
                    onInteract = viewModel::interactWithNearby,
                    onOpenOverlay = viewModel::openOverlay,
                    onPlayAgain = { viewModel.startMatch(state.gameMode) },
                    onReturnToLobby = viewModel::returnToLobby,
                    onDismissDialogue = viewModel::dismissDialogue
                )
            }

            // 3. Free Fire-style Tactical Lobby Overlay
            AnimatedVisibility(
                visible = state.showTitleOverlay && state.activeOverlay == ActiveOverlay.NONE,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                TitleOverlay(
                    state = state,
                    onSelectMode = viewModel::selectGameMode,
                    onStartGame = { viewModel.startMatch(state.gameMode) },
                    onOpenCustomize = { viewModel.openOverlay(ActiveOverlay.CUSTOMIZE) },
                    onOpenSettings = { viewModel.openOverlay(ActiveOverlay.SETTINGS) },
                    onToggleLanguage = {
                        val next = if (state.language == AppLanguage.BN) AppLanguage.EN else AppLanguage.BN
                        viewModel.setLanguage(next)
                    }
                )
            }

            // 4. Missions Sheet
            AnimatedVisibility(
                visible = state.activeOverlay == ActiveOverlay.MISSIONS,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                MissionsSheet(
                    state = state,
                    onTrackMission = { id ->
                        viewModel.selectTrackedMission(id)
                        viewModel.closeOverlay()
                    },
                    onClaimReward = viewModel::claimMissionReward,
                    onClose = viewModel::closeOverlay
                )
            }

            // 5. Customization & Armory Studio
            AnimatedVisibility(
                visible = state.activeOverlay == ActiveOverlay.CUSTOMIZE,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                CustomizationScreen(
                    state = state,
                    upgradeCostFn = viewModel::upgradeGoldCost,
                    onSelectCloak = viewModel::setCloakIndex,
                    onSelectHat = viewModel::setHatIndex,
                    onSelectWeapon = viewModel::setWeaponIndex,
                    onSelectAura = viewModel::setAuraIndex,
                    onUpgradeStat = viewModel::upgradeAttribute,
                    onSetGraphicsQuality = viewModel::setGraphicsQuality,
                    onSetTimeMode = viewModel::setTimeMode,
                    onSetLanguage = viewModel::setLanguage,
                    onResetAdventure = viewModel::resetAdventure,
                    onClose = viewModel::closeOverlay
                )
            }

            // 6. Full Tactical World Map & Safe Zone Overview
            AnimatedVisibility(
                visible = state.activeOverlay == ActiveOverlay.WORLD_MAP,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                WorldMapOverlay(
                    state = state,
                    waypointCoords = waypointCoords,
                    onFastTravel = viewModel::fastTravelToWaypoint,
                    onClose = viewModel::closeOverlay
                )
            }

            // 7. Sensitivity & Game Settings Overlay
            AnimatedVisibility(
                visible = state.activeOverlay == ActiveOverlay.SETTINGS,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                SettingsOverlay(
                    state = state,
                    inMatch = !state.showTitleOverlay,
                    onSetGeneralSensitivity = viewModel::setGeneralSensitivity,
                    onSetScopeSensitivity = viewModel::setScopeSensitivity,
                    onSetAutoLoot = viewModel::setAutoLootEnabled,
                    onSetSoundEnabled = viewModel::setSoundEnabled,
                    onSetSoundVolume = viewModel::setSoundVolume,
                    onSetGraphicsQuality = viewModel::setGraphicsQuality,
                    onSetTimeMode = viewModel::setTimeMode,
                    onSetLanguage = viewModel::setLanguage,
                    onReturnToLobby = viewModel::returnToLobby,
                    onClose = viewModel::closeOverlay
                )
            }
        }
    }
}
