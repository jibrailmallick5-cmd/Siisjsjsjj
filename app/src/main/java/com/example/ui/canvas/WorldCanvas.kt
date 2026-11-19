package com.example.ui.canvas

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.game.AllyState
import com.example.game.BackpackStyle
import com.example.game.BiomeType
import com.example.game.CustomizationCatalog
import com.example.game.EnemyState
import com.example.game.GameLoop
import com.example.game.GameUiState
import com.example.game.GroundLootItem
import com.example.game.InteractableKind
import com.example.game.LootKind
import com.example.game.PropKind
import com.example.game.SpawnCageBounds
import com.example.game.TacticalOutfit
import com.example.game.WeaponDef
import com.example.game.WorldInteractable
import com.example.game.WorldProp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun WorldCanvas(
    stateProvider: () -> GameUiState,
    waypointProvider: (GameUiState) -> Pair<Float, Float>?,
    gameLoop: GameLoop,
    onAttachDrawHandler: ((DrawScope.() -> Unit)?) -> Unit,
    modifier: Modifier = Modifier
) {
    val labelPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            textSize = 23f
            setShadowLayer(4f, 0f, 1.5f, android.graphics.Color.BLACK)
        }
    }
    val dmgPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            textSize = 30f
            setShadowLayer(6f, 0f, 2f, android.graphics.Color.BLACK)
        }
    }
    val scratch = remember { RenderScratch() }

    DisposableEffect(gameLoop, stateProvider, waypointProvider) {
        onAttachDrawHandler {
            val state = stateProvider()
            render3DTacticalWorld(
                state = state,
                waypointCoords = waypointProvider(state),
                labelPaint = labelPaint,
                dmgPaint = dmgPaint,
                scratch = scratch
            )
        }
        onDispose {
            onAttachDrawHandler(null)
        }
    }

    gameLoop.GameCanvas(modifier = modifier.fillMaxSize())
}

private const val KIND_PROP = 0
private const val KIND_LOOT = 1
private const val KIND_INTERACTABLE = 2
private const val KIND_ALLY = 3
private const val KIND_ENEMY = 4
private const val KIND_PLAYER = 5

private class RenderEntry(
    var depth: Float = 0f,
    var kind: Int = 0,
    var index: Int = 0
)

private class RenderScratch {
    val cam = Camera3D()
    val path = Path()
    val entryPool = ArrayList<RenderEntry>(256)
    val activeEntries = ArrayList<RenderEntry>(256)
    val depthComparator = Comparator<RenderEntry> { a, b -> b.depth.compareTo(a.depth) }

    // 8 projected corners for drawBox3D: x0..x7, y0..y7, valid0..valid7
    val bx = FloatArray(8)
    val by = FloatArray(8)
    val bValid = BooleanArray(8)

    fun resetEntries() {
        activeEntries.clear()
    }

    fun addEntry(depth: Float, kind: Int, index: Int) {
        val poolIdx = activeEntries.size
        val entry = if (poolIdx < entryPool.size) {
            entryPool[poolIdx]
        } else {
            RenderEntry().also { entryPool.add(it) }
        }
        entry.depth = depth
        entry.kind = kind
        entry.index = index
        activeEntries.add(entry)
    }
}

private class Camera3D {
    var camX: Float = 0f
    var camY: Float = 0f
    var camZ: Float = 0f
    var cosYaw: Float = 1f
    var sinYaw: Float = 0f
    var cosPitch: Float = 1f
    var sinPitch: Float = 0f
    var focal: Float = 600f
    var halfW: Float = 540f
    var halfH: Float = 360f
    var fogColor: Color = Color.Gray
    var drawDistance: Float = 1200f

    // Inline camera-space scratch outputs to avoid Triple allocations
    var outRx: Float = 0f
    var outRy: Float = 0f
    var outRz: Float = 0f

    // Inline screen-space scratch outputs to avoid Offset allocations
    var screenX: Float = 0f
    var screenY: Float = 0f

    fun toCameraSpaceInto(wx: Float, wy: Float, wz: Float) {
        val dx = wx - camX
        val dy = wy - camY
        val dz = wz - camZ

        val forward = dx * cosYaw + dy * sinYaw
        outRx = -dx * sinYaw + dy * cosYaw
        outRz = forward * cosPitch + dz * sinPitch
        outRy = dz * cosPitch - forward * sinPitch
    }

    fun projectInto(wx: Float, wy: Float, wz: Float): Boolean {
        toCameraSpaceInto(wx, wy, wz)
        if (outRz <= 2.5f) return false
        val scale = focal / outRz
        screenX = halfW + outRx * scale
        screenY = halfH - outRy * scale
        return true
    }

    fun project(wx: Float, wy: Float, wz: Float): Offset? {
        return if (projectInto(wx, wy, wz)) Offset(screenX, screenY) else null
    }

    fun fogBlend(base: Color, depth: Float): Color {
        val start = drawDistance * 0.30f
        if (depth <= start) return base
        val t = ((depth - start) / (drawDistance * 0.70f)).coerceIn(0f, 0.90f)
        return lerp(base, fogColor, t)
    }
}

