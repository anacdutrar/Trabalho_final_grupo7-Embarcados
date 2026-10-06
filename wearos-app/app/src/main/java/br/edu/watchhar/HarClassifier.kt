package br.edu.watchhar

import android.content.res.AssetManager
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.io.FileNotFoundException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

class HarClassifier private constructor(
    private val interpreter: Interpreter,
) : Closeable {
    private val inputTensor = interpreter.getInputTensor(0)
    private val outputTensor = interpreter.getOutputTensor(0)
    private val inputScale = inputTensor.quantizationParams().scale
    private val inputZeroPoint = inputTensor.quantizationParams().zeroPoint
    private val outputScale = outputTensor.quantizationParams().scale
    private val outputZeroPoint = outputTensor.quantizationParams().zeroPoint

    init {
        require(inputTensor.dataType() == DataType.INT8) { "A entrada deve ser int8" }
        require(outputTensor.dataType() == DataType.INT8) { "A saida deve ser int8" }
        require(inputTensor.shape().contentEquals(intArrayOf(1, 100, 6))) {
            "Entrada inesperada: ${inputTensor.shape().contentToString()}"
        }
        require(outputTensor.shape().contentEquals(intArrayOf(1, 5))) {
            "Saida inesperada: ${outputTensor.shape().contentToString()}"
        }
        require(inputScale > 0f && outputScale > 0f) {
            "Escalas de quantizacao invalidas"
        }
    }

    fun classify(values: FloatArray): FloatArray {
        require(values.size == ModelContract.SAMPLE_COUNT * ModelContract.CHANNEL_COUNT)
        val input = ByteBuffer.allocateDirect(values.size).order(ByteOrder.nativeOrder())
        values.forEach { value ->
            val quantized = (value / inputScale).roundToInt() + inputZeroPoint
            input.put(quantized.coerceIn(-128, 127).toByte())
        }
        input.rewind()

        val output = ByteBuffer.allocateDirect(ModelContract.CLASS_COUNT)
            .order(ByteOrder.nativeOrder())
        interpreter.run(input, output)
        output.rewind()

        val probabilities = FloatArray(ModelContract.CLASS_COUNT) { index ->
            val quantized = output.get(index).toInt()
            ((quantized - outputZeroPoint) * outputScale).coerceAtLeast(0f)
        }
        val total = probabilities.sum()
        if (total > 0f) {
            probabilities.indices.forEach { probabilities[it] /= total }
        }
        return probabilities
    }

    override fun close() = interpreter.close()

    companion object {
        fun load(assets: AssetManager): Result<HarClassifier> = runCatching {
            val bytes = try {
                assets.open(ModelContract.MODEL_ASSET).use { it.readBytes() }
            } catch (error: FileNotFoundException) {
                throw IllegalStateException(
                    "Copie ${ModelContract.MODEL_ASSET} para app/src/main/assets",
                    error,
                )
            }
            val model = ByteBuffer.allocateDirect(bytes.size)
                .order(ByteOrder.nativeOrder())
                .put(bytes)
            model.rewind()
            val options = Interpreter.Options().setNumThreads(2)
            HarClassifier(Interpreter(model, options))
        }
    }
}

