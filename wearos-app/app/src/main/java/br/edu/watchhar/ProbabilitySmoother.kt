package br.edu.watchhar

import java.util.ArrayDeque

class ProbabilitySmoother(private val capacity: Int = 3) {
    private val history = ArrayDeque<FloatArray>()

    init {
        require(capacity > 0)
    }

    fun reset() = history.clear()

    fun add(probabilities: FloatArray): Classification {
        require(probabilities.size == ModelContract.CLASS_COUNT)
        history.addLast(probabilities.copyOf())
        while (history.size > capacity) history.removeFirst()

        val average = FloatArray(ModelContract.CLASS_COUNT)
        history.forEach { row ->
            row.forEachIndexed { index, value -> average[index] += value }
        }
        average.indices.forEach { index -> average[index] /= history.size.toFloat() }
        val winner = average.indices.maxBy { average[it] }
        return Classification(winner, average)
    }
}