private fun DrawScope.render3DTacticalWorld(
    state: GameUiState,
    waypointCoords: Pair<Float, Float>?,
    labelPaint: Paint,
    dmgPaint: Paint,
    scratch: RenderScratch
) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return
    val nightFactor = computeNightFactor(state.timeOfDay)

    val shakeX = if (state.screenShake > 0.2f) (Random.nextFloat() - 0.5f) * state.screenShake * 0.45f else 0f
    val shakeY = if (state.screenShake > 0.2f) (Random.nextFloat() - 0.5f) * state.screenShake * 0.45f else 0f

    val effectivePitch = (state.cameraPitch + state.recoilPitch).coerceIn(-0.30f, 0.28f)
    val bobZ = if (state.isMoving && state.isGrounded) sin(state.walkPhase * 2f) * (if (state.isSprinting) 1.5f else 0.7f) else 0f

    val distBehind = if (state.isScoped) 12f else 94f
    val shoulder = if (state.isScoped) 4f else 21f
    val cosYaw = cos(state.cameraYaw)
    val sinYaw = sin(state.cameraYaw)
    val camX = state.playerX - cosYaw * distBehind - sinYaw * shoulder + shakeX
    val camY = state.playerY - sinYaw * distBehind + cosYaw * shoulder + shakeY
    val camZ = state.playerZ + 45f + bobZ

    val baseFocal = minOf(w, h) * 0.88f
    val focal = when {
        state.isScoped -> state.activeWeapon.scopeZoomFov * (minOf(w, h) / 720f)
        state.isSprinting -> baseFocal * 0.91f
        else -> baseFocal
    }

    val fogColor = lerp(
        Color(0xFFD6B995),
        Color(0xFF0E1729),
        nightFactor
    )

    val cam = scratch.cam
    cam.camX = camX
    cam.camY = camY
    cam.camZ = camZ
    cam.cosYaw = cosYaw
    cam.sinYaw = sinYaw
    cam.cosPitch = cos(effectivePitch)
    cam.sinPitch = sin(effectivePitch)
    cam.focal = focal
    cam.halfW = w * 0.5f
    cam.halfH = h * 0.5f
    cam.fogColor = fogColor
    cam.drawDistance = state.graphicsQuality.drawDistance

    // 1. Sky, Sun & Parallax Mountain Range
    val horizonY = (cam.halfH + cam.sinPitch / cam.cosPitch * focal).coerceIn(-h * 0.4f, h * 1.4f)
    drawSkyAndMountains(w, h, horizonY, state.cameraYaw, nightFactor, fogColor, scratch.path)

    // 2. 3D Perspective Ground, Paved Courtyard & Roads
    drawGroundPlane(w, h, horizonY, state, cam, nightFactor, scratch.path)

    // 3. Circular Shrinking Safe Zone Wall (3D Glowing Storm Ring)
    drawSafeZoneRing3D(state.zoneCenterX, state.zoneCenterY, state.zoneRadius, state.timeOfDay, cam, scratch.path)

    // 4. Frustum-Culled & Depth-Sorted 3D World Objects (Zero heap allocations!)
    scratch.resetEntries()
    val maxDist = cam.drawDistance
    val maxDistSq = maxDist * maxDist
    val horizSlope = (cam.halfW / focal) + 0.25f

    val props = state.props
    for (i in props.indices) {
        val prop = props[i]
        val dx = prop.x - camX
        val dy = prop.y - camY
        if (dx * dx + dy * dy > maxDistSq) continue
        cam.toCameraSpaceInto(prop.x, prop.y, 0f)
        val rz = cam.outRz
        if (rz > -prop.collisionRadius && abs(cam.outRx) <= maxOf(rz, 90f) * horizSlope + prop.collisionRadius * 1.9f) {
            scratch.addEntry(rz, KIND_PROP, i)
        }
    }

    val loots = state.groundLoot
    for (i in loots.indices) {
        val loot = loots[i]
        if (loot.pickedUp) continue
        val dx = loot.x - camX
        val dy = loot.y - camY
        if (dx * dx + dy * dy > maxDistSq) continue
        cam.toCameraSpaceInto(loot.x, loot.y, 0f)
        val rz = cam.outRz
        if (rz > 4f && abs(cam.outRx) <= rz * horizSlope + 32f) {
            scratch.addEntry(rz, KIND_LOOT, i)
        }
    }

    val interactables = state.interactables
    for (i in interactables.indices) {
        val item = interactables[i]
        if ((item.kind == InteractableKind.AIRDROP_CRATE || item.kind == InteractableKind.AMMO_BOX) && item.activated) continue
        val dx = item.x - camX
        val dy = item.y - camY
        if (dx * dx + dy * dy > maxDistSq) continue
        cam.toCameraSpaceInto(item.x, item.y, 0f)
        val rz = cam.outRz
        if (rz > 5f && abs(cam.outRx) <= rz * horizSlope + 60f) {
            scratch.addEntry(rz, KIND_INTERACTABLE, i)
        }
    }

    val allies = state.allies
    for (i in allies.indices) {
        val ally = allies[i]
        if (!ally.isAlive) continue
        val dx = ally.x - camX
        val dy = ally.y - camY
        if (dx * dx + dy * dy > maxDistSq) continue
        cam.toCameraSpaceInto(ally.x, ally.y, ally.z)
        val rz = cam.outRz
        if (rz > 6f && abs(cam.outRx) <= rz * horizSlope + 35f) {
            scratch.addEntry(rz, KIND_ALLY, i)
        }
    }

    val enemies = state.enemies
    for (i in enemies.indices) {
        val enemy = enemies[i]
        if (!enemy.isAlive) continue
        val dx = enemy.x - camX
        val dy = enemy.y - camY
        if (dx * dx + dy * dy > maxDistSq) continue
        cam.toCameraSpaceInto(enemy.x, enemy.y, enemy.z)
        val rz = cam.outRz
        if (rz > 6f && abs(cam.outRx) <= rz * horizSlope + 40f) {
            scratch.addEntry(rz, KIND_ENEMY, i)
        }
    }

    if (state.isPlayerAlive && !state.isScoped) {
        cam.toCameraSpaceInto(state.playerX, state.playerY, state.playerZ)
        if (cam.outRz > 6f) {
            scratch.addEntry(cam.outRz, KIND_PLAYER, 0)
        }
    }

    val activeEntries = scratch.activeEntries
    activeEntries.sortWith(scratch.depthComparator)
    for (i in activeEntries.indices) {
        val entry = activeEntries[i]
        when (entry.kind) {
            KIND_PROP -> drawProp3D(props[entry.index], cam, nightFactor, scratch)
            KIND_LOOT -> {
                val loot = loots[entry.index]
                drawGroundLoot3D(loot, cam, state.timeOfDay, state.nearbyLoot?.id == loot.id, labelPaint, scratch)
            }
            KIND_INTERACTABLE -> {
                val item = interactables[entry.index]
                drawInteractable3D(item, cam, state.timeOfDay, state.nearbyInteractable?.id == item.id, labelPaint, scratch)
            }
            KIND_ALLY -> drawAlly3D(allies[entry.index], cam, labelPaint)
            KIND_ENEMY -> drawEnemy3D(enemies[entry.index], cam, labelPaint)
            KIND_PLAYER -> drawPlayerCharacter3D(state, cam)
        }
    }

    // 5. 15-Second Spawn Barrier Cage Walls (surrounding all 4 sides)
    state.allyCage?.let { drawSpawnCage3D(it, state.timeOfDay, cam, scratch.path) }
    state.enemyCage?.let { drawSpawnCage3D(it, state.timeOfDay, cam, scratch.path) }

    // 6. 3D Bullet Tracers, Projectiles & Particles
    val tracers = state.tracers
    for (i in tracers.indices) {
        val tracer = tracers[i]
        if (!cam.projectInto(tracer.startX, tracer.startY, tracer.startZ)) continue
        val x1 = cam.screenX
        val y1 = cam.screenY
        if (!cam.projectInto(tracer.endX, tracer.endY, tracer.endZ)) continue
        val p1 = Offset(x1, y1)
        val p2 = Offset(cam.screenX, cam.screenY)
        val alpha = (tracer.life / tracer.maxLife).coerceIn(0f, 1f)
        drawLine(tracer.color.copy(alpha = alpha * 0.48f), p1, p2, strokeWidth = 6f, cap = StrokeCap.Round)
        drawLine(Color.White.copy(alpha = alpha), p1, p2, strokeWidth = 2.4f, cap = StrokeCap.Round)
    }

    val projectiles = state.enemyProjectiles
    for (i in projectiles.indices) {
        val proj = projectiles[i]
        if (!cam.projectInto(proj.x, proj.y, proj.z)) continue
        val r = (3.5f * cam.focal / cam.outRz).coerceIn(2.5f, 10f)
        val pos = Offset(cam.screenX, cam.screenY)
        val glowColor = if (proj.fromAlly) Color(0xFF00E5FF) else Color(0xFFFF4D6D)
        drawCircle(glowColor.copy(alpha = 0.45f), radius = r * 2f, center = pos)
        drawCircle(Color(0xFFFFD166), radius = r, center = pos)
    }

    val particles = state.particles
    for (i in particles.indices) {
        val pt = particles[i]
        if (!cam.projectInto(pt.x, pt.y, pt.z)) continue
        val alpha = (pt.life / pt.maxLife).coerceIn(0f, 1f)
        val rad = (pt.size * cam.focal / cam.outRz * 0.6f).coerceIn(1.5f, 12f)
        drawCircle(pt.color.copy(alpha = alpha), radius = rad, center = Offset(cam.screenX, cam.screenY))
    }

    // 7. Floating Damage & Headshot Numbers
    val floating = state.floatingTexts
    for (i in floating.indices) {
        val ft = floating[i]
        if (!cam.projectInto(ft.x, ft.y, ft.z)) continue
        val alpha = ((ft.life / ft.maxLife) * 255).toInt().coerceIn(0, 255)
        dmgPaint.color = ft.color.copy(alpha = alpha / 255f).toArgb()
        drawContext.canvas.nativeCanvas.drawText(ft.text, cam.screenX, cam.screenY, dmgPaint)
    }

    // 8. Active Waypoint Beacon
    waypointCoords?.let { (wx, wy) ->
        if (cam.projectInto(wx, wy, 4f)) {
            val gx = cam.screenX
            val gy = cam.screenY
            if (cam.projectInto(wx, wy, 240f)) {
                val gPos = Offset(gx, gy)
                drawLine(Color(0x55FFB703), gPos, Offset(cam.screenX, cam.screenY), strokeWidth = 4f, cap = StrokeCap.Round)
                drawCircle(Color(0xBBFFB703), radius = 9f, center = gPos, style = Stroke(2.5f))
            }
        }
    }

    // 9. Tactical Reticle, Hitmarker, Zone Storm Vignette & Scope Overlay
    drawTacticalReticleOverlay(w, h, state)
}

