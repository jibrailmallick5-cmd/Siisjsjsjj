package com.example.game

import androidx.compose.ui.graphics.Color

const val WORLD_SIZE = 3200f
const val CLASH_ROUND_COUNTDOWN_SEC = 15f
const val STANDARD_MAX_HP = 200f

enum class AppLanguage(val code: String) {
    BN("bn"),
    EN("en");

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.firstOrNull { it.code == code } ?: BN
    }
}

enum class GameMode {
    CLASH_SQUAD,
    OPEN_WORLD
}

enum class BiomeType {
    CLOCKTOWER_VALLEY,
    PINE_FOREST,
    DESERT_OUTPOST,
    MILITARY_PEAK
}

enum class GraphicsQuality(val level: Int, val maxParticles: Int, val drawDistance: Float) {
    STANDARD(0, 32, 920f),
    HIGH(1, 60, 1180f),
    ULTRA(2, 96, 1380f);

    companion object {
        fun fromLevel(level: Int): GraphicsQuality =
            entries.firstOrNull { it.level == level } ?: ULTRA
    }
}

enum class TimeOfDayMode(val index: Int) {
    DYNAMIC(0),
    DAWN(1),
    NIGHT(2);

    companion object {
        fun fromIndex(index: Int): TimeOfDayMode =
            entries.firstOrNull { it.index == index } ?: DYNAMIC
    }
}

data class TacticalOutfit(
    val index: Int,
    val primaryColor: Color,
    val trimColor: Color,
    val pantColor: Color,
    val nameBn: String,
    val nameEn: String
)

data class HeadgearStyle(
    val index: Int,
    val nameBn: String,
    val nameEn: String
)

data class WeaponDef(
    val index: Int,
    val id: String,
    val nameBn: String,
    val nameEn: String,
    val damage: Float,
    val fireInterval: Float,
    val magSize: Int,
    val reloadTime: Float,
    val range: Float,
    val scopeZoomFov: Float,
    val spread: Float,
    val bodyColor: Color,
    val accentColor: Color,
    val isAutomatic: Boolean
)

data class BackpackStyle(
    val index: Int,
    val coreColor: Color,
    val packColor: Color,
    val nameBn: String,
    val nameEn: String
)

object CustomizationCatalog {
    val outfits = listOf(
        TacticalOutfit(0, Color(0xFF283340), Color(0xFF00E5FF), Color(0xFF192028), "ঘোস্ট স্পেক-অপস", "Ghost Spec-Ops"),
        TacticalOutfit(1, Color(0xFF6B1D24), Color(0xFFFFB703), Color(0xFF261C1E), "ক্রিমসন মার্সেনারি", "Crimson Merc"),
        TacticalOutfit(2, Color(0xFF3B4935), Color(0xFFA3B18A), Color(0xFF252D20), "জাঙ্গল কমান্ডো", "Jungle Commando"),
        TacticalOutfit(3, Color(0xFF7D6B4F), Color(0xFFE9C46A), Color(0xFF4A3F2E), "ডেজার্ট রিকন", "Desert Recon"),
        TacticalOutfit(4, Color(0xFF1E2B48), Color(0xFF60A5FA), Color(0xFF111827), "কোবাল্ট ভাইপার", "Cobalt Viper"),
        TacticalOutfit(5, Color(0xFF8C9BAE), Color(0xFF38BDF8), Color(0xFF334155), "আর্কটিক উলফ", "Arctic Wolf")
    )

    val headgears = listOf(
        HeadgearStyle(0, "ট্যাকটিক্যাল হেলমেট", "Ops Helmet"),
        HeadgearStyle(1, "নাইট-ভিশন গগলস", "NVG Helmet"),
        HeadgearStyle(2, "ব্যালিস্টিক মাস্ক", "Ballistic Mask"),
        HeadgearStyle(3, "কমান্ডো বেরেট", "Crimson Beret"),
        HeadgearStyle(4, "সাইবার ভিজর", "Tactical Visor")
    )

