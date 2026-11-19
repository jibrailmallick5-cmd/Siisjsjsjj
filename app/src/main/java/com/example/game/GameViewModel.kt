package com.example.game

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.GameSoundManager
import com.example.audio.NoOpSoundManager
import com.example.data.local.MissionProgressEntity
import com.example.data.local.PlayerSaveEntity
import com.example.data.repository.GameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

class GameViewModel(
    private val repository: GameRepository,
    private val soundManager: GameSoundManager = NoOpSoundManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    @Volatile
    private var liveRenderState: GameUiState = _uiState.value

    fun currentRenderState(): GameUiState = liveRenderState

    private inline fun updateBothStates(transform: (GameUiState) -> GameUiState) {
        val next = transform(liveRenderState)
        liveRenderState = next
        _uiState.value = next
    }

    private inline fun updateRenderOnly(transform: (GameUiState) -> GameUiState) {
        liveRenderState = transform(liveRenderState)
    }

    private var joyDx = 0f
    private var joyDy = 0f
    private var nextEntityId = 1L
    private var saveAccumulator = 0f
    private var hudEmitAccumulator = 0f
    private var statusBannerTimer = 0f
    private var roundTransitionTimer = 0f
    private var lastWalkHalfCycle = 0
    private var wasGrounded = true
    private var zoneAlertCooldown = 0f
    private var wasOutsideZone = false
    private var lastPlayerDamagerName: String? = null
    private var lastPlayerDamagerWeapon: String = "AR"
    private var lastPlayerDamageFromZone: Boolean = false

    private var discoveredWaypoints = mutableSetOf("wp_clocktower")
    private var openedAirdrops = mutableSetOf<String>()
    private var disabledIntels = mutableSetOf<String>()
    private var collectedAmmo = mutableSetOf<String>()
    private var talkedNpcs = mutableSetOf<String>()

    private var activeProps: List<WorldProp> = WorldGenerator.generateClashSquadProps()
    private var drawHandler: (DrawScope.() -> Unit)? = null

    val gameLoop: GameLoop = object : GameLoop(targetFps = 60) {
        override fun onUpdate(deltaTime: Float) {
            tickSimulation(deltaTime)
        }

        override fun onDraw(drawScope: DrawScope) {
            drawHandler?.invoke(drawScope)
        }
    }

    fun attachDrawHandler(handler: (DrawScope.() -> Unit)?) {
        drawHandler = handler
    }

    init {
        viewModelScope.launch {
            val (save, missionRows) = repository.ensureInitialized()
            loadFromEntities(save, missionRows)
            gameLoop.start(viewModelScope)
        }
    }

    private fun loadFromEntities(save: PlayerSaveEntity, missionRows: List<MissionProgressEntity>) {
        discoveredWaypoints = save.discoveredWaypointsCsv.split(",").filter { it.isNotBlank() }.toMutableSet()
        if (discoveredWaypoints.isEmpty()) discoveredWaypoints.add("wp_clocktower")
        openedAirdrops = save.openedChestsCsv.split(",").filter { it.isNotBlank() }.toMutableSet()
        disabledIntels = save.activatedRunesCsv.split(",").filter { it.isNotBlank() }.toMutableSet()
        collectedAmmo = save.collectedShardsCsv.split(",").filter { it.isNotBlank() }.toMutableSet()
        talkedNpcs = save.talkedNpcsCsv.split(",").filter { it.isNotBlank() }.toMutableSet()

        val rowMap = missionRows.associateBy { it.missionId }
        val missions = WorldGenerator.initialMissions().map { def ->
            val row = rowMap[def.id]
            val prog = when (def.id) {
                "m2_airdrops" -> maxOf(row?.progress ?: 0, openedAirdrops.size)
                "m3_intel" -> maxOf(row?.progress ?: 0, disabledIntels.size)
                "m5_recon" -> maxOf(row?.progress ?: 1, discoveredWaypoints.size)
                else -> row?.progress ?: 0
            }.coerceAtMost(def.targetCount)
            def.copy(
                progress = prog,
                completed = (row?.completed == true) || prog >= def.targetCount,
                rewardClaimed = row?.rewardClaimed == true
            )
        }

        val maxHp = computeMaxHp(save.level, save.vitalityLevel)
        val maxArmor = computeMaxArmor(save.vitalityLevel)
        activeProps = WorldGenerator.generateClashSquadProps()

        updateBothStates {
            it.copy(
                isLoaded = true,
                showTitleOverlay = true,
                gameMode = GameMode.CLASH_SQUAD,
                language = AppLanguage.fromCode(save.language),
                playerName = save.playerName,
                level = save.level,
                xp = save.xp,
                xpToNext = xpRequiredFor(save.level),
                gold = save.gold,
                aetherGems = save.aetherGems,
                hp = maxHp,
                maxHp = maxHp,
                armor = maxArmor,
                maxArmor = maxArmor,
                medkits = save.medkits.coerceIn(2, 8),
                playerX = 1600f,
                playerY = 2360f,
                generalSensitivity = save.generalSensitivity.coerceIn(0.25f, 3.0f),
                scopeSensitivity = save.scopeSensitivity.coerceIn(0.20f, 2.50f),
                autoLootEnabled = save.autoLootEnabled,
                soundEnabled = save.soundEnabled,
                soundVolume = save.soundVolume.coerceIn(0f, 1f),
                cloakIndex = save.cloakIndex.coerceIn(0, CustomizationCatalog.outfits.lastIndex),
                hatIndex = save.hatIndex.coerceIn(0, CustomizationCatalog.headgears.lastIndex),
                weaponIndex = save.weaponIndex.coerceIn(0, CustomizationCatalog.weapons.lastIndex),
                auraIndex = save.auraIndex.coerceIn(0, CustomizationCatalog.backpacks.lastIndex),
                vitalityLevel = save.vitalityLevel.coerceIn(1, 10),
                mightLevel = save.mightLevel.coerceIn(1, 10),
                agilityLevel = save.agilityLevel.coerceIn(1, 10),
                energyLevel = save.energyLevel.coerceIn(1, 10),
                graphicsQuality = GraphicsQuality.fromLevel(save.graphicsQuality),
                timeMode = TimeOfDayMode.fromIndex(save.timeMode),
                currentBiome = BiomeType.CLOCKTOWER_VALLEY,
                trackedMissionId = save.trackedMissionId,
                missions = missions,
                props = activeProps,
                allies = WorldGenerator.generateClashSquadAllies(),
                enemies = WorldGenerator.generateClashSquadEnemies(),
                groundLoot = WorldGenerator.generateClashSquadLoot(),
                allyCage = WorldGenerator.clashAllyCage(),
                enemyCage = WorldGenerator.clashEnemyCage(),
                roundCountdown = CLASH_ROUND_COUNTDOWN_SEC,
                interactables = emptyList(),
                killFeed = emptyList(),
                enemiesDefeated = save.enemiesDefeated,
                headshots = save.headshots
            )
        }
        soundManager.setSoundEnabled(save.soundEnabled)
        soundManager.setMasterVolume(save.soundVolume.coerceIn(0f, 1f))
    }

    fun selectGameMode(mode: GameMode) {
        updateBothStates { it.copy(gameMode = mode) }
    }

    fun startMatch(mode: GameMode = liveRenderState.gameMode) {
        roundTransitionTimer = 0f
        if (mode == GameMode.CLASH_SQUAD) {
            setupClashSquadMatch(round = 1, allyWins = 0, enemyWins = 0)
        } else {
            setupOpenWorldMode()
        }
    }

    fun startOrContinueGame() {
        startMatch(liveRenderState.gameMode)
    }

    fun returnToLobby() {
        joyDx = 0f
        joyDy = 0f
        updateBothStates {
            it.copy(
                showTitleOverlay = true,
                activeOverlay = ActiveOverlay.NONE,
                isFiring = false,
                isScoped = false,
                sprintLock = false,
                matchFinishedWinner = null,
                roundEndBanner = null,
                killFeed = emptyList()
            )
        }
        persistCurrentState()
    }

    private fun setupClashSquadMatch(round: Int, allyWins: Int, enemyWins: Int) {
        activeProps = WorldGenerator.generateClashSquadProps()
        val s = liveRenderState
        val maxHp = computeMaxHp(s.level, s.vitalityLevel)
        val maxArmor = computeMaxArmor(s.vitalityLevel)
        val msg = if (s.language == AppLanguage.BN) {
            "রাউন্ড $round — ৪ দিক ঘেরা! ১৫ সেকেন্ড পর যুদ্ধ শুরু"
        } else {
            "ROUND $round — SPAWN BARRIER LOCKED (15s)"
        }
        statusBannerTimer = 3.2f
        lastWalkHalfCycle = 0
        wasGrounded = true
        zoneAlertCooldown = 0f
        wasOutsideZone = false
        lastPlayerDamagerName = null
        lastPlayerDamagerWeapon = "AR"
        lastPlayerDamageFromZone = false
        hudEmitAccumulator = 0f
        updateBothStates {
            it.copy(
                showTitleOverlay = false,
                activeOverlay = ActiveOverlay.NONE,
                gameMode = GameMode.CLASH_SQUAD,
                clashRound = round,
                allyRoundWins = allyWins,
                enemyRoundWins = enemyWins,
                roundCountdown = CLASH_ROUND_COUNTDOWN_SEC,
                roundEndBanner = null,
                matchFinishedWinner = null,
                playerX = 1600f,
                playerY = 2360f,
                playerZ = 0f,
                velZ = 0f,
                isGrounded = true,
                isPlayerAlive = true,
                cameraYaw = -1.5708f,
                cameraPitch = -0.025f,
                recoilPitch = 0f,
                hp = maxHp,
                maxHp = maxHp,
                armor = maxArmor,
                maxArmor = maxArmor,
                medkits = maxOf(it.medkits, 3),
                magAmmo = intArrayOf(30, 36, 28, 6),
                reserveAmmo = intArrayOf(150, 180, 120, 30),
                isFiring = false,
                isScoped = false,
                sprintLock = false,
                props = activeProps,
                allies = WorldGenerator.generateClashSquadAllies(),
                enemies = WorldGenerator.generateClashSquadEnemies(),
                groundLoot = WorldGenerator.generateClashSquadLoot(),
                interactables = emptyList(),
                allyCage = WorldGenerator.clashAllyCage(),
                enemyCage = WorldGenerator.clashEnemyCage(),
                zoneCenterX = 1600f,
                zoneCenterY = 1800f,
                zoneRadius = 680f,
                zoneTargetRadius = 175f,
                isOutsideZone = false,
                zoneTickFlash = 0f,
                tracers = emptyList(),
                enemyProjectiles = emptyList(),
                particles = emptyList(),
                floatingTexts = emptyList(),
                killFeed = if (round == 1) emptyList() else it.killFeed,
                nearbyLoot = null,
                nearbyInteractable = null,
                statusBanner = msg
            )
        }
    }

    private fun setupOpenWorldMode() {
        activeProps = WorldGenerator.generateProps()
        lastWalkHalfCycle = 0
        wasGrounded = true
        zoneAlertCooldown = 0f
        wasOutsideZone = false
        lastPlayerDamagerName = null
        lastPlayerDamagerWeapon = "AR"
        lastPlayerDamageFromZone = false
        hudEmitAccumulator = 0f
        val s = liveRenderState
        val maxHp = computeMaxHp(s.level, s.vitalityLevel)
        val maxArmor = computeMaxArmor(s.vitalityLevel)
        updateBothStates {
            it.copy(
                showTitleOverlay = false,
                activeOverlay = ActiveOverlay.NONE,
                gameMode = GameMode.OPEN_WORLD,
                roundCountdown = 0f,
                roundEndBanner = null,
                matchFinishedWinner = null,
                playerX = 1600f,
                playerY = 2200f,
                playerZ = 0f,
                velZ = 0f,
                isGrounded = true,
                isPlayerAlive = true,
                cameraYaw = -1.5708f,
                cameraPitch = -0.025f,
                recoilPitch = 0f,
                hp = maxHp,
                maxHp = maxHp,
                armor = maxArmor,
                maxArmor = maxArmor,
                medkits = maxOf(it.medkits, 3),
                props = activeProps,
                allies = emptyList(),
                enemies = WorldGenerator.generateEnemies(),
                groundLoot = WorldGenerator.generateOpenWorldLoot(),
                interactables = WorldGenerator.generateInteractables(
                    discoveredWaypoints, openedAirdrops, disabledIntels, collectedAmmo
                ),
                allyCage = null,
                enemyCage = null,
                zoneCenterX = 1600f,
                zoneCenterY = 1650f,
                zoneRadius = 1480f,
                zoneTargetRadius = 920f,
                isOutsideZone = false,
                zoneTickFlash = 0f,
                tracers = emptyList(),
                enemyProjectiles = emptyList(),
                particles = emptyList(),
                floatingTexts = emptyList(),
                killFeed = emptyList(),
                nearbyLoot = null,
                statusBanner = if (it.language == AppLanguage.BN) "ওপেন ওয়ার্ল্ড ব্যাটলগ্রাউন্ড!" else "Open World Deployed!"
            )
        }
        statusBannerTimer = 2.5f
    }

    private fun tickSimulation(dt: Float) {
        val current = liveRenderState
        if (!current.isLoaded) return

        val nextTimeOfDay = when (current.timeMode) {
            TimeOfDayMode.DYNAMIC -> (current.timeOfDay + dt * 0.0045f) % 1f
            TimeOfDayMode.DAWN -> 0.24f
            TimeOfDayMode.NIGHT -> 0.85f
        }

        if (current.showTitleOverlay || current.activeOverlay != ActiveOverlay.NONE) {
            if (current.timeMode == TimeOfDayMode.DYNAMIC) {
                updateRenderOnly { it.copy(timeOfDay = nextTimeOfDay) }
            }
            return
        }

        // Handle Clash Squad Round Transition Delay
        if (current.gameMode == GameMode.CLASH_SQUAD && current.roundEndBanner != null) {
            if (current.matchFinishedWinner != null) {
                updateRenderOnly { it.copy(timeOfDay = nextTimeOfDay) }
                return
            }
            roundTransitionTimer -= dt
            if (roundTransitionTimer <= 0f) {
                setupClashSquadMatch(
                    round = current.clashRound + 1,
                    allyWins = current.allyRoundWins,
                    enemyWins = current.enemyRoundWins
                )
            } else {
                updateRenderOnly { it.copy(timeOfDay = nextTimeOfDay) }
            }
            return
        }

        val isClash = current.gameMode == GameMode.CLASH_SQUAD
        var countdown = current.roundCountdown
        var barrierJustDropped = false
        if (isClash && countdown > 0f) {
            countdown = (countdown - dt).coerceAtLeast(0f)
            if (countdown == 0f) {
                barrierJustDropped = true
            }
        }
        val barriersActive = isClash && countdown > 0f

        var px = current.playerX
        var py = current.playerY
        var pz = current.playerZ
        var velZ = current.velZ
        var grounded = current.isGrounded
        var hp = current.hp
        var armor = current.armor
        var playerAlive = current.isPlayerAlive
        var walkPhase = current.walkPhase
        var isMoving = false
        var isSprinting = false

        val fireCd = (current.fireCooldown - dt).coerceAtLeast(0f)
        val muzzleTimer = (current.muzzleFlashTimer - dt).coerceAtLeast(0f)
        val hitmarkerTimer = (current.hitmarkerTimer - dt).coerceAtLeast(0f)
        val hitFlash = (current.hitFlashTimer - dt).coerceAtLeast(0f)
        val recoil = (current.recoilPitch - dt * 0.28f).coerceAtLeast(0f)
        val shake = (current.screenShake - dt * 28f).coerceAtLeast(0f)

        // Reload completion
        var reloadTimer = current.reloadTimer
        var magAmmo = current.magAmmo
        var reserveAmmo = current.reserveAmmo
        if (reloadTimer > 0f) {
            reloadTimer = (reloadTimer - dt).coerceAtLeast(0f)
            if (reloadTimer == 0f) {
                val wIdx = current.weaponIndex
                val maxMag = current.activeWeapon.magSize
                val needed = (maxMag - magAmmo[wIdx]).coerceAtLeast(0)
                val load = minOf(needed, reserveAmmo[wIdx])
                if (load > 0) {
                    magAmmo = magAmmo.copyOf().also { it[wIdx] = it[wIdx] + load }
                    reserveAmmo = reserveAmmo.copyOf().also { it[wIdx] = it[wIdx] - load }
                }
            }
        }

        // Camera-relative movement (Left thumb joystick + Sprint)
        val joyMag = hypot(joyDx, joyDy)
        val forwardInput = if (current.sprintLock && joyMag < 0.1f) 1f else -joyDy
        val strafeInput = if (current.sprintLock && joyMag < 0.1f) 0f else joyDx
        val moveMag = hypot(forwardInput, strafeInput)

        val newParticles = if (current.particles.isEmpty()) {
            mutableListOf()
        } else {
            val list = ArrayList<Particle3D>(current.particles.size)
            for (p in current.particles) {
                val rem = p.life - dt
                if (rem > 0f) {
                    list.add(
                        p.copy(
                            x = p.x + p.vx * dt,
                            y = p.y + p.vy * dt,
                            z = (p.z + p.vz * dt).coerceAtLeast(0f),
                            vz = p.vz - 95f * dt,
                            life = rem
                        )
                    )
                }
            }
            list
        }

        if (playerAlive && moveMag > 0.08f) {
            val wantSprint = current.sprintLock || (joyMag > 0.72f && !current.isScoped)
            isSprinting = wantSprint && !current.isScoped
            val speed = if (isSprinting) current.effectiveSprintSpeed else current.effectiveWalkSpeed
            val yaw = current.cameraYaw
            val fwdX = cos(yaw)
            val fwdY = sin(yaw)
            val rightX = -sin(yaw)
            val rightY = cos(yaw)

            val normFwd = forwardInput / moveMag
            val normStrafe = strafeInput / moveMag
            val moveFactor = moveMag.coerceIn(0.3f, 1f)

            px += (fwdX * normFwd + rightX * normStrafe) * speed * moveFactor * dt
            py += (fwdY * normFwd + rightY * normStrafe) * speed * moveFactor * dt
            isMoving = true
            val nextWalkPhase = (walkPhase + dt * (if (isSprinting) 15f else 9.5f)) % (2f * PI.toFloat())
            val prevHalfCycle = (walkPhase / PI.toFloat()).toInt()
            val nextHalfCycle = (nextWalkPhase / PI.toFloat()).toInt()
            if (grounded && prevHalfCycle != nextHalfCycle) {
                soundManager.playFootstep(isSprinting)
            }
            walkPhase = nextWalkPhase
            lastWalkHalfCycle = nextHalfCycle

            if (grounded && isSprinting && Random.nextFloat() < 0.25f && newParticles.size < current.graphicsQuality.maxParticles) {
                newParticles += Particle3D(
                    x = px + (Random.nextFloat() - 0.5f) * 14f,
                    y = py + (Random.nextFloat() - 0.5f) * 14f,
                    z = 2f,
                    vx = (Random.nextFloat() - 0.5f) * 20f,
                    vy = (Random.nextFloat() - 0.5f) * 20f,
                    vz = 18f,
                    life = 0.35f,
                    maxLife = 0.35f,
                    size = 4.5f,
                    color = Color(0xFFA8A29E)
                )
            }
        }

        // Jump Vertical Physics (Z axis)
        if (!grounded || velZ != 0f) {
            pz += velZ * dt
            velZ -= 420f * dt
            if (pz <= 0f) {
                pz = 0f
                velZ = 0f
                grounded = true
                if (!wasGrounded) {
                    soundManager.playJumpLand()
                }
                if (newParticles.size < current.graphicsQuality.maxParticles) {
                    repeat(6) {
                        val a = it * 1.047f
                        newParticles += Particle3D(
                            x = px, y = py, z = 2f,
                            vx = cos(a) * 48f, vy = sin(a) * 48f, vz = 24f,
                            life = 0.35f, maxLife = 0.35f, size = 5f,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }
        }
        wasGrounded = grounded

        // Clamp to 15-second Spawn Cage Barrier if active, else Arena/World bounds + Props collision
        if (barriersActive) {
            current.allyCage?.let { cage ->
                px = px.coerceIn(cage.minX + 18f, cage.maxX - 18f)
                py = py.coerceIn(cage.minY + 18f, cage.maxY - 18f)
            }
        } else {
            px = px.coerceIn(80f, WORLD_SIZE - 80f)
            py = py.coerceIn(80f, WORLD_SIZE - 80f)
        }

        val resolvedPlayer = resolvePropCollision(px, py, pz, 17f)
        px = resolvedPlayer.first
        py = resolvedPlayer.second

        // Shrinking Circular Safe Zone & Zone Damage
        var zoneRadius = current.zoneRadius
        if (!barriersActive) {
            val shrinkRate = if (isClash) 6.5f else 1.8f
            if (zoneRadius > current.zoneTargetRadius) {
                zoneRadius = (zoneRadius - shrinkRate * dt).coerceAtLeast(current.zoneTargetRadius)
            }
        }

        val playerDistZone = hypot(px - current.zoneCenterX, py - current.zoneCenterY)
        val outsideZone = playerAlive && playerDistZone > zoneRadius
        var zoneTickFlash = (current.zoneTickFlash - dt).coerceAtLeast(0f)
        var playerKilledByZone = false
        if (outsideZone) {
            val zoneDps = if (isClash) 11f else 7f
            hp = (hp - zoneDps * dt).coerceAtLeast(0f)
            lastPlayerDamageFromZone = true
            if (hp <= 0f) playerKilledByZone = true
            zoneTickFlash = (zoneTickFlash + dt * 2.5f).coerceAtMost(1f)
            if (!wasOutsideZone) {
                soundManager.playZoneDamageAlert(isInitialWarning = true)
                zoneAlertCooldown = 1.0f
            } else {
                zoneAlertCooldown -= dt
                if (zoneAlertCooldown <= 0f) {
                    soundManager.playZoneDamageAlert(isInitialWarning = false)
                    zoneAlertCooldown = 1.05f
                }
            }
        } else {
            zoneAlertCooldown = 0f
        }
        wasOutsideZone = outsideZone

        // Base Med Station passive healing in Open World
        if (!isClash && playerAlive && hypot(px - 1600f, py - 2280f) < 95f && hp < current.maxHp) {
            hp = (hp + 20f * dt).coerceAtMost(current.maxHp)
        }

        // Update tracers & floating texts without allocating if empty
        val newTracers: List<BulletTracer3D> = if (current.tracers.isEmpty()) {
            emptyList()
        } else {
            val list = ArrayList<BulletTracer3D>(current.tracers.size)
            for (t in current.tracers) {
                val rem = t.life - dt
                if (rem > 0f) list.add(t.copy(life = rem))
            }
            list
        }
        val newFloating: List<FloatingText3D> = if (current.floatingTexts.isEmpty()) {
            emptyList()
        } else {
            val list = ArrayList<FloatingText3D>(current.floatingTexts.size)
            for (f in current.floatingTexts) {
                val rem = f.life - dt
                if (rem > 0f) list.add(f.copy(z = f.z + 24f * dt, life = rem))
            }
            list
        }

        var killFeedList: MutableList<KillFeedEntry>? = null
        fun pushKillFeed(
            killerName: String,
            killerTeam: KillFeedTeam,
            victimName: String,
            victimTeam: KillFeedTeam,
            weaponName: String,
            isHeadshot: Boolean = false,
            isPlayerInvolved: Boolean = false
        ) {
            val list = killFeedList ?: current.killFeed.toMutableList().also { killFeedList = it }
            list.add(
                KillFeedEntry(
                    id = nextEntityId++,
                    killerName = killerName,
                    killerTeam = killerTeam,
                    victimName = victimName,
                    victimTeam = victimTeam,
                    weaponName = weaponName,
                    isHeadshot = isHeadshot,
                    isPlayerInvolved = isPlayerInvolved
                )
            )
            if (list.size > 20) {
                list.removeAt(0)
            }
        }

        val spawnedProjectiles = ArrayList<EnemyProjectile>(8)
        val spawnedLootDrops = ArrayList<GroundLootItem>(2)

        // Update 3 AI Allies (Clash Squad 4v4)
        val finalAllies = ArrayList<AllyState>(current.allies.size)
        for (ally in current.allies) {
            if (!ally.isAlive) {
                finalAllies.add(ally)
                continue
            }
            if (barriersActive) {
                finalAllies.add(
                    ally.copy(
                        hitFlashTimer = (ally.hitFlashTimer - dt).coerceAtLeast(0f),
                        muzzleFlashTimer = (ally.muzzleFlashTimer - dt).coerceAtLeast(0f)
                    )
                )
                continue
            }
            var ax = ally.x
            var ay = ally.y
            var ahp = ally.hp
            var aYaw = ally.yaw
            var aWalk = ally.walkPhase
            var aCd = (ally.fireCooldown - dt).coerceAtLeast(0f)
            var aMuzzle = (ally.muzzleFlashTimer - dt).coerceAtLeast(0f)
            val aHit = (ally.hitFlashTimer - dt).coerceAtLeast(0f)
            var strafeDir = ally.strafeDir
            var strafeTimer = ally.strafeTimer - dt
            if (strafeTimer <= 0f) {
                strafeDir = -strafeDir
                strafeTimer = 1.3f + Random.nextFloat() * 1.4f
            }

            // Zone damage & pull inward
            val distZ = hypot(ax - current.zoneCenterX, ay - current.zoneCenterY)
            if (distZ > zoneRadius) {
                ahp = (ahp - 10f * dt).coerceAtLeast(0f)
                if (ahp <= 0f) {
                    pushKillFeed(
                        killerName = GameStrings.safeZoneName(current.language),
                        killerTeam = KillFeedTeam.ZONE,
                        victimName = ally.name,
                        victimTeam = KillFeedTeam.ALLY,
                        weaponName = "ZONE"
                    )
                }
            }

            var targetEnemy: EnemyState? = null
            var bestEnemyDist = Float.MAX_VALUE
            for (candidate in current.enemies) {
                if (!candidate.isAlive) continue
                val d = hypot(candidate.x - ax, candidate.y - ay)
                if (d < bestEnemyDist) {
                    bestEnemyDist = d
                    targetEnemy = candidate
                }
            }

            if (targetEnemy != null && ahp > 0f) {
                val dx = targetEnemy.x - ax
                val dy = targetEnemy.y - ay
                val dist = hypot(dx, dy).coerceAtLeast(1f)
                aYaw = atan2(dy, dx)
                val dirX = dx / dist
                val dirY = dy / dist
                val perpX = -dirY * strafeDir
                val perpY = dirX * strafeDir

                val approach = if (distZ > zoneRadius * 0.9f) 0.9f else if (dist > 250f) 0.85f else if (dist < 160f) -0.4f else 0.1f
                ax += (dirX * approach + perpX * 0.6f) * 128f * dt
                ay += (dirY * approach + perpY * 0.6f) * 128f * dt
                aWalk = (aWalk + dt * 9f) % (2f * PI.toFloat())

                if (aCd <= 0f && dist < 560f && !isLineBlockedByCover(ax, ay, targetEnemy.x, targetEnemy.y)) {
                    aCd = 0.72f + Random.nextFloat() * 0.35f
                    aMuzzle = 0.08f
                    val shotAngle = aYaw + (Random.nextFloat() - 0.5f) * 0.07f
                    spawnedProjectiles += EnemyProjectile(
                        id = nextEntityId++,
                        fromAlly = true,
                        shooterName = ally.name,
                        weaponName = "M4A1",
                        x = ax + cos(shotAngle) * 18f,
                        y = ay + sin(shotAngle) * 18f,
                        z = 28f,
                        vx = cos(shotAngle) * 760f,
                        vy = sin(shotAngle) * 760f,
                        vz = 0f,
                        damage = 16f,
                        life = 1.4f
                    )
                }
            }
            val resolved = resolvePropCollision(ax, ay, 0f, 17f)
            finalAllies.add(
                ally.copy(
                    x = resolved.first.coerceIn(80f, WORLD_SIZE - 80f),
                    y = resolved.second.coerceIn(80f, WORLD_SIZE - 80f),
                    yaw = aYaw,
                    walkPhase = aWalk,
                    hp = ahp,
                    isAlive = ahp > 0f,
                    fireCooldown = aCd,
                    muzzleFlashTimer = aMuzzle,
                    hitFlashTimer = aHit,
                    strafeDir = strafeDir,
                    strafeTimer = strafeTimer
                )
            )
        }

        // Update Enemies (4v4 Clash Squad or Open World Patrols)
        val updatedEnemies = ArrayList<EnemyState>(current.enemies.size)
        for (enemy in current.enemies) {
            if (!enemy.isAlive) {
                if (isClash) {
                    updatedEnemies.add(enemy)
                } else {
                    val rem = enemy.respawnTimer - dt
                    if (rem <= 0f) {
                        updatedEnemies.add(
                            enemy.copy(
                                x = enemy.homeX, y = enemy.homeY, hp = enemy.maxHp,
                                isAlive = true, respawnTimer = 0f, fireCooldown = 0.8f
                            )
                        )
                    } else {
                        updatedEnemies.add(enemy.copy(respawnTimer = rem))
                    }
                }
                continue
            }
            if (barriersActive) {
                updatedEnemies.add(
                    enemy.copy(
                        hitFlashTimer = (enemy.hitFlashTimer - dt).coerceAtLeast(0f),
                        muzzleFlashTimer = (enemy.muzzleFlashTimer - dt).coerceAtLeast(0f)
                    )
                )
                continue
            }
            val eFlash = (enemy.hitFlashTimer - dt).coerceAtLeast(0f)
            val eMuzzle = (enemy.muzzleFlashTimer - dt).coerceAtLeast(0f)
            var eHp = enemy.hp
            val distZ = hypot(enemy.x - current.zoneCenterX, enemy.y - current.zoneCenterY)
            if (isClash && distZ > zoneRadius) {
                eHp = (eHp - 10f * dt).coerceAtLeast(0f)
                if (eHp <= 0f) {
                    pushKillFeed(
                        killerName = GameStrings.safeZoneName(current.language),
                        killerTeam = KillFeedTeam.ZONE,
                        victimName = enemy.name,
                        victimTeam = KillFeedTeam.ENEMY,
                        weaponName = "ZONE"
                    )
                }
            }

            // Find nearest opposing fighter (Player or Alive Ally) without allocating Triple/List
            var hasTarget = false
            var targetX = 0f
            var targetY = 0f
            var targetZ = 0f
            var bestTargetDist = Float.MAX_VALUE
            if (playerAlive) {
                hasTarget = true
                targetX = px
                targetY = py
                targetZ = pz + 24f
                bestTargetDist = hypot(px - enemy.x, py - enemy.y)
            }
            for (al in finalAllies) {
                if (!al.isAlive) continue
                val d = hypot(al.x - enemy.x, al.y - enemy.y)
                if (!hasTarget || d < bestTargetDist) {
                    hasTarget = true
                    targetX = al.x
                    targetY = al.y
                    targetZ = al.z + 24f
                    bestTargetDist = d
                }
            }

            var ex = enemy.x
            var ey = enemy.y
            var eYaw = enemy.yaw
            var eWalk = enemy.walkPhase
            var eFireCd = (enemy.fireCooldown - dt).coerceAtLeast(0f)
            var strafeDir = enemy.strafeDir
            var strafeTimer = enemy.strafeTimer - dt
            if (strafeTimer <= 0f) {
                strafeDir = -strafeDir
                strafeTimer = 1.2f + Random.nextFloat() * 1.4f
            }

            var nextMuzzle = eMuzzle
            val aggroRange = if (isClash) 1150f else if (enemy.kind.isBoss) 720f else 560f
            val preferredDist = if (enemy.kind == EnemyKind.FOREST_SNIPER) 360f else 225f

            if (hasTarget && eHp > 0f) {
                val dx = targetX - ex
                val dy = targetY - ey
                val dist = hypot(dx, dy).coerceAtLeast(1f)
                if (dist < aggroRange) {
                    eYaw = atan2(dy, dx)
                    val dirX = dx / dist
                    val dirY = dy / dist
                    val perpX = -dirY * strafeDir
                    val perpY = dirX * strafeDir

                    val approach = when {
                        isClash && distZ > zoneRadius * 0.9f -> 0.95f
                        dist > preferredDist + 45f -> 0.85f
                        dist < preferredDist - 55f -> -0.5f
                        else -> 0f
                    }
                    ex += (dirX * approach + perpX * 0.65f) * enemy.kind.speed * dt
                    ey += (dirY * approach + perpY * 0.65f) * enemy.kind.speed * dt
                    eWalk = (eWalk + dt * 9f) % (2f * PI.toFloat())

                    if (eFireCd <= 0f && !isLineBlockedByCover(ex, ey, targetX, targetY)) {
                        eFireCd = if (enemy.kind.isBoss) 0.55f else if (enemy.kind == EnemyKind.FOREST_SNIPER) 1.55f else 0.98f
                        nextMuzzle = 0.08f
                        val aimSpread = (Random.nextFloat() - 0.5f) * 0.085f
                        val shotAngle = eYaw + aimSpread
                        val projSpeed = if (enemy.kind == EnemyKind.FOREST_SNIPER) 880f else 700f
                        val dz = (targetZ - (enemy.z + 28f)) / dist
                        spawnedProjectiles += EnemyProjectile(
                            id = nextEntityId++,
                            fromAlly = false,
                            shooterName = enemy.name,
                            weaponName = if (enemy.kind == EnemyKind.FOREST_SNIPER) "AWM" else "AK47",
                            x = ex + cos(shotAngle) * 18f,
                            y = ey + sin(shotAngle) * 18f,
                            z = enemy.z + 28f,
                            vx = cos(shotAngle) * projSpeed,
                            vy = sin(shotAngle) * projSpeed,
                            vz = dz * projSpeed,
                            damage = enemy.kind.damage,
                            life = 1.6f
                        )
                    }
                }
            }

            val resolvedE = resolvePropCollision(ex, ey, enemy.z, enemy.kind.radius)
            updatedEnemies.add(
                enemy.copy(
                    x = resolvedE.first.coerceIn(80f, WORLD_SIZE - 80f),
                    y = resolvedE.second.coerceIn(80f, WORLD_SIZE - 80f),
                    yaw = eYaw,
                    walkPhase = eWalk,
                    hp = eHp,
                    isAlive = eHp > 0f,
                    fireCooldown = eFireCd,
                    muzzleFlashTimer = nextMuzzle,
                    hitFlashTimer = eFlash,
                    strafeDir = strafeDir,
                    strafeTimer = strafeTimer
                )
            )
        }

        // Resolve Projectiles (Ally Bullets -> Enemies; Enemy Bullets -> Player or Allies)
        var playerHitFlash = hitFlash
        var playerShake = shake
        var bulletKilledPlayer = false
        var allyKillsXp = 0
        var allyKillsGold = 0
        val totalProjCount = current.enemyProjectiles.size + spawnedProjectiles.size
        val survivingBullets: List<EnemyProjectile> = if (totalProjCount == 0) {
            emptyList()
        } else {
            val list = ArrayList<EnemyProjectile>(totalProjCount)

            fun stepProjectile(proj: EnemyProjectile) {
                val rem = proj.life - dt
                if (rem <= 0f) return
                val nx = proj.x + proj.vx * dt
                val ny = proj.y + proj.vy * dt
                val nz = proj.z + proj.vz * dt

                var hitCover = false
                for (p in activeProps) {
                    if (p.blocksBullets && nz <= p.height && abs(nx - p.x) <= p.halfW && abs(ny - p.y) <= p.halfD) {
                        hitCover = true
                        break
                    }
                }
                if (hitCover) {
                    if (newParticles.size < current.graphicsQuality.maxParticles) {
                        newParticles += Particle3D(nx, ny, nz, 0f, 0f, 35f, 0.25f, 0.25f, 4f, Color(0xFFFFD166))
                    }
                    return
                }

                if (proj.fromAlly) {
                    var hitEnemy = false
                    for (i in updatedEnemies.indices) {
                        val en = updatedEnemies[i]
                        if (!en.isAlive) continue
                        if (hypot(nx - en.x, ny - en.y) < en.kind.radius + 5f && nz in 0f..(en.kind.height + 6f)) {
                            val nextHp = (en.hp - proj.damage).coerceAtLeast(0f)
                            val died = nextHp <= 0f
                            updatedEnemies[i] = en.copy(
                                hp = nextHp,
                                isAlive = !died,
                                hitFlashTimer = 0.14f,
                                respawnTimer = if (!isClash && died) 18f else 0f
                            )
                            if (died) {
                                allyKillsXp += en.kind.xpReward / 2
                                allyKillsGold += en.kind.goldReward / 2
                                pushKillFeed(
                                    killerName = proj.shooterName.ifBlank { "Squad" },
                                    killerTeam = KillFeedTeam.ALLY,
                                    victimName = en.name,
                                    victimTeam = KillFeedTeam.ENEMY,
                                    weaponName = proj.weaponName
                                )
                                spawnedLootDrops += GroundLootItem(
                                    id = "drop_${nextEntityId++}",
                                    kind = LootKind.MEDKIT_PACK,
                                    x = en.x,
                                    y = en.y,
                                    labelBn = "মেডকিট (+2)",
                                    labelEn = "Medkit (+2)",
                                    color = Color(0xFF10B981)
                                )
                            }
                            hitEnemy = true
                            break
                        }
                    }
                    if (!hitEnemy) list.add(proj.copy(x = nx, y = ny, z = nz, life = rem))
                } else {
                    if (playerAlive && !playerKilledByZone && hypot(nx - px, ny - py) < 20f && nz in (pz - 2f)..(pz + 42f)) {
                        var dmg = proj.damage
                        if (armor > 0f) {
                            val absorbed = minOf(armor, dmg * 0.65f)
                            armor -= absorbed
                            dmg -= absorbed
                        }
                        hp = (hp - dmg).coerceAtLeast(0f)
                        playerHitFlash = 0.22f
                        playerShake = 5.5f
                        lastPlayerDamagerName = proj.shooterName.ifBlank { "Enemy" }
                        lastPlayerDamagerWeapon = proj.weaponName
                        lastPlayerDamageFromZone = false
                        if (hp <= 0f) bulletKilledPlayer = true
                        return
                    }
                    var hitAlly = false
                    for (i in finalAllies.indices) {
                        val al = finalAllies[i]
                        if (!al.isAlive) continue
                        if (hypot(nx - al.x, ny - al.y) < 20f && nz in 0f..44f) {
                            val nextHp = (al.hp - proj.damage).coerceAtLeast(0f)
                            val died = nextHp <= 0f
                            finalAllies[i] = al.copy(
                                hp = nextHp,
                                isAlive = !died,
                                hitFlashTimer = 0.14f
                            )
                            if (died) {
                                pushKillFeed(
                                    killerName = proj.shooterName.ifBlank { "Enemy" },
                                    killerTeam = KillFeedTeam.ENEMY,
                                    victimName = al.name,
                                    victimTeam = KillFeedTeam.ALLY,
                                    weaponName = proj.weaponName
                                )
                            }
                            hitAlly = true
                            break
                        }
                    }
                    if (!hitAlly) list.add(proj.copy(x = nx, y = ny, z = nz, life = rem))
                }
            }

            for (proj in current.enemyProjectiles) stepProjectile(proj)
            for (proj in spawnedProjectiles) stepProjectile(proj)
            list
        }

        // Player elimination check + kill feed entry
        var reviveBanner: String? = null
        if (playerAlive && hp <= 0f) {
            val killedByZone = playerKilledByZone || (!bulletKilledPlayer && (outsideZone || lastPlayerDamageFromZone))
            if (killedByZone) {
                pushKillFeed(
                    killerName = GameStrings.safeZoneName(current.language),
                    killerTeam = KillFeedTeam.ZONE,
                    victimName = current.playerName,
                    victimTeam = KillFeedTeam.ALLY,
                    weaponName = "ZONE",
                    isPlayerInvolved = true
                )
            } else {
                pushKillFeed(
                    killerName = lastPlayerDamagerName ?: "Enemy Squad",
                    killerTeam = KillFeedTeam.ENEMY,
                    victimName = current.playerName,
                    victimTeam = KillFeedTeam.ALLY,
                    weaponName = lastPlayerDamagerWeapon,
                    isPlayerInvolved = true
                )
            }

            if (isClash) {
                playerAlive = false
                reviveBanner = if (current.language == AppLanguage.BN) {
                    "আপনি নকড আউট! আপনার টিম লড়ছে..."
                } else {
                    "Eliminated! Spectating remaining squadmates..."
                }
                statusBannerTimer = 2.8f
            } else {
                px = 1600f
                py = 2200f
                pz = 0f
                hp = current.maxHp
                armor = current.maxArmor * 0.6f
                lastPlayerDamagerName = null
                lastPlayerDamageFromZone = false
                reviveBanner = if (current.language == AppLanguage.BN) {
                    "এলিমিনেট! আপনি ক্লকটাওয়ার বেসে রিস্টোর হয়েছেন।"
                } else {
                    "Eliminated! Redeployed at Clocktower Base."
                }
                statusBannerTimer = 3.2f
            }
        }

        // Clash Squad Round Win / Loss Evaluation
        var nextRoundEndBanner: String? = null
        var nextAllyWins = current.allyRoundWins
        var nextEnemyWins = current.enemyRoundWins
        var matchWinner: Boolean? = null
        var roundBonusGold = allyKillsGold
        var roundBonusXp = allyKillsXp

        if (isClash) {
            var alliesAlive = if (playerAlive) 1 else 0
            for (a in finalAllies) if (a.isAlive) alliesAlive++
            var enemiesAlive = 0
            for (e in updatedEnemies) if (e.isAlive) enemiesAlive++

            if (enemiesAlive == 0) {
                nextAllyWins++
                roundBonusGold += 140
                roundBonusXp += 90
                roundTransitionTimer = 3.0f
                if (nextAllyWins >= 4) {
                    matchWinner = true
                    nextRoundEndBanner = if (current.language == AppLanguage.BN) "BOOYAH! আপনি ক্লাশ স্কোয়াড ম্যাচ জিতেছেন!" else "BOOYAH! CLASH SQUAD VICTORY!"
                } else {
                    nextRoundEndBanner = if (current.language == AppLanguage.BN) "রাউন্ড ${current.clashRound} জয়! (+140G)" else "ROUND ${current.clashRound} WON! (+140G)"
                }
            } else if (alliesAlive == 0) {
                nextEnemyWins++
                roundBonusGold += 60
                roundBonusXp += 40
                roundTransitionTimer = 3.0f
                if (nextEnemyWins >= 4) {
                    matchWinner = false
                    nextRoundEndBanner = if (current.language == AppLanguage.BN) "ম্যাচ পরাজয় — আবার চেষ্টা করুন!" else "DEFEAT — ENEMY SQUAD WON"
                } else {
                    nextRoundEndBanner = if (current.language == AppLanguage.BN) "রাউন্ড ${current.clashRound} পরাজয় — পরবর্তী রাউন্ড..." else "ROUND ${current.clashRound} LOST — NEXT ROUND..."
                }
            }
        }

        // Ground Loot Proximity & Optional Auto-Pickup (zero-alloc loop)
        var updatedLoot = if (spawnedLootDrops.isEmpty()) current.groundLoot else current.groundLoot + spawnedLootDrops
        var nextMedkits = current.medkits
        val nextReserve = reserveAmmo
        var lootBanner: String? = null

        var nearbyLootItem: GroundLootItem? = null
        if (playerAlive) {
            var bestLootDist = 62f
            for (item in updatedLoot) {
                if (item.pickedUp) continue
                val d = hypot(px - item.x, py - item.y)
                if (d < bestLootDist) {
                    bestLootDist = d
                    nearbyLootItem = item
                }
            }
        }

        var finalNearbyLoot = nearbyLootItem
        if (nearbyLootItem != null && current.autoLootEnabled) {
            val shouldAuto = nearbyLootItem.kind == LootKind.MEDKIT_PACK ||
                nearbyLootItem.kind == LootKind.AMMO_CRATE ||
                nearbyLootItem.kind == LootKind.ARMOR_VEST
            if (shouldAuto) {
                var ammoBoosted: IntArray? = null
                when (nearbyLootItem.kind) {
                    LootKind.MEDKIT_PACK -> nextMedkits = (nextMedkits + 2).coerceAtMost(8)
                    LootKind.AMMO_CRATE -> {
                        ammoBoosted = nextReserve.copyOf()
                        for (i in ammoBoosted.indices) ammoBoosted[i] += 55
                    }
                    LootKind.ARMOR_VEST -> armor = current.maxArmor
                    else -> {}
                }
                if (ammoBoosted != null) {
                    for (i in ammoBoosted.indices) nextReserve[i] = ammoBoosted[i]
                }
                updatedLoot = updatedLoot.map { if (it.id == nearbyLootItem.id) it.copy(pickedUp = true) else it }
                lootBanner = if (current.language == AppLanguage.BN) "অটো লুট: ${nearbyLootItem.labelBn}" else "Looted: ${nearbyLootItem.labelEn}"
                statusBannerTimer = 1.8f
                finalNearbyLoot = null
            }
        }

        // Open World Interactables & Waypoints (zero-alloc loop)
        var updatedInteractables = current.interactables
        var waystoneUpdated = false
        if (!isClash) {
            for (item in updatedInteractables) {
                if (item.kind == InteractableKind.WAYPOINT && !item.activated && hypot(px - item.x, py - item.y) < 110f) {
                    discoveredWaypoints.add(item.id)
                    waystoneUpdated = true
                }
            }
            if (waystoneUpdated) {
                updatedInteractables = updatedInteractables.map {
                    if (it.kind == InteractableKind.WAYPOINT && it.id in discoveredWaypoints) it.copy(activated = true) else it
                }
            }
        }
        var nearby: WorldInteractable? = null
        if (updatedInteractables.isNotEmpty()) {
            var bestInteractDist = 95f
            for (item in updatedInteractables) {
                val depleted = item.activated && (
                    item.kind == InteractableKind.AIRDROP_CRATE ||
                        item.kind == InteractableKind.INTEL_TERMINAL ||
                        item.kind == InteractableKind.AMMO_BOX
                    )
                if (depleted) continue
                val d = hypot(px - item.x, py - item.y)
                if (d < bestInteractDist) {
                    bestInteractDist = d
                    nearby = item
                }
            }
        }

        val dropBanner = if (barrierJustDropped) {
            statusBannerTimer = 2.4f
            if (current.language == AppLanguage.BN) "ব্যারিয়ার খুলেছে! ৪v৪ ফাইট শুরু!" else "BARRIERS DOWN! 4v4 FIGHT!"
        } else null

        val nextBanner: String? = when {
            reviveBanner != null -> reviveBanner
            dropBanner != null -> dropBanner
            lootBanner != null -> lootBanner
            statusBannerTimer > 0f -> {
                statusBannerTimer -= dt
                if (statusBannerTimer > 0f) current.statusBanner else null
            }
            else -> null
        }

        val shouldAutoFire = playerAlive && current.isFiring && current.activeWeapon.isAutomatic && fireCd <= 0f && reloadTimer <= 0f

        var next = current.copy(
            timeOfDay = nextTimeOfDay,
            playerX = px,
            playerY = py,
            playerZ = pz,
            velZ = velZ,
            isGrounded = grounded,
            isPlayerAlive = playerAlive,
            hp = hp,
            armor = armor,
            medkits = nextMedkits,
            isMoving = isMoving,
            isSprinting = isSprinting,
            walkPhase = walkPhase,
            fireCooldown = fireCd,
            reloadTimer = reloadTimer,
            magAmmo = magAmmo,
            reserveAmmo = nextReserve,
            muzzleFlashTimer = muzzleTimer,
            hitmarkerTimer = hitmarkerTimer,
            hitFlashTimer = playerHitFlash,
            recoilPitch = recoil,
            screenShake = playerShake,
            currentBiome = WorldGenerator.biomeAt(px, py),
            roundCountdown = countdown,
            allyRoundWins = nextAllyWins,
            enemyRoundWins = nextEnemyWins,
            roundEndBanner = nextRoundEndBanner,
            matchFinishedWinner = matchWinner,
            allyCage = if (barriersActive) current.allyCage else null,
            enemyCage = if (barriersActive) current.enemyCage else null,
            zoneRadius = zoneRadius,
            isOutsideZone = outsideZone,
            zoneTickFlash = zoneTickFlash,
            allies = finalAllies,
            enemies = updatedEnemies,
            groundLoot = updatedLoot,
            nearbyLoot = finalNearbyLoot,
            interactables = updatedInteractables,
            nearbyInteractable = nearby,
            tracers = newTracers,
            enemyProjectiles = survivingBullets,
            particles = newParticles,
            floatingTexts = newFloating,
            killFeed = killFeedList ?: current.killFeed,
            statusBanner = nextBanner
        )
        if (roundBonusGold > 0 || roundBonusXp > 0) {
            next = applyRewards(next, roundBonusGold, 0, roundBonusXp)
        }
        if (waystoneUpdated) {
            next = applyMissionProgress(next, "m5_recon", discoveredWaypoints.size)
        }

        liveRenderState = next
        hudEmitAccumulator += dt
        val discreteHudChange =
            next.killFeed.size != current.killFeed.size ||
                next.hp.toInt() != current.hp.toInt() ||
                next.armor.toInt() != current.armor.toInt() ||
                next.isPlayerAlive != current.isPlayerAlive ||
                next.isOutsideZone != current.isOutsideZone ||
                next.nearbyLoot?.id != current.nearbyLoot?.id ||
                next.nearbyInteractable?.id != current.nearbyInteractable?.id ||
                ceil(next.roundCountdown) != ceil(current.roundCountdown) ||
                next.roundEndBanner != current.roundEndBanner ||
                next.statusBanner != current.statusBanner ||
                next.allyAliveCount != current.allyAliveCount ||
                next.enemyAliveCount != current.enemyAliveCount ||
                (current.reloadTimer > 0f && next.reloadTimer == 0f)

        if (discreteHudChange || hudEmitAccumulator >= 0.055f) {
            hudEmitAccumulator = 0f
            _uiState.value = next
        }

        if (shouldAutoFire) {
            fireShot()
        }

        if (waystoneUpdated || nextRoundEndBanner != null) persistCurrentState()

        saveAccumulator += dt
        if (saveAccumulator >= 6f) {
            saveAccumulator = 0f
            persistCurrentState()
        }
    }


    private fun resolvePropCollision(inX: Float, inY: Float, pz: Float, radius: Float): Pair<Float, Float> {
        var px = inX
        var py = inY
        for (prop in activeProps) {
            if (pz > prop.height - 6f) continue
            val dx = px - prop.x
            val dy = py - prop.y
            val clampedX = dx.coerceIn(-prop.halfW, prop.halfW)
            val clampedY = dy.coerceIn(-prop.halfD, prop.halfD)
            val closestX = prop.x + clampedX
            val closestY = prop.y + clampedY
            val distX = px - closestX
            val distY = py - closestY
            val dist = hypot(distX, distY)
            if (dist < radius) {
                if (dist > 0.001f) {
                    val push = radius - dist
                    px += (distX / dist) * push
                    py += (distY / dist) * push
                } else {
                    px += radius
                }
            }
        }
        return px to py
    }

    fun setJoystickVector(dx: Float, dy: Float) {
        joyDx = dx
        joyDy = dy
    }

    fun rotateCamera(deltaYaw: Float, deltaPitch: Float) {
        val state = liveRenderState
        val activeSens = if (state.isScoped) state.scopeSensitivity else state.generalSensitivity
        val yawPrecision = if (abs(deltaYaw) < 5f) 0.78f else 1.0f
        val pitchPrecision = if (abs(deltaPitch) < 6f) 0.72f else 1.0f
        val yawRate = 0.0042f * activeSens * yawPrecision
        val pitchRate = 0.0018f * activeSens * pitchPrecision
        var nextYaw = state.cameraYaw + deltaYaw * yawRate
        while (nextYaw > PI) nextYaw -= (2.0 * PI).toFloat()
        while (nextYaw < -PI) nextYaw += (2.0 * PI).toFloat()
        updateRenderOnly {
            it.copy(
                cameraYaw = nextYaw,
                cameraPitch = (it.cameraPitch - deltaPitch * pitchRate).coerceIn(-0.28f, 0.24f)
            )
        }
    }

    fun setFiring(firing: Boolean) {
        updateBothStates { it.copy(isFiring = firing) }
        if (firing) {
            fireShot()
        }
    }

    fun fireShot() {
        val state = liveRenderState
        if (!state.isPlayerAlive || state.fireCooldown > 0f || state.reloadTimer > 0f) return

        val wIdx = state.weaponIndex
        val weapon = state.activeWeapon
        val currentMag = state.magAmmo[wIdx]
        if (currentMag <= 0) {
            startReload()
            return
        }

        val newMag = state.magAmmo.copyOf().also { it[wIdx] = currentMag - 1 }
        val autoReloadNext = if (newMag[wIdx] == 0 && state.reserveAmmo[wIdx] > 0) weapon.reloadTime * reloadSpeedFactor(state.energyLevel) else 0f
        soundManager.playWeaponFire(weapon.index)

        val yaw = state.cameraYaw + (Random.nextFloat() - 0.5f) * weapon.spread * (if (state.isScoped) 0.22f else 1f)
        val pitch = state.cameraPitch + state.recoilPitch + (Random.nextFloat() - 0.5f) * weapon.spread * 0.5f
        val dirX = cos(pitch) * cos(yaw)
        val dirY = cos(pitch) * sin(yaw)
        val dirZ = sin(pitch)

        val distBehind = if (state.isScoped) 12f else 92f
        val shoulder = if (state.isScoped) 4f else 20f
        val camX = state.playerX - cos(state.cameraYaw) * distBehind - sin(state.cameraYaw) * shoulder
        val camY = state.playerY - sin(state.cameraYaw) * distBehind + cos(state.cameraYaw) * shoulder
        val camZ = state.playerZ + 44f

        val muzzleX = state.playerX + cos(state.cameraYaw) * 24f - sin(state.cameraYaw) * 10f
        val muzzleY = state.playerY + sin(state.cameraYaw) * 24f + cos(state.cameraYaw) * 10f
        val muzzleZ = state.playerZ + 28f

        var maxRayDist = weapon.range
        for (prop in activeProps) {
            if (!prop.blocksBullets) continue
            val t = rayIntersectsBoxXY(camX, camY, camZ, dirX, dirY, dirZ, prop.x, prop.y, prop.halfW, prop.halfD, prop.height)
            if (t != null && t > 20f && t < maxRayDist) {
                maxRayDist = t
            }
        }

        var hitEnemyIndex = -1
        var hitEnemyDist = maxRayDist
        var isHeadshot = false

        // Do not hit enemies across spawn barrier during 15s countdown
        if (!(state.gameMode == GameMode.CLASH_SQUAD && state.roundCountdown > 0f)) {
            state.enemies.forEachIndexed { idx, enemy ->
                if (!enemy.isAlive) return@forEachIndexed
                val toEx = enemy.x - camX
                val toEy = enemy.y - camY
                val proj = toEx * dirX + toEy * dirY
                if (proj <= distBehind * 0.5f || proj >= hitEnemyDist) return@forEachIndexed

                val closestX = camX + dirX * proj
                val closestY = camY + dirY * proj
                val closestZ = camZ + dirZ * proj
                val horizDist = hypot(closestX - enemy.x, closestY - enemy.y)

                val hitRadius = enemy.kind.radius * 1.18f
                if (horizDist <= hitRadius && closestZ in (enemy.z - 2f)..(enemy.z + enemy.kind.height + 6f)) {
                    hitEnemyIndex = idx
                    hitEnemyDist = proj
                    isHeadshot = closestZ >= enemy.z + enemy.kind.height * 0.68f
                }
            }
        }

        val endX = camX + dirX * hitEnemyDist
        val endY = camY + dirY * hitEnemyDist
        val endZ = (camZ + dirZ * hitEnemyDist).coerceAtLeast(2f)

        val newTracers = state.tracers.toMutableList()
        newTracers += BulletTracer3D(
            id = nextEntityId++,
            startX = muzzleX, startY = muzzleY, startZ = muzzleZ,
            endX = endX, endY = endY, endZ = endZ,
            color = weapon.accentColor, life = 0.11f, maxLife = 0.11f
        )

        val newParticles = state.particles.toMutableList()
        val newFloating = state.floatingTexts.toMutableList()
        val droppedLoot = mutableListOf<GroundLootItem>()
        var xpGain = 0
        var goldGain = 0
        var raiderKills = 0
        var bossKills = 0
        var totalKills = 0
        var headshotCount = 0

        var playerKillEntry: KillFeedEntry? = null
        val updatedEnemies = if (hitEnemyIndex >= 0) {
            val target = state.enemies[hitEnemyIndex]
            val dmg = weapon.damage * state.effectiveDamageMultiplier * (if (isHeadshot) 2.0f else 1.0f)
            val newHp = (target.hp - dmg).coerceAtLeast(0f)

            newFloating += FloatingText3D(
                id = nextEntityId++,
                x = target.x, y = target.y, z = target.z + target.kind.height + 10f,
                text = if (isHeadshot) "${GameStrings.headshotLabel(state.language)} ${dmg.toInt()}" else "${dmg.toInt()}",
                color = if (isHeadshot) Color(0xFFFF4D6D) else Color(0xFFFFD166),
                life = 0.85f, maxLife = 0.85f
            )

            repeat(if (isHeadshot) 8 else 4) {
                val a = Random.nextFloat() * 6.28f
                newParticles += Particle3D(
                    x = endX, y = endY, z = endZ,
                    vx = cos(a) * 65f, vy = sin(a) * 65f, vz = 45f,
                    life = 0.35f, maxLife = 0.35f, size = 4f,
                    color = if (isHeadshot) Color(0xFFFF4D6D) else Color(0xFFEF4444)
                )
            }

            state.enemies.mapIndexed { idx, enemy ->
                if (idx != hitEnemyIndex) enemy
                else if (newHp <= 0f) {
                    totalKills++
                    if (isHeadshot) headshotCount++
                    xpGain += enemy.kind.xpReward
                    goldGain += enemy.kind.goldReward
                    if (enemy.kind == EnemyKind.SCOUT_RAIDER) raiderKills++
                    if (enemy.kind == EnemyKind.PEAK_COMMANDER) bossKills++

                    playerKillEntry = KillFeedEntry(
                        id = nextEntityId++,
                        killerName = state.playerName,
                        killerTeam = KillFeedTeam.ALLY,
                        victimName = enemy.name,
                        victimTeam = KillFeedTeam.ENEMY,
                        weaponName = shortWeaponTag(weapon.index),
                        isHeadshot = isHeadshot,
                        isPlayerInvolved = true
                    )

                    newFloating += FloatingText3D(
                        id = nextEntityId++,
                        x = enemy.x, y = enemy.y, z = enemy.z + 52f,
                        text = "+${enemy.kind.goldReward}G  +${enemy.kind.xpReward}XP",
                        color = Color(0xFF34D399),
                        life = 1.25f, maxLife = 1.25f
                    )
                    // Enemy drops ground loot box!
                    val dropKind = if (idx % 2 == 0) LootKind.ARMOR_VEST else LootKind.MEDKIT_PACK
                    droppedLoot += GroundLootItem(
                        id = "edrop_${nextEntityId++}",
                        kind = dropKind,
                        x = enemy.x,
                        y = enemy.y,
                        labelBn = if (dropKind == LootKind.ARMOR_VEST) "আর্মর ভেস্ট" else "মেডকিট (+2)",
                        labelEn = if (dropKind == LootKind.ARMOR_VEST) "Armor Vest" else "Medkit (+2)",
                        color = if (dropKind == LootKind.ARMOR_VEST) Color(0xFF00E5FF) else Color(0xFF10B981)
                    )
                    enemy.copy(
                        hp = 0f,
                        isAlive = false,
                        respawnTimer = if (state.gameMode == GameMode.OPEN_WORLD) (if (enemy.kind.isBoss) 40f else 18f) else 0f
                    )
                } else {
                    enemy.copy(hp = newHp, hitFlashTimer = 0.14f)
                }
            }
        } else {
            if (hitEnemyDist < weapon.range && newParticles.size < state.graphicsQuality.maxParticles) {
                repeat(3) {
                    newParticles += Particle3D(
                        x = endX, y = endY, z = endZ,
                        vx = (Random.nextFloat() - 0.5f) * 55f,
                        vy = (Random.nextFloat() - 0.5f) * 55f,
                        vz = 38f,
                        life = 0.28f, maxLife = 0.28f, size = 3.5f,
                        color = Color(0xFFFDE047)
                    )
                }
            }
            state.enemies
        }

        val recoilKick = when (weapon.index) {
            3 -> 0.028f
            2 -> 0.0085f
            else -> 0.0055f
        }
        updateBothStates { s ->
            val updatedKillFeed = playerKillEntry?.let { entry ->
                (s.killFeed + entry).takeLast(20)
            } ?: s.killFeed
            var next = s.copy(
                magAmmo = newMag,
                fireCooldown = weapon.fireInterval,
                reloadTimer = autoReloadNext,
                muzzleFlashTimer = 0.065f,
                recoilPitch = (s.recoilPitch + recoilKick).coerceAtMost(0.055f),
                screenShake = if (weapon.index == 3) 4.5f else 1.5f,
                hitmarkerTimer = if (hitEnemyIndex >= 0) 0.18f else s.hitmarkerTimer,
                hitmarkerIsHeadshot = if (hitEnemyIndex >= 0) isHeadshot else s.hitmarkerIsHeadshot,
                tracers = newTracers,
                particles = newParticles,
                floatingTexts = newFloating,
                enemies = updatedEnemies,
                groundLoot = if (droppedLoot.isNotEmpty()) s.groundLoot + droppedLoot else s.groundLoot,
                killFeed = updatedKillFeed,
                enemiesDefeated = s.enemiesDefeated + totalKills,
                headshots = s.headshots + headshotCount
            )
            if (xpGain > 0 || goldGain > 0) {
                next = applyRewards(next, goldGain, 0, xpGain)
            }
            if (raiderKills > 0) {
                val prev = next.missions.firstOrNull { it.id == "m1_patrol" }?.progress ?: 0
                next = applyMissionProgress(next, "m1_patrol", prev + raiderKills)
            }
            if (bossKills > 0) {
                next = applyMissionProgress(next, "m4_commander", 1)
            }
            next
        }
        if (hitEnemyIndex >= 0) {
            soundManager.playHitmarker(isHeadshot)
        }
        if (totalKills > 0) persistCurrentState()
    }

    fun pickupNearbyLoot() {
        val state = liveRenderState
        val loot = state.nearbyLoot ?: return
        updateBothStates { s ->
            var nextWIdx = s.weaponIndex
            val nextMag = s.magAmmo.copyOf()
            val nextReserve = s.reserveAmmo.copyOf()
            var nextArmor = s.armor
            var nextMedkits = s.medkits

            when (loot.kind) {
                LootKind.WEAPON_M4A1 -> {
                    nextWIdx = 0
                    nextMag[0] = CustomizationCatalog.weapons[0].magSize
                    nextReserve[0] += 60
                }
                LootKind.WEAPON_MP40 -> {
                    nextWIdx = 1
                    nextMag[1] = CustomizationCatalog.weapons[1].magSize
                    nextReserve[1] += 72
                }
                LootKind.WEAPON_AK47 -> {
                    nextWIdx = 2
                    nextMag[2] = CustomizationCatalog.weapons[2].magSize
                    nextReserve[2] += 56
                }
                LootKind.WEAPON_AWM -> {
                    nextWIdx = 3
                    nextMag[3] = CustomizationCatalog.weapons[3].magSize
                    nextReserve[3] += 18
                }
                LootKind.ARMOR_VEST -> nextArmor = s.maxArmor
                LootKind.MEDKIT_PACK -> nextMedkits = (s.medkits + 2).coerceAtMost(8)
                LootKind.AMMO_CRATE -> {
                    for (i in nextReserve.indices) nextReserve[i] += 60
                }
            }
            statusBannerTimer = 2.0f
            val msg = if (s.language == AppLanguage.BN) "লুট সম্পন্ন: ${loot.labelBn}" else "Equipped: ${loot.labelEn}"
            s.copy(
                weaponIndex = nextWIdx,
                magAmmo = nextMag,
                reserveAmmo = nextReserve,
                armor = nextArmor,
                medkits = nextMedkits,
                reloadTimer = 0f,
                groundLoot = s.groundLoot.map { if (it.id == loot.id) it.copy(pickedUp = true) else it },
                nearbyLoot = null,
                statusBanner = msg
            )
        }
        persistCurrentState()
    }

    fun performJump() {
        val state = liveRenderState
        if (!state.isPlayerAlive || !state.isGrounded) return
        updateBothStates {
            it.copy(velZ = 165f, isGrounded = false)
        }
    }

    fun toggleSprintLock() {
        updateBothStates {
            val nextLock = !it.sprintLock
            it.copy(sprintLock = nextLock, isScoped = if (nextLock) false else it.isScoped)
        }
    }

    fun toggleScope() {
        updateBothStates {
            val nextScoped = !it.isScoped
            it.copy(isScoped = nextScoped, sprintLock = if (nextScoped) false else it.sprintLock)
        }
    }

    fun startReload() {
        val state = liveRenderState
        if (state.reloadTimer > 0f) return
        val wIdx = state.weaponIndex
        if (state.magAmmo[wIdx] >= state.activeWeapon.magSize || state.reserveAmmo[wIdx] <= 0) return
        soundManager.playReload()
        updateBothStates {
            it.copy(reloadTimer = state.activeWeapon.reloadTime * reloadSpeedFactor(state.energyLevel))
        }
    }

    fun cycleWeapon() {
        val nextIdx = (liveRenderState.weaponIndex + 1) % CustomizationCatalog.weapons.size
        setWeaponIndex(nextIdx)
    }

    fun useMedkit() {
        val state = liveRenderState
        if (!state.isPlayerAlive || state.medkits <= 0 || state.hp >= state.maxHp) return
        updateBothStates { s ->
            val healed = (s.hp + 75f).coerceAtMost(s.maxHp)
            statusBannerTimer = 2.0f
            s.copy(
                hp = healed,
                medkits = s.medkits - 1,
                statusBanner = if (s.language == AppLanguage.BN) "মেডকিট ব্যবহার সম্পন্ন (+75 HP)" else "Medkit Applied (+75 HP)"
            )
        }
        persistCurrentState()
    }

    fun interactWithNearby() {
        val state = liveRenderState
        val target = state.nearbyInteractable ?: return
        val newFloating = state.floatingTexts.toMutableList()

        when (target.kind) {
            InteractableKind.NPC -> {
                talkedNpcs.add(target.id)
                updateBothStates {
                    it.copy(
                        activeDialogue = ActiveDialogue(
                            speakerBn = target.labelBn,
                            speakerEn = target.labelEn,
                            messageBn = target.dialogueBn,
                            messageEn = target.dialogueEn
                        )
                    )
                }
            }
            InteractableKind.MEDKIT_STATION -> {
                newFloating += FloatingText3D(nextEntityId++, target.x, target.y, 40f, "FULL HP & ARMOR!", Color(0xFF10B981), 1.2f, 1.2f)
                updateBothStates {
                    it.copy(hp = it.maxHp, armor = it.maxArmor, medkits = maxOf(it.medkits, 3), floatingTexts = newFloating)
                }
            }
            InteractableKind.WAYPOINT -> {
                discoveredWaypoints.add(target.id)
                updateBothStates { s ->
                    val updated = s.interactables.map { if (it.id == target.id) it.copy(activated = true) else it }
                    val msg = if (s.language == AppLanguage.BN) "${target.labelBn} আনলক হয়েছে!" else "${target.labelEn} Unlocked!"
                    statusBannerTimer = 2.5f
                    applyMissionProgress(s.copy(interactables = updated, statusBanner = msg), "m5_recon", discoveredWaypoints.size)
                }
                persistCurrentState()
            }
            InteractableKind.AIRDROP_CRATE -> {
                if (openedAirdrops.add(target.id)) {
                    newFloating += FloatingText3D(nextEntityId++, target.x, target.y, 44f, "+150G +2 Medkits +Ammo", Color(0xFFFFB703), 1.3f, 1.3f)
                    updateBothStates { s ->
                        val updated = s.interactables.map { if (it.id == target.id) it.copy(activated = true) else it }
                        val boostedReserve = s.reserveAmmo.map { it + 45 }.toIntArray()
                        val withLoot = applyRewards(
                            s.copy(
                                interactables = updated, armor = s.maxArmor,
                                medkits = (s.medkits + 2).coerceAtMost(8), reserveAmmo = boostedReserve, floatingTexts = newFloating
                            ),
                            gold = 150, gems = 15, xp = 75
                        )
                        applyMissionProgress(withLoot, "m2_airdrops", openedAirdrops.size)
                    }
                    persistCurrentState()
                }
            }
            InteractableKind.INTEL_TERMINAL -> {
                if (disabledIntels.add(target.id)) {
                    newFloating += FloatingText3D(nextEntityId++, target.x, target.y, 44f, "INTEL HACKED!", Color(0xFF00E5FF), 1.25f, 1.25f)
                    updateBothStates { s ->
                        val updated = s.interactables.map { if (it.id == target.id) it.copy(activated = true) else it }
                        val withXp = applyRewards(s.copy(interactables = updated, floatingTexts = newFloating), gold = 90, gems = 12, xp = 65)
                        applyMissionProgress(withXp, "m3_intel", disabledIntels.size)
                    }
                    persistCurrentState()
                }
            }
            InteractableKind.AMMO_BOX -> {
                if (collectedAmmo.add(target.id)) {
                    newFloating += FloatingText3D(nextEntityId++, target.x, target.y, 38f, "+AMMO +MEDKIT", Color(0xFF34D399), 1.15f, 1.15f)
                    updateBothStates { s ->
                        val updated = s.interactables.map { if (it.id == target.id) it.copy(activated = true) else it }
                        val boostedReserve = s.reserveAmmo.map { it + 60 }.toIntArray()
                        s.copy(interactables = updated, reserveAmmo = boostedReserve, medkits = (s.medkits + 1).coerceAtMost(8), floatingTexts = newFloating)
                    }
                    persistCurrentState()
                }
            }
        }
    }

    fun setGeneralSensitivity(value: Float) {
        updateBothStates { it.copy(generalSensitivity = value.coerceIn(0.25f, 3.0f)) }
        persistCurrentState()
    }

    fun setScopeSensitivity(value: Float) {
        updateBothStates { it.copy(scopeSensitivity = value.coerceIn(0.20f, 2.50f)) }
        persistCurrentState()
    }

    fun setAutoLootEnabled(enabled: Boolean) {
        updateBothStates { it.copy(autoLootEnabled = enabled) }
        persistCurrentState()
    }

    fun setSoundEnabled(enabled: Boolean) {
        soundManager.setSoundEnabled(enabled)
        updateBothStates { it.copy(soundEnabled = enabled) }
        persistCurrentState()
    }

    fun setSoundVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        soundManager.setMasterVolume(clamped)
        updateBothStates { it.copy(soundVolume = clamped) }
        persistCurrentState()
    }

    fun onAppPause() {
        soundManager.onPause()
    }

    fun onAppResume() {
        soundManager.onResume()
    }

    fun openOverlay(overlay: ActiveOverlay) {
        updateBothStates { it.copy(activeOverlay = overlay, isFiring = false) }
    }

    fun closeOverlay() {
        updateBothStates { it.copy(activeOverlay = ActiveOverlay.NONE) }
    }

    fun dismissDialogue() {
        updateBothStates { it.copy(activeDialogue = null) }
    }

    fun selectTrackedMission(missionId: String) {
        updateBothStates { it.copy(trackedMissionId = missionId) }
        persistCurrentState()
    }

    fun claimMissionReward(missionId: String) {
        val state = liveRenderState
        val mission = state.missions.firstOrNull { it.id == missionId } ?: return
        if (!mission.completed || mission.rewardClaimed) return

        updateBothStates { s ->
            val updatedMissions = s.missions.map { if (it.id == missionId) it.copy(rewardClaimed = true) else it }
            val nextTracked = updatedMissions.firstOrNull { !it.completed || !it.rewardClaimed }?.id ?: s.trackedMissionId
            val banner = if (s.language == AppLanguage.BN) {
                "রিওয়ার্ড সংগৃহীত: +${mission.rewardGold} কয়েন, +${mission.rewardGems} রত্ন!"
            } else {
                "Reward Claimed: +${mission.rewardGold} Coins, +${mission.rewardGems} Gems!"
            }
            statusBannerTimer = 3.0f
            applyRewards(
                s.copy(missions = updatedMissions, trackedMissionId = nextTracked, statusBanner = banner),
                gold = mission.rewardGold, gems = mission.rewardGems, xp = mission.rewardXp
            )
        }
        persistCurrentState()
    }

    fun fastTravelToWaypoint(waypointId: String) {
        val state = liveRenderState
        val wp = state.interactables.firstOrNull {
            it.id == waypointId && it.kind == InteractableKind.WAYPOINT && it.activated
        } ?: return

        updateBothStates {
            statusBannerTimer = 2.5f
            val msg = if (it.language == AppLanguage.BN) "${wp.labelBn}-এ ল্যান্ড করেছেন!" else "Deployed to ${wp.labelEn}!"
            it.copy(
                playerX = wp.x, playerY = wp.y + 45f, playerZ = 0f,
                currentBiome = wp.biome, activeOverlay = ActiveOverlay.NONE, statusBanner = msg
            )
        }
        persistCurrentState()
    }

    fun setCloakIndex(index: Int) {
        updateBothStates { it.copy(cloakIndex = index.coerceIn(0, CustomizationCatalog.outfits.lastIndex)) }
        persistCurrentState()
    }

    fun setHatIndex(index: Int) {
        updateBothStates { it.copy(hatIndex = index.coerceIn(0, CustomizationCatalog.headgears.lastIndex)) }
        persistCurrentState()
    }

    fun setWeaponIndex(index: Int) {
        updateBothStates {
            it.copy(weaponIndex = index.coerceIn(0, CustomizationCatalog.weapons.lastIndex), reloadTimer = 0f, isFiring = false)
        }
        persistCurrentState()
    }

    fun setAuraIndex(index: Int) {
        updateBothStates { it.copy(auraIndex = index.coerceIn(0, CustomizationCatalog.backpacks.lastIndex)) }
        persistCurrentState()
    }

    fun upgradeAttribute(attr: String) {
        val s = liveRenderState
        val currentLvl = when (attr) {
            "vitality" -> s.vitalityLevel
            "might" -> s.mightLevel
            "agility" -> s.agilityLevel
            "energy" -> s.energyLevel
            else -> return
        }
        if (currentLvl >= 10) return
        val cost = upgradeGoldCost(currentLvl)
        if (s.gold < cost) return

        updateBothStates { state ->
            val newVit = if (attr == "vitality") state.vitalityLevel + 1 else state.vitalityLevel
            val newMight = if (attr == "might") state.mightLevel + 1 else state.mightLevel
            val newAgi = if (attr == "agility") state.agilityLevel + 1 else state.agilityLevel
            val newEng = if (attr == "energy") state.energyLevel + 1 else state.energyLevel
            val newMaxHp = computeMaxHp(state.level, newVit)
            val newMaxArmor = computeMaxArmor(newVit)
            state.copy(
                gold = state.gold - cost,
                vitalityLevel = newVit, mightLevel = newMight, agilityLevel = newAgi, energyLevel = newEng,
                maxHp = newMaxHp, hp = if (attr == "vitality") newMaxHp else state.hp,
                maxArmor = newMaxArmor, armor = if (attr == "vitality") newMaxArmor else state.armor
            )
        }
        persistCurrentState()
    }

    fun setGraphicsQuality(quality: GraphicsQuality) {
        updateBothStates { it.copy(graphicsQuality = quality) }
        persistCurrentState()
    }

    fun setTimeMode(mode: TimeOfDayMode) {
        updateBothStates { it.copy(timeMode = mode) }
        persistCurrentState()
    }

    fun setLanguage(language: AppLanguage) {
        updateBothStates { it.copy(language = language) }
        persistCurrentState()
    }

    fun resetAdventure() {
        val langCode = liveRenderState.language.code
        viewModelScope.launch {
            val (freshSave, freshMissions) = repository.resetAdventure(langCode)
            loadFromEntities(freshSave, freshMissions)
            updateBothStates {
                it.copy(
                    activeOverlay = ActiveOverlay.NONE,
                    showTitleOverlay = true,
                    statusBanner = if (it.language == AppLanguage.BN) "গেম রিসেট সম্পন্ন!" else "Match Data Reset!"
                )
            }
            statusBannerTimer = 2.5f
        }
    }

    fun upgradeGoldCost(currentLevel: Int): Int = 80 + (currentLevel - 1) * 65

    private fun shortWeaponTag(weaponIndex: Int): String = when (weaponIndex) {
        0 -> "M4A1"
        1 -> "MP40"
        2 -> "AK47"
        3 -> "AWM"
        else -> "AR"
    }

    fun activeWaypointCoordinates(state: GameUiState): Pair<Float, Float>? {
        if (state.gameMode == GameMode.CLASH_SQUAD) {
            var nearestX = state.zoneCenterX
            var nearestY = state.zoneCenterY
            var bestDist = Float.MAX_VALUE
            for (en in state.enemies) {
                if (!en.isAlive) continue
                val d = hypot(en.x - state.playerX, en.y - state.playerY)
                if (d < bestDist) {
                    bestDist = d
                    nearestX = en.x
                    nearestY = en.y
                }
            }
            return nearestX to nearestY
        }
        val m = state.activeMission ?: return null
        if (m.completed) return null
        var bestX = m.waypointX
        var bestY = m.waypointY
        var bestDist = Float.MAX_VALUE
        when (m.id) {
            "m1_patrol" -> {
                for (en in state.enemies) {
                    if (!en.isAlive || en.kind != EnemyKind.SCOUT_RAIDER) continue
                    val d = hypot(en.x - state.playerX, en.y - state.playerY)
                    if (d < bestDist) {
                        bestDist = d
                        bestX = en.x
                        bestY = en.y
                    }
                }
            }
            "m2_airdrops" -> {
                for (item in state.interactables) {
                    if (item.kind != InteractableKind.AIRDROP_CRATE || item.activated) continue
                    val d = hypot(item.x - state.playerX, item.y - state.playerY)
                    if (d < bestDist) {
                        bestDist = d
                        bestX = item.x
                        bestY = item.y
                    }
                }
            }
            "m3_intel" -> {
                for (item in state.interactables) {
                    if (item.kind != InteractableKind.INTEL_TERMINAL || item.activated) continue
                    val d = hypot(item.x - state.playerX, item.y - state.playerY)
                    if (d < bestDist) {
                        bestDist = d
                        bestX = item.x
                        bestY = item.y
                    }
                }
            }
            "m5_recon" -> {
                for (item in state.interactables) {
                    if (item.kind != InteractableKind.WAYPOINT || item.activated) continue
                    val d = hypot(item.x - state.playerX, item.y - state.playerY)
                    if (d < bestDist) {
                        bestDist = d
                        bestX = item.x
                        bestY = item.y
                    }
                }
            }
        }
        return bestX to bestY
    }

    private fun isLineBlockedByCover(x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
        val dx = x2 - x1
        val dy = y2 - y1
        val len = hypot(dx, dy)
        if (len < 1f) return false
        val dirX = dx / len
        val dirY = dy / len
        for (prop in activeProps) {
            if (!prop.blocksBullets || prop.height < 28f) continue
            val t = rayIntersectsBoxXY(x1, y1, 24f, dirX, dirY, 0f, prop.x, prop.y, prop.halfW, prop.halfD, prop.height)
            if (t != null && t > 10f && t < len) return true
        }
        return false
    }

    private fun rayIntersectsBoxXY(
        ox: Float, oy: Float, oz: Float,
        dx: Float, dy: Float, dz: Float,
        bx: Float, by: Float, halfW: Float, halfD: Float, height: Float
    ): Float? {
        val minX = bx - halfW
        val maxX = bx + halfW
        val minY = by - halfD
        val maxY = by + halfD

        val invX = if (abs(dx) > 1e-5f) 1f / dx else 1e5f
        val invY = if (abs(dy) > 1e-5f) 1f / dy else 1e5f

        val tx1 = (minX - ox) * invX
        val tx2 = (maxX - ox) * invX
        var tmin = minOf(tx1, tx2)
        var tmax = maxOf(tx1, tx2)

        val ty1 = (minY - oy) * invY
        val ty2 = (maxY - oy) * invY
        tmin = maxOf(tmin, minOf(ty1, ty2))
        tmax = minOf(tmax, maxOf(ty1, ty2))

        if (tmax < maxOf(0f, tmin)) return null
        val tHit = if (tmin >= 0f) tmin else tmax
        val zAtHit = oz + dz * tHit
        return if (zAtHit in 0f..height) tHit else null
    }

    private fun applyRewards(state: GameUiState, gold: Int, gems: Int, xp: Int): GameUiState {
        var lvl = state.level
        var curXp = state.xp + xp
        var nextReq = xpRequiredFor(lvl)
        var leveledUp = false
        while (curXp >= nextReq) {
            curXp -= nextReq
            lvl++
            nextReq = xpRequiredFor(lvl)
            leveledUp = true
        }
        val newMaxHp = computeMaxHp(lvl, state.vitalityLevel)
        val newHp = if (leveledUp) newMaxHp else state.hp
        val newBanner = if (leveledUp) {
            statusBannerTimer = 3.0f
            if (state.language == AppLanguage.BN) "র‍্যাংক আপ! এখন আপনি র‍্যাংক $lvl" else "Rank Up! Reached Level $lvl"
        } else state.statusBanner

        return state.copy(
            level = lvl, xp = curXp, xpToNext = nextReq,
            gold = state.gold + gold, aetherGems = state.aetherGems + gems,
            maxHp = newMaxHp, hp = newHp, statusBanner = newBanner
        )
    }

    private fun applyMissionProgress(state: GameUiState, missionId: String, newProgress: Int): GameUiState {
        var justCompletedTitle: String? = null
        val updated = state.missions.map { m ->
            if (m.id == missionId) {
                val clamped = newProgress.coerceAtMost(m.targetCount)
                val nowComplete = clamped >= m.targetCount
                if (nowComplete && !m.completed) {
                    justCompletedTitle = if (state.language == AppLanguage.BN) m.titleBn else m.titleEn
                }
                m.copy(progress = clamped, completed = m.completed || nowComplete)
            } else m
        }
        val banner = justCompletedTitle?.let {
            statusBannerTimer = 3.2f
            if (state.language == AppLanguage.BN) "মিশন সম্পন্ন: $it!" else "Mission Complete: $it!"
        } ?: state.statusBanner
        return state.copy(missions = updated, statusBanner = banner)
    }

    private fun persistCurrentState() {
        val s = liveRenderState
        if (!s.isLoaded) return
        viewModelScope.launch {
            repository.savePlayer(
                PlayerSaveEntity(
                    id = 1,
                    playerName = s.playerName,
                    level = s.level,
                    xp = s.xp,
                    gold = s.gold,
                    aetherGems = s.aetherGems,
                    hp = s.hp,
                    maxHp = s.maxHp,
                    armor = s.armor,
                    maxArmor = s.maxArmor,
                    medkits = s.medkits,
                    posX = s.playerX,
                    posY = s.playerY,
                    cloakIndex = s.cloakIndex,
                    hatIndex = s.hatIndex,
                    weaponIndex = s.weaponIndex,
                    auraIndex = s.auraIndex,
                    vitalityLevel = s.vitalityLevel,
                    mightLevel = s.mightLevel,
                    agilityLevel = s.agilityLevel,
                    energyLevel = s.energyLevel,
                    graphicsQuality = s.graphicsQuality.level,
                    timeMode = s.timeMode.index,
                    language = s.language.code,
                    generalSensitivity = s.generalSensitivity,
                    scopeSensitivity = s.scopeSensitivity,
                    autoLootEnabled = s.autoLootEnabled,
                    soundEnabled = s.soundEnabled,
                    soundVolume = s.soundVolume,
                    trackedMissionId = s.trackedMissionId,
                    discoveredWaypointsCsv = discoveredWaypoints.joinToString(","),
                    openedChestsCsv = openedAirdrops.joinToString(","),
                    activatedRunesCsv = disabledIntels.joinToString(","),
                    collectedShardsCsv = collectedAmmo.joinToString(","),
                    talkedNpcsCsv = talkedNpcs.joinToString(","),
                    enemiesDefeated = s.enemiesDefeated,
                    headshots = s.headshots
                )
            )
            repository.saveAllMissions(
                s.missions.map {
                    MissionProgressEntity(
                        missionId = it.id, progress = it.progress, completed = it.completed, rewardClaimed = it.rewardClaimed
                    )
                }
            )
        }
    }

    private fun reloadSpeedFactor(energyLevel: Int): Float =
        (1f - (energyLevel - 1) * 0.06f).coerceAtLeast(0.45f)

    private fun computeMaxHp(level: Int, vitalityLevel: Int): Float =
        STANDARD_MAX_HP + (level - 1) * 10f + (vitalityLevel - 1) * 12f

    private fun computeMaxArmor(vitalityLevel: Int): Float =
        100f + (vitalityLevel - 1) * 15f

    private fun xpRequiredFor(level: Int): Int = 120 + (level - 1) * 75

    override fun onCleared() {
        super.onCleared()
        gameLoop.stop()
        soundManager.release()
        persistCurrentState()
    }

    companion object {
        fun factory(
            repository: GameRepository,
            soundManagerProvider: () -> GameSoundManager = { NoOpSoundManager }
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return GameViewModel(repository, soundManagerProvider()) as T
                }
            }
    }
}