private fun DrawScope.drawSafeZoneRing3D(
    cx: Float,
    cy: Float,
    radius: Float,
    timeOfDay: Float,
    cam: Camera3D,
    reusablePath: Path
) {
    val segments = 24
    val wallH = 165f
    val pulse = 0.24f + sin(timeOfDay * 60f) * 0.05f
    val wallColor = Color(0xFF00B4D8).copy(alpha = pulse)
    val rimColor = Color(0xFF38BDF8).copy(alpha = 0.85f)
    val stepAngle = (2f * PI.toFloat()) / segments

    for (i in 0 until segments) {
        val a1 = i * stepAngle
        val a2 = (i + 1) * stepAngle
        val x1 = cx + cos(a1) * radius
        val y1 = cy + sin(a1) * radius
        val x2 = cx + cos(a2) * radius
        val y2 = cy + sin(a2) * radius

        if (!cam.projectInto(x1, y1, 0f)) continue
        val b1x = cam.screenX; val b1y = cam.screenY
        if (!cam.projectInto(x2, y2, 0f)) continue
        val b2x = cam.screenX; val b2y = cam.screenY

        val hasT1 = cam.projectInto(x1, y1, wallH)
        val t1x = cam.screenX; val t1y = cam.screenY
        val hasT2 = cam.projectInto(x2, y2, wallH)
        val t2x = cam.screenX; val t2y = cam.screenY

        if (hasT1 && hasT2) {
            reusablePath.reset()
            reusablePath.moveTo(b1x, b1y)
            reusablePath.lineTo(b2x, b2y)
            reusablePath.lineTo(t2x, t2y)
            reusablePath.lineTo(t1x, t1y)
            reusablePath.close()
            drawPath(reusablePath, wallColor)
        }
        drawLine(rimColor, Offset(b1x, b1y), Offset(b2x, b2y), strokeWidth = 4f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawSpawnCage3D(
    cage: SpawnCageBounds,
    timeOfDay: Float,
    cam: Camera3D,
    reusablePath: Path
) {
    val h = 72f
    val pulse = 0.24f + sin(timeOfDay * 90f) * 0.06f
    val wallFill = cage.accentColor.copy(alpha = pulse)
    val edgeColor = cage.accentColor.copy(alpha = 0.9f)

    for (i in 0 until 4) {
        val x1: Float
        val y1: Float
        val x2: Float
        val y2: Float
        when (i) {
            0 -> { x1 = cage.minX; y1 = cage.minY; x2 = cage.maxX; y2 = cage.minY }
            1 -> { x1 = cage.maxX; y1 = cage.minY; x2 = cage.maxX; y2 = cage.maxY }
            2 -> { x1 = cage.maxX; y1 = cage.maxY; x2 = cage.minX; y2 = cage.maxY }
            else -> { x1 = cage.minX; y1 = cage.maxY; x2 = cage.minX; y2 = cage.minY }
        }
        if (!cam.projectInto(x1, y1, 0f)) continue
        val b1x = cam.screenX; val b1y = cam.screenY
        if (!cam.projectInto(x2, y2, 0f)) continue
        val b2x = cam.screenX; val b2y = cam.screenY
        if (!cam.projectInto(x1, y1, h)) continue
        val t1x = cam.screenX; val t1y = cam.screenY
        if (!cam.projectInto(x2, y2, h)) continue
        val t2x = cam.screenX; val t2y = cam.screenY

        reusablePath.reset()
        reusablePath.moveTo(b1x, b1y)
        reusablePath.lineTo(b2x, b2y)
        reusablePath.lineTo(t2x, t2y)
        reusablePath.lineTo(t1x, t1y)
        reusablePath.close()
        drawPath(reusablePath, wallFill)

        val b1 = Offset(b1x, b1y)
        val b2 = Offset(b2x, b2y)
        val t1 = Offset(t1x, t1y)
        val t2 = Offset(t2x, t2y)
        drawLine(edgeColor, b1, b2, strokeWidth = 3f)
        drawLine(edgeColor, t1, t2, strokeWidth = 3f)
        drawLine(edgeColor, b1, t1, strokeWidth = 3.5f)
        if (cam.projectInto(x1, y1, h * 0.5f)) {
            val m1 = Offset(cam.screenX, cam.screenY)
            if (cam.projectInto(x2, y2, h * 0.5f)) {
                drawLine(Color.White.copy(alpha = 0.5f), m1, Offset(cam.screenX, cam.screenY), strokeWidth = 2f)
            }
        }
    }
}

private fun DrawScope.drawGroundLoot3D(
    loot: GroundLootItem,
    cam: Camera3D,
    timeOfDay: Float,
    highlighted: Boolean,
    labelPaint: Paint,
    scratch: RenderScratch
) {
    val depth = hypot(loot.x - cam.camX, loot.y - cam.camY)
    val bob = sin(timeOfDay * 100f) * 2.5f + 5f
    if (!cam.projectInto(loot.x, loot.y, 1f)) return
    val baseX = cam.screenX; val baseY = cam.screenY
    if (!cam.projectInto(loot.x, loot.y, bob)) return
    val itemX = cam.screenX; val itemY = cam.screenY
    val rad = (14f * cam.focal / depth).coerceIn(6f, 26f)

    drawOval(
        color = loot.color.copy(alpha = if (highlighted) 0.55f else 0.25f),
        topLeft = Offset(baseX - rad, baseY - rad * 0.38f),
        size = Size(rad * 2f, rad * 0.76f)
    )
    drawOval(
        color = loot.color,
        topLeft = Offset(baseX - rad, baseY - rad * 0.38f),
        size = Size(rad * 2f, rad * 0.76f),
        style = Stroke(if (highlighted) 3f else 2f)
    )

    when (loot.kind) {
        LootKind.WEAPON_M4A1, LootKind.WEAPON_MP40, LootKind.WEAPON_AK47, LootKind.WEAPON_AWM -> {
            drawLine(
                color = Color(0xFF0F172A),
                start = Offset(itemX - rad * 0.9f, itemY + rad * 0.15f),
                end = Offset(itemX + rad * 0.9f, itemY - rad * 0.15f),
                strokeWidth = (rad * 0.32f).coerceIn(3f, 9f),
                cap = StrokeCap.Round
            )
            drawLine(
                color = loot.color,
                start = Offset(itemX - rad * 0.4f, itemY + rad * 0.05f),
                end = Offset(itemX + rad * 0.5f, itemY - rad * 0.08f),
                strokeWidth = (rad * 0.16f).coerceIn(2f, 5f),
                cap = StrokeCap.Round
            )
        }
        LootKind.ARMOR_VEST -> {
            drawBox3D(loot.x, loot.y, 1f, 8f, 6f, 10f, loot.color, Color(0xFF1E293B), Color(0xFF0F172A), cam, depth, scratch)
        }
        LootKind.MEDKIT_PACK -> {
            drawBox3D(loot.x, loot.y, 1f, 8f, 6f, 8f, Color.White, Color(0xFF10B981), Color(0xFF059669), cam, depth, scratch)
        }
        LootKind.AMMO_CRATE -> {
            drawBox3D(loot.x, loot.y, 1f, 9f, 7f, 8f, Color(0xFFFDE047), Color(0xFF3F6212), Color(0xFF365314), cam, depth, scratch)
        }
    }

    if ((depth < 260f || highlighted) && cam.projectInto(loot.x, loot.y, 24f)) {
        labelPaint.color = loot.color.toArgb()
        drawContext.canvas.nativeCanvas.drawText(loot.labelEn, cam.screenX, cam.screenY, labelPaint)
    }
}

private fun DrawScope.drawSkyAndMountains(
    w: Float,
    h: Float,
    horizonY: Float,
    cameraYaw: Float,
    nightFactor: Float,
    fogColor: Color,
    reusablePath: Path
) {
    val topSky = lerp(Color(0xFF1D4E89), Color(0xFF040814), nightFactor)
    val midSky = lerp(Color(0xFF6BA3D6), Color(0xFF0B1528), nightFactor)
    drawRect(
        brush = Brush.verticalGradient(listOf(topSky, midSky, fogColor), startY = 0f, endY = horizonY.coerceAtLeast(10f)),
        topLeft = Offset.Zero,
        size = Size(w, horizonY.coerceAtLeast(0f))
    )

    val sunYaw = -0.8f
    var relYaw = (sunYaw - cameraYaw) % (2f * PI.toFloat())
    if (relYaw > PI) relYaw -= 2f * PI.toFloat()
    if (relYaw < -PI) relYaw += 2f * PI.toFloat()
    if (abs(relYaw) < 1.3f && horizonY > 0f) {
        val sunX = w * 0.5f + relYaw * w * 0.55f
        val sunY = (horizonY - h * 0.24f).coerceAtLeast(30f)
        val sunColor = lerp(Color(0xFFFFE082), Color(0xFFCBD5E1), nightFactor)
        drawCircle(
            brush = Brush.radialGradient(listOf(sunColor.copy(alpha = 0.7f), sunColor.copy(alpha = 0.16f), Color.Transparent), Offset(sunX, sunY), 120f),
            radius = 120f, center = Offset(sunX, sunY)
        )
        drawCircle(Color.White.copy(alpha = 0.85f), radius = 18f, center = Offset(sunX, sunY))
    }

    if (horizonY > 0f) {
        val shift = (cameraYaw * 240f) % w
        val mtColorFar = lerp(Color(0xFF475C70), Color(0xFF0C1424), nightFactor)
        val mtColorNear = lerp(Color(0xFF364857), Color(0xFF08101C), nightFactor)
        drawMountainBand(w, horizonY, shift * 0.6f, 65f, 0.011f, mtColorFar, reusablePath)
        drawMountainBand(w, horizonY, shift, 42f, 0.019f, mtColorNear, reusablePath)
    }
}

private fun DrawScope.drawMountainBand(
    w: Float,
    horizonY: Float,
    phaseOffset: Float,
    maxHeight: Float,
    freq: Float,
    color: Color,
    reusablePath: Path
) {
    reusablePath.reset()
    reusablePath.moveTo(0f, horizonY)
    val step = w / 16f
    var x = 0f
    while (x <= w + step) {
        val sample = x + phaseOffset
        val ridge = (sin(sample * freq) * 0.5f + sin(sample * freq * 2.3f) * 0.3f + 0.8f) * maxHeight
        reusablePath.lineTo(x, horizonY - ridge.coerceAtLeast(8f))
        x += step
    }
    reusablePath.lineTo(w, horizonY)
    reusablePath.close()
    drawPath(reusablePath, color)
}

private fun DrawScope.drawGroundPlane(
    w: Float,
    h: Float,
    horizonY: Float,
    state: GameUiState,
    cam: Camera3D,
    nightFactor: Float,
    reusablePath: Path
) {
    val nearGround = when (state.currentBiome) {
        BiomeType.CLOCKTOWER_VALLEY -> lerp(Color(0xFF4B5742), Color(0xFF151E19), nightFactor)
        BiomeType.PINE_FOREST -> lerp(Color(0xFF2B422E), Color(0xFF0F1B16), nightFactor)
        BiomeType.DESERT_OUTPOST -> lerp(Color(0xFF9C7B4B), Color(0xFF2B2216), nightFactor)
        BiomeType.MILITARY_PEAK -> lerp(Color(0xFF47484C), Color(0xFF181A20), nightFactor)
    }

    val top = horizonY.coerceIn(0f, h)
    drawRect(
        brush = Brush.verticalGradient(listOf(cam.fogColor, nearGround), startY = top, endY = h),
        topLeft = Offset(0f, top),
        size = Size(w, (h - top).coerceAtLeast(0f))
    )

    val gridSize = 260f
    val startX = ((state.playerX - 780f) / gridSize).toInt() * gridSize
    val endX = state.playerX + 780f
    val startY = ((state.playerY - 780f) / gridSize).toInt() * gridSize
    val endY = state.playerY + 780f
    val gridColor = Color.Black.copy(alpha = 0.11f)

    var gx = startX
    while (gx <= endX) {
        if (cam.projectInto(gx, startY, 0f)) {
            val x1 = cam.screenX; val y1 = cam.screenY
            if (cam.projectInto(gx, endY, 0f)) {
                drawLine(gridColor, Offset(x1, y1), Offset(cam.screenX, cam.screenY), strokeWidth = 1.5f)
            }
        }
        gx += gridSize
    }
    var gy = startY
    while (gy <= endY) {
        if (cam.projectInto(startX, gy, 0f)) {
            val x1 = cam.screenX; val y1 = cam.screenY
            if (cam.projectInto(endX, gy, 0f)) {
                drawLine(gridColor, Offset(x1, y1), Offset(cam.screenX, cam.screenY), strokeWidth = 1.5f)
            }
        }
        gy += gridSize
    }

    drawRoadSegment3D(1600f, 460f, 1600f, 2600f, 65f, cam, nightFactor, reusablePath)
    drawRoadSegment3D(620f, 1800f, 2650f, 1800f, 58f, cam, nightFactor, reusablePath)
}

private fun DrawScope.drawRoadSegment3D(
    x1: Float, y1: Float, x2: Float, y2: Float, halfWidth: Float, cam: Camera3D, nightFactor: Float, reusablePath: Path
) {
    val step = 280f
    val dx = x2 - x1
    val dy = y2 - y1
    val len = hypot(dx, dy).coerceAtLeast(1f)
    val ux = dx / len
    val uy = dy / len
    val px = -uy * halfWidth
    val py = ux * halfWidth

    var d = 0f
    while (d < len) {
        val nextD = minOf(d + step, len)
        val ax = x1 + ux * d
        val ay = y1 + uy * d
        val bx = x1 + ux * nextD
        val by = y1 + uy * nextD
        val midDist = hypot((ax + bx) * 0.5f - cam.camX, (ay + by) * 0.5f - cam.camY)
        if (midDist < cam.drawDistance) {
            if (cam.projectInto(ax - px, ay - py, 0.3f)) {
                val c1x = cam.screenX; val c1y = cam.screenY
                if (cam.projectInto(ax + px, ay + py, 0.3f)) {
                    val c2x = cam.screenX; val c2y = cam.screenY
                    if (cam.projectInto(bx + px, by + py, 0.3f)) {
                        val c3x = cam.screenX; val c3y = cam.screenY
                        if (cam.projectInto(bx - px, by - py, 0.3f)) {
                            val c4x = cam.screenX; val c4y = cam.screenY
                            val asphalt = cam.fogBlend(lerp(Color(0xFF3B4048), Color(0xFF14181F), nightFactor), midDist)
                            reusablePath.reset()
                            reusablePath.moveTo(c1x, c1y)
                            reusablePath.lineTo(c2x, c2y)
                            reusablePath.lineTo(c3x, c3y)
                            reusablePath.lineTo(c4x, c4y)
                            reusablePath.close()
                            drawPath(reusablePath, asphalt)

                            if (midDist < 880f && cam.projectInto(ax + ux * 45f, ay + uy * 45f, 0.5f)) {
                                val s1x = cam.screenX; val s1y = cam.screenY
                                if (cam.projectInto(bx - ux * 45f, by - uy * 45f, 0.5f)) {
                                    drawLine(cam.fogBlend(Color(0xFFD97706), midDist), Offset(s1x, s1y), Offset(cam.screenX, cam.screenY), strokeWidth = 3f)
                                }
                            }
                        }
                    }
                }
            }
        }
        d = nextD
    }
}

private fun DrawScope.drawProp3D(prop: WorldProp, cam: Camera3D, nightFactor: Float, scratch: RenderScratch) {
    val depth = hypot(prop.x - cam.camX, prop.y - cam.camY)
    val darkFactor = 1f - nightFactor * 0.55f

    when (prop.kind) {
        PropKind.PINE_TREE -> {
            drawBox3D(
                prop.x, prop.y, 0f, 5f, 5f, prop.height * 0.35f,
                Color(0xFF4A3222), Color(0xFF382418), Color(0xFF5C402E), cam, depth, scratch
            )
            val colLight = cam.fogBlend(prop.secondaryColor, depth)
            val colDark = cam.fogBlend(prop.primaryColor, depth)
            val tiers = if (depth > 800f) 1 else 2
            val path = scratch.path
            for (tier in 0..tiers) {
                val baseZ = prop.height * (0.28f + tier * 0.23f)
                val topZ = baseZ + prop.height * 0.34f
                val rad = prop.halfW * (1.25f - tier * 0.28f)
                if (!cam.projectInto(prop.x, prop.y, topZ)) continue
                val ax = cam.screenX; val ay = cam.screenY
                if (!cam.projectInto(prop.x - rad, prop.y, baseZ)) continue
                val lx = cam.screenX; val ly = cam.screenY
                if (!cam.projectInto(prop.x + rad, prop.y, baseZ)) continue
                val rx = cam.screenX; val ry = cam.screenY
                if (!cam.projectInto(prop.x, prop.y + rad, baseZ)) continue
                val fx = cam.screenX; val fy = cam.screenY
                if (!cam.projectInto(prop.x, prop.y - rad, baseZ)) continue
                val bx = cam.screenX; val by = cam.screenY

                path.reset(); path.moveTo(ax, ay); path.lineTo(lx, ly); path.lineTo(fx, fy); path.close(); drawPath(path, colDark)
                path.reset(); path.moveTo(ax, ay); path.lineTo(fx, fy); path.lineTo(rx, ry); path.close(); drawPath(path, colLight)
                path.reset(); path.moveTo(ax, ay); path.lineTo(rx, ry); path.lineTo(bx, by); path.close(); drawPath(path, colLight)
                path.reset(); path.moveTo(ax, ay); path.lineTo(bx, by); path.lineTo(lx, ly); path.close(); drawPath(path, colDark)
            }
        }

        PropKind.WATCHTOWER -> {
            drawBox3D(prop.x, prop.y, 0f, prop.halfW * 0.72f, prop.halfD * 0.72f, prop.height * 0.68f, prop.secondaryColor, prop.primaryColor, prop.primaryColor, cam, depth, scratch)
            drawBox3D(prop.x, prop.y, prop.height * 0.68f, prop.halfW * 1.15f, prop.halfD * 1.15f, prop.height * 0.32f, Color(0xFF64748B), Color(0xFF334155), Color(0xFF475569), cam, depth, scratch)
        }

        PropKind.BUILDING_HOUSE -> {
            drawBox3D(prop.x, prop.y, 0f, prop.halfW, prop.halfD, prop.height, prop.primaryColor, prop.secondaryColor, lerp(prop.secondaryColor, Color.Black, 0.25f), cam, depth, scratch)
            if (depth < 920f) {
                drawBox3D(prop.x, prop.y, prop.height * 0.48f, prop.halfW + 3.5f, prop.halfD + 3.5f, 5f, Color(0xFF475569), Color(0xFF334155), Color(0xFF1E293B), cam, depth, scratch)
            }
            drawBox3D(prop.x, prop.y, prop.height, prop.halfW + 5f, prop.halfD + 5f, 10f, Color(0xFF64748B), Color(0xFF334155), Color(0xFF1E293B), cam, depth, scratch)
            if (depth < 720f) {
                drawHouseDetails3D(prop, cam, depth, scratch.path)
            }
        }

        else -> {
            val shadeLeft = Color(prop.secondaryColor.red * darkFactor, prop.secondaryColor.green * darkFactor, prop.secondaryColor.blue * darkFactor)
            val shadeTop = lerp(prop.primaryColor, Color.White, 0.14f)
            drawBox3D(prop.x, prop.y, 0f, prop.halfW, prop.halfD, prop.height, shadeTop, prop.primaryColor, shadeLeft, cam, depth, scratch)
        }
    }
}

private fun DrawScope.drawHouseDetails3D(prop: WorldProp, cam: Camera3D, depth: Float, reusablePath: Path) {
    val glassCol = cam.fogBlend(Color(0xFF1E293B), depth)
    val frameCol = cam.fogBlend(Color(0xFF38BDF8).copy(alpha = 0.32f), depth)
    val yFace = if (cam.camY < prop.y) prop.y - prop.halfD - 0.6f else prop.y + prop.halfD + 0.6f

    for (side in -1..1 step 2) {
        val wxOffset = side * prop.halfW * 0.45f
        if (!cam.projectInto(prop.x + wxOffset - 14f, yFace, prop.height * 0.6f)) continue
        val w0x = cam.screenX; val w0y = cam.screenY
        if (!cam.projectInto(prop.x + wxOffset + 14f, yFace, prop.height * 0.6f)) continue
        val w1x = cam.screenX; val w1y = cam.screenY
        if (!cam.projectInto(prop.x + wxOffset + 14f, yFace, prop.height * 0.82f)) continue
        val w2x = cam.screenX; val w2y = cam.screenY
        if (!cam.projectInto(prop.x + wxOffset - 14f, yFace, prop.height * 0.82f)) continue
        val w3x = cam.screenX; val w3y = cam.screenY
        drawQuadCoords(reusablePath, w0x, w0y, w1x, w1y, w2x, w2y, w3x, w3y, glassCol)
        drawLine(frameCol, Offset(w0x, w0y), Offset(w2x, w2y), strokeWidth = 1.5f)
    }

    if (cam.projectInto(prop.x - 15f, yFace, 0f)) {
        val d0x = cam.screenX; val d0y = cam.screenY
        if (cam.projectInto(prop.x + 15f, yFace, 0f)) {
            val d1x = cam.screenX; val d1y = cam.screenY
            if (cam.projectInto(prop.x + 15f, yFace, 38f)) {
                val d2x = cam.screenX; val d2y = cam.screenY
                if (cam.projectInto(prop.x - 15f, yFace, 38f)) {
                    drawQuadCoords(reusablePath, d0x, d0y, d1x, d1y, d2x, d2y, cam.screenX, cam.screenY, cam.fogBlend(Color(0xFF0F172A), depth))
                }
            }
        }
    }
}

private fun DrawScope.drawBox3D(
    cx: Float, cy: Float, baseZ: Float,
    halfW: Float, halfD: Float, height: Float,
    topColor: Color, frontColor: Color, sideColor: Color,
    cam: Camera3D, depth: Float, scratch: RenderScratch
) {
    val x0 = cx - halfW
    val x1 = cx + halfW
    val y0 = cy - halfD
    val y1 = cy + halfD
    val z0 = baseZ
    val z1 = baseZ + height

    val bx = scratch.bx
    val by = scratch.by
    val bv = scratch.bValid

    // 0: (x0,y0,z0), 1: (x1,y0,z0), 2: (x1,y1,z0), 3: (x0,y1,z0)
    // 4: (x0,y0,z1), 5: (x1,y0,z1), 6: (x1,y1,z1), 7: (x0,y1,z1)
    bv[0] = cam.projectInto(x0, y0, z0); bx[0] = cam.screenX; by[0] = cam.screenY
    bv[1] = cam.projectInto(x1, y0, z0); bx[1] = cam.screenX; by[1] = cam.screenY
    bv[2] = cam.projectInto(x1, y1, z0); bx[2] = cam.screenX; by[2] = cam.screenY
    bv[3] = cam.projectInto(x0, y1, z0); bx[3] = cam.screenX; by[3] = cam.screenY
    bv[4] = cam.projectInto(x0, y0, z1); bx[4] = cam.screenX; by[4] = cam.screenY
    bv[5] = cam.projectInto(x1, y0, z1); bx[5] = cam.screenX; by[5] = cam.screenY
    bv[6] = cam.projectInto(x1, y1, z1); bx[6] = cam.screenX; by[6] = cam.screenY
    bv[7] = cam.projectInto(x0, y1, z1); bx[7] = cam.screenX; by[7] = cam.screenY

    val path = scratch.path
    val colFront = cam.fogBlend(frontColor, depth)
    val colSide = cam.fogBlend(sideColor, depth)

    if (cam.camY < y0 && bv[0] && bv[1] && bv[5] && bv[4]) {
        drawQuadCoords(path, bx[0], by[0], bx[1], by[1], bx[5], by[5], bx[4], by[4], colFront)
    }
    if (cam.camY > y1 && bv[3] && bv[2] && bv[6] && bv[7]) {
        drawQuadCoords(path, bx[2], by[2], bx[3], by[3], bx[7], by[7], bx[6], by[6], colFront)
    }
    if (cam.camX < x0 && bv[3] && bv[0] && bv[4] && bv[7]) {
        drawQuadCoords(path, bx[3], by[3], bx[0], by[0], bx[4], by[4], bx[7], by[7], colSide)
    }
    if (cam.camX > x1 && bv[1] && bv[2] && bv[6] && bv[5]) {
        drawQuadCoords(path, bx[1], by[1], bx[2], by[2], bx[6], by[6], bx[5], by[5], colSide)
    }
    if (cam.camZ > z1 && bv[4] && bv[5] && bv[6] && bv[7]) {
        drawQuadCoords(path, bx[4], by[4], bx[5], by[5], bx[6], by[6], bx[7], by[7], cam.fogBlend(topColor, depth))
    }
}

private fun DrawScope.drawQuadCoords(
    reusablePath: Path,
    x1: Float, y1: Float,
    x2: Float, y2: Float,
    x3: Float, y3: Float,
    x4: Float, y4: Float,
    color: Color
) {
    reusablePath.reset()
    reusablePath.moveTo(x1, y1)
    reusablePath.lineTo(x2, y2)
    reusablePath.lineTo(x3, y3)
    reusablePath.lineTo(x4, y4)
    reusablePath.close()
    drawPath(reusablePath, color)
}

private fun DrawScope.drawInteractable3D(
    item: WorldInteractable,
    cam: Camera3D,
    timeOfDay: Float,
    highlighted: Boolean,
    labelPaint: Paint,
    scratch: RenderScratch
) {
    val depth = hypot(item.x - cam.camX, item.y - cam.camY)
    if (!cam.projectInto(item.x, item.y, 0f)) return
    val base = Offset(cam.screenX, cam.screenY)
    val pulse = sin(timeOfDay * 65f) * 0.5f + 0.5f

    when (item.kind) {
        InteractableKind.AIRDROP_CRATE -> {
            drawBox3D(item.x, item.y, 0f, 16f, 16f, 24f, Color(0xFFFFB703), Color(0xFFDC2626), Color(0xFF991B1B), cam, depth, scratch)
            if (cam.projectInto(item.x, item.y, 24f)) {
                val top = Offset(cam.screenX, cam.screenY)
                if (cam.projectInto(item.x, item.y, 340f)) {
                    drawLine(Color(0x88FF4D6D), top, Offset(cam.screenX, cam.screenY), strokeWidth = 6f, cap = StrokeCap.Round)
                }
            }
        }
        InteractableKind.INTEL_TERMINAL -> {
            val col = if (item.activated) Color(0xFF10B981) else Color(0xFF00E5FF)
            drawBox3D(item.x, item.y, 0f, 12f, 10f, 30f, col, Color(0xFF1E293B), Color(0xFF0F172A), cam, depth, scratch)
        }
        InteractableKind.AMMO_BOX -> {
            drawBox3D(item.x, item.y, 0f, 12f, 9f, 12f, Color(0xFF34D399), Color(0xFF166534), Color(0xFF14532D), cam, depth, scratch)
        }
        InteractableKind.WAYPOINT -> {
            val rad = (48f * cam.focal / depth).coerceIn(10f, 70f)
            val col = if (item.activated) Color(0xFF00E5FF) else Color(0xFF94A3B8)
            drawOval(col.copy(alpha = 0.25f), Offset(base.x - rad, base.y - rad * 0.35f), Size(rad * 2f, rad * 0.7f))
            drawOval(col, Offset(base.x - rad, base.y - rad * 0.35f), Size(rad * 2f, rad * 0.7f), style = Stroke(3f))
        }
        InteractableKind.MEDKIT_STATION -> {
            drawBox3D(item.x, item.y, 0f, 18f, 18f, 14f, Color(0xFF10B981), Color(0xFFE2E8F0), Color(0xFFCBD5E1), cam, depth, scratch)
        }
        InteractableKind.NPC -> {
            drawHumanFigure3D(
                x = item.x, y = item.y, z = 0f,
                facingYaw = atan2(cam.camY - item.y, cam.camX - item.x),
                walkPhase = 0f, isJumping = false, heightScale = 1f,
                vestColor = Color(0xFF0F766E), pantColor = Color(0xFF1E293B), helmetColor = Color(0xFFFFB703),
                weaponColor = Color(0xFF334155), hasWeapon = false, muzzleFlash = false, hitFlash = false,
                cam = cam, depth = depth
            )
            if (cam.projectInto(item.x, item.y, 58f + pulse * 4f)) {
                labelPaint.color = android.graphics.Color.rgb(255, 195, 0)
                drawContext.canvas.nativeCanvas.drawText("!", cam.screenX, cam.screenY, labelPaint)
            }
        }
    }

    if (highlighted) {
        val ringR = (28f * cam.focal / depth).coerceIn(12f, 44f)
        drawCircle(Color(0xFFFFB703), radius = ringR, center = base, style = Stroke(3f))
    }
}

private fun DrawScope.drawAlly3D(ally: AllyState, cam: Camera3D, labelPaint: Paint) {
    val depth = hypot(ally.x - cam.camX, ally.y - cam.camY)
    drawHumanFigure3D(
        x = ally.x, y = ally.y, z = ally.z,
        facingYaw = ally.yaw, walkPhase = ally.walkPhase,
        isJumping = false, heightScale = 1f,
        vestColor = ally.vestColor, pantColor = Color(0xFF1E293B),
        helmetColor = ally.helmetColor, weaponColor = Color(0xFF0F172A),
        hasWeapon = true, muzzleFlash = ally.muzzleFlashTimer > 0f, hitFlash = ally.hitFlashTimer > 0f,
        cam = cam, depth = depth,
        backpackColor = Color(0xFF1E293B), trimColor = Color(0xFF00E5FF)
    )
    if (!cam.projectInto(ally.x, ally.y, ally.z + 54f)) return
    val topX = cam.screenX; val topY = cam.screenY
    val barW = (52f * cam.focal / depth).coerceIn(30f, 90f)
    drawRect(Color(0xAA0F172A), Offset(topX - barW / 2f, topY), Size(barW, 5f))
    drawRect(Color(0xFF00E5FF), Offset(topX - barW / 2f, topY), Size(barW * (ally.hp / ally.maxHp).coerceIn(0f, 1f), 5f))
    if (depth < 520f) {
        labelPaint.color = android.graphics.Color.rgb(0, 229, 255)
        drawContext.canvas.nativeCanvas.drawText(ally.name, topX, topY - 6f, labelPaint)
    }
}

private fun DrawScope.drawEnemy3D(enemy: EnemyState, cam: Camera3D, labelPaint: Paint) {
    val depth = hypot(enemy.x - cam.camX, enemy.y - cam.camY)
    val scale = if (enemy.kind.isBoss) 1.42f else 1f
    drawHumanFigure3D(
        x = enemy.x, y = enemy.y, z = enemy.z,
        facingYaw = enemy.yaw, walkPhase = enemy.walkPhase,
        isJumping = false, heightScale = scale,
        vestColor = enemy.kind.vestColor, pantColor = Color(0xFF1F2937),
        helmetColor = enemy.kind.helmetColor, weaponColor = Color(0xFF111827),
        hasWeapon = true, muzzleFlash = enemy.muzzleFlashTimer > 0f, hitFlash = enemy.hitFlashTimer > 0f,
        cam = cam, depth = depth,
        trimColor = Color(0xFFFF4D6D)
    )

    if (enemy.hp < enemy.maxHp || enemy.kind.isBoss) {
        if (!cam.projectInto(enemy.x, enemy.y, enemy.z + enemy.kind.height + 12f)) return
        val topX = cam.screenX; val topY = cam.screenY
        val barW = ((if (enemy.kind.isBoss) 96f else 54f) * cam.focal / depth).coerceIn(32f, 110f)
        val barH = if (enemy.kind.isBoss) 8f else 5f
        drawRect(Color(0xAA0F172A), Offset(topX - barW / 2f, topY), Size(barW, barH))
        val pct = (enemy.hp / enemy.maxHp).coerceIn(0f, 1f)
        drawRect(if (enemy.kind.isBoss) Color(0xFFFF5A1F) else Color(0xFFFF4D6D), Offset(topX - barW / 2f, topY), Size(barW * pct, barH))
        if (enemy.kind.isBoss) {
            labelPaint.color = android.graphics.Color.rgb(255, 183, 3)
            drawContext.canvas.nativeCanvas.drawText("COMMANDER BOSS", topX, topY - 8f, labelPaint)
        }
    }
}

private fun DrawScope.drawPlayerCharacter3D(state: GameUiState, cam: Camera3D) {
    val outfit: TacticalOutfit = CustomizationCatalog.outfits[state.cloakIndex]
    val weapon: WeaponDef = state.activeWeapon
    val backpack: BackpackStyle = CustomizationCatalog.backpacks[state.auraIndex]
    val depth = hypot(state.playerX - cam.camX, state.playerY - cam.camY)

    val helmetColor = when (state.hatIndex) {
        0 -> Color(0xFF334155)
        1 -> Color(0xFF10B981)
        2 -> Color(0xFF18181B)
        3 -> Color(0xFFB91C1C)
        else -> Color(0xFF00E5FF)
    }

    drawHumanFigure3D(
        x = state.playerX, y = state.playerY, z = state.playerZ,
        facingYaw = state.cameraYaw,
        walkPhase = if (state.isMoving) state.walkPhase else 0f,
        isJumping = !state.isGrounded, heightScale = 1f,
        vestColor = outfit.primaryColor, pantColor = outfit.pantColor,
        helmetColor = helmetColor, weaponColor = weapon.bodyColor,
        hasWeapon = true, muzzleFlash = state.muzzleFlashTimer > 0f, hitFlash = state.hitFlashTimer > 0f,
        cam = cam, depth = depth,
        backpackColor = backpack.packColor, trimColor = outfit.trimColor
    )
}

private fun DrawScope.drawHumanFigure3D(
    x: Float, y: Float, z: Float,
    facingYaw: Float, walkPhase: Float, isJumping: Boolean, heightScale: Float,
    vestColor: Color, pantColor: Color, helmetColor: Color, weaponColor: Color,
    hasWeapon: Boolean, muzzleFlash: Boolean, hitFlash: Boolean,
    cam: Camera3D, depth: Float,
    backpackColor: Color? = null,
    trimColor: Color? = null
) {
    if (cam.projectInto(x, y, 0.4f)) {
        val shadowScale = (1f - z / 90f).coerceIn(0.45f, 1f)
        val rx = (15f * heightScale * shadowScale * cam.focal / depth).coerceIn(4f, 45f)
        drawOval(
            color = Color.Black.copy(alpha = 0.38f * shadowScale),
            topLeft = Offset(cam.screenX - rx, cam.screenY - rx * 0.35f),
            size = Size(rx * 2f, rx * 0.7f)
        )
    }

    val fwdX = cos(facingYaw)
    val fwdY = sin(facingYaw)
    val rightX = -fwdY
    val rightY = fwdX

    val s = heightScale
    val stride = if (isJumping) 5.5f * s else sin(walkPhase) * 6.5f * s
    val kneeLift = if (isJumping) 5.5f * s else 0f

    val pColor = cam.fogBlend(if (hitFlash) Color.White else pantColor, depth)
    val bootColor = cam.fogBlend(Color(0xFF0F172A), depth)
    val vColor = cam.fogBlend(if (hitFlash) Color.White else vestColor, depth)
    val vDark = cam.fogBlend(lerp(vestColor, Color.Black, 0.35f), depth)
    val hColor = cam.fogBlend(if (hitFlash) Color.White else helmetColor, depth)
    val skinColor = cam.fogBlend(Color(0xFFD2A380), depth)

    val legThick = (5.8f * s * cam.focal / depth).coerceIn(2f, 20f)
    val torsoThick = (14.5f * s * cam.focal / depth).coerceIn(4f, 46f)
    val armThick = (4.2f * s * cam.focal / depth).coerceIn(1.5f, 15f)
    val headRad = (6.6f * s * cam.focal / depth).coerceIn(2.5f, 23f)

    // Legs + Tactical Boots
    val lHip = cam.project(x - rightX * 4.5f * s, y - rightY * 4.5f * s, z + 19f * s)
    val lAnkle = cam.project(x - rightX * 4.5f * s + fwdX * stride * 0.8f, y - rightY * 4.5f * s + fwdY * stride * 0.8f, z + kneeLift + 4f * s)
    val lFoot = cam.project(x - rightX * 4.5f * s + fwdX * stride, y - rightY * 4.5f * s + fwdY * stride, z + kneeLift)

    val rHip = cam.project(x + rightX * 4.5f * s, y + rightY * 4.5f * s, z + 19f * s)
    val rAnkle = cam.project(x + rightX * 4.5f * s - fwdX * stride * 0.8f, y + rightY * 4.5f * s - fwdY * stride * 0.8f, z + kneeLift + 4f * s)
    val rFoot = cam.project(x + rightX * 4.5f * s - fwdX * stride, y + rightY * 4.5f * s - fwdY * stride, z + kneeLift)

    if (lHip != null && lAnkle != null) drawLine(pColor, lHip, lAnkle, strokeWidth = legThick, cap = StrokeCap.Round)
    if (lAnkle != null && lFoot != null) drawLine(bootColor, lAnkle, lFoot, strokeWidth = legThick * 1.08f, cap = StrokeCap.Round)
    if (rHip != null && rAnkle != null) drawLine(pColor, rHip, rAnkle, strokeWidth = legThick, cap = StrokeCap.Round)
    if (rAnkle != null && rFoot != null) drawLine(bootColor, rAnkle, rFoot, strokeWidth = legThick * 1.08f, cap = StrokeCap.Round)

    // Armored Torso Plate Carrier + Shoulders
    val pelvis = cam.project(x, y, z + 18f * s)
    val chest = cam.project(x, y, z + 35f * s)
    val lSh = cam.project(x - rightX * 8f * s, y - rightY * 8f * s, z + 33f * s)
    val rSh = cam.project(x + rightX * 8f * s, y + rightY * 8f * s, z + 33f * s)

    if (pelvis != null && chest != null) {
        drawLine(vColor, pelvis, chest, strokeWidth = torsoThick, cap = StrokeCap.Round)
        drawLine(vDark, pelvis, chest, strokeWidth = torsoThick * 0.52f)
        trimColor?.let { drawLine(cam.fogBlend(it, depth), pelvis, chest, strokeWidth = torsoThick * 0.16f) }
    }
    if (lSh != null && rSh != null) {
        drawLine(vColor, lSh, rSh, strokeWidth = torsoThick * 0.45f, cap = StrokeCap.Round)
    }

    // Tactical Backpack
    if (backpackColor != null && cam.projectInto(x - fwdX * 7.5f * s, y - fwdY * 7.5f * s, z + 27f * s)) {
        val pw = (8.2f * s * cam.focal / depth).coerceIn(3f, 26f)
        drawRoundRect(
            color = cam.fogBlend(backpackColor, depth),
            topLeft = Offset(cam.screenX - pw * 0.65f, cam.screenY - pw * 0.8f),
            size = Size(pw * 1.3f, pw * 1.6f)
        )
    }

    // Realistic Head & Helmet
    if (cam.projectInto(x, y, z + 42f * s)) {
        val hx = cam.screenX; val hy = cam.screenY
        drawCircle(skinColor, radius = headRad, center = Offset(hx, hy))
        drawArc(
            color = hColor,
            startAngle = 175f, sweepAngle = 190f, useCenter = true,
            topLeft = Offset(hx - headRad * 1.14f, hy - headRad * 1.18f),
            size = Size(headRad * 2.28f, headRad * 1.88f)
        )
    }

    // Two-handed Rifle Grip + Muzzle Flash
    if (hasWeapon) {
        val grip = cam.project(x + rightX * 6.5f * s + fwdX * 6f * s, y + rightY * 6.5f * s + fwdY * 6f * s, z + 29f * s)
        val fore = cam.project(x + rightX * 4.5f * s + fwdX * 14f * s, y + rightY * 4.5f * s + fwdY * 14f * s, z + 29f * s)
        val muzzle = cam.project(x + rightX * 4f * s + fwdX * 24f * s, y + rightY * 4f * s + fwdY * 24f * s, z + 29f * s)

        if (rSh != null && grip != null) drawLine(vDark, rSh, grip, strokeWidth = armThick, cap = StrokeCap.Round)
        if (lSh != null && fore != null) drawLine(vDark, lSh, fore, strokeWidth = armThick, cap = StrokeCap.Round)

        if (grip != null && muzzle != null) {
            val gunThick = (4.0f * s * cam.focal / depth).coerceIn(2f, 13f)
            drawLine(cam.fogBlend(weaponColor, depth), grip, muzzle, strokeWidth = gunThick, cap = StrokeCap.Round)
            if (muzzleFlash) {
                drawCircle(Color(0x99FF9500), radius = gunThick * 2.5f, center = muzzle)
                drawCircle(Color(0xFFFFF3B0), radius = gunThick * 1.2f, center = muzzle)
            }
        }
    }
}

private fun DrawScope.drawTacticalReticleOverlay(w: Float, h: Float, state: GameUiState) {
    val cx = w * 0.5f
    val cy = h * 0.5f

    if (state.isOutsideZone) {
        val alpha = (0.35f + state.zoneTickFlash * 0.25f).coerceIn(0.25f, 0.65f)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0xFF0077B6).copy(alpha = alpha), Color(0xFFDC2626).copy(alpha = alpha * 0.7f)),
                center = Offset(cx, cy),
                radius = maxOf(w, h) * 0.65f
            ),
            size = size
        )
    }

    if (state.hitFlashTimer > 0f) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0x88FF2A4B)),
                center = Offset(cx, cy),
                radius = maxOf(w, h) * 0.6f
            ),
            size = size
        )
    }

    if (state.isScoped) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Transparent, Color(0xEC040810)),
                center = Offset(cx, cy),
                radius = minOf(w, h) * 0.48f
            ),
            size = size
        )
        val scopeR = minOf(w, h) * 0.36f
        drawCircle(Color(0xAA00E5FF), radius = scopeR, center = Offset(cx, cy), style = Stroke(2.5f))
        drawLine(Color(0xAA00E5FF), Offset(cx - scopeR, cy), Offset(cx + scopeR, cy), strokeWidth = 1.5f)
        drawLine(Color(0xAA00E5FF), Offset(cx, cy - scopeR), Offset(cx, cy + scopeR), strokeWidth = 1.5f)
        drawCircle(Color(0xFFFF4D6D), radius = 3.5f, center = Offset(cx, cy))
    } else {
        val gap = if (state.isSprinting) 22f else if (state.isFiring) 17f else 11f
        val len = 10f
        val retColor = Color.White.copy(alpha = 0.88f)
        drawLine(retColor, Offset(cx - gap - len, cy), Offset(cx - gap, cy), strokeWidth = 2.5f, cap = StrokeCap.Round)
        drawLine(retColor, Offset(cx + gap, cy), Offset(cx + gap + len, cy), strokeWidth = 2.5f, cap = StrokeCap.Round)
        drawLine(retColor, Offset(cx, cy - gap - len), Offset(cx, cy - gap), strokeWidth = 2.5f, cap = StrokeCap.Round)
        drawLine(retColor, Offset(cx, cy + gap), Offset(cx, cy + gap + len), strokeWidth = 2.5f, cap = StrokeCap.Round)
        drawCircle(Color(0xFF00E5FF), radius = 2.2f, center = Offset(cx, cy))
    }

    if (state.hitmarkerTimer > 0f) {
        val hmColor = if (state.hitmarkerIsHeadshot) Color(0xFFFF2A4B) else Color.White
        val d1 = 7f
        val d2 = 16f
        drawLine(hmColor, Offset(cx - d2, cy - d2), Offset(cx - d1, cy - d1), strokeWidth = 3f, cap = StrokeCap.Round)
        drawLine(hmColor, Offset(cx + d1, cy - d1), Offset(cx + d2, cy - d2), strokeWidth = 3f, cap = StrokeCap.Round)
        drawLine(hmColor, Offset(cx - d2, cy + d2), Offset(cx - d1, cy + d1), strokeWidth = 3f, cap = StrokeCap.Round)
        drawLine(hmColor, Offset(cx + d1, cy + d1), Offset(cx + d2, cy + d2), strokeWidth = 3f, cap = StrokeCap.Round)
    }
}

private fun computeNightFactor(timeOfDay: Float): Float {
    val daylight = sin(timeOfDay * 2f * PI.toFloat()).coerceIn(-1f, 1f)
    return ((-daylight + 0.12f) * 0.52f).coerceIn(0f, 0.82f)
}