    val weapons = listOf(
        WeaponDef(
            index = 0,
            id = "m4a1",
            nameBn = "M4A1 ট্যাকটিক্যাল",
            nameEn = "M4A1 Tactical AR",
            damage = 30f,
            fireInterval = 0.125f,
            magSize = 30,
            reloadTime = 1.35f,
            range = 1150f,
            scopeZoomFov = 780f,
            spread = 0.013f,
            bodyColor = Color(0xFF1E293B),
            accentColor = Color(0xFF38BDF8),
            isAutomatic = true
        ),
        WeaponDef(
            index = 1,
            id = "mp40",
            nameBn = "MP40 কোবরা SMG",
            nameEn = "MP40 Cobra SMG",
            damage = 23f,
            fireInterval = 0.082f,
            magSize = 36,
            reloadTime = 1.15f,
            range = 780f,
            scopeZoomFov = 680f,
            spread = 0.024f,
            bodyColor = Color(0xFF27272A),
            accentColor = Color(0xFFFFB703),
            isAutomatic = true
        ),
        WeaponDef(
            index = 2,
            id = "ak47",
            nameBn = "AK-47 গোল্ডলাইন",
            nameEn = "AK-47 Goldline",
            damage = 40f,
            fireInterval = 0.165f,
            magSize = 28,
            reloadTime = 1.45f,
            range = 1200f,
            scopeZoomFov = 800f,
            spread = 0.018f,
            bodyColor = Color(0xFF451A03),
            accentColor = Color(0xFFF59E0B),
            isAutomatic = true
        ),
        WeaponDef(
            index = 3,
            id = "awm",
            nameBn = "AWM স্নাইপার",
            nameEn = "AWM Sniper",
            damage = 135f,
            fireInterval = 0.85f,
            magSize = 6,
            reloadTime = 1.85f,
            range = 1750f,
            scopeZoomFov = 1220f,
            spread = 0.002f,
            bodyColor = Color(0xFF14532D),
            accentColor = Color(0xFF34D399),
            isAutomatic = false
        )
    )

    val backpacks = listOf(
        BackpackStyle(0, Color(0xFF38BDF8), Color(0xFF1E293B), "অ্যাসল্ট প্যাক", "Assault Pack"),
        BackpackStyle(1, Color(0xFFFF5A1F), Color(0xFF3F2E28), "প্যারাশুট রিগ", "Parachute Rig"),
        BackpackStyle(2, Color(0xFF10B981), Color(0xFF1B3B2B), "গিলি ম্যান্টল", "Ghillie Mantle"),
        BackpackStyle(3, Color(0xFFA855F7), Color(0xFF2E1065), "ড্রোন প্যাক", "Tactical Drone")
    )
}

enum class MissionCategory {
    ELIMINATE,
    AIRDROP,
    INTEL,
    BOSS,
    EXPLORE
}

data class MissionDefinition(
    val id: String,
    val titleBn: String,
    val titleEn: String,
    val descBn: String,
    val descEn: String,
    val category: MissionCategory,
    val targetCount: Int,
    val rewardGold: Int,
    val rewardGems: Int,
    val rewardXp: Int,
    val waypointX: Float,
    val waypointY: Float,
    val progress: Int = 0,
    val completed: Boolean = false,
    val rewardClaimed: Boolean = false
)

enum class EnemyKind(
    val radius: Float,
    val height: Float,
    val maxHp: Float,
    val speed: Float,
    val damage: Float,
    val xpReward: Int,
    val goldReward: Int,
    val vestColor: Color,
    val helmetColor: Color,
    val isBoss: Boolean = false
) {
    SCOUT_RAIDER(18f, 44f, STANDARD_MAX_HP, 128f, 11f, 45, 40, Color(0xFF7F1D1D), Color(0xFF3F1D20)),
    FOREST_SNIPER(18f, 44f, STANDARD_MAX_HP, 112f, 14f, 55, 50, Color(0xFF1E3A2F), Color(0xFF14532D)),
    DESERT_ASSAULT(20f, 46f, STANDARD_MAX_HP, 134f, 15f, 70, 65, Color(0xFF785A3A), Color(0xFF92400E)),
    CLASH_OPERATIVE(18f, 44f, STANDARD_MAX_HP, 138f, 13f, 60, 55, Color(0xFF991B1B), Color(0xFF1F2937)),
    PEAK_COMMANDER(30f, 64f, 520f, 108f, 22f, 300, 250, Color(0xFF7F1D1D), Color(0xFFFFB703), isBoss = true)
}

data class EnemyState(
    val id: String,
    val kind: EnemyKind,
    val name: String = "Hostile",
    val homeX: Float,
    val homeY: Float,
    val x: Float,
    val y: Float,
    val z: Float = 0f,
    val yaw: Float = 0f,
    val walkPhase: Float = 0f,
    val hp: Float = STANDARD_MAX_HP,
    val maxHp: Float = STANDARD_MAX_HP,
    val fireCooldown: Float = 0.8f,
    val muzzleFlashTimer: Float = 0f,
    val hitFlashTimer: Float = 0f,
    val respawnTimer: Float = 0f,
    val strafeDir: Float = 1f,
    val strafeTimer: Float = 1.5f,
    val isAlive: Boolean = true
)

