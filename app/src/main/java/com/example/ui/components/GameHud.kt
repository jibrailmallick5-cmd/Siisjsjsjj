package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.ActiveDialogue
import com.example.game.ActiveOverlay
import com.example.game.AppLanguage
import com.example.game.CustomizationCatalog
import com.example.game.GameMode
import com.example.game.GameStrings
import com.example.game.GameUiState
import com.example.game.InteractableKind
import com.example.game.KillFeedEntry
import com.example.game.KillFeedTeam
import com.example.game.WORLD_SIZE
import com.example.ui.theme.AetherCyan
import com.example.ui.theme.ArcaneViolet
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.EmeraldLife
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SolarGold
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun GameHud(
    state: GameUiState,
    waypointCoords: Pair<Float, Float>?,
    onJoystickMove: (Float, Float) -> Unit,
    onRotateCamera: (Float, Float) -> Unit,
    onSetFiring: (Boolean) -> Unit,
    onJump: () -> Unit,
    onToggleSprint: () -> Unit,
    onToggleScope: () -> Unit,
    onReload: () -> Unit,
    onCycleWeapon: () -> Unit,
    onUseMedkit: () -> Unit,
    onPickupLoot: () -> Unit,
    onInteract: () -> Unit,
    onOpenOverlay: (ActiveOverlay) -> Unit,
    onPlayAgain: () -> Unit,
    onReturnToLobby: () -> Unit,
    onDismissDialogue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = state.language
    val backpack = CustomizationCatalog.backpacks[state.auraIndex.coerceIn(0, CustomizationCatalog.backpacks.lastIndex)]
    val weapon = state.activeWeapon
    val density = LocalDensity.current.density.coerceAtLeast(1f)

    Box(modifier = modifier.fillMaxSize()) {
        // Right-side 360° Touch-Drag Camera Aim Zone (DP-normalized & Sensitivity-scaled)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.64f)
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
                .testTag("hud_camera_drag_zone")
                .pointerInput(density) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onRotateCamera(dragAmount.x / density, dragAmount.y / density)
                    }
                }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(12.dp)
        ) {
            // TOP ROW: Left (HP 200, Armor, Kills) | Right (Minimap with Safe Zone + Quick Menus & Settings)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Top-Left Status Card + Scrolling Kill Feed
                Column(
                    modifier = Modifier.widthIn(max = 252.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = ObsidianSurface.copy(alpha = 0.88f),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, ObsidianOutline),
                        modifier = Modifier.widthIn(max = 205.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = GameStrings.levelLabel(state.level, lang),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = AetherCyan
                                )
                                Text(
                                    text = "K:${state.enemiesDefeated}  HS:${state.headshots}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SolarGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(5.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Shield, contentDescription = null, tint = AetherCyan, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                LinearProgressIndicator(
                                    progress = { (state.armor / state.maxArmor.coerceAtLeast(1f)).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = AetherCyan,
                                    trackColor = ObsidianBg
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { (state.hp / state.maxHp.coerceAtLeast(1f)).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (state.hp / state.maxHp < 0.3f) CrimsonDanger else EmeraldLife,
                                trackColor = ObsidianBg
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "HP ${state.hp.toInt()}/${state.maxHp.toInt()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = GameStrings.biomeName(state.currentBiome, lang),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (state.gameMode == GameMode.CLASH_SQUAD || state.killFeed.isNotEmpty()) {
                        ScrollingKillFeed(
                            killFeed = state.killFeed,
                            lang = lang
                        )
                    }

                    if (state.gameMode == GameMode.OPEN_WORLD) {
                        state.activeMission?.let { mission ->
                            Surface(
                                color = ObsidianSurface.copy(alpha = 0.85f),
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(1.dp, SolarGold.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .widthIn(max = 205.dp)
                                    .clickable { onOpenOverlay(ActiveOverlay.MISSIONS) }
                                    .testTag("hud_active_mission_card")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.MilitaryTech, contentDescription = null, tint = SolarGold, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (lang == AppLanguage.BN) mission.titleBn else mission.titleEn,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${mission.progress}/${mission.targetCount}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (mission.completed) EmeraldLife else AetherCyan
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Top-Right Minimap + Menu Buttons (Settings, Armory, Map, Missions)
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniMapWidget(
                        state = state,
                        waypointCoords = waypointCoords,
                        onClick = { onOpenOverlay(ActiveOverlay.WORLD_MAP) }
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        HudCircleIconButton(
                            icon = Icons.Rounded.Settings,
                            tint = Color.White,
                            contentDescription = stringResource(R.string.cd_settings_button),
                            testTag = "hud_settings_button",
                            onClick = { onOpenOverlay(ActiveOverlay.SETTINGS) }
                        )
                        HudCircleIconButton(
                            icon = Icons.Rounded.Palette,
                            tint = AetherCyan,
                            contentDescription = stringResource(R.string.cd_customize_button),
                            testTag = "hud_customize_button",
                            onClick = { onOpenOverlay(ActiveOverlay.CUSTOMIZE) }
                        )
                        HudCircleIconButton(
                            icon = Icons.Rounded.Map,
                            tint = ArcaneViolet,
                            contentDescription = stringResource(R.string.cd_map_button),
                            testTag = "hud_map_button",
                            onClick = { onOpenOverlay(ActiveOverlay.WORLD_MAP) }
                        )
                        if (state.gameMode == GameMode.OPEN_WORLD) {
                            HudCircleIconButton(
                                icon = Icons.Rounded.MilitaryTech,
                                tint = SolarGold,
                                contentDescription = stringResource(R.string.cd_missions_button),
                                testTag = "hud_missions_button",
                                onClick = { onOpenOverlay(ActiveOverlay.MISSIONS) }
                            )
                        }
                    }
                }
            }

            // TOP-CENTER: CLASH SQUAD 4v4 SCOREBOARD OR COMPASS + 15s BARRIER TIMER + ZONE WARNING
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 2.dp)
            ) {
                if (state.gameMode == GameMode.CLASH_SQUAD) {
                    ClashSquadScoreboard(state = state)
                } else {
                    TacticalCompassPill(cameraYaw = state.cameraYaw, gold = state.gold)
                }

                // 15-Second Spawn Barrier Cage Countdown
                AnimatedVisibility(visible = state.gameMode == GameMode.CLASH_SQUAD && state.roundCountdown > 0f) {
                    val sec = ceil(state.roundCountdown).toInt().coerceAtLeast(1)
                    Surface(
                        color = ObsidianSurface.copy(alpha = 0.94f),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.5.dp, SolarGold),
                        modifier = Modifier.testTag("hud_barrier_countdown")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Icon(Icons.Rounded.Timer, contentDescription = null, tint = SolarGold, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = GameStrings.barrierCountdown(sec, lang),
                                style = MaterialTheme.typography.labelLarge,
                                color = SolarGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Outside Safe Zone Warning Pill
                AnimatedVisibility(visible = state.isOutsideZone) {
                    Surface(
                        color = CrimsonDanger.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, Color.White),
                        modifier = Modifier.testTag("hud_zone_warning")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = GameStrings.outsideZoneWarning(lang),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Reloading or Status Banner
                AnimatedVisibility(visible = state.reloadTimer > 0f || state.statusBanner != null) {
                    val text = when {
                        state.reloadTimer > 0f -> GameStrings.reloading(lang)
                        state.statusBanner != null -> state.statusBanner
                        else -> ""
                    }
                    Surface(
                        color = ObsidianSurface.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, if (state.reloadTimer > 0f) SolarGold else AetherCyan)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (state.reloadTimer > 0f) Icons.Rounded.Refresh else Icons.Rounded.Star,
                                contentDescription = null,
                                tint = SolarGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            // CLASH SQUAD ROUND END / MATCH VICTORY BANNER
            AnimatedVisibility(
                visible = state.roundEndBanner != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Surface(
                    color = ObsidianSurface.copy(alpha = 0.96f),
                    shape = MaterialTheme.shapes.extraLarge,
                    border = BorderStroke(2.dp, if (state.matchFinishedWinner == false) CrimsonDanger else SolarGold),
                    modifier = Modifier
                        .widthIn(max = 360.dp)
                        .padding(16.dp)
                        .testTag("hud_round_result_card")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = state.roundEndBanner.orEmpty(),
                            style = MaterialTheme.typography.titleLarge,
                            color = if (state.matchFinishedWinner == false) CrimsonDanger else SolarGold,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "${state.allyRoundWins}  -  ${state.enemyRoundWins}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = AetherCyan,
                            fontWeight = FontWeight.Bold
                        )
                        if (state.matchFinishedWinner != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onReturnToLobby,
                                    modifier = Modifier.weight(1f).height(48.dp).testTag("match_end_lobby_button")
                                ) {
                                    Text(GameStrings.backToLobby(lang), maxLines = 1)
                                }
                                Button(
                                    onClick = onPlayAgain,
                                    colors = ButtonDefaults.buttonColors(containerColor = SolarGold, contentColor = ObsidianBg),
                                    modifier = Modifier.weight(1f).height(48.dp).testTag("match_end_play_again_button")
                                ) {
                                    Text(if (lang == AppLanguage.BN) "আবার খেলুন" else "Play Again", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // NPC DIALOGUE BANNER
            AnimatedVisibility(
                visible = state.activeDialogue != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 186.dp)
            ) {
                state.activeDialogue?.let { dlg ->
                    DialogueCard(dialogue = dlg, isBengali = lang == AppLanguage.BN, onDismiss = onDismissDialogue)
                }
            }

            // BOTTOM-LEFT CONTROLS: Sprint Lock + Medkit + Virtual Movement Joystick
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 4.dp, bottom = 4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onToggleSprint,
                        shape = RoundedCornerShape(22.dp),
                        color = if (state.sprintLock || state.isSprinting) AetherCyan.copy(alpha = 0.92f) else ObsidianSurface.copy(alpha = 0.88f),
                        border = BorderStroke(1.5.dp, if (state.sprintLock) Color.White else AetherCyan.copy(alpha = 0.7f)),
                        modifier = Modifier.height(48.dp).testTag("hud_run_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.DirectionsRun,
                                contentDescription = stringResource(R.string.cd_run_button),
                                tint = if (state.sprintLock || state.isSprinting) ObsidianBg else AetherCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = GameStrings.run(lang),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (state.sprintLock || state.isSprinting) ObsidianBg else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        onClick = onUseMedkit,
                        enabled = state.isPlayerAlive && state.medkits > 0 && state.hp < state.maxHp,
                        shape = RoundedCornerShape(22.dp),
                        color = ObsidianSurface.copy(alpha = 0.88f),
                        border = BorderStroke(1.5.dp, if (state.medkits > 0) EmeraldLife else ObsidianOutline),
                        modifier = Modifier.height(48.dp).testTag("hud_medkit_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocalHospital,
                                contentDescription = stringResource(R.string.cd_medkit_button),
                                tint = if (state.medkits > 0) EmeraldLife else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${GameStrings.medkit(lang)} ×${state.medkits}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (state.medkits > 0) Color.White else Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                VirtualJoystick(accentColor = backpack.coreColor, onMove = onJoystickMove)
            }

            // BOTTOM-RIGHT CONTROLS: Ground Gun Loot + Interact + Weapon Switch + Scope/Reload/Jump/FIRE
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 4.dp, bottom = 4.dp)
            ) {
                // Free Fire style Ground Gun & Gear Loot Button
                AnimatedVisibility(visible = state.nearbyLoot != null) {
                    state.nearbyLoot?.let { loot ->
                        Button(
                            onClick = onPickupLoot,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = loot.color,
                                contentColor = ObsidianBg
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("hud_loot_button")
                        ) {
                            Icon(Icons.Rounded.Inventory2, contentDescription = stringResource(R.string.cd_loot_button))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "${GameStrings.loot(lang)}: ${if (lang == AppLanguage.BN) loot.labelBn else loot.labelEn}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = state.nearbyInteractable != null) {
                    state.nearbyInteractable?.let { target ->
                        Button(
                            onClick = onInteract,
                            colors = ButtonDefaults.buttonColors(containerColor = SolarGold, contentColor = ObsidianBg),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(48.dp).testTag("hud_interact_button")
                        ) {
                            Icon(Icons.Rounded.TouchApp, contentDescription = stringResource(R.string.cd_interact_button))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (lang == AppLanguage.BN) target.labelBn else target.labelEn,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Equipped Weapon Switcher Pill
                Surface(
                    onClick = onCycleWeapon,
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianSurface.copy(alpha = 0.9f),
                    border = BorderStroke(1.5.dp, weapon.accentColor.copy(alpha = 0.75f)),
                    modifier = Modifier.height(48.dp).testTag("hud_weapon_switch_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SwapHoriz,
                            contentDescription = stringResource(R.string.cd_weapon_switch),
                            tint = weapon.accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (lang == AppLanguage.BN) weapon.nameBn else weapon.nameEn,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            maxLines = 1
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "${state.currentMagAmmo}/${state.currentReserveAmmo}",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (state.currentMagAmmo == 0) CrimsonDanger else SolarGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Combat Buttons: SCOPE + RELOAD + JUMP + FIRE
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    TacticalCircleButton(
                        icon = Icons.Rounded.CenterFocusStrong,
                        label = GameStrings.scope(lang),
                        isActive = state.isScoped,
                        accentColor = SolarGold,
                        sizeDp = 54,
                        contentDescription = stringResource(R.string.cd_scope_button),
                        testTag = "hud_scope_button",
                        onClick = onToggleScope
                    )
                    TacticalCircleButton(
                        icon = Icons.Rounded.Refresh,
                        label = GameStrings.reload(lang),
                        isActive = state.reloadTimer > 0f,
                        accentColor = AetherCyan,
                        sizeDp = 54,
                        contentDescription = stringResource(R.string.cd_reload_button),
                        testTag = "hud_reload_button",
                        onClick = onReload
                    )
                    TacticalCircleButton(
                        icon = Icons.Rounded.ArrowUpward,
                        label = GameStrings.jump(lang),
                        isActive = !state.isGrounded,
                        accentColor = EmeraldLife,
                        sizeDp = 60,
                        contentDescription = stringResource(R.string.cd_jump_button),
                        testTag = "hud_jump_button",
                        onClick = onJump
                    )
                    FireTriggerButton(
                        label = GameStrings.fire(lang),
                        isFiring = state.isFiring,
                        contentDescription = stringResource(R.string.cd_fire_button),
                        onSetFiring = onSetFiring
                    )
                }
            }
        }
    }
}

@Composable
private fun ClashSquadScoreboard(state: GameUiState) {
    Surface(
        color = ObsidianSurface.copy(alpha = 0.88f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.2.dp, ObsidianOutline),
        modifier = Modifier.testTag("hud_clash_scoreboard")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${state.allyRoundWins}",
                    style = MaterialTheme.typography.titleMedium,
                    color = AetherCyan,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "R${state.clashRound}",
                    style = MaterialTheme.typography.labelLarge,
                    color = SolarGold,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${state.enemyRoundWins}",
                    style = MaterialTheme.typography.titleMedium,
                    color = CrimsonDanger,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Text(
                text = "4v4  •  ${state.allyAliveCount} vs ${state.enemyAliveCount}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ScrollingKillFeed(
    killFeed: List<KillFeedEntry>,
    lang: AppLanguage,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    LaunchedEffect(killFeed.lastOrNull()?.id) {
        if (killFeed.isNotEmpty()) {
            listState.animateScrollToItem(killFeed.lastIndex)
        }
    }

    Surface(
        color = ObsidianSurface.copy(alpha = 0.80f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ObsidianOutline.copy(alpha = 0.85f)),
        modifier = modifier
            .widthIn(min = 188.dp, max = 252.dp)
            .testTag("hud_kill_feed")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GpsFixed,
                        contentDescription = null,
                        tint = SolarGold,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = GameStrings.killFeedTitle(lang),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = SolarGold,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                if (killFeed.isNotEmpty()) {
                    Text(
                        text = "${killFeed.size}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = AetherCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (killFeed.isEmpty()) {
                Text(
                    text = GameStrings.killFeedEmpty(lang),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(vertical = 1.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 96.dp)
                        .testTag("hud_kill_feed_list")
                ) {
                    items(items = killFeed, key = { it.id }) { entry ->
                        KillFeedItemRow(entry = entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun KillFeedItemRow(entry: KillFeedEntry) {
    val killerColor = teamColor(entry.killerTeam)
    val victimColor = teamColor(entry.victimTeam)
    val containerColor = if (entry.isPlayerInvolved) {
        SolarGold.copy(alpha = 0.14f)
    } else {
        ObsidianBg.copy(alpha = 0.62f)
    }
    val borderColor = if (entry.isPlayerInvolved) {
        SolarGold.copy(alpha = 0.58f)
    } else {
        ObsidianOutline.copy(alpha = 0.45f)
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.75.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("kill_feed_row_${entry.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = entry.killerName,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = killerColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Surface(
                color = ObsidianSurface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(0.6.dp, if (entry.isHeadshot) SolarGold else ObsidianOutline)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = entry.weaponName,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = if (entry.killerTeam == KillFeedTeam.ZONE) Color(0xFF38BDF8) else Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                    if (entry.isHeadshot) {
                        Icon(
                            imageVector = Icons.Rounded.CenterFocusStrong,
                            contentDescription = "Headshot",
                            tint = SolarGold,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
            Text(
                text = entry.victimName,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = victimColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
    }
}

private fun teamColor(team: KillFeedTeam): Color = when (team) {
    KillFeedTeam.ALLY -> AetherCyan
    KillFeedTeam.ENEMY -> CrimsonDanger
    KillFeedTeam.ZONE -> Color(0xFF38BDF8)
}

@Composable
private fun FireTriggerButton(
    label: String,
    isFiring: Boolean,
    contentDescription: String,
    onSetFiring: (Boolean) -> Unit
) {
    val bgBrush = Brush.radialGradient(
        colors = if (isFiring) listOf(Color(0xFFFF8A00), Color(0xFFDC2626)) else listOf(Color(0xFFEF4444), Color(0xFF991B1B))
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(84.dp)
            .clip(CircleShape)
            .background(bgBrush)
            .border(2.5.dp, if (isFiring) SolarGold else Color.White.copy(alpha = 0.85f), CircleShape)
            .testTag("hud_fire_button")
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onSetFiring(true)
                        try {
                            tryAwaitRelease()
                        } finally {
                            onSetFiring(false)
                        }
                    }
                )
            }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.GpsFixed, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(32.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun TacticalCircleButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    accentColor: Color,
    sizeDp: Int,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isActive) accentColor.copy(alpha = 0.92f) else ObsidianSurface.copy(alpha = 0.9f),
        border = BorderStroke(2.dp, if (isActive) Color.White else accentColor.copy(alpha = 0.85f)),
        modifier = Modifier.size(sizeDp.dp).testTag(testTag)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = contentDescription, tint = if (isActive) ObsidianBg else accentColor, modifier = Modifier.size(22.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = if (isActive) ObsidianBg else Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TacticalCompassPill(cameraYaw: Float, gold: Int, modifier: Modifier = Modifier) {
    val deg = (((cameraYaw * 180f / PI.toFloat()) + 90f) % 360f + 360f) % 360f
    val cardinal = when {
        deg < 22.5f || deg >= 337.5f -> "N"
        deg < 67.5f -> "NE"
        deg < 112.5f -> "E"
        deg < 157.5f -> "SE"
        deg < 202.5f -> "S"
        deg < 247.5f -> "SW"
        deg < 292.5f -> "W"
        else -> "NW"
    }
    Surface(
        color = ObsidianSurface.copy(alpha = 0.78f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ObsidianOutline),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "$cardinal ${deg.toInt()}°", style = MaterialTheme.typography.labelMedium, color = AetherCyan, fontWeight = FontWeight.Bold)
            Text(text = "${gold}G", style = MaterialTheme.typography.labelSmall, color = SolarGold, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HudCircleIconButton(
    icon: ImageVector, tint: Color, contentDescription: String, testTag: String, onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = ObsidianSurface.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, ObsidianOutline),
        modifier = Modifier.size(48.dp).testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun VirtualJoystick(accentColor: Color, onMove: (Float, Float) -> Unit, modifier: Modifier = Modifier) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    val maxRadiusPx = 110f

    Box(
        modifier = modifier
            .size(134.dp)
            .testTag("hud_virtual_joystick")
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { knobOffset = Offset.Zero; onMove(0f, 0f) },
                    onDragCancel = { knobOffset = Offset.Zero; onMove(0f, 0f) },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val next = knobOffset + dragAmount
                        val dist = hypot(next.x, next.y)
                        knobOffset = if (dist > maxRadiusPx) Offset(next.x / dist * maxRadiusPx, next.y / dist * maxRadiusPx) else next
                        onMove(knobOffset.x / maxRadiusPx, knobOffset.y / maxRadiusPx)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                brush = Brush.radialGradient(listOf(ObsidianSurface.copy(alpha = 0.42f), ObsidianBg.copy(alpha = 0.72f))),
                radius = size.minDimension / 2f - 4f,
                center = center
            )
            drawCircle(color = accentColor.copy(alpha = 0.5f), radius = size.minDimension / 2f - 4f, center = center, style = Stroke(width = 2.dp.toPx()))
            drawCircle(color = Color.White.copy(alpha = 0.16f), radius = (size.minDimension / 2f - 4f) * 0.72f, center = center, style = Stroke(width = 1.dp.toPx()))
            val knobPos = center + knobOffset
            drawCircle(color = accentColor.copy(alpha = 0.92f), radius = 23.dp.toPx(), center = knobPos)
            drawCircle(color = Color.White.copy(alpha = 0.7f), radius = 11.dp.toPx(), center = knobPos)
        }
    }
}

@Composable
private fun MiniMapWidget(
    state: GameUiState,
    waypointCoords: Pair<Float, Float>?,
    onClick: () -> Unit
) {
    val arrowPath = remember { Path() }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = ObsidianSurface.copy(alpha = 0.88f),
        border = BorderStroke(1.5.dp, ObsidianOutline),
        modifier = Modifier.size(102.dp).testTag("hud_minimap")
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            val mapW = size.width
            val mapH = size.height
            drawRect(Color(0xFF334155), Offset(0f, 0f), Size(mapW, mapH * 0.33f))
            drawRect(Color(0xFF133831), Offset(0f, mapH * 0.33f), Size(mapW * 0.35f, mapH * 0.67f))
            drawRect(Color(0xFF28593C), Offset(mapW * 0.35f, mapH * 0.33f), Size(mapW * 0.3f, mapH * 0.67f))
            drawRect(Color(0xFF8C6D3F), Offset(mapW * 0.65f, mapH * 0.33f), Size(mapW * 0.35f, mapH * 0.67f))

            // Circular Safe Zone Ring on Minimap!
            val zx = (state.zoneCenterX / WORLD_SIZE) * mapW
            val zy = (state.zoneCenterY / WORLD_SIZE) * mapH
            val zr = (state.zoneRadius / WORLD_SIZE) * mapW
            drawCircle(Color(0xFF38BDF8), radius = zr, center = Offset(zx, zy), style = Stroke(2.dp.toPx()))

            // Ground Loot Markers
            for (loot in state.groundLoot) {
                if (loot.pickedUp) continue
                val lx = (loot.x / WORLD_SIZE) * mapW
                val ly = (loot.y / WORLD_SIZE) * mapH
                drawCircle(SolarGold, radius = 1.8.dp.toPx(), center = Offset(lx, ly))
            }

            // 3 AI Allies (Cyan)
            for (ally in state.allies) {
                if (!ally.isAlive) continue
                val ax = (ally.x / WORLD_SIZE) * mapW
                val ay = (ally.y / WORLD_SIZE) * mapH
                drawCircle(AetherCyan, radius = 2.4.dp.toPx(), center = Offset(ax, ay))
            }

            // Enemies (Red)
            for (enemy in state.enemies) {
                if (!enemy.isAlive) continue
                if (state.gameMode == GameMode.CLASH_SQUAD || hypot(enemy.x - state.playerX, enemy.y - state.playerY) < 750f) {
                    val ex = (enemy.x / WORLD_SIZE) * mapW
                    val ey = (enemy.y / WORLD_SIZE) * mapH
                    drawCircle(Color(0xFFFF4D6D), radius = if (enemy.kind.isBoss) 3.5.dp.toPx() else 2.2.dp.toPx(), center = Offset(ex, ey))
                }
            }

            // Waypoints in Open World
            for (item in state.interactables) {
                if (item.kind == InteractableKind.WAYPOINT) {
                    val mx = (item.x / WORLD_SIZE) * mapW
                    val my = (item.y / WORLD_SIZE) * mapH
                    drawCircle(if (item.activated) AetherCyan else Color.Gray, radius = 2.5.dp.toPx(), center = Offset(mx, my))
                }
            }

            // Player Arrow
            val px = (state.playerX / WORLD_SIZE) * mapW
            val py = (state.playerY / WORLD_SIZE) * mapH
            val fx = cos(state.cameraYaw)
            val fy = sin(state.cameraYaw)
            val rx = -fy
            val ry = fx
            val r = 5.dp.toPx()
            arrowPath.reset()
            arrowPath.moveTo(px + fx * r * 1.4f, py + fy * r * 1.4f)
            arrowPath.lineTo(px - fx * r * 0.8f + rx * r * 0.85f, py - fy * r * 0.8f + ry * r * 0.85f)
            arrowPath.lineTo(px - fx * r * 0.8f - rx * r * 0.85f, py - fy * r * 0.8f - ry * r * 0.85f)
            arrowPath.close()
            drawPath(arrowPath, color = Color.White)
            drawCircle(AetherCyan, radius = 2.dp.toPx(), center = Offset(px, py))
        }
    }
}

@Composable
private fun DialogueCard(dialogue: ActiveDialogue, isBengali: Boolean, onDismiss: () -> Unit) {
    Surface(
        color = ObsidianSurface.copy(alpha = 0.96f),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.5.dp, SolarGold.copy(alpha = 0.7f)),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 540.dp)
            .padding(horizontal = 8.dp)
            .testTag("hud_dialogue_card")
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.Explore, contentDescription = null, tint = SolarGold, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = if (isBengali) dialogue.speakerBn else dialogue.speakerEn, style = MaterialTheme.typography.titleMedium, color = SolarGold)
                Spacer(Modifier.height(4.dp))
                Text(text = if (isBengali) dialogue.messageBn else dialogue.messageEn, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = onDismiss, modifier = Modifier.testTag("dialogue_close_button")) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_close_dialog), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
