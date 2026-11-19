package com.example.ui.canvas

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import com.example.game.CustomizationCatalog
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HeroPreviewCanvas(
    cloakIndex: Int,
    hatIndex: Int,
    weaponIndex: Int,
    auraIndex: Int,
    modifier: Modifier = Modifier,
    isFullLobby: Boolean = false,
    yawAngle: Float = 0.25f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "operative_preview")
    val anim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "preview_cycle"
    )

    val outfit = CustomizationCatalog.outfits[cloakIndex.coerceIn(0, CustomizationCatalog.outfits.lastIndex)]
    val weapon = CustomizationCatalog.weapons[weaponIndex.coerceIn(0, CustomizationCatalog.weapons.lastIndex)]
    val backpack = CustomizationCatalog.backpacks[auraIndex.coerceIn(0, CustomizationCatalog.backpacks.lastIndex)]

    Canvas(modifier = modifier) {
        val scale = if (isFullLobby) (size.minDimension / 245f).coerceIn(1.1f, 2.5f) else (size.minDimension / 145f).coerceIn(0.85f, 1.4f)
        val cx = size.width / 2f
        val cy = if (isFullLobby) size.height * 0.56f else size.height * 0.54f
        val breathe = sin(anim * 1.5f) * 1.8f * scale
        val sway = sin(anim) * 1.5f * scale
        val turnSin = sin(yawAngle)
        val turnCos = cos(yawAngle)

        // Holographic Floor Pedestal & Volumetric Ground Light
        val footY = cy + 68f * scale
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(outfit.trimColor.copy(alpha = 0.36f), outfit.trimColor.copy(alpha = 0.08f), Color.Transparent),
                center = Offset(cx, footY),
                radius = 85f * scale
            ),
            radius = 85f * scale,
            center = Offset(cx, footY)
        )
        drawOval(
            color = outfit.trimColor.copy(alpha = 0.68f),
            topLeft = Offset(cx - 56f * scale, footY - 11f * scale),
            size = Size(112f * scale, 22f * scale),
            style = Stroke(2.4f * scale)
        )
        drawOval(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(cx - 30f * scale, footY - 6f * scale),
            size = Size(60f * scale, 12f * scale)
        )

        // Tactical Backpack (visible behind shoulder based on rotation)
        val packOffsetX = -turnSin * 14f * scale - 18f * scale * turnCos.coerceAtLeast(0.25f)
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(backpack.packColor, Color(0xFF0B0F17))),
            topLeft = Offset(cx + packOffsetX - 10f * scale, cy - 30f * scale + breathe),
            size = Size(20f * scale, 34f * scale),
            cornerRadius = CornerRadius(4f * scale, 4f * scale)
        )
        drawLine(
            color = backpack.coreColor,
            start = Offset(cx + packOffsetX - 4f * scale, cy - 22f * scale + breathe),
            end = Offset(cx + packOffsetX + 4f * scale, cy - 22f * scale + breathe),
            strokeWidth = 2.5f * scale,
            cap = StrokeCap.Round
        )

        // Realistic Legs (Upper thigh + Knee Pad + Shin + Tactical Combat Boots)
        val stanceW = (11f + 3f * kotlin.math.abs(turnCos)) * scale
        val lHip = Offset(cx - stanceW + sway * 0.3f, cy + 14f * scale + breathe)
        val rHip = Offset(cx + stanceW + sway * 0.3f, cy + 14f * scale + breathe)
        val lKnee = Offset(cx - stanceW * 1.12f, cy + 40f * scale + breathe * 0.5f)
        val rKnee = Offset(cx + stanceW * 1.12f, cy + 40f * scale + breathe * 0.5f)
        val lBoot = Offset(cx - stanceW * 1.22f, footY - 3f * scale)
        val rBoot = Offset(cx + stanceW * 1.22f, footY - 3f * scale)

        val pantShade = lerp(outfit.pantColor, Color.Black, 0.32f)
        drawLine(outfit.pantColor, lHip, lKnee, strokeWidth = 12f * scale, cap = StrokeCap.Round)
        drawLine(pantShade, lKnee, lBoot, strokeWidth = 10.5f * scale, cap = StrokeCap.Round)
        drawLine(outfit.pantColor, rHip, rKnee, strokeWidth = 12f * scale, cap = StrokeCap.Round)
        drawLine(pantShade, rKnee, rBoot, strokeWidth = 10.5f * scale, cap = StrokeCap.Round)

        // Knee Pads & Combat Boots
        drawRoundRect(Color(0xFF1E293B), Offset(lKnee.x - 5.5f * scale, lKnee.y - 4f * scale), Size(11f * scale, 9f * scale), CornerRadius(3f * scale))
        drawRoundRect(Color(0xFF1E293B), Offset(rKnee.x - 5.5f * scale, rKnee.y - 4f * scale), Size(11f * scale, 9f * scale), CornerRadius(3f * scale))
        drawRoundRect(Color(0xFF0F172A), Offset(lBoot.x - 8f * scale, lBoot.y - 4f * scale), Size(15f * scale, 8f * scale), CornerRadius(3f * scale))
        drawRoundRect(Color(0xFF0F172A), Offset(rBoot.x - 6f * scale, rBoot.y - 4f * scale), Size(15f * scale, 8f * scale), CornerRadius(3f * scale))

        // Utility Belt & Holster
        drawRoundRect(
            color = Color(0xFF111827),
            topLeft = Offset(cx - 18f * scale + sway * 0.3f, cy + 8f * scale + breathe),
            size = Size(36f * scale, 7f * scale),
            cornerRadius = CornerRadius(2f * scale)
        )
        drawRect(
            color = outfit.trimColor,
            topLeft = Offset(cx - 4f * scale + sway * 0.3f, cy + 9f * scale + breathe),
            size = Size(8f * scale, 5f * scale)
        )

        // Armored Plate-Carrier Torso Vest (Shaded)
        val torsoTop = cy - 34f * scale + breathe
        val torsoW = (38f - kotlin.math.abs(turnSin) * 6f) * scale
        val vestBrush = Brush.horizontalGradient(
            colors = listOf(lerp(outfit.primaryColor, Color.White, 0.14f), outfit.primaryColor, lerp(outfit.primaryColor, Color.Black, 0.38f)),
            startX = cx - torsoW / 2f,
            endX = cx + torsoW / 2f
        )
        val vestPath = Path().apply {
            moveTo(cx - torsoW * 0.56f + sway * 0.4f, torsoTop + 3f * scale)
            lineTo(cx + torsoW * 0.56f + sway * 0.4f, torsoTop + 3f * scale)
            lineTo(cx + torsoW * 0.46f + sway * 0.3f, cy + 10f * scale + breathe)
            lineTo(cx - torsoW * 0.46f + sway * 0.3f, cy + 10f * scale + breathe)
            close()
        }
        drawPath(vestPath, vestBrush)
        // Chest Armor Plate + Ammo Pouches
        drawRoundRect(
            color = lerp(outfit.primaryColor, Color.Black, 0.28f),
            topLeft = Offset(cx - torsoW * 0.34f + sway * 0.4f, torsoTop + 9f * scale),
            size = Size(torsoW * 0.68f, 22f * scale),
            cornerRadius = CornerRadius(4f * scale)
        )
        drawRoundRect(
            color = outfit.trimColor.copy(alpha = 0.85f),
            topLeft = Offset(cx - torsoW * 0.34f + sway * 0.4f, torsoTop + 9f * scale),
            size = Size(torsoW * 0.68f, 22f * scale),
            cornerRadius = CornerRadius(4f * scale),
            style = Stroke(1.6f * scale)
        )

        // Shoulder Pauldrons & Arms gripping Rifle
        val lShoulder = Offset(cx - torsoW * 0.58f + sway * 0.4f, torsoTop + 6f * scale)
        val rShoulder = Offset(cx + torsoW * 0.58f + sway * 0.4f, torsoTop + 6f * scale)
        val lHand = Offset(cx + 8f * scale + sway * 0.5f, cy - 6f * scale + breathe)
        val rHand = Offset(cx - 10f * scale + sway * 0.5f, cy - 3f * scale + breathe)

        drawLine(outfit.primaryColor, lShoulder, Offset(lShoulder.x - 4f * scale, cy - 6f * scale + breathe), strokeWidth = 9.5f * scale, cap = StrokeCap.Round)
        drawLine(Color(0xFF1E293B), Offset(lShoulder.x - 4f * scale, cy - 6f * scale + breathe), lHand, strokeWidth = 8.5f * scale, cap = StrokeCap.Round)
        drawLine(outfit.primaryColor, rShoulder, Offset(rShoulder.x + 4f * scale, cy - 6f * scale + breathe), strokeWidth = 9.5f * scale, cap = StrokeCap.Round)
        drawLine(Color(0xFF1E293B), Offset(rShoulder.x + 4f * scale, cy - 6f * scale + breathe), rHand, strokeWidth = 8.5f * scale, cap = StrokeCap.Round)

        // Neck & Sculpted Realistic Head + Helmet/NVG/Mask/Beret/Visor
        val headCenter = Offset(cx + sway * 0.5f, torsoTop - 14f * scale)
        drawLine(Color(0xFFA97B5C), Offset(headCenter.x, headCenter.y + 8f * scale), Offset(headCenter.x, torsoTop + 4f * scale), strokeWidth = 9f * scale)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFE0B899), Color(0xFFB88664)),
                center = Offset(headCenter.x - 3f * scale, headCenter.y - 3f * scale),
                radius = 15f * scale
            ),
            radius = 12.5f * scale,
            center = headCenter
        )
        // Subtle eyes / brow shadow
        drawLine(Color(0xFF2B1E16), Offset(headCenter.x - 5.5f * scale, headCenter.y - 1f * scale), Offset(headCenter.x - 2f * scale, headCenter.y - 1f * scale), strokeWidth = 1.6f * scale)
        drawLine(Color(0xFF2B1E16), Offset(headCenter.x + 2f * scale, headCenter.y - 1f * scale), Offset(headCenter.x + 5.5f * scale, headCenter.y - 1f * scale), strokeWidth = 1.6f * scale)

        when (hatIndex) {
            0 -> { // Ops Helmet
                drawArc(Color(0xFF334155), 175f, 190f, true, Offset(headCenter.x - 14.5f * scale, headCenter.y - 15f * scale), Size(29f * scale, 24f * scale))
                drawArc(outfit.trimColor.copy(alpha = 0.7f), 195f, 150f, false, Offset(headCenter.x - 14.5f * scale, headCenter.y - 15f * scale), Size(29f * scale, 24f * scale), style = Stroke(1.8f * scale))
            }
            1 -> { // NVG Helmet
                drawArc(Color(0xFF1E293B), 175f, 190f, true, Offset(headCenter.x - 14.5f * scale, headCenter.y - 15f * scale), Size(29f * scale, 24f * scale))
                drawRoundRect(Color(0xFF0F172A), Offset(headCenter.x - 10f * scale, headCenter.y - 5f * scale), Size(20f * scale, 7f * scale), CornerRadius(2f * scale))
                drawCircle(Color(0xFF10B981), radius = 3.2f * scale, center = Offset(headCenter.x - 4.5f * scale, headCenter.y - 1.5f * scale))
                drawCircle(Color(0xFF10B981), radius = 3.2f * scale, center = Offset(headCenter.x + 4.5f * scale, headCenter.y - 1.5f * scale))
            }
            2 -> { // Ballistic Mask
                drawArc(Color(0xFF27272A), 175f, 190f, true, Offset(headCenter.x - 14f * scale, headCenter.y - 14.5f * scale), Size(28f * scale, 22f * scale))
                drawRoundRect(Color(0xFF18181B), Offset(headCenter.x - 11f * scale, headCenter.y - 1f * scale), Size(22f * scale, 13f * scale), CornerRadius(4f * scale))
            }
            3 -> { // Crimson Beret
                drawOval(Color(0xFFB91C1C), Offset(headCenter.x - 15f * scale, headCenter.y - 17f * scale), Size(29f * scale, 13f * scale))
                drawCircle(Color(0xFFFFB703), radius = 2.4f * scale, center = Offset(headCenter.x + 5f * scale, headCenter.y - 10f * scale))
            }
            else -> { // Tactical Visor
                drawArc(Color(0xFF1E293B), 175f, 190f, true, Offset(headCenter.x - 14f * scale, headCenter.y - 14.5f * scale), Size(28f * scale, 22f * scale))
                drawRoundRect(Color(0xFF0F172A), Offset(headCenter.x - 12f * scale, headCenter.y - 5f * scale), Size(24f * scale, 7.5f * scale), CornerRadius(3f * scale))
                drawLine(Color(0xFF00E5FF), Offset(headCenter.x - 9f * scale, headCenter.y - 1.5f * scale), Offset(headCenter.x + 9f * scale, headCenter.y - 1.5f * scale), strokeWidth = 2.5f * scale, cap = StrokeCap.Round)
            }
        }

        // Detailed Realistic Firearm (Stock, Receiver, Magazine, Optic, Barrel)
        rotate(degrees = -14f + sin(anim) * 2.5f, pivot = Offset(cx + 2f * scale, cy - 5f * scale + breathe)) {
            val gy = cy - 4f * scale + breathe
            // Stock
            drawRoundRect(lerp(weapon.bodyColor, Color.Black, 0.25f), Offset(cx - 26f * scale, gy - 4f * scale), Size(16f * scale, 9f * scale), CornerRadius(2f * scale))
            // Receiver
            drawRoundRect(weapon.bodyColor, Offset(cx - 12f * scale, gy - 5f * scale), Size(32f * scale, 9f * scale), CornerRadius(2f * scale))
            // Curved Magazine
            drawLine(Color(0xFF111827), Offset(cx + 4f * scale, gy + 4f * scale), Offset(cx + 8f * scale, gy + 16f * scale), strokeWidth = 5f * scale, cap = StrokeCap.Round)
            // Optic / Scope
            drawRoundRect(Color(0xFF0F172A), Offset(cx - 4f * scale, gy - 10f * scale), Size(16f * scale, 4.5f * scale), CornerRadius(1.5f * scale))
            // Barrel
            val barrelLen = if (weapon.index == 3) 42f * scale else 28f * scale
            drawLine(Color(0xFF334155), Offset(cx + 18f * scale, gy - 1.5f * scale), Offset(cx + 18f * scale + barrelLen, gy - 1.5f * scale), strokeWidth = 3.8f * scale, cap = StrokeCap.Round)
            // Accent trim
            drawLine(weapon.accentColor, Offset(cx - 6f * scale, gy), Offset(cx + 16f * scale, gy), strokeWidth = 2.2f * scale, cap = StrokeCap.Round)
        }
    }
}