data class AllyState(
    val id: String,
    val name: String,
    val x: Float,
    val y: Float,
    val z: Float = 0f,
    val yaw: Float = -1.5708f,
    val walkPhase: Float = 0f,
    val hp: Float = STANDARD_MAX_HP,
    val maxHp: Float = STANDARD_MAX_HP,
    val vestColor: Color = Color(0xFF0E7490),
    val helmetColor: Color = Color(0xFF00E5FF),
    val fireCooldown: Float = 0.7f,
    val muzzleFlashTimer: Float = 0f,
    val hitFlashTimer: Float = 0f,
    val strafeDir: Float = 1f,
    val strafeTimer: Float = 1.4f,
    val isAlive: Boolean = true
)

enum class LootKind {
    WEAPON_M4A1,
    WEAPON_MP40,
    WEAPON_AK47,
    WEAPON_AWM,
    ARMOR_VEST,
    MEDKIT_PACK,
    AMMO_CRATE
}

data class GroundLootItem(
    val id: String,
    val kind: LootKind,
    val x: Float,
    val y: Float,
    val labelBn: String,
    val labelEn: String,
    val color: Color,
    val pickedUp: Boolean = false
)

data class SpawnCageBounds(
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float,
    val accentColor: Color
)

enum class PropKind {
    BUILDING_HOUSE,
    WATCHTOWER,
    SHIPPING_CONTAINER,
    CONCRETE_BARRIER,
    WOODEN_CRATE,
    PINE_TREE,
    ROCK_BOULDER,
    FUEL_BARREL
}

data class WorldProp(
    val id: String,
    val kind: PropKind,
    val x: Float,
    val y: Float,
    val halfW: Float,
    val halfD: Float,
    val height: Float,
    val primaryColor: Color,
    val secondaryColor: Color,
    val blocksBullets: Boolean = true
) {
    val collisionRadius: Float get() = maxOf(halfW, halfD)
}

enum class InteractableKind {
    NPC,
    WAYPOINT,
    AIRDROP_CRATE,
    INTEL_TERMINAL,
    AMMO_BOX,
    MEDKIT_STATION
}

data class WorldInteractable(
    val id: String,
    val kind: InteractableKind,
    val x: Float,
    val y: Float,
    val labelBn: String,
    val labelEn: String,
    val dialogueBn: String = "",
    val dialogueEn: String = "",
    val biome: BiomeType = BiomeType.CLOCKTOWER_VALLEY,
    val activated: Boolean = false
)

data class BulletTracer3D(
    val id: Long,
    val startX: Float,
    val startY: Float,
    val startZ: Float,
    val endX: Float,
    val endY: Float,
    val endZ: Float,
    val color: Color,
    val life: Float,
    val maxLife: Float
)

data class EnemyProjectile(
    val id: Long,
    val fromAlly: Boolean = false,
    val shooterName: String = "",
    val weaponName: String = "AR",
    val x: Float,
    val y: Float,
    val z: Float,
    val vx: Float,
    val vy: Float,
    val vz: Float,
    val damage: Float,
    val life: Float
)

enum class KillFeedTeam {
    ALLY,
    ENEMY,
    ZONE
}

data class KillFeedEntry(
    val id: Long,
    val killerName: String,
    val killerTeam: KillFeedTeam,
    val victimName: String,
    val victimTeam: KillFeedTeam,
    val weaponName: String,
    val isHeadshot: Boolean = false,
    val isPlayerInvolved: Boolean = false
)

data class Particle3D(
    val x: Float,
    val y: Float,
    val z: Float,
    val vx: Float,
    val vy: Float,
    val vz: Float,
    val life: Float,
    val maxLife: Float,
    val size: Float,
    val color: Color
)

data class FloatingText3D(
    val id: Long,
    val x: Float,
    val y: Float,
    val z: Float,
    val text: String,
    val color: Color,
    val life: Float,
    val maxLife: Float
)

data class ActiveDialogue(
    val speakerBn: String,
    val speakerEn: String,
    val messageBn: String,
    val messageEn: String
)

enum class ActiveOverlay {
    NONE,
    MISSIONS,
    CUSTOMIZE,
    WORLD_MAP,
    SETTINGS
}

