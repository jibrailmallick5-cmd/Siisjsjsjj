package com.example.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Base class for real-time game worlds using Jetpack Compose [Canvas] and Kotlin Coroutines
 * to manage the periodic [onUpdate] simulation cycle and [onDraw] render cycle.
 */
abstract class GameLoop(
    targetFps: Int = 60
) {
    private var loopJob: Job? = null

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _frameTick = MutableStateFlow(0L)
    val frameTick: StateFlow<Long> = _frameTick.asStateFlow()

    private val frameDelayMs: Long = (1000L / targetFps.coerceIn(15, 120)).coerceAtLeast(8L)

    /**
     * Starts the coroutine-driven update loop within [scope].
     * Calling [start] while already active is a safe no-op.
     */
    fun start(scope: CoroutineScope) {
        if (loopJob?.isActive == true) return
        _isRunning.value = true
        loopJob = scope.launch {
            var lastTimeNanos = System.nanoTime()
            while (isActive) {
                delay(frameDelayMs)
                val nowNanos = System.nanoTime()
                val deltaSeconds = ((nowNanos - lastTimeNanos) / 1_000_000_000f)
                    .coerceIn(0.004f, 0.05f)
                lastTimeNanos = nowNanos

                if (!_isPaused.value) {
                    onUpdate(deltaSeconds)
                    _frameTick.value = _frameTick.value + 1L
                }
            }
        }
    }

    /** Pauses [onUpdate] ticks without cancelling the coroutine loop. */
    fun pause() {
        _isPaused.value = true
    }

    /** Resumes [onUpdate] ticks after [pause]. */
    fun resume() {
        _isPaused.value = false
    }

    /** Stops the coroutine loop and releases the running job. */
    fun stop() {
        loopJob?.cancel()
        loopJob = null
        _isRunning.value = false
    }

    /**
     * Advances world state, physics, AI, and particles by [deltaTime] seconds.
     */
    abstract fun onUpdate(deltaTime: Float)

    /**
     * Draws the current world frame using Compose [DrawScope].
     * Defaults to delegating to [onDrawNativeCanvas] for raw Android [android.graphics.Canvas] rendering.
     */
    open fun onDraw(drawScope: DrawScope) {
        onDrawNativeCanvas(
            canvas = drawScope.drawContext.canvas.nativeCanvas,
            width = drawScope.size.width,
            height = drawScope.size.height
        )
    }

    /**
     * Optional override for subclasses that draw directly on [android.graphics.Canvas].
     */
    protected open fun onDrawNativeCanvas(
        canvas: android.graphics.Canvas,
        width: Float,
        height: Float
    ) = Unit

    /**
     * Renders this [GameLoop]'s [onDraw] cycle inside a Compose [Canvas] invalidated each frame tick.
     */
    @Composable
    fun GameCanvas(modifier: Modifier = Modifier) {
        val tick by frameTick.collectAsStateWithLifecycle()
        Canvas(modifier = modifier) {
            @Suppress("UNUSED_VARIABLE")
            val currentTick = tick
            onDraw(this)
        }
    }
}
