package com.example

import android.graphics.Canvas
import com.example.game.GameLoop
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameLoopTest {

    @Test
    fun gameLoopRunsUpdatesAndHonorsPauseResumeStop() = runBlocking {
        var updates = 0
        var nativeDraws = 0

        val loop = object : GameLoop(targetFps = 60) {
            override fun onUpdate(deltaTime: Float) {
                assertTrue(deltaTime > 0f)
                updates++
            }

            override fun onDrawNativeCanvas(canvas: Canvas, width: Float, height: Float) {
                nativeDraws++
            }
        }

        loop.start(this)
        assertTrue(loop.isRunning.value)
        delay(65L)
        val afterStart = updates
        assertTrue(afterStart >= 2)

        loop.pause()
        assertTrue(loop.isPaused.value)
        val countAtPause = updates
        delay(50L)
        assertEquals(countAtPause, updates)

        loop.resume()
        assertFalse(loop.isPaused.value)
        delay(55L)
        assertTrue(updates > countAtPause)

        loop.stop()
        assertFalse(loop.isRunning.value)
        assertEquals(0, nativeDraws)
    }
}
