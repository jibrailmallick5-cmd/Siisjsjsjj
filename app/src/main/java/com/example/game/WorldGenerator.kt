package com.example.game

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object WorldGenerator {

    fun biomeAt(x: Float, y: Float): BiomeType {
        if (y < 1050f + sin(x * 0.004f) * 70f) return BiomeType.MILITARY_PEAK
        if (x < 1120f + cos(y * 0.004f) * 70f) return BiomeType.PINE_FOREST
        if (x > 2100f + sin(y * 0.004f) * 70f) return BiomeType.DESERT_OUTPOST
        return BiomeType.CLOCKTOWER_VALLEY
    }

    fun initialMissions(): List<MissionDefinition> = listOf(
        MissionDefinition(
            id = "m1_patrol",
            titleBn = "মিশন ১: স্কোয়াড এলিমিনেশন",
            titleEn = "Mission 1: Squad Sweep",
            descBn = "ক্লকটাওয়ার এরেনার আশেপাশে থাকা ৫ জন শত্রু সৈন্যকে এলিমিনেট করুন।",
            descEn = "Eliminate 5 hostile Raiders patrolling Clocktower Arena.",
            category = MissionCategory.ELIMINATE,
            targetCount = 5,
            rewardGold = 240,
            rewardGems = 25,
            rewardXp = 160,
            waypointX = 1450f,
            waypointY = 1750f
        ),
        MissionDefinition(
            id = "m2_airdrops",
            titleBn = "মিশন ২: এয়ারড্রপ ক্যাপচার",
            titleEn = "Mission 2: Supply Airdrops",
            descBn = "ওপেন ওয়ার্ল্ডের বিভিন্ন অঞ্চল থেকে ৪টি লাল এয়ারড্রপ সাপ্লাই লুট করুন।",
            descEn = "Secure 4 tactical Supply Airdrop crates across the map.",
            category = MissionCategory.AIRDROP,
            targetCount = 4,
            rewardGold = 320,
            rewardGems = 40,
            rewardXp = 220,
            waypointX = 2460f,
            waypointY = 1920f
        ),
        MissionDefinition(
            id = "m3_intel",
            titleBn = "মিশন ৩: রাডার টার্মিনাল হ্যাক",
            titleEn = "Mission 3: Signal Blackout",
            descBn = "পাইন জঙ্গল ও আউটপোস্টের ৩টি শত্রু ইন্টেল টার্মিনাল নিষ্ক্রিয় করুন।",
            descEn = "Disable 3 enemy Intel Terminals in Pine Forest & Outpost.",
            category = MissionCategory.INTEL,
            targetCount = 3,
            rewardGold = 340,
            rewardGems = 45,
            rewardXp = 240,
            waypointX = 720f,
            waypointY = 1750f
        ),
        MissionDefinition(
            id = "m4_commander",
            titleBn = "মিশন ৪: পিক কমান্ডার টেকডাউন",
            titleEn = "Mission 4: Peak Takedown",
            descBn = "উত্তরের মিলিটারি পিক ঘাঁটিতে প্রবেশ করে ভারী আর্মার্ড কমান্ডার বসকে পরাজিত করুন।",
            descEn = "Assault Military Peak in the north and defeat the Peak Commander Boss.",
            category = MissionCategory.BOSS,
            targetCount = 1,
            rewardGold = 600,
            rewardGems = 90,
            rewardXp = 450,
            waypointX = 1600f,
            waypointY = 520f
        ),
        MissionDefinition(
            id = "m5_recon",
            titleBn = "মিশন ৫: রিকন এক্সট্র্যাকশন",
            titleEn = "Mission 5: Sector Recon",
            descBn = "ম্যাপের ৪টি সেক্টরের সব হেলিপ্যাড আনলক করে ফাস্ট ট্র্যাভেল চালু করুন।",
            descEn = "Unlock all 4 Extraction Helipads across the battleground.",
            category = MissionCategory.EXPLORE,
            targetCount = 4,
            rewardGold = 380,
            rewardGems = 55,
            rewardXp = 280,
            waypointX = 2520f,
            waypointY = 2120f
        )
    )

    fun clashAllyCage(): SpawnCageBounds = SpawnCageBounds(
        minX = 1465f,
        maxX = 1735f,
        minY = 2260f,
        maxY = 2440f,
        accentColor = Color(0xFF00E5FF)
    )

    fun clashEnemyCage(): SpawnCageBounds = SpawnCageBounds(
        minX = 1465f,
        maxX = 1735f,
        minY = 1160f,
        maxY = 1340f,
        accentColor = Color(0xFFFF4D6D)
    )

    fun generateClashSquadProps(): List<WorldProp> {
        val props = mutableListOf<WorldProp>()

        // Realistic Big Houses (West Mansion, East Barracks, North-West Villa, South-East Depot)
        props += WorldProp("cs_house_west", PropKind.BUILDING_HOUSE, 1280f, 1800f, 95f, 80f, 122f, Color(0xFFBFA888), Color(0xFF8C775C))
        props += WorldProp("cs_house_east", PropKind.BUILDING_HOUSE, 1920f, 1800f, 95f, 80f, 122f, Color(0xFFA89F91), Color(0xFF6B655D))
        props += WorldProp("cs_house_nw", PropKind.BUILDING_HOUSE, 1210f, 1460f, 82f, 65f, 105f, Color(0xFF8E9AAF), Color(0xFF5C677D))
        props += WorldProp("cs_house_se", PropKind.BUILDING_HOUSE, 1990f, 2120f, 82f, 65f, 105f, Color(0xFFC29B73), Color(0xFF8C6B4B))

        // Watchtowers & Containers in Mid Courtyard
        props += WorldProp("cs_tower_w", PropKind.WATCHTOWER, 1435f, 1610f, 26f, 26f, 138f, Color(0xFF52525B), Color(0xFF3F3F46))
        props += WorldProp("cs_tower_e", PropKind.WATCHTOWER, 1765f, 1990f, 26f, 26f, 138f, Color(0xFF52525B), Color(0xFF3F3F46))
        props += WorldProp("cs_cont_mid_1", PropKind.SHIPPING_CONTAINER, 1525f, 1790f, 52f, 24f, 46f, Color(0xFF1E40AF), Color(0xFF1D4ED8))
        props += WorldProp("cs_cont_mid_2", PropKind.SHIPPING_CONTAINER, 1685f, 1825f, 52f, 24f, 46f, Color(0xFF991B1B), Color(0xFFB91C1C))
        props += WorldProp("cs_cont_north", PropKind.SHIPPING_CONTAINER, 1790f, 1520f, 24f, 52f, 46f, Color(0xFF3F6212), Color(0xFF4D7C0F))
        props += WorldProp("cs_cont_south", PropKind.SHIPPING_CONTAINER, 1405f, 2080f, 24f, 52f, 46f, Color(0xFFB45309), Color(0xFF92400E))

        // Tactical Concrete Barriers, Crates & Barrels for Peek-and-Shoot Cover
        props += WorldProp("cs_bar_s1", PropKind.CONCRETE_BARRIER, 1600f, 2145f, 42f, 10f, 22f, Color(0xFF94A3B8), Color(0xFF64748B))
        props += WorldProp("cs_bar_n1", PropKind.CONCRETE_BARRIER, 1600f, 1455f, 42f, 10f, 22f, Color(0xFF94A3B8), Color(0xFF64748B))
        props += WorldProp("cs_bar_w1", PropKind.CONCRETE_BARRIER, 1435f, 1800f, 12f, 38f, 22f, Color(0xFF94A3B8), Color(0xFF64748B))
        props += WorldProp("cs_bar_e1", PropKind.CONCRETE_BARRIER, 1765f, 1800f, 12f, 38f, 22f, Color(0xFF94A3B8), Color(0xFF64748B))
        props += WorldProp("cs_crate_c1", PropKind.WOODEN_CRATE, 1600f, 1705f, 18f, 18f, 24f, Color(0xFF92400E), Color(0xFF78350F))
        props += WorldProp("cs_crate_c2", PropKind.WOODEN_CRATE, 1600f, 1900f, 18f, 18f, 24f, Color(0xFF92400E), Color(0xFF78350F))
        props += WorldProp("cs_barrel_1", PropKind.FUEL_BARREL, 1495f, 1690f, 12f, 12f, 26f, Color(0xFFDC2626), Color(0xFF991B1B))
        props += WorldProp("cs_barrel_2", PropKind.FUEL_BARREL, 1710f, 1915f, 12f, 12f, 26f, Color(0xFFDC2626), Color(0xFF991B1B))

        // Surrounding Realistic Trees & Boulders outside the main courtyard
        val rng = Random(4419)
        for (i in 0 until 42) {
            val angle = (i / 42f) * 6.28318f
            val rad = 520f + rng.nextFloat() * 260f
            val tx = 1600f + kotlin.math.cos(angle) * rad
            val ty = 1800f + kotlin.math.sin(angle) * rad
            if (abs(tx - 1600f) < 160f && (ty in 1140f..1360f || ty in 2240f..2460f)) continue
            props += if (i % 3 == 0) {
                WorldProp("cs_rock_$i", PropKind.ROCK_BOULDER, tx, ty, 28f, 24f, 36f, Color(0xFF64748B), Color(0xFF475569))
            } else {
                WorldProp("cs_tree_$i", PropKind.PINE_TREE, tx, ty, 22f, 22f, 125f, Color(0xFF1B4332), Color(0xFF2D6A4F), blocksBullets = false)
            }
        }
        return props
    }

    fun generateClashSquadAllies(): List<AllyState> = listOf(
        AllyState(
            id = "ally_1",
            name = "Viper-02",
            x = 1515f,
            y = 2350f,
            hp = STANDARD_MAX_HP,
            maxHp = STANDARD_MAX_HP,
            vestColor = Color(0xFF0E7490),
            helmetColor = Color(0xFF00E5FF)
        ),
        AllyState(
            id = "ally_2",
            name = "Kira-03",
            x = 1685f,
            y = 2350f,
            hp = STANDARD_MAX_HP,
            maxHp = STANDARD_MAX_HP,
            vestColor = Color(0xFF1D4ED8),
            helmetColor = Color(0xFF38BDF8)
        ),
        AllyState(
            id = "ally_3",
            name = "Titan-04",
            x = 1600f,
            y = 2395f,
            hp = STANDARD_MAX_HP,
            maxHp = STANDARD_MAX_HP,
            vestColor = Color(0xFF0F766E),
            helmetColor = Color(0xFF34D399)
        )
    )

    fun generateClashSquadEnemies(): List<EnemyState> = listOf(
        EnemyState(
            id = "cs_enemy_1",
            kind = EnemyKind.CLASH_OPERATIVE,
            name = "Red-Alpha",
            homeX = 1510f,
            homeY = 1250f,
            x = 1510f,
            y = 1250f,
            yaw = 1.5708f,
            hp = STANDARD_MAX_HP,
            maxHp = STANDARD_MAX_HP,
            fireCooldown = 0.7f
        ),
        EnemyState(
            id = "cs_enemy_2",
            kind = EnemyKind.CLASH_OPERATIVE,
            name = "Red-Bravo",
            homeX = 1570f,
            homeY = 1220f,
            x = 1570f,
            y = 1220f,
            yaw = 1.5708f,
            hp = STANDARD_MAX_HP,
            maxHp = STANDARD_MAX_HP,
            fireCooldown = 0.9f
        ),
        EnemyState(
            id = "cs_enemy_3",
            kind = EnemyKind.CLASH_OPERATIVE,
            name = "Red-Charlie",
            homeX = 1635f,
            homeY = 1220f,
            x = 1635f,
            y = 1220f,
            yaw = 1.5708f,
            hp = STANDARD_MAX_HP,
            maxHp = STANDARD_MAX_HP,
            fireCooldown = 1.0f
        ),
        EnemyState(
            id = "cs_enemy_4",
            kind = EnemyKind.CLASH_OPERATIVE,
            name = "Red-Delta",
            homeX = 1690f,
            homeY = 1250f,
            x = 1690f,
            y = 1250f,
            yaw = 1.5708f,
            hp = STANDARD_MAX_HP,
            maxHp = STANDARD_MAX_HP,
            fireCooldown = 0.8f
        )
    )

    fun generateClashSquadLoot(): List<GroundLootItem> = listOf(
        // Inside Ally Spawn Cage so player can pick up preferred guns during the 15s wait!
        GroundLootItem("loot_spawn_mp40", LootKind.WEAPON_MP40, 1525f, 2305f, "MP40 কোবরা SMG", "MP40 Cobra SMG", Color(0xFFFFB703)),
        GroundLootItem("loot_spawn_ak47", LootKind.WEAPON_AK47, 1600f, 2295f, "AK-47 গোল্ডলাইন", "AK-47 Goldline", Color(0xFFF59E0B)),
        GroundLootItem("loot_spawn_awm", LootKind.WEAPON_AWM, 1675f, 2305f, "AWM স্নাইপার", "AWM Sniper", Color(0xFF34D399)),
        // Ground Gun & Gear Loot around the Big Houses & Courtyard
        GroundLootItem("loot_west_m4", LootKind.WEAPON_M4A1, 1410f, 1800f, "M4A1 ট্যাকটিক্যাল", "M4A1 Tactical", Color(0xFF38BDF8)),
        GroundLootItem("loot_east_ak", LootKind.WEAPON_AK47, 1790f, 1800f, "AK-47 গোল্ডলাইন", "AK-47 Goldline", Color(0xFFF59E0B)),
        GroundLootItem("loot_mid_awm", LootKind.WEAPON_AWM, 1600f, 1800f, "AWM স্নাইপার", "AWM Sniper", Color(0xFF34D399)),
        GroundLootItem("loot_vest_1", LootKind.ARMOR_VEST, 1435f, 1960f, "লেভেল-৩ আর্মর ভেস্ট", "Level-3 Vest", Color(0xFF00E5FF)),
        GroundLootItem("loot_vest_2", LootKind.ARMOR_VEST, 1765f, 1640f, "লেভেল-৩ আর্মর ভেস্ট", "Level-3 Vest", Color(0xFF00E5FF)),
        GroundLootItem("loot_med_1", LootKind.MEDKIT_PACK, 1510f, 2060f, "মেডকিট প্যাক (x2)", "Medkit Pack (x2)", Color(0xFF10B981)),
        GroundLootItem("loot_med_2", LootKind.MEDKIT_PACK, 1695f, 1540f, "মেডকিট প্যাক (x2)", "Medkit Pack (x2)", Color(0xFF10B981)),
        GroundLootItem("loot_ammo_1", LootKind.AMMO_CRATE, 1405f, 1680f, "অ্যামো ক্রেট (+60)", "Ammo Crate (+60)", Color(0xFFFDE047)),
        GroundLootItem("loot_ammo_2", LootKind.AMMO_CRATE, 1795f, 1920f, "অ্যামো ক্রেট (+60)", "Ammo Crate (+60)", Color(0xFFFDE047))
    )

    fun generateOpenWorldLoot(): List<GroundLootItem> = listOf(
        GroundLootItem("ow_loot_ak", LootKind.WEAPON_AK47, 1660f, 2090f, "AK-47 গোল্ডলাইন", "AK-47 Goldline", Color(0xFFF59E0B)),
        GroundLootItem("ow_loot_mp40", LootKind.WEAPON_MP40, 1540f, 2090f, "MP40 কোবরা SMG", "MP40 Cobra SMG", Color(0xFFFFB703)),
        GroundLootItem("ow_loot_awm", LootKind.WEAPON_AWM, 1420f, 1920f, "AWM স্নাইপার", "AWM Sniper", Color(0xFF34D399)),
        GroundLootItem("ow_loot_vest", LootKind.ARMOR_VEST, 1740f, 1980f, "লেভেল-৩ আর্মর ভেস্ট", "Level-3 Vest", Color(0xFF00E5FF)),
        GroundLootItem("ow_loot_med", LootKind.MEDKIT_PACK, 1600f, 1850f, "মেডকিট প্যাক (x2)", "Medkit Pack (x2)", Color(0xFF10B981))
    )

    fun generateProps(): List<WorldProp> {
        val props = mutableListOf<WorldProp>()

        // Clocktower Valley Big Houses & Compound
        props += WorldProp("ct_hq_1", PropKind.BUILDING_HOUSE, 1340f, 1980f, 88f, 70f, 118f, Color(0xFFBFA888), Color(0xFF8C775C))
        props += WorldProp("ct_hq_2", PropKind.BUILDING_HOUSE, 1860f, 1960f, 88f, 70f, 114f, Color(0xFFA89F91), Color(0xFF6B655D))
        props += WorldProp("ct_tower_1", PropKind.WATCHTOWER, 1420f, 2260f, 26f, 26f, 138f, Color(0xFF52525B), Color(0xFF3F3F46))
        props += WorldProp("ct_cont_1", PropKind.SHIPPING_CONTAINER, 1740f, 2140f, 52f, 24f, 44f, Color(0xFF1E40AF), Color(0xFF1D4ED8))
        props += WorldProp("ct_cont_2", PropKind.SHIPPING_CONTAINER, 1460f, 1820f, 24f, 52f, 44f, Color(0xFF991B1B), Color(0xFFB91C1C))
        props += WorldProp("ct_bar_1", PropKind.CONCRETE_BARRIER, 1600f, 2060f, 36f, 10f, 20f, Color(0xFF94A3B8), Color(0xFF64748B))
        props += WorldProp("ct_bar_2", PropKind.CONCRETE_BARRIER, 1520f, 1940f, 32f, 10f, 20f, Color(0xFF94A3B8), Color(0xFF64748B))
        props += WorldProp("ct_crate_1", PropKind.WOODEN_CRATE, 1675f, 2240f, 18f, 18f, 24f, Color(0xFF92400E), Color(0xFF78350F))

        // Military Peak Citadel (1600, 520)
        props += WorldProp("pk_bunker_1", PropKind.BUILDING_HOUSE, 1380f, 540f, 85f, 65f, 110f, Color(0xFF64748B), Color(0xFF475569))
        props += WorldProp("pk_bunker_2", PropKind.BUILDING_HOUSE, 1820f, 540f, 85f, 65f, 110f, Color(0xFF64748B), Color(0xFF475569))
        props += WorldProp("pk_tower_1", PropKind.WATCHTOWER, 1460f, 740f, 26f, 26f, 145f, Color(0xFF3F3F46), Color(0xFF27272A))
        props += WorldProp("pk_tower_2", PropKind.WATCHTOWER, 1740f, 740f, 26f, 26f, 145f, Color(0xFF3F3F46), Color(0xFF27272A))
        props += WorldProp("pk_cont_1", PropKind.SHIPPING_CONTAINER, 1600f, 680f, 54f, 24f, 44f, Color(0xFF3F6212), Color(0xFF4D7C0F))
        props += WorldProp("pk_bar_1", PropKind.CONCRETE_BARRIER, 1510f, 610f, 34f, 10f, 20f, Color(0xFF64748B), Color(0xFF475569))
        props += WorldProp("pk_bar_2", PropKind.CONCRETE_BARRIER, 1690f, 610f, 34f, 10f, 20f, Color(0xFF64748B), Color(0xFF475569))

        // Desert Outpost Compound (2520, 2020)
        props += WorldProp("ds_bldg_1", PropKind.BUILDING_HOUSE, 2440f, 1880f, 82f, 65f, 100f, Color(0xFFD6B98C), Color(0xFFA68B64))
        props += WorldProp("ds_cont_1", PropKind.SHIPPING_CONTAINER, 2620f, 2040f, 52f, 24f, 44f, Color(0xFFB45309), Color(0xFFD97706))
        props += WorldProp("ds_cont_2", PropKind.SHIPPING_CONTAINER, 2360f, 2120f, 24f, 52f, 44f, Color(0xFF155E75), Color(0xFF0E7490))

        // Pine Forest Outpost (740, 1860)
        props += WorldProp("pf_cabin_1", PropKind.BUILDING_HOUSE, 760f, 1860f, 78f, 60f, 96f, Color(0xFF7D5A44), Color(0xFF5C4033))
        props += WorldProp("pf_tower_1", PropKind.WATCHTOWER, 900f, 1980f, 26f, 26f, 130f, Color(0xFF4B3621), Color(0xFF3B2A1A))

        val rng = Random(88026)
        for (i in 0 until 135) {
            val px = rng.nextFloat() * (WORLD_SIZE - 300f) + 150f
            val py = rng.nextFloat() * (WORLD_SIZE - 300f) + 150f

            if (abs(px - 1600f) < 95f || abs(py - 2200f) < 95f) continue
            if (abs(px - 1600f) + abs(py - 520f) < 280f) continue
            if (abs(px - 1600f) + abs(py - 2200f) < 240f) continue

            val biome = biomeAt(px, py)
            val roll = rng.nextFloat()
            val prop = when (biome) {
                BiomeType.CLOCKTOWER_VALLEY -> when {
                    roll < 0.45f -> WorldProp("p_$i", PropKind.PINE_TREE, px, py, 20f, 20f, 105f, Color(0xFF1B4332), Color(0xFF2D6A4F), blocksBullets = false)
                    roll < 0.72f -> WorldProp("p_$i", PropKind.WOODEN_CRATE, px, py, 18f, 18f, 26f, Color(0xFF92400E), Color(0xFF78350F))
                    roll < 0.88f -> WorldProp("p_$i", PropKind.CONCRETE_BARRIER, px, py, 30f, 10f, 20f, Color(0xFF94A3B8), Color(0xFF64748B))
                    else -> WorldProp("p_$i", PropKind.FUEL_BARREL, px, py, 12f, 12f, 26f, Color(0xFFDC2626), Color(0xFF991B1B))
                }
                BiomeType.PINE_FOREST -> when {
                    roll < 0.75f -> WorldProp("p_$i", PropKind.PINE_TREE, px, py, 22f, 22f, 120f + rng.nextFloat() * 35f, Color(0xFF143628), Color(0xFF1F4E3D), blocksBullets = false)
                    roll < 0.90f -> WorldProp("p_$i", PropKind.ROCK_BOULDER, px, py, 26f, 24f, 32f, Color(0xFF64748B), Color(0xFF475569))
                    else -> WorldProp("p_$i", PropKind.WOODEN_CRATE, px, py, 18f, 18f, 26f, Color(0xFF78350F), Color(0xFF5C3A21))
                }
                BiomeType.DESERT_OUTPOST -> when {
                    roll < 0.55f -> WorldProp("p_$i", PropKind.ROCK_BOULDER, px, py, 28f, 26f, 38f, Color(0xFFB08D57), Color(0xFF8C6D3F))
                    roll < 0.82f -> WorldProp("p_$i", PropKind.SHIPPING_CONTAINER, px, py, 48f, 22f, 44f, Color(0xFFB45309), Color(0xFF78350F))
                    else -> WorldProp("p_$i", PropKind.CONCRETE_BARRIER, px, py, 32f, 10f, 20f, Color(0xFFD6B98C), Color(0xFFA68B64))
                }
                BiomeType.MILITARY_PEAK -> when {
                    roll < 0.52f -> WorldProp("p_$i", PropKind.ROCK_BOULDER, px, py, 30f, 26f, 44f, Color(0xFF52525B), Color(0xFF3F3F46))
                    roll < 0.80f -> WorldProp("p_$i", PropKind.CONCRETE_BARRIER, px, py, 34f, 10f, 22f, Color(0xFF64748B), Color(0xFF475569))
                    else -> WorldProp("p_$i", PropKind.FUEL_BARREL, px, py, 12f, 12f, 26f, Color(0xFFB91C1C), Color(0xFF7F1D1D))
                }
            }
            props += prop
        }

        return props
    }

    fun generateInteractables(
        discoveredWaypoints: Set<String>,
        openedAirdrops: Set<String>,
        disabledIntels: Set<String>,
        collectedAmmo: Set<String>
    ): List<WorldInteractable> {
        val list = mutableListOf<WorldInteractable>()

        list += WorldInteractable(
            id = "npc_commander_rex",
            kind = InteractableKind.NPC,
            x = 1545f,
            y = 2160f,
            labelBn = "কমান্ডার রেক্স",
            labelEn = "Commander Rex",
            dialogueBn = "এজেন্ট, স্বাগতম! ডান দিকে ড্র্যাগ করে ৩৬০° এইম ঘুরাও, সেটিংস থেকে সেনসিভিটি ঠিক করো এবং জোনের ভেতরে থেকে লড়াই করো!",
            dialogueEn = "Operative! Drag the right screen to aim 360°, tune Sensitivity in Settings, and stay inside the Safe Zone!"
        )
        list += WorldInteractable(
            id = "npc_armory_kira",
            kind = InteractableKind.NPC,
            x = 1660f,
            y = 2160f,
            labelBn = "ওয়েপন স্পেশালিস্ট কিরা",
            labelEn = "Armory Chief Kira",
            dialogueBn = "ক্যারেক্টার ও অস্ত্র মেনু থেকে তোমার আউটফিট, হেলমেট, ব্যাকপ্যাক এবং M4A1 বা AWM স্নাইপার কাস্টমাইজ করো!",
            dialogueEn = "Open Character & Armory anytime to customize your tactical outfit, helmet, backpack, and switch between M4A1, MP40, AK-47, and AWM!"
        )

        list += WorldInteractable(
            id = "station_med_base",
            kind = InteractableKind.MEDKIT_STATION,
            x = 1600f,
            y = 2280f,
            labelBn = "মেডিকেল ও আর্মর স্টেশন",
            labelEn = "Med & Armor Station"
        )

        list += WorldInteractable(
            id = "wp_clocktower",
            kind = InteractableKind.WAYPOINT,
            x = 1600f,
            y = 2225f,
            labelBn = "ক্লকটাওয়ার হেলিপ্যাড",
            labelEn = "Clocktower Helipad",
            biome = BiomeType.CLOCKTOWER_VALLEY,
            activated = "wp_clocktower" in discoveredWaypoints
        )
        list += WorldInteractable(
            id = "wp_forest",
            kind = InteractableKind.WAYPOINT,
            x = 740f,
            y = 2120f,
            labelBn = "পাইন ফরেস্ট হেলিপ্যাড",
            labelEn = "Pine Forest Helipad",
            biome = BiomeType.PINE_FOREST,
            activated = "wp_forest" in discoveredWaypoints
        )
        list += WorldInteractable(
            id = "wp_desert",
            kind = InteractableKind.WAYPOINT,
            x = 2520f,
            y = 2120f,
            labelBn = "ডেজার্ট আউটপোস্ট হেলিপ্যাড",
            labelEn = "Desert Outpost Helipad",
            biome = BiomeType.DESERT_OUTPOST,
            activated = "wp_desert" in discoveredWaypoints
        )
        list += WorldInteractable(
            id = "wp_peak",
            kind = InteractableKind.WAYPOINT,
            x = 1600f,
            y = 920f,
            labelBn = "মিলিটারি পিক হেলিপ্যাড",
            labelEn = "Military Peak Helipad",
            biome = BiomeType.MILITARY_PEAK,
            activated = "wp_peak" in discoveredWaypoints
        )

        val airdrops = listOf(
            Triple("drop_1", 1920f, 1740f),
            Triple("drop_2", 2460f, 1920f),
            Triple("drop_3", 680f, 2460f),
            Triple("drop_4", 2240f, 760f)
        )
        airdrops.forEachIndexed { idx, (id, ax, ay) ->
            list += WorldInteractable(
                id = id,
                kind = InteractableKind.AIRDROP_CRATE,
                x = ax,
                y = ay,
                labelBn = "সাপ্লাই এয়ারড্রপ #${idx + 1}",
                labelEn = "Supply Airdrop #${idx + 1}",
                activated = id in openedAirdrops
            )
        }

        val intels = listOf(
            Triple("intel_1", 720f, 1750f),
            Triple("intel_2", 540f, 1380f),
            Triple("intel_3", 2560f, 1760f)
        )
        intels.forEachIndexed { idx, (id, ix, iy) ->
            list += WorldInteractable(
                id = id,
                kind = InteractableKind.INTEL_TERMINAL,
                x = ix,
                y = iy,
                labelBn = "ইন্টেল রাডার #${idx + 1}",
                labelEn = "Intel Terminal #${idx + 1}",
                activated = id in disabledIntels
            )
        }

        val boxes = listOf(
            Triple("ammo_1", 1500f, 1980f),
            Triple("ammo_2", 820f, 1920f),
            Triple("ammo_3", 2460f, 2040f),
            Triple("ammo_4", 1600f, 760f)
        )
        boxes.forEachIndexed { idx, (id, bx, by) ->
            list += WorldInteractable(
                id = id,
                kind = InteractableKind.AMMO_BOX,
                x = bx,
                y = by,
                labelBn = "অ্যামো ও মেডকিট বক্স #${idx + 1}",
                labelEn = "Ammo & Medkit Crate #${idx + 1}",
                activated = id in collectedAmmo
            )
        }

        return list
    }

    fun generateEnemies(): List<EnemyState> {
        val list = mutableListOf<EnemyState>()

        val raiders = listOf(
            1450f to 1750f,
            1780f to 1840f,
            1320f to 1580f,
            1920f to 2460f,
            1240f to 2480f,
            1620f to 1460f
        )
        raiders.forEachIndexed { idx, (ex, ey) ->
            list += EnemyState(
                id = "raider_$idx",
                kind = EnemyKind.SCOUT_RAIDER,
                name = "Raider-${idx + 1}",
                homeX = ex,
                homeY = ey,
                x = ex,
                y = ey,
                hp = EnemyKind.SCOUT_RAIDER.maxHp,
                maxHp = EnemyKind.SCOUT_RAIDER.maxHp,
                fireCooldown = 0.6f + (idx % 3) * 0.35f
            )
        }

        val snipers = listOf(
            700f to 1700f,
            580f to 1450f,
            880f to 1620f,
            660f to 2380f
        )
        snipers.forEachIndexed { idx, (ex, ey) ->
            list += EnemyState(
                id = "sniper_$idx",
                kind = EnemyKind.FOREST_SNIPER,
                name = "Sniper-${idx + 1}",
                homeX = ex,
                homeY = ey,
                x = ex,
                y = ey,
                hp = EnemyKind.FOREST_SNIPER.maxHp,
                maxHp = EnemyKind.FOREST_SNIPER.maxHp,
                fireCooldown = 1.1f + (idx % 2) * 0.4f
            )
        }

        val desertTroops = listOf(
            2380f to 1820f,
            2620f to 1960f,
            2480f to 2340f,
            2760f to 2180f
        )
        desertTroops.forEachIndexed { idx, (ex, ey) ->
            list += EnemyState(
                id = "desert_$idx",
                kind = EnemyKind.DESERT_ASSAULT,
                name = "Merc-${idx + 1}",
                homeX = ex,
                homeY = ey,
                x = ex,
                y = ey,
                hp = EnemyKind.DESERT_ASSAULT.maxHp,
                maxHp = EnemyKind.DESERT_ASSAULT.maxHp,
                fireCooldown = 0.9f
            )
        }

        list += EnemyState(
            id = "peak_guard_1",
            kind = EnemyKind.DESERT_ASSAULT,
            name = "Guard-1",
            homeX = 1480f,
            homeY = 660f,
            x = 1480f,
            y = 660f,
            hp = EnemyKind.DESERT_ASSAULT.maxHp,
            maxHp = EnemyKind.DESERT_ASSAULT.maxHp
        )
        list += EnemyState(
            id = "peak_guard_2",
            kind = EnemyKind.DESERT_ASSAULT,
            name = "Guard-2",
            homeX = 1720f,
            homeY = 660f,
            x = 1720f,
            y = 660f,
            hp = EnemyKind.DESERT_ASSAULT.maxHp,
            maxHp = EnemyKind.DESERT_ASSAULT.maxHp
        )
        list += EnemyState(
            id = "boss_peak_commander",
            kind = EnemyKind.PEAK_COMMANDER,
            name = "Peak Commander",
            homeX = 1600f,
            homeY = 520f,
            x = 1600f,
            y = 520f,
            hp = EnemyKind.PEAK_COMMANDER.maxHp,
            maxHp = EnemyKind.PEAK_COMMANDER.maxHp
        )

        return list
    }
}
