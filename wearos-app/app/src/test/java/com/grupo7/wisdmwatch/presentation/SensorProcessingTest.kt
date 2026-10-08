package com.grupo7.wisdmwatch.presentation

import org.junit.Assert.*
import org.junit.Test

class SensorProcessingTest {

    @Test
    fun testInterpolation() {
        val samples = ArrayDeque<Pair<Long, FloatArray>>()
        samples.addLast(0L to floatArrayOf(0f, 2f, 4f))
        samples.addLast(100_000_000L to floatArrayOf(10f, 12f, 14f))

        val result = requireNotNull(interpolateAt(samples, 50_000_000L))
        assertArrayEquals(floatArrayOf(5f, 7f, 9f), result, 0.001f)
    }

    @Test
    fun testInterpolationGap() {
        val samples = ArrayDeque<Pair<Long, FloatArray>>()
        samples.addLast(0L to floatArrayOf(0f, 0f, 0f))
        samples.addLast(300_000_000L to floatArrayOf(1f, 1f, 1f))

        assertNull(interpolateAt(samples, 150_000_000L))
    }

    @Test
    fun testQuantization() {
        val window = Array(100) { FloatArray(6) }
        window[0][1] = 1f
        window[0][2] = -1f
        window[0][3] = 200f
        window[0][4] = -200f

        val result = quantizeWindow(window, 0.5f, 20)

        assertEquals(600, result.size)
        assertEquals(20, result[0].toInt())
        assertEquals(22, result[1].toInt())
        assertEquals(18, result[2].toInt())
        assertEquals(127, result[3].toInt())
        assertEquals(-128, result[4].toInt())
    }
}
