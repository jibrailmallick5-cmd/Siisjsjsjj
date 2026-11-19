package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

/**
 * Abstraction for game audio playback so ViewModel and simulation logic remain unit-testable.
 */
interface GameSoundManager {
    fun playWeaponFire(weaponIndex: Int)
    fun playFootstep(isSprinting: Boolean)
    fun playJumpLand()
    fun playZoneDamageAlert(isInitialWarning: Boolean = false)
    fun playHitmarker(isHeadshot: Boolean)
    fun playReload()
    fun setSoundEnabled(enabled: Boolean)
    fun setMasterVolume(volume: Float)
    fun onPause()
    fun onResume()
    fun release()
}

object NoOpSoundManager : GameSoundManager {
    override fun playWeaponFire(weaponIndex: Int) = Unit
    override fun playFootstep(isSprinting: Boolean) = Unit
    override fun playJumpLand() = Unit
    override fun playZoneDamageAlert(isInitialWarning: Boolean) = Unit
    override fun playHitmarker(isHeadshot: Boolean) = Unit
    override fun playReload() = Unit
    override fun setSoundEnabled(enabled: Boolean) = Unit
    override fun setMasterVolume(volume: Float) = Unit
    override fun onPause() = Unit
    override fun onResume() = Unit
    override fun release() = Unit
}

/**
 * Low-latency SoundPool-backed game audio engine powered by procedurally synthesized 16-bit PCM WAV SFX.
 * Handles weapon gunfire (per gun class), cadence-synced footsteps, landing thuds, zone damage alerts,
 * hitmarkers, and reloads with polyphonic mixing and pitch jitter.
 */
