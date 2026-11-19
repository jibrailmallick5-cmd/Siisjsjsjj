package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Redeem
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.game.AppLanguage
import com.example.game.GameStrings
import com.example.game.GameUiState
import com.example.game.MissionDefinition
import com.example.ui.theme.AetherCyan
import com.example.ui.theme.EmeraldLife
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.SolarGold

@Composable
fun MissionsSheet(
    state: GameUiState,
    onTrackMission: (String) -> Unit,
    onClaimReward: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onClose)
    val lang = state.language

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg.copy(alpha = 0.78f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = ObsidianSurface,
            shape = MaterialTheme.shapes.extraLarge,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, ObsidianOutline),
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
                        Icon(Icons.Rounded.MilitaryTech, contentDescription = null, tint = SolarGold, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = GameStrings.missions(lang),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("missions_close_button")
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_close_dialog))
                    }
                }

                Spacer(Modifier.height(12.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(state.missions, key = { it.id }) { mission ->
                        MissionCard(
                            mission = mission,
                            isTracked = mission.id == state.trackedMissionId,
                            lang = lang,
                            onTrack = { onTrackMission(mission.id) },
                            onClaim = { onClaimReward(mission.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MissionCard(
    mission: MissionDefinition,
    isTracked: Boolean,
    lang: AppLanguage,
    onTrack: () -> Unit,
    onClaim: () -> Unit
) {
    val borderColor = when {
        mission.completed && !mission.rewardClaimed -> EmeraldLife
        isTracked -> SolarGold
        else -> ObsidianOutline
    }

    Surface(
        color = ObsidianSurfaceElevated,
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.2.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (lang == AppLanguage.BN) mission.titleBn else mission.titleEn,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${mission.progress}/${mission.targetCount}",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (mission.completed) EmeraldLife else AetherCyan
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = if (lang == AppLanguage.BN) mission.descBn else mission.descEn,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { (mission.progress.toFloat() / mission.targetCount.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (mission.completed) EmeraldLife else AetherCyan,
                trackColor = ObsidianBg
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "+${mission.rewardGold}G  •  +${mission.rewardGems}◆  •  +${mission.rewardXp}XP",
                    style = MaterialTheme.typography.labelLarge,
                    color = SolarGold
                )

                when {
                    mission.completed && !mission.rewardClaimed -> {
                        Button(
                            onClick = onClaim,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldLife, contentColor = ObsidianBg),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("mission_claim_${mission.id}")
                        ) {
                            Icon(Icons.Rounded.Redeem, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(GameStrings.claimReward(lang))
                        }
                    }
                    mission.rewardClaimed -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = EmeraldLife, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = GameStrings.claimed(lang),
                                style = MaterialTheme.typography.labelLarge,
                                color = EmeraldLife
                            )
                        }
                    }
                    else -> {
                        OutlinedButton(
                            onClick = onTrack,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("mission_track_${mission.id}")
                        ) {
                            Icon(
                                Icons.Rounded.GpsFixed,
                                contentDescription = null,
                                tint = if (isTracked) SolarGold else AetherCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isTracked) GameStrings.tracked(lang) else GameStrings.trackMission(lang),
                                color = if (isTracked) SolarGold else AetherCyan
                            )
                        }
                    }
                }
            }
        }
    }
}
