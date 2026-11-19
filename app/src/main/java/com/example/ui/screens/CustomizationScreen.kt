package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Upgrade
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.game.AppLanguage
import com.example.game.CustomizationCatalog
import com.example.game.GameStrings
import com.example.game.GameUiState
import com.example.game.GraphicsQuality
import com.example.game.TimeOfDayMode
import com.example.ui.canvas.HeroPreviewCanvas
import com.example.ui.theme.AetherCyan
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.SolarGold

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomizationScreen(
    state: GameUiState,
    upgradeCostFn: (Int) -> Int,
    onSelectCloak: (Int) -> Unit,
    onSelectHat: (Int) -> Unit,
    onSelectWeapon: (Int) -> Unit,
    onSelectAura: (Int) -> Unit,
    onUpgradeStat: (String) -> Unit,
    onSetGraphicsQuality: (GraphicsQuality) -> Unit,
    onSetTimeMode: (TimeOfDayMode) -> Unit,
    onSetLanguage: (AppLanguage) -> Unit,
    onResetAdventure: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onClose)
    val lang = state.language
    var selectedTab by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg.copy(alpha = 0.8f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = ObsidianSurface,
            shape = MaterialTheme.shapes.extraLarge,
            border = BorderStroke(1.5.dp, ObsidianOutline),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Palette, contentDescription = null, tint = AetherCyan, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = GameStrings.customizeTitle(lang),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${state.gold}G",
                            style = MaterialTheme.typography.labelLarge,
                            color = SolarGold
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.testTag("customize_close_button")
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_close_dialog))
                        }
                    }
                }

                // Live Animated Tactical Operative Preview Banner
                Surface(
                    color = ObsidianBg,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, ObsidianOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(145.dp)
                ) {
                    HeroPreviewCanvas(
                        cloakIndex = state.cloakIndex,
                        hatIndex = state.hatIndex,
                        weaponIndex = state.weaponIndex,
                        auraIndex = state.auraIndex,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(Modifier.height(10.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = ObsidianSurfaceElevated,
                    contentColor = AetherCyan
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(GameStrings.tabAppearance(lang)) },
                        icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("customize_tab_appearance")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(GameStrings.tabUpgrades(lang)) },
                        icon = { Icon(Icons.Rounded.Upgrade, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("customize_tab_upgrades")
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text(GameStrings.tabGraphics(lang)) },
                        icon = { Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("customize_tab_graphics")
                    )
                }

                Spacer(Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // Tactical Outfits
                            Text(GameStrings.cloakColor(lang), style = MaterialTheme.typography.titleMedium, color = AetherCyan)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CustomizationCatalog.outfits.forEach { outfit ->
                                    val selected = outfit.index == state.cloakIndex
                                    Surface(
                                        color = ObsidianSurfaceElevated,
                                        shape = MaterialTheme.shapes.small,
                                        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) AetherCyan else ObsidianOutline),
                                        modifier = Modifier
                                            .clickable { onSelectCloak(outfit.index) }
                                            .testTag("cloak_option_${outfit.index}")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(outfit.primaryColor)
                                                    .border(1.5.dp, outfit.trimColor, CircleShape)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = if (lang == AppLanguage.BN) outfit.nameBn else outfit.nameEn,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }

                            // Helmet & Mask Chips
                            Text(GameStrings.headgear(lang), style = MaterialTheme.typography.titleMedium, color = AetherCyan)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CustomizationCatalog.headgears.forEach { hat ->
                                    FilterChip(
                                        selected = hat.index == state.hatIndex,
                                        onClick = { onSelectHat(hat.index) },
                                        label = { Text(if (lang == AppLanguage.BN) hat.nameBn else hat.nameEn) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = AetherCyan.copy(alpha = 0.22f),
                                            selectedLabelColor = AetherCyan
                                        ),
                                        modifier = Modifier.testTag("hat_option_${hat.index}")
                                    )
                                }
                            }

                            // Primary Firearms
                            Text(GameStrings.weaponStyle(lang), style = MaterialTheme.typography.titleMedium, color = AetherCyan)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CustomizationCatalog.weapons.forEach { weapon ->
                                    val labelName = if (lang == AppLanguage.BN) weapon.nameBn else weapon.nameEn
                                    FilterChip(
                                        selected = weapon.index == state.weaponIndex,
                                        onClick = { onSelectWeapon(weapon.index) },
                                        label = { Text("$labelName • DMG ${weapon.damage.toInt()}") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SolarGold.copy(alpha = 0.22f),
                                            selectedLabelColor = SolarGold
                                        ),
                                        modifier = Modifier.testTag("weapon_option_${weapon.index}")
                                    )
                                }
                            }

                            // Tactical Backpacks
                            Text(GameStrings.elementalAura(lang), style = MaterialTheme.typography.titleMedium, color = AetherCyan)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CustomizationCatalog.backpacks.forEach { pack ->
                                    FilterChip(
                                        selected = pack.index == state.auraIndex,
                                        onClick = { onSelectAura(pack.index) },
                                        label = { Text(if (lang == AppLanguage.BN) pack.nameBn else pack.nameEn) },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(pack.coreColor)
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = pack.coreColor.copy(alpha = 0.2f),
                                            selectedLabelColor = pack.coreColor
                                        ),
                                        modifier = Modifier.testTag("aura_option_${pack.index}")
                                    )
                                }
                            }
                        }

                        1 -> {
                            val reloadFactor = (1f - (state.energyLevel - 1) * 0.06f).coerceAtLeast(0.45f)
                            val activeDmg = state.activeWeapon.damage * state.effectiveDamageMultiplier
                            UpgradeStatCard(
                                title = GameStrings.statVitality(lang),
                                currentLevel = state.vitalityLevel,
                                detail = "Max HP: ${state.maxHp.toInt()}  •  Armor: ${state.maxArmor.toInt()}",
                                cost = upgradeCostFn(state.vitalityLevel),
                                gold = state.gold,
                                lang = lang,
                                testTag = "upgrade_vitality_button",
                                onUpgrade = { onUpgradeStat("vitality") }
                            )
                            UpgradeStatCard(
                                title = GameStrings.statMight(lang),
                                currentLevel = state.mightLevel,
                                detail = "${state.activeWeapon.id.uppercase()} DMG: ${activeDmg.toInt()} (${(state.effectiveDamageMultiplier * 100).toInt()}%)",
                                cost = upgradeCostFn(state.mightLevel),
                                gold = state.gold,
                                lang = lang,
                                testTag = "upgrade_might_button",
                                onUpgrade = { onUpgradeStat("might") }
                            )
                            UpgradeStatCard(
                                title = GameStrings.statAgility(lang),
                                currentLevel = state.agilityLevel,
                                detail = "Sprint Speed: ${state.effectiveSprintSpeed.toInt()}",
                                cost = upgradeCostFn(state.agilityLevel),
                                gold = state.gold,
                                lang = lang,
                                testTag = "upgrade_agility_button",
                                onUpgrade = { onUpgradeStat("agility") }
                            )
                            UpgradeStatCard(
                                title = GameStrings.statEnergy(lang),
                                currentLevel = state.energyLevel,
                                detail = "Reload: ${"%.2f".format(state.activeWeapon.reloadTime * reloadFactor)}s",
                                cost = upgradeCostFn(state.energyLevel),
                                gold = state.gold,
                                lang = lang,
                                testTag = "upgrade_energy_button",
                                onUpgrade = { onUpgradeStat("energy") }
                            )
                        }

                        2 -> {
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
                                        modifier = Modifier.testTag("graphics_quality_${q.level}")
                                    )
                                }
                            }

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
                                        modifier = Modifier.testTag("time_mode_${m.index}")
                                    )
                                }
                            }

                            Text(GameStrings.languageLabel(lang), style = MaterialTheme.typography.titleMedium, color = AetherCyan)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FilterChip(
                                    selected = state.language == AppLanguage.BN,
                                    onClick = { onSetLanguage(AppLanguage.BN) },
                                    label = { Text("বাংলা (Bengali)") },
                                    modifier = Modifier.testTag("language_bn_chip")
                                )
                                FilterChip(
                                    selected = state.language == AppLanguage.EN,
                                    onClick = { onSetLanguage(AppLanguage.EN) },
                                    label = { Text("English") },
                                    modifier = Modifier.testTag("language_en_chip")
                                )
                            }

                            Spacer(Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = onResetAdventure,
                                border = BorderStroke(1.dp, CrimsonDanger),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("reset_adventure_button")
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, tint = CrimsonDanger)
                                Spacer(Modifier.width(8.dp))
                                Text(GameStrings.resetWorld(lang), color = CrimsonDanger)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpgradeStatCard(
    title: String,
    currentLevel: Int,
    detail: String,
    cost: Int,
    gold: Int,
    lang: AppLanguage,
    testTag: String,
    onUpgrade: () -> Unit
) {
    val isMaxed = currentLevel >= 10
    Surface(
        color = ObsidianSurfaceElevated,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, ObsidianOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "$title (Lv.$currentLevel/10)", style = MaterialTheme.typography.titleMedium)
                Text(text = detail, style = MaterialTheme.typography.bodyMedium, color = AetherCyan)
            }
            Button(
                onClick = onUpgrade,
                enabled = !isMaxed && gold >= cost,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SolarGold,
                    contentColor = ObsidianBg,
                    disabledContainerColor = ObsidianBg,
                    disabledContentColor = Color.Gray
                ),
                modifier = Modifier
                    .height(48.dp)
                    .testTag(testTag)
            ) {
                Text(
                    text = if (isMaxed) GameStrings.maxed(lang) else "${GameStrings.upgrade(lang)} (${cost}G)",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