class SoundManager(
    context: Context
) : GameSoundManager {

    private val appContext = context.applicationContext
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val loadedIds = ConcurrentHashMap.newKeySet<Int>()

    @Volatile
    private var isEnabled: Boolean = true

    @Volatile
    private var masterVolume: Float = 0.85f

    @Volatile
    private var isReleased: Boolean = false

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(12)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
        .apply {
            setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0) {
                    loadedIds.add(sampleId)
                }
            }
        }

    @Volatile private var fireM4A1Id = 0
    @Volatile private var fireMp40Id = 0
    @Volatile private var fireAk47Id = 0
    @Volatile private var fireAwmId = 0
    @Volatile private var footstepWalkId = 0
    @Volatile private var footstepSprintId = 0
    @Volatile private var jumpLandId = 0
    @Volatile private var zoneInitialAlertId = 0
    @Volatile private var zoneDamageTickId = 0
    @Volatile private var hitmarkerId = 0
    @Volatile private var headshotId = 0
    @Volatile private var reloadId = 0

    init {
        ioScope.launch {
            preloadSynthesizedSounds()
        }
    }

    private fun preloadSynthesizedSounds() {
        if (isReleased) return
        val sfxDir = File(appContext.cacheDir, "fight_sim_sfx_v1").apply { mkdirs() }

        fun writeAndLoad(fileName: String, wavBytes: ByteArray): Int {
            if (isReleased) return 0
            val outFile = File(sfxDir, fileName)
            if (!outFile.exists() || outFile.length() != wavBytes.size.toLong()) {
                outFile.writeBytes(wavBytes)
            }
            return if (!isReleased) soundPool.load(outFile.absolutePath, 1) else 0
        }

        fireM4A1Id = writeAndLoad("fire_m4a1.wav", SoundSynthesizer.synthesizeGunshot(GunSoundProfile.M4A1))
        fireMp40Id = writeAndLoad("fire_mp40.wav", SoundSynthesizer.synthesizeGunshot(GunSoundProfile.MP40))
        fireAk47Id = writeAndLoad("fire_ak47.wav", SoundSynthesizer.synthesizeGunshot(GunSoundProfile.AK47))
        fireAwmId = writeAndLoad("fire_awm.wav", SoundSynthesizer.synthesizeGunshot(GunSoundProfile.AWM))
        footstepWalkId = writeAndLoad("step_walk.wav", SoundSynthesizer.synthesizeFootstep(isSprint = false))
        footstepSprintId = writeAndLoad("step_sprint.wav", SoundSynthesizer.synthesizeFootstep(isSprint = true))
        jumpLandId = writeAndLoad("jump_land.wav", SoundSynthesizer.synthesizeJumpLand())
        zoneInitialAlertId = writeAndLoad("zone_warn.wav", SoundSynthesizer.synthesizeZoneAlert(isInitial = true))
        zoneDamageTickId = writeAndLoad("zone_tick.wav", SoundSynthesizer.synthesizeZoneAlert(isInitial = false))
        hitmarkerId = writeAndLoad("hit_body.wav", SoundSynthesizer.synthesizeHitmarker(isHeadshot = false))
        headshotId = writeAndLoad("hit_head.wav", SoundSynthesizer.synthesizeHitmarker(isHeadshot = true))
        reloadId = writeAndLoad("reload.wav", SoundSynthesizer.synthesizeReload())
    }

    private fun playSample(soundId: Int, gain: Float, priority: Int = 1, rateJitter: Float = 0f) {
        if (!isEnabled || isReleased || soundId == 0 || !loadedIds.contains(soundId)) return
        val vol = (masterVolume * gain).coerceIn(0f, 1f)
        if (vol <= 0.005f) return
        val rate = if (rateJitter > 0f) {
            1f + (Random.nextFloat() * 2f - 1f) * rateJitter
        } else {
            1f
        }
        soundPool.play(soundId, vol, vol, priority, 0, rate.coerceIn(0.75f, 1.35f))
    }

    override fun playWeaponFire(weaponIndex: Int) {
        when (weaponIndex) {
            1 -> playSample(fireMp40Id, gain = 0.72f, priority = 2, rateJitter = 0.05f)
            2 -> playSample(fireAk47Id, gain = 0.88f, priority = 2, rateJitter = 0.04f)
            3 -> playSample(fireAwmId, gain = 1.0f, priority = 3, rateJitter = 0.02f)
            else -> playSample(fireM4A1Id, gain = 0.80f, priority = 2, rateJitter = 0.045f)
        }
    }

    override fun playFootstep(isSprinting: Boolean) {
        if (isSprinting) {
            playSample(footstepSprintId, gain = 0.56f, priority = 1, rateJitter = 0.09f)
        } else {
            playSample(footstepWalkId, gain = 0.40f, priority = 1, rateJitter = 0.08f)
        }
    }

    override fun playJumpLand() {
        playSample(jumpLandId, gain = 0.65f, priority = 1, rateJitter = 0.06f)
    }

    override fun playZoneDamageAlert(isInitialWarning: Boolean) {
        if (isInitialWarning) {
            playSample(zoneInitialAlertId, gain = 0.92f, priority = 3)
        } else {
            playSample(zoneDamageTickId, gain = 0.78f, priority = 2)
        }
    }

    override fun playHitmarker(isHeadshot: Boolean) {
        if (isHeadshot) {
            playSample(headshotId, gain = 0.88f, priority = 3)
        } else {
            playSample(hitmarkerId, gain = 0.68f, priority = 2, rateJitter = 0.04f)
        }
    }

    override fun playReload() {
        playSample(reloadId, gain = 0.65f, priority = 1)
    }

    override fun setSoundEnabled(enabled: Boolean) {
        isEnabled = enabled
        if (!enabled && !isReleased) {
            soundPool.autoPause()
        } else if (enabled && !isReleased) {
            soundPool.autoResume()
        }
    }

    override fun setMasterVolume(volume: Float) {
        masterVolume = volume.coerceIn(0f, 1f)
    }

    override fun onPause() {
        if (!isReleased) {
            soundPool.autoPause()
        }
    }

    override fun onResume() {
        if (!isReleased && isEnabled) {
            soundPool.autoResume()
        }
    }

    override fun release() {
        if (isReleased) return
        isReleased = true
        ioScope.cancel()
        loadedIds.clear()
        soundPool.release()
    }
}

enum class GunSoundProfile(
    val durationSec: Float,
    val startFreqHz: Float,
    val endFreqHz: Float,
    val bodyDecay: Float,
    val noiseDecay: Float,
    val punchGain: Float
) {
    M4A1(durationSec = 0.145f, startFreqHz = 310f, endFreqHz = 62f, bodyDecay = 36f, noiseDecay = 22f, punchGain = 1.15f),
    MP40(durationSec = 0.095f, startFreqHz = 270f, endFreqHz = 86f, bodyDecay = 48f, noiseDecay = 32f, punchGain = 1.0f),
    AK47(durationSec = 0.180f, startFreqHz = 210f, endFreqHz = 46f, bodyDecay = 28f, noiseDecay = 18f, punchGain = 1.35f),
    AWM(durationSec = 0.350f, startFreqHz = 160f, endFreqHz = 32f, bodyDecay = 14f, noiseDecay = 9.5f, punchGain = 1.55f)
}

