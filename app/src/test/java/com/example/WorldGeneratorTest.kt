package com.example

import com.example.game.BiomeType
import com.example.game.CLASH_ROUND_COUNTDOWN_SEC
import com.example.game.CustomizationCatalog
import com.example.game.InteractableKind
import com.example.game.LootKind
import com.example.game.PropKind
import com.example.game.STANDARD_MAX_HP
import com.example.game.WorldGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldGeneratorTest {

    @Test
    fun biomesCoverAllFourRegions() {
        assertEquals(BiomeType.CLOCKTOWER_VALLEY, WorldGenerator.biomeAt(1600f, 2200f))
        assertEquals(BiomeType.PINE_FOREST, WorldGenerator.biomeAt(600f, 2000f))
        assertEquals(BiomeType.DESERT_OUTPOST, WorldGenerator.biomeAt(2600f, 2200f))
        assertEquals(BiomeType.MILITARY_PEAK, WorldGenerator.biomeAt(1600f, 520f))
    }

    @Test
    fun missionsAndInteractablesAreConsistent() {
        val missions = WorldGenerator.initialMissions()
        assertEquals(5, missions.size)

        val interactables = WorldGenerator.generateInteractables(
            discoveredWaypoints = setOf("wp_clocktower"),
            openedAirdrops = emptySet(),
            disabledIntels = emptySet(),
            collectedAmmo = emptySet()
        )
        assertEquals(4, interactables.count { it.kind == InteractableKind.WAYPOINT })
        assertEquals(3, interactables.count { it.kind == InteractableKind.INTEL_TERMINAL })
        assertEquals(4, interactables.count { it.kind == InteractableKind.AMMO_BOX })
        assertEquals(4, interactables.count { it.kind == InteractableKind.AIRDROP_CRATE })
        assertTrue(CustomizationCatalog.outfits.size >= 6)
        assertTrue(CustomizationCatalog.weapons.size >= 4)
        assertTrue(CustomizationCatalog.backpacks.size >= 4)
    }

    @Test
    fun clashSquadHasFourVsFourWith200HpHousesAndGunLoot() {
        assertEquals(15f, CLASH_ROUND_COUNTDOWN_SEC, 0.01f)
        assertEquals(200f, STANDARD_MAX_HP, 0.01f)

        val allies = WorldGenerator.generateClashSquadAllies()
        val enemies = WorldGenerator.generateClashSquadEnemies()
        assertEquals(3, allies.size) // +1 Player = 4 allies
        assertEquals(4, enemies.size) // 4 enemies
        assertTrue(allies.all { it.maxHp == 200f && it.hp == 200f })
        assertTrue(enemies.all { it.maxHp == 200f && it.hp == 200f })

        val clashProps = WorldGenerator.generateClashSquadProps()
        assertTrue(clashProps.count { it.kind == PropKind.BUILDING_HOUSE } >= 4)

        val loot = WorldGenerator.generateClashSquadLoot()
        assertTrue(loot.any { it.kind == LootKind.WEAPON_MP40 })
        assertTrue(loot.any { it.kind == LootKind.WEAPON_AK47 })
        assertTrue(loot.any { it.kind == LootKind.WEAPON_AWM })
    }
}
