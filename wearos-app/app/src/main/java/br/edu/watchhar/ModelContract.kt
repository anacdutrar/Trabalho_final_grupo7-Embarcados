package br.edu.watchhar

object ModelContract {
    const val MODEL_ASSET = "wisdm_har_int8.tflite"
    const val SAMPLE_RATE_HZ = 20
    const val WINDOW_SECONDS = 5
    const val SAMPLE_COUNT = SAMPLE_RATE_HZ * WINDOW_SECONDS
    const val CHANNEL_COUNT = 6
    const val CLASS_COUNT = 5
    const val SAMPLE_PERIOD_NS = 1_000_000_000L / SAMPLE_RATE_HZ
    const val INFERENCE_HOP_NS = 1_000_000_000L
    const val MAX_INTERPOLATION_GAP_NS = 250_000_000L

    val codes = arrayOf("A", "B", "C", "G", "R")
    val names = arrayOf(
        "Caminhada",
        "Corrida",
        "Escadas",
        "Escovar os dentes",
        "Bater palmas",
    )
}

enum class MotionSensor {
    ACCELEROMETER,
    GYROSCOPE,
}

data class TimedVector(
    val timestampNs: Long,
    val x: Float,
    val y: Float,
    val z: Float,
)

data class ModelWindow(
    val endTimestampNs: Long,
    val values: FloatArray,
)

data class Classification(
    val classIndex: Int,
    val probabilities: FloatArray,
) {
    val code: String get() = ModelContract.codes[classIndex]
    val name: String get() = ModelContract.names[classIndex]
    val confidence: Float get() = probabilities[classIndex]
}

