package br.edu.watchhar

import org.junit.Assert.assertEquals
import org.junit.Test

class ProbabilitySmootherTest {
    @Test
    fun averagesLastThreePredictions() {
        val smoother = ProbabilitySmoother(3)
        smoother.add(floatArrayOf(0.8f, 0.1f, 0.05f, 0.03f, 0.02f))
        smoother.add(floatArrayOf(0.1f, 0.7f, 0.1f, 0.05f, 0.05f))
        val result = smoother.add(floatArrayOf(0.1f, 0.8f, 0.05f, 0.03f, 0.02f))

        assertEquals(1, result.classIndex)
        assertEquals((0.1f + 0.7f + 0.8f) / 3f, result.confidence, 0.0001f)
    }
}

