package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.game.AppLanguage
import com.example.game.GameStrings
import com.example.game.GameUiState
import com.example.game.GraphicsQuality
import com.example.game.TimeOfDayMode
import com.example.ui.theme.AetherCyan
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.SolarGold

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsOverlay(
    state: GameUiState,
    inMatch: Boolean,
    onSetGeneralSensitivity: (Float) -> Unit,
    onSetScopeSensitivity: (Float) -> Unit,
    onSetAutoLoot: (Boolean) -> Unit,
    onSetSoundEnabled: (Boolean) -> Unit,
    onSetSoundVolume: (Float) -> Unit,
    onSetGraphicsQuality: (GraphicsQuality) -> Unit,
    onSetTimeMode: (TimeOfDayMode) -> Unit,
    onSetLanguage: (AppLanguage) -> Unit,
    onReturnToLobby: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onClose)
    val lang = state.language

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg.copy(alpha = 0.84f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = ObsidianSurface,
            shape = MaterialTheme.shapes.extraLarge,
            border = BorderStroke(1.5.dp, ObsidianOutline),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 580.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Settings, contentDescription = null, tint = AetherCyan, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(GameStrings.settingsTitle(lang), style = MaterialTheme.typography.titleLarge)
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("settings_close_button")
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_close_dialog))
                    }
                }

                Spacer(Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Sensitivity Controls Card
                    Surface(
                        color = ObsidianSurfaceElevated,
                        shape = MaterialTheme.shapes.large,
                        border = BorderStroke(1.dp, AetherCyan.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Tune, contentDescription = null, tint = SolarGold, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = GameStrings.sensitivityHint(lang),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // General Sensitivity Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(GameStrings.generalSensitivityLabel(lang), style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${(state.generalSensitivity * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = AetherCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = state.generalSensitivity,
                                onValueChange = onSetGeneralSensitivity,
                                valueRange = 0.25f..3.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = AetherCyan,
                                    activeTrackColor = AetherCyan
                                ),
                                modifier = Modifier.testTag("settings_general_sens_slider")
                            )

                            // Quick Sensitivity Presets
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(0.55f to "Low", 1.0f to "Normal", 1.75f to "Pro", 2.6f to "Ultra").forEach { (presetVal, label) ->
                                    FilterChip(
                                        selected = kotlin.math.abs(state.generalSensitivity - presetVal) < 0.12f,
                                        onClick = { onSetGeneralSensitivity(presetVal) },
                                        label = { Text(label) },
                                        modifier = Modifier.testTag("sens_preset_${label.lowercase()}")
                                    )
                                }
                            }

                            // Scope Sensitivity Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(GameStrings.scopeSensitivityLabel(lang), style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${(state.scopeSensitivity * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = SolarGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = state.scopeSensitivity,
                                onValueChange = onSetScopeSensitivity,
                                valueRange = 0.20f..2.50f,
                                colors = SliderDefaults.colors(
                                    thumbColor = SolarGold,
                                    activeTrackColor = SolarGold
                                ),
                                modifier = Modifier.testTag("settings_scope_sens_slider")
                            )
                        }
                    }

                    // Auto-Loot Toggle
                    Surface(
                        color = ObsidianSurfaceElevated,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, ObsidianOutline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = GameStrings.autoLootLabel(lang),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = state.autoLootEnabled,
                                onCheckedChange = onSetAutoLoot,
                                colors = SwitchDefaults.colors(checkedThumbColor = AetherCyan),
                                modifier = Modifier.testTag("settings_autoloot_switch")
                            )
                        }
                    }

                    // Audio & SFX Controls Card
                    Surface(
                        color = ObsidianSurfaceElevated,
                        shape = MaterialTheme.shapes.large,
                        border = BorderStroke(1.dp, ObsidianOutline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = if (state.soundEnabled) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                                        contentDescription = null,
                                        tint = if (state.soundEnabled) AetherCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = GameStrings.soundEffectsLabel(lang),
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                                Switch(
                                    checked = state.soundEnabled,
                                    onCheckedChange = onSetSoundEnabled,
                                    colors = SwitchDefaults.colors(checkedThumbColor = AetherCyan),
                                    modifier = Modifier.testTag("settings_sound_switch")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(GameStrings.soundVolumeLabel(lang), style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "${(state.soundVolume * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (state.soundEnabled) AetherCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = state.soundVolume,
                                onValueChange = onSetSoundVolume,
                                enabled = state.soundEnabled,
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = AetherCyan,
                                    activeTrackColor = AetherCyan
                                ),
                                modifier = Modifier.testTag("settings_sound_volume_slider")
                            )
                        }
                    }

                    // HD Graphics Quality
                    Text(GameStrings.graphicsQualityLabel(lang), style = MaterialTheme.typography.titleMedium, color = AetherCyan)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GraphicsQuality.entries.forEach { q ->
                            FilterChip(
                                selected = state.graphicsQuality == q,
                                onClick = { onSetGraphicsQuality(q) },
                                label = { Text(GameStrings.qualityName(q, lang)) },
                                modifier = Modifier.testTag("settings_graphics_${q.level}")
                            )
                        }
                    }

                    // Lighting Mode
                    Text(GameStrings.timeModeLabel(lang), style = MaterialTheme.typography.titleMedium, color = AetherCyan)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TimeOfDayMode.entries.forEach { m ->
                            FilterChip(
                                selected = state.timeMode == m,
                                onClick = { onSetTimeMode(m) },
                                label = { Text(GameStrings.timeModeName(m, lang)) },
                                modifier = Modifier.testTag("settings_time_${m.index}")
                            )
                        }
                    }

                    // Language Switch
                    Text(GameStrings.languageLabel(lang), style = MaterialTheme.typography.titleMedium, color = AetherCyan)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = state.language == AppLanguage.BN,
                            onClick = { onSetLanguage(AppLanguage.BN) },
                            label = { Text("বাংলা (Bengali)") },
                            modifier = Modifier.testTag("settings_lang_bn")
                        )
                        FilterChip(
                            selected = state.language == AppLanguage.EN,
                            onClick = { onSetLanguage(AppLanguage.EN) },
                            label = { Text("English") },
                            modifier = Modifier.testTag("settings_lang_en")
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (inMatch) {
                        OutlinedButton(
                            onClick = onReturnToLobby,
                            border = BorderStroke(1.dp, CrimsonDanger),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("settings_return_lobby_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ExitToApp, contentDescription = null, tint = CrimsonDanger)
                            Spacer(Modifier.width(6.dp))
                            Text(GameStrings.backToLobby(lang), color = CrimsonDanger, maxLines = 1)
                        }
                    }
                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(containerColor = AetherCyan, contentColor = ObsidianBg),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("settings_done_button")
                    ) {
                        Text(if (lang == AppLanguage.BN) "ঠিক আছে" else "Apply", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
