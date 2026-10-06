package br.edu.watchhar

import java.util.ArrayDeque
import kotlin.math.max

/**
 * Guarda eventos assincronos e os reamostra para a grade fixa usada no treino.
 * A ordem de saida por instante e accel XYZ, gyro XYZ.
 */
class SensorWindowBuffer {
    private val accelerometer = ArrayDeque<TimedVector>()
    private val gyroscope = ArrayDeque<TimedVector>()

    @Synchronized
    fun reset() {
        accelerometer.clear()
        gyroscope.clear()
    }

    @Synchronized
    fun add(sensor: MotionSensor, sample: TimedVector): Boolean {
        val target = if (sensor == MotionSensor.ACCELEROMETER) {
            accelerometer
        } else {
            gyroscope
        }

        if (target.isNotEmpty() && sample.timestampNs <= target.peekLast().timestampNs) {
            return false
        }
        target.addLast(sample)
        pruneOldSamples()
        return true
    }

    @Synchronized
    fun coverage(): Float {
        if (accelerometer.isEmpty() || gyroscope.isEmpty()) return 0f
        val commonStart = max(
            accelerometer.peekFirst().timestampNs,
            gyroscope.peekFirst().timestampNs,
        )
        val commonEnd = minOf(
            accelerometer.peekLast().timestampNs,
            gyroscope.peekLast().timestampNs,
        )
        val required = (ModelContract.SAMPLE_COUNT - 1L) * ModelContract.SAMPLE_PERIOD_NS
        return ((commonEnd - commonStart).coerceAtLeast(0L).toDouble() / required)
            .coerceIn(0.0, 1.0)
            .toFloat()
    }

    @Synchronized
    fun buildLatestWindow(): ModelWindow? {
        if (accelerometer.size < 2 || gyroscope.size < 2) return null

        val end = minOf(
            accelerometer.peekLast().timestampNs,
            gyroscope.peekLast().timestampNs,
        )
        val start = end -
            (ModelContract.SAMPLE_COUNT - 1L) * ModelContract.SAMPLE_PERIOD_NS

        if (accelerometer.peekFirst().timestampNs > start ||
            gyroscope.peekFirst().timestampNs > start
        ) {
            return null
        }

        val accel = accelerometer.toList()
        val gyro = gyroscope.toList()
        val output = FloatArray(ModelContract.SAMPLE_COUNT * ModelContract.CHANNEL_COUNT)

        var accelCursor = 0
        var gyroCursor = 0
        for (sampleIndex in 0 until ModelContract.SAMPLE_COUNT) {
            val targetTime = start + sampleIndex * ModelContract.SAMPLE_PERIOD_NS
            val accelResult = interpolate(accel, targetTime, accelCursor) ?: return null
            accelCursor = accelResult.nextCursor
            val gyroResult = interpolate(gyro, targetTime, gyroCursor) ?: return null
            gyroCursor = gyroResult.nextCursor

            val base = sampleIndex * ModelContract.CHANNEL_COUNT
            output[base] = accelResult.value.x
            output[base + 1] = accelResult.value.y
            output[base + 2] = accelResult.value.z
            output[base + 3] = gyroResult.value.x
            output[base + 4] = gyroResult.value.y
            output[base + 5] = gyroResult.value.z
        }
        return ModelWindow(end, output)
    }

    private data class InterpolationResult(
        val value: TimedVector,
        val nextCursor: Int,
    )

    private fun interpolate(
        samples: List<TimedVector>,
        targetTime: Long,
        initialCursor: Int,
    ): InterpolationResult? {
        var right = initialCursor.coerceIn(0, samples.lastIndex)
        while (right < samples.size && samples[right].timestampNs < targetTime) {
            right++
        }
        if (right >= samples.size) return null

        val rightSample = samples[right]
        if (rightSample.timestampNs == targetTime) {
            return InterpolationResult(rightSample, right)
        }
        if (right == 0) return null

        val leftSample = samples[right - 1]
        val gap = rightSample.timestampNs - leftSample.timestampNs
        if (gap <= 0L || gap > ModelContract.MAX_INTERPOLATION_GAP_NS) return null

        val ratio = (targetTime - leftSample.timestampNs).toFloat() / gap.toFloat()
        return InterpolationResult(
            TimedVector(
                targetTime,
                leftSample.x + ratio * (rightSample.x - leftSample.x),
                leftSample.y + ratio * (rightSample.y - leftSample.y),
                leftSample.z + ratio * (rightSample.z - leftSample.z),
            ),
            right - 1,
        )
    }

    private fun pruneOldSamples() {
        val newest = maxOf(
            accelerometer.peekLast()?.timestampNs ?: Long.MIN_VALUE,
            gyroscope.peekLast()?.timestampNs ?: Long.MIN_VALUE,
        )
        if (newest == Long.MIN_VALUE) return
        val keepAfter = newest -
            (ModelContract.WINDOW_SECONDS + 2L) * 1_000_000_000L
        while (accelerometer.size > 2 &&
            accelerometer.elementAt(1).timestampNs < keepAfter
        ) {
            accelerometer.removeFirst()
        }
        while (gyroscope.size > 2 &&
            gyroscope.elementAt(1).timestampNs < keepAfter
        ) {
            gyroscope.removeFirst()
        }
    }
}