data class GameUiState(
    val isLoaded: Boolean = false,
    val showTitleOverlay: Boolean = true,
    val gameMode: GameMode = GameMode.CLASH_SQUAD,
    val activeOverlay: ActiveOverlay = ActiveOverlay.NONE,
    val language: AppLanguage = AppLanguage.BN,
    val playerName: String = "Ghost-01",
    val level: Int = 1,
    val xp: Int = 0,
    val xpToNext: Int = 120,
    val gold: Int = 350,
    val aetherGems: Int = 25,
    val hp: Float = STANDARD_MAX_HP,
    val maxHp: Float = STANDARD_MAX_HP,
    val armor: Float = 100f,
    val maxArmor: Float = 100f,
    val medkits: Int = 3,
    val playerX: Float = 1600f,
    val playerY: Float = 2360f,
    val playerZ: Float = 0f,
    val velZ: Float = 0f,
    val isGrounded: Boolean = true,
    val isPlayerAlive: Boolean = true,
    val cameraYaw: Float = -1.5708f,
    val cameraPitch: Float = -0.025f,
    val generalSensitivity: Float = 1.0f,
    val scopeSensitivity: Float = 0.75f,
    val autoLootEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val soundVolume: Float = 0.85f,
    val isMoving: Boolean = false,
    val isSprinting: Boolean = false,
    val sprintLock: Boolean = false,
    val isScoped: Boolean = false,
    val isFiring: Boolean = false,
    val walkPhase: Float = 0f,
    val recoilPitch: Float = 0f,
    val muzzleFlashTimer: Float = 0f,
    val hitmarkerTimer: Float = 0f,
    val hitmarkerIsHeadshot: Boolean = false,
    val hitFlashTimer: Float = 0f,
    val screenShake: Float = 0f,
    val fireCooldown: Float = 0f,
    val reloadTimer: Float = 0f,
    val magAmmo: IntArray = intArrayOf(30, 36, 28, 6),
    val reserveAmmo: IntArray = intArrayOf(150, 180, 120, 30),
    val cloakIndex: Int = 0,
    val hatIndex: Int = 0,
    val weaponIndex: Int = 0,
    val auraIndex: Int = 0,
    val vitalityLevel: Int = 1,
    val mightLevel: Int = 1,
    val agilityLevel: Int = 1,
    val energyLevel: Int = 1,
    val graphicsQuality: GraphicsQuality = GraphicsQuality.ULTRA,
    val timeMode: TimeOfDayMode = TimeOfDayMode.DYNAMIC,
    val timeOfDay: Float = 0.24f,
    val currentBiome: BiomeType = BiomeType.CLOCKTOWER_VALLEY,
    // Clash Squad 4v4 + 15s Spawn Barrier + Shrinking Circular Safe Zone
    val clashRound: Int = 1,
    val allyRoundWins: Int = 0,
    val enemyRoundWins: Int = 0,
    val roundCountdown: Float = CLASH_ROUND_COUNTDOWN_SEC,
    val roundEndBanner: String? = null,
    val matchFinishedWinner: Boolean? = null, // true = Ally team won match, false = Enemy team won
    val allies: List<AllyState> = emptyList(),
    val allyCage: SpawnCageBounds? = null,
    val enemyCage: SpawnCageBounds? = null,
    val zoneCenterX: Float = 1600f,
    val zoneCenterY: Float = 1800f,
    val zoneRadius: Float = 720f,
    val zoneTargetRadius: Float = 720f,
    val isOutsideZone: Boolean = false,
    val zoneTickFlash: Float = 0f,
    val groundLoot: List<GroundLootItem> = emptyList(),
    val nearbyLoot: GroundLootItem? = null,
    // Open World & Missions
    val trackedMissionId: String = "m1_patrol",
    val missions: List<MissionDefinition> = emptyList(),
    val enemies: List<EnemyState> = emptyList(),
    val props: List<WorldProp> = emptyList(),
    val interactables: List<WorldInteractable> = emptyList(),
    val tracers: List<BulletTracer3D> = emptyList(),
    val enemyProjectiles: List<EnemyProjectile> = emptyList(),
    val particles: List<Particle3D> = emptyList(),
    val floatingTexts: List<FloatingText3D> = emptyList(),
    val nearbyInteractable: WorldInteractable? = null,
    val activeDialogue: ActiveDialogue? = null,
    val statusBanner: String? = null,
    val killFeed: List<KillFeedEntry> = emptyList(),
    val enemiesDefeated: Int = 0,
    val headshots: Int = 0
) {
    val activeWeapon: WeaponDef
        get() = CustomizationCatalog.weapons[weaponIndex.coerceIn(0, CustomizationCatalog.weapons.lastIndex)]

    val currentMagAmmo: Int
        get() = magAmmo.getOrElse(weaponIndex) { 0 }

    val currentReserveAmmo: Int
        get() = reserveAmmo.getOrElse(weaponIndex) { 0 }

    val activeMission: MissionDefinition?
        get() = missions.firstOrNull { it.id == trackedMissionId } ?: missions.firstOrNull()

    val effectiveDamageMultiplier: Float
        get() = 1f + (mightLevel - 1) * 0.14f + (level - 1) * 0.04f

    val effectiveWalkSpeed: Float
        get() = 168f + (agilityLevel - 1) * 10f

    val effectiveSprintSpeed: Float
        get() = 290f + (agilityLevel - 1) * 16f

    val allyAliveCount: Int
        get() = (if (isPlayerAlive) 1 else 0) + allies.count { it.isAlive }

    val enemyAliveCount: Int
        get() = enemies.count { it.isAlive }

    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}
