package br.edu.watchhar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SensorWindowBufferTest {
    @Test
    fun buildsWindowWithExpectedChannelOrder() {
        val buffer = SensorWindowBuffer()
        val base = 1_000_000_000L
        repeat(ModelContract.SAMPLE_COUNT) { index ->
            val timestamp = base + index * ModelContract.SAMPLE_PERIOD_NS
            buffer.add(
                MotionSensor.ACCELEROMETER,
                TimedVector(timestamp, index.toFloat(), 2f, 3f),
            )
            buffer.add(
                MotionSensor.GYROSCOPE,
                TimedVector(timestamp, 4f, 5f, 6f),
            )
        }

        val window = buffer.buildLatestWindow()
        assertNotNull(window)
        val validWindow = requireNotNull(window)
        assertEquals(ModelContract.SAMPLE_COUNT * 6, validWindow.values.size)
        assertEquals(0f, validWindow.values[0], 0.0001f)
        assertEquals(2f, validWindow.values[1], 0.0001f)
        assertEquals(3f, validWindow.values[2], 0.0001f)
        assertEquals(4f, validWindow.values[3], 0.0001f)
        assertEquals(5f, validWindow.values[4], 0.0001f)
        assertEquals(6f, validWindow.values[5], 0.0001f)
        assertEquals(99f, validWindow.values[99 * 6], 0.0001f)
    }

    @Test
    fun rejectsWindowThatWouldBridgeLargeGap() {
        val buffer = SensorWindowBuffer()
        repeat(ModelContract.SAMPLE_COUNT) { index ->
            if (index !in 40..46) {
                val timestamp = index * ModelContract.SAMPLE_PERIOD_NS
                buffer.add(
                    MotionSensor.ACCELEROMETER,
                    TimedVector(timestamp, 1f, 2f, 3f),
                )
            }
            val timestamp = index * ModelContract.SAMPLE_PERIOD_NS
            buffer.add(
                MotionSensor.GYROSCOPE,
                TimedVector(timestamp, 4f, 5f, 6f),
            )
        }

        assertNull(buffer.buildLatestWindow())
    }
}
