package com.grupo7.wisdmwatch.presentation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.Text
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import android.util.Log
import com.google.ai.edge.litert.Accelerator
import com.google.ai.edge.litert.CompiledModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        setContent {
            AccelerometerScreen()
        }
    }
}

internal fun interpolateAt(
    samples: ArrayDeque<Pair<Long, FloatArray>>,
    timestamp: Long
): FloatArray? {
    var previous: Pair<Long, FloatArray>? = null

    for (current in samples) {
        if (current.first == timestamp) {
            return current.second.copyOf()
        }

        if (current.first > timestamp) {
            val before = previous ?: return null
            val gap = current.first - before.first

            if (gap <= 0 || gap > 250_000_000L) return null

            val ratio = (timestamp - before.first).toFloat() / gap

            return FloatArray(3) { i ->
                before.second[i] +
                        ratio * (current.second[i] - before.second[i])
            }
        }

        previous = current
    }

    return null
}

private fun buildWindow(
    accelBuffer: ArrayDeque<Pair<Long, FloatArray>>,
    gyroBuffer: ArrayDeque<Pair<Long, FloatArray>>
): Array<FloatArray>? {

    if (accelBuffer.isEmpty() || gyroBuffer.isEmpty()) {
        return null
    }

    val endTime = minOf(
        accelBuffer.last().first,
        gyroBuffer.last().first
    )

    val window = Array(100) { FloatArray(6) }

    for (i in 0 until 100) {
        val timestamp = endTime - (99 - i) * 50_000_000L

        val accel = interpolateAt(accelBuffer, timestamp)
            ?: return null

        val gyro = interpolateAt(gyroBuffer, timestamp)
            ?: return null

        window[i] = floatArrayOf(
            accel[0], accel[1], accel[2],
            gyro[0], gyro[1], gyro[2]
        )
    }

    return window
}

internal fun quantizeWindow(
    window: Array<FloatArray>,
    scale: Float,
    zeroPoint: Int
): ByteArray {

    val result = ByteArray(100 * 6)
    var index = 0

    for (row in window) {
        for (value in row) {
            val quantized = (value / scale).roundToInt() + zeroPoint

            result[index++] = quantized
                .coerceIn(-128, 127)
                .toByte()
        }
    }

    return result
}

@Composable
fun AccelerometerScreen() {

    val context = androidx.compose.ui.platform.LocalContext.current

    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    val accelerometer = remember {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    var x by remember { mutableStateOf(0f) }
    var y by remember { mutableStateOf(0f) }
    var z by remember { mutableStateOf(0f) }

    val gyroscope = remember {
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    var gx by remember { mutableStateOf(0f) }
    var gy by remember { mutableStateOf(0f) }
    var gz by remember { mutableStateOf(0f) }
    val accelBuffer = remember { ArrayDeque<Pair<Long, FloatArray>>() }
    val gyroBuffer = remember { ArrayDeque<Pair<Long, FloatArray>>() }


    DisposableEffect(accelerometer, gyroscope) {

        val listener = object : SensorEventListener {

            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        x = event.values[0]
                        y = event.values[1]
                        z = event.values[2]
                        accelBuffer.addLast(event.timestamp to event.values.copyOf())

                        if (accelBuffer.size > 200) {
                            accelBuffer.removeFirst()
                        }
                    }

                    Sensor.TYPE_GYROSCOPE -> {
                        gx = event.values[0]
                        gy = event.values[1]
                        gz = event.values[2]
                        gyroBuffer.addLast(event.timestamp to event.values.copyOf())

                        if (gyroBuffer.size > 200) {
                            gyroBuffer.removeFirst()
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            }
        }

        accelerometer?.let {
            sensorManager.registerListener(
                listener,
                it,
                50_000
            )
        }

        gyroscope?.let {
            sensorManager.registerListener(
                listener,
                it,
                50_000
            )
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    var predictedActivity by remember {
        mutableStateOf("Aguardando classificação...")
    }

    LaunchedEffect(Unit) {

        val labels = context.assets.open("labels.txt")
            .bufferedReader().use { reader ->
                reader.readLines()
                    .filter { it.isNotBlank() }
                    .map { it.substringAfter(",") }
            }

        try {
            withContext(Dispatchers.Default) {

                CompiledModel.create(
                    context.assets,
                    "wisdm_har_int8.tflite",
                    CompiledModel.Options(Accelerator.CPU)
                ).use { model ->

                    val inputs = model.createInputBuffers()
                    val outputs = model.createOutputBuffers()

                    try {
                        while (true) {
                            delay(1_000)

                            val snapshots = withContext(Dispatchers.Main) {
                                ArrayDeque(accelBuffer) to ArrayDeque(gyroBuffer)
                            }

                            val window = buildWindow(
                                snapshots.first,
                                snapshots.second
                            ) ?: continue

                            val data = quantizeWindow(
                                window,
                                0.5244015f,
                                20
                            )

                            inputs[0].writeInt8(data)

                            model.run(inputs, outputs)

                            val scores = outputs[0].readInt8()

                            val index = scores.indices.maxByOrNull {
                                scores[it].toInt()
                            } ?: continue

                            Log.i(
                                "WISDM",
                                "Atividade identificada: ${labels[index]}"
                            )
                            withContext(Dispatchers.Main) {
                                predictedActivity = labels[index]
                            }
                        }
                    } finally {
                        inputs.forEach { it.close() }
                        outputs.forEach { it.close() }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WISDM", "Erro durante inferência", e)
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(top = 32.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Atividade:", color = Color.White)
        Text(
            text = predictedActivity,
            color = Color.Green,
            fontWeight = FontWeight.Bold
        )
        Text("Acelerômetro", color = Color.White)
        Text("X: %.2f".format(x), color = Color.White)
        Text("Y: %.2f".format(y), color = Color.White)
        Text("Z: %.2f".format(z), color = Color.White)
        Text("Giroscópio", color = Color.White)
        Text("GX: %.2f".format(gx), color = Color.White)
        Text("GY: %.2f".format(gy), color = Color.White)
        Text("GZ: %.2f".format(gz), color = Color.White)
    }
}