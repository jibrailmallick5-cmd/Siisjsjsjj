package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.game.AppLanguage
import com.example.game.GameStrings
import com.example.game.GameUiState
import com.example.game.InteractableKind
import com.example.game.WORLD_SIZE
import com.example.ui.theme.AetherCyan
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.EmeraldLife
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.SolarGold

@Composable
fun WorldMapOverlay(
    state: GameUiState,
    waypointCoords: Pair<Float, Float>?,
    onFastTravel: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onClose)
    val lang = state.language
    val helipads = state.interactables.filter { it.kind == InteractableKind.WAYPOINT }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg.copy(alpha = 0.82f))
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
                .widthIn(max = 600.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Map, contentDescription = null, tint = AetherCyan, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(GameStrings.worldMap(lang), style = MaterialTheme.typography.headlineMedium)
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("world_map_close_button")
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_close_dialog))
                    }
                }

                Text(
                    text = GameStrings.tapToFastTravel(lang),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(10.dp))

                // Interactive Tactical Battleground World Overview Canvas
                Surface(
                    color = ObsidianBg,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, ObsidianOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                        val w = size.width
                        val h = size.height

                        // 4 Tactical Biomes Layout
                        drawRect(Color(0xFF334155), Offset(0f, 0f), Size(w, h * 0.33f))
                        drawRect(Color(0xFF133831), Offset(0f, h * 0.33f), Size(w * 0.35f, h * 0.67f))
                        drawRect(Color(0xFF28593C), Offset(w * 0.35f, h * 0.33f), Size(w * 0.3f, h * 0.67f))
                        drawRect(Color(0xFF8C6D3F), Offset(w * 0.65f, h * 0.33f), Size(w * 0.35f, h * 0.67f))

                        // Military Roads
                        drawLine(Color(0x99CBD5E1), Offset(w * 0.5f, h * 0.15f), Offset(w * 0.5f, h * 0.78f), strokeWidth = 5f)
                        drawLine(Color(0x99CBD5E1), Offset(w * 0.2f, h * 0.68f), Offset(w * 0.82f, h * 0.68f), strokeWidth = 5f)

                        // Airdrops, Intel Terminals, Ammo Crates & Extraction Helipads
                        for (item in state.interactables) {
                            val mx = (item.x / WORLD_SIZE) * w
                            val my = (item.y / WORLD_SIZE) * h
                            when (item.kind) {
                                InteractableKind.AIRDROP_CRATE -> if (!item.activated) {
                                    drawCircle(CrimsonDanger, radius = 4.5f, center = Offset(mx, my))
                                }
                                InteractableKind.INTEL_TERMINAL -> {
                                    drawCircle(if (item.activated) EmeraldLife else Color(0xFF94A3B8), radius = 4f, center = Offset(mx, my))
                                }
                                InteractableKind.AMMO_BOX -> if (!item.activated) {
                                    drawCircle(SolarGold, radius = 3.5f, center = Offset(mx, my))
                                }
                                InteractableKind.WAYPOINT -> {
                                    drawCircle(if (item.activated) AetherCyan else Color.DarkGray, radius = 7f, center = Offset(mx, my))
                                    drawCircle(Color.White, radius = 7f, center = Offset(mx, my), style = Stroke(2f))
                                }
                                else -> {}
                            }
                        }

                        // Circular Safe Zone Ring
                        val zx = (state.zoneCenterX / WORLD_SIZE) * w
                        val zy = (state.zoneCenterY / WORLD_SIZE) * h
                        val zr = (state.zoneRadius / WORLD_SIZE) * w
                        drawCircle(Color(0x3300E5FF), radius = zr, center = Offset(zx, zy))
                        drawCircle(Color(0xFF38BDF8), radius = zr, center = Offset(zx, zy), style = Stroke(3.5f))

                        // Ground Gun & Gear Loot
                        for (loot in state.groundLoot) {
                            if (loot.pickedUp) continue
                            val lx = (loot.x / WORLD_SIZE) * w
                            val ly = (loot.y / WORLD_SIZE) * h
                            drawCircle(SolarGold, radius = 3f, center = Offset(lx, ly))
                        }

                        // 3 AI Allies (Cyan)
                        for (ally in state.allies) {
                            if (!ally.isAlive) continue
                            val ax = (ally.x / WORLD_SIZE) * w
                            val ay = (ally.y / WORLD_SIZE) * h
                            drawCircle(AetherCyan, radius = 4f, center = Offset(ax, ay))
                        }

                        // Hostile Squad Markers
                        for (enemy in state.enemies) {
                            if (!enemy.isAlive) continue
                            val ex = (enemy.x / WORLD_SIZE) * w
                            val ey = (enemy.y / WORLD_SIZE) * h
                            drawCircle(
                                color = if (enemy.kind.isBoss) Color(0xFFFF4D6D) else Color(0xFFF87171),
                                radius = if (enemy.kind.isBoss) 6f else 3.5f,
                                center = Offset(ex, ey)
                            )
                        }

                        // Active Mission Target
                        waypointCoords?.let { (tx, ty) ->
                            val mx = (tx / WORLD_SIZE) * w
                            val my = (ty / WORLD_SIZE) * h
                            drawCircle(SolarGold, radius = 10f, center = Offset(mx, my), style = Stroke(3f))
                        }

                        // Player Position
                        val px = (state.playerX / WORLD_SIZE) * w
                        val py = (state.playerY / WORLD_SIZE) * h
                        drawCircle(Color.White, radius = 7f, center = Offset(px, py))
                        drawCircle(AetherCyan, radius = 4.5f, center = Offset(px, py))
                    }
                }

                Spacer(Modifier.height(12.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(helipads, key = { it.id }) { wp ->
                        Surface(
                            color = ObsidianSurfaceElevated,
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, if (wp.activated) AetherCyan.copy(alpha = 0.55f) else ObsidianOutline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (lang == AppLanguage.BN) wp.labelBn else wp.labelEn,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = GameStrings.biomeName(wp.biome, lang),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = { onFastTravel(wp.id) },
                                    enabled = wp.activated,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AetherCyan,
                                        contentColor = ObsidianBg
                                    ),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("fast_travel_${wp.id}")
                                ) {
                                    Icon(
                                        imageVector = if (wp.activated) Icons.Rounded.FlightTakeoff else Icons.Rounded.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (wp.activated) {
                                            if (lang == AppLanguage.BN) "যান" else "Deploy"
                                        } else {
                                            if (lang == AppLanguage.BN) "লকড" else "Locked"
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
