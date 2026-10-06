package br.edu.watchhar

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread

class SensorController(
    context: Context,
    private val classifier: HarClassifier,
    private val callback: Callback,
) : SensorEventListener {
    interface Callback {
        fun onProgress(progress: Float)
        fun onClassification(classification: Classification)
        fun onError(message: String)
    }

    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val buffer = SensorWindowBuffer()
    private val smoother = ProbabilitySmoother(3)
    private var workerThread: HandlerThread? = null
    private var lastInferenceTimestampNs = Long.MIN_VALUE
    private var lastProgressNotificationNs = Long.MIN_VALUE

    @Volatile
    var isRunning: Boolean = false
        private set

    fun start(): Boolean {
        if (isRunning) return true
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        if (accelerometer == null || gyroscope == null) {
            callback.onError("Acelerometro ou giroscopio nao encontrado")
            return false
        }

        buffer.reset()
        smoother.reset()
        lastInferenceTimestampNs = Long.MIN_VALUE
        lastProgressNotificationNs = Long.MIN_VALUE

        val thread = HandlerThread("watch-har-sensors").also { it.start() }
        workerThread = thread
        val handler = Handler(thread.looper)
        val samplingPeriodUs = 1_000_000 / ModelContract.SAMPLE_RATE_HZ
        val accelRegistered = sensorManager.registerListener(
            this,
            accelerometer,
            samplingPeriodUs,
            0,
            handler,
        )
        val gyroRegistered = sensorManager.registerListener(
            this,
            gyroscope,
            samplingPeriodUs,
            0,
            handler,
        )
        if (!accelRegistered || !gyroRegistered) {
            sensorManager.unregisterListener(this)
            thread.quitSafely()
            workerThread = null
            callback.onError("Falha ao registrar os sensores")
            return false
        }
        isRunning = true
        return true
    }

    fun stop() {
        if (!isRunning && workerThread == null) return
        isRunning = false
        sensorManager.unregisterListener(this)
        workerThread?.quitSafely()
        workerThread = null
        buffer.reset()
        smoother.reset()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isRunning || event.values.size < 3) return
        val sensor = when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> MotionSensor.ACCELEROMETER
            Sensor.TYPE_GYROSCOPE -> MotionSensor.GYROSCOPE
            else -> return
        }
        val accepted = buffer.add(
            sensor,
            TimedVector(
                event.timestamp,
                event.values[0],
                event.values[1],
                event.values[2],
            ),
        )
        if (!accepted) return

        if (lastProgressNotificationNs == Long.MIN_VALUE ||
            event.timestamp - lastProgressNotificationNs >= 250_000_000L
        ) {
            lastProgressNotificationNs = event.timestamp
            callback.onProgress(buffer.coverage())
        }

        val window = buffer.buildLatestWindow() ?: return
        if (lastInferenceTimestampNs != Long.MIN_VALUE &&
            window.endTimestampNs - lastInferenceTimestampNs < ModelContract.INFERENCE_HOP_NS
        ) {
            return
        }
        lastInferenceTimestampNs = window.endTimestampNs

        runCatching { classifier.classify(window.values) }
            .onSuccess { callback.onClassification(smoother.add(it)) }
            .onFailure { callback.onError("Falha na inferencia: ${it.message}") }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