/**
 * Pure Kotlin 16-bit PCM WAV synthesizer for deterministic, zero-asset procedural game SFX.
 */
object SoundSynthesizer {
    const val SAMPLE_RATE = 22050

    fun synthesizeGunshot(profile: GunSoundProfile): ByteArray {
        val count = (SAMPLE_RATE * profile.durationSec).toInt()
        val samples = ShortArray(count)
        val rng = Random(profile.ordinal * 7919 + 42)
        var phase = 0.0
        var prevNoise = 0f

        for (i in 0 until count) {
            val t = i.toFloat() / SAMPLE_RATE
            val freq = profile.endFreqHz + (profile.startFreqHz - profile.endFreqHz) * exp(-t * 42f)
            phase += 2.0 * PI * freq / SAMPLE_RATE

            val transientCrack = if (t < 0.011f) (1f - t / 0.011f) * 0.9f else 0f
            val bodyTone = sin(phase).toFloat() * exp(-t * profile.bodyDecay)
            val rawNoise = rng.nextFloat() * 2f - 1f
            // Light low-pass smoothing on noise tail for realistic gunpowder punch
            val filteredNoise = prevNoise * 0.35f + rawNoise * 0.65f
            prevNoise = filteredNoise
            val noiseBurst = filteredNoise * exp(-t * profile.noiseDecay)

            val raw = (bodyTone * 0.68f + noiseBurst * 0.72f + transientCrack * (rng.nextFloat() * 0.5f + 0.5f)) * profile.punchGain
            val faded = applyEdgeFade(tanh(raw.toDouble()).toFloat(), i, count, fadeSamples = 80)
            samples[i] = (faded * 31000f).toInt().coerceIn(-32767, 32767).toShort()
        }
        return encodeWav(samples)
    }

    fun synthesizeFootstep(isSprint: Boolean): ByteArray {
        val duration = if (isSprint) 0.085f else 0.075f
        val count = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(count)
        val rng = Random(if (isSprint) 2026 else 1013)
        var phase = 0.0
        var lpNoise = 0f

        for (i in 0 until count) {
            val t = i.toFloat() / SAMPLE_RATE
            val startF = if (isSprint) 128f else 98f
            val endF = if (isSprint) 44f else 38f
            val freq = endF + (startF - endF) * exp(-t * 55f)
            phase += 2.0 * PI * freq / SAMPLE_RATE

            val thump = sin(phase).toFloat() * exp(-t * (if (isSprint) 34f else 42f))
            val white = rng.nextFloat() * 2f - 1f
            lpNoise = lpNoise * 0.72f + white * 0.28f
            val grit = lpNoise * exp(-t * 48f) * (if (isSprint) 0.52f else 0.36f)

            val mixed = tanh(((thump * 0.85f + grit) * (if (isSprint) 1.15f else 0.9f)).toDouble()).toFloat()
            val faded = applyEdgeFade(mixed, i, count, fadeSamples = 60)
            samples[i] = (faded * 26000f).toInt().coerceIn(-32767, 32767).toShort()
        }
        return encodeWav(samples)
    }

    fun synthesizeJumpLand(): ByteArray {
        val duration = 0.115f
        val count = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(count)
        val rng = Random(5150)
        var phase = 0.0
        var lpNoise = 0f

        for (i in 0 until count) {
            val t = i.toFloat() / SAMPLE_RATE
            val freq = 34f + 78f * exp(-t * 38f)
            phase += 2.0 * PI * freq / SAMPLE_RATE
            val impact = sin(phase).toFloat() * exp(-t * 24f)
            lpNoise = lpNoise * 0.65f + (rng.nextFloat() * 2f - 1f) * 0.35f
            val scuff = lpNoise * exp(-t * 32f) * 0.45f
            val mixed = tanh(((impact + scuff) * 1.1f).toDouble()).toFloat()
            val faded = applyEdgeFade(mixed, i, count, fadeSamples = 70)
            samples[i] = (faded * 29000f).toInt().coerceIn(-32767, 32767).toShort()
        }
        return encodeWav(samples)
    }

