package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.game.AppLanguage
import com.example.game.GameMode
import com.example.game.GameStrings
import com.example.game.GameUiState
import com.example.ui.canvas.HeroPreviewCanvas
import com.example.ui.theme.AetherCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.SolarGold

@Composable
fun TitleOverlay(
    state: GameUiState,
    onSelectMode: (GameMode) -> Unit,
    onStartGame: () -> Unit,
    onOpenCustomize: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleLanguage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = state.language
    var lobbyYaw by remember { mutableFloatStateOf(0.22f) }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Photorealistic Military Hangar Lobby Backdrop
        Image(
            painter = painterResource(id = R.drawable.img_lobby_bg),
            contentDescription = stringResource(R.string.cd_tactical_banner),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ObsidianBg.copy(alpha = 0.55f),
                            Color.Transparent,
                            ObsidianBg.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // 2. Standing Realistic Character in Center Lobby (drag horizontally to inspect 360°)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        lobbyYaw += dragAmount * 0.01f
                    }
                }
        ) {
            HeroPreviewCanvas(
                cloakIndex = state.cloakIndex,
                hatIndex = state.hatIndex,
                weaponIndex = state.weaponIndex,
                auraIndex = state.auraIndex,
                isFullLobby = true,
                yawAngle = lobbyYaw,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 64.dp, bottom = 140.dp)
                    .testTag("lobby_character_preview")
            )
        }

        // 3. Lobby Top & Bottom Tactical UI
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(14.dp)
        ) {
            // TOP BAR: Player Profile Badge (Left) + Language & Settings Buttons (Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    color = ObsidianSurface.copy(alpha = 0.88f),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, ObsidianOutline),
                    modifier = Modifier.widthIn(max = 240.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            text = "${state.playerName}  •  ${GameStrings.levelLabel(state.level, lang)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = AetherCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "HP ${state.maxHp.toInt()}  •  ${state.gold}G  •  ${state.aetherGems}◆",
                            style = MaterialTheme.typography.labelSmall,
                            color = SolarGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = onToggleLanguage,
                        shape = RoundedCornerShape(24.dp),
                        color = ObsidianSurface.copy(alpha = 0.88f),
                        border = BorderStroke(1.dp, ObsidianOutline),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("title_language_toggle")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Icon(Icons.Rounded.Language, contentDescription = null, tint = AetherCyan, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (lang == AppLanguage.BN) "বাংলা" else "EN", color = AetherCyan, style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    Surface(
                        onClick = onOpenSettings,
                        shape = CircleShape,
                        color = ObsidianSurface.copy(alpha = 0.88f),
                        border = BorderStroke(1.dp, AetherCyan.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("lobby_settings_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.Settings,
                                contentDescription = stringResource(R.string.cd_settings_button),
                                tint = AetherCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // LOBBY GAME TITLE BADGE
            Surface(
                color = ObsidianSurface.copy(alpha = 0.80f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SolarGold.copy(alpha = 0.55f)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 60.dp)
                    .testTag("lobby_game_title")
            ) {
                Text(
                    text = GameStrings.appTitle(lang),
                    style = MaterialTheme.typography.titleMedium,
                    color = SolarGold,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // BOTTOM LOBBY CONTROLS: Mode Selector + Armory + START MATCH
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .align(Alignment.BottomCenter)
            ) {
                // Active Loadout Strip
                Surface(
                    color = ObsidianSurfaceElevated.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ObsidianOutline)
                ) {
                    Text(
                        text = "${if (lang == AppLanguage.BN) state.activeWeapon.nameBn else state.activeWeapon.nameEn}  •  ${GameStrings.dualHandTip(lang)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }

                // Mode Selection Chips (Clash Squad 4v4 Default vs Open World)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.gameMode == GameMode.CLASH_SQUAD,
                        onClick = { onSelectMode(GameMode.CLASH_SQUAD) },
                        leadingIcon = { Icon(Icons.Rounded.Groups, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        label = {
                            Text(
                                text = GameStrings.clashSquadMode(lang),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SolarGold.copy(alpha = 0.28f),
                            selectedLabelColor = SolarGold,
                            selectedLeadingIconColor = SolarGold
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("mode_clash_squad")
                    )

                    FilterChip(
                        selected = state.gameMode == GameMode.OPEN_WORLD,
                        onClick = { onSelectMode(GameMode.OPEN_WORLD) },
                        leadingIcon = { Icon(Icons.Rounded.Public, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        label = {
                            Text(
                                text = GameStrings.openWorldMode(lang),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AetherCyan.copy(alpha = 0.25f),
                            selectedLabelColor = AetherCyan,
                            selectedLeadingIconColor = AetherCyan
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("mode_open_world")
                    )
                }

                // Armory & START MATCH Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenCustomize,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.5.dp, AetherCyan.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .weight(0.95f)
                            .height(56.dp)
                            .testTag("title_customize_button")
                    ) {
                        Icon(Icons.Rounded.Palette, contentDescription = null, tint = AetherCyan)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = GameStrings.customizeHero(lang),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = onStartGame,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SolarGold,
                            contentColor = ObsidianBg
                        ),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .weight(1.2f)
                            .height(56.dp)
                            .testTag("title_start_button")
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = GameStrings.startMatch(lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
