package br.edu.watchhar

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

class MainActivity : Activity(), SensorController.Callback {
    private lateinit var titleView: TextView
    private lateinit var predictionView: TextView
    private lateinit var confidenceView: TextView
    private lateinit var statusView: TextView
    private lateinit var actionButton: Button

    private var classifier: HarClassifier? = null
    private var controller: SensorController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildInterface()

        HarClassifier.load(assets)
            .onSuccess { loaded ->
                classifier = loaded
                controller = SensorController(this, loaded, this)
                statusView.text = "Modelo pronto. Use o pulso dominante."
                actionButton.isEnabled = true
            }
            .onFailure { error ->
                statusView.text = error.message ?: "Modelo nao encontrado"
                actionButton.isEnabled = false
            }
    }

    private fun buildInterface() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(26), dp(20), dp(26), dp(20))
            setBackgroundColor(Color.BLACK)
        }

        titleView = textView("Watch HAR", 18f, Color.LTGRAY)
        predictionView = textView("--", 25f, Color.WHITE).apply {
            setPadding(0, dp(10), 0, 0)
        }
        confidenceView = textView("", 15f, Color.rgb(128, 203, 196))
        statusView = textView("Carregando modelo...", 13f, Color.LTGRAY).apply {
            setPadding(0, dp(8), 0, dp(10))
        }
        actionButton = Button(this).apply {
            text = getString(R.string.start)
            isEnabled = false
            setOnClickListener { toggleCollection() }
        }

        root.addView(titleView, matchWrap())
        root.addView(predictionView, matchWrap())
        root.addView(confidenceView, matchWrap())
        root.addView(statusView, matchWrap())
        root.addView(actionButton, matchWrap())
        setContentView(root)
    }

    private fun toggleCollection() {
        val sensorController = controller ?: return
        if (sensorController.isRunning) {
            stopCollection("Coleta parada")
            return
        }
        if (sensorController.start()) {
            predictionView.text = "Aguardando"
            confidenceView.text = ""
            statusView.text = "Preenchendo janela: 0%"
            actionButton.text = getString(R.string.stop)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun stopCollection(message: String) {
        controller?.stop()
        actionButton.text = getString(R.string.start)
        statusView.text = message
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onProgress(progress: Float) {
        runOnUiThread {
            if (progress < 1f && controller?.isRunning == true) {
                statusView.text = "Preenchendo janela: ${(progress * 100).toInt()}%"
            }
        }
    }

    override fun onClassification(classification: Classification) {
        runOnUiThread {
            predictionView.text = classification.name
            confidenceView.text = String.format(
                Locale.US,
                "%s  |  %.1f%%",
                classification.code,
                classification.confidence * 100f,
            )
            statusView.text = "Classificando a cada 1 segundo"
        }
    }

    override fun onError(message: String) {
        runOnUiThread {
            stopCollection(message)
        }
    }

    override fun onPause() {
        super.onPause()
        if (controller?.isRunning == true) stopCollection("Pausado: app fora da tela")
    }

    override fun onDestroy() {
        controller?.stop()
        classifier?.close()
        super.onDestroy()
    }

    private fun textView(text: String, size: Float, color: Int) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(color)
        gravity = Gravity.CENTER
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    )

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}