    fun synthesizeZoneAlert(isInitial: Boolean): ByteArray {
        val duration = if (isInitial) 0.28f else 0.21f
        val count = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(count)
        val rng = Random(if (isInitial) 9001 else 9002)
        var phase = 0.0

        for (i in 0 until count) {
            val t = i.toFloat() / SAMPLE_RATE
            val pulseEnv = if (isInitial) {
                val p1 = if (t < 0.11f) 1f else 0f
                val p2 = if (t in 0.14f..0.27f) 1f else 0f
                maxOf(p1, p2) * exp(-((t % 0.14f) * 10f))
            } else {
                exp(-t * 11f)
            }
            val freq = if (isInitial) {
                if (t < 0.13f) 780f else 1040f
            } else {
                640f + 120f * sin(2f * PI.toFloat() * 14f * t)
            }
            phase += 2.0 * PI * freq / SAMPLE_RATE
            val beep = sin(phase).toFloat() * 0.65f + sin(phase * 2.0).toFloat() * 0.22f
            val stormStatic = (rng.nextFloat() * 2f - 1f) * 0.16f * exp(-t * 8f)
            val mixed = tanh(((beep * pulseEnv + stormStatic) * 0.95f).toDouble()).toFloat()
            val faded = applyEdgeFade(mixed, i, count, fadeSamples = 90)
            samples[i] = (faded * 27000f).toInt().coerceIn(-32767, 32767).toShort()
        }
        return encodeWav(samples)
    }

    fun synthesizeHitmarker(isHeadshot: Boolean): ByteArray {
        val duration = if (isHeadshot) 0.11f else 0.052f
        val count = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toFloat() / SAMPLE_RATE
            val env = exp(-t * (if (isHeadshot) 32f else 68f))
            val signal = if (isHeadshot) {
                (sin(2.0 * PI * 2050.0 * t).toFloat() * 0.6f + sin(2.0 * PI * 2920.0 * t).toFloat() * 0.45f) * env
            } else {
                sin(2.0 * PI * 1450.0 * t).toFloat() * env
            }
            val faded = applyEdgeFade(signal, i, count, fadeSamples = 35)
            samples[i] = (faded * 28000f).toInt().coerceIn(-32767, 32767).toShort()
        }
        return encodeWav(samples)
    }

    fun synthesizeReload(): ByteArray {
        val duration = 0.135f
        val count = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(count)
        val rng = Random(303)
        for (i in 0 until count) {
            val t = i.toFloat() / SAMPLE_RATE
            val c1 = if (t < 0.03f) exp(-t * 140f) * sin(2.0 * PI * 1150.0 * t).toFloat() else 0f
            val c2 = if (t >= 0.08f) exp(-(t - 0.08f) * 120f) * sin(2.0 * PI * 1580.0 * (t - 0.08f)).toFloat() else 0f
            val mech = (c1 + c2 * 1.15f) * 0.8f + (rng.nextFloat() * 2f - 1f) * 0.08f * (if (t < 0.02f || t in 0.08f..0.10f) 1f else 0f)
            val faded = applyEdgeFade(mech.coerceIn(-1f, 1f), i, count, fadeSamples = 40)
            samples[i] = (faded * 26000f).toInt().coerceIn(-32767, 32767).toShort()
        }
        return encodeWav(samples)
    }

    private fun applyEdgeFade(sample: Float, index: Int, total: Int, fadeSamples: Int): Float {
        val fadeIn = if (index < fadeSamples) index.toFloat() / fadeSamples else 1f
        val rem = total - 1 - index
        val fadeOut = if (rem < fadeSamples) rem.toFloat() / fadeSamples else 1f
        return sample * minOf(fadeIn, fadeOut).coerceIn(0f, 1f)
    }

    private fun encodeWav(samples: ShortArray): ByteArray {
        val dataSize = samples.size * 2
        val totalSize = 36 + dataSize
        val out = ByteArrayOutputStream(44 + dataSize)

        fun writeString(s: String) = s.forEach { out.write(it.code) }
        fun writeIntLE(v: Int) {
            out.write(v and 0xFF)
            out.write((v shr 8) and 0xFF)
            out.write((v shr 16) and 0xFF)
            out.write((v shr 24) and 0xFF)
        }
        fun writeShortLE(v: Int) {
            out.write(v and 0xFF)
            out.write((v shr 8) and 0xFF)
        }

        writeString("RIFF")
        writeIntLE(totalSize)
        writeString("WAVE")
        writeString("fmt ")
        writeIntLE(16) // PCM chunk size
        writeShortLE(1) // AudioFormat = 1 (PCM)
        writeShortLE(1) // NumChannels = 1 (Mono)
        writeIntLE(SAMPLE_RATE)
        writeIntLE(SAMPLE_RATE * 2) // ByteRate
        writeShortLE(2) // BlockAlign
        writeShortLE(16) // BitsPerSample
        writeString("data")
        writeIntLE(dataSize)

        for (s in samples) {
            writeShortLE(s.toInt())
        }
        return out.toByteArray()
    }
}
